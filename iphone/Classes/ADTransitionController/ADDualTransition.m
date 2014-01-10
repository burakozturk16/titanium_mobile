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
    if (self = [self init]) {
        _inAnimation = [inAnimation retain];
        _outAnimation = [outAnimation retain];
        _duration = _inAnimation.duration;
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
    _inAnimation.fillMode = _outAnimation.fillMode = kCAFillModeForwards;
    _inAnimation.removedOnCompletion = _outAnimation.removedOnCompletion = NO;
}

- (ADTransition *)reverseTransition {
    CAAnimation * inAnimationCopy = [self.inAnimation copy];
    CAAnimation * outAnimationCopy = [self.outAnimation copy];
    ADDualTransition * reversedTransition = [[[self class] alloc] initWithInAnimation:outAnimationCopy // Swapped
                                                                      andOutAnimation:inAnimationCopy];
    reversedTransition.isReversed = YES;
    reversedTransition.delegate = self.delegate; // Pointer assignment
    reversedTransition.inAnimation.speed = -1.0 * reversedTransition.inAnimation.speed;
    reversedTransition.outAnimation.speed = -1.0 * reversedTransition.outAnimation.speed;
    reversedTransition.inAnimation.timingFunction = [reversedTransition.outAnimation.timingFunction inverseFunction];
    reversedTransition.outAnimation.timingFunction = [reversedTransition.outAnimation.timingFunction inverseFunction];
    [outAnimationCopy release];
    [inAnimationCopy release];
    reversedTransition.type = ADTransitionTypeNull;
    if (self.type == ADTransitionTypePush) {
        reversedTransition.type = ADTransitionTypePop;
    } else if (self.type == ADTransitionTypePop) {
        reversedTransition.type = ADTransitionTypePush;
    }
    return [reversedTransition autorelease];
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
    if ([[animation valueForKey:ADTransitionAnimationKey] isEqualToString:ADTransitionAnimationOutValue]) {
        _outAnimation.delegate = nil;
    }
    if ([[animation valueForKey:ADTransitionAnimationKey] isEqualToString:ADTransitionAnimationInValue]) {
        _inAnimation.delegate = nil;
        [super animationDidStop:animation finished:flag];
    }
}

@end
