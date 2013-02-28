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

MAKE_SYSTEM_PROP(LEFT_SIDE,IIViewDeckLeftSide);
MAKE_SYSTEM_PROP(RIGHT_SIDE,IIViewDeckRightSide);
MAKE_SYSTEM_PROP(TOP_SIDE,IIViewDeckTopSide);
MAKE_SYSTEM_PROP(BOTTOM_SIDE,IIViewDeckBottomSide);

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

-(IIViewDeckController *)_controller {
	return [(TiUISlideMenu*)[self view] controller];
}

-(TiUIView*)newView {
	return [[TiUISlideMenu alloc] init];
}

-(void)_configure
{
	[self setValue:NUMINT(65) forKey:@"leftWidth"];
	[self setValue:NUMINT(65) forKey:@"rightWidth"];
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
-(void)toggleLeftView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] toggleLeftView:args];}, NO);
}
-(void)toggleRightView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] toggleRightView:args];}, NO);
}
-(void)bounceLeftView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] bounceLeftView:args];}, NO);
}
-(void)bounceRightView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] bounceRightView:args];}, NO);
}
-(void)bounceTopView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] bounceTopView:args];}, NO);
}
-(void)bounceBottomView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] bounceBottomView:args];}, NO);
}
-(void)toggleOpenView:(id)args {
    TiThreadPerformOnMainThread(^{[(TiUISlideMenu*)[self view] toggleOpenView:args];}, NO);
}


//delegate
- (void)viewDeckController:(IIViewDeckController*)viewDeckController didChangeOffset:(CGFloat)offset orientation:(IIViewDeckOffsetOrientation)orientation panning:(BOOL)panning
{
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMFLOAT(offset), @"offset", nil];
    if ([self _hasListeners:@"scroll"])
    {
        [self fireEvent:@"scroll" withObject:evt propagate:YES];
    }
}

- (void)viewDeckController:(IIViewDeckController*)viewDeckController willCloseViewSide:(IIViewDeckSide)viewDeckSide animated:(BOOL)animated{
        
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(viewDeckSide), @"side",
                        NUMFLOAT([[self _controller] closeSlideAnimationDuration]), @"duration",
                         NUMBOOL(animated), @"animated", nil];
    if ([self _hasListeners:@"closeside"])
    {
        [self fireEvent:@"closeside" withObject:evt propagate:YES];
    }
}

- (void)viewDeckController:(IIViewDeckController*)viewDeckController willOpenViewSide:(IIViewDeckSide)viewDeckSide animated:(BOOL)animated{
    
    NSDictionary *evt = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(viewDeckSide), @"side",
                         NUMFLOAT([[self _controller] closeSlideAnimationDuration]), @"duration",
                         NUMBOOL(animated), @"animated", nil];
    if ([self _hasListeners:@"openside"])
    {
        [self fireEvent:@"openside" withObject:evt propagate:YES];
    }
}


@end
