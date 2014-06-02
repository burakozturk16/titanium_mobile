//
//  ADZoomTransition.h
//  AppLibrary
//
//  Created by Romain Goyet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "ADDualTransition.h"

@interface ADZoomTransition : ADDualTransition
- (id)initWithSourceRect:(CGRect)sourceRect andTargetRect:(CGRect)targetRect forDuration:(double)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed;
- (id)initWithScale:(CGFloat)scale forDuration:(double)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed;
@end
