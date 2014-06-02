/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UINAVIGATIONWINDOW

#import "TiUINavigationWindowProxy.h"
#import "TiUINavigationWindow.h"
#import "TiApp.h"
#import "TiTransition.h"
#import "UIViewController+ADTransitionController.h"

@interface TiUINavigationWindowProxy()
{
    ADNavigationControllerDelegate* _navigationDelegate;
}
@property(nonatomic,retain) NSDictionary *defaultTransition;
@end

@implementation TiUINavigationWindowProxy
{
    BOOL _hasOnStackChange;
    UIScreenEdgePanGestureRecognizer* popRecognizer;
}

-(void)dealloc
{
	RELEASE_TO_NIL_AUTORELEASE(rootWindow);
    RELEASE_TO_NIL(_navigationDelegate);
    RELEASE_TO_NIL(navController);
    RELEASE_TO_NIL(current);
    RELEASE_TO_NIL(_defaultTransition);
    RELEASE_TO_NIL(popRecognizer);
	[super dealloc];
}

-(id)init
{
	if ((self = [super init]))
	{
        self.defaultTransition = [self platformDefaultTransition];
        _hasOnStackChange = NO;
	}
	return self;
}

-(NSDictionary*)platformDefaultTransition
{
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        return @{ @"style" : [NSNumber numberWithInt:NWTransitionModernPush], @"duration" : @550 };
    }
    else {
        return @{ @"style" : [NSNumber numberWithInt:NWTransitionSwipe], @"duration" : @300 };
    }
}

-(void)_initWithProperties:(NSDictionary *)properties
{
    [super _initWithProperties:properties];
}

-(NSString*)apiName
{
    return @"Ti.UI.iOS.NavigationWindow";
}

#pragma mark - TiOrientationController

-(TiOrientationFlags) orientationFlags
{
    for (id thisController in [[navController viewControllers] reverseObjectEnumerator])
    {
        if (![thisController isKindOfClass:[TiViewController class]])
        {
            continue;
        }
        TiWindowProxy * thisProxy = (TiWindowProxy *)[(TiViewController *)thisController proxy];
        if ([thisProxy conformsToProtocol:@protocol(TiOrientationController)])
        {
            TiOrientationFlags result = [thisProxy orientationFlags];
            if (result != TiOrientationNone)
            {
                return result;
            }
        }
    }
    return _supportedOrientations;
}

#pragma mark - TiTab Protocol

-(id)tabGroup
{
    return nil;
}

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

-(UIViewController *)rootController
{
    if (rootWindow == nil) {
        id window = [self valueForKey:@"window"];
        ENSURE_TYPE(window, TiWindowProxy);
        rootWindow = [window retain];
        [rootWindow setIsManaged:YES];
        [rootWindow setTab:(TiViewProxy<TiTab> *)self];
        [rootWindow setParentOrientationController:self];
        [rootWindow open:nil];
        [rootWindow windowWillOpen];
        [rootWindow windowDidOpen];
        current = [rootWindow retain];
    }
    return [rootWindow hostingController];
}

-(ADNavigationControllerDelegate*) navigationDelegate
{
    if (AD_SYSTEM_VERSION_GREATER_THAN_7 && rootWindow && _navigationDelegate == nil) {
        [self controller];
    }
    return _navigationDelegate;
}

-(id)controller
{
    if (navController == nil) {
        UIViewController * transitionController = nil;
        if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
            navController = [[UINavigationController alloc] initWithRootViewController:[self rootController]];
            [rootWindow viewWillAppear:NO]; // not called otherwise :s
            RELEASE_TO_NIL(_navigationDelegate)
            _navigationDelegate = [[ADNavigationControllerDelegate alloc] init];
            [_navigationDelegate manageNavigationController:(id)navController];
            _navigationDelegate.delegate = self;
            SETPROP(@"swipeToClose", setSwipeToClose);
        } else {
            navController = [[ADTransitionController alloc] initWithRootViewController:[self rootController]];
            ((ADTransitionController*)navController).delegate = self;
        }
        [TiUtils configureController:navController withObject:self];
        [navController navigationBar].translucent = YES;
    }
    return navController;
}

