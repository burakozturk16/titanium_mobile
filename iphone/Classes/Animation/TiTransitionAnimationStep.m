#import "TiTransitionAnimationStep.h"

#import "CALayer+HLSExtensions.h"
#import "HLSAnimationStep+Protected.h"
#import "TiTransitionAnimation+Friend.h"
#import "TiUIView.h"
#import "TiAnimation.h"
#import "TiViewProxy.h"

@interface TiTransitionAnimationStep ()

@end

@implementation TiTransitionAnimationStep

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

- (void)addTransitionAnimation:(TiTransitionAnimation *)animation insideHolder:(UIView *)holder
{
    [self addObjectAnimation:animation forObject:holder];
}

- (void)playAnimationWithStartTime:(NSTimeInterval)startTime animated:(BOOL)animated
{
    if (!animated || self.terminating) return;
    
    for (UIView *holderView in [self objects]) {
        TiTransitionAnimation *transitionAnimation = (TiTransitionAnimation *)[self objectAnimationForObject:holderView];
        
//        TiUIView *holderView = [holderViewProxy getOrCreateView];
        TiUIView *transitionView = [transitionAnimation.transitionViewProxy getOrCreateView];
        
        if (transitionView != nil && holderView!=nil && transitionView != holderView)
        {
            // we need to first make sure our new view that we're transitioning to is sized but we don't want
            // to add to the view hiearchry inside the animation block or you'll get the sizings as part of the
            // animation.. which we don't want
            LayoutConstraint *contraints = [transitionAnimation.transitionViewProxy layoutProperties];
            ApplyConstraintToViewWithBounds(contraints, transitionView, holderView.bounds);
            [transitionAnimation.transitionViewProxy windowWillOpen];
            [transitionAnimation.transitionViewProxy layoutChildren:NO];
        }
        
        // NOTE: This results in a behavior change from previous versions, where interaction
        // with animations was allowed. In particular, with the new block system, animations can
        // be concurrent or interrupted, as opposed to being synchronous.
        [UIView transitionWithView:holderView
              duration:self.duration
               options:transitionAnimation.transition
            animations:^{
                if (transitionAnimation.closeTransition) {
                    [transitionView removeFromSuperview];
                }
                else {
                    if (!transitionAnimation.openTransition) {
                        // transitions are between 2 views so we need to remove existing views (normally only one)
                        // and then we need to add our new view
                        for (UIView *subview in [holderView subviews])
                        {
                            if (subview != transitionView) {
                                //Making sure the view being transitioned off is properly removed
                                //from the view hierarchy.
                                if ([subview isKindOfClass:[TiUIView class]]){
                                    TiUIView *subView = (TiUIView *)subview;
                                    TiViewProxy *ourProxy = (TiViewProxy *)subView.proxy ;
                                    [[ourProxy parent] remove:ourProxy];
                                }
                                
                                [subview removeFromSuperview];
                            }
                        }
                    }
                    [holderView addSubview:transitionView];
                }
                
            }
            completion:^(BOOL finished) {
                //Adding the new view to the transition view's hierarchy.
                [transitionAnimation.holderViewProxy add:transitionAnimation.transitionViewProxy];
                [self notifyAsynchronousAnimationStepDidStopFinished:finished];
            }
         ];
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

- (id)TiTransitionAnimationStep
{
    TiTransitionAnimationStep *reverseAnimationStep = [super reverseAnimationStep];

    return reverseAnimationStep;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiTransitionAnimationStep *animationStepCopy = [super copyWithZone:zone];
    return animationStepCopy;
}

@end
