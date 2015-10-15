/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIWINDOW

#import "TiUIWindowProxy.h"
#import "Webcolor.h"
#import "TiUIViewProxy.h"
#import "ImageLoader.h"
#import "TiComplexValue.h"
#import "TiApp.h"
#import "TiLayoutQueue.h"
#import "UIViewController+ADTransitionController.h"
#import "TiViewController.h"

// this is how long we should wait on the new JS context to be loaded
// holding the UI thread before we return during an window open. we 
// attempt to hold it for a small period of time to allow the window
// to loaded before we return from the open such that the paint will be
// much smoother on the new window during a tab transition
#define EXTERNAL_JS_WAIT_TIME (150/1000)

/** 
 * This class is a helper that will be used when we have an external
 * window w/ JS so that we can attempt to wait for the window context
 * to be fully loaded on the UI thread (since JS runs in a different
 * thread) and attempt to wait up til EXTERNAL_JS_WAIT_TIME before
 * timing out. If timed out, will go ahead and start opening the window
 * and as the JS context finishes, will continue opening from there - 
 * this has a nice effect of immediately opening if fast but not delaying
 * if slow (so you get weird button delay effects for example)
 *
 */

@interface TiUIWindowProxyLatch : NSObject
{
	NSCondition *lock;
	TiUIWindowProxy* window;
	id args;
	BOOL completed;
	BOOL timeout;
}
@end

@implementation TiUIWindowProxyLatch

-(id)initWithTiWindow:(id)window_ args:(id)args_
{
	if (self = [super init])
	{
		window = [window_ retain];
		args = [args_ retain];
		lock = [[NSCondition alloc] init];
	}
	return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(lock);
	RELEASE_TO_NIL(window);
	RELEASE_TO_NIL(args);
	[super dealloc];
}

-(void)booted:(id)arg
{
	[lock lock];
	completed = YES;
	[lock signal];
	if (timeout)
	{
		[window boot:YES args:args];
	}
	[lock unlock];
}

-(BOOL)waitForBoot
{
	BOOL yn = NO;
	[lock lock];
	if(completed)
	{
		yn = YES;
	}
	else
	{
		if ([lock waitUntilDate:[NSDate dateWithTimeIntervalSinceNow:EXTERNAL_JS_WAIT_TIME]]==NO)
		{
			timeout = YES;
		}
		else 
		{
			yn = YES;
		}
	}
	[lock unlock];
	return yn;
}

@end


@implementation TiUIWindowProxy
{
    BOOL navButtonNotSet;
    BOOL navBarWillShow;
    BOOL toolbarButtonNotSet;
    BOOL toolbarBarWillShow;
    BOOL noNavBar;
    CGFloat _defaultNavBarTop;
    CGFloat _defaultToolbarTop;
    BOOL _setingUpWindowDecorations;
    
    CGRect startingNavbarFrame;
    CGRect startingToolbarFrame;
}

-(NSArray *)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"barColor"]] retain];;
    });
    return keySequence;
}

-(void)_destroy
{
    if (!closing && opened) {
        TiThreadPerformOnMainThread(^{[self close:nil];}, YES);
    }
    
#ifdef TI_USE_KROLL_THREAD
	TiThreadRemoveFromSuperviewOnMainThread(barImageView, NO);
	TiThreadReleaseOnMainThread(barImageView, NO);
	barImageView = nil;
#else
    TiThreadPerformOnMainThread(^{
        [barImageView removeFromSuperview];
        RELEASE_TO_NIL(barImageView);
    }, YES);
#endif
    if (context!=nil)
	{
		[context shutdown:nil];
		RELEASE_TO_NIL(context);
	}
	RELEASE_TO_NIL(oldBaseURL);
	RELEASE_TO_NIL(latch);
	[super _destroy];
}

-(void)_configure
{
    noNavBar = NO;
    navButtonNotSet = NO;
    navBarWillShow = NO;
    toolbarButtonNotSet = NO;
    toolbarBarWillShow = NO;
    _defaultNavBarTop = 20;
    _defaultToolbarTop = [TiUtils appFrame].size.height - 44;
    _setingUpWindowDecorations = NO;
    startingNavbarFrame = CGRectZero;
    startingToolbarFrame = CGRectZero;
    
	[super _configure];
}

-(void)_initWithProperties:(NSDictionary *)properties
{
    NSNull* null = [NSNull null];
    [self initializeProperties:@{
                                 @"backgroundColor":@"white",
                                 @"barColor":null,
                                 @"navTintColor":null,
                                 @"barImage":null,
                                 @"translucent":null,
                                 @"barDeltaY":@(0),
                                 @"toolbarDeltaY":@(0),
                                 @"barStyle":null,
                                 @"titleAttributes":null,
                                 @"tabBarHidden":@(NO),
                                 @"navBarHidden":@(NO),
                                 }];
    [super _initWithProperties:properties];
}

-(NSString*)apiName
{
    return @"Ti.UI.Window";
}


-(void)dealloc
{
    [super dealloc];
}

//-(oneway void)release
//{
//    [super release];
//}

-(void)boot:(BOOL)timeout args:args
{
    RELEASE_TO_NIL(latch);
    if (timeout) {
        if (![context evaluationError]) {
            contextReady = YES;
            [self open:args];
        } else {
            DebugLog(@"Could not boot context. Context has evaluation error");
        }
    }
}

-(BOOL)optimizeSubviewInsertion
{
    return YES;
}

-(NSMutableDictionary*)langConversionTable
{
	return [NSMutableDictionary dictionaryWithObjectsAndKeys:@"title",@"titleid",@"titlePrompt",@"titlepromptid",nil];
}

-(void)rotateEvent:(NSNotification*)sender
{
    [self fireEvent:@"orientationchange" withObject:@{
                                                     @"orientation": @([UIDevice currentDevice].orientation) }];
}

-(void)registerForOrientation
{
    [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(rotateEvent:) name:UIDeviceOrientationDidChangeNotification object:nil];
}