-(void)openWindow:(NSArray*)args
{
	TiWindowProxy *window = [args objectAtIndex:0];
	ENSURE_TYPE(window,TiWindowProxy);
    
    if (window == current) return;

    if ((window == rootWindow && ![rootWindow opening]) || [self controllerForWindow:window] != nil) {
        TiThreadPerformOnMainThread(^{
            [self popOnUIThread:args];
        }, YES);
        return;
    }
    [window setIsManaged:YES];
	[window setTab:(TiViewProxy<TiTab> *)self];
	[window setParentOrientationController:self];
    //Send to open. Will come back after _handleOpen returns true.
    if (![window opening]) {
        args = ([args count] > 1) ? [args objectAtIndex:1] : nil;
        if (args != nil) {
            args = [NSArray arrayWithObject:args];
        }
        [window open:args];
        return;
    }
    
	[[[TiApp app] controller] dismissKeyboard];
	TiThreadPerformOnMainThread(^{
		[self pushOnUIThread:args];
	}, YES);
}

-(void)closeWindow:(NSArray*)args
{
	TiWindowProxy *window = [args objectAtIndex:0];
	ENSURE_TYPE(window,TiWindowProxy);
    if (window == rootWindow) {
        DebugLog(@"[WARN] Closing the first window is like closing ourself");
        if ([args count] > 1) {
            args = [NSArray arrayWithObjects:[args objectAtIndex:1], nil];
        } else {
            args = [NSArray array];
        }
        [self close:args];
        return;
    }
    UIViewController* winController = [self controllerForWindow:window];
    if (winController != nil) {
        TiWindowProxy *realWindow = rootWindow;
        int index = [[navController viewControllers] indexOfObject:winController];
        if (index > 0) {
            realWindow = (TiWindowProxy *)[[[navController viewControllers] objectAtIndex:(index-1)] proxy];
            TiThreadPerformOnMainThread(^{
                [self popOnUIThread:([args count] > 1) ? @[realWindow,[args objectAtIndex:1]] : @[realWindow]];
            }, YES);
            return;
        }
    }
    TiThreadPerformOnMainThread(^{
        [self popOnUIThread:args];
    }, YES);
}

-(void)closeCurrentWindow:(NSArray*)args
{
    TiThreadPerformOnMainThread(^{
        [self popOnUIThread:([args count] > 0) ? @[current,[args objectAtIndex:0]] : @[current]];
    }, YES);
}


-(void)closeAllWindows:(NSArray*)args
{
    TiThreadPerformOnMainThread(^{
        [self popOnUIThread:args];
    }, YES);
}


-(id)stackSize
{
    return [NSNumber numberWithInt:[[navController viewControllers] count]];
}

-(void)windowClosing:(TiWindowProxy*)window animated:(BOOL)animated
{
    //NO OP NOW
}


#pragma mark - UINavigationControllerDelegate

- (void)navController:(id)transitionController willShowViewController:(UIViewController *)viewController animated:(BOOL)animated;
{
    if (current != nil) {
        UIViewController *curController = [current hostingController];
        NSArray* curStack = [navController viewControllers];
        BOOL winclosing = NO;
        if (![curStack containsObject:curController]) {
            winclosing = YES;
        } else {
            NSUInteger curIndex = [curStack indexOfObject:curController];
            if (curIndex > 1) {
                UIViewController* currentPopsTo = [curStack objectAtIndex:(curIndex - 1)];
                if (currentPopsTo == viewController) {
                    winclosing = YES;
                }
            }
        }
        
        
        BOOL transitionWithGesture = NO;
        if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
            transitionWithGesture = _navigationDelegate.isInteracting;
            if (!transitionWithGesture) {
                ADTransition* transition = [((ADTransitioningViewController*)viewController) transition];
                if (transition) {
                    [self fireEvent:winclosing?@"closeWindow":@"openWindow" forController:viewController transition:transition];
                }
            }
        }
        if (winclosing && !transitionWithGesture) {
            //TIMOB-15033. Have to call windowWillClose so any keyboardFocussedProxies resign
            //as first responders. This is ok since tab is not nil so no message will be sent to
            //hosting controller.
            [current windowWillClose];
        }
    }
    TiWindowProxy* theWindow = (TiWindowProxy*)[(TiViewController*)viewController proxy];
    if ((theWindow != rootWindow) && [theWindow opening]) {
//        [theWindow windowWillOpen];
        [theWindow setAnimating:YES];
    }
}

