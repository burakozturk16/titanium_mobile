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
#import "TiUISlideMenuVisualState.h"
#import "TiTransition.h"

@interface TiUISlideMenu()
{
@private
	SlideMenuDrawerController *_controller;    
    TiViewProxy* leftView;
    TiViewProxy* rightView;
    TiViewProxy* centerView;
    TiDimension _leftScrollScale;
    TiDimension _rightScrollScale;
    TiDimension _leftViewWidth;
    TiDimension _rightViewWidth;
    TiTransition* _leftTransition;
    TiTransition* _rightTransition;
}
@end

@implementation TiUISlideMenu

-(UIViewController *) controllerForViewProxy:(TiViewProxy * )proxy withFrame:(CGRect)frame
{
    proxy.sandboxBounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
    
    UIViewController* controller;
        if([proxy respondsToSelector:@selector(hostingController)])
    {
        controller =  [(TiWindowProxy *)proxy hostingController];
    }
    else {
        controller =  [[[TiViewController alloc] initWithViewProxy:proxy] autorelease];
    }
    [proxy windowWillOpen];
    [proxy layoutChildren:NO];
    [proxy windowDidOpen];
    return controller;
}

-(id)init
{
    if (self = [super init])
    {
        _leftScrollScale = TiDimensionDip(0.0f);
        _rightScrollScale = TiDimensionDip(0.0f);
        _leftViewWidth = TiDimensionDip(200.0f);
        _rightViewWidth = TiDimensionDip(200.0f);
    }
    return self;
}

-(id)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame])
    {
        _leftScrollScale = TiDimensionDip(0.0f);
        _rightScrollScale = TiDimensionDip(0.0f);
        _leftViewWidth = TiDimensionDip(200.0f);
        _rightViewWidth = TiDimensionDip(200.0f);
    }
    return self;
}


-(void)dealloc
{
	RELEASE_TO_NIL(_controller);
	RELEASE_TO_NIL(leftView);
	RELEASE_TO_NIL(rightView);
	RELEASE_TO_NIL(centerView);
    RELEASE_TO_NIL(_leftTransition);
    RELEASE_TO_NIL(_rightTransition);
    [super dealloc];
}

-(SlideMenuDrawerController*)controller
{
	if (_controller==nil)
	{
        _controller = [[SlideMenuDrawerController alloc] initWithNibName:nil bundle:nil];
        _controller.proxy = self.proxy;
        
        UIView * controllerView = [_controller view];
        [controllerView setFrame:[self bounds]];
        [self addSubview:controllerView];
        _controller.openDrawerGestureModeMask = MMOpenDrawerGestureModePanningCenterView;
        _controller.closeDrawerGestureModeMask = MMCloseDrawerGestureModePanningCenterView;

	}
	return _controller;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [self update];

    [super frameSizeChanged:frame bounds:bounds];
}

//API

-(void)setCenterView_:(id)args
{
    BOOL odlCenter = centerView != nil;
    UIViewController* ctlr;
    CGRect frame = [[self controller] childControllerContainerViewFrame];
    if ([args isKindOfClass:[UIViewController class]]) {
        ctlr = args;
    }
    else {
        ENSURE_UI_THREAD(setCenterView_,args);
        ENSURE_TYPE_OR_NIL(args,TiViewProxy);
        
        if (args == centerView) {
            [[self controller] closeDrawerAnimated:YES completion:nil];
            return;
        }
        
        RELEASE_TO_NIL(centerView);
        centerView = [args retain];
        ctlr = [self controllerForViewProxy:centerView withFrame:frame];
    }
    if ([centerView isKindOfClass:[TiUIWindowProxy class]]) {
        TiUIWindowProxy* window = (TiUIWindowProxy*)centerView;
        [window updateOrientationModes];
        [window setIsManaged:YES];
    }
    
    [[self controller] setCenterViewController:ctlr withFullCloseAnimation:YES completion:nil];
}

