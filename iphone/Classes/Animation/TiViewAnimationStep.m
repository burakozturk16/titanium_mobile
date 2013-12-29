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
        if (!viewAnimation.animationProxy.restartFromBeginning && !viewAnimation.animationProxy.cancelRunningAnimations) {
            options |= UIViewAnimationOptionBeginFromCurrentState;
        }
        NSTimeInterval animationDuration = self.duration;
        NSAssert(viewAnimation != nil, @"Missing view animation; data consistency failure");
        [viewAnimation checkParameters];
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

- (id)reverseAnimationStep
{
    TiViewAnimationStep *reverseAnimationStep = [super reverseAnimationStep];
    reverseAnimationStep.curve = [self.curve inverseFunction];

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
