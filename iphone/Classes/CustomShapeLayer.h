//
//  CustomShapeLayer.h
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import <QuartzCore/QuartzCore.h>
#import "TiUIHelper.h"


static NSString * const kAnimRadius = @"radius";
static NSString * const kAnimCenter = @"center";
static NSString * const kAnimLineColor = @"lineColor";
static NSString * const kAnimLineOpacity = @"lineOpacity";
static NSString * const kAnimLineGradient = @"lineGradient";
static NSString * const kAnimLineImage = @"lineImage";
static NSString * const kAnimLineJoin = @"lineJoin";
static NSString * const kAnimLineWidth = @"lineWidth";
static NSString * const kAnimLineCap = @"lineCap";
static NSString * const kAnimFillColor = @"fillColor";
static NSString * const kAnimFillOpacity = @"fillOpacity";
static NSString * const kAnimFillGradient = @"fillGradient";
static NSString * const kAnimFillImage = @"fillImage";
static NSString * const kAnimFillInversed = @"fillInversed";
static NSString * const kAnimLineInversed = @"lineInversed";
static NSString * const kAnimShapeTransform = @"shapeTransform";

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
    
    CGColorRef _lineShadowColor;
    CGColorRef _fillShadowColor;
}
@property(nonatomic,assign) ShapeCustomProxy* proxy;
@property(nonatomic,retain) TiGradient* fillGradient;
@property(nonatomic,retain) TiGradient* lineGradient;
@property(nonatomic,retain) UIImage* lineImage;
@property(nonatomic,retain) UIImage* fillImage;
@property(nonatomic,retain) NSArray* dashPattern;
@property(nonatomic) CGColorRef lineColor;
@property(nonatomic) CGColorRef fillColor;
@property(nonatomic, assign) CATransform3D shapeTransform;

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
@property(nonatomic,assign) BOOL lineInversed;
@property(nonatomic,assign) BOOL fillInversed;
@property(nonatomic,assign) BOOL lineClipped;
@property(nonatomic,assign) BOOL retina;
@property(nonatomic,assign) BOOL antialiasing;

-(void) setDashPattern:(NSArray *)dashPattern;
+(NSArray *)animationKeys;
-(CGRect)getBoundingBox;
@end
