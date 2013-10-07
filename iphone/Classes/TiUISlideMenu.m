/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiBase.h"
#import "TiUIWindowProxy.h"
#import "TiUISlideMenu.h"
#import "TiUISlideMenuProxy.h"
#import "TiUtils.h"
#import "TiViewController.h"
#import "UIViewController+ADTransitionController.h"

#define kLockViewId 102345

@interface TiUISlideMenu()
{
@private
	SWRevealViewController *_controller;
    PanningMode panningMode;
    PanningMode _lastUpdatePanningMode;
    
    TiViewProxy* leftView;
    TiViewProxy* rightView;
    TiViewProxy* centerView;
    UIView* _lockingView;
    UIView* _leftViewFadingView;
    UIView* _rightViewFadingView;
    CGFloat _fadeDegree; //between 0.0f and 1.0f
    TiDimension _leftScrollScale; //between 0.0f and 1.0f
    TiDimension _rightScrollScale; //between 0.0f and 1.0f
}
@end

@implementation TiUISlideMenu

-(UIViewController *) controllerForViewProxy:(TiViewProxy * )proxy
{
        [[proxy getOrCreateView] setAutoresizingMask:UIViewAutoresizingNone];
        [proxy windowWillOpen];
        [proxy layoutChildren:NO];
        [proxy windowDidOpen];
        if([proxy respondsToSelector:@selector(hostingController)])
        {
            return [(TiWindowProxy *)proxy hostingController];
        }
        return [[[TiViewController alloc] initWithViewProxy:proxy] autorelease];
}

-(id)init
{
    if (self = [super init])
    {
        panningMode = PanningModeCenterView;
        _lastUpdatePanningMode = -1;
        _fadeDegree = 0.0f;
        _leftScrollScale = TiDimensionDip(0.0f);
        _rightScrollScale = TiDimensionDip(0.0f);
    }
    return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(_controller);
	RELEASE_TO_NIL(leftView);
	RELEASE_TO_NIL(rightView);
	RELEASE_TO_NIL(centerView);
	RELEASE_TO_NIL(_lockingView);
	RELEASE_TO_NIL(_leftViewFadingView);
	RELEASE_TO_NIL(_rightViewFadingView);
    [super dealloc];
}

-(SWRevealViewController*)controller
{
	if (_controller==nil)
	{
        _controller = [[SWRevealViewController alloc] init];
        
//        controller.shouldAddPanGestureRecognizerToTopViewSnapshot = YES;
        UIView * controllerView = [_controller view];
//        [self setBackgroundColor:[UIColor clearColor]];
        [controllerView setBackgroundColor:[UIColor clearColor]];
        [controllerView setFrame:[self bounds]];
        [self addSubview:controllerView];
        _lockingView = [[UIView alloc] initWithFrame:[self bounds]];
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:_controller action:@selector(revealToggle:)];
        [_lockingView addGestureRecognizer:tap];
        [_lockingView setTag:kLockViewId];
        
        _controller.delegate = self;
        
//        [self setCenterView_:[[[UIViewController alloc] init] autorelease]];
        
//        [controller setAnchorLeftPeekAmount:40.0f];
//        [controller setAnchorRightPeekAmount:40.0f];
        
//        controller.rearViewRevealWidth = 60;
//        controller.rearViewRevealOverdraw = 120;
        _controller.bounceBackOnOverdraw = YES;
        _controller.stableDragOnOverdraw = YES;
        
//        [controller setUnderLeftWidthLayout:ECVariableRevealWidth];
//        [controller setUnderRightWidthLayout:ECVariableRevealWidth];
	}
	return _controller;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [[[self controller] view] setFrame:bounds];
    if ([self controller].frontViewController)
    {
        [[[self controller].frontViewController view] setFrame:bounds];
        [[self controller].frontViewController view].layer.shadowPath = [UIBezierPath bezierPathWithRect:bounds].CGPath;
    }
    [self updateLeftDisplacement];
    [self updateRightDisplacement];

    [super frameSizeChanged:frame bounds:bounds];
}