-(void)setLeftView_:(id)args
{
    ENSURE_UI_THREAD(setLeftView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
    
	RELEASE_TO_NIL(leftView);
    if (args != nil) {
        leftView = [args retain];
        CGRect frame = [[self controller] childControllerContainerViewFrame];
        frame.size.width = [self controller].maximumLeftDrawerWidth;
        [self controller].leftDrawerViewController = [self controllerForViewProxy:leftView withFrame:frame];
    }
    else {
        [self controller].leftDrawerViewController = nil;
    }
}

-(void)setRightView_:(id)args
{
    ENSURE_UI_THREAD(setRightView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
    
	RELEASE_TO_NIL(rightView);
    if (args != nil) {
        rightView = [args retain];
        CGRect frame = [[self controller] childControllerContainerViewFrame];
        frame.size.width = [self controller].maximumRightDrawerWidth;
        [self controller].rightDrawerViewController = [self controllerForViewProxy:rightView withFrame:frame];
    }
    else {
        [self controller].rightDrawerViewController = nil;
    }
}

-(void)setLeftViewWidth_:(id)args
{
    _leftViewWidth = [TiUtils dimensionValue:args];
    [self updateLeftViewWidth];
}

-(void)setRightViewWidth_:(id)args
{
    _rightViewWidth = [TiUtils dimensionValue:args];
    [self updateRightViewWidth];
}

-(void)update
{
    [self updateLeftViewWidth];
    [self updateRightDisplacement];
}

-(void)updateLeftViewWidth
{
    [self controller].maximumLeftDrawerWidth = TiDimensionCalculateValue(_leftViewWidth, self.bounds.size.width);
    [self updateLeftDisplacement];
}

-(void)updateRightViewWidth
{
    [self controller].maximumRightDrawerWidth = TiDimensionCalculateValue(_rightViewWidth, self.bounds.size.width);
    [self updateRightDisplacement];
}

-(void)updateLeftDisplacement
{
    CGFloat leftMenuWidth = [self controller].maximumLeftDrawerWidth;

    [self controller].leftDisplacement = -TiDimensionCalculateValue(_leftScrollScale, leftMenuWidth);
}

-(void)updateRightDisplacement
{
    CGFloat rightMenuWidth = [self controller].maximumRightDrawerWidth;
    [self controller].rightDisplacement = -TiDimensionCalculateValue(_rightScrollScale, rightMenuWidth);
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
    [self controller].fadeDegree = [args floatValue];
}

-(id) navControllerForController:(UIViewController*)theController
{
    if ([theController transitionController] != nil)
        return [theController transitionController];
    return [theController navigationController];
}

//Properties
- (void)setPanningMode_:(id)args
{
    ENSURE_UI_THREAD(setPanningMode_,args);
    if(args !=nil){
        int num = [TiUtils intValue:args];
        [self controller].openDrawerGestureModeMask = [TiUtils intValue:args];
    }
}

- (void)setShadowWidth_:(id)args
{
    ENSURE_TYPE_OR_NIL(args, NSNumber);
    [self controller].showsShadow = ([args floatValue] != 0.0f);
}

-(void)setLeftTransition_:(id)value
{
    ENSURE_SINGLE_ARG_OR_NIL(value, NSDictionary)
    RELEASE_TO_NIL(_leftTransition);
    _leftTransition = [TiTransitionHelper transitionFromArg:value containerView:self];
    if (_leftTransition) {
        MMDrawerControllerDrawerVisualStateBlock visualStateBlock =
        ^(MMDrawerController * drawerController, MMDrawerSide drawerSide, CGFloat percentVisible){
            if(percentVisible <= 1.f){
                CGFloat maxDrawerWidth = MAX(drawerController.maximumLeftDrawerWidth,drawerController.visibleLeftDrawerWidth);
                [_leftTransition transformView:drawerController.leftDrawerViewController.view withPosition:percentVisible-1];
            }
        };

        [self controller].leftVisualBlock = visualStateBlock;
    }
	else [self controller].leftVisualBlock = nil;
}


-(void)setRightTransition_:(id)value
{
    ENSURE_SINGLE_ARG_OR_NIL(value, NSDictionary)
    RELEASE_TO_NIL(_rightTransition);
    _rightTransition = [TiTransitionHelper transitionFromArg:value containerView:self];
    if (_rightTransition) {
        MMDrawerControllerDrawerVisualStateBlock visualStateBlock =
        ^(MMDrawerController * drawerController, MMDrawerSide drawerSide, CGFloat percentVisible){
            if(percentVisible <= 1.f){
                CGFloat maxDrawerWidth = MAX(drawerController.maximumRightDrawerWidth,drawerController.visibleRightDrawerWidth);
                [_rightTransition transformView:drawerController.rightDrawerViewController.view withPosition:1-percentVisible];
            }
        };
        
        [self controller].rightVisualBlock = visualStateBlock;
    }
	else [self controller].rightVisualBlock = nil;
}

@end