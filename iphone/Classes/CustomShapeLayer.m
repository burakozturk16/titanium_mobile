//
//  CustomShapeLayer.m
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import "CustomShapeLayer.h"
#import "ShapeCustomProxy.h"

@implementation BezierPoint
@synthesize curvePoint1, curvePoint2, curvePoint1Assigned, curvePoint2Assigned;


-(void)setCurvePoint1:(CGPoint)value
{
    curvePoint1 = value;
    curvePoint1Assigned = YES;
}

-(void)setCurvePoint2:(CGPoint)value
{
    curvePoint2 = value;
    curvePoint2Assigned = YES;
}

- (id)copyWithZone:(NSZone *)zone {
    BezierPoint* copy = [[BezierPoint alloc] init];
    copy.point = self.point;
    if(self.curvePoint1Assigned) {
        copy.curvePoint1 = self.curvePoint1;
    }
    if(self.curvePoint2Assigned) {
        copy.curvePoint2 = self.curvePoint2;
    }
    return copy;
}

@end

@implementation UIBezierPath(Custom)

+ (UIBezierPath *)bezierPathWithCustomRoundedRect:(CGRect)rect byRoundingCorners:(UIRectCorner)corners cornerRadii:(CGSize)cornerRadii
{
    CGMutablePathRef path = CGPathCreateMutable();
    
    const CGPoint topLeft = rect.origin;
    const CGPoint topRight = CGPointMake(CGRectGetMaxX(rect), CGRectGetMinY(rect));
    const CGPoint bottomRight = CGPointMake(CGRectGetMaxX(rect), CGRectGetMaxY(rect));
    const CGPoint bottomLeft = CGPointMake(CGRectGetMinX(rect), CGRectGetMaxY(rect));
    
    if (corners & UIRectCornerTopLeft) {
        CGPathMoveToPoint(path, NULL, topLeft.x+cornerRadii.width, topLeft.y);
    } else {
        CGPathMoveToPoint(path, NULL, topLeft.x, topLeft.y);
    }
    
    if (corners & UIRectCornerTopRight) {
        CGPathAddLineToPoint(path, NULL, topRight.x-cornerRadii.width, topRight.y);
        CGPathAddCurveToPoint(path, NULL, topRight.x, topRight.y, topRight.x, topRight.y+cornerRadii.height, topRight.x, topRight.y+cornerRadii.height);
    } else {
        CGPathAddLineToPoint(path, NULL, topRight.x, topRight.y);
    }
    
    if (corners & UIRectCornerBottomRight) {
        CGPathAddLineToPoint(path, NULL, bottomRight.x, bottomRight.y-cornerRadii.height);
        CGPathAddCurveToPoint(path, NULL, bottomRight.x, bottomRight.y, bottomRight.x-cornerRadii.width, bottomRight.y, bottomRight.x-cornerRadii.width, bottomRight.y);
    } else {
        CGPathAddLineToPoint(path, NULL, bottomRight.x, bottomRight.y);
    }
    
    if (corners & UIRectCornerBottomLeft) {
        CGPathAddLineToPoint(path, NULL, bottomLeft.x+cornerRadii.width, bottomLeft.y);
        CGPathAddCurveToPoint(path, NULL, bottomLeft.x, bottomLeft.y, bottomLeft.x, bottomLeft.y-cornerRadii.height, bottomLeft.x, bottomLeft.y-cornerRadii.height);
    } else {
        CGPathAddLineToPoint(path, NULL, bottomLeft.x, bottomLeft.y);
    }
    
    if (corners & UIRectCornerTopLeft) {
        CGPathAddLineToPoint(path, NULL, topLeft.x, topLeft.y+cornerRadii.height);
        CGPathAddCurveToPoint(path, NULL, topLeft.x, topLeft.y, topLeft.x+cornerRadii.width, topLeft.y, topLeft.x+cornerRadii.width, topLeft.y);
    } else {
        CGPathAddLineToPoint(path, NULL, topLeft.x, topLeft.y);
    }
    
    CGPathCloseSubpath(path);
    
    return [UIBezierPath bezierPathWithCGPath:path];
}
+ (UIBezierPath *)bezierPathWithPoints:(NSArray*)points {
    CGMutablePathRef path = CGPathCreateMutable();
    
    BOOL firstOne = YES;
    for (BezierPoint* bezierPoint in points) {
        if (firstOne) {
            firstOne = NO;
            CGPoint point = bezierPoint.point;
            CGPathMoveToPoint(path, NULL,point.x, point.y);
        }
        else {
            if (bezierPoint.curvePoint2Assigned && bezierPoint.curvePoint1Assigned) {
                CGPoint point = bezierPoint.point;
                CGPoint curvePoint1 = bezierPoint.curvePoint1;
                CGPoint curvePoint2 = bezierPoint.curvePoint2;
                CGPathAddCurveToPoint(path, NULL, curvePoint1.x, curvePoint1.y, curvePoint2.x, curvePoint2.y, point.x, point.y);
            }
            else if (bezierPoint.curvePoint1Assigned) {
                CGPoint point = bezierPoint.point;
                CGPoint curvePoint1 = bezierPoint.curvePoint1;
                CGPathAddQuadCurveToPoint(path, NULL, curvePoint1.x, curvePoint1.y, point.x, point.y);
            }
            else {
                CGPoint point = bezierPoint.point;
                CGPathAddLineToPoint(path, NULL, point.x, point.y);
            }
        }
    }
    return [UIBezierPath bezierPathWithCGPath:path];
}

