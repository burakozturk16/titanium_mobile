/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiRootViewController.h"
#import "TiUtils.h"
#import "TiApp.h"
#import "TiLayoutQueue.h"
#import "TiErrorController.h"
#import "TiViewProxy.h"

#ifdef FORCE_WITH_MODAL
@interface ForcingController: UIViewController {
@private
    TiOrientationFlags orientationFlags;
    UIInterfaceOrientation supportedOrientation;
}
-(void)setOrientation:(UIInterfaceOrientation)newOrientation;
@end

@implementation ForcingController
-(void)setOrientation:(UIInterfaceOrientation)newOrientation
{
    supportedOrientation = newOrientation;
    orientationFlags = TiOrientationNone;
    TI_ORIENTATION_SET(orientationFlags, supportedOrientation);
}
- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
    return (toInterfaceOrientation == supportedOrientation);
}

// New Autorotation support.
- (BOOL)shouldAutorotate{
    return YES;
}
- (NSUInteger)supportedInterfaceOrientations
{
    return orientationFlags;
}
// Returns interface orientation masks.
- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation
{
    return supportedOrientation;
}

@end
#endif

@interface TiRootViewNeue : UIView
@end

@implementation TiRootViewNeue

- (void)motionEnded:(UIEventSubtype)motion withEvent:(UIEvent *)event
{
	if (event.type == UIEventTypeMotion && event.subtype == UIEventSubtypeMotionShake)
	{
        [[NSNotificationCenter defaultCenter] postNotificationName:kTiGestureShakeNotification object:event];
    }
}

- (BOOL)canBecomeFirstResponder
{
	return YES;
}

@end

@interface TiRootViewController (notifications_internal)
-(void)didOrientNotify:(NSNotification *)notification;
-(void)keyboardWillHide:(NSNotification*)notification;
-(void)keyboardWillShow:(NSNotification*)notification;
-(void)keyboardDidHide:(NSNotification*)notification;
-(void)keyboardDidShow:(NSNotification*)notification;
-(void)adjustFrameForUpSideDownOrientation:(NSNotification*)notification;
@end

@interface TiRootViewController()
@property (nonatomic, retain) UIView* keyboardActiveInput;
@end

@implementation TiRootViewController
{
    //Keyboard stuff
    BOOL _rotating;
    BOOL _willShowKeyboard;
    BOOL _willHideKeyboard;
    BOOL keyboardVisible;	//If false, use enterCurve. If true, use leaveCurve.
	
    CGRect startFrame;		//Where the keyboard was before the handling
    CGRect endFrame;		//Where the keyboard will be after the handling
    UIViewAnimationCurve enterCurve;
    CGFloat enterDuration;
    UIViewAnimationCurve leaveCurve;
    CGFloat leaveDuration;
}

@synthesize keyboardFocusedProxy = keyboardFocusedProxy;
@synthesize statusBarVisibilityChanged;
@synthesize statusBarInitiallyHidden;
@synthesize defaultStatusBarStyle;
@synthesize keyboardActiveInput;
-(void)dealloc
{
	RELEASE_TO_NIL(bgColor);
	RELEASE_TO_NIL(bgImage);
    RELEASE_TO_NIL(containedWindows);
    RELEASE_TO_NIL(modalWindows);
    RELEASE_TO_NIL(hostView);
    
	WARN_IF_BACKGROUND_THREAD;	//NSNotificationCenter is not threadsafe!
	NSNotificationCenter * nc = [NSNotificationCenter defaultCenter];
	[nc removeObserver:self];
	[super dealloc];
}

- (id) init
{
    self = [super init];
    if (self != nil) {
        _rotating = NO;
        forcingRotation = NO;
//        forcedOrientation = NO;
        deviceOrientation = [[UIDevice currentDevice] orientation];
        orientationHistory[0] = UIInterfaceOrientationPortrait;
        orientationHistory[1] = UIInterfaceOrientationLandscapeLeft;
        orientationHistory[2] = UIInterfaceOrientationLandscapeRight;
        orientationHistory[3] = UIInterfaceOrientationPortraitUpsideDown;
		
        //Keyboard initialization
        leaveCurve = UIViewAnimationCurveEaseIn;
        enterCurve = UIViewAnimationCurveEaseIn;
        leaveDuration = 0.3;
        enterDuration = 0.3;
        curTransformAngle = 0;
        
        defaultOrientations = TiOrientationNone;
        containedWindows = [[NSMutableArray alloc] init];
        modalWindows = [[NSMutableArray alloc] init];
        /*
         *	Default image view -- Since this goes away after startup, it's made here and
         *	nowhere else. We don't do this during loadView because it's possible that
         *	the view will be unloaded (by, perhaps a Memory warning while a modal view
         *	controller and loaded at a later time.
         */
        defaultImageView = [[UIImageView alloc] init];
        [defaultImageView setAutoresizingMask:UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth];
        [defaultImageView setContentMode:UIViewContentModeScaleToFill];
		
        [self processInfoPlist];
        
        //Notifications
        WARN_IF_BACKGROUND_THREAD;	//NSNotificationCenter is not threadsafe!
        NSNotificationCenter * nc = [NSNotificationCenter defaultCenter];
        [nc addObserver:self selector:@selector(didOrientNotify:) name:UIDeviceOrientationDidChangeNotification object:nil];
        [nc addObserver:self selector:@selector(keyboardDidHide:) name:UIKeyboardDidHideNotification object:nil];
        [nc addObserver:self selector:@selector(keyboardDidShow:) name:UIKeyboardDidShowNotification object:nil];
        [nc addObserver:self selector:@selector(keyboardWillHide:) name:UIKeyboardWillHideNotification object:nil];
        [nc addObserver:self selector:@selector(keyboardWillShow:) name:UIKeyboardWillShowNotification object:nil];
        [nc addObserver:self selector:@selector(adjustFrameForUpSideDownOrientation:) name:UIApplicationDidChangeStatusBarFrameNotification object:nil];
        // Register for text input notifications
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(responderDidBecomeActive:)
                                                     name:UITextFieldTextDidBeginEditingNotification
                                                   object:nil];
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(responderDidBecomeActive:)
                                                     name:UITextViewTextDidBeginEditingNotification
                                                   object:nil];
        [[UIDevice currentDevice] beginGeneratingDeviceOrientationNotifications];
    }
    return self;
}

-(UIStatusBarStyle)styleFromString:(NSString*)theString
{
    if (!IS_NULL_OR_NIL(theString)) {
        if ([theString isEqualToString:@"UIStatusBarStyleDefault"]) {
            return UIStatusBarStyleDefault;
        } else if ([theString isEqualToString:@"UIStatusBarStyleBlackTranslucent"] || [theString isEqualToString:@"UIStatusBarStyleLightContent"] || [theString isEqualToString:@"UIStatusBarStyleBlackOpaque"]) {
            return UIStatusBarStyleLightContent;
        }
    }
    return UIStatusBarStyleDefault;
}

-(void)processInfoPlist
{
    //read the default orientations
    [self getDefaultOrientations];
    
    //read the default value of UIStatusBarHidden
    id statHidden = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIStatusBarHidden"];
    statusBarInitiallyHidden = [TiUtils boolValue:statHidden];
    //read the value of UIViewControllerBasedStatusBarAppearance
    id vcbasedStatHidden = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIViewControllerBasedStatusBarAppearance"];
    viewControllerControlsStatusBar = [TiUtils boolValue:vcbasedStatHidden def:YES];
    //read the value of statusBarStyle
    id statusStyle = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UIStatusBarStyle"];
    defaultStatusBarStyle = [self styleFromString:statusStyle];
    
}

-(void)loadView
{
    TiRootViewNeue *rootView = [[TiRootViewNeue alloc] initWithFrame:[TiUtils frameForController:self]];
    self.view = rootView;
    rootView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
    [self updateBackground];
    
    UIView* theHost = nil;
    
    if ([TiUtils isIOS8OrGreater]) {
        hostView = [[UIView alloc] initWithFrame:[rootView bounds]];
        hostView.backgroundColor = [UIColor clearColor];
        hostView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
        [rootView addSubview:hostView];
        theHost = hostView;
    } else {
        theHost = rootView;
    }
    
    if (defaultImageView != nil) {
        [self rotateDefaultImageViewToOrientation:[[UIApplication sharedApplication] statusBarOrientation]];
        [theHost addSubview:defaultImageView];
    }
    [rootView becomeFirstResponder];
    [rootView release];
}



#pragma mark - TiRootControllerProtocol
//Background Control
-(void)updateBackground
{
	UIView * ourView = [self view];
	UIColor * chosenColor = (bgColor==nil)?[UIColor blackColor]:bgColor;
	[ourView setBackgroundColor:chosenColor];
	[[ourView superview] setBackgroundColor:chosenColor];
	if (bgImage!=nil)
	{
		[[ourView layer] setContents:(id)bgImage.CGImage];
	}
	else
	{
		[[ourView layer] setContents:nil];
	}
}

-(void)setBackgroundImage:(UIImage*)newImage
{
    if ((newImage == bgImage) || [bgImage isEqual:newImage]) {
        return;
    }
    [bgImage release];
	bgImage = [newImage retain];
	TiThreadPerformOnMainThread(^{[self updateBackground];}, NO);
}

-(void)setBackgroundColor:(UIColor*)newColor
{
    if ((newColor == bgColor) || [bgColor isEqual:newColor]) {
        return;
    }
    [bgColor release];
	bgColor = [newColor retain];
	TiThreadPerformOnMainThread(^{[self updateBackground];}, NO);
}

-(void)dismissDefaultImage
{
    if (defaultImageView != nil) {
        [defaultImageView setHidden:YES];
        [defaultImageView removeFromSuperview];
        RELEASE_TO_NIL(defaultImageView);
    }
}

+ (NSString *)splashImageNameForOrientation:(UIDeviceOrientation)orientation
{
    CGSize viewSize = [UIScreen mainScreen].bounds.size;
    
    NSString *viewOrientation = @"Portrait";
    
    if (UIDeviceOrientationIsLandscape(orientation))
    {
        viewSize = CGSizeMake(viewSize.height, viewSize.width);
        viewOrientation = @"Landscape";
    }
    
    NSArray* imagesDict = [[[NSBundle mainBundle] infoDictionary] valueForKey:@"UILaunchImages"];
    
    for (NSDictionary *dict in imagesDict)
    {
        CGSize imageSize = CGSizeFromString(dict[@"UILaunchImageSize"]);
        if (CGSizeEqualToSize(imageSize, viewSize) && [viewOrientation isEqualToString:dict[@"UILaunchImageOrientation"]])
            return dict[@"UILaunchImageName"];
    }
    return nil;
}

+ (UIImage*)splashImageForOrientation:(UIDeviceOrientation)orientation
{
    NSString *imageName = [TiRootViewController splashImageNameForOrientation:orientation];
    UIImage *image = [UIImage imageNamed:imageName];
    return image;
}


-(void)rotateDefaultImageViewToOrientation: (UIInterfaceOrientation )newOrientation;
{
    if (defaultImageView == nil)
    {
        return;
    }
    /*
     *	This code could stand for some refinement, but it is rarely called during
     *	an application's lifetime and is meant to recreate the quirks and edge cases
     *	that iOS uses during application startup, including Apple's own
     *	inconsistencies between iPad and iPhone.
     */
    
    UIImage * defaultImage = [TiRootViewController splashImageForOrientation:newOrientation];
    
    CGFloat imageScale = [defaultImage scale];
    CGRect newFrame = [[self view] bounds];
    CGSize imageSize = [defaultImage size];
    
    if(imageSize.width == newFrame.size.width)
    {
        CGFloat overheight;
        overheight = imageSize.height - newFrame.size.height;
        if (overheight > 0.0) {
            newFrame.origin.y -= overheight;
            newFrame.size.height += overheight;
        }
    }
    [defaultImageView setImage:defaultImage];
    [defaultImageView setFrame:newFrame];
}

#pragma mark - Keyboard Control

-(void)dismissKeyboard
{
    if (self.keyboardActiveInput) {
        [self.keyboardActiveInput endEditing:YES];
    }
}

-(void)dismissKeyboardFromWindow:(id<TiWindowProtocol>)theWindow
{
    if (self.keyboardActiveInput && [[self.keyboardActiveInput superview] isKindOfClass:[TiUIView class]]) {
        TiProxy* keyboardInputProxy = [(TiUIView*)[self.keyboardActiveInput superview] proxy];
        if (keyboardInputProxy && [theWindow isKindOfClass:[TiParentingProxy class]] &&
            ![(TiParentingProxy*)theWindow containsChild:keyboardInputProxy])
        {
            return;
        }
    }
    [self dismissKeyboard];
}

-(BOOL)keyboardVisible
{
    return (keyboardVisible && !_willHideKeyboard) || _willShowKeyboard;
}

- (void)keyboardWillHide:(NSNotification*)notification
{
	NSDictionary *userInfo = [notification userInfo];
	leaveCurve = [[userInfo valueForKey:UIKeyboardAnimationCurveUserInfoKey] intValue];
	leaveDuration = [[userInfo valueForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
	[self extractKeyboardInfo:userInfo];
    
    if (_willHideKeyboard) return;
    _willHideKeyboard = YES;
    
    [self handleNewNewKeyboardStatus];
    if (_rotating) return;
    TiViewProxy* topWindow = [self topWindow];
    if ([topWindow _hasListeners:@"keyboard"]) {
        CGRect endingFrame = endFrame;
        NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                               [NSNumber numberWithInt:(keyboardVisible?leaveDuration:enterDuration)*1000], @"animationDuration",
                               [NSNumber numberWithBool:keyboardVisible], @"keyboardVisible",
                               [TiUtils rectToDictionary:endingFrame], @"keyboardFrame",
                               nil];
        [topWindow fireEvent:@"keyboard" withObject:event];
    }
}

- (void)keyboardWillShow:(NSNotification*)notification
{
	NSDictionary *userInfo = [notification userInfo];
	enterCurve = [[userInfo valueForKey:UIKeyboardAnimationCurveUserInfoKey] intValue];
	enterDuration = [[userInfo valueForKey:UIKeyboardAnimationDurationUserInfoKey] floatValue];
	[self extractKeyboardInfo:userInfo];
    
    if (_willShowKeyboard) return;
    _willShowKeyboard = YES;
    
    [self handleNewNewKeyboardStatus];
    
    if (_rotating) return;
    TiViewProxy* topWindow = [self topWindow];
    if ([topWindow _hasListeners:@"keyboard"]) {
        CGRect startingFrame = startFrame;
        NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:
                               [NSNumber numberWithInt:(keyboardVisible?leaveDuration:enterDuration)*1000], @"animationDuration",
                               [NSNumber numberWithBool:keyboardVisible], @"keyboardVisible",
                               [TiUtils rectToDictionary:startingFrame], @"keyboardFrame",
                               nil];
        [topWindow fireEvent:@"keyboard" withObject:event];
    }
}

