//
//  ADCarrouselTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADCarrouselTransition.h"

@implementation ADCarrouselTransition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    CAAnimation * animation = nil;
    CGFloat viewWidth = sourceRect.size.width;
    CGFloat viewHeight = sourceRect.size.height;
    
    CGFloat zPositionMax = 0.0f;
    
    CATransform3D rotation = CATransform3DIdentity;
    CATransform3D ouTransform = CATransform3DIdentity;
    
    if (reversed) orientation = [ADTransition reversedOrientation:orientation];
    if (orientation == ADTransitionRightToLeft) {
        zPositionMax = - viewWidth;
        rotation = CATransform3DTranslate(rotation, viewWidth * 0.5f, 0.0f, viewWidth * 0.5f);
        rotation = CATransform3DRotate(rotation, - M_PI * 0.5f, 0.0f, 1.0f, 0.0f);
        ouTransform = CATransform3DRotate(ouTransform, M_PI * 0.5f, 0.0f, 1.0f, 0.0f);
        ouTransform = CATransform3DTranslate(ouTransform, - viewWidth * 0.5f, 0.0f, - viewWidth * 0.5f);
    } else if (orientation == ADTransitionLeftToRight) {
        zPositionMax = - viewWidth;
        rotation = CATransform3DTranslate(rotation, - viewWidth * 0.5f, 0.0f, viewWidth * 0.5f);
        rotation = CATransform3DRotate(rotation, M_PI * 0.5f, 0.0f, 1.0f, 0.0f);
        ouTransform = CATransform3DRotate(ouTransform, - M_PI * 0.5f, 0.0f, 1.0f, 0.0f);
        ouTransform = CATransform3DTranslate(ouTransform, viewWidth * 0.5f, 0.0f, - viewWidth * 0.5f);
    } else if (orientation == ADTransitionTopToBottom) {
        zPositionMax = - viewHeight;
        rotation = CATransform3DTranslate(rotation, 0.0f, viewHeight * 0.5f, viewHeight * 0.5f);
        rotation = CATransform3DRotate(rotation, M_PI * 0.5f, 1.0f, 0.0f, 0.0f);
        ouTransform = CATransform3DRotate(ouTransform, - M_PI * 0.5f, 1.0f, 0.0f, 0.0f);
        ouTransform = CATransform3DTranslate(ouTransform, 0.0f, - viewHeight * 0.5f, - viewHeight * 0.5f);
    } else if (orientation == ADTransitionBottomToTop) {
        zPositionMax = - viewHeight;
        rotation = CATransform3DTranslate(rotation, 0.0f, - viewHeight * 0.5f, viewHeight * 0.5f);
        rotation = CATransform3DRotate(rotation,- M_PI * 0.5f, 1.0f, 0.0f, 0.0f);
        ouTransform = CATransform3DRotate(ouTransform, M_PI * 0.5f, 1.0f, 0.0f, 0.0f);
        ouTransform = CATransform3DTranslate(ouTransform, 0.0f, viewHeight * 0.5f, - viewHeight * 0.5f);
    } else {
        NSAssert(FALSE, @"Unhandled ADTransitionOrientation!");
    }
    
    CABasicAnimation * carrouselRotation = [CABasicAnimation animationWithKeyPath:@"transform"];
    carrouselRotation.fromValue = [NSValue valueWithCATransform3D:rotation];
    carrouselRotation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    
    CAKeyframeAnimation * zTranslationAnimation = [CAKeyframeAnimation animationWithKeyPath:@"zPosition"];
    zTranslationAnimation.values = @[@0.0f, @(zPositionMax), @0.0f];
    zTranslationAnimation.timingFunctions = [self getCircleApproximationTimingFunctions];
    zTranslationAnimation.duration = duration;
    
    animation = [CAAnimationGroup animation];
    animation.duration = duration;
    [(CAAnimationGroup *)animation setAnimations:@[carrouselRotation, zTranslationAnimation]];

    return [super initWithAnimation:animation orientation:orientation inLayerTransform:CATransform3DIdentity outLayerTransform:ouTransform reversed:reversed];
}

@end