+ (UIBezierPath *)bezierPathWithPieSliceCenter:(CGPoint)center radius:(CGFloat)radius innerRadius:(CGFloat)innerRadius startAngle:(CGFloat)startAngle endAngle:(CGFloat)endAngle
{
    CGMutablePathRef path = CGPathCreateMutable();
    
    BOOL clockwise = endAngle < startAngle;
    
    if (innerRadius == 0.0f) {
        CGPathMoveToPoint(path, NULL,center.x, center.y);
        CGPathAddArc(path, NULL, center.x, center.y, radius, startAngle, endAngle, clockwise);
    }
    else {
        CGPathAddArc(path, NULL, center.x, center.y, innerRadius, startAngle, endAngle, clockwise);
        CGPathAddArc(path, NULL, center.x, center.y, radius, endAngle, startAngle, !clockwise);
    }
    CGPathCloseSubpath(path);
    return [UIBezierPath bezierPathWithCGPath:path];
}
@end

@implementation CustomShapeLayer
@synthesize dashPhase, lineWidth, lineOpacity, fillOpacity, lineColor = _lineColor, proxy = _proxy, fillColor = _fillColor, fillGradient, lineGradient, lineCap, lineJoin, center, lineShadowRadius, lineShadowColor = _lineShadowColor, lineShadowOffset, radius, fillShadowOffset, fillShadowColor = _fillShadowColor, fillShadowRadius, lineImage, fillImage;

- (id)init {
    if (self = [super init])
    {
        self.needsDisplayOnBoundsChange = YES;
        self.shouldRasterize = NO;
//        self.allowsEdgeAntialiasing = YES;
        self.contentsScale = self.rasterizationScale = [UIScreen mainScreen].scale;
        
        fillOpacity = lineOpacity = 1.0f;
        lineShadowOffset = fillShadowOffset = CGSizeZero;
    }
    return self;
}

