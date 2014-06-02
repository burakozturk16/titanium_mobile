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
    return [self initWithAnimation:animation inLayerTransform:inTransform outLayerTransform:outTransform reversed:NO];
}

- (id)initWithAnimation:(CAAnimation *)animation reversed:(BOOL)reversed{
    return [self initWithAnimation:animation inLayerTransform:CATransform3DIdentity outLayerTransform:CATransform3DIdentity reversed:reversed];
}

- (id)initWithAnimation:(CAAnimation *)animation inLayerTransform:(CATransform3D)inTransform outLayerTransform:(CATransform3D)outTransform reversed:(BOOL)reversed
{
    return [self initWithAnimation:animation orientation:ADTransitionLeftToRight inLayerTransform:inTransform outLayerTransform:outTransform reversed:reversed];
}

- (id)initWithAnimation:(CAAnimation *)animation orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed{
    return [self initWithAnimation:animation orientation:orientation inLayerTransform:CATransform3DIdentity outLayerTransform:CATransform3DIdentity reversed:reversed];
}

- (id)initWithAnimation:(CAAnimation *)animation orientation:(ADTransitionOrientation)orientation inLayerTransform:(CATransform3D)inTransform outLayerTransform:(CATransform3D)outTransform reversed:(BOOL)reversed {
    if (self = [super initWithDuration:[animation duration] orientation:orientation sourceRect:CGRectZero reversed:reversed]) {
        _animation = [animation retain];
        _animation.delegate = self;
        _animation.fillMode = kCAFillModeBoth;
        _animation.removedOnCompletion = NO;
        _inLayerTransform = inTransform;
        _outLayerTransform = outTransform;
        if (reversed) {
            _animation.timingFunction = [_animation.timingFunction inverseFunction];
        }
    }
    return self;
}

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    if (self = [super initWithDuration:duration orientation:orientation sourceRect:sourceRect reversed:reversed]) {
        _inLayerTransform = CATransform3DIdentity;
        _outLayerTransform = CATransform3DIdentity;
    }
}


-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    viewOut.layer.transform = viewIn.layer.transform = CATransform3DIdentity;
    [viewContainer.layer removeAnimationForKey:kAdKey];
    [super finishedTransitionFromView:viewOut toView:viewIn inside:viewContainer];
}

-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [super startTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    viewIn.layer.transform = self.inLayerTransform;
    viewOut.layer.transform = self.outLayerTransform;
    
    // We now balance viewIn.layer.transform by taking its invert and putting it in the superlayer of viewIn.layer
    // so that viewIn.layer appears ok in the final state.
    // (When pushing, viewIn.layer.transform == CATransform3DIdentity)
    viewContainer.layer.transform = CATransform3DInvert(viewIn.layer.transform);
    
    [viewContainer.layer addAnimation:self.animation forKey:kAdKey];
}

- (NSTimeInterval)duration {
    return self.animation.duration;
}

-(BOOL)needsPerspective {
    return YES;
}

@end