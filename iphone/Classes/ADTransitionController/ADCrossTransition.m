//
//  ADCrossTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADCrossTransition.h"

@implementation ADCrossTransition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    float angle = (reversed?-1:1)*M_PI;
    CAAnimation * animation = nil;
    CATransform3D ouTransform = CATransform3DIdentity;
    animation = [CABasicAnimation animationWithKeyPath:@"transform"];
    ((CABasicAnimation *)animation).fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeRotation(angle * 0.5f, 0.0f, 1.0f, 0.0f)];
    ((CABasicAnimation *)animation).toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    ouTransform = CATransform3DRotate(ouTransform, -angle * 0.5f, 0.0f, 1.0f, 0.0f);
    
    _animation = [animation retain];
    _animation.duration = duration;
    _animation.delegate = self;
    return [super initWithAnimation:animation orientation:orientation inLayerTransform:CATransform3DIdentity outLayerTransform:ouTransform reversed:reversed];
}

@end
