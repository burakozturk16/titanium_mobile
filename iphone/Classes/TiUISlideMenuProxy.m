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

-(void)windowDidOpen {
	[super windowDidOpen];
	[self reposition];
}

-(SWRevealViewController *)_controller {
	return [(TiUISlideMenu*)[self view] controller];
}

-(TiUIView*)newView {
    TiUISlideMenu* menu = [[TiUISlideMenu alloc] init];
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(underLeftWillAppear:)
//                                                 name:ECSlidingViewUnderLeftWillAppear
//                                               object:[menu controller]];
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(underLeftWillDisappear:)
//                                                 name:ECSlidingViewUnderLeftWillDisappear
//                                               object:[menu controller]];
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(topDidAnchorRight:)
//                                                 name:ECSlidingViewTopDidAnchorRight
//                                               object:[menu controller]];
    
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(underRightWillAppear:)
//                                                 name:ECSlidingViewUnderRightWillAppear
//                                               object:[menu controller]];
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(underRightWillDisappear:)
//                                                 name:ECSlidingViewUnderRightWillDisappear
//                                               object:[menu controller]];
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(topDidAnchorLeft:)
//                                                 name:ECSlidingViewTopDidAnchorLeft
//                                               object:[menu controller]];
//    
//    [[NSNotificationCenter defaultCenter] addObserver:self
//                                             selector:@selector(topDidReset:)
//                                                 name:ECSlidingViewTopDidReset
//                                               object:[menu controller]];
	return menu;
}

-(void)_configure
{
	[super _configure];
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
    UIViewController* topVC = [[self _controller] frontViewController];
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
    UIViewController* topVC = [[self _controller] frontViewController];
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
    UIViewController* topVC = [[self _controller] frontViewController];
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
    return NUMFLOAT([self _controller].rearViewRevealWidth);
}

-(id)getRealRightViewWidth:(id)args
{
    return NUMFLOAT([self _controller].rightViewRevealWidth);
}

-(void)toggleLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
        
    [[self _controller] revealToggleAnimated:animated];}
-(void)toggleRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    
    [[self _controller] rightRevealToggleAnimated:animated];

}

-(void)openLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
//    [self willShowSide:0 animated:animated];
  [[self _controller] setFrontViewPosition:FrontViewPositionRight animated:animated];
}

-(void)openRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
//    [self willShowSide:1 animated:animated];
    [[self _controller] setFrontViewPosition:FrontViewPositionLeftSide animated:animated];
}

-(void)closeLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
//    [self willHideSide:0 animated:animated];
    [[self _controller] setFrontViewPosition:FrontViewPositionLeft animated:animated];
}

-(void)closeRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
//    [self willHideSide:1 animated:animated];
    [[self _controller] setFrontViewPosition:FrontViewPositionLeft animated:animated];
}

@end