-(void)_listenerAdded:(NSString *)type count:(NSInteger)count
{
    if (count == 1 && [type isEqualToString:@"orientationchange"])
    {
        TiThreadPerformOnMainThread(^{[self registerForOrientation];}, NO);
    }
}

-(void)unregisterForNotificationNamed:(NSString *)oldNotification
{
    WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
    [[NSNotificationCenter defaultCenter] removeObserver:self name:oldNotification object:nil];
}

-(void)_listenerRemoved:(NSString *)type count:(NSInteger)count
{
    if (count == 0 && [type isEqualToString:@"orientationchange"])
    {
        TiThreadPerformOnMainThread(^{[self unregisterForNotificationNamed:UIDeviceOrientationDidChangeNotification];}, NO);
    }
}

#pragma mark - TiWindowProtocol overrides

-(UIViewController*)hostingController;
{
    if (controller == nil) {
        UIViewController* theController = [super hostingController];
        [theController setHidesBottomBarWhenPushed:[TiUtils boolValue:[self valueForUndefinedKey:@"tabBarHidden"] def:NO]];
        return theController;
    }
    return [super hostingController];
}

-(BOOL)_handleOpen:(id)args
{
	// this is a special case that calls open again above to cause the event lifecycle to
	// happen after the JS context is fully up and ready
	if (contextReady && context!=nil)
	{
		return [super _handleOpen:args];
	}
	
	//
	// at this level, open is top-level since this is a window.  if you want
	// to open a window within a tab, you'll need to call tab.open(window)
	//
	
	NSURL *url = [TiUtils toURL:[self valueForKey:@"url"] proxy:self];
	
	if (url!=nil)
	{
        DebugLog(@"[WARN] The Ti.Window.url property is deprecated and will be remove on the next release");
        // Window based JS can only be loaded from local filesystem within app resources
		if ([url isFileURL] && [[[url absoluteString] lastPathComponent] hasSuffix:@".js"])
		{
			// since this function is recursive, only do this if we haven't already created the context
			if (context==nil)
			{
				RELEASE_TO_NIL(context);
				// remember our base url so we can restore on close
				oldBaseURL = [[self _baseURL] retain];
				// set our new base
				[self _setBaseURL:url];
				contextReady=NO;
				context = [[KrollBridge alloc] initWithHost:[self _host]];
				id theTabGroup = [tab tabGroup];
				id theTab = (theTabGroup == nil)?nil:tab;
				NSDictionary *values = [NSDictionary dictionaryWithObjectsAndKeys:self,@"currentWindow",theTabGroup,@"currentTabGroup",theTab,@"currentTab",nil];
				NSDictionary *preload = [NSDictionary dictionaryWithObjectsAndKeys:values,@"UI",nil];
				latch = [[TiUIWindowProxyLatch alloc] initWithTiWindow:self args:args];
				[context boot:latch url:url preload:preload];
				if ([latch waitForBoot])
				{
                    if ([context evaluationError]) {
                        DebugLog(@"Could not boot context. Context has evaluation error");
                        return NO;
                    }
                    contextReady = YES;
					return [super _handleOpen:args];
				}
				else
				{
					return NO;
				}
			}
		}
		else
		{
			DebugLog(@"[ERROR] Url not supported in a window. %@",url);
		}
	}
	
	return [super _handleOpen:args];
}

-(void) windowDidClose
{
	[self cleanupWindowDecorations];
    // Because other windows or proxies we have open and wish to continue functioning might be relying
    // on our created context, we CANNOT explicitly shut down here.  Instead we should memory-manage
    // contexts better so they stop when they're no longer in use.
    
    // Sadly, today is not that day. Without shutdown, we leak all over the place.
    if (context!=nil) {
        NSMutableArray* childrenToRemove = [[NSMutableArray alloc] init];
        pthread_rwlock_rdlock(&childrenLock);
        for (TiViewProxy* child in children) {
            if ([child belongsToContext:context]) {
                [childrenToRemove addObject:child];
            }
        }
        pthread_rwlock_unlock(&childrenLock);
        [context performSelector:@selector(shutdown:) withObject:nil afterDelay:1.0];
        RELEASE_TO_NIL(context);
        
        for (TiViewProxy* child in childrenToRemove) {
            [self remove:child];
        }
        [childrenToRemove release];
    }
    [super windowDidClose];
}

-(BOOL)_handleClose:(id)args
{
    if (oldBaseURL!=nil)
	{
		[self _setBaseURL:oldBaseURL];
	}
	RELEASE_TO_NIL(oldBaseURL);
	return [super _handleClose:args];
}

-(id) navControllerForController:(UIViewController*)theController
{
    if ([theController transitionController] != nil)
        return [theController transitionController];
    return [theController navigationController];
}