// Make sure that, when the layer is copied, so is the custom ivar
- (id) initWithLayer:(id)layer
{
    self = [super initWithLayer:layer];
    if (self) {
        CustomShapeLayer *customLayer = (CustomShapeLayer *)layer;
        self.proxy = customLayer.proxy;
        self.dashPhase = customLayer.dashPhase;
        self.dashPattern = customLayer.dashPattern;
        self.center = customLayer.center;
        self.radius = customLayer.radius;
        self.lineOpacity = customLayer.lineOpacity;
        self.fillOpacity = customLayer.fillOpacity;
        self.fillColor = customLayer.fillColor;
        self.lineColor = customLayer.lineColor;
        self.fillGradient = customLayer.fillGradient;
        self.lineGradient = customLayer.lineGradient;
        self.lineCap = customLayer.lineCap;
        self.lineJoin = customLayer.lineJoin;
        self.lineWidth = customLayer.lineWidth;
        self.lineShadowOffset = customLayer.lineShadowOffset;
        self.lineShadowColor = customLayer.lineShadowColor;
        self.lineShadowRadius = customLayer.lineShadowRadius;
        self.fillShadowOffset = customLayer.fillShadowOffset;
        self.fillShadowColor = customLayer.fillShadowColor;
        self.fillShadowRadius = customLayer.fillShadowRadius;
        self.lineImage = customLayer.lineImage;
        self.fillImage = customLayer.fillImage;
    }
    return self;
}

- (void) dealloc
{
	if (_fillColor) {
        [(id)_fillColor release];
        _fillColor = nil;
    }
    if (_lineColor) {
        [(id)_lineColor release];
        _lineColor = nil;
    }
    if (_fillShadowColor) {
        [(id)_fillShadowColor release];
        _fillShadowColor = nil;
    }
    if (_lineShadowColor) {
        [(id)_lineShadowColor release];
        _lineShadowColor = nil;
    }

	RELEASE_TO_NIL(fillGradient)
	RELEASE_TO_NIL(lineGradient)
	RELEASE_TO_NIL(_dashPattern)
	RELEASE_TO_NIL(lineImage)
	RELEASE_TO_NIL(fillImage)
	RELEASE_TO_NIL(_proxy)
    if (_cgDashPattern) free(_cgDashPattern);
    
	[super dealloc];
}

static NSArray *animationKeys;
+ (NSArray *)animationKeys
{
    if (!animationKeys)
        animationKeys = [[NSArray arrayWithObjects:@"lineColor",@"lineCap",@"lineJoin",@"lineOpacity"
                         ,@"fillColor",@"fillOpacity", @"lineWidth"
                         , @"center", @"dashPattern", @"dashPhase", @"radius", @"center",@"lineShadowColor",@"fillShadowColor",@"lineShadowOffset",@"fillShadowOffset",@"lineShadowRadius",@"fillShadowRadius",nil] retain];
    
    return animationKeys;
}

+ (BOOL)needsDisplayForKey:(NSString *)key {
    if ([[self  animationKeys] indexOfObject:key] != NSNotFound)
        return YES;
    return [super needsDisplayForKey:key];
}

//- (id < CAAction >)actionForKey:(NSString *)key {
//    if ([self presentationLayer] != nil) {
//        if ([[self animationKeys] indexOfObject:key] != NSNotFound) {
//            CABasicAnimation *anim = [CABasicAnimation
//                                      animationWithKeyPath:key];
//            anim.fromValue = [[self presentationLayer] valueForKey:key];
//            return anim;
//        }
//    }
//
//    return [super actionForKey:key];
//}

- (UIBezierPath *)getBPath
{
    return [UIBezierPath bezierPath];
}

