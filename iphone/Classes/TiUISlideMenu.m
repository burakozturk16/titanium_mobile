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
//#import "TiUISlideFakeWindowProxy.h"
#import "UIViewController+ADTransitionController.h"






@implementation TiUISlideMenu

-(UIViewController *) controllerForViewProxy:(TiViewProxy * )proxy
{
        [[proxy getOrCreateView] setAutoresizingMask:UIViewAutoresizingNone];
        [proxy windowWillOpen];
        [proxy windowDidOpen];
        [proxy reposition];
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
        shadowWidth = 5;
        panningMode = PanningModeFullscreen;
        _lastUpdatePanningMode = -1;
    }
    return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(controller);
	RELEASE_TO_NIL(shadowLayer);
	RELEASE_TO_NIL(leftView);
	RELEASE_TO_NIL(rightView);
	RELEASE_TO_NIL(centerView);
    [super dealloc];
}

-(ECSlidingViewController*)controller
{
	if (controller==nil)
	{
        controller = [[ECSlidingViewController alloc] init];
        
        
        controller.shouldAddPanGestureRecognizerToTopViewSnapshot = YES;
        UIView * controllerView = [controller view];
        [self setBackgroundColor:[UIColor clearColor]];
        [controllerView setBackgroundColor:[UIColor clearColor]];
        [controllerView setFrame:[self bounds]];
        [self addSubview:controllerView];
        
        controller.delegate = [self proxy];
        
        [self setCenterView_:[[[UIViewController alloc] init] autorelease]];
        
        [controller setAnchorLeftPeekAmount:40.0f];
        [controller setAnchorRightPeekAmount:40.0f];
        
        [controller setUnderLeftWidthLayout:ECVariableRevealWidth];
        [controller setUnderRightWidthLayout:ECVariableRevealWidth];
        
        [controller viewWillAppear:NO];
        [controller viewDidAppear:NO];
	}
	return controller;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    if ([self controller].topViewController)
    {
        [[[self controller].topViewController view] setFrame:bounds];
        [[self controller].topViewController view].layer.shadowPath = [UIBezierPath bezierPathWithRect:bounds].CGPath;
    }
    [super frameSizeChanged:frame bounds:bounds];
}

//API

-(void)setCenterView_:(id)args
{
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
    
    [self clearGestures];
    [self controller].topViewController = ctlr;
    
    _lastUpdatePanningMode = -1;
    [self updatePanningMode];
    
    ctlr.view.layer.shadowOpacity = 0.9f;
    ctlr.view.layer.shadowRadius = shadowWidth;
    ctlr.view.layer.shadowColor = [UIColor blackColor].CGColor;
}

-(void)setLeftView_:(id)args
{
    ENSURE_UI_THREAD(setLeftView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
    
	RELEASE_TO_NIL(leftView);
    leftView = [args retain];
    [self controller].underLeftViewController = [self controllerForViewProxy:leftView];
}

-(void)setRightView_:(id)args
{
    ENSURE_UI_THREAD(setRightView_,args);
    ENSURE_TYPE_OR_NIL(args,TiViewProxy);
    
	RELEASE_TO_NIL(rightView);
    rightView = [args retain];
    [self controller].underRightViewController = [self controllerForViewProxy:rightView];
}

-(void)setLeftViewWidth_:(id)args
{
    ENSURE_UI_THREAD(setLeftViewWidth_,args);
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    
    CGFloat value = [args floatValue];
    
    if (value >0)
    {
        [[self controller] setAnchorRightRevealAmount:value];
        [[self controller] setUnderLeftWidthLayout:ECFixedRevealWidth];
    }
    else
    {
        [[self controller] setAnchorRightPeekAmount:-value];
        [[self controller] setUnderLeftWidthLayout:ECVariableRevealWidth];
    }
}

-(void)setRightViewWidth_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,NSNumber);
    ENSURE_UI_THREAD(setRightViewWidth_,args);
    
    CGFloat value = [args floatValue];
    
    if (value >0)
    {
        [[self controller] setAnchorLeftRevealAmount:value];
        [[self controller] setUnderRightWidthLayout:ECFixedRevealWidth];
    }
    else
    {
        [[self controller] setAnchorLeftPeekAmount:-value];
        [[self controller] setUnderRightWidthLayout:ECVariableRevealWidth];
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
    UIViewController* localcontroller = nil;
    if ([self controller].topViewController) localcontroller = [self controller].topViewController;
    else localcontroller = [self controller];
    if ([localcontroller isKindOfClass:[UINavigationController class]] ||
        [localcontroller isKindOfClass:[ADTransitionController class]])
        [[localcontroller navigationBar] removeGestureRecognizer:[self controller].panGesture];
    else
        [[[self navControllerForController:localcontroller] navigationBar] removeGestureRecognizer:[self controller].panGesture];
    [[localcontroller view] removeGestureRecognizer:[self controller].panGesture];
}

-(void) updatePanningMode
{
    if (_lastUpdatePanningMode == panningMode) return;
    _lastUpdatePanningMode = panningMode;
    UIViewController* localcontroller = nil;
    if ([self controller].topViewController) localcontroller = [self controller].topViewController;
    else localcontroller = [self controller];

    [self controller].grabbableBorderAmount = -1.0f;
    if (panningMode != PanningModeNone)
    {
        if (panningMode == PanningModeBorders)
        {
            [[localcontroller view] addGestureRecognizer:[self controller].panGesture];
            [self controller].grabbableBorderAmount = 50.0f;
        }
        else if (panningMode == PanningModeNavBar)
        {
            if ([localcontroller isKindOfClass:[UINavigationController class]] ||
                [localcontroller isKindOfClass:[ADTransitionController class]])
                [[localcontroller navigationBar] addGestureRecognizer:[self controller].panGesture];
            else
                [[[self navControllerForController:localcontroller] navigationBar] addGestureRecognizer:[self controller].panGesture];
        }
        else if (panningMode == PanningModeFullscreen)
        {
            [[localcontroller view] addGestureRecognizer:[self controller].panGesture];
        }
        else if (panningMode == PanningModeNonScrollView)
        {
            [[localcontroller view] addGestureRecognizer:[self controller].panGesture];
            [self controller].disableOnScrollView = YES;
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
            case 5: // MENU_PANNING_NON_SCROLLVIEW
                panningMode = PanningModeNonScrollView;
                break;
            case 2: // MENU_PANNING_CENTER_VIEW
            default:
                panningMode = PanningModeFullscreen;
                break;
        }
    }
    if (panningMode != _lastUpdatePanningMode) {
        [self clearGestures];
        [self updatePanningMode];
    }
}

- (void)setShadowWidth:(id)args
{
    ENSURE_TYPE_OR_NIL(args, NSNumber);
    shadowWidth = [args floatValue];
    if ([self controller].topViewController != nil)
        [self controller].topViewController.view.layer.shadowRadius = shadowWidth;
}
 
 // applies a small shadow
//-(void)viewDeckController:(IIViewDeckController *)viewDeckController applyShadow:(CALayer *)layer withBounds:(CGRect)rect {
//	RELEASE_TO_NIL(shadowLayer);
//    shadowLayer = [layer retain];
//     layer.masksToBounds = NO;
//     layer.shadowRadius = shadowWidth;
//     layer.shadowOpacity = 0.9;
//     layer.shadowColor = [[UIColor blackColor] CGColor];
//     layer.shadowOffset = CGSizeZero;
//     layer.shadowPath = [[UIBezierPath bezierPathWithRect:rect] CGPath];
//}



@end