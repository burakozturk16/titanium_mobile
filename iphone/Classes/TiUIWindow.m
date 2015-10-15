/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiUIWindow.h"
#import "TiUIWindowProxy.h"
#import "TiApp.h"

@implementation TiUIWindow

- (void) dealloc
{
	[super dealloc];
}

//-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
//{
//    [super frameSizeChanged:frame bounds:bounds];
//    
//    //Need the delay so that we get the right navbar bounds
//    TiProxy* windowProxy = [self proxy];
//    if ([windowProxy respondsToSelector:@selector(willChangeSize)]) {
//        [(id)windowProxy willChangeSize];
//    }
//    if ([windowProxy respondsToSelector:@selector(updateNavBar)]) {
//        [windowProxy performSelector:@selector(updateNavBar) 
//                           withObject:nil 
//                           afterDelay:[[UIApplication sharedApplication] statusBarOrientationAnimationDuration] ];
//    }
//}

//-(void)setFrame:(CGRect)frame
//{
//	// this happens when a controller resizes its view
//	if (!CGRectIsEmpty(frame) && [self.proxy isKindOfClass:[TiViewProxy class]])
//	{
//        CGRect currentframe = [self frame];
//        if (!CGRectEqualToRect(frame, currentframe))
//        {
//            CGRect bounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
//            [(TiViewProxy*)self.proxy setSandboxBounds:bounds];
//        }
//	}
//    [super setFrame:frame];
//}

-(void)setStatusBarStyle_:(id)value withObject:(id)props
{
    NSInteger theStyle = [TiUtils intValue:value def:[[[TiApp app] controller] defaultStatusBarStyle]];
    BOOL animated = [TiUtils boolValue:@"animated" properties:props def:(props != nil)];
    UIStatusBarAnimation animationStyle = UIStatusBarAnimationNone;
    if (animated) {
        animationStyle = [TiUtils intValue:@"animationStyle" properties:props def:animationStyle] ;
    }
    ((TiWindowProxy*)[self viewProxy]).internalStatusBarStyle = theStyle;
    if([[self viewProxy] viewInitialized] && [[self viewProxy] focussed]) {
        TiThreadPerformBlockOnMainThread(^{
            [(TiRootViewController*)[[TiApp app] controller] updateStatusBar:animated withStyle:animationStyle];
        }, YES);
    }
}

-(void)setFullscreen_:(id)value withObject:(id)props
{
    BOOL newValue = [TiUtils boolValue:value def:[[[TiApp app] controller] statusBarInitiallyHidden]];
    BOOL animated = [TiUtils boolValue:@"animated" properties:props def:(props != nil)];
    UIStatusBarAnimation animationStyle = UIStatusBarAnimationNone;
    if (animated) {
        animationStyle = [TiUtils intValue:@"animationStyle" properties:props def:animationStyle] ;
    }
    ((TiWindowProxy*)[self viewProxy]).hidesStatusBar = newValue;
    if([[self viewProxy] viewInitialized] && [[self viewProxy] focussed]) {
        TiThreadPerformBlockOnMainThread(^{
            [(TiRootViewController*)[[TiApp app] controller] updateStatusBar:animated withStyle:animationStyle];
        }, YES);
    }
}

//-(void)setFullscreen:(id)args
//{
//    ENSURE_ARG_COUNT(args, 1);
//    NSUInteger value = [TiUtils intValue:[args objectAtIndex:0]];
//    NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
//    BOOL newValue = [TiUtils boolValue:[args objectAtIndex:0] def:[[[TiApp app] controller] statusBarInitiallyHidden]];
//    
//    if (hidesStatusBar != newValue) {
//        hidesStatusBar = newValue;
//        
//        [self setValue:NUMINT(hidesStatusBar) forUndefinedKey:@"fullscreen"];
//        if([self focussed]) {
//            NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
//            BOOL animate = NO;
//            UIStatusBarAnimation animationStyle = UIStatusBarAnimationNone;
//            if (properties) {
//                animate = [TiUtils boolValue:@"animated" properties:properties def:animate];
//                if (animate) {
//                    animationStyle = [TiUtils intValue:@"animationStyle" properties:properties def:animationStyle] ;
//                }
//            }
//            TiThreadPerformBlockOnMainThread(^{
//                [(TiRootViewController*)[[TiApp app] controller] updateStatusBar:animate withStyle:animationStyle];
//            }, YES);
//        }
//    }
//}

@end

