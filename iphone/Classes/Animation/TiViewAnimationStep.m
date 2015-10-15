#import "TiViewAnimationStep.h"

#import "CALayer+HLSExtensions.h"
#import "HLSAnimationStep+Protected.h"
#import "TiViewAnimation+Friend.h"
#import "TiUIView.h"
#import "TiAnimation.h"
#import "TiViewProxy.h"
#import "CAMediaTimingFunction+HLSExtensions.h"

@interface TiViewAnimationStep ()

@end

@implementation TiViewAnimationStep

#pragma mark Object creation and destruction

- (id)init
{
    if ((self = [super init])) {
    }
    return self;
}

- (void)dealloc
{
    [super dealloc];
}

#pragma mark Accessors and mutators


#pragma mark Managing the animation

- (void)addViewAnimation:(TiViewAnimation *)viewAnimation forView:(UIView *)view
{
    [self addObjectAnimation:viewAnimation forObject:view];
}

- (void)playAnimationWithStartTime:(NSTimeInterval)startTime animated:(BOOL)animated
{
    if (!animated || self.terminating) return;
    
    for (UIView *view in [self objects]) {
        
        TiViewAnimation *viewAnimation = (TiViewAnimation *)[self objectAnimationForObject:view];
        UIViewAnimationOptions options = (UIViewAnimationOptionAllowUserInteraction); //Backwards compatible
        if (viewAnimation.animationProxy.shouldBeginFromCurrentState) {
            options |= UIViewAnimationOptionBeginFromCurrentState;
        }
        NSTimeInterval animationDuration = self.duration;
        NSAssert(viewAnimation != nil, @"Missing view animation; data consistency failure");
//        [viewAnimation checkParameters];
        void (^animation)() = ^{
            if (self.curve) {
                [CATransaction begin];
                [CATransaction setAnimationDuration:self.duration];
                [CATransaction setAnimationTimingFunction:self.curve];
                [viewAnimation applyOnView:view forStep:self];
                [CATransaction commit];
            }
            else {
                [viewAnimation applyOnView:view forStep:self];
            }
            
        };
        
        void (^complete)(BOOL) = ^(BOOL finished) {
            [self notifyAsynchronousAnimationStepDidStopFinished:finished];
        };
        
        
//        [UIView animateWithDuration:animationDuration
//                              delay:0
//             usingSpringWithDamping:0.0f
//              initialSpringVelocity:0.0f
//                            options:options
//                         animations:animation
//                         completion:complete];
        [UIView animateWithDuration:animationDuration
                              delay:0
                            options:options
                         animations:animation
                         completion:complete];
    }
}

- (void)pauseAnimation
{
    for (UIView *view in [self objects]) {
        [view.layer pauseAllAnimations];
    }
}

- (void)resumeAnimation
{
    for (UIView *view in [self objects]) {
        [view.layer resumeAllAnimations];
    }
}

- (BOOL)isAnimationPaused
{
    return [((UIView*)[[self objects] firstObject]).layer isPaused];
}

- (void)terminateAnimation
{
    // We must recursively cancel subview animations (this is especially important since altering the frame (e.g.
    // by scaling it) seems to create additional implicit animations, which still finish and trigger their end
    // animation callback with finished = YES!)
    for (UIView *view in [self objects]) {
        [view.layer removeAllAnimationsRecursively];
    }
}

- (NSTimeInterval)elapsedTime
{
    return self.duration;
}

#pragma mark Reverse animation


- (CAMediaTimingFunction *)inverseFunction:(CAMediaTimingFunction*)function
{
    float values1[2];
    memset(values1, 0, sizeof(values1));
    [function getControlPointAtIndex:1 values:values1];
    
    float values2[2];
    memset(values2, 0, sizeof(values2));
    [function getControlPointAtIndex:2 values:values2];
    
    // Flip the original curve around the y = 1 - x axis
    // Refer to the "Introduction to Animation Types and Timing Programming Guide"
    return [CAMediaTimingFunction functionWithControlPoints:1.f - values2[0] :1.f - values2[1] :1.f - values1[0] :1.f - values1[1]];
}

- (id)reverseAnimationStep
{
    TiViewAnimationStep *reverseAnimationStep = [super reverseAnimationStep];
    if ([self.objects count] == 1) {
        TiViewAnimation *viewAnimation = (TiViewAnimation *)[self objectAnimationForObject:[self.objects firstObject]];
        TiAnimation* anim = viewAnimation.animationProxy;
        if (anim.reverseCurve) {
            reverseAnimationStep.curve = anim.reverseCurve;
        }
        if (anim.reverseDuration > 0) {
            reverseAnimationStep.duration = [anim getAnimationReverseDuration];
        }
    }

    return reverseAnimationStep;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiViewAnimationStep *animationStepCopy = [super copyWithZone:zone];
    animationStepCopy.curve = self.curve;
    return animationStepCopy;
}

@end
