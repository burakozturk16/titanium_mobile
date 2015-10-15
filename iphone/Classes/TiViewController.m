/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiViewController.h"
#import "TiApp.h"
#import "TiViewProxy.h"
#import "TiModalNavViewController.h"

@interface ControllerWrapperView : UIView
@property (nonatomic,assign) TiViewProxy* proxy;

@end

@implementation ControllerWrapperView

-(void)setFrame:(CGRect)frame
{
    BOOL needsLayout = NO;
//    BOOL needsDirtyItAll = NO;
    
    // this happens when a controller resizes its view
    if (!CGRectIsEmpty(frame) && [self.proxy isKindOfClass:[TiViewProxy class]])
    {
        CGRect currentframe = [self frame];
        if (!CGRectEqualToRect(frame, currentframe))
        {
            needsLayout = YES;
//            needsDirtyItAll = CGRectIsEmpty(currentframe);
            CGRect bounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
            [(TiViewProxy*)self.proxy setSandboxBounds:bounds];
        }
    }
    [super setFrame:frame];
    if (needsLayout) {
//        if (needsDirtyItAll) {
//            [(TiViewProxy*)self.proxy dirtyItAll];
//            [(TiViewProxy*)self.proxy refreshViewOrParent];
//            
//        } else {
            if ([[self.layer animationKeys] count] > 0) {
                [(TiViewProxy*)self.proxy performBlockWithoutLayout:^{
                    [(TiViewProxy*)self.proxy parentSizeWillChange];
                }];
                
                [(TiViewProxy*)self.proxy refreshViewOrParent];
            }
            else {
                [(TiViewProxy*)self.proxy willChangeSize];
            }
//        }
    }
}