- (void)navController:(id)transitionController didShowViewController:(UIViewController *)viewController animated:(BOOL)animated;
{
    if (current != nil) {
        UIViewController* oldController = [current hostingController];
        
        if (![[navController viewControllers] containsObject:oldController]) {
            [current setTab:nil];
            [current setParentOrientationController:nil];
            [current close:nil];
        }
    }
    RELEASE_TO_NIL(current);
    TiWindowProxy* theWindow = (TiWindowProxy*)[(TiViewController*)viewController proxy];
    if ((theWindow != rootWindow) && [theWindow opening]) {
        [theWindow setAnimating:NO];
        [theWindow windowDidOpen];
    }
    current = [theWindow retain];
    [self childOrientationControllerChangedFlags:current];
    if (focussed) {
        [current gainFocus];
    }
}

- (void)transitionController:(ADTransitionController *)transitionController willShowViewController:(UIViewController *)viewController animated:(BOOL)animated;
{
    [self navController:transitionController willShowViewController:viewController animated:animated];
}

- (void)transitionController:(ADTransitionController *)transitionController didShowViewController:(UIViewController *)viewController animated:(BOOL)animated;
{
    [self navController:transitionController didShowViewController:viewController animated:animated];
}

- (void)navigationController:(UINavigationController *)navigationController willShowViewController:(UIViewController *)viewController animated:(BOOL)animated
{
    [self navController:navigationController willShowViewController:viewController animated:animated];
    id<UIViewControllerTransitionCoordinator> tc = navigationController.topViewController.transitionCoordinator;
    [tc notifyWhenInteractionEndsUsingBlock:^(id<UIViewControllerTransitionCoordinatorContext> context) {
        if (![context isCancelled]) {
            [self fireEvent:@"closeWindow" forController:viewController transition:[((ADTransitioningViewController*)viewController) transition]];
        }
    }];
}

- (void)navigationController:(UINavigationController *)navigationController didShowViewController:(UIViewController *)viewController animated:(BOOL)animated
{
    [self navController:navigationController didShowViewController:viewController animated:animated];
}


-(NSDictionary*)propsDictFromTransition:(ADTransition*)transition
{
    if (!transition) return @{};
    return @{@"duration": NUMINT([transition duration]*1000),
             @"style": [TiTransitionHelper tiTransitionTypeForADTransition:transition],
             @"substyle": NUMINT(transition.orientation),
             @"reverse": NUMBOOL(transition.isReversed)};
}

-(void)setOnstackchange:(KrollCallback *)callback
{
	_hasOnStackChange = [callback isKindOfClass:[KrollCallback class]];
	[self setValue:callback forUndefinedKey:@"onstackchange"];
}

-(void)fireEvent:(NSString *)type forController:(UIViewController *)viewController transition:(ADTransition *)transition
{
    BOOL hasEvent = [self _hasListeners:type checkParent:NO];
    
    if (_hasOnStackChange || hasEvent) {
        NSDictionary* dict = @{@"window": ((TiViewController*)viewController).proxy,
                               @"transition":[self propsDictFromTransition:transition],
                               @"stackIndex":NUMINT([[navController viewControllers] indexOfObject:viewController]),
                               @"animated": NUMBOOL(transition != nil)};
        if (_hasOnStackChange){
            NSMutableDictionary * event = [dict mutableCopy];
            [event setObject:type forKey:@"type"];
            [self fireCallback:@"onstackchange" withArg:event withSource:self];
        }
        else {
            [self fireEvent:type withObject:dict propagate:NO checkForListener:NO];
        }
    }
}

- (void)transitionController:(ADTransitionController *)transitionController willPushViewController:(UIViewController *)viewController transition:(ADTransition *)transition
{
    [self fireEvent:@"openWindow" forController:viewController transition:transition];
}