- (void)viewWillAppear:(BOOL)animated;    // Called when the view is about to made visible. Default does nothing
{
    shouldUpdateNavBar = !noNavBar;
    id delegate = [[self navControllerForController:controller] delegate];
    BOOL isInteracting = NO;
    if (IS_OF_CLASS(delegate, ADNavigationControllerDelegate)) {
        isInteracting = [delegate isInteracting];
    }
    if (!isInteracting) {
        [self setupWindowDecorations:animated];
    }
    [super viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated;     // Called when the view has been fully transitioned onto the screen. Default does nothing
{
    BOOL isInteracting = NO;
    id delegate = [[self navControllerForController:controller] delegate];
    if (IS_OF_CLASS(delegate, ADNavigationControllerDelegate)) {
        isInteracting = [delegate isInteracting];
    }
    if (isInteracting) {
        [self setupWindowDecorations:animated];
        ((ADNavigationControllerDelegate*)delegate).isInteracting = NO;
    } else {
        [self updateTitleView];
    }
    [super viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated; // Called when the view is dismissed, covered or otherwise hidden. Default does nothing
{
    shouldUpdateNavBar = NO;
    
    
    id navController = [self navControllerForController:controller];
    if (navController) {
        BOOL isInteracting = NO;
        id delegate = [navController delegate];
        if (IS_OF_CLASS(delegate, ADNavigationControllerDelegate)) {
            isInteracting = [delegate isInteracting];
        }
        if (!isInteracting) {
            if (animated) {
                [UIView beginAnimations:@"navbarAnim" context:NULL];
                [UIView setAnimationBeginsFromCurrentState:YES];
            }
            if (![navController navigationBar].hidden) {
                [navController navigationBar].frame = startingNavbarFrame;
            }
            if (![navController toolbar].hidden) {
                [navController toolbar].frame = startingToolbarFrame;
            }

            if (animated) {
                [UIView commitAnimations];
            }
        }
        
    }
    
    
    [super viewWillDisappear:animated];
}


-(void)windowWillOpen
{
    [super windowWillOpen];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(rootViewDidForceFrame:) name:kTiFrameAdjustNotification object:nil];
}

#pragma mark - UINavController, NavItem UI


#define SETPROP(m,x) \
{\
id value = [self valueForKey:m]; \
if (value!=nil)\
{\
[self x:(value==[NSNull null]) ? nil : value];\
}\
else{\
[self replaceValue:nil forKey:m notification:NO];\
}\
}\

#define SETPROPOBJ(m,x) \
{\
id value = [self valueForKey:m]; \
if (value!=nil)\
{\
if ([value isKindOfClass:[TiComplexValue class]])\
{\
TiComplexValue *cv = (TiComplexValue*)value;\
[self x:(cv.value==[NSNull null]) ? nil : cv.value withObject:cv.properties];\
}\
else\
{\
[self x:(value==[NSNull null]) ? nil : value withObject:nil];\
}\
}\
else{\
[self replaceValue:nil forKey:m notification:NO];\
}\
}\

-(void)showNavBar:(NSArray*)args
{
	ENSURE_UI_THREAD(showNavBar,args);
    id navController = [self navControllerForController:controller];
    if ((controller == nil) || navController == nil) {
        return;
    }
    navBarWillShow = YES;
    if (navButtonNotSet) {
        [self refreshLeftNavButtons:nil];
        [self refreshRightNavButtons:nil];
    }
	[self replaceValue:@NO forKey:@"navBarHidden" notification:NO];
	if (controller!=nil)
	{
        id navController = [self navControllerForController:controller];
		id properties = (args!=nil && [args count] > 0) ? [args objectAtIndex:0] : nil;
		BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
		[navController setNavigationBarHidden:NO animated:animated];
        navBarWillShow = NO;
	}
}

-(void)hideNavBar:(NSArray*)args
{
	ENSURE_UI_THREAD(hideNavBar,args);
	[self replaceValue:@YES forKey:@"navBarHidden" notification:NO];
	if (controller!=nil)
	{
        id navController = [self navControllerForController:controller];
		id properties = (args!=nil && [args count] > 0) ? [args objectAtIndex:0] : nil;
		BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
		[navController setNavigationBarHidden:YES animated:animated];
		//TODO: need to fix height
	}
}

-(void)setNoNavBar:(id)value
{
    [self replaceValue:value forKey:@"noNavBar" notification:NO];
    noNavBar = [TiUtils boolValue:value def:NO];
}

-(void)setNavTintColor:(id)colorString
{
    ENSURE_UI_THREAD(setNavTintColor,colorString);
    NSString *color = [TiUtils stringValue:colorString];
    [self replaceValue:color forKey:@"navTintColor" notification:NO];

    
    if(controller != nil) {
        id navController = [self navControllerForController:controller];
        TiColor * newColor = [TiUtils colorValue:color];
        if (newColor == nil) {
            //Get from TabGroup
            newColor = [TiUtils colorValue:[[self tabGroup] valueForKey:@"navTintColor"]];
        }
        
        UINavigationBar * navBar = [navController navigationBar];
        if (newColor == nil) {
            [navBar setTintColor:[self view].tintColor];
        }
        else {
            [navBar setTintColor:[newColor color]];
        }
        if (!_setingUpWindowDecorations) {
            [self performSelector:@selector(refreshBackButton) withObject:nil afterDelay:0.0];
        }
    }
        
}

-(void)setBarColor:(id)colorString
{
    ENSURE_UI_THREAD(setBarColor,colorString);
    [self replaceValue:colorString forKey:@"barColor" notification:NO];
    id navController = [self navControllerForController:controller];
    if (shouldUpdateNavBar && navController != nil)
    {
        TiColor * newColor = [TiUtils colorValue:colorString];
        if (newColor == nil)
        {
            newColor =[TiUtils colorValue:[[self tabGroup] valueForKey:@"barColor"]];
        }
        
        UIColor * barColor = [TiUtils barColorForColor:newColor];
        UIBarStyle navBarStyle = [TiUtils barStyleForColor:newColor];
        
        UINavigationBar * navBar = [navController navigationBar];
        [navBar setBarStyle:navBarStyle];
//        [navBar setTranslucent:[TiUtils barTranslucencyForColor:newColor]];
        [navBar performSelector:@selector(setBarTintColor:) withObject:barColor];

        if (!_setingUpWindowDecorations) {
            [self performSelector:@selector(refreshBackButton) withObject:nil afterDelay:0.0];
        }
    }
}

-(void)setBarDeltaY:(id)value {
    ENSURE_UI_THREAD(setBarDeltaY,value);
    id navController = [self navControllerForController:controller];
    if (shouldUpdateNavBar && navController != nil)
    {
        CGFloat deltaY = [TiUtils floatValue:value def:0];
        UINavigationBar * navBar = [navController navigationBar];
        CGRect frame = navBar.frame;
        frame.origin.y = startingNavbarFrame.origin.y - deltaY;
        navBar.frame = frame;
    }
    [self replaceValue:value forKey:@"barDeltaY" notification:NO];
}

-(void)setToolbarDeltaY:(id)value {
    ENSURE_UI_THREAD(setBarDeltaY,value);
    id navController = [self navControllerForController:controller];
    if (shouldUpdateNavBar && navController != nil)
    {
        CGFloat deltaY = [TiUtils floatValue:value def:0];
        UIToolbar * toolBar = [navController toolbar];
        CGRect frame = toolBar.frame;
        frame.origin.y = startingToolbarFrame.origin.y + deltaY;
        toolBar.frame = frame;
    }
    [self replaceValue:value forKey:@"toolbarDeltaY" notification:NO];
}

-(void)setBarStyle:(id)value
{
	ENSURE_UI_THREAD(setBarStyle,value);
    [self replaceValue:value forKey:@"barStyle" notification:NO];
    id navController = [self navControllerForController:controller];
    if (shouldUpdateNavBar && navController != nil)
    {
        UINavigationBar * navBar = [navController navigationBar];
        UIBarStyle style = [TiUtils intValue:value def:navBar.barStyle];
        
        [navBar setBarStyle:style];
    }
}

-(void)setTitleAttributes:(id)args
{
    ENSURE_UI_THREAD(setTitleAttributes,args);
    ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary);
    [self replaceValue:args forKey:@"titleAttributes" notification:NO];
    
    if (args == nil) {
        args = [[self tabGroup] valueForUndefinedKey:@"titleAttributes"];
    }

    NSMutableDictionary* theAttributes = nil;
    if (args != nil) {
        theAttributes = [NSMutableDictionary dictionary];
        if ([args objectForKey:@"color"] != nil) {
            UIColor* theColor = [[TiUtils colorValue:@"color" properties:args] _color];
            if (theColor != nil) {
                [theAttributes setObject:theColor forKey:NSForegroundColorAttributeName];
            }
        }
        if ([args objectForKey:@"shadow"] != nil) {
            NSShadow* shadow = [TiUtils shadowValue:[args objectForKey:@"shadow"]];
            if (shadow != nil) {
                [theAttributes setObject:shadow forKey:NSShadowAttributeName];
            }
        }
        
        if ([args objectForKey:@"font"] != nil) {
            UIFont* theFont = [[TiUtils fontValue:[args objectForKey:@"font"] def:nil] font];
            if (theFont != nil) {
                [theAttributes setObject:theFont forKey:NSFontAttributeName];
            }
        }
        
        if ([theAttributes count] == 0) {
            theAttributes = nil;
        }
    }
    
    if (shouldUpdateNavBar && ([controller navigationController] != nil)) {
        id navController = [self navControllerForController:controller];
        [[controller navigationItem] setTitle:@""];
        [[navController navigationBar] setTitleTextAttributes:theAttributes];
    }
}

-(void)updateBarImage
{
	id navController = [self navControllerForController:controller];
    if (navController == nil || !shouldUpdateNavBar) {
        return;
    }
    
    id barImageValue = [self valueForUndefinedKey:@"barImage"];
    
    UINavigationBar * ourNB = [navController navigationBar];
    UIImage* theImage = nil;
    //TIMOB-16490
    theImage = [TiUtils toImage:barImageValue proxy:self];

    
    if (theImage == nil) {
        [ourNB setBackgroundImage:nil forBarMetrics:UIBarMetricsDefault];
    } else {
        UIImage* resizableImage = [theImage resizableImageWithCapInsets:UIEdgeInsetsMake(0, 0, 0, 0) resizingMode:UIImageResizingModeStretch];
        [ourNB setBackgroundImage:resizableImage forBarMetrics:UIBarMetricsDefault];
        
    }
    id shadowImageValue = [self valueForUndefinedKey:@"shadowImage"];
    UIImage* theShadowImage = [TiUtils toImage:shadowImageValue proxy:self];
    
    if (theShadowImage != nil) {
        UIImage* resizableImage = [theImage resizableImageWithCapInsets:UIEdgeInsetsMake(0, 0, 0, 0) resizingMode:UIImageResizingModeStretch];
        ourNB.shadowImage = resizableImage;
    } else {
        BOOL clipValue = [TiUtils boolValue:[self valueForUndefinedKey:@"hideShadow"] def:NO];
        if (clipValue) {
            
            if (theImage == nil) {
                TiColor* barColor =[TiUtils colorValue:[self valueForUndefinedKey:@"barColor"]];
                if (barColor && barColor.color != [UIColor clearColor]) {
                    [ourNB setBackgroundImage:[TiUtils imageFromColor:barColor.color] forBarMetrics:UIBarMetricsDefault];
                } else {
                    [ourNB setBackgroundImage:[[[UIImage alloc] init] autorelease] forBarMetrics:UIBarMetricsDefault];
                }
            }
            //Set an empty Image.
            ourNB.shadowImage = [[[UIImage alloc] init] autorelease];
        } else {
            ourNB.shadowImage = nil;
        }
    }
}

-(void)setBarImage:(id)value
{
	[self replaceValue:value forKey:@"barImage" notification:NO];
	if (controller!=nil)
	{
		TiThreadPerformOnMainThread(^{[self updateBarImage];}, NO);
	}
}

-(id)barHeight
{
    id navController = [self navControllerForController:controller];
    if (navController == nil) {
        return @(0);
    }
    UINavigationBar * ourNB = [navController navigationBar];
    return @(ourNB.frame.size.height);
}

-(void)setShadowImage:(id)value
{
    ENSURE_UI_THREAD(setShadowImage,value);
	[self replaceValue:value forKey:@"shadowImage" notification:NO];
	if (controller!=nil)
	{
		[self updateBarImage];
	}
}

-(void)setHideShadow:(id)value
{
    ENSURE_UI_THREAD(setHideShadow,value);
	[self replaceValue:value forKey:@"hideShadow" notification:NO];
	if (controller!=nil)
	{
        [self updateBarImage];
	}
}

-(void)setTranslucent:(id)value
{
	ENSURE_UI_THREAD(setTranslucent,value);
	[self replaceValue:value forKey:@"translucent" notification:NO];
	if (controller!=nil)
	{
        id navController = [self navControllerForController:controller];
        BOOL def = [navController navigationBar].translucent;
		[navController navigationBar].translucent = [TiUtils boolValue:value def:def];
	}
}

-(void)updateNavButtons
{
    //Update LeftNavButton
//    NSDictionary* lProperties = [self valueForUndefinedKey:@"leftNavSettings"];
//    id leftNavButtons = [self valueForUndefinedKey:@"leftNavButtons"];
//    if (!IS_NULL_OR_NIL(leftNavButtons)) {
//        [self setLeftNavButtons:leftNavButtons withObject:lProperties];
//    } else {
//        leftNavButtons = [self valueForUndefinedKey:@"leftNavButton"];
//        [self setLeftNavButton:leftNavButtons withObject:lProperties];
//    }
//    //Update RightNavButton
//    NSDictionary* rProperties = [self valueForUndefinedKey:@"rightNavSettings"];
//    id rightNavButtons = [self valueForUndefinedKey:@"rightNavButtons"];
//    if (!IS_NULL_OR_NIL(rightNavButtons)) {
//        [self setRightNavButtons:rightNavButtons withObject:rProperties];
//    } else {
//        rightNavButtons = [self valueForUndefinedKey:@"rightNavButton"];
//        [self setRightNavButton:rightNavButtons withObject:rProperties];
//    }
    [self refreshLeftNavButtons:nil];
    [self refreshRightNavButtons:nil];
}

-(void)refreshRightNavButtons:(id)unused
{
	id navController = [self navControllerForController:controller];
    if (controller == nil || navController == nil) {
        return; // No need to refresh
    }
	if (!navBarWillShow && [TiUtils boolValue:[self valueForKey:@"navBarHidden"] def:NO]){
    	navButtonNotSet = YES;
    	return;
    }
    navButtonNotSet = NO;
    NSArray* theObjects = [self holdedProxyForKey:@"rightNavButtons"];
    NSDictionary* theProperties = [self valueForUndefinedKey:@"rightNavSettings"];
    
    ENSURE_TYPE_OR_NIL(theObjects, NSArray);
    ENSURE_TYPE_OR_NIL(theProperties, NSDictionary);
    
    
    UINavigationBar * ouNB = [navController navigationBar];
    CGRect barFrame = [ouNB bounds];
    NSMutableArray* theItems = [[NSMutableArray alloc] init];
    for (TiProxy* theProxy in theObjects) {
        if (IS_OF_CLASS(theProxy, TiViewProxy) && [(TiViewProxy*)theProxy supportsNavBarPositioning]) {
            [theItems addObject:[(TiViewProxy*)theProxy barButtonItemForRect:barFrame]];
        } else {
            DebugLog(@"%@ does not support nav bar positioning", theProxy);
        }
    }
    
    if ([theItems count] == 0) {
        [theItems release];
        return;
    }
    
    BOOL animated = [TiUtils boolValue:@"animated" properties:theProperties def:NO];
    
    if ([theItems count] > 0) {
        [controller.navigationItem setRightBarButtonItems:theItems animated:animated];
    } else {
        [controller.navigationItem setRightBarButtonItems:nil animated:animated];
    }
    [theItems release];
}

-(void)setRightNavButtons:(id)arg withObject:(id)properties
{
    ENSURE_TYPE_OR_NIL(arg, NSArray);
    ENSURE_TYPE_OR_NIL(properties, NSDictionary);
    
    // detach existing one
//    NSArray* currentButtons = controller.navigationItem.rightBarButtonItems;
//    for (id item in currentButtons) {
//        if ([item respondsToSelector:@selector(proxy)])
//        {
//            TiViewProxy* p = (TiViewProxy*)[item performSelector:@selector(proxy)];
//            if (![arg containsObject:p]) {
//                [p removeBarButtonView];
//            }
//        }
//    }
    
    [self addObjectToHold:arg forKey:@"rightNavButtons" shouldRelayout:NO];
    [self replaceValue:arg forKey:@"rightNavButtons" notification:NO];
    [self replaceValue:properties forKey:@"rightNavSettings" notification:NO];
    TiThreadPerformBlockOnMainThread(^{
        [self refreshRightNavButtons:nil];
    }, NO);
    
}

-(void)setRightNavButtons:(id)value
{
    id firstarg = value;
    id secondarg = [NSDictionary dictionary];
    
    if ([firstarg isKindOfClass:[TiComplexValue class]])
    {
        firstarg = [(TiComplexValue*)value value];
        secondarg = [(TiComplexValue*)value properties];
    }
    [self setRightNavButtons:firstarg withObject:secondarg];
}

-(void)setRightNavButton:(id)proxy withObject:(id)properties
{
//    UIBarButtonItem *item = controller.navigationItem.rightBarButtonItem;
//    if ([item respondsToSelector:@selector(proxy)])
//    {
//        TiViewProxy* p = (TiViewProxy*)[item performSelector:@selector(proxy)];
//        if (p != proxy) {
//            [p removeBarButtonView];
//        }
//    }
    [self replaceValue:proxy forKey:@"rightNavButton" notification:NO];
    if (IS_NULL_OR_NIL(proxy)) {
        [self setRightNavButtons:nil withObject:properties];
    } else {
        [self setRightNavButtons:[NSArray arrayWithObject:proxy] withObject:properties];
    }

}

-(void)setRightNavButton:(id)value
{
    id firstarg = value;
    id secondarg = [NSDictionary dictionary];
    
    if ([firstarg isKindOfClass:[TiComplexValue class]])
    {
        firstarg = [(TiComplexValue*)value value];
        secondarg = [(TiComplexValue*)value properties];
    }
    [self setRightNavButton:firstarg withObject:secondarg];
}

-(void)refreshLeftNavButtons:(id)unused
{
    id navController = [self navControllerForController:controller];
    if ((controller == nil) || navController == nil) {
        return;
    }
    if (!navBarWillShow && [TiUtils boolValue:[self valueForKey:@"navBarHidden"] def:NO]){
    	navButtonNotSet = YES;
    	return;
    }
    NSArray* theObjects = [self holdedProxyForKey:@"leftNavButtons"];
    NSDictionary* theProperties = [self valueForUndefinedKey:@"leftNavSettings"];
    
    ENSURE_TYPE_OR_NIL(theObjects, NSArray);
    ENSURE_TYPE_OR_NIL(theProperties, NSDictionary);
    
    
    UINavigationBar * ouNB = [navController navigationBar];
    CGRect barFrame = [ouNB bounds];
    NSMutableArray* theItems = [[NSMutableArray alloc] init];
    for (TiProxy* theProxy in theObjects) {
        if (IS_OF_CLASS(theProxy, TiViewProxy) && [(TiViewProxy*)theProxy supportsNavBarPositioning]) {
            [theItems addObject:[(TiViewProxy*)theProxy barButtonItemForRect:barFrame]];
        } else {
            DebugLog(@"%@ does not support nav bar positioning", theProxy);
        }
    }
    if ([theItems count] == 0) {
        [theItems release];
        return;
    }
    
    BOOL animated = [TiUtils boolValue:@"animated" properties:theProperties def:NO];
    
    if ([theItems count] > 0) {
        [controller.navigationItem setLeftBarButtonItems:theItems animated:animated];
    } else {
        [controller.navigationItem setLeftBarButtonItems:nil animated:animated];
    }
    [theItems release];
}

-(void)setLeftNavButtons:(id)value
{
    id firstarg = value;
    id secondarg = [NSDictionary dictionary];
    
    if ([firstarg isKindOfClass:[TiComplexValue class]])
    {
        firstarg = [(TiComplexValue*)value value];
        secondarg = [(TiComplexValue*)value properties];
    }
    [self setLeftNavButtons:firstarg withObject:secondarg];
}

-(void)setLeftNavButtons:(id)arg withObject:(id)properties
{
    ENSURE_TYPE_OR_NIL(arg, NSArray);
    ENSURE_TYPE_OR_NIL(properties, NSDictionary);
    
    // detach existing one
//    NSArray* currentButtons = controller.navigationItem.leftBarButtonItems;
//    for (id item in currentButtons) {
//        if ([item respondsToSelector:@selector(proxy)])
//        {
//            TiViewProxy* p = (TiViewProxy*)[item performSelector:@selector(proxy)];
//            if (![arg containsObject:p]) {
//                [p removeBarButtonView];
//            }
//        }
//    }
//    
    [self addObjectToHold:arg forKey:@"leftNavButtons" shouldRelayout:NO];
    [self replaceValue:arg forKey:@"leftNavButtons" notification:NO];
    [self replaceValue:properties forKey:@"leftNavSettings" notification:NO];
    TiThreadPerformBlockOnMainThread(^{
        [self refreshLeftNavButtons:nil];
    }, NO);

}


-(void)setLeftNavButton:(id)value
{
    id firstarg = value;
    id secondarg = [NSDictionary dictionary];
    
    if ([firstarg isKindOfClass:[TiComplexValue class]])
    {
        firstarg = [(TiComplexValue*)value value];
        secondarg = [(TiComplexValue*)value properties];
    }
    [self setLeftNavButton:firstarg withObject:secondarg];
}

-(void)setLeftNavButton:(id)proxy withObject:(id)properties
{
//    UIBarButtonItem *item = controller.navigationItem.leftBarButtonItem;
//    if ([item respondsToSelector:@selector(proxy)])
//    {
//        TiViewProxy* p = (TiViewProxy*)[item performSelector:@selector(proxy)];
//        if (p != proxy) {
//            [p removeBarButtonView];
//        }
//    }
    [self replaceValue:proxy forKey:@"leftNavButton" notification:NO];
    if (IS_NULL_OR_NIL(proxy)) {
        [self setLeftNavButtons:nil withObject:properties];
    } else {
        [self setLeftNavButtons:[NSArray arrayWithObject:proxy] withObject:properties];
    }
}

-(void)setTabBarHidden:(id)value
{
	[self replaceValue:value forKey:@"tabBarHidden" notification:NO];
    TiThreadPerformOnMainThread(^{
        if (controller != nil) {
            [controller setHidesBottomBarWhenPushed:[TiUtils boolValue:value]];
        }
    }, NO);
}

-(void)hideTabBar:(id)value
{
	[self setTabBarHidden:@YES];
}

-(void)showTabBar:(id)value
{
	[self setTabBarHidden:@NO];
}

-(void)refreshBackButton
{
	ENSURE_UI_THREAD_0_ARGS;
	
    id navController = [self navControllerForController:controller];
	if (controller == nil || navController == nil) {
		return; // No need to refresh
	}
	
	NSArray * controllerArray = [[controller navigationController] viewControllers];
	NSUInteger controllerPosition = [controllerArray indexOfObject:controller];
	if ((controllerPosition == 0) || (controllerPosition == NSNotFound))
	{
		return;
	}
    
	UIViewController * prevController = [controllerArray objectAtIndex:controllerPosition-1];
	UIBarButtonItem * backButton = nil;
    

    UIImage * backImage = [TiUtils image:[self valueForKey:@"backButtonTitleImage"] proxy:self];
    if (backImage != nil)
    {
        backButton = [[[UIBarButtonItem alloc] initWithImage:backImage style:UIBarButtonItemStylePlain target:nil action:nil] autorelease];
    }
    else
    {
        NSString * backTitle = [TiUtils stringValue:[self valueForKey:@"backButtonTitle"]];
        if ((backTitle == nil) && [prevController isKindOfClass:[TiViewController class]])
        {
            id tc = [(TiViewController*)prevController proxy];
            backTitle = [TiUtils stringValue:[tc valueForKey:@"title"]];
        }
        if (backTitle != nil)
        {
            backButton = [[[UIBarButtonItem alloc] initWithTitle:backTitle style:UIBarButtonItemStylePlain target:nil action:nil] autorelease];
        }
    }
    
	
	[[prevController navigationItem] setBackBarButtonItem:backButton];
}


-(void)setBackButtonTitle:(id)proxy
{
	ENSURE_UI_THREAD_1_ARG(proxy);
	[self replaceValue:proxy forKey:@"backButtonTitle" notification:NO];
	if (controller!=nil)
	{
		[self refreshBackButton];	//Because this is actually a property of a DIFFERENT view controller,
                                    //we can't attach this until we're in the navbar stack.
	}
}

-(void)setBackButtonTitleImage:(id)proxy
{
	ENSURE_UI_THREAD_1_ARG(proxy);
	[self replaceValue:proxy forKey:@"backButtonTitleImage" notification:NO];
	if (controller!=nil)
	{
		[self refreshBackButton];	//Because this is actually a property of a DIFFERENT view controller,
                                    //we can't attach this until we're in the navbar stack.
	}
}

-(void)updateNavBar
{
    //Called from the view when the screen rotates.
    //Resize titleControl and barImage based on navbar bounds
    id navController = [self navControllerForController:controller];
    if (!shouldUpdateNavBar || controller == nil || navController == nil) {
        return; // No need to update the title if not in a nav controller
    }
    TiThreadPerformOnMainThread(^{
        [self updateTitleView];
    }, NO);
}

-(void)updateTitleView
{
	UIView * newTitleView = nil;
	
    id navController = [self navControllerForController:controller];
	if (!shouldUpdateNavBar || controller == nil || navController == nil) {
		return; // No need to update the title if not in a nav controller
	}
	
    UINavigationItem * ourNavItem = [controller navigationItem];
    UINavigationBar * ourNB = [navController navigationBar];
    CGRect barFrame = [ourNB bounds];
    CGRect availableTitleSize = CGRectZero;
    availableTitleSize.size.width = barFrame.size.width - (2*TI_NAVBAR_BUTTON_WIDTH);
    availableTitleSize.size.height = barFrame.size.height;
    
    //Check for titlePrompt. Ugly hack. Assuming 50% for prompt height.
    if (ourNavItem.prompt != nil) {
        availableTitleSize.size.height /= 2.0f;
        barFrame.origin.y = barFrame.size.height = availableTitleSize.size.height;
    }
    
    
    TiProxy * titleControl = [self holdedProxyForKey:@"titleView"];
    UIView * oldView = [ourNavItem titleView];
    
    if ([oldView isKindOfClass:[TiUIView class]]) {
        TiViewProxy * oldProxy = (TiViewProxy *)[(TiUIView *)oldView proxy];
        if (oldProxy == titleControl) {
            return;
        }
    }
    
	if ([titleControl isKindOfClass:[TiViewProxy class]])
	{
		newTitleView = [(TiViewProxy*)titleControl barButtonViewForRect:availableTitleSize];
	}
	else
	{
		NSURL * path = [TiUtils toURL:[self valueForKey:@"titleImage"] proxy:self];
		//Todo: This should be [TiUtils navBarTitleViewSize] with the thumbnail scaling. For now, however, we'll go with auto.
		UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:path withSize:CGSizeZero];
		if (image!=nil) {
			if ([oldView isKindOfClass:[UIImageView class]]) {
				[(UIImageView*)oldView setImage:image];
				newTitleView = oldView;
			} else {
				newTitleView = [[[UIImageView alloc] initWithImage:image] autorelease];
			}
		}
	}
    
    if (oldView != newTitleView) {
        [ourNavItem setTitleView:newTitleView];
    }
}


-(void)setTitleView:(id)proxy
{
    //	ENSURE_UI_THREAD(setTitleView,proxy);
    [self replaceValue:proxy forKey:@"titleView" notification:NO];
    [self addObjectToHold:proxy forKey:@"titleView"];
    if (controller!=nil)
    {
        TiThreadPerformBlockOnMainThread( ^{
            [self updateTitleView];
        }, NO);
    }
}

-(void)setTitleImage:(id)image
{
	ENSURE_UI_THREAD(setTitleImage,image);
	NSURL *path = [TiUtils toURL:image proxy:self];
	[self replaceValue:[path absoluteString] forKey:@"titleImage" notification:NO];
	if (controller!=nil)
	{
		[self updateTitleView];
	}
}

-(void)setTitle:(NSString*)title_
{
    NSString *title = [TiUtils stringValue:title_];
    [self replaceValue:title forKey:@"title" notification:NO];
    TiThreadPerformOnMainThread(^{
    	id navController = [self navControllerForController:controller];
        if (shouldUpdateNavBar && navController != nil) {
            controller.navigationItem.title = title;
        }
    }, YES);
}

-(void)setTitlePrompt:(NSString*)title_
{
	ENSURE_UI_THREAD(setTitlePrompt,title_);
	NSString *title = [TiUtils stringValue:title_];
	[self replaceValue:title forKey:@"titlePrompt" notification:NO];
    id navController = [self navControllerForController:controller];
	if (navController != nil)
	{
		controller.navigationItem.prompt = title;
	}
}

-(void)setToolbar:(id)items
{
    [self addObjectToHold:items forKey:@"toolbar"];
    [self replaceValue:items forKey:@"toolbar" notification:NO];
}
-(void)setToolbar:(id)items withObject:(id)properties
{
	ENSURE_TYPE_OR_NIL(items,NSArray);
	if (properties == nil)
	{
        properties = [self valueForKey:@"toolbarSettings"];
    }
    else
	{
        [self setValue:properties forKey:@"toolbarSettings"];
    }
	NSArray * theProxies = [self holdedProxyForKey:@"toolbar"];
//	if((id)oldarray == [NSNull null])
//	{
//		oldarray = nil;
//	}
//    
//    for (TiViewProxy *proxy in items)
//    {
//        [self rememberProxy:proxy];
//    }
//    
//	for(TiViewProxy * oldProxy in oldarray)
//	{
//		if(![items containsObject:oldProxy])
//		{
//			[self forgetProxy:oldProxy];
//		}
//	}
	
//	[self replaceValue:items forKey:@"toolbar" notification:NO];
	TiThreadPerformBlockOnMainThread( ^{
        id navController = [self navControllerForController:controller];
		if (shouldUpdateNavBar && navController != nil)
		{
			NSArray *existing = [controller toolbarItems];
            //			UINavigationController * ourNC = navController;
			if (existing!=nil)
			{
				for (id current in existing)
				{
					if ([current respondsToSelector:@selector(proxy)])
					{
						TiViewProxy* p = (TiViewProxy*)[current performSelector:@selector(proxy)];
						[p removeBarButtonView];
					}
				}
			}
            UIToolbar * ouTB = [navController toolbar];
            CGRect barFrame = [ouTB bounds];
			NSMutableArray * array = [[NSMutableArray alloc] initWithObjects:nil];
			for (TiViewProxy *proxy in theProxies)
			{
				if([proxy supportsNavBarPositioning])
				{
					UIBarButtonItem *item = [proxy barButtonItemForRect:barFrame];
					[array addObject:item];
				}
			}
			hasToolbar = (array != nil && [array count] > 0) ? YES : NO ;
			BOOL translucent = [TiUtils boolValue:@"translucent" properties:properties def:YES];
			BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:hasToolbar];
			TiColor* toolbarColor = [TiUtils colorValue:@"barColor" properties:properties];
			UIColor* barColor = [TiUtils barColorForColor:toolbarColor];
			[controller setToolbarItems:array animated:animated];
			[navController setToolbarHidden:(hasToolbar == NO ? YES : NO) animated:animated];
			[[navController toolbar] setTranslucent:translucent];
            
            
            UIColor* tintColor = [[TiUtils colorValue:@"tintColor" properties:properties] color];
            [[navController toolbar] performSelector:@selector(setBarTintColor:) withObject:barColor];
            if (tintColor) {
                [[navController toolbar] setTintColor:tintColor];
            }

			[array release];
			
		}
	},YES);
	
}

