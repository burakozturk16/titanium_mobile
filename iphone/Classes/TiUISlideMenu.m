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

@interface TiUISlideMenu()
{
@private
	SlideMenuDrawerController *_controller;    
    TiViewProxy* leftView;
    TiViewProxy* rightView;
    TiViewProxy* centerView;
    TiDimension _leftScrollScale; //between 0.0f and 1.0f
    TiDimension _rightScrollScale; //between 0.0f and 1.0f
}
@end

@implementation TiUISlideMenu

-(UIViewController *) controllerForViewProxy:(TiViewProxy * )proxy withFrame:(CGRect)frame
{
    [proxy getOrCreateView];
    proxy.sandboxBounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
    [proxy windowWillOpen];
    [proxy layoutChildren:NO];
    [proxy windowDidOpen];
    UIViewController* controller;
        if([proxy respondsToSelector:@selector(hostingController)])
    {
        controller =  [(TiWindowProxy *)proxy hostingController];
    }
    else {
        controller =  [[[TiViewController alloc] initWithViewProxy:proxy] autorelease];
    }    
    return controller;
}

-(id)init
{
    if (self = [super init])
    {
        _leftScrollScale = TiDimensionDip(0.0f);
        _rightScrollScale = TiDimensionDip(0.0f);
    }
    return self;
}

-(id)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame])
    {
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
    [self updateLeftDisplacement];
    [self updateRightDisplacement];

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
        [window setIsManaged:YES];
    }
    
    [[self controller] setCenterViewController:ctlr withFullCloseAnimation:YES completion:nil];
}

-(void)setLeftView_:(id)args
{
    ENSURE_UI_THREAD(setLeftView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
    
	RELEASE_TO_NIL(leftView);
    leftView = [args retain];
    CGRect frame = [[self controller] childControllerContainerViewFrame];
    UIViewController* ctlr = [self controllerForViewProxy:leftView withFrame:frame];
    [self controller].leftDrawerViewController = ctlr;
}

-(void)setRightView_:(id)args
{
    ENSURE_UI_THREAD(setRightView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
  
	RELEASE_TO_NIL(rightView);
    rightView = [args retain];
    CGRect frame = [[self controller] childControllerContainerViewFrame];
    UIViewController* ctlr = [self controllerForViewProxy:leftView withFrame:frame];
    [self controller].rightDrawerViewController = ctlr;
}

-(void)setLeftViewWidth_:(id)args
{
    ENSURE_UI_THREAD(setLeftViewWidth_,args);
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    
    CGFloat value = [args floatValue];
    [self controller].maximumLeftDrawerWidth = value;
    [self updateLeftDisplacement];
}

-(void)setRightViewWidth_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    ENSURE_UI_THREAD(setRightViewWidth_,args);
    
    CGFloat value = [args floatValue];
    [self controller].maximumRightDrawerWidth = value;
    [self updateRightDisplacement];
}

-(void)updateLeftDisplacement
{
    CGFloat leftMenuWidth = [self controller].maximumLeftDrawerWidth;

    [self controller].leftDisplacement = TiDimensionCalculateValue(_leftScrollScale, leftMenuWidth);
}

-(void)updateRightDisplacement
{
    CGFloat rightMenuWidth = [self controller].maximumRightDrawerWidth;
    [self controller].rightDisplacement = TiDimensionCalculateValue(_rightScrollScale, rightMenuWidth);
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

- (void)setLeftAnimation_:(id)args
{
    ENSURE_TYPE_OR_NIL(args, NSNumber);
    int mode = [TiUtils intValue:args];
    MMDrawerControllerDrawerVisualStateBlock block = nil;
    switch (mode) {
        case 1:
            block = [TiUISlideMenuVisualState slideAndScaleVisualStateBlock];
            break;
        case 2:
            block = [TiUISlideMenuVisualState swingingDoorVisualStateBlock];
            break;
        case 3:
            block = [TiUISlideMenuVisualState slideVisualStateBlock];
            break;
        case 0:
        default:
            break;
    }
    [self controller].leftVisualBlock = block;
}
@end