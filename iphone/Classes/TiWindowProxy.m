/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiWindowProxy.h"
#import "TiUIWindow.h"
#import "TiApp.h"
#import "TiErrorController.h"
#import "TiTransitionAnimation+Friend.h"
#import "TiTransitionAnimationStep.h"
#import "TiModalNavViewController.h"

@interface TiUITouchPassThroughWindow:UIWindow

@end

@implementation TiUITouchPassThroughWindow

-(UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event{
    UIView *hitView = [super hitTest:point withEvent:event];
    if(hitView == self) {
        return nil;
    }
    return hitView;
}

@end

@interface TiWindowProxy(Private)
-(void)openOnUIThread:(id)args;
-(void)closeOnUIThread:(id)args;
@end

@implementation TiWindowProxy
{
    BOOL readyToBeLayout;
    BOOL _isManaged;
    
    BOOL _useCustomUIWindow;
    TiUITouchPassThroughWindow* _customUIWindow;
    UIWindowLevel _windowLevel;
}

@synthesize tab = tab;
@synthesize isManaged = _isManaged;

-(id)init
{
	if ((self = [super init]))
	{
        [self setDefaultReadyToCreateView:YES];
        opening = NO;
        opened = NO;
        readyToBeLayout = NO;
        _isManaged = NO;
        _useCustomUIWindow = NO;
        _windowLevel = UIWindowLevelNormal;
	}
	return self;
}

-(void) dealloc {
    RELEASE_TO_NIL(_customUIWindow)
    FORGET_AND_RELEASE_WITH_DELEGATE(openAnimation);
    FORGET_AND_RELEASE_WITH_DELEGATE(closeAnimation);

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
    FORGET_AND_RELEASE(transitionProxy)
#endif
    [super dealloc];
}

-(UIViewController*)controller;
{
    if (!_useCustomUIWindow && !controller) {
        return [[TiApp app] controller];
    }
    return controller;
}


//-(void)_destroy {
//    [self windowWillClose];
//    [self windowDidClose];
//    [super _destroy];
//}

-(void)_configure
{
    [self replaceValue:nil forKey:@"orientationModes" notification:NO];
    [super _configure];
}

-(NSString*)apiName
{
    return @"Ti.Window";
}

-(TiUIView*)newView
{
	TiUIWindow * win = (TiUIWindow*)[super newView];
    //try no to set frame because it might be wrong. Let it be computed
    //in viewWillAppear
//    if (!controller) {
    if (CGRectIsEmpty(self.sandboxBounds)) {
        self.sandboxBounds = [TiUtils appFrame];
    }
//    win.frame = CGRectIsEmpty(self.sandboxBounds) ? [TiUtils appFrame] : self.sandboxBounds;
//    }
    
	return win;
}

-(void)refreshViewIfNeeded
{
	if (!readyToBeLayout) return;
    [super refreshViewIfNeeded];
}

-(BOOL)relayout
{
    if (!readyToBeLayout) {
        //in case where the window was actually added as a child we want to make sure we are good
        readyToBeLayout = YES;
    }
    return [super relayout];
}

-(void)setSandboxBounds:(CGRect)rect
{
    if (!readyToBeLayout) {
        //in case where the window was actually added as a child we want to make sure we are good
        readyToBeLayout = YES;
    }
    [super setSandboxBounds:rect];
}

-(BOOL)isManaged
{
    if (parent) {
        return [[self getParentWindow] isManaged];
    }
    return _isManaged;
}

#pragma mark - Utility Methods
-(void)windowWillOpen
{
    if (!opened){
        opening = YES;
    }
    if (!_useCustomUIWindow && tab == nil && (self.isManaged == NO) && controller == nil) {
        [[[[TiApp app] controller] topContainerController] willOpenWindow:self];
    }
    [super windowWillOpen];
}

-(void)windowDidOpen
{
    opening = NO;
    opened = YES;
    [self fireEvent:@"open" propagate:NO];
    if ([self focussed] && [self handleFocusEvents]) {
        [self fireEvent:@"focus" propagate:NO];
    }
    [super windowDidOpen];
    FORGET_AND_RELEASE_WITH_DELEGATE(openAnimation);
    if (!_useCustomUIWindow && tab == nil && (self.isManaged == NO) && controller == nil) {
        [[[[TiApp app] controller] topContainerController] didOpenWindow:self];
    }
}

-(void) windowWillClose
{
//    [self viewWillDisappear:false];
    if (!_useCustomUIWindow && tab == nil && (self.isManaged == NO) && controller == nil) {
        [[[[TiApp app] controller] topContainerController] willCloseWindow:self];
    }
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [super windowWillClose];
}

-(void) windowDidClose
{
    opened = NO;
    closing = NO;
    if (_customUIWindow)
    {
        _customUIWindow.hidden = YES;
        if ([_customUIWindow isKeyWindow]) {
            [_customUIWindow resignFirstResponder];
        }
        RELEASE_TO_NIL(_customUIWindow)
    }
//    [self viewDidDisappear:false];
    [self fireEvent:@"close" propagate:NO];
    [[NSNotificationCenter defaultCenter] removeObserver:self]; //just to be sure
    FORGET_AND_RELEASE_WITH_DELEGATE(closeAnimation);
    if (tab == nil && (self.isManaged == NO) && controller == nil) {
        [[[[TiApp app] controller] topContainerController] didCloseWindow:self];
    }
    tab = nil;
    self.isManaged = NO;
    
    [super windowDidClose];
    [self forgetSelf];
}

-(void)attachViewToTopContainerController
{
    if (!controller) {
        UIViewController<TiControllerContainment>* topContainerController = [[[TiApp app] controller] topContainerController];
        UIView *rootView = [topContainerController hostingView];
        TiUIView* theView = [self view];
        [rootView addSubview:theView];
        [rootView bringSubviewToFront:theView];
        [[TiViewProxy class] reorderViewsInParent:rootView]; //make sure views are ordered along zindex
    }
    
}

-(BOOL)isRootViewLoaded
{
    return _useCustomUIWindow || [[[TiApp app] controller] isViewLoaded];
}

-(BOOL)isRootViewAttached
{
    //When a modal window is up, just return yes
    if (_useCustomUIWindow || [[[TiApp app] controller] presentedViewController] != nil) {
        return YES;
    }
    return ([[[[TiApp app] controller] view] superview]!=nil);
}

#pragma mark - TiWindowProtocol Base Methods
-(void)open:(id)args
{
    //If an error is up, Go away
    if (!_useCustomUIWindow && [[[[TiApp app] controller] topPresentedController] isKindOfClass:[TiErrorController class]]) {
        DebugLog(@"[ERROR] ErrorController is up. ABORTING open");
        return;
    }
    
    //I am already open or will be soon. Go Away
    if (opening || opened) {
        return;
    }
    
    if (([args count] > 0) && [[args objectAtIndex:0] isKindOfClass:[NSDictionary class]]) {
        NSDictionary* props = [args objectAtIndex:0];
        NSMutableSet* transferableProperties = [NSMutableSet setWithArray:[props allKeys]];
        NSMutableSet* supportedProperties = [NSMutableSet setWithArray:@[@"fullscreen", @"modal", @"navBarHidden", @"orientationModes", @"statusBarStyle"]];
        [transferableProperties intersectSet:supportedProperties];
        id<NSFastEnumeration> keySeq = transferableProperties;
		for (NSString * thisKey in keySeq)
		{
            [self replaceValue:[props valueForKey:thisKey] forKey:thisKey notification:NO];
		}
    }
    
    //Lets keep ourselves safe
    [self rememberSelf];

    //Make sure our RootView Controller is attached
    if (![self isRootViewLoaded]) {
        DebugLog(@"[WARN] ROOT VIEW NOT LOADED. WAITING");
        [self performSelector:@selector(open:) withObject:args afterDelay:0.1];
        return;
    }
    if (![self isRootViewAttached]) {
        DebugLog(@"[WARN] ROOT VIEW NOT ATTACHED. WAITING");
        [self performSelector:@selector(open:) withObject:args afterDelay:0.1];
        return;
    }
    
    opening = YES;
    
    isModal = (tab == nil && !self.isManaged) ? [TiUtils boolValue:[self valueForUndefinedKey:@"modal"] def:NO] : NO;
    _hidesStatusBar = [TiUtils boolValue:[self valueForUndefinedKey:@"fullscreen"] def:[[[TiApp app] controller] statusBarInitiallyHidden]];

	NSInteger theStyle = [TiUtils intValue:[self valueForUndefinedKey:@"statusBarStyle"] def:[[[TiApp app] controller] defaultStatusBarStyle]];
    switch (theStyle){
        case UIStatusBarStyleDefault:
        case UIStatusBarStyleLightContent:
            _internalStatusBarStyle = theStyle;
            break;
        default:
            _internalStatusBarStyle = UIStatusBarStyleDefault;
    }
    
    if (!isModal && (tab==nil)) {
        openAnimation = [[TiAnimation animationFromArg:args context:[self pageContext] create:NO] retain];
        if (openAnimation) {
            [self rememberProxy:openAnimation];
        }
    }
    [self updateOrientationModes];
    
    //GO ahead and call open on the UI thread
    TiThreadPerformBlockOnMainThread(^{
        [self openOnUIThread:args];
    }, YES);
    
}

-(void)updateOrientationModes
{
    //TODO Argument Processing
    id object = [self valueForUndefinedKey:@"orientationModes"];
    _supportedOrientations = [TiUtils TiOrientationFlagsFromObject:object];
}

//-(void)setStatusBarStyle:(id)style
//{
//    NSInteger theStyle = [TiUtils intValue:style def:[[[TiApp app] controller] defaultStatusBarStyle]];
//    switch (theStyle){
//        case UIStatusBarStyleDefault:
//        case UIStatusBarStyleLightContent:
//            _internalStatusBarStyle = theStyle;
//            break;
//        default:
//            _internalStatusBarStyle = UIStatusBarStyleDefault;
//    }
//    [self setValue:NUMINT(_internalStatusBarStyle) forUndefinedKey:@"statusBarStyle"];
//}

//-(void)setFullscreen:(id)value
//{
//    BOOL newValue = [TiUtils boolValue:[self valueForUndefinedKey:@"fullscreen"] def:[[[TiApp app] controller] statusBarInitiallyHidden]];
//    
//    if (_hidesStatusBar != newValue) {
//        _hidesStatusBar = newValue;
//        [self setValue:NUMINT(hidesStatusBar) forUndefinedKey:@"fullscreen"];
//        if([self focussed]) {
//            TiThreadPerformOnMainThread(^{
//                [(TiRootViewController*)[[TiApp app] controller] updateStatusBar:YES];
//            }, YES);
//        }
//    }
//    
//}

-(void)close:(id)args
{
    //I am not open. Go Away
//    if (opening) {
//        DebugLog(@"Window is opening. Ignoring this close call");
//        return;
//    }
    
    if (!opened) {
        DebugLog(@"Window is not open. Ignoring this close call");
        return;
    }
    
    if (closing) {
        DebugLog(@"Window is already closing. Ignoring this close call.");
        return;
    }
    
    if (tab != nil) {
        if ([args count] > 0) {
            args = [NSArray arrayWithObjects:self, [args objectAtIndex:0], nil];
        } else {
            args = [NSArray arrayWithObject:self];
        }
        [tab closeWindow:args];
        return;
    }
    
    closing = YES;
    
    //TODO Argument Processing
    closeAnimation = [[TiAnimation animationFromArg:args context:[self pageContext] create:NO] retain];
    if (closeAnimation) {
        [self rememberProxy:closeAnimation];
    }

    //GO ahead and call close on UI thread
    TiThreadPerformOnMainThread(^{
        [self closeOnUIThread:args];
    }, YES);
    
}

-(BOOL)_handleOpen:(id)args
{
    if (_useCustomUIWindow) {
       _customUIWindow = [[TiUITouchPassThroughWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
        _customUIWindow.windowLevel        = _windowLevel;
        _customUIWindow.backgroundColor    = [UIColor clearColor];
        _customUIWindow.rootViewController = [self hostingController];
        _customUIWindow;
        _customUIWindow.hidden = NO;
    }
    TiRootViewController* theController = [[TiApp app] controller];
    if (_customUIWindow || isModal || (tab != nil) || self.isManaged) {
        FORGET_AND_RELEASE_WITH_DELEGATE(openAnimation);
    } else if ((!self.isManaged) && (!isModal) && (openAnimation != nil) && ([theController topPresentedController] != [theController topContainerController]) ){
        DeveloperLog(@"[WARN] The top View controller is not a container controller. This window will open behind the presented controller without animations.")
        FORGET_AND_RELEASE_WITH_DELEGATE(openAnimation);
    }
    
    return YES;
}

-(BOOL)_handleClose:(id)args
{
    if (isModal || (tab != nil) || self.isManaged) {
        if (closeAnimation) {
            FORGET_AND_RELEASE_WITH_DELEGATE(closeAnimation);
        }
    }
    TiRootViewController* theController = [[TiApp app] controller];
    if (_customUIWindow || (!self.isManaged) && (!isModal) && (closeAnimation != nil) && ([theController topPresentedController] != [theController topContainerController]) ){
        DeveloperLog(@"[WARN] The top View controller is not a container controller. This window will close behind the presented controller without animations.")
        FORGET_AND_RELEASE_WITH_DELEGATE(closeAnimation);
    }
    return YES;
}

-(BOOL)opening
{
    return opening;
}

-(BOOL)closing
{
    return closing;
}

-(void)setModal:(id)val
{
    [self replaceValue:val forKey:@"modal" notification:NO];
}

-(void)setWindowLevel:(id)val
{
    if (opening || opened) {
        return;
    }
    _windowLevel = [TiUtils floatValue:val];
    _useCustomUIWindow = _windowLevel != UIWindowLevelNormal;
    [self replaceValue:val forKey:@"windowLevel" notification:NO];
}


-(id)modal
{
    return [self valueForUndefinedKey:@"modal"];
}

-(BOOL)isModal
{
    TiParentingProxy* topParent = [self topParent];
    if ([topParent isKindOfClass:[TiWindowProxy class]])
    {
        return [(TiWindowProxy*)topParent isModal];
    }
    return isModal;
}

-(TiParentingProxy*)topParent
{
    TiParentingProxy* result = parent;
    while ([result parent]) {
        result = [result parent];
    }
    return result;
}


-(BOOL)hidesStatusBar
{
    return _hidesStatusBar;
}

-(UIStatusBarStyle)preferredStatusBarStyle;
{
    return _internalStatusBarStyle;
}

-(BOOL)handleFocusEvents
{
	return YES;
}

-(BOOL)focussed
{
	return focussed || [tab focussed] || [[self getParentWindow] focussed];
}

-(void)gainFocus
{
    if (focussed == NO) {
        focussed = YES;
        if ([self handleFocusEvents] && opened) {
            [self fireEvent:@"focus" propagate:NO];
        }
        UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, nil);
        [view setAccessibilityElementsHidden:NO];
    }
    TiThreadPerformOnMainThread(^{
        [self forceNavBarFrame];
    }, NO);

}

-(void)resignFocus
{
    if (focussed == YES) {
        focussed = NO;
        if ([self handleFocusEvents]) {
            [self fireEvent:@"blur" propagate:NO];
        }
        [view setAccessibilityElementsHidden:YES];
    }
}

-(void)blur:(id)args
{
	ENSURE_UI_THREAD_1_ARG(args)
	[self resignFocus];
    [super blur:nil];
}

-(void)focus:(id)args
{
	ENSURE_UI_THREAD_1_ARG(args)
	[self gainFocus];
    [super focus:nil];
}

-(TiProxy *)topWindow
{
    return self;
}

-(TiProxy *)parentForBubbling
{
    if ([super parentForBubbling]) return [super parentForBubbling];
    else return tab;
}

#pragma mark - Private Methods
-(TiProxy*)tabGroup
{
    return [tab tabGroup];
}

-(NSNumber*)orientation
{
	return NUMINT([UIApplication sharedApplication].statusBarOrientation);
}

-(void)forceNavBarFrame
{
    if (![self focussed]) {
        return;
    }
    if ( (controller == nil) || ([controller navigationController] == nil) ) {
        return;
    }
    
    if (![[[TiApp app] controller] statusBarVisibilityChanged]) {
        return;
    }
    
    UINavigationController* nc = [controller navigationController];
    BOOL isHidden = [nc isNavigationBarHidden];
    [nc setNavigationBarHidden:!isHidden animated:NO];
    [nc setNavigationBarHidden:isHidden animated:NO];
    [[nc view] setNeedsLayout];
}


-(void)openOnUIThread:(NSArray*)args
{
    if ([self _handleOpen:args]) {
        [self parentWillShow];
        if (tab != nil) {
            if ([args count] > 0) {
                args = [NSArray arrayWithObjects:self, [args objectAtIndex:0], nil];
            } else {
                args = [NSArray arrayWithObject:self];
            }
            [tab openWindow:args];
        } else if (isModal) {
            UIViewController* theController = [self hostingController];
            if (![TiUtils boolValue:[self valueForUndefinedKey:@"navBarHidden"] def:YES]) {
                //put it in a navigation controller to get the navbar if it was explicitely asked for
                theController = [[[TiModalNavViewController alloc] initWithRootViewController:theController] autorelease];
            }
            [self windowWillOpen];
            NSDictionary *dict = [args count] > 0 ? [args objectAtIndex:0] : nil;
            int style = [TiUtils intValue:@"modalTransitionStyle" properties:dict def:-1];
            if (style != -1) {
                [theController setModalTransitionStyle:style];
            }
            style = [TiUtils intValue:@"modalStyle" properties:dict def:-1];
            if (style != -1) {
				// modal transition style page curl must be done only in fullscreen
				// so only allow if not page curl
				if ([theController modalTransitionStyle]!=UIModalTransitionStylePartialCurl)
				{
					[theController setModalPresentationStyle:style];
				}
            }
            BOOL animated = [TiUtils boolValue:@"animated" properties:dict def:YES];
            [[TiApp app] showModalController:theController animated:animated];
        } else {
            [self windowWillOpen];
            [self view];
            if ((self.isManaged == NO) && ((openAnimation == nil) || (![openAnimation isTransitionAnimation]))){
                [self attachViewToTopContainerController];
            }
            if (openAnimation != nil) {
                [self animate:openAnimation];
            } else {
                [self windowDidOpen];
            }
        }
    } else {
        DebugLog(@"[WARN] OPEN ABORTED. _handleOpen returned NO");
        opening = NO;
        opened = NO;
        FORGET_AND_RELEASE_WITH_DELEGATE(openAnimation);
    }
}

-(void)closeOnUIThread:(NSArray *)args
{
    if ([self _handleClose:args]) {
        [self windowWillClose];
        if (isModal) {
            NSDictionary *dict = [args count] > 0 ? [args objectAtIndex:0] : nil;
            BOOL animated = [TiUtils boolValue:@"animated" properties:dict def:YES];
            [[TiApp app] hideModalController:controller animated:animated];
        } else {
            if (closeAnimation != nil) {
                [closeAnimation setDelegate:self];
                [self animate:closeAnimation];
            } else {
                [self windowDidClose];
            }
        }
        
    } else {
        DebugLog(@"[WARN] CLOSE ABORTED. _handleClose returned NO");
        closing = NO;
        FORGET_AND_RELEASE_WITH_DELEGATE(closeAnimation);
    }
}

#pragma mark - TiOrientationController
-(void)childOrientationControllerChangedFlags:(id<TiOrientationController>) orientationController;
{
    [parentController childOrientationControllerChangedFlags:self];
}

-(void)setParentOrientationController:(id <TiOrientationController>)newParent
{
    parentController = newParent;
}

-(id)parentOrientationController
{
	return parentController;
}

-(TiOrientationFlags) orientationFlags
{
    if ([self isModal]) {
        return (_supportedOrientations==TiOrientationNone) ? [[[TiApp app] controller] getDefaultOrientations] : _supportedOrientations;
    }
    return _supportedOrientations;
}


-(void)showNavBar:(NSArray*)args
{
    ENSURE_UI_THREAD(showNavBar,args);
    [self replaceValue:[NSNumber numberWithBool:NO] forKey:@"navBarHidden" notification:NO];
    if (controller!=nil)
    {
        id properties = (args!=nil && [args count] > 0) ? [args objectAtIndex:0] : nil;
        BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
        [[controller navigationController] setNavigationBarHidden:NO animated:animated];
    }
}

-(void)hideNavBar:(NSArray*)args
{
    ENSURE_UI_THREAD(hideNavBar,args);
    [self replaceValue:[NSNumber numberWithBool:YES] forKey:@"navBarHidden" notification:NO];
    if (controller!=nil)
    {
        id properties = (args!=nil && [args count] > 0) ? [args objectAtIndex:0] : nil;
        BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
        [[controller navigationController] setNavigationBarHidden:YES animated:animated];
        //TODO: need to fix height
    }
}


#pragma mark - Appearance and Rotation Callbacks. For subclasses to override.
//Containing controller will call these callbacks(appearance/rotation) on contained windows when it receives them.
-(void)viewWillAppear:(BOOL)animated
{
    readyToBeLayout = YES;
    [super viewWillAppear:animated];
    [self refreshViewIfNeeded];
}
-(void)viewWillDisappear:(BOOL)animated
{
    if (controller != nil) {
        [self resignFocus];
    }
    [super viewWillDisappear:animated];
}
-(void)viewDidAppear:(BOOL)animated
{
    if (isModal && opening) {
        [self windowDidOpen];
    }
    if (controller != nil && !self.isManaged) {
        [self gainFocus];
    }
    [super viewDidAppear:animated];
}
-(void)viewDidDisappear:(BOOL)animated
{
    if (isModal && closing) {
        [self windowDidClose];
    }
    [super viewDidDisappear:animated];
}

-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    [self refreshViewIfNeeded];
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}

-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    [self setFakeAnimationOfDuration:duration andCurve:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut]];
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    [self removeFakeAnimation];
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