- (void)transitionController:(ADTransitionController *)transitionController willPopToViewController:(UIViewController *)viewController transition:(ADTransition *)transition
{
    [self fireEvent:@"closeWindow" forController:viewController transition:transition];
}

-(ADTransition*) lastTransition {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        return [(ADTransitioningViewController*)[current hostingController] transition];
    }
    else {
        return [navController lastTransition];
    }
}

-(ADTransition*) lastTransitionReversed {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        return [[(ADTransitioningViewController*)[current hostingController] transition] reverseTransitionForSourceRect:[[self view] bounds]];
    }
    else {
        return [navController lastTransitionReversed];
    }
}

-(UIViewController*) beforeLastController {
    return [[navController viewControllers] objectAtIndex:([[navController viewControllers] count] - 2)];
}

-(UIViewController*) lastController {
    return [[navController viewControllers] objectAtIndex:([[navController viewControllers] count] - 1)];
}

- (void)_pushViewController:(UIViewController *)viewController withTransition:(ADTransition *)transition {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
//        [(ADTransitioningViewController*)viewController setTransition:transition];
        [navController pushViewController:viewController animated:YES];
    } else {
        [navController pushViewController:viewController withTransition:transition];
    }
}

- (UIViewController *)popViewController {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        return [navController popViewControllerAnimated:YES];
    } else {
        return [navController popViewController];
    }
}

- (UIViewController *)_popViewControllerWithTransition:(ADTransition *)transition {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
//        UIViewController* ctlr = [current hostingController];
//        [(ADTransitioningViewController*)ctlr setTransition:transition];
        return [navController popViewControllerAnimated:YES];
    } else {
        return [navController popViewControllerWithTransition:transition];
    }
}

- (NSArray *)_popToViewController:(UIViewController *)viewController {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        return [navController popToViewController:viewController animated:YES];
    } else {
        return [navController popToViewController:viewController];
    }
}

- (NSArray *)_popToViewController:(UIViewController *)viewController withTransition:(ADTransition *)transition {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
//        [(ADTransitioningViewController*)viewController setTransition:transition];
        return [navController popToViewController:viewController animated:YES];
    } else {
        return [navController popToViewController:viewController withTransition:transition];
    }
}

- (NSArray *)_popToRootViewController {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        return [navController popToRootViewControllerAnimated:YES];
    } else {
    return [navController popToRootViewController];
    }
}

- (NSArray *)_popToRootViewControllerWithTransition:(ADTransition *)transition {
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
//        [(ADTransitioningViewController*)[rootWindow hostingController] setTransition:transition];
        return [navController popToRootViewControllerAnimated:YES];
    } else {
    return [navController popToRootViewControllerWithTransition:transition];
    }
}

#pragma mark - Public API

-(void)setTransition:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary)
    if(arg != nil) {
        self.defaultTransition = arg;
    }
    else {
        self.defaultTransition = [self platformDefaultTransition];
    }
    [self replaceValue:arg forKey:@"transition" notification:NO];
}

-(void)setSwipeToClose:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSNumber)
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        [[self navigationDelegate] setIsInteractive:[TiUtils boolValue:arg def:YES]];
    }
    [self replaceValue:arg forKey:@"swipeToClose" notification:NO];
}

#pragma mark - Private API

-(void)setFrame:(CGRect)bounds
{
    if (navController != nil) {
        [[navController view] setFrame:bounds];
    }
}


-(UIViewAnimationTransition)popTransition:(UIViewAnimationTransition)pushTransition {
    switch (pushTransition) {
        case UIViewAnimationTransitionFlipFromLeft:
            return UIViewAnimationTransitionFlipFromRight;
            break;
        case UIViewAnimationTransitionFlipFromRight:
            return UIViewAnimationTransitionFlipFromLeft;
            break;
        case UIViewAnimationTransitionCurlDown:
            return UIViewAnimationTransitionCurlUp;
            break;
        case UIViewAnimationTransitionCurlUp:
            return UIViewAnimationTransitionCurlDown;
            break;
        default:
            return pushTransition;
            break;
    }
}

