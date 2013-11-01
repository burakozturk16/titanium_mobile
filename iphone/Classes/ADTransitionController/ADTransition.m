//
//  ADTransition.m
//  Transition
//
//  Created by Patrick Nollet on 21/02/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADTransition.h"
#import "ADTransformTransition.h"
#import "ADDualTransition.h"

NSString * ADTransitionAnimationKey = @"ADTransitionAnimationKey";
NSString * ADTransitionAnimationInValue = @"ADTransitionAnimationInValue";
NSString * ADTransitionAnimationOutValue = @"ADTransitionAnimationOutValue";

@implementation ADTransition
@synthesize delegate = _delegate;
@synthesize type = _type;
@synthesize orientation = _orientation;

- (id)init {
    if (self = [super init]) {
        self.isReversed = NO;
    }
    return self;
}

+ (ADTransition *)nullTransition {
    return [[[ADTransition alloc] init] autorelease];
}

- (ADTransition *)reverseTransition {
    return nil;
}

- (float)getDuration {
    return _duration;
}


- (void)dealloc {
    [super dealloc];
}

- (NSArray *)getCircleApproximationTimingFunctions {
    // The following CAMediaTimingFunction mimics zPosition = sin(t)
    // Empiric (possibly incorrect, but it does the job) implementation based on the circle approximation with bezier cubic curves
    // ( http://www.whizkidtech.redprince.net/bezier/circle/ )
    // sin(t) tangent for t=0 is a diagonal. But we have to remap x=[0;PI/2] to t=[0;1]. => scale with M_PI/2.0f factor
    
    const double kappa = 4.0/3.0 * (sqrt(2.0)-1.0) / sqrt(2.0);
    CAMediaTimingFunction *firstQuarterCircleApproximationFuction = [CAMediaTimingFunction functionWithControlPoints:kappa /(M_PI/2.0f) :kappa :1.0-kappa :1.0];
    CAMediaTimingFunction * secondQuarterCircleApproximationFuction = [CAMediaTimingFunction functionWithControlPoints:kappa :0.0 :1.0-(kappa /(M_PI/2.0f)) :1.0-kappa];
    return @[firstQuarterCircleApproximationFuction, secondQuarterCircleApproximationFuction];
}

#pragma mark -
#pragma mark CAAnimationDelegate
- (void)animationDidStop:(CAAnimation *)animation finished:(BOOL)flag {    
    switch (self.type) {
        case ADTransitionTypePop:
            [self.delegate popTransitionDidFinish:self];
            break;
        case ADTransitionTypePush:
            [self.delegate pushTransitionDidFinish:self];
            break;
        default:
            NSAssert(FALSE, @"Unexpected case in switch statement !");
            break;
    }
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

-(void)prepareTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    viewIn.layer.doubleSided = NO;
    viewOut.layer.doubleSided = NO;
    [self _setupLayers:@[viewIn.layer, viewOut.layer]];
}

-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [self _teardownLayers:@[viewIn.layer, viewOut.layer]];
}

-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    NSAssert(FALSE, @"Unhandled ADTransition subclass!");
}

- (void)transitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [self prepareTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    [CATransaction setCompletionBlock:^{
        [self finishedTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    }];
    
    [self startTransitionFromView:viewOut toView:viewIn inside:viewContainer];
}
@end
