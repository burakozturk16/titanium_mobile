//
//  ADFadeTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADFadeTransition.h"

@implementation ADFadeTransition

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    CABasicAnimation * inAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    inAnimation.fromValue = @0.0f;
    inAnimation.toValue = @1.0f;
    inAnimation.duration = duration;
    
    CABasicAnimation * outAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    outAnimation.fromValue = @1.0f;
    outAnimation.toValue = @0.0f;
    outAnimation.duration = duration;
    
    return [super initWithInAnimation:inAnimation andOutAnimation:outAnimation orientation:orientation reversed:reversed];
}

@end
