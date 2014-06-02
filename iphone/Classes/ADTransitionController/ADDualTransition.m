//
//  ADDualTransition.m
//  AppLibrary
//
//  Created by Patrick Nollet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADDualTransition.h"

@implementation ADDualTransition
@synthesize inAnimation = _inAnimation;
@synthesize outAnimation = _outAnimation;

- (id)initWithInAnimation:(CAAnimation *)inAnimation andOutAnimation:(CAAnimation *)outAnimation {
    return [self initWithInAnimation:inAnimation andOutAnimation:outAnimation reversed:NO];
}

- (id)initWithInAnimation:(CAAnimation *)inAnimation andOutAnimation:(CAAnimation *)outAnimation reversed:(BOOL)reversed {
    return[self initWithInAnimation:inAnimation andOutAnimation:outAnimation orientation:ADTransitionLeftToRight reversed:reversed];
}

- (id)initWithInAnimation:(CAAnimation *)inAnimation andOutAnimation:(CAAnimation *)outAnimation orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed {
    if (self = [super initWithDuration:_inAnimation.duration orientation:orientation sourceRect:CGRectZero reversed:reversed]) {
        _inAnimation = [inAnimation retain];
        _outAnimation = [outAnimation retain];
        if (reversed) {
            _inAnimation.timingFunction = [_inAnimation.timingFunction inverseFunction];
            _outAnimation.timingFunction = [_outAnimation.timingFunction inverseFunction];
        }
        [self finishInit];
    }
    return self;
}

- (id)initWithDuration:(CFTimeInterval)duration {
    _duration = duration;
    return nil;
}

- (void)dealloc {
    [_inAnimation release];
    [_outAnimation release];
    [super dealloc];
}

- (void)finishInit {
    _delegate = nil;
    _inAnimation.delegate = self; // The delegate object is retained by the receiver. This is a rare exception to the memory management rules described in 'Memory Management Programming Guide'.
    [_inAnimation setValue:ADTransitionAnimationInValue forKey:ADTransitionAnimationKey]; // See 'Core Animation Extensions To Key-Value Coding' : "while the key “someKey” is not a declared property of the CALayer class, however you can still set a value for the key “someKey” "
    _outAnimation.delegate = self;
    [_outAnimation setValue:ADTransitionAnimationOutValue forKey:ADTransitionAnimationKey];
    _inAnimation.fillMode = _outAnimation.fillMode = kCAFillModeBoth;
    _inAnimation.removedOnCompletion = _outAnimation.removedOnCompletion = NO;
}

-(void)prepareTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [super prepareTransitionFromView:viewOut toView:viewIn inside:viewContainer];
}

-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [super startTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    [viewIn.layer addAnimation:self.inAnimation forKey:kAdKey];
    [viewOut.layer addAnimation:self.outAnimation forKey:kAdKey];
}

- (NSTimeInterval)duration {
    NSTimeInterval result = MAX(self.inAnimation.duration, self.outAnimation.duration);
    return result;
}

#pragma mark -
#pragma mark CAAnimationDelegate
- (void)animationDidStop:(CAAnimation *)animation finished:(BOOL)flag {
    if ([[animation valueForKey:ADTransitionAnimationKey] isEqualToString:ADTransitionAnimationInValue]) {
        [super animationDidStop:animation finished:flag];
    }
}

@end