-(void)pushOnUIThread:(NSArray*)args
{
//	if (transitionIsAnimating || transitionWithGesture)
//	{
//		[self performSelector:_cmd withObject:args afterDelay:0.1];
//		return;
//	}
	TiWindowProxy *window = [args objectAtIndex:0];
    NSDictionary* props = [args count] > 1 ? [args objectAtIndex:1] : nil;
    if ([props isKindOfClass:[NSNull class]]) props = nil;
	BOOL animated = props!=nil ?[TiUtils boolValue:@"animated" properties:props def:YES] : YES;
    TiTransition* transition = nil;
    if (animated) {
        transition = [TiTransitionHelper transitionFromArg:[props objectForKey:@"transition"] defaultArg:[self defaultTransition] containerView:self.view];
    }
    if (AD_SYSTEM_VERSION_GREATER_THAN_7) {
        ((ADTransitioningViewController*)[window hostingController]).transition = transition.adTransition;
    }
    
    [window windowWillOpen];
    
    [self _pushViewController:[window hostingController] withTransition:transition.adTransition];
}

-(void)popOnUIThread:(NSArray*)args
{
//	if (transitionIsAnimating || transitionWithGesture)
//	{
//		[self performSelector:_cmd withObject:args afterDelay:0.1];
//		return;
//	}
    int propsIndex = 0;
    TiWindowProxy *window;
    if ([[args objectAtIndex:0] isKindOfClass:[TiWindowProxy class]]) {
        window = [args objectAtIndex:0];
        propsIndex = 1;
    }
    else {
        window = rootWindow;
    }
    
    NSDictionary* props = ([args count] > propsIndex)?[args objectAtIndex:propsIndex]:nil;
    if ([props isKindOfClass:[NSNull class]]) props = nil;
    BOOL animated = props!=nil ?[TiUtils boolValue:@"animated" properties:props def:YES] : YES;
    TiTransition* transition = nil;
    if (animated) {
        transition = [TiTransitionHelper transitionFromArg:[props objectForKey:@"transition"] defaultTransition:[[[TiTransition alloc] initWithADTransition:[self lastTransitionReversed]] autorelease] containerView:self.view];
    }
    
    if (window == current) {
        [self _popViewControllerWithTransition:transition.adTransition];
    }
    else {
        if (window == rootWindow) {
            NSArray* outViewControllers = [self _popToRootViewControllerWithTransition:transition.adTransition];
            if (outViewControllers) {
                for (int i = 0; i < outViewControllers.count - 1; i++) {
                    TiWindowProxy* win = (TiWindowProxy *)[[outViewControllers objectAtIndex:i ] proxy];
                    [win setTab:nil];
                    [win setParentOrientationController:nil];
                    [win close:nil];
                }
            }
        }
        else {
            UIViewController* winController = [self controllerForWindow:window];
            if (winController) {
                NSArray* outViewControllers = [self _popToViewController:winController withTransition:transition.adTransition];
                if (outViewControllers) {
                    for (int i = 0; i < outViewControllers.count - 1; i++) {
                        TiWindowProxy* win = (TiWindowProxy *)[[outViewControllers objectAtIndex:i ] proxy];
                        [win setTab:nil];
                        [win setParentOrientationController:nil];
                        [win close:nil];
                    }
                }
            }
        }
        
    }
}

-(UIViewController*) controllerForWindow:(TiWindowProxy*)window
{
    if (navController != nil) {
        for (TiViewController* viewController in [navController viewControllers]) {
            TiWindowProxy* win = (TiWindowProxy *)[viewController proxy];
            if (win == window) {
                return viewController;
            }
        }
    }
    return nil;
}

- (void)closeWindow:(TiWindowProxy*)window animated:(BOOL)animated
{
    [window retain];
    UIViewController *windowController = [[window hostingController] retain];
    
	// Manage the navigation controller stack
	NSMutableArray* newControllerStack = [NSMutableArray arrayWithArray:[navController viewControllers]];
	[newControllerStack removeObject:windowController];
	[navController setViewControllers:newControllerStack];
    [window setTab:nil];
	[window setParentOrientationController:nil];
	
	// for this to work right, we need to sure that we always have the tab close the window
	// and not let the window simply close by itself. this will ensure that we tell the
	// tab that we're doing that
	[window close:nil];
    RELEASE_TO_NIL_AUTORELEASE(window);
    RELEASE_TO_NIL(windowController);
}