- (void)drawBPath:(UIBezierPath *)bPath_ inContext:(CGContextRef)context
{
    CGContextSetAllowsAntialiasing(context, YES);
    CGContextSetShouldAntialias(context, YES);
    CGRect currentBounds = CGRectZero;
    if (_fillColor || fillGradient || fillImage) {
        CGContextSaveGState(context);
        CGContextBeginPath(context);
        CGContextAddPath(context, bPath_.CGPath);
        CGContextSetAlpha(context, self.fillOpacity);
        if (_fillShadowColor) CGContextSetShadowWithColor(context, fillShadowOffset, fillShadowRadius, _fillShadowColor);
        CGContextSetFillColorWithColor(context, _fillColor);
        CGContextFillPath(context);
        
        if (fillGradient || fillImage) {
            currentBounds = CGContextGetPathBoundingBox(context);
            CGContextBeginPath(context);
            CGContextAddPath(context, bPath_.CGPath);
            CGContextClip(context);
            if (fillGradient) {
                [fillGradient paintContext:context bounds:self.bounds];
            }
            if (fillImage) {
                CGContextScaleCTM (context, 1, -1);
                CGRect imageRect = CGRectMake(0, 0, fillImage.size.width, fillImage.size.height);
                CGContextDrawTiledImage(context, imageRect, fillImage.CGImage);
            }
        }
       
        CGContextRestoreGState(context);
    }
    
    if (_lineColor || lineGradient || lineImage) {
        CGContextSaveGState(context);
        
        CGContextSetAlpha(context, self.lineOpacity);
        CGContextBeginPath(context);
        CGContextAddPath(context, bPath_.CGPath);
        
        if (_lineShadowColor) CGContextSetShadowWithColor(context, lineShadowOffset, lineShadowRadius, _lineShadowColor);
        CGContextSetStrokeColorWithColor(context, _lineColor);
        CGContextSetLineWidth(context, self.lineWidth);
        CGContextSetLineCap(context, lineCap);
        CGContextSetLineJoin(context, lineJoin);
        if (_dashPattern) {
            CGContextSetLineDash(context, dashPhase, _cgDashPattern, _dashPatternCount);
        }
        CGContextStrokePath(context);
        
        CGContextBeginPath(context);
        CGContextAddPath(context, bPath_.CGPath);
        CGContextReplacePathWithStrokedPath(context);
        currentBounds = CGContextGetPathBoundingBox(context);
        
        if (lineGradient || lineImage) {
            CGContextClip(context);
            if (lineGradient) {
                [lineGradient paintContext:context bounds:self.bounds];
            }
            if (lineImage) {
                CGContextScaleCTM (context, 1, -1);
                CGRect imageRect = CGRectMake(0, 0, fillImage.size.width, fillImage.size.height);
                CGContextDrawTiledImage(context, imageRect, lineImage.CGImage);
            }
        }
        CGContextRestoreGState(context);
    }
    if (_proxy && !CGRectEqualToRect(currentBounds, CGRectZero)) _proxy.currentBounds = currentBounds;
}

- (void)drawInContext:(CGContextRef)context
{
    UIBezierPath* bPath = [self getBPath];
    [self drawBPath:bPath inContext:context];
}

-(void) setDashPattern:(NSArray *)dashPattern
{
    RELEASE_TO_NIL(_dashPattern);
    if (_cgDashPattern) free(_cgDashPattern);
    _dashPattern = [dashPattern retain];
    if (_dashPattern) {
        _cgDashPattern = [_proxy arrayFromNSArray:dashPattern];
        _dashPatternCount = [dashPattern count];
    }
    else {
        _cgDashPattern = nil;
        _dashPatternCount = 0;
    }
}

-(void)setFillColor:(CGColorRef)value
{
    if (_fillColor) {
        [(id)_fillColor release];
        _fillColor = nil;
    }
    _fillColor = (CGColorRef)[(id)value retain];
}

-(void)setLineColor:(CGColorRef)value
{
    if (_lineColor) {
        [(id)_lineColor release];
        _lineColor = nil;
    }
    _lineColor = (CGColorRef)[(id)value retain];
}

-(void)setFillShadowColor:(CGColorRef)value
{
    if (_fillShadowColor) {
        [(id)_fillShadowColor release];
        _fillShadowColor = nil;
    }
    _fillShadowColor = (CGColorRef)[(id)value retain];
}

-(void)setLineShadowColor:(CGColorRef)value
{
    if (_lineShadowColor) {
        [(id)_lineShadowColor release];
        _lineShadowColor = nil;
    }
    _lineShadowColor = (CGColorRef)[(id)value retain];
}
@end
