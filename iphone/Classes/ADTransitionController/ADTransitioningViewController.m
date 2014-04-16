//
//  ADTransitioningViewController.m
//  ADTransitionController
//
//  Created by Patrick Nollet on 10/10/13.
//  Copyright (c) 2013 Applidium. All rights reserved.
//

#import "ADTransitioningViewController.h"
#import "ADTransitioningDelegate.h"
#import "ADModernPushTransition.h"

#define AD_SYSTEM_VERSION_GREATER_THAN_7 ([[[UIDevice currentDevice] systemVersion] compare:@"7" options:NSNumericSearch] == NSOrderedDescending)


@interface ADTransitioningViewController () {
    ADTransitioningDelegate * _customTransitioningDelegate;
}

@end

@implementation ADTransitioningViewController
@synthesize transition = _transition;

- (void)dealloc {
    [_customTransitioningDelegate release], _customTransitioningDelegate = nil;
    [super dealloc];
}

- (void)setTransitioningDelegate:(id <UIViewControllerTransitioningDelegate>)delegate {
    NSAssert(FALSE, @"This setter shouldn't be used! You should set the transition property instead.");
}

- (void)setTransition:(ADTransition *)transition {
    [_transition release];
    _transition = nil;
    if (transition) {
        _transition = [transition retain];
        if ((!AD_SYSTEM_VERSION_GREATER_THAN_7 || ![transition isKindOfClass:[ADModernPushTransition class]])) {
            [_customTransitioningDelegate release]; _customTransitioningDelegate = [[ADTransitioningDelegate alloc] initWithTransition:transition];
            [super setTransitioningDelegate:_customTransitioningDelegate]; // don't call the setter of the current class
        }
    }
    else {
        [super setTransitioningDelegate:nil];
    }
}
@end
