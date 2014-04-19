#import "ADSwipeDualFadeTransition.h"

@implementation ADSwipeDualFadeTransition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    const CGFloat viewWidth = sourceRect.size.width;
    const CGFloat viewHeight = sourceRect.size.height;
    
    CABasicAnimation * inSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    inSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    CABasicAnimation * outSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    outSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    ADTransitionOrientation rOrient = reversed?[ADTransition reversedOrientation:orientation]:orientation;
    switch (rOrient) {
        case ADTransitionRightToLeft:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(-viewWidth*kSwipeDualFadeTranslate, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionLeftToRight:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation( viewWidth*kSwipeDualFadeTranslate, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionTopToBottom:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight*kSwipeDualFadeTranslate, 0.0f)];
        }
            break;
        case ADTransitionBottomToTop:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight*kSwipeDualFadeTranslate, 0.0f)];
        }
            break;
        default:
            NSAssert(FALSE, @"Unhandled ADTransitionOrientation");
            break;
    }
    inSwipeAnimation.duration = duration;
    outSwipeAnimation.duration = duration;
    
    CABasicAnimation * inOpacityAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    inOpacityAnimation.fromValue = @0.0f;
    inOpacityAnimation.toValue = @1.0f;
    inOpacityAnimation.duration = duration;

    CABasicAnimation * inPositionAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    inPositionAnimation.fromValue = @-0.001;
    inPositionAnimation.toValue = @-0.001;
    inPositionAnimation.duration = duration;

    CAAnimationGroup * inAnimation = [CAAnimationGroup animation];
    inAnimation.animations = @[inOpacityAnimation, inSwipeAnimation, inPositionAnimation];
    inAnimation.duration = duration;
    
    CABasicAnimation * outOpacityAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    outOpacityAnimation.fromValue = @1.0f;
    outOpacityAnimation.toValue = @0.0f;
    outOpacityAnimation.duration = duration;
    
    CABasicAnimation * outPositionAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outPositionAnimation.fromValue = @-0.01;
    outPositionAnimation.toValue = @-0.01;
    outPositionAnimation.duration = duration;
    
    CAAnimationGroup * outAnimation = [CAAnimationGroup animation];
    [outAnimation setAnimations:@[outOpacityAnimation, outPositionAnimation, outSwipeAnimation]];
    outAnimation.duration = duration;
    
    return [super initWithInAnimation:inAnimation andOutAnimation:outAnimation orientation:orientation reversed:reversed];
}

@end
