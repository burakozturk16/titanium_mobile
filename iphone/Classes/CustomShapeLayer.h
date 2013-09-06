//
//  CustomShapeLayer.h
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import <QuartzCore/QuartzCore.h>
#import "TiUIHelper.h"

@interface BezierPoint : NSObject<NSCopying>
@property (nonatomic, readwrite, assign) CGPoint point;
@property (nonatomic, readwrite, assign) CGPoint curvePoint1;
@property (nonatomic, readwrite, assign) CGPoint curvePoint2;
@property (nonatomic, readonly) BOOL curvePoint2Assigned;
@property (nonatomic, readonly) BOOL curvePoint1Assigned;

@end

@interface UIBezierPath(Custom)
+ (UIBezierPath *)bezierPathWithCustomRoundedRect:(CGRect)rect byRoundingCorners:(UIRectCorner)corners cornerRadii:(CGSize)cornerRadii;
+ (UIBezierPath *)bezierPathWithPoints:(NSArray*)points;
+ (UIBezierPath *)bezierPathWithPieSliceCenter:(CGPoint)center radius:(CGFloat)radius innerRadius:(CGFloat)innerRadius startAngle:(CGFloat)startAngle endAngle:(CGFloat)endAngle;

@end

@class ShapeCustomProxy;
@class TiPoint;
@class TiGradient;
@interface CustomShapeLayer : CALayer
{
    ShapeCustomProxy* _proxy;
    CGFloat* _cgDashPattern;
    NSArray* _dashPattern;
    size_t _dashPatternCount;
    CGColorRef _lineColor;
    CGColorRef _fillColor;
    UIImage* _lineImage;
    UIImage* _fillImage;
    
    CGColorRef _lineShadowColor;
    CGColorRef _fillShadowColor;
}
@property(nonatomic,retain) ShapeCustomProxy* proxy;
@property(nonatomic,retain) TiGradient* fillGradient;
@property(nonatomic,retain) TiGradient* lineGradient;
@property(nonatomic,retain) UIImage* lineImage;
@property(nonatomic,retain) UIImage* fillImage;
@property(nonatomic,retain) NSArray* dashPattern;
@property(nonatomic) CGColorRef lineColor;
@property(nonatomic) CGColorRef fillColor;

@property(nonatomic,assign) CGPoint center;
@property(nonatomic,assign) CGSize radius;
@property(nonatomic,assign) CGFloat lineOpacity;
@property(nonatomic,assign) CGFloat fillOpacity;
@property(nonatomic,assign) CGLineCap lineCap;
@property(nonatomic,assign) CGLineJoin lineJoin;
@property(nonatomic,assign) CGFloat dashPhase;
@property(nonatomic,assign) CGFloat lineWidth;


@property(nonatomic,assign) CGColorRef lineShadowColor;
@property(nonatomic,assign) CGFloat lineShadowRadius;
@property(nonatomic,assign) CGSize lineShadowOffset;
@property(nonatomic,assign) CGColorRef fillShadowColor;
@property(nonatomic,assign) CGFloat fillShadowRadius;
@property(nonatomic,assign) CGSize fillShadowOffset;

-(void) setDashPattern:(NSArray *)dashPattern;
+(NSArray *)animationKeys;
@end
