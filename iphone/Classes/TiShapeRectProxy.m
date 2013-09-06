//
//  TiShapeRectProxy.m
//  Titanium
//
//  Created by Martin Guillon on 26/08/13.
//
//

#import "TiShapeRectProxy.h"
#import "CustomShapeLayer.h"


@interface CustomRectShapeLayer: CustomShapeLayer
@end

@implementation CustomRectShapeLayer

- (UIBezierPath *)getBPath
{
    return [UIBezierPath bezierPathWithRect:[_proxy computeRect:self.center radius:self.radius]];
}
@end

@implementation TiShapeRectProxy

+ (Class)layerClass {
    return [CustomRectShapeLayer class];
}


@end
