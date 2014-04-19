
//
//  ADBackFadeTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADBackFadeTransition.h"
#import "ADDualTransition.h"

@implementation ADBackFadeTransition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    CABasicAnimation * inFadeAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    inFadeAnimation.fromValue = @0.0f;
    inFadeAnimation.toValue = @1.0f;
    
    CABasicAnimation * outFadeAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    outFadeAnimation.fromValue = @1.0f;
    outFadeAnimation.toValue = @0.0f;
    
    CAKeyframeAnimation * backFadeTranslation = [CAKeyframeAnimation animationWithKeyPath:@"zPosition"];
    backFadeTranslation.values = @[@0.0f, @-2000.0f, @0.0f];
    
    CAMediaTimingFunction * SShapedFunction = [CAMediaTimingFunction functionWithControlPoints:0.8f :0.0f :0.0f :0.2f];
    inFadeAnimation.timingFunction = SShapedFunction;
    outFadeAnimation.timingFunction = SShapedFunction;
    
    CAAnimationGroup * inAnimation = [CAAnimationGroup animation];
    [inAnimation setAnimations:@[backFadeTranslation, inFadeAnimation]];
    inAnimation.duration = duration;
    
    CAAnimationGroup * outAnimation = [CAAnimationGroup animation];
    [outAnimation setAnimations:@[backFadeTranslation, outFadeAnimation]];
    outAnimation.duration = duration;
    
    return [super initWithInAnimation:inAnimation andOutAnimation:outAnimation orientation:orientation reversed:reversed];
}

@end
