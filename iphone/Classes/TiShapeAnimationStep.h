#import "HLSLayerAnimationStep.h"

@class TiShapeAnimation, ShapeProxy;
@interface TiShapeAnimationStep : HLSLayerAnimationStep {
@private
}

- (void)addShapeAnimation:(TiShapeAnimation *)shapeAnimation forShape:(ShapeProxy *)shape;

@end