- (void)keyboardDidHide:(NSNotification*)notification
{
    keyboardVisible = NO;
    _willHideKeyboard = NO;
    if (!_rotating) {
        self.keyboardActiveInput = nil;
    }
}

- (void)keyboardDidShow:(NSNotification*)notification
{
    keyboardVisible = YES;
    _willShowKeyboard = NO;
}

-(UIView *)viewForKeyboardAccessory;
{
    return [[[[TiApp app] window] subviews] lastObject];
}

- (void)responderDidBecomeActive:(NSNotification *)notification
{
    // Grab the active input, it will be used to find the keyboard view later on
    self.keyboardActiveInput = notification.object;
    if (self.keyboardActiveInput.inputAccessoryView)
    {
        UIView* accessory = self.keyboardActiveInput.inputAccessoryView;
        if ([accessory isKindOfClass:[TiUIView class]]) {
            [[((TiUIView*)accessory) viewProxy] refreshView];
            //for now make sure we are origin (0,0)
            //x wont matter as it will be set to 0
            //but y needs to be set as it is seen as an y decale
            CGRect frame = accessory.frame;
            frame.origin.y = 0;
            accessory.frame = frame;
        }
    }
    [self handleNewNewKeyboardStatus];
}

- (TiUIView *)keyboardAccessoryView
{
    TiUIView* result = nil;
    if (self.keyboardActiveInput && [[self.keyboardActiveInput superview] isKindOfClass:[TiUIView class]]) {
        result = (TiUIView*)[self.keyboardActiveInput superview];
		UIView * ourView = result;
        while (ourView != nil)
		{
			if (ourView == self.keyboardActiveInput.inputAccessoryView)
			{
				//We found a match!
				return nil;
			}
			ourView = [ourView superview];
		}
    }
	return result;
}

-(void)extractKeyboardInfo:(NSDictionary *)userInfo
{
    NSValue *v = nil;
    v = [userInfo valueForKey:UIKeyboardFrameEndUserInfoKey];
	
    if (v != nil) {
        endFrame = [self getAbsRect:[v CGRectValue] fromView:nil];
    }
    
    v = [userInfo valueForKey:UIKeyboardFrameBeginUserInfoKey];
    
    if (v != nil) {
        startFrame = [self getAbsRect:[v CGRectValue] fromView:nil];
    }
}

-(CGRect)getKeyboardFrameInView:(UIView*)view {
    CGRect frame = [self currentKeyboardFrame];
    if (!CGRectIsEmpty(frame)) {
        return [[self viewForKeyboardAccessory] convertRect:frame toView:view];
    }
    return frame;
}

-(CGRect)currentKeyboardFrame
{
    CGRect result = CGRectZero;
    if ([self keyboardVisible]) {
        result = endFrame;
    }
    return result;
}

-(CGRect)getAbsRect:(CGRect)rect fromView:(UIView*)view
{
    return [[self viewForKeyboardAccessory] convertRect:rect fromView:view];
}

-(CGFloat)keyboardHeight
{
    if ([self keyboardVisible]) {
        CGFloat keyboardHeight = endFrame.origin.y;
        TiViewProxy* topWindow = [self topWindow];
        if ([topWindow valueForKey:@"keyboardOffset"]) {
            keyboardHeight -= [TiUtils floatValue:[topWindow valueForKey:@"keyboardOffset"] def:0.0f];
        }
        return keyboardHeight;
    }
    return 0;
}

-(void) handleNewNewKeyboardStatus
{
    NSMutableDictionary* data = [NSMutableDictionary dictionaryWithObject:@([self keyboardHeight]) forKey:@"keyboardHeight"];
    if (self.keyboardActiveInput) {
        [data setObject:self.keyboardActiveInput forKey:@"inputView"];
        if (self.keyboardActiveInput.inputAccessoryView) {
            [data setObject:self.keyboardActiveInput.inputAccessoryView forKey:@"accessoryView"];
        }
    }
    [[NSNotificationCenter defaultCenter] postNotificationName:kTiKeyboardHeightChangedNotification object:data];
}


-(UIView *)topWindowProxyView
{
    TiViewProxy* topProxy = [self topWindow];
    if (topProxy) return [topProxy view];
    return [self view];
}

-(TiViewProxy *)topWindow
{
    UIViewController* topVC = [self topPresentedController];
    //handle the case of UINavigationController
    if ([topVC respondsToSelector:@selector(topViewController)]) {
        topVC = [(id)topVC topViewController];
    }
    if ([topVC isKindOfClass:[TiErrorController class]]) {
        DebugLog(@"[ERROR] ErrorController is up");
        return nil;
    }
    if (topVC != self && [topVC respondsToSelector:@selector(proxy)]) {
        id theProxy = [(id)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            return [(id<TiWindowProtocol>)theProxy topWindow];
        }
    }
    
    if ([modalWindows count] > 0) {
        @synchronized(modalWindows) {
            return [[modalWindows lastObject] topWindow];
        }
    } else if ([containedWindows count] > 0) {
        @synchronized(containedWindows) {
            return [[containedWindows lastObject] topWindow];
        }
    } else {
        return nil;
    }
}


#if defined(DEBUG) || defined(DEVELOPER)
-(void)shutdownUi:(id)arg
{
    //FIRST DISMISS ALL MODAL WINDOWS
    UIViewController* topVC = [self topPresentedController];
    if (topVC != self) {
        UIViewController* presenter = [topVC presentingViewController];
        [presenter dismissViewControllerAnimated:NO completion:^{
            [self shutdownUi:arg];
        }];
        return;
    }
    NSArray* modalCopy;
    NSArray* windowCopy;
    //At this point all modal stuff is done. Go ahead and clean up proxies.
    @synchronized(modalWindows) {
        modalCopy = [modalWindows copy];
    }
    @synchronized(containedWindows) {
        windowCopy = [containedWindows copy];
    }
    
    if(modalCopy != nil) {
        for (TiViewProxy* theWindow in [modalCopy reverseObjectEnumerator]) {
            [theWindow windowWillClose];
            [theWindow windowDidClose];
        }
        [modalCopy release];
    }
    if (windowCopy != nil) {
        for (TiViewProxy* theWindow in [windowCopy reverseObjectEnumerator]) {
            [theWindow windowWillClose];
            [theWindow windowDidClose];
        }
        [windowCopy release];
    }
    
    DebugLog(@"[INFO] UI SHUTDOWN COMPLETE. TRYING TO RESUME RESTART");
    if ([arg respondsToSelector:@selector(_resumeRestart:)]) {
        [arg performSelector:@selector(_resumeRestart:) withObject:nil];
    } else {
        DebugLog(@"[WARN] Could not resume. No selector _resumeRestart: found for arg");
    }
}
#endif

#pragma mark - TiControllerContainment
-(BOOL)canHostWindows
{
    return YES;
}