#pragma mark - TiAnimation Delegate Methods

-(BOOL)canAnimateWithoutParent
{
    return YES;
}

-(HLSAnimation*)animationForAnimation:(TiAnimation*)animation
{
    if (animation.isTransitionAnimation && (animation == openAnimation || animation == closeAnimation)) {
        
        TiTransitionAnimation * hlsAnimation = [TiTransitionAnimation animation];
        UIView* hostingView = nil;
        if (animation == openAnimation) {
            hostingView = [[[[TiApp app] controller] topContainerController] hostingView];
            hlsAnimation.openTransition = YES;
        } else {
            hostingView = [[self getOrCreateView] superview];
            hlsAnimation.closeTransition = YES;
        }
        hlsAnimation.animatedProxy = self;
        hlsAnimation.animationProxy = animation;
        hlsAnimation.transition = animation.transition;
        hlsAnimation.transitionViewProxy = self;
        TiTransitionAnimationStep* step = [TiTransitionAnimationStep animationStep];
        step.duration = [animation getAnimationDuration];
        [step addTransitionAnimation:hlsAnimation insideHolder:hostingView];
        return [HLSAnimation animationWithAnimationStep:step];
    }
    else {
        return [super animationForAnimation:animation];
    }
}

-(void)animationDidComplete:(TiAnimation *)sender
{
    [super animationDidComplete:sender];
    if (sender == openAnimation) {
        if (animatedOver != nil) {
            if ([animatedOver isKindOfClass:[TiUIView class]]) {
                TiViewProxy* theProxy = (TiViewProxy*)[(TiUIView*)animatedOver proxy];
                if ([theProxy viewAttached]) {
                    [[[self view] superview] insertSubview:animatedOver belowSubview:[self view]];
#ifndef TI_USE_AUTOLAYOUT
                    LayoutConstraint* layoutProps = [theProxy layoutProperties];
                    ApplyConstraintToViewWithBounds(layoutProps, &layoutProperties, (TiUIView*)animatedOver, [[animatedOver superview] bounds]);
#endif
                    [theProxy layoutChildren:NO];
                    RELEASE_TO_NIL(animatedOver);
                }
            } else {
                [[[self view] superview] insertSubview:animatedOver belowSubview:[self view]];
            }
        }
        [self windowDidOpen];
    } else if (sender == closeAnimation) {
        [self windowDidClose];
    }
}
#ifdef USE_TI_UIIOSTRANSITIONANIMATION
-(TiUIiOSTransitionAnimationProxy*)transitionAnimation
{
    return transitionProxy;
}

-(void)setTransitionAnimation:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, TiUIiOSTransitionAnimationProxy)
    if(transitionProxy != nil) {
        FORGET_AND_RELEASE(transitionProxy)
    }
    transitionProxy = [args retain];
    [self rememberProxy:transitionProxy];
}
#endif

@end
