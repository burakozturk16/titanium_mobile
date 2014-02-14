//
//  ADGhostTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADGhostTransition.h"

@implementation ADGhostTransition

- (id)initWithDuration:(CFTimeInterval)duration {    
    CAKeyframeAnimation * inFadeAnimation = [CAKeyframeAnimation animationWithKeyPath:@"opacity"];
    inFadeAnimation.values = @[@0.0f, @1.0f, @1.0f];
    
    CABasicAnimation * inScaleAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    inScaleAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeScale(2.0f, 2.0f, 2.0f)];
    inScaleAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    inScaleAnimation.duration = duration;
    inScaleAnimation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
    
    CAAnimationGroup * inAnimation = [CAAnimationGroup animation];
    [inAnimation setAnimations:@[inFadeAnimation, inScaleAnimation]];
    inAnimation.duration = duration;
    
    CABasicAnimation * outPositionAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outPositionAnimation.fromValue = @-0.001;
    outPositionAnimation.toValue = @-0.001;
    outPositionAnimation.duration = duration;
    
    CAKeyframeAnimation * outFadeAnimation = [CAKeyframeAnimation animationWithKeyPath:@"opacity"];
    outFadeAnimation.values = @[@1.0f, @0.0f, @0.0f];
    
    CABasicAnimation * outScaleAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    outScaleAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    outScaleAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeScale(0.01f, 0.01f, 0.01f)];
    outScaleAnimation.timingFunction = [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
   
    CAAnimationGroup * outAnimation = [CAAnimationGroup animation];
    [outAnimation setAnimations:@[outFadeAnimation, outScaleAnimation, outPositionAnimation]];
    outAnimation.duration = duration;
    
    self = [super initWithInAnimation:inAnimation andOutAnimation:outAnimation];
    return self;
}
@end
