//
//  CustomShapeLayer.h
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import <QuartzCore/QuartzCore.h>
#import "TiUIHelper.h"



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

-(void) setDashPattern:(NSArray *)dashPattern;
+(NSArray *)animationKeys;
@end
