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
        readyToCreateView = YES;
	}
	return self;
}

-(void)windowDidOpen {
	[super windowDidOpen];
	[self reposition];
}

-(ECSlidingViewController *)_controller {
	return [(TiUISlideMenu*)[self view] controller];
}

-(TiUIView*)newView {
    TiUISlideMenu* menu = [[TiUISlideMenu alloc] init];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(underLeftWillAppear:)
                                                 name:ECSlidingViewUnderLeftWillAppear
                                               object:[menu controller]];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(underLeftWillDisappear:)
                                                 name:ECSlidingViewUnderLeftWillDisappear
                                               object:[menu controller]];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(topDidAnchorRight:)
                                                 name:ECSlidingViewTopDidAnchorRight
                                               object:[menu controller]];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(underRightWillAppear:)
                                                 name:ECSlidingViewUnderRightWillAppear
                                               object:[menu controller]];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(underRightWillDisappear:)
                                                 name:ECSlidingViewUnderRightWillDisappear
                                               object:[menu controller]];
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

// Prevents dumb visual glitches - see 4619
//-(void)ignoringRotationToOrientation:(UIInterfaceOrientation)orientation
//{
//    if (![[[TiApp app] controller] isTopWindow:self]) {
//        [[self _controller] arrangeViewsAfterRotation];
//    }
//}


-(void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    [[self _controller] willAnimateRotationToInterfaceOrientation:toInterfaceOrientation duration:duration];
}

- (void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    [[self _controller] willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
}

- (void)didRotateFromInterfaceOrientation:(UIInterfaceOrientation)fromInterfaceOrientation
{
    [[self _controller] didRotateFromInterfaceOrientation:fromInterfaceOrientation];
}

- (void)viewWillAppear:(BOOL)animated
{
    [[self _controller] viewWillAppear:animated];
}

- (void)viewDidAppear:(BOOL)animated
{
    [[self _controller] viewDidAppear:animated];
}

- (void)viewWillDisappear:(BOOL)animated
{
    [[self _controller] viewWillDisappear:animated];
}

- (void)viewDidDisappear:(BOOL)animated
{
    [[self _controller] viewDidDisappear:animated];
}


//API
-(void)toggleLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
        
    if ([self _controller].underLeftShowing)
        [[self _controller] resetTopView:animated];
    else
        [[self _controller] anchorTopViewTo:ECRight animated:animated];
}
-(void)toggleRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    
    if ([self _controller].underRightShowing)
        [[self _controller] resetTopView:animated];
    else
        [[self _controller] anchorTopViewTo:ECLeft animated:animated];
}

-(void)openLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
  [[self _controller] anchorTopViewTo:ECRight animated:animated];
}

-(void)openRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    [[self _controller] anchorTopViewTo:ECLeft animated:animated];
}

-(void)closeLeftView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    [[self _controller] resetTopView:animated];
}

-(void)closeRightView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args, NSNumber);
    ENSURE_UI_THREAD_1_ARG(args);
    BOOL animated = YES;
	if (args != nil)
		animated = [args boolValue];
    [[self _controller] resetTopView:animated];
}


//Notifications
// slidingViewController notification

- (void)underLeftWillDisappear:(NSNotification *)notification
{
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(0), @"side",
                         NUMFLOAT(0.25f), @"duration",
                         NUMBOOL(true), @"animated", nil];
    if ([self _hasListeners:@"closemenu"])
    {
        [self fireEvent:@"closemenu" withObject:evt propagate:YES];
    }
}

- (void)underRightWillDisappear:(NSNotification *)notification
{
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(1), @"side",
                        NUMFLOAT(0.25f), @"duration",
                         NUMBOOL(true), @"animated", nil];
    if ([self _hasListeners:@"closemenu"])
    {
        [self fireEvent:@"closemenu" withObject:evt propagate:YES];
    }
}
- (void)underLeftWillAppear:(NSNotification *)notification
{
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(0), @"side",
                         NUMFLOAT(0.25f), @"duration",
                         NUMBOOL(true), @"animated", nil];
    if ([self _hasListeners:@"openmenu"])
    {
        [self fireEvent:@"openmenu" withObject:evt propagate:YES];
    }
}

- (void)topDidAnchorRight:(NSNotification *)notification
{
    NSLog(@"top did anchor right");
}

- (void)underRightWillAppear:(NSNotification *)notification
{
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(1), @"side",
                         NUMFLOAT(0.25f), @"duration",
                         NUMBOOL(true), @"animated", nil];
    if ([self _hasListeners:@"openmenu"])
    {
        [self fireEvent:@"openmenu" withObject:evt propagate:YES];
    }
}

//- (void)topDidAnchorLeft:(NSNotification *)notification
//{
//    NSLog(@"top did anchor left");
//}
//
//- (void)topDidReset:(NSNotification *)notification
//{
//    NSLog(@"top did reset");
//}

//delegate
//- (void)viewDeckController:(IIViewDeckController*)viewDeckController didChangeOffset:(CGFloat)offset orientation:(IIViewDeckOffsetOrientation)orientation panning:(BOOL)panning
//{
//    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMFLOAT(offset), @"offset", nil];
//    if ([self _hasListeners:@"scroll"])
//    {
//        [self fireEvent:@"scroll" withObject:evt propagate:YES];
//    }
//}
//
//- (void)viewDeckController:(IIViewDeckController*)viewDeckController willCloseViewSide:(IIViewDeckSide)viewDeckSide animated:(BOOL)animated{
//        
//    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(viewDeckSide), @"side",
//                        NUMFLOAT([[self _controller] closeSlideAnimationDuration]), @"duration",
//                         NUMBOOL(animated), @"animated", nil];
//    if ([self _hasListeners:@"closemenu"])
//    {
//        [self fireEvent:@"closemenu" withObject:evt propagate:YES];
//    }
//}
//
//- (void)viewDeckController:(IIViewDeckController*)viewDeckController willOpenViewSide:(IIViewDeckSide)viewDeckSide animated:(BOOL)animated{
//    
//    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(viewDeckSide), @"side",
//                         NUMFLOAT([[self _controller] closeSlideAnimationDuration]), @"duration",
//                         NUMBOOL(animated), @"animated", nil];
//    if ([self _hasListeners:@"openmenu"])
//    {
//        [self fireEvent:@"openmenu" withObject:evt propagate:YES];
//    }
//}


@end