//API

-(void)setCenterView_:(id)args
{
    BOOL odlCenter = centerView != nil;
    UIViewController* ctlr;
    if ([args isKindOfClass:[UIViewController class]]) {
        ctlr = args;
    }
    else {
        ENSURE_UI_THREAD(setCenterView_,args);
        ENSURE_TYPE_OR_NIL(args,TiViewProxy);
        
        RELEASE_TO_NIL(centerView);
        centerView = [args retain];
        ctlr = [self controllerForViewProxy:centerView];
    }
    if ([centerView isKindOfClass:[TiUIWindowProxy class]]) {
        TiUIWindowProxy* window = (TiUIWindowProxy*)centerView;
        [window setIsManaged:YES];
    }
    
    [self clearGestures];
    [_lockingView removeFromSuperview];
    [[ctlr view] setFrame:[self bounds]];
    [[self controller] setFrontViewController:ctlr animated:odlCenter];
    
    _lastUpdatePanningMode = -1;
    [self updatePanningModeOnController:ctlr];
    
}

-(void)setLeftView_:(id)args
{
    ENSURE_UI_THREAD(setLeftView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
    
	RELEASE_TO_NIL(leftView);
    leftView = [args retain];
    UIViewController* ctlr = [self controllerForViewProxy:leftView];
//    [[ctlr view] setFrame:[self bounds]];
    [self controller].rearViewController = ctlr;
    [_leftViewFadingView removeFromSuperview];
}

-(void)setRightView_:(id)args
{
    ENSURE_UI_THREAD(setRightView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
  
	RELEASE_TO_NIL(rightView);
    rightView = [args retain];
    UIViewController* ctlr = [self controllerForViewProxy:leftView];
    [self controller].rightViewController = ctlr;
    [_rightViewFadingView removeFromSuperview];
}

-(void)setLeftViewWidth_:(id)args
{
    ENSURE_UI_THREAD(setLeftViewWidth_,args);
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    
    CGFloat value = [args floatValue];
    [self controller].rearViewRevealWidth = value;
    [self updateLeftDisplacement];
}

-(void)setRightViewWidth_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    ENSURE_UI_THREAD(setRightViewWidth_,args);
    
    CGFloat value = [args floatValue];
    [self controller].rightViewRevealWidth = value;
    [self updateRightDisplacement];
}

-(void)updateLeftDisplacement
{
    CGFloat leftMenuWidth = [self controller].rearViewRevealWidth;
    if (leftMenuWidth < 0) {
        leftMenuWidth = [self controller].view.bounds.size.width + leftMenuWidth;
    }
    [self controller].rearViewRevealDisplacement = TiDimensionCalculateValue(_leftScrollScale, leftMenuWidth);
}

-(void)updateRightDisplacement
{
    CGFloat rightMenuWidth = [self controller].rightViewRevealWidth;
    if (rightMenuWidth < 0) {
        rightMenuWidth = [self controller].view.bounds.size.width + rightMenuWidth;
    }
    [self controller].rightViewRevealDisplacement = TiDimensionCalculateValue(_rightScrollScale, rightMenuWidth);
}

-(void)setLeftViewDisplacement_:(id)args
{
    _leftScrollScale = [TiUtils dimensionValue:args];
    [self updateLeftDisplacement];
}

-(void)setRightViewDisplacement_:(id)args
{
    _rightScrollScale = [TiUtils dimensionValue:args];
    [self updateRightDisplacement];
}

-(void)setFading_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    _fadeDegree = [args floatValue];
    if (_fadeDegree == 0.0f) {
        [_leftViewFadingView removeFromSuperview];
        [_rightViewFadingView removeFromSuperview];
    }
    else {
        if (_leftViewFadingView == nil) {
            _leftViewFadingView = [[UIView alloc] initWithFrame:[self bounds]];
            _leftViewFadingView.backgroundColor = [UIColor blackColor];
            _leftViewFadingView.alpha = 0.0f;
            _leftViewFadingView.userInteractionEnabled = NO;
            _leftViewFadingView.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
        }
        if (_rightViewFadingView == nil) {
            _rightViewFadingView = [[UIView alloc] initWithFrame:[self bounds]];
            _rightViewFadingView.backgroundColor = [UIColor blackColor];
            _rightViewFadingView.alpha = 0.0f;
            _rightViewFadingView.userInteractionEnabled = NO;
            _rightViewFadingView.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
        }
    }
}