-(UIView *)hostingView
{
    if ([self canHostWindows] && [self isViewLoaded]) {
        if ([TiUtils isIOS8OrGreater]) {
            return hostView;
        } else {
            return self.view;
        }
    } else {
        return nil;
    }
}

-(void)willOpenWindow:(id<TiWindowProtocol>)theWindow
{
    [self dismissKeyboardFromWindow:theWindow];
    @synchronized(containedWindows) {
        if ([containedWindows lastObject] != theWindow) {
            [[containedWindows lastObject] resignFocus];
        }
    }
    if ([theWindow isModal]) {
        @synchronized(modalWindows) {
            if (![modalWindows containsObject:theWindow]) {
                [modalWindows addObject:theWindow];
            }
        }
    } else {
        @synchronized(containedWindows) {
            if (![containedWindows containsObject:theWindow]) {
                [containedWindows addObject:theWindow];
                theWindow.parentOrientationController = self;
            }
        }
        if (isCurrentlyVisible) {
            [theWindow viewWillAppear:NO];
        }
        if ([self presentedViewController] == nil ||
            ([TiUtils isIOS8OrGreater] && [[self presentedViewController] isKindOfClass:[UIAlertController class]])) {
            [self childOrientationControllerChangedFlags:theWindow];
        }
    }
}

-(void)didOpenWindow:(id<TiWindowProtocol>)theWindow
{
    [self dismissKeyboardFromWindow:theWindow];
    if ([self presentedViewController] == nil ||
        ([TiUtils isIOS8OrGreater] && [[self presentedViewController] isKindOfClass:[UIAlertController class]])) {
        [self childOrientationControllerChangedFlags:theWindow];
        [theWindow gainFocus];
    }
    if (![theWindow isManaged]) {
        [self dismissDefaultImage];
    }
}

-(void)willCloseWindow:(id<TiWindowProtocol>)theWindow
{
    [self dismissKeyboardFromWindow:theWindow];
    [theWindow resignFocus];
    if ([theWindow isModal]) {
        @synchronized(modalWindows) {
            [modalWindows removeObject:theWindow];
        }
    } else {
        @synchronized(containedWindows) {
            [containedWindows removeObject:theWindow];
        }
        theWindow.parentOrientationController = nil;
        if ([self presentedViewController] == nil ||
            ([TiUtils isIOS8OrGreater] && [[self presentedViewController] isKindOfClass:[UIAlertController class]])) {
            [self childOrientationControllerChangedFlags:[containedWindows lastObject]];
        }
    }
}

-(void)didCloseWindow:(id<TiWindowProtocol>)theWindow
{
    [self dismissKeyboardFromWindow:theWindow];
    if ([self presentedViewController] == nil ||
        ([TiUtils isIOS8OrGreater] && [[self presentedViewController] isKindOfClass:[UIAlertController class]])) {
        @synchronized(containedWindows) {
            [[containedWindows lastObject] gainFocus];
        }
    }
}

@class TiViewController;

-(void)showControllerModal:(UIViewController*)theController animated:(BOOL)animated
{
    BOOL trulyAnimated = animated;
    UIViewController* topVC = [self topPresentedController];

    
    if ([topVC isKindOfClass:[TiErrorController class]]) {
        DebugLog(@"[ERROR] ErrorController is up. ABORTING showing of modal controller");
        return;
    }
    if ([TiUtils isIOS8OrGreater]) {
        if ([topVC isKindOfClass:[UIAlertController class]]) {
            if (((UIAlertController*)topVC).preferredStyle == UIAlertControllerStyleAlert ) {
                trulyAnimated = NO;
                if (![theController isKindOfClass:[TiErrorController class]]) {
                    DebugLog(@"[ERROR] UIAlertController is up and showing an alert. ABORTING showing of modal controller");
                    return;
                }
            }
        }
    }
    if (topVC == self) {
        @synchronized(containedWindows) {
            [[containedWindows lastObject] resignFocus];
        }
    } else if ([topVC respondsToSelector:@selector(proxy)]) {
        id theProxy = [(id)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            [(id<TiWindowProtocol>)theProxy resignFocus];
        }
    }
    [self dismissKeyboard];
    [topVC presentViewController:theController animated:trulyAnimated completion:nil];
}

-(void)hideControllerModal:(UIViewController*)theController animated:(BOOL)animated
{
    BOOL trulyAnimated = animated;
    UIViewController* presenter = [theController presentingViewController];
    
    if ([TiUtils isIOS8OrGreater]) {
        if ([presenter isKindOfClass:[UIAlertController class]]) {
            if (((UIAlertController*)presenter).preferredStyle == UIAlertControllerStyleAlert ) {
                trulyAnimated = NO;
            }
        }
    }
    [presenter dismissViewControllerAnimated:trulyAnimated completion:^{
        if (presenter == self) {
            if ([theController respondsToSelector:@selector(proxy)]) {
                id theProxy = [(id)theController proxy];
                [self didCloseWindow:theProxy];
                
                //sometimes the keyboard doesn't show if we are trying to show it as
                //we close a modal window
                if (keyboardFocusedProxy && [theProxy isKindOfClass:[TiParentingProxy class]] &&
                    ![(TiParentingProxy*)theProxy containsChild:keyboardFocusedProxy])
                {
                    [keyboardFocusedProxy focus:nil];
                }
            }
            else {
                [self didCloseWindow:nil];
            }
        } else {

            if ([presenter respondsToSelector:@selector(proxy)]) {
                id theProxy = [(id)presenter proxy];
                [self dismissKeyboardFromWindow:theProxy];
                if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
                    [(id<TiWindowProtocol>)theProxy gainFocus];
                }
            } else if ([TiUtils isIOS8OrGreater]){
                //This code block will only execute when errorController is presented on top of an alert
                if ([presenter isKindOfClass:[UIAlertController class]] && (((UIAlertController*)presenter).preferredStyle == UIAlertControllerStyleAlert)) {
                    UIViewController* alertPresenter = [presenter presentingViewController];
                    [alertPresenter dismissViewControllerAnimated:NO completion:^{
                        [alertPresenter presentViewController:presenter animated:NO completion:nil];
                    }];
                }
            }
        }
    }];
}


#pragma mark - Orientation Control
-(TiOrientationFlags)getDefaultOrientations
{
    if (defaultOrientations == TiOrientationNone) {
        // Read the orientation values from the plist - if they exist.
        NSArray* orientations = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UISupportedInterfaceOrientations"];
        TiOrientationFlags defaultFlags = TiOrientationPortrait;
        
        if ([TiUtils isIPad]) {
            defaultFlags = TiOrientationAny;
            NSArray * ipadOrientations = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"UISupportedInterfaceOrientations~ipad"];
            if ([ipadOrientations respondsToSelector:@selector(count)] && ([ipadOrientations count] > 0)) {
                orientations = ipadOrientations;
            }
        }
        
        if ([orientations respondsToSelector:@selector(count)] && ([orientations count] > 0)) {
            defaultFlags = TiOrientationNone;
            for (NSString* orientationString in orientations) {
                UIInterfaceOrientation orientation = (UIInterfaceOrientation)[TiUtils orientationValue:orientationString def:-1];
                switch (orientation) {
                    case UIInterfaceOrientationLandscapeLeft:
                    case UIInterfaceOrientationLandscapeRight:
                    case UIInterfaceOrientationPortrait:
                    case UIInterfaceOrientationPortraitUpsideDown:
                        TI_ORIENTATION_SET(defaultFlags, orientation);
                        break;
                        
                    default:
                        break;
                }
            }
        }
        defaultOrientations = defaultFlags;
    }
	
	return defaultOrientations;
}

-(UIViewController*)topPresentedControllerCheckingPopover:(BOOL)checkPopover
{
    UIViewController* topmostController = self;
    UIViewController* presentedViewController = nil;
    while ( topmostController != nil ) {
        presentedViewController = [topmostController presentedViewController];
        if ((presentedViewController != nil) && checkPopover && [TiUtils isIOS8OrGreater]) {
            if (presentedViewController.modalPresentationStyle == UIModalPresentationPopover ||
                [presentedViewController isKindOfClass:[UIAlertController class]]) {
                break;
            }
        }
        if ([presentedViewController isBeingDismissed]) {
            presentedViewController = [presentedViewController presentingViewController];
        }
        if (!presentedViewController || topmostController == presentedViewController) {
            break;
        }
        topmostController = presentedViewController;
    }
    return topmostController;
}

-(UIViewController*)topPresentedController
{
    return [self topPresentedControllerCheckingPopover:NO];
}

-(UIViewController<TiControllerContainment>*)topContainerController;
{
    UIViewController* topmostController = self;
    UIViewController* presentedViewController = nil;
    UIViewController<TiControllerContainment>* result = nil;
    UIViewController<TiControllerContainment>* match = nil;
    while (topmostController != nil) {
        if ([topmostController conformsToProtocol:@protocol(TiControllerContainment)]) {
            match = (UIViewController<TiControllerContainment>*)topmostController;
            if ([match canHostWindows]) {
                result = match;
            }
        }
        presentedViewController = [topmostController presentedViewController];
        if (presentedViewController != nil) {
            topmostController = presentedViewController;
            presentedViewController = nil;
        }
        else {
            break;
        }
    }
    
    return result;
}

-(CGRect)resizeView
{
//    CGRect rect = [TiUtils frameForController:self];
//    if (_forcedOrientation) {
//        switch ([[UIApplication sharedApplication] statusBarOrientation])
//        {
//            case UIInterfaceOrientationLandscapeLeft:
//            case UIInterfaceOrientationLandscapeRight:
//            {
//                CGFloat leftMargin = rect.origin.y;
//                CGFloat topMargin = rect.origin.x;
//                CGFloat newHeight = rect.size.width;
//                CGFloat newWidth = rect.size.height;
//                rect = CGRectMake(leftMargin, topMargin, newWidth, newHeight);
//                break;
//            }
//            default: {
//                break;
//            }
//        }
//    }
    CGRect rect = [TiUtils frameForController:self];
    [[self view] setFrame:rect];
    return [[self view]bounds];
    
//    [[self hostingView] setFrame:rect];
//    return [[self hostingView] bounds];
}

-(void)repositionSubviews
{
    //Since the window relayout is now driven from viewDidLayoutSubviews
    //this is not required. Leaving it in place in case someone is using it now.
    /*
    for (id<TiWindowProtocol> thisWindow in [containedWindows reverseObjectEnumerator]) {
        [TiLayoutQueue layoutProxy:(TiViewProxy*)thisWindow];
    }
    */
}

-(UIInterfaceOrientation) lastValidOrientation:(TiOrientationFlags)orientationFlags
{
    if (TI_ORIENTATION_ALLOWED(orientationFlags,deviceOrientation)) {
        return deviceOrientation;
    }
    for (int i = 0; i<4; i++) {
        if (TI_ORIENTATION_ALLOWED(orientationFlags,orientationHistory[i])) {
            return orientationHistory[i];
        }
    }
    
    //This line should never happen, but just in case...
    return UIInterfaceOrientationPortrait;
}

- (BOOL)shouldRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation checkModal:(BOOL)check
{
    return TI_ORIENTATION_ALLOWED([self getFlags:check],toInterfaceOrientation) ? YES : NO;
}

-(void)adjustFrameForUpSideDownOrientation:(NSNotification*)notification
{
    if ( (![TiUtils isIPad]) &&  ([[UIApplication sharedApplication] statusBarOrientation] == UIInterfaceOrientationPortraitUpsideDown) ) {
        CGRect statusBarFrame = [[UIApplication sharedApplication] statusBarFrame];
        if (statusBarFrame.size.height == 0) {
            return;
        }
        
        CGRect mainScreenBounds = [[UIScreen mainScreen] bounds];
        CGRect viewBounds = [[self view] bounds];
        
        //Need to do this to force navigation bar to draw correctly on iOS7
        [[NSNotificationCenter defaultCenter] postNotificationName:kTiFrameAdjustNotification object:nil];
        if (statusBarFrame.size.height > 20) {
            if (viewBounds.size.height != (mainScreenBounds.size.height - statusBarFrame.size.height)) {
                CGRect newBounds = CGRectMake(0, 0, mainScreenBounds.size.width, mainScreenBounds.size.height - statusBarFrame.size.height);
                CGPoint newCenter = CGPointMake(mainScreenBounds.size.width/2, (mainScreenBounds.size.height - statusBarFrame.size.height)/2);
                [[self view] setBounds:newBounds];
                [[self view] setCenter:newCenter];
                [[self view] setNeedsLayout];
            }
        } else {
            if (viewBounds.size.height != mainScreenBounds.size.height) {
                CGRect newBounds = CGRectMake(0, 0, mainScreenBounds.size.width, mainScreenBounds.size.height);
                CGPoint newCenter = CGPointMake(mainScreenBounds.size.width/2, mainScreenBounds.size.height/2);
                [[self view] setBounds:newBounds];
                [[self view] setCenter:newCenter];
                [[self view] setNeedsLayout];
            }
        }
    }
}


#ifdef DEVELOPER
- (void)viewWillLayoutSubviews
{
    CGRect bounds = [[self hostingView] bounds];
    NSLog(@"ROOT WILL LAYOUT SUBVIEWS %.1f %.1f",bounds.size.width, bounds.size.height);
    [super viewWillLayoutSubviews];
}
#endif

- (void)viewDidLayoutSubviews
{
    if ([TiUtils isIOS8OrGreater] && curTransformAngle == 0 && forceLayout) {
        [[self hostingView] setFrame:self.view.bounds];
    }
#ifdef DEVELOPER
    CGRect bounds = [[self hostingView] bounds];
    NSLog(@"ROOT DID LAYOUT SUBVIEWS %.1f %.1f",bounds.size.width, bounds.size.height);
#endif
    for (id<TiWindowProtocol> thisWindow in containedWindows) {
        if ([thisWindow isKindOfClass:[TiViewProxy class]]) {
            if (!CGRectEqualToRect([(TiViewProxy*)thisWindow sandboxBounds], [[self hostingView] bounds])) {
                [(TiViewProxy*)thisWindow parentSizeWillChange];
            }
        }
    }
    forceLayout = NO;
    [super viewDidLayoutSubviews];
    [self adjustFrameForUpSideDownOrientation:nil];
}

//IOS5 support. Begin Section. Drop in 3.2
- (BOOL)automaticallyForwardAppearanceAndRotationMethodsToChildViewControllers
{
    return YES;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
    return [self shouldRotateToInterfaceOrientation:toInterfaceOrientation checkModal:YES];
}
//IOS5 support. End Section


