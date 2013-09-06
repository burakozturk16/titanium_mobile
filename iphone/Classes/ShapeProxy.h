//
//  TiPr.h
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "TiProxy.h"
#import "TiShapeAnimation.h"
#import "TiGradient.h"

enum
{
	ShapeAnchorTopMiddle = 0,
	ShapeAnchorLeftTop,
	ShapeAnchorLeftMiddle,
	ShapeAnchorLeftBottom,
	ShapeAnchorRightTop,
	ShapeAnchorRightMiddle,
	ShapeAnchorRightBottom,
	ShapeAnchorBottomMiddle,
	ShapeAnchorCenter
    
};

enum
{
	ShapeOpRect = 0,
	ShapeOpPoints,
	ShapeOpCircle,
	ShapeOpArc,
	ShapeOpEllipse,
	ShapeOpRoundedRect,
    ShapeOperationNb
} ShapeOperation;

@class TiShapeViewProxy;
@class TiShapeAnimation;
@class Ti2DMatrix;
@interface ShapeProxy : TiProxy<TiAnimatableProxy>
{
    NSMutableArray* mShapes;
    CGMutablePathRef path;
    CGRect _parentBounds;
    CGRect _currentBounds;
    CALayer* _layer;
    CAShapeLayer* _strokeLayer;
    CAShapeLayer* _fillLayer;
    BOOL _configurationSet;
    TiShapeAnimation* pendingAnimation_;
//    TiGradientLayer* _strokeGradientLayer;
//    TiGradientLayer* _fillGradientLayer;
    Ti2DMatrix* _transform;
    CGAffineTransform _realTransform;
    NSArray* _operations;
}

@property(nonatomic,retain) TiShapeViewProxy* shapeViewProxy;
@property(nonatomic,retain) NSArray* operations;
@property(nonatomic,retain) Ti2DMatrix* transform;
@property(nonatomic,assign) int type;
@property(nonatomic,assign) CGRect currentBounds;
@property(nonatomic,readonly) CALayer* layer;
-(void)boundsChanged:(CGRect)bounds;
-(CGPoint) computePoint:(TiPoint*)center withAnchor:(int)anchor inSize:(CGSize)size decale:(CGSize)decale;
-(CGFloat*) arrayFromNSArray:(NSArray*)array;
-(TiPoint *)defaultCenter;
-(TiPoint *)defaultRadius;
-(CGSize) getRadius:(CGSize)size inProperties:(NSDictionary*)properties;
-(TiPoint*)tiPointValue:(NSString*)name properties:(NSDictionary*)properties def:(TiPoint*)def;
-(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties def:(TiDimension)def;
+(ShapeProxy*)shapeFromArg:(id)args context:(id<TiEvaluator>)context;
-(CGRect) computeRect:(CGPoint)center radius:(CGSize)radius;
-(void)updateRect:(CGRect) parentBounds;
-(void)animationDidComplete:(TiShapeAnimation*)animation;
-(void)update;

@end
