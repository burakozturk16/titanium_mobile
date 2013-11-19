//
//  ShapeCustomProxy.h
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import "ShapeProxy.h"
#import "TiUIHelper.h"


@class TiGradient, TiShapeAnimation;
@interface ShapeCustomProxy : ShapeProxy
{
    TiPoint* _center;
}
@property(nonatomic,retain) TiPoint* center;

@property(nonatomic,assign) int anchor;
@property(nonatomic,assign) BOOL clockwise;

-(void)prepareAnimation:(TiAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps;
-(void)setLayerValue:(id)value forKey:(NSString*)key;
-(CABasicAnimation*) animation;
-(CABasicAnimation *)animationForKeyPath:(NSString*)keyPath_ value:(id)value_ restartFromBeginning:(BOOL)restartFromBeginning_;
-(CABasicAnimation *)addAnimationForKeyPath:(NSString*)keyPath_ restartFromBeginning:(BOOL)restartFromBeginning_ animation:(TiAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps;
-(CALayer*)getLayer;
-(NSMutableArray*)animationsForShapeAnimation:(TiShapeAnimation*)animation;
@end
