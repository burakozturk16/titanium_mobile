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
    CGRect _parentBounds;
    CGRect _currentBounds;
    CGRect _currentShapeBounds;
    CALayer* _layer;
}

@property(nonatomic,assign) TiShapeViewProxy* shapeViewProxy;
@property(nonatomic,retain) NSArray* operations;
@property(nonatomic,retain) Ti2DMatrix* transform;
@property(nonatomic,readonly) CGRect parentBounds;
@property(nonatomic,assign) CGRect currentBounds;
@property(nonatomic,assign) CGRect currentShapeBounds;
@property(nonatomic,readonly) CGAffineTransform realTransform;
//@property(nonatomic,readonly) CALayer* layer;

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
-(CGAffineTransform)getRealTransform:(CGRect)bounds parentSize:(CGSize)parentSize;
-(CGAffineTransform)prepareTransform:(Ti2DMatrix*)matrix bounds:(CGRect)bounds parentSize:(CGSize)parentSize;
-(BOOL) handleTouchEvent:(NSString*)eventName withObject:(id)data propagate:bubbles point:(CGPoint)point;
-(void)removeFromSuperLayer;
-(CALayer*) layer;
@end
