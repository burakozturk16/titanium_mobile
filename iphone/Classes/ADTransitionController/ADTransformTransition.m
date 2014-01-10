//
//  ADTransformTransition.m
//  Transition
//
//  Created by Patrick Nollet on 08/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADTransformTransition.h"

@interface ADTransformTransition (Private)
- (void)_initADTransformTransitionTemplateCrossWithDuration:(CFTimeInterval)duration;
@end

@implementation ADTransformTransition
@synthesize inLayerTransform = _inLayerTransform;
@synthesize outLayerTransform = _outLayerTransform;
@synthesize animation = _animation;

- (void)dealloc {
    [_animation release], _animation = nil;
    [super dealloc];
}

- (id)initWithAnimation:(CAAnimation *)animation inLayerTransform:(CATransform3D)inTransform outLayerTransform:(CATransform3D)outTransform {
    if (self = [super init]) {
        _animation = [animation copy]; // the instances should be different because we don't want them to have the same delegate
        _duration = _animation.duration;
        _animation.delegate = self;
        _inLayerTransform = inTransform;
        _outLayerTransform = outTransform;
    }
    return self;
}

- (id)initWithDuration:(CFTimeInterval)duration {
    if (self = [super init]) {
        _duration = duration;
        _inLayerTransform = CATransform3DIdentity;
        _outLayerTransform = CATransform3DIdentity;
    }
    return self;
}

- (id)initWithDuration:(CFTimeInterval)duration sourceRect:(CGRect)sourceRect {
    return [self initWithDuration:duration];
}

- (ADTransition *)reverseTransition {
    ADTransformTransition * reversedTransition = [[[self class] alloc] initWithAnimation:_animation inLayerTransform:_outLayerTransform outLayerTransform:_inLayerTransform];;
    reversedTransition.isReversed = YES;
    reversedTransition.delegate = self.delegate; // Pointer assignment
    reversedTransition.animation.speed = - 1.0 * reversedTransition.animation.speed;
    reversedTransition.type = ADTransitionTypeNull;
    if (self.type == ADTransitionTypePush) {
        reversedTransition.type = ADTransitionTypePop;
    } else if (self.type == ADTransitionTypePop) {
        reversedTransition.type = ADTransitionTypePush;
    }
    return [reversedTransition autorelease];
}

-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [super startTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    viewIn.layer.transform = self.inLayerTransform;
    viewOut.layer.transform = self.outLayerTransform;
    
    // We now balance viewIn.layer.transform by taking its invert and putting it in the superlayer of viewIn.layer
    // so that viewIn.layer appears ok in the final state.
    // (When pushing, viewIn.layer.transform == CATransform3DIdentity)
    viewContainer.layer.transform = CATransform3DInvert(viewIn.layer.transform);
    
    [viewContainer.layer addAnimation:self.animation forKey:nil];
}

- (NSTimeInterval)duration {
    return self.animation.duration;
}

@end