#import "ADPercentDrivenInteractiveTransition.h"

// Slightly modified version of https://github.com/bnickel/Silly-Transitions/blob/master/Silly%20Transitions/BKNPercentDrivenInteractiveTransition.m

@interface ADPercentDrivenInteractiveTransition ()
@property (nonatomic, readonly, weak) CALayer *containerLayer;
@property (nonatomic, assign) CFTimeInterval pausedTime;
@end

@implementation ADPercentDrivenInteractiveTransition

- (CALayer *)containerLayer
{
    return [self.transitionContext containerView].layer;
}

- (void)animateTransition:(id<UIViewControllerContextTransitioning>)transitionContext
{
    [self doesNotRecognizeSelector:_cmd];
}

- (NSTimeInterval)transitionDuration:(id<UIViewControllerContextTransitioning>)transitionContext {
    return 1.0;
}

- (void)animationEnded:(BOOL)transitionCompleted
{
    self.containerLayer.speed = 1.0;
}

- (void)startInteractiveTransition:(id<UIViewControllerContextTransitioning>)transitionContext
{
    NSLog(@"startInteractiveTransition");
    _transitionContext = transitionContext;
    _duration = [self transitionDuration:transitionContext];
    
    [self pauseLayer:self.containerLayer];
    [self animateTransition:transitionContext];
}

- (void)updateInteractiveTransition:(CGFloat)percentComplete
{
    NSLog(@"updateInteractiveTransition %f", percentComplete);
    
//    UIViewController * toViewController = [_transitionCo dntext viewControllerForKey:UITransitionContextToViewControllerKey];
    

    [self.transitionContext updateInteractiveTransition:percentComplete];
    self.containerLayer.timeOffset =  self.pausedTime + MAX(0, self.duration * percentComplete);
}

- (void)cancelInteractiveTransition
{
    NSLog(@"cancelInteractiveTransition");
    self.containerLayer.speed = -1.0;
    self.containerLayer.beginTime = CACurrentMediaTime();
    [self.transitionContext cancelInteractiveTransition];
}

- (void)finishInteractiveTransition
{
    NSLog(@"finishInteractiveTransition");
    [self.transitionContext finishInteractiveTransition];
    [self resumeLayer:self.containerLayer];
}

- (void)pauseLayer:(CALayer*)layer
{
    CFTimeInterval pausedTime = [layer convertTime:CACurrentMediaTime() fromLayer:nil];
    layer.timeOffset = pausedTime;
    layer.speed = 0.0;
    self.pausedTime = pausedTime;
}

- (void)resumeLayer:(CALayer*)layer
{
    CFTimeInterval pausedTime = [layer timeOffset];
    layer.speed = 1.0;
    layer.timeOffset = 0.0;
    layer.beginTime = 0.0;
    CFTimeInterval timeSincePause = [layer convertTime:CACurrentMediaTime() fromLayer:nil] - pausedTime;
    layer.beginTime = timeSincePause;
}

@end
