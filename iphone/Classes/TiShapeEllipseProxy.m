//
//  TiShapeEllipseProxy.m
//  Titanium
//
//  Created by Martin Guillon on 26/08/13.
//
//

#import "TiShapeEllipseProxy.h"
#import "CustomShapeLayer.h"

@interface CustomEllipseShapeLayer: CustomShapeLayer
@end

@implementation CustomEllipseShapeLayer

- (UIBezierPath *)getBPath
{
    return [UIBezierPath bezierPathWithOvalInRect:[_proxy computeRect:self.center radius:self.radius]];
}
@end

@implementation TiShapeEllipseProxy

+ (Class)layerClass {
    return [CustomEllipseShapeLayer class];
}

-(TiPoint *)defaultRadius
{
	static TiPoint * defaultRadius;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		defaultRadius = [[TiPoint alloc] initWithObject:[NSDictionary dictionaryWithObjectsAndKeys:@"50%", @"x", @"50%", @"y", nil]];
	});
	return defaultRadius;
}


@end
