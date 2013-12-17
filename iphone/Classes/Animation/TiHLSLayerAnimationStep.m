//
//  TiLayerAnimationStep.m
//  Titanium
//
//  Created by Martin Guillon on 17/12/13.
//
//

#import "TiHLSLayerAnimationStep.h"

@implementation TiHLSLayerAnimationStep

- (id)reverseAnimationStep
{
    TiHLSLayerAnimationStep *reverseAnimationStep = [super reverseAnimationStep];
    return reverseAnimationStep;
}

#pragma mark NSCopying protocol implementation

- (id)copyWithZone:(NSZone *)zone
{
    TiHLSLayerAnimationStep *animationStepCopy = [super copyWithZone:zone];
    return animationStepCopy;
}
@end