-(id) navControllerForController:(UIViewController*)theController
{
    if ([theController transitionController] != nil)
        return [theController transitionController];
    return [theController navigationController];
}

-(void)clearGestures
{
    UIGestureRecognizer* gesture = [self controller].panGestureRecognizer;
    UIViewController* localcontroller = nil;
    if ([self controller].frontViewController) localcontroller = [self controller].frontViewController;
    else localcontroller = [self controller];
    if ([localcontroller isKindOfClass:[UINavigationController class]] ||
        [localcontroller isKindOfClass:[ADTransitionController class]])
        [[localcontroller navigationBar] removeGestureRecognizer:gesture];
    else
        [[[self navControllerForController:localcontroller] navigationBar] removeGestureRecognizer:gesture];
    [[localcontroller view] removeGestureRecognizer:gesture];
}

-(void) updatePanningModeOnController:(UIViewController*) localcontroller
{
    if (_lastUpdatePanningMode == panningMode) return;
    _lastUpdatePanningMode = panningMode;
    

//    [self controller].grabbableBorderAmount = -1.0f;
    if (panningMode != PanningModeNone)
    {
        UIGestureRecognizer* gesture = [self controller].panGestureRecognizer;
        if (panningMode == PanningModeBorders)
        {
            [[localcontroller view] addGestureRecognizer:gesture];
            [self controller].draggableBorderWidth = 50.0f;
        }
        else if (panningMode == PanningModeNavBar)
        {
            if ([localcontroller isKindOfClass:[UINavigationController class]] ||
                [localcontroller isKindOfClass:[ADTransitionController class]])
                [[localcontroller navigationBar] addGestureRecognizer:gesture];
            else
                [[[self navControllerForController:localcontroller] navigationBar] addGestureRecognizer:gesture];
        }
        else// PanningModeCenterView
        {
            [[localcontroller view] addGestureRecognizer:gesture];
        }

    }
}

//Properties
- (void)setPanningMode_:(id)args
{
    ENSURE_UI_THREAD(setPanningMode_,args);
    if(args !=nil){
        int num = [TiUtils intValue:args];
        switch(num){
            case 0: // MENU_PANNING_NONE
                panningMode = PanningModeNone;
                break;
            case 1: // MENU_PANNING_ALL_VIEWS
                break;
            case 3: // MENU_PANNING_BORDERS
                panningMode = PanningModeBorders;
                break;
            case 4: // MENU_PANNING_NAV_BAR
                panningMode = PanningModeNavBar;
                break;
            case 2: // MENU_PANNING_CENTER_VIEW
            default:
                panningMode = PanningModeCenterView;
                break;
        }
    }
    if (panningMode != _lastUpdatePanningMode) {
        [self clearGestures];
        [self updatePanningModeOnController:[[self controller] frontViewController]];
    }
}

- (void)setShadowWidth:(id)args
{
    ENSURE_TYPE_OR_NIL(args, NSNumber);
    [self controller].frontViewShadowRadius = [args floatValue];
}


-(BOOL)rightViewOpened:(SWRevealViewController *)revealController
{
    return (revealController.frontViewPosition == FrontViewPositionLeftSide ||
            revealController.frontViewPosition == FrontViewPositionLeftSideMost ||
            revealController.frontViewPosition == FrontViewPositionLeftSideMostRemoved);
}

