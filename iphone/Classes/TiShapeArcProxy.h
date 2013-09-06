//
//  TiShapeArcProxy.h
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import "ShapeCustomProxy.h"
#import "CustomShapeLayer.h"

@interface CustomArcShapeLayer: CustomShapeLayer
@property(nonatomic,assign) CGFloat startAngle;
@property(nonatomic,assign) CGFloat sweepAngle;
@end

@interface TiShapeArcProxy : ShapeCustomProxy
@end