//IOS6 new stuff.

- (BOOL)shouldAutomaticallyForwardRotationMethods
{
    return YES;
}

- (BOOL)shouldAutomaticallyForwardAppearanceMethods
{
    return YES;
}

- (BOOL)shouldAutorotate{
    return YES;
}

-(void)incrementActiveAlertControllerCount
{
    if ([TiUtils isIOS8OrGreater]){
        ++activeAlertControllerCount;
    }
}
-(void)decrementActiveAlertControllerCount
{
    if ([TiUtils isIOS8OrGreater]) {
        --activeAlertControllerCount;
        if (activeAlertControllerCount == 0) {
            UIViewController* topVC = [self topPresentedController];
            if (topVC == self) {
                [self didCloseWindow:nil];
            } else {
                [self dismissKeyboard];
                
                if ([topVC respondsToSelector:@selector(proxy)]) {
                    id theProxy = [(id)topVC proxy];
                    if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
                        [(id<TiWindowProtocol>)theProxy gainFocus];
                    }
                }
            }
        }
    }
}

-(NSUInteger)supportedOrientationsForAppDelegate;
{
    if (forcingStatusBarOrientation) {
        return 0;
    }
    
    if ([TiUtils isIOS8OrGreater] && activeAlertControllerCount > 0) {
        return [self supportedInterfaceOrientations];
    }
    
    //Since this is used just for intersection, ok to return UIInterfaceOrientationMaskAll
    return 30;//UIInterfaceOrientationMaskAll
}