- (void)revealController:(SWRevealViewController *)revealController willMoveToPosition:(FrontViewPosition)position withDuration:(NSTimeInterval)duration
{
    if (position == FrontViewPositionLeft) {
        
        if ([self.proxy _hasListeners:@"closemenu"])
        {
            NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT([self rightViewOpened:revealController]?1:0), @"side",
                                 NUMFLOAT(duration), @"duration",
                                 NUMBOOL(duration > 0), @"animated", nil];
            [self.proxy fireEvent:@"closemenu" withObject:evt];
        }
    }
    else {
        if ([self.proxy _hasListeners:@"openmenu"])
        {
            NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT([self rightViewOpened:revealController]?1:0), @"side",
                                 NUMFLOAT(duration), @"duration",
                                 NUMBOOL(duration > 0), @"animated", nil];
            [self.proxy fireEvent:@"openmenu" withObject:evt];
        }
    }
}

-(void)revealController:(SWRevealViewController *)revealController didMoveToPosition:(FrontViewPosition)position
{
    if (revealController.frontViewPosition == FrontViewPositionLeftSide ||
        revealController.frontViewPosition == FrontViewPositionLeftSideMost ||
        revealController.frontViewPosition == FrontViewPositionRight ||
        revealController.frontViewPosition == FrontViewPositionRightMost) {
        _lockingView.frame = revealController.frontViewController.view.bounds;
        [revealController.frontViewController.view addSubview:_lockingView];
    }
    else
        [_lockingView removeFromSuperview];
}

- (void)revealController:(SWRevealViewController *)revealController animateToPosition:(FrontViewPosition)position
{
    if (_leftViewFadingView != nil) {
        _leftViewFadingView.alpha = (position == FrontViewPositionRight ||
                                      position == FrontViewPositionRightMost ||
                                      position == FrontViewPositionRightMostRemoved)?0.0f:_fadeDegree;
    }
    if (_rightViewFadingView != nil) {
        _rightViewFadingView.alpha = (position == FrontViewPositionLeftSide ||
                                     position == FrontViewPositionLeftSideMost ||
                                     position == FrontViewPositionLeftSideMostRemoved)?0.0f:_fadeDegree;
    }
    
}

- (void)revealController:(SWRevealViewController *)revealController frontViewPosition:(CGFloat)position
{
    
}

- (void)revealControllerPanGestureBegan:(SWRevealViewController *)revealController
{
    if ([self.proxy _hasListeners:@"scrollstart"])
    {
        NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMFLOAT(0), @"offset", nil];
        [self.proxy fireEvent:@"scrollstart" withObject:evt];
    }
}
- (void)revealControllerPanGestureChanged:(SWRevealViewController *)revealController withOffset:(CGFloat) xPosition
{
    if (_fadeDegree > 0.0f) {
        if (xPosition > 0) {
            CGFloat leftMenuWidth = revealController.rearViewRevealWidth;
            if (leftMenuWidth < 0) {
                leftMenuWidth = revealController.view.bounds.size.width + leftMenuWidth;
            }
            CGFloat percentage = MIN(xPosition / leftMenuWidth, 1.0f);
            if (_leftViewFadingView.superview == nil) {
                _leftViewFadingView.frame = revealController.rearViewController.view.bounds;
                [revealController.rearViewController.view addSubview:_leftViewFadingView];
            }
            _leftViewFadingView.alpha = (1 - percentage)*_fadeDegree;
        }
    }
    if ([self.proxy _hasListeners:@"scroll"])
    {
        NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMFLOAT(xPosition), @"offset", nil];
        [self.proxy fireEvent:@"scroll" withObject:evt];
    }
}
- (void)revealControllerPanGestureEnded:(SWRevealViewController *)revealController withOffset:(CGFloat) xPosition
{
    if ([self.proxy _hasListeners:@"scrollend"])
    {
        NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMFLOAT(xPosition), @"offset", nil];
        [self.proxy fireEvent:@"scrollend" withObject:evt];
    }
}

@end