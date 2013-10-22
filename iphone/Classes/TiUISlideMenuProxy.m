/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUISlideMenuProxy.h"
#import "TiUISlideMenu.h"

#import "TiBase.h"
#import "TiUtils.h"
#import "TiApp.h"

@implementation TiUISlideMenuProxy

-(id)init
{
	if ((self = [super init]))
	{
        [self setDefaultReadyToCreateView:YES];
	}
	return self;
}

-(SlideMenuDrawerController *)_controller {
	return [(TiUISlideMenu*)[self view] controller];
}

-(TiUIView*)newView {
    CGRect frame = [TiUtils appFrame];
    TiUISlideMenu* menu = [[TiUISlideMenu alloc] initWithFrame:frame];
	return menu;
}

#pragma mark - TiOrientationController

-(TiOrientationFlags) orientationFlags
{
    UIViewController* topVC = [[self _controller] centerViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiWindowProxy * thisProxy = (TiWindowProxy *)[(TiViewController *)topVC proxy];
        if ([thisProxy conformsToProtocol:@protocol(TiOrientationController)]) {
            TiOrientationFlags result = [thisProxy orientationFlags];
            if (result != TiOrientationNone)
            {
                return result;
            }
        }
    }
    return [super orientationFlags];
}

#pragma mark - TiWindowProtocol
-(void)viewWillAppear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[self _controller] viewWillAppear:animated];
    }
    [super viewWillAppear:animated];
}
-(void)viewWillDisappear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[self _controller] viewWillDisappear:animated];
    }
    [super viewWillDisappear:animated];
}

-(void)viewDidAppear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[self _controller] viewDidAppear:animated];
    }
    [super viewDidAppear:animated];
}
-(void)viewDidDisappear:(BOOL)animated
{
    if ([self viewAttached]) {
        [[self _controller] viewDidDisappear:animated];
    }
    [super viewDidDisappear:animated];
    
}

-(BOOL) hidesStatusBar
{
    UIViewController* topVC = [[self _controller] centerViewController];
    if ([topVC isKindOfClass:[TiViewController class]]) {
        TiViewProxy* theProxy = [(TiViewController*)topVC proxy];
        if ([theProxy conformsToProtocol:@protocol(TiWindowProtocol)]) {
            return [(id<TiWindowProtocol>)theProxy hidesStatusBar];
        }
    }
    return [super hidesStatusBar];
}

-(void)gainFocus
{
    UIViewController* topVC = [[self _controller] centerViewController];
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
    UIViewController* topVC = [[self _controller] centerViewController];
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
        [[self _controller] willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    [super willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if ([self viewAttached]) {
        [[self _controller] willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
    }
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}
-(void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    if ([self viewAttached]) {
        [[self _controller] didRotateFromInterfaceOrientation:fromInterfaceOrientation];
    }
    [super didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

//API

-(id)getRealLeftViewWidth:(id)args
{
    return NUMFLOAT([self _controller].maximumLeftDrawerWidth);
}

-(id)getRealRightViewWidth:(id)args
{
    return NUMFLOAT([self _controller].maximumRightDrawerWidth);
}

-(void)toggleLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
        
    [[self _controller] toggleDrawerSide:MMDrawerSideLeft animated:animated completion:nil];
}
-(void)toggleRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    
    [[self _controller] toggleDrawerSide:MMDrawerSideRight animated:animated completion:nil];

}

-(void)openLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
  [[self _controller] openDrawerSide:MMDrawerSideLeft animated:animated completion:nil];
}

-(void)openRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    [[self _controller] openDrawerSide:MMDrawerSideRight animated:animated completion:nil];
}

-(void)closeLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    [[self _controller] closeDrawerAnimated:animated completion:nil];
}

-(void)closeRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    [[self _controller] closeDrawerAnimated:animated completion:nil];
}

@end