- (UIInterfaceOrientationMask)supportedInterfaceOrientations{
    //IOS6. If forcing status bar orientation, this must return 0.
    if (forcingStatusBarOrientation) {
        return 0;
    }
    //IOS6. If we are presenting a modal view controller, get the supported
    //orientations from the modal view controller
    UIViewController* topmostController = [self topPresentedControllerCheckingPopover:YES];
    if (topmostController != self) {
        NSUInteger retVal = [topmostController supportedInterfaceOrientations];
        // if ([topmostController isBeingDismissed]) {
            // UIViewController* presenter = [topmostController presentingViewController];
            // if (presenter == self) {
            //     return retVal | [self orientationFlags];
            // } else {
            //     return retVal | [presenter supportedInterfaceOrientations];
            // }
        // } else {
            return retVal;
        // }
    }
    return [self orientationFlags];
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation
{
    return [self lastValidOrientation:[self getFlags:NO]];
}

-(void)didOrientNotify:(NSNotification *)notification
{
    UIDeviceOrientation newOrientation = [[UIDevice currentDevice] orientation];
	
    if (forcingRotation || !UIDeviceOrientationIsValidInterfaceOrientation(newOrientation)) {
        return;
    }
    deviceOrientation = (UIInterfaceOrientation) newOrientation;
//    if ([self shouldRotateToInterfaceOrientation:deviceOrientation checkModal:NO]) {
//        [[UIDevice currentDevice] setValue:@(deviceOrientation) forKey:@"orientation"];
//        [self resetTransformAndForceLayout:YES];
//        forcingStatusBarOrientation = YES;
//        [[UIApplication sharedApplication] setStatusBarOrientation:deviceOrientation animated:NO];
//        forcingStatusBarOrientation = NO;
//        [self updateOrientationHistory:deviceOrientation];
//    }
}


-(void)refreshOrientationWithDuration:(id)unused forController:(id<TiOrientationController>) orientationController
{
    if (![[TiApp app] windowIsKeyWindow]) {
        VerboseLog(@"[DEBUG] RETURNING BECAUSE WE ARE NOT KEY WINDOW");
        return;
    }
    
//    if (forcingRotation ) {
//        return;
//    }
    
    UIInterfaceOrientation target = [self lastValidOrientation:[self getFlags:NO]];
    //Device Orientation takes precedence.
    if (target != deviceOrientation) {
        if ([self shouldRotateToInterfaceOrientation:deviceOrientation checkModal:NO]) {
            target = deviceOrientation;
        }
//    } else if(forcedOrientation) {
//        return; //dont do it twice
    }
    
//    if ([[UIDevice currentDevice] orientation] != target) {
        forcingRotation = YES;
//        forcedOrientation = YES;
        [[UIDevice currentDevice] setValue:@(target) forKey:@"orientation"];
        forcingRotation = NO;
//#if defined(DEBUG) || defined(DEVELOPER)
//        DebugLog(@"Forcing rotation to %d. Current Orientation %d. This is not good UI design. Please reconsider.",target,[[UIApplication sharedApplication] statusBarOrientation]);
//#endif
//#ifdef FORCE_WITH_MODAL
//        [self forceRotateToOrientation:target];
//#else
        
//        if ([TiUtils isIOS8OrGreater]) {
//            [self rotateHostingViewToOrientation:target fromOrientation:[[UIApplication sharedApplication] statusBarOrientation]];
//        } else {
//            [self manuallyRotateToOrientation:target duration:[[UIApplication sharedApplication] statusBarOrientationAnimationDuration]];
//        }

//        forcingRotation = NO;
//#endif
//    } else {
//        [self resetTransformAndForceLayout:NO];
//    }
    
}

-(void)updateOrientationHistory:(UIInterfaceOrientation)newOrientation
{
	/*
	 *	And now, to push the orientation onto the history stack. This could be
	 *	expressed as a for loop, but the loop is so small that it might as well
	 *	be unrolled. The end result of this push is that only other orientations
	 *	are copied back, ensuring the newOrientation will be unique when it's
	 *	placed at the top of the stack.
	 */
	int i=0;
	for (int j=0;j<4;j++)
	{
		if (orientationHistory[j] == newOrientation) {
			i = j;
			break;
		}
	}
	while (i > 0) {
		orientationHistory[i] = orientationHistory[i-1];
		i--;
	}
	orientationHistory[0] = newOrientation;
}

//#ifdef FORCE_WITH_MODAL
//-(void)forceRotateToOrientation:(UIInterfaceOrientation)newOrientation
//{
//    UIViewController* tempPresenter = [self topPresentedController];
//    ForcingController* dummy = [[ForcingController alloc] init];
//    [dummy setOrientation:newOrientation];
//    forcingStatusBarOrientation = YES;
//
//    [[UIApplication sharedApplication] setStatusBarOrientation:newOrientation animated:NO];
//    
//    forcingStatusBarOrientation = NO;
//    
//    [self updateOrientationHistory:newOrientation];
//    
//    [tempPresenter presentViewController:dummy animated:NO completion:^{
//        [UIViewController attemptRotationToDeviceOrientation];
//        [tempPresenter dismissViewControllerAnimated:NO completion:nil];
//        [dummy release];
//    }];
//}
//#endif

//-(void)resetTransformAndForceLayout:(BOOL)updateStatusBar
//{
//    if (curTransformAngle != 0) {
//        forcedOrientation = NO;
//        curTransformAngle = 0;
//        forceLayout = YES;
//        [[self hostingView] setTransform:CGAffineTransformIdentity];
//        [[self view] setNeedsLayout];
//        if (updateStatusBar) {
//            [self updateStatusBar:NO];
//        }
//    }
//}

//-(void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator {
//    /// The size is wrong if this view controller is off screen
//    [super viewWillTransitionToSize:size withTransitionCoordinator:coordinator];
//            forcingStatusBarOrientation = YES;
//            [[UIApplication sharedApplication] setStatusBarOrientation:deviceOrientation animated:NO];
//            forcingStatusBarOrientation = NO;
//}

//-(void)rotateHostingViewToOrientation:(UIInterfaceOrientation)newOrientation fromOrientation:(UIInterfaceOrientation)oldOrientation
//{
//    if (!forcingRotation || (newOrientation == oldOrientation) ) {
//        return;
//    }
//    
//    NSInteger offset = 0;
//    CGAffineTransform transform;
//    
//    switch (oldOrientation) {
//        case UIInterfaceOrientationPortrait:
//        case UIInterfaceOrientationUnknown:
//            
//            if (newOrientation == UIInterfaceOrientationPortraitUpsideDown) {
//                offset = 180;
//            } else if (newOrientation == UIInterfaceOrientationLandscapeLeft) {
//                offset = -90;
//            } else if (newOrientation == UIInterfaceOrientationLandscapeRight) {
//                offset = 90;
//            }
//            break;
//        
//        case UIInterfaceOrientationLandscapeLeft:
//            if (newOrientation == UIInterfaceOrientationPortraitUpsideDown) {
//                offset = -90;
//            } else if (newOrientation == UIInterfaceOrientationPortrait) {
//                offset = 90;
//            } else if (newOrientation == UIInterfaceOrientationLandscapeRight) {
//                offset = 180;
//            }
//            break;
//            
//        case UIInterfaceOrientationLandscapeRight:
//            if (newOrientation == UIInterfaceOrientationPortraitUpsideDown) {
//                offset = 90;
//            } else if (newOrientation == UIInterfaceOrientationPortrait) {
//                offset = -90;
//            } else if (newOrientation == UIInterfaceOrientationLandscapeLeft) {
//                offset = 180;
//            }
//            break;
//            
//        case UIInterfaceOrientationPortraitUpsideDown:
//            if (newOrientation == UIInterfaceOrientationPortrait) {
//                offset = 180;
//            } else if (newOrientation == UIInterfaceOrientationLandscapeLeft) {
//                offset = 90;
//            } else if (newOrientation == UIInterfaceOrientationLandscapeRight) {
//                offset = -90;
//            }
//            break;
//    }
    //Blur out keyboard
//    [keyboardFocusedProxy blur:nil];
    
    //Rotate statusbar
    /*
     We will not rotae the status bar here but will temporarily force hide it. That way we will get
     correct size in viewWillTransitionToSize and re-enable visibility there. If we force the status
     bar to rotate the sizes are completely messed up.
    forcingStatusBarOrientation = YES;
    [[UIApplication sharedApplication] setStatusBarOrientation:newOrientation animated:NO];
    forcingStatusBarOrientation = NO;
    */
//    curTransformAngle = offset % 360;
//    
//    switch (curTransformAngle) {
//        case 90:
//        case -270:
//            transform = CGAffineTransformMakeRotation(M_PI_2);
//            break;
//        case -90:
//        case 270:
//            transform = CGAffineTransformMakeRotation(-M_PI_2);
//            break;
//        case 180:
//            transform = CGAffineTransformMakeRotation(M_PI);
//            break;
//        default:
//            transform = CGAffineTransformIdentity;
//            break;
//    }
//    [hostView setTransform:transform];
//    [hostView setFrame:self.view.bounds];
//}

//-(void)manuallyRotateToOrientation:(UIInterfaceOrientation)newOrientation duration:(NSTimeInterval)duration
//{
//    if (!forcingRotation) {
//        return;
//    }
//    UIApplication * ourApp = [UIApplication sharedApplication];
//    UIInterfaceOrientation oldOrientation = [ourApp statusBarOrientation];
//    CGAffineTransform transform;
//    
//    switch (newOrientation) {
//        case UIInterfaceOrientationPortraitUpsideDown:
//            transform = CGAffineTransformMakeRotation(M_PI);
//            break;
//        case UIInterfaceOrientationLandscapeLeft:
//            transform = CGAffineTransformMakeRotation(-M_PI_2);
//            break;
//        case UIInterfaceOrientationLandscapeRight:
//            transform = CGAffineTransformMakeRotation(M_PI_2);
//            break;
//        default:
//            transform = CGAffineTransformIdentity;
//            break;
//    }
//    
//    [self willRotateToInterfaceOrientation:newOrientation duration:duration];
//    
//    // Have to batch all of the animations together, so that it doesn't look funky
//    if (duration > 0.0) {
//        [UIView beginAnimations:@"orientation" context:nil];
//        [UIView setAnimationDuration:duration];
//    }
//    
//    if ((newOrientation != oldOrientation) && isCurrentlyVisible) {
//        TiViewProxy<TiKeyboardFocusableView> *kfvProxy = [keyboardFocusedProxy retain];
//        BOOL focusAfterBlur = [kfvProxy focused:nil];
//        if (focusAfterBlur) {
//            [kfvProxy blur:nil];
//        }
//        forcingStatusBarOrientation = YES;
//        [ourApp setStatusBarOrientation:newOrientation animated:(duration > 0.0)];
//        forcingStatusBarOrientation = NO;
//        if (focusAfterBlur) {
//            [kfvProxy focus:nil];
//        }
//        [kfvProxy release];
//    }
//    
//    UIView * ourView = [self view];
//    [ourView setTransform:transform];
//    [self resizeView];
//    
//    [self willAnimateRotationToInterfaceOrientation:newOrientation duration:duration];
//    
//    //Propigate this to everyone else. This has to be done INSIDE the animation.
//    [self repositionSubviews];
//    
//    if (duration > 0.0) {
//        [UIView commitAnimations];
//    }
//    
//    [self didRotateFromInterfaceOrientation:oldOrientation];
//}

#pragma mark - TiOrientationController
-(void)childOrientationControllerChangedFlags:(id<TiOrientationController>) orientationController
{
	WARN_IF_BACKGROUND_THREAD_OBJ;
    if ([self presentedViewController] == nil ||
        ([TiUtils isIOS8OrGreater] && [[self presentedViewController] isKindOfClass:[UIAlertController class]]) && isCurrentlyVisible) {
        [self refreshOrientationWithDuration:nil forController:(id<TiOrientationController>) orientationController];
        [self updateStatusBar:NO];
    }
}

-(void)setParentOrientationController:(id <TiOrientationController>)newParent
{
	//Blank method since we never have a parent.
}

-(id)parentOrientationController
{
	//Blank method since we never have a parent.
	return nil;
}

-(TiOrientationFlags) orientationFlags
{
    return [self getFlags:YES];
}

-(TiOrientationFlags) getFlags:(BOOL)checkModal
{
    TiOrientationFlags result = TiOrientationNone;
    if (checkModal) {
        @synchronized(modalWindows) {
            for (id<TiWindowProtocol> thisWindow in [modalWindows reverseObjectEnumerator])
            {
                if ([thisWindow closing] == NO) {
                    result = [thisWindow orientationFlags];
                    if (result != TiOrientationNone)
                    {
                        return result;
                    }
                }
            }
        }
        
    }
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in [containedWindows reverseObjectEnumerator])
        {
            if ([thisWindow closing] == NO) {
                result = [thisWindow orientationFlags];
                if (result != TiOrientationNone)
                {
                    return result;
                }
            }
        }
    }
    return [self getDefaultOrientations];
}

