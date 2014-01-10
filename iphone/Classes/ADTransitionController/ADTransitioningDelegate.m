//
//  ADTransitioningDelegate.m
//  ADTransitionController
//
//  Created by Patrick Nollet on 09/10/13.
//  Copyright (c) 2013 Applidium. All rights reserved.
//

#import "ADTransitioningDelegate.h"
#import "ADTransitionController.h"

#define AD_Z_DISTANCE 1000.0f

@interface ADTransitioningDelegate () {
    id<UIViewControllerContextTransitioning> _currentTransitioningContext;
}

@end

@interface ADTransitioningDelegate (Private)
- (void)_setupLayers:(NSArray *)layers;
- (void)_teardownLayers:(NSArray *)layers;
- (void)_completeTransition:(ADTransition *)transition;
- (void)_transitionInContainerView:(UIView *)containerView fromView:(UIView *)viewOut toView:(UIView *)viewIn withTransition:(ADTransition *)transition;
@end

@implementation ADTransitioningDelegate
@synthesize transition = _transition;

- (void)dealloc {
    [_transition release], _transition = nil;
    [super dealloc];
}

- (id)initWithTransition:(ADTransition *)transition {
    self = [self init];
    if (self) {
        _transition = [transition retain];
        _transition.delegate = self;
    }
    return self;
}

#pragma mark - ADTransitionDelegate
- (void)pushTransitionDidFinish:(ADTransition *)transition {
    [self _completeTransition:transition];
}

- (void)popTransitionDidFinish:(ADTransition *)transition {
    [self _completeTransition:transition];
}

#pragma mark - UIViewControllerTransitioningDelegate

- (id<UIViewControllerAnimatedTransitioning>)animationControllerForPresentedController:(UIViewController *)presented presentingController:(UIViewController *)presenting sourceController:(UIViewController *)source {
    return self;
}

- (id<UIViewControllerAnimatedTransitioning>)animationControllerForDismissedController:(UIViewController *)dismissed {
    return self;
}

#pragma mark - UIViewControllerAnimatedTransitioning

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext {
    [_currentTransitioningContext release], _currentTransitioningContext = [transitionContext retain];
    UIViewController * fromViewController = [transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];
    UIViewController * toViewController = [transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];

    if (self.transition.type == ADTransitionTypeNull) {
        self.transition.type = ADTransitionTypePush;
    }

    UIView * containerView = transitionContext.containerView;
    UIView * fromView = fromViewController.view;
    UIView * toView = toViewController.view;

    CATransform3D sublayerTransform = CATransform3DIdentity;
    sublayerTransform.m34 = 1.0 / -AD_Z_DISTANCE;
    containerView.layer.sublayerTransform = sublayerTransform;

    UIView * wrapperView = [[ADTransitionView alloc] initWithFrame:fromView.frame];
    fromView.frame = fromView.bounds;
    toView.frame = toView.bounds;

    wrapperView.autoresizesSubviews = YES;
    wrapperView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
    [wrapperView addSubview:fromView];
    [wrapperView addSubview:toView];
    [containerView addSubview:wrapperView];
    [wrapperView release];

    ADTransition * transition = nil;
    switch (self.transition.type) {
        case ADTransitionTypePush:
            transition = self.transition;
            break;
        case ADTransitionTypePop:
            transition = self.transition.reverseTransition;
            transition.type = ADTransitionTypePop;
        default:
            break;
    }
    transition.delegate = self;
    [self _transitionInContainerView:wrapperView fromView:fromView toView:toView withTransition:transition];
}

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
    return self.transition.duration;
}
@end

@implementation ADTransitioningDelegate (Private)
- (void)_transitionInContainerView:(UIView *)containerView fromView:(UIView *)viewOut toView:(UIView *)viewIn withTransition:(ADTransition *)transition {
    [CATransaction setAnimationDuration:transition.duration];
    [CATransaction setCompletionBlock:^{
        UIView * contextView = [_currentTransitioningContext containerView];
        viewOut.frame = containerView.frame;
        [contextView addSubview:viewOut];
        viewIn.frame = containerView.frame;
        [contextView addSubview:viewIn];
        [containerView removeFromSuperview];
    }];
    [transition transitionFromView:viewOut toView:viewIn inside:containerView];
}

- (void)_setupLayers:(NSArray *)layers {
    for (CALayer * layer in layers) {
        layer.shouldRasterize = YES;
        layer.rasterizationScale = [UIScreen mainScreen].scale;
    }
}

- (void)_teardownLayers:(NSArray *)layers {
    for (CALayer * layer in layers) {
        layer.shouldRasterize = NO;
    }
}

- (void)_completeTransition:(ADTransition *)transition {
    UIViewController * fromViewController = [_currentTransitioningContext viewControllerForKey:UITransitionContextFromViewControllerKey];
    UIViewController * toViewController = [_currentTransitioningContext viewControllerForKey:UITransitionContextToViewControllerKey];
    UIView * containerView = _currentTransitioningContext.containerView;
    UIView * fromView = fromViewController.view;
    UIView * toView = toViewController.view;
    [transition finishedTransitionFromView:fromView toView:toView inside:containerView];


    [_currentTransitioningContext completeTransition:YES];
    [_currentTransitioningContext release], _currentTransitioningContext = nil;
}

@end
