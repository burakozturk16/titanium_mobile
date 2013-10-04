//
//  ADIOS7Transition.m
//  Titanium
//
//  Created by Martin Guillon on 23/09/13.
//
//

#import "ADIOS7Transition.h"

#define OUT_TRANSLATE_FACTOR 0.3
#define OUT_OPACITY 0.7
@implementation ADIOS7Transition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect {
    
    const CGFloat viewWidth = sourceRect.size.width;
    const CGFloat viewHeight = sourceRect.size.height;
    
    CABasicAnimation * inSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    CABasicAnimation * outSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    inSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    outSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    switch (orientation) {
        case ADTransitionRightToLeft:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(-OUT_TRANSLATE_FACTOR * viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionLeftToRight:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation( OUT_TRANSLATE_FACTOR * viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionTopToBottom:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, OUT_TRANSLATE_FACTOR * viewHeight, 0.0f)];
        }
            break;
        case ADTransitionBottomToTop:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - OUT_TRANSLATE_FACTOR * viewHeight, 0.0f)];
        }
            break;
        default:
            NSAssert(FALSE, @"Unhandled ADTransitionOrientation");
            break;
    }
    inSwipeAnimation.duration = duration;
    inSwipeAnimation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
    
    CABasicAnimation * outPositionAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outPositionAnimation.fromValue = [NSNumber numberWithDouble:-0.001f];
    outPositionAnimation.toValue = [NSNumber numberWithDouble:-0.001f];
    outPositionAnimation.duration = duration;
    
    CABasicAnimation * outOpacityAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    outOpacityAnimation.fromValue = [NSNumber numberWithFloat:1.0f];
    outOpacityAnimation.toValue = [NSNumber numberWithFloat:OUT_OPACITY];
    
    CAAnimationGroup * outAnimation = [CAAnimationGroup animation];
    [outAnimation setAnimations:@[outPositionAnimation, outOpacityAnimation, outSwipeAnimation]];
    outAnimation.duration = duration;
    outAnimation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseIn];
    
    self = [super initWithInAnimation:inSwipeAnimation andOutAnimation:outAnimation];
    return self;
}

@end
