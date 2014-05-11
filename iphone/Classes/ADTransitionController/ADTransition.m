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

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    if (self = [self init]) {
        self.orientation = orientation;
        _duration = duration;
        if (reversed) {
            if (self.type == ADTransitionTypePush) {
                self.type = ADTransitionTypePop;
            } else if (self.type == ADTransitionTypePop) {
                self.type = ADTransitionTypePush;
            }
        }
        self.isReversed = reversed;
    }
    return self;
}

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed {
    return [self initWithDuration:duration orientation:orientation sourceRect:CGRectZero reversed:reversed];

}

- (id)initWithDuration:(CFTimeInterval)duration sourceRect:(CGRect)sourceRect reversed:(BOOL)reverse {
    return [self initWithDuration:duration orientation:ADTransitionLeftToRight sourceRect:sourceRect reversed:NO];
}

- (id)initWithDuration:(CFTimeInterval)duration sourceRect:(CGRect)sourceRect {
    return [self initWithDuration:duration sourceRect:sourceRect reversed:NO];
}

- (id)initWithDuration:(CFTimeInterval)duration {
    return [self initWithDuration:duration sourceRect:CGRectZero reversed:NO];
}

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect {
    return [self initWithDuration:duration orientation:orientation sourceRect:sourceRect reversed:NO];
}

+ (ADTransition *)nullTransition {
    return [[[ADTransition alloc] init] autorelease];
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

- (NSTimeInterval)duration {
    NSAssert(FALSE, @"This abstract method must be implemented!");
    return 0.0;
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
        if (![layer isKindOfClass:[NSNull class]]) {
            layer.shouldRasterize = YES;
            [layer setDoubleSided:NO];
            layer.rasterizationScale = [UIScreen mainScreen].scale;
        }
    }
}

- (void)_teardownLayers:(NSArray *)layers {
    for (CALayer * layer in layers) {
        if (![layer isKindOfClass:[NSNull class]]) {
            layer.shouldRasterize = NO;
        }
    }
}

-(void)prepareTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [self _setupLayers:@[viewIn?[viewIn layer]:[NSNull null], viewOut?[viewOut layer]:[NSNull null]]];
}

-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [self _teardownLayers:@[viewIn?[viewIn layer]:[NSNull null], viewOut?[viewOut layer]:[NSNull null]]];
    [viewIn.layer removeAnimationForKey:kAdKey];
    [viewOut.layer removeAnimationForKey:kAdKey];
}

-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
//    [viewIn.layer removeAnimationForKey:kAdKey];
//    [viewOut.layer removeAnimationForKey:kAdKey];
}

- (void)transitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [self prepareTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    [CATransaction setCompletionBlock:^{
        [self finishedTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    }];
    
    [self startTransitionFromView:viewOut toView:viewIn inside:viewContainer];
}

- (ADTransition *)reverseTransitionForSourceRect:(CGRect)rect {
    
    return [[[self class] alloc] initWithDuration:self.duration orientation:self.orientation sourceRect:rect reversed:!self.isReversed];
}

+(ADTransitionOrientation)reversedOrientation:(ADTransitionOrientation)orientation {
    switch (orientation) {
        case ADTransitionRightToLeft:
            return ADTransitionLeftToRight;
            break;
        case ADTransitionLeftToRight:
            return ADTransitionRightToLeft;
            break;
        case ADTransitionTopToBottom:
            return ADTransitionBottomToTop;
            break;
        case ADTransitionBottomToTop:
            return ADTransitionTopToBottom;
            break;
        default:
            return orientation;
            break;
    }
}

+(void)inverseTOFromInAnimation:(CABasicAnimation*)animation {
    id oldValue = animation.fromValue;
    animation.fromValue = animation.toValue;
    animation.toValue = oldValue;
}

-(BOOL)needsPerspective {
    return NO;
}
@end