#pragma mark - Appearance and rotation callbacks

//- (void)traitCollectionDidChange:(UITraitCollection *)previousTraitCollection;
//{
//    [self resetTransformAndForceLayout:YES];
//    [super traitCollectionDidChange:previousTraitCollection];
//}

//Containing controller will call these callbacks(appearance/rotation) on contained windows when it receives them.
-(void)viewWillAppear:(BOOL)animated
{
#ifdef TI_USE_KROLL_THREAD
    TiThreadProcessPendingMainThreadBlocks(0.1, YES, nil);
#endif
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in containedWindows) {
            [thisWindow viewWillAppear:animated];
        }
    }
    
    [super viewWillAppear:animated];
    isCurrentlyVisible = YES;
}
-(void)viewWillDisappear:(BOOL)animated
{
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in containedWindows) {
            [thisWindow viewWillDisappear:animated];
        }
        [[containedWindows lastObject] resignFocus];
    }
    [super viewWillDisappear:animated];
}
-(void)viewDidAppear:(BOOL)animated
{
    [self.view becomeFirstResponder];
    @synchronized(containedWindows) {
        if ([containedWindows count] > 0) {
            for (id<TiWindowProtocol> thisWindow in containedWindows) {
                [thisWindow viewDidAppear:animated];
            }
//            if (forcingRotation || [TiUtils isIOS8OrGreater]) {
//                forcingRotation = NO;
//                [self performSelector:@selector(childOrientationControllerChangedFlags:) withObject:[containedWindows lastObject] afterDelay:[[UIApplication sharedApplication] statusBarOrientationAnimationDuration]];
//            } else {
                [self childOrientationControllerChangedFlags:[containedWindows lastObject]];
//            }
            [[containedWindows lastObject] gainFocus];
        }
    }
    [super viewDidAppear:animated];
}
-(void)viewDidDisappear:(BOOL)animated
{
    isCurrentlyVisible = NO;
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in containedWindows) {
            [thisWindow viewDidDisappear:animated];
        }
    }
    [super viewDidDisappear:animated];
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    _rotating = YES;
//    if (!forcingRotation) {
//        [self resetTransformAndForceLayout:YES];
//        //        [self resizeView];
//    }
    if (self.keyboardActiveInput) {
        UIView* accessory = self.keyboardActiveInput.inputAccessoryView;
        if ([accessory isKindOfClass:[TiUIView class]]) {
            [[((TiUIView*)accessory) viewProxy] refreshView];
        }
    }
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in containedWindows) {
//            TiOrientationFlags flags = [thisWindow orientationFlags];
//            if (TI_ORIENTATION_ALLOWED(flags,toInterfaceOrientation)) {
                [thisWindow willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
//            }
        }
    }
    [self updateOrientationHistory:toInterfaceOrientation];
    [self rotateDefaultImageViewToOrientation:toInterfaceOrientation];
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    _rotating = YES;
    
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in containedWindows) {
//            TiOrientationFlags flags = [thisWindow orientationFlags];
//            if (TI_ORIENTATION_ALLOWED(flags,toInterfaceOrientation)) {
                [thisWindow willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
//            }
        }
    }
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    
    _rotating = NO;
    @synchronized(containedWindows) {
        for (id<TiWindowProtocol> thisWindow in containedWindows) {
//            TiOrientationFlags flags = [thisWindow orientationFlags];
//            if (TI_ORIENTATION_ALLOWED(flags,toInterfaceOrientation)) {
                [thisWindow didRotateFromInterfaceOrientation:fromInterfaceOrientation];
//            }
        }
    }
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

#pragma mark - Status Bar Appearance
- (BOOL)prefersStatusBarHidden
{
    BOOL oldStatus = statusBarIsHidden;
    if ([modalWindows count] > 0) {
        @synchronized(modalWindows) {
            statusBarIsHidden = [[modalWindows lastObject] hidesStatusBar];
        }
        if ([TiUtils isIOS8OrGreater] && curTransformAngle != 0) {
            statusBarIsHidden = YES;
        }
    } else if ([containedWindows count] > 0) {
        @synchronized(containedWindows) {
            statusBarIsHidden = [[containedWindows lastObject] hidesStatusBar];
        }
//        if ([TiUtils isIOS8OrGreater] && curTransformAngle != 0) {
//            statusBarIsHidden = YES;
//        }
    } else {
        statusBarIsHidden = oldStatus = statusBarInitiallyHidden;
    }
    
    
    statusBarVisibilityChanged = (statusBarIsHidden != oldStatus);
    return statusBarIsHidden;
}

- (UIStatusBarAnimation)preferredStatusBarUpdateAnimation
{
    return UIStatusBarAnimationNone;
}

- (UIStatusBarStyle)preferredStatusBarStyle
{
    @synchronized(modalWindows) {
       if ([modalWindows count] > 0) {
            return [[modalWindows lastObject] preferredStatusBarStyle];
        }
    }
    @synchronized(containedWindows) {
        if ([containedWindows count] > 0) {
            return [[containedWindows lastObject] preferredStatusBarStyle];
        }
    }
    return defaultStatusBarStyle;
}

-(BOOL) modalPresentationCapturesStatusBarAppearance
{
    return YES;
}

- (void) updateStatusBar:(BOOL)animated
{
    [self updateStatusBar:animated withStyle:animated?UIStatusBarAnimationFade:UIStatusBarAnimationNone];
}

- (void) updateStatusBar:(BOOL)animated withStyle:(UIStatusBarAnimation)style
{
    if (viewControllerControlsStatusBar) {
        [self performSelector:@selector(setNeedsStatusBarAppearanceUpdate) withObject:nil];
    } else {
        BOOL oldValue = statusBarIsHidden;;
        BOOL newValue = [self prefersStatusBarHidden];
//        if (oldValue != newValue) {
            if (animated) {
                [UIView beginAnimations:@"navbarAnim" context:NULL];
                [UIView setAnimationBeginsFromCurrentState:YES];
            }
            UIViewController* topController = [self topWindow].controller;
            CGFloat newY = newValue?0:20;
            CGRect navFrame = topController.navigationController.navigationBar.frame;
            CGFloat realDelta = newY - navFrame.origin.y;
            navFrame.origin.y += realDelta;
            if (!topController.navigationController.isNavigationBarHidden) {
                topController.navigationController.navigationBar.frame = navFrame;
            }
            //CGRect viewFrame = topController.view.frame;
            //viewFrame.origin.y += realDelta;
            //viewFrame.size.height -= realDelta;
            //topController.view.frame = viewFrame;
            [[UIApplication sharedApplication] setStatusBarStyle:[self preferredStatusBarStyle] animated:animated];
            [[UIApplication sharedApplication] setStatusBarHidden:newValue withAnimation:style];
            [self resizeView];
            if (animated) {
                [UIView commitAnimations];
            }
            
//        }
        
    }
}

#pragma mark Remote Control Notifications

//-(BOOL)canBecomeFirstResponder {
//    return YES;
//}

-(void)remoteControlReceivedWithEvent:(UIEvent *)event {
    /*Can not find code associated with this anywhere. Keeping in place just in case*/
    [[NSNotificationCenter defaultCenter] postNotificationName:kTiRemoteControlNotification object:self userInfo:[NSDictionary dictionaryWithObject:event forKey:@"event"]];
}


@end
