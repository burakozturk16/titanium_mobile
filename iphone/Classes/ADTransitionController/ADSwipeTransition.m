//
//  ADSwipeTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 15/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADSwipeTransition.h"

@implementation ADSwipeTransition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect {
    const CGFloat viewWidth = sourceRect.size.width;
    const CGFloat viewHeight = sourceRect.size.height;
    
    CABasicAnimation * inSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    inSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    
    CABasicAnimation * outSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    outSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    
    switch (orientation) {
        case ADTransitionRightToLeft:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionLeftToRight:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionTopToBottom:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight, 0.0f)];
        }
            break;
        case ADTransitionBottomToTop:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight, 0.0f)];
        }
            break;
        default:
            NSAssert(FALSE, @"Unhandled ADTransitionOrientation");
            break;
    }
    
    inSwipeAnimation.duration = duration;
    outSwipeAnimation.duration = duration;
    
    self = [super initWithInAnimation:inSwipeAnimation andOutAnimation:outSwipeAnimation];
    return self;
}

@end