-(UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event{
    UIView *hitView = [super hitTest:point withEvent:event];
    if(hitView == self) {
        return nil;
    }
    return hitView;
}

@end

@implementation TiViewController

-(id)initWithViewProxy:(TiViewProxy*)window
{
    if (self = [super init]) {
        _proxy = window;
        [self updateOrientations];
        [TiUtils configureController:self withObject:_proxy];
    }
    return self;
}

-(void)dealloc
{
    [super dealloc];
}

#if IS_XCODE_7
-(NSArray<id<UIPreviewActionItem>> *)previewActionItems
{
    if ([self previewActions] == nil) {
        [self setPreviewActions:[NSArray array]];
    }
    
    return [self previewActions];
}
#endif

-(void)updateOrientations
{
    id object = [_proxy valueForUndefinedKey:@"orientationModes"];
    _supportedOrientations = [TiUtils TiOrientationFlagsFromObject:object];
    if (_supportedOrientations == TiOrientationNone) {
        _supportedOrientations = [[[TiApp app] controller] getDefaultOrientations];
    }
}

-(TiViewProxy*) proxy
{
    return _proxy;
}
-(void)detachProxy
{
    _proxy = nil;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    BOOL statusBarHidden = [self prefersStatusBarHidden];
    
    //fixes a bug on ios8 where the statusbar would get hidden on the first landscape orientation change
    [[UIApplication sharedApplication] setStatusBarHidden:!statusBarHidden withAnimation:UIStatusBarAnimationNone];
    [[UIApplication sharedApplication] setStatusBarHidden:statusBarHidden withAnimation:UIStatusBarAnimationNone];
}

#ifdef DEVELOPER
- (void)viewWillLayoutSubviews
{
    CGRect bounds = [[self view] bounds];
    NSLog(@"TIVIEWCONTROLLER WILL LAYOUT SUBVIEWS %.1f %.1f",bounds.size.width, bounds.size.height);
    [super viewWillLayoutSubviews];
}
#endif

- (void)viewDidLayoutSubviews
{
    CGRect bounds = [[self view] bounds];
#ifdef DEVELOPER
    NSLog(@"TIVIEWCONTROLLER DID LAYOUT SUBVIEWS %.1f %.1f",bounds.size.width, bounds.size.height);
#endif
    if (!CGRectEqualToRect([_proxy sandboxBounds], bounds)) {
        [_proxy setSandboxBounds:bounds];
        [_proxy parentSizeWillChange];
    }
    [super viewDidLayoutSubviews];
}

//IOS5 support. Begin Section. Drop in 3.2
- (BOOL)automaticallyForwardAppearanceAndRotationMethodsToChildViewControllers
{
    return YES;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
{
    return TI_ORIENTATION_ALLOWED(_supportedOrientations,toInterfaceOrientation) ? YES : NO;
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

- (UIInterfaceOrientationMask)supportedInterfaceOrientations {
    /*
     If we are in a navigation controller, let us match so it doesn't get freaked 
     out in when pushing/popping. We are going to force orientation anyways.
     */
    if ([self navigationController] != nil && ![[self navigationController]isKindOfClass:[TiModalNavViewController class]]) {
        return [[self navigationController] supportedInterfaceOrientations];
    }
    //This would be for modal.
    return (UIInterfaceOrientationMask)_supportedOrientations;
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation
{
    return [[[TiApp app] controller] lastValidOrientation:_supportedOrientations];
}

-(void)loadView
{
    if (_proxy == nil) {
        DebugLog(@"NO PROXY ASSOCIATED WITH VIEWCONTROLLER. RETURNING")
        return;
    }
    [self updateOrientations];
    [self setHidesBottomBarWhenPushed:[TiUtils boolValue:[_proxy valueForUndefinedKey:@"tabBarHidden"] def:NO]];
    //Always wrap proxy view with a wrapperView.
    //This way proxy always has correct sandbox when laying out
    TiUIView* pView = [_proxy getOrCreateView];
    [_proxy parentWillShow];
    ControllerWrapperView *wrapperView = [[ControllerWrapperView alloc] initWithFrame:pView.bounds];
    wrapperView.proxy = _proxy;
    wrapperView.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
    [wrapperView addSubview:pView];
    [wrapperView bringSubviewToFront:pView];
    self.view = wrapperView;
    [wrapperView release];
}

#pragma mark - Appearance & rotation methods

-(void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear:animated];
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy viewWillAppear:animated];
    }
    else {
        [_proxy parentWillShow];
        [_proxy refreshViewIfNeeded];
    }
}
-(void)viewWillDisappear:(BOOL)animated
{
    [super viewWillDisappear:animated];
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy viewWillDisappear:animated];
    }
    else {
        [_proxy parentWillHide];
    }
}
-(void)viewDidAppear:(BOOL)animated
{
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy viewDidAppear:animated];
    }
    [super viewDidAppear:animated];
}
-(void)viewDidDisappear:(BOOL)animated
{
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy viewDidDisappear:animated];
    }
    [super viewDidDisappear:animated];
}
-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    else {
        [_proxy setFakeAnimationOfDuration:duration andCurve:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut]];
        [_proxy refreshViewIfNeeded];
    }
    if ([_proxy respondsToSelector:@selector(isModal)])
    {
        if ([(id)_proxy isModal])
        {
            [[[[TiApp app] controller] topContainerController] willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
        }
    }
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    else {
        [_proxy setFakeAnimationOfDuration:duration andCurve:[CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut]];
    }
    if ([_proxy respondsToSelector:@selector(isModal)])
    {
        if ([(id)_proxy isModal])
        {
            [[[[TiApp app] controller] topContainerController] willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
        }
    }
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
   	if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        [(id<TiWindowProtocol>)_proxy didRotateFromInterfaceOrientation:fromInterfaceOrientation];
    }
    else {
        [_proxy removeFakeAnimation];
    }
    if ([_proxy respondsToSelector:@selector(isModal)])
    {
        if ([(id)_proxy isModal])
        {
            [[[[TiApp app] controller] topContainerController] didRotateFromInterfaceOrientation:fromInterfaceOrientation];
        }
    }
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

#pragma mark - Status Bar Appearance

- (BOOL)prefersStatusBarHidden
{
    if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        return [(id<TiWindowProtocol>)_proxy hidesStatusBar];
    } else {
        return NO;
    }
}

- (UIStatusBarStyle)preferredStatusBarStyle
{
    if ([_proxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
        return [(id<TiWindowProtocol>)_proxy preferredStatusBarStyle];
    } else if ([[[TiApp app] controller] topContainerController] != nil) {
        // Prefer the style of the most recent view controller.
        return [[[[TiApp app] controller] topContainerController] preferredStatusBarStyle];
    } else {
        return UIStatusBarStyleDefault;
    }
}

-(BOOL) modalPresentationCapturesStatusBarAppearance
{
    return YES;
}

- (UIStatusBarAnimation)preferredStatusBarUpdateAnimation
{
    return UIStatusBarAnimationNone;
}
- (BOOL)disablesAutomaticKeyboardDismissal {
    return NO;
}
@end