-(void) cleanNavStack
{
    TiThreadPerformOnMainThread(^{
        if (navController != nil) {
            [navController setDelegate:nil];
            NSArray* currentControllers = [[navController viewControllers] retain];
            [navController setViewControllers:[NSMutableArray array]];
            
            for (TiViewController* viewController in currentControllers) {
                TiWindowProxy* win = (TiWindowProxy *)[viewController proxy];
                [win setTab:nil];
                [win setParentOrientationController:nil];
                [win close:nil];
            }
            [[navController view] removeFromSuperview];
            RELEASE_TO_NIL(navController);
            RELEASE_TO_NIL(current);
            RELEASE_TO_NIL(currentControllers);
        }
    },YES);
}


#pragma mark - TiWindowProtocol
-(void)viewWillAppear:(BOOL)animated
{
    if ([self viewAttached]) {
        [navController viewWillAppear:animated];
    }
    [super viewWillAppear:animated];
}
-(void)viewWillDisappear:(BOOL)animated
{
    if ([self viewAttached]) {
        [navController viewWillDisappear:animated];
    }
    [super viewWillDisappear:animated];
}

-(void)viewDidAppear:(BOOL)animated
{
    if ([self viewAttached]) {
        [navController viewDidAppear:animated];
    }
    [super viewDidAppear:animated];
}
-(void)viewDidDisappear:(BOOL)animated
{
    if ([self viewAttached]) {
        [navController viewDidDisappear:animated];
    }
    [super viewDidDisappear:animated];
    
}

-(BOOL) hidesStatusBar
{
    UIViewController* topVC = [navController topViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiViewProxy* theProxy = [(TiViewController*)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            return [(id<TiWindowProtocol>)theProxy hidesStatusBar];
        }
    }
    return [super hidesStatusBar];
}

-(UIStatusBarStyle)preferredStatusBarStyle;
{
    UIViewController* topVC = [navController topViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiViewProxy* theProxy = [(TiViewController*)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            return [(id<TiWindowProtocol>)theProxy preferredStatusBarStyle];
        }
    }
    return [super preferredStatusBarStyle];
}

-(void)gainFocus
{
    UIViewController* topVC = [navController topViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiViewProxy* theProxy = [(TiViewController*)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            [(id<TiWindowProtocol>)theProxy gainFocus];
        }
    }
    [super gainFocus];
}

-(void)resignFocus
{
    UIViewController* topVC = [navController topViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiViewProxy* theProxy = [(TiViewController*)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            [(id<TiWindowProtocol>)theProxy resignFocus];
        }
    }
    [super resignFocus];
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if ([self viewAttached]) {
        [navController willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if ([self viewAttached]) {
        [navController willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    if ([self viewAttached]) {
        [navController didRotateFromInterfaceOrientation:fromInterfaceOrientation];
    }
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

-(TiProxy *)topWindow
{
    UIViewController* topVC = [navController topViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiViewProxy* theProxy = [(TiViewController*)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            return [(id<TiWindowProtocol>)theProxy topWindow];
        }
    }
    return self;
}

#pragma mark - TiViewProxy overrides
-(TiUIView*)newView
{
	CGRect frame = [TiUtils appFrame];
	TiUINavigationWindow * win = [[TiUINavigationWindow alloc] initWithFrame:frame];
	return win;
}

-(void)windowWillOpen
{
    UIView *nview = [[self controller] view];
	[nview setFrame:[[self view] bounds]];
	[[self view] addSubview:nview];
    return [super windowWillOpen];
}


-(void) windowDidClose
{
    [self cleanNavStack];
    [super windowDidClose];
}

-(void)willChangeSize
{
	[super willChangeSize];
	
	//TODO: Shouldn't this be not through UI? Shouldn't we retain the windows ourselves?
	for (UIViewController * thisController in [navController viewControllers])
	{
		if ([thisController isKindOfClass:[TiViewController class]])
		{
			TiViewProxy * thisProxy = [(TiViewController *)thisController proxy];
			[thisProxy willChangeSize];
		}
	}
}

@end

#endif