-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    id navController = [self navControllerForController:controller];
    if (navController) {
        CGRect frame = [navController navigationBar].frame;
        startingNavbarFrame.size = [navController navigationBar].frame.size;
        startingToolbarFrame.size = [navController toolbar].frame.size;
    }
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

-(void)setupWindowDecorations:(BOOL)animated
{
    id navController = [self navControllerForController:controller];
    if ((controller == nil) || navController == nil) {
        return;
    }
    _setingUpWindowDecorations = YES;
    
    [navController setToolbarHidden:!hasToolbar animated:animated];
    
    //Need to clear title for titleAttributes to apply correctly on iOS6.
    SETPROP(@"titleAttributes",setTitleAttributes);
    SETPROP(@"title",setTitle);
    SETPROP(@"titlePrompt",setTitlePrompt);
    [self updateTitleView];
    SETPROP(@"barColor",setBarColor);
    SETPROP(@"navTintColor",setNavTintColor);
    SETPROP(@"translucent",setTranslucent);
    SETPROP(@"barStyle",setBarStyle);
    SETPROP(@"tabBarHidden",setTabBarHidden);
    SETPROPOBJ(@"toolbar",setToolbar);
    [self updateBarImage];
    [self updateNavButtons];
    [self refreshBackButton];

    if (animated) {
        [UIView beginAnimations:@"navbarAnim" context:NULL];
        [UIView setAnimationBeginsFromCurrentState:YES];
    }
    
    BOOL navBarHidden = [TiUtils boolValue:[self valueForKey:@"navBarHidden"] def:[navController isNavigationBarHidden]];
    if (navBarHidden != [navController isNavigationBarHidden]) {
        if (navBarHidden) {
            [self hideNavBar:@[@{@"animated":@(NO)}]];
        }
        else {
            [self showNavBar:@[@{@"animated":@(NO)}]];
        }
    }
    
    if ((isModal || self.isManaged)) {
        [(TiRootViewController*)[[TiApp app] controller] updateStatusBar:animated];
    }
    
    if (navController) {
        startingNavbarFrame = [navController navigationBar].frame;
        startingToolbarFrame = [navController toolbar].frame;
    }

    
    SETPROP(@"barDeltaY",setBarDeltaY);
    SETPROP(@"toolbarDeltaY",setToolbarDeltaY);
    if (animated) {
        [UIView commitAnimations];
    }
    
    _setingUpWindowDecorations = NO;
    if (self.tab) {
        [self.tab windowSetUpDecoration:self animated:animated];
    }
}

