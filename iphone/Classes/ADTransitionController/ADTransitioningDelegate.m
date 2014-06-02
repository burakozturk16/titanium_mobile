//
//  ADTransitioningDelegate.m
//  ADTransitionController
//
//  Created by Patrick Nollet on 09/10/13.
//  Copyright (c) 2013 Applidium. All rights reserved.
//

#import "ADTransitioningDelegate.h"
#import "ADTransitionController.h"

#define AD_Z_DISTANCE 500.0f

@interface ADTransitioningDelegate () {
    id<UIViewControllerContextTransitioning> _currentTransitioningContext;
}

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
    }
    return self;
}

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext {
    NSLog(@"animateTransition");
    self.transitionContext = transitionContext;
    UIViewController * fromViewController = [transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];
    UIViewController * toViewController = [transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];
    
    if (self.transition.type == ADTransitionTypeNull) {
        self.transition.type = ADTransitionTypePush;
    }
    UIView * containerView = transitionContext.containerView;
    UIView * fromView = fromViewController.view;
    UIView * toView = toViewController.view;
    
    BOOL needsTransformFix = ([self.transition needsPerspective]) && ![containerView.layer isKindOfClass:[CATransformLayer class]];
    UIView* workingView = containerView;
    
    if (needsTransformFix) {
        if (![[[containerView subviews] firstObject] isKindOfClass:[ADTransitionView class]]) {
            float zDistance = AD_Z_DISTANCE;
            CATransform3D sublayerTransform = CATransform3DIdentity;
            sublayerTransform.m34 = 1.0 / -zDistance;
            containerView.layer.sublayerTransform = sublayerTransform;
            workingView = [[ADTransitionView alloc] initWithFrame: containerView.bounds];
            workingView.autoresizesSubviews = YES;
            workingView.autoresizingMask = UIViewAutoresizingFlexibleHeight | UIViewAutoresizingFlexibleWidth;
            [containerView addSubview:workingView];
            [workingView release];
        }
        else {
            workingView = [[containerView subviews] firstObject];
        }
    }
    fromView.frame = [transitionContext initialFrameForViewController:fromViewController];
    toView.frame = [transitionContext finalFrameForViewController:toViewController];
    [workingView addSubview:fromView]; //not needed if not using ADTransitionView
    [workingView addSubview:toView];
    
    ADTransition * transition = nil;
    switch (self.transition.type) {
        case ADTransitionTypePush:
            transition = self.transition;
            break;
        case ADTransitionTypePop:
            transition = [self.transition reverseTransitionForSourceRect:containerView.bounds];
            transition.type = ADTransitionTypePop;
        default:
            break;
    }
    [CATransaction begin];
    [CATransaction setAnimationDuration:[self transitionDuration:transitionContext]];
    [transition prepareTransitionFromView:fromView toView:toView inside:workingView];
    [CATransaction setCompletionBlock:^{
        [self _completeTransition:transition];
    }];
    
    [transition startTransitionFromView:fromView toView:toView inside:workingView];
    
    [CATransaction commit];
}

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
    return self.transition.duration;
}

- (void)_completeTransition:(ADTransition *)transition {
    id<UIViewControllerContextTransitioning> transitionContext = [self transitionContext];
    UIViewController * from = [transitionContext viewControllerForKey:UITransitionContextFromViewControllerKey];
    UIViewController * to = [transitionContext viewControllerForKey:UITransitionContextToViewControllerKey];
    UIView * contextView = [transitionContext containerView];
    UIView * workingView = contextView;
    if ([[[contextView subviews] firstObject] isKindOfClass:[ADTransitionView class]]) {
        workingView = [[contextView subviews] firstObject];
    }
    
    BOOL cancelled = [transitionContext transitionWasCancelled];
    if (cancelled) {
        [contextView addSubview:from.view];
        [to.view removeFromSuperview];
    } else {
        [contextView addSubview:to.view];
        [from.view removeFromSuperview];
    }
    
    [transition finishedTransitionFromView:from.view toView:to.view inside:workingView];
    [transitionContext completeTransition:!cancelled];
}

@end