-(void)cleanupWindowDecorations
{
    id navController = [self navControllerForController:controller];
    if ((controller == nil) || (navController == nil)) {
        return;
    }
    
    NSMutableArray* items = [NSMutableArray array];
    if (controller.navigationItem.leftBarButtonItem) {
        [items addObject:controller.navigationItem.leftBarButtonItem];
    }
    if (controller.navigationItem.rightBarButtonItem) {
        [items addObject:controller.navigationItem.rightBarButtonItem];
    }
    if (controller.navigationItem.leftBarButtonItems) {
        [items addObjectsFromArray:controller.navigationItem.leftBarButtonItems];
    }
    if (controller.navigationItem.rightBarButtonItems) {
        [items addObjectsFromArray:controller.navigationItem.rightBarButtonItems];
    }
    for (UIBarButtonItem* item in items) {
        TiViewProxy* p = nil;
        if ([item respondsToSelector:@selector(proxy)])
        {
            p = (TiViewProxy*)[item performSelector:@selector(proxy)];
        }
        else if ([[item customView] respondsToSelector:@selector(proxy)]){
            p = (TiViewProxy*)[[item customView] performSelector:@selector(proxy)];
        }
        if (p)
        {
            [p removeBarButtonView];
            [p windowDidClose];
            [self forgetProxy:p];
        }
    }
    controller.navigationItem.leftBarButtonItem = controller.navigationItem.rightBarButtonItem = nil;
    controller.navigationItem.leftBarButtonItems = controller.navigationItem.rightBarButtonItems = nil;
    if (barImageView != nil) {
        [barImageView removeFromSuperview];
    }
}

-(void)rootViewDidForceFrame:(NSNotification *)notification
{
    if ([self focussed] && opened) {
        id navController = [self navControllerForController:controller];
        if ((controller == nil) || (navController == nil)) {
            return;
        }
        
        BOOL isHidden = [navController isNavigationBarHidden];
        [navController setNavigationBarHidden:!isHidden animated:NO];
        //we need the small duration animation or it wont get updated
        [UIView animateWithDuration:0.001 animations:^{
            [[navController view] setFrame:[navController view].frame];
            [navController setNavigationBarHidden:isHidden animated:NO];
        }];
    }
}
@end

#endif
