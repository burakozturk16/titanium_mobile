//
//  TiPr.m
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "ShapeProxy.h"
#import "TiShapeViewProxy.h"
#import "TiUtils.h"
#import "TiShapeAnimation.h"
#import "Ti2DMatrix.h"
#import "TiUIHelper.h"


@interface ShapeProxy()
{
    TiShapeViewProxy* _shapeViewProxy;
}

@end
@implementation ShapeProxy
@synthesize shapeViewProxy = _shapeViewProxy, operations = _operations, type, currentBounds = _currentBounds, transform = _transform;


+ (Class)layerClass {
    return [CALayer class];
}

- (id)init {
    if (self = [super init])
    {
        mShapes = [[NSMutableArray alloc] init];
        _parentBounds = CGRectZero;
        _currentBounds = CGRectZero;
        _layer = [[[[self class] layerClass] alloc] init];
        _layer.masksToBounds = NO;
        _configurationSet = NO;
        _realTransform = CGAffineTransformIdentity;
        type = -1;
    }
    return self;
}


+(ShapeProxy*)shapeFromArg:(id)args context:(id<TiEvaluator>)context
{
	id arg = nil;
	BOOL isArray = NO;
	
	if ([args isKindOfClass:[ShapeProxy class]])
	{
		return (ShapeProxy*)args;
	}
	else if ([args isKindOfClass:[NSArray class]])
	{
		isArray = YES;
		arg = [args objectAtIndex:0];
		if ([arg isKindOfClass:[ShapeProxy class]])
		{
			return (ShapeProxy*)arg;
		}
	}
    if (![arg isKindOfClass:[NSDictionary class]]) return nil;
    
	return [[[ShapeProxy alloc] _initWithPageContext:context args:args] autorelease];
}

-(TiPoint *)defaultCenter
{
	static TiPoint *defaultCenter = nil;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		defaultCenter = [[TiPoint alloc] initWithObject:[NSDictionary dictionaryWithObjectsAndKeys:@"0%", @"x", @"0%", @"y", nil]];
	});
	return defaultCenter;
}

-(TiPoint *)defaultRadius
{
	static TiPoint * defaultRadius;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		defaultRadius = [[TiPoint alloc] initWithObject:[NSDictionary dictionaryWithObjectsAndKeys:@"50%", @"x", nil]];
	});
	return defaultRadius;
}



-(void)applyProperties:(id)args
{
    [CATransaction begin];
    [CATransaction setDisableActions: YES];
    [super applyProperties:args];
    [self update:nil];
    [CATransaction commit];
    
}

- (void)setShapeViewProxy:(TiShapeViewProxy*) proxy {
    _shapeViewProxy = [proxy retain];
    
    for (int i = 0; i < [mShapes count]; i++) {
        ShapeProxy* shapeProxy = [mShapes objectAtIndex:i];
        [shapeProxy setShapeViewProxy:proxy];
    }
}

-(void)updateRealTransform
{
    if (_transform) {
        _realTransform = [_transform matrixInViewSize:_currentBounds.size andParentSize:_parentBounds.size decaleCenter:NO];
    }
}

-(void)setTransform:(id)transform
{
    ENSURE_SINGLE_ARG_OR_NIL(transform, Ti2DMatrix)
    _realTransform = CGAffineTransformIdentity;
    if (transform != nil) {
        _transform = [transform retain];
    }
    else {
        _transform = nil;
    }
}


- (void) dealloc
{
    RELEASE_TO_NIL(_layer);
    RELEASE_TO_NIL(_fillLayer);
    RELEASE_TO_NIL(_strokeLayer);
    RELEASE_TO_NIL(_shapeViewProxy);
    RELEASE_TO_NIL(_transform);
    RELEASE_TO_NIL(_operations);
    
    CGPathRelease(path);
	[super dealloc];
}

-(void)updatePath
{
    if (path != nil) {
        CGPathRelease(path);
        path = nil;
    }
    path = CGPathCreateMutable();
    [self updatePath:path];
    
    if (_strokeLayer) {
        _strokeLayer.path = path;
    }
    if (_fillLayer) {
        _fillLayer.path = path;
    }
}

-(CGPoint) computePoint:(TiPoint*)center_ withAnchor:(int)anchor_ inSize:(CGSize)size_ decale:(CGSize)decale_
{
    CGFloat width = size_.width;
    CGFloat height = size_.height;
    CGFloat decaleX = decale_.width;
    CGFloat decaleY = decale_.height;
    CGPoint cgCenter = CGPointZero;
    if (anchor_ == ShapeAnchorCenter) {
        cgCenter = [center_ pointWithinSize:CGSizeMake(width - 2* decaleX, height - 2* decaleY)];
        cgCenter.x += width/2;
        cgCenter.y += height/2;
    }
    else {
        if (anchor_ == ShapeAnchorRightTop ||
            anchor_ == ShapeAnchorRightMiddle ||
            anchor_ == ShapeAnchorRightBottom) {
            cgCenter.x =TiDimensionCalculateValue(center_.xDimension, width - 2* decaleX);
            cgCenter.x = width - cgCenter.x - decaleX;
        } else if(anchor_ == ShapeAnchorTopMiddle ||
                  anchor_ == ShapeAnchorBottomMiddle)
        {
            cgCenter.x =TiDimensionCalculateValue(center_.xDimension, width/2 - decaleX);
            cgCenter.x += width/2;
        }
        else {
            cgCenter.x =TiDimensionCalculateValue(center_.xDimension, width - 2* decaleX);
            cgCenter.x += decaleX;
        }
        if (anchor_ == ShapeAnchorLeftBottom ||
            anchor_ == ShapeAnchorBottomMiddle ||
            anchor_ == ShapeAnchorRightBottom) {
            cgCenter.y =TiDimensionCalculateValue(center_.yDimension, height - 2* decaleY);
            cgCenter.y = height - cgCenter.y - decaleY;
        }
        else if(anchor_ == ShapeAnchorLeftMiddle ||
                anchor_ == ShapeAnchorRightMiddle) {
            cgCenter.y =TiDimensionCalculateValue(center_.yDimension, height/2 - decaleY);
            cgCenter.y += height/2;
        }
        else {
            cgCenter.y =TiDimensionCalculateValue(center_.yDimension, height - 2* decaleY);
            cgCenter.y += decaleY;
        }
        
    }
    return cgCenter;
}

-(CGRect) computeRect:(CGPoint)center_ radius:(CGSize)radius
{
    return CGRectMake(center_.x - radius.width, center_.y - radius.height, 2*radius.width, 2*radius.height);
}

CGPathRef CGPathCreateRoundRect( const CGRect r, const CGFloat cornerRadius )
{
	CGMutablePathRef p = CGPathCreateMutable() ;
	
	CGPathMoveToPoint( p, NULL, r.origin.x + cornerRadius, r.origin.y ) ;
	
	CGFloat maxX = CGRectGetMaxX( r ) ;
	CGFloat maxY = CGRectGetMaxY( r ) ;
	
	CGPathAddArcToPoint( p, NULL, maxX, r.origin.y, maxX, r.origin.y + cornerRadius, cornerRadius ) ;
	CGPathAddArcToPoint( p, NULL, maxX, maxY, maxX - cornerRadius, maxY, cornerRadius ) ;
	
	CGPathAddArcToPoint( p, NULL, r.origin.x, maxY, r.origin.x, maxY - cornerRadius, cornerRadius ) ;
	CGPathAddArcToPoint( p, NULL, r.origin.x, r.origin.y, r.origin.x + cornerRadius, r.origin.y, cornerRadius ) ;
	
	return p ;
}

-(TiPoint*)tiPointValue:(NSString*)name properties:(NSDictionary*)properties def:(TiPoint*)def
{
	TiPoint* center = [TiUtils tiPointValue:name properties:properties def:[self defaultCenter]];
	if (center == nil) return def;
    return center;
}

-(TiDimension)dimensionValue:(NSString*)name properties:(NSDictionary*)properties def:(TiDimension)def
{
	NSString* str = [TiUtils stringValue:name properties:properties def:nil];
	if (str == nil) return def;
    return TiDimensionFromObject(str);
}

-(CGSize) getRadius:(CGSize)size inProperties:(NSDictionary*)properties
{
    CGSize radius = CGSizeZero;
    TiPoint* radiusPoint = [TiPoint pointWithObject:nil];
    
    BOOL needsMin = NO;
    id obj = [properties objectForKey:@"radius"];
    if (obj == nil) {
        radiusPoint = [self defaultRadius];
        needsMin = YES;
    }
    else if ([obj isKindOfClass:[NSDictionary class]]) {
        [radiusPoint setValues:obj];
    }
    else {
        [radiusPoint setX:obj];
        needsMin = YES;
    }
    if (!TiDimensionIsUndefined(radiusPoint.xDimension) && !TiDimensionIsUndefined(radiusPoint.yDimension)) {
        CGPoint result = [radiusPoint pointWithinSize:size];
        radius = CGSizeMake(result.x, result.y);
    } else if(!TiDimensionIsUndefined(radiusPoint.xDimension)) {
        CGFloat result = TiDimensionCalculateValue(radiusPoint.xDimension, needsMin?(MIN(size.width, size.height)):size.width);
        radius = CGSizeMake(result, result);
    } else if(!TiDimensionIsUndefined(radiusPoint.yDimension)) {
        CGFloat result = TiDimensionCalculateValue(radiusPoint.yDimension, size.height);
        radius = CGSizeMake(result, result);
    }
    return radius;
}

-(void)applyOperation:(int)operation toPath:(CGMutablePathRef)path_ withProperties:(NSDictionary*)properties
{
    CGSize size = _parentBounds.size;
    CGFloat width = size.width;
    CGFloat height = size.height;
    

    int anchor = [TiUtils intValue:@"anchor" properties:properties def:ShapeAnchorCenter];
    BOOL clockwise = [TiUtils intValue:@"clockwise" properties:properties def:NO];
    CGSize radius = [self getRadius:size inProperties:properties];
    TiPoint* center = [self tiPointValue:@"center" properties:properties def:[self defaultCenter]];
    switch (operation) {
        case ShapeOpCircle:
        {
            CGFloat fRadius = radius.width;
            CGPoint cgCenter = [self computePoint:center withAnchor:anchor inSize:_parentBounds.size decale:CGSizeMake(fRadius, fRadius)];
            
            CGPathAddArc(path_, NULL, cgCenter.x, cgCenter.y, fRadius, -M_PI_2, M_PI_2*3, clockwise);
            break;
        }
        case ShapeOpRect:
        {
            CGPoint cgCenter = [self computePoint:center withAnchor:anchor inSize:_parentBounds.size decale:radius];
            
            CGPathAddRect(path_, NULL, [self computeRect:cgCenter radius:radius]);
            break;
        }
        case ShapeOpRoundedRect:
        {
            CGFloat cornerRadius = [TiUtils floatValue:@"cornerRadius" properties:properties def:0.0f];
            CGPoint cgCenter = [self computePoint:center withAnchor:anchor inSize:_parentBounds.size decale:radius];
            
            CGRect rect = [self computeRect:cgCenter radius:radius];
            if (cornerRadius == 0.0f)
                CGPathAddRect(path_, NULL, rect);
            else
                CGPathAddPath(path_, NULL, CGPathCreateRoundRect(rect,cornerRadius)) ;
            break;
        }
        case ShapeOpArc:
        {
            CGFloat fRadius = radius.width;
            CGPoint cgCenter = [self computePoint:center withAnchor:anchor inSize:_parentBounds.size decale:CGSizeMake(fRadius, fRadius)];
            CGFloat startAngle = ([TiUtils floatValue:@"startAngle" properties:properties def:0] - 90)*M_PI /180;
            CGFloat sweepAngle = ([TiUtils floatValue:@"sweepAngle" properties:properties def:360])*M_PI /180;
            
            CGPathAddArc(path_, NULL, cgCenter.x, cgCenter.y, fRadius, startAngle, startAngle + sweepAngle, clockwise);
        }
        case ShapeOpPoints:
        {
            NSArray* points = [properties objectForKey:@"points"];
            if (points) {
                TiPoint* tiPoint = [[TiPoint alloc] init];
                NSArray* firstPoint = [points objectAtIndex:0];
                if ([firstPoint count] >= 2) {
                    [tiPoint setX:[firstPoint objectAtIndex:0]];
                    [tiPoint setY:[firstPoint objectAtIndex:1]];
                    CGPoint cgpoint = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                    CGPathMoveToPoint(path_, NULL, cgpoint.x, cgpoint.y);
                }
                else return;
                for (int i = 1; i < [points count]; i++) {
                    NSArray* point = [points objectAtIndex:i];
                    if ([point count] == 6) {
                        [tiPoint setX:[point objectAtIndex:0]];
                        [tiPoint setY:[point objectAtIndex:1]];
                        CGPoint cgpoint = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                        [tiPoint setX:[point objectAtIndex:1]];
                        [tiPoint setY:[point objectAtIndex:2]];
                        CGPoint cgcurve = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                        [tiPoint setX:[point objectAtIndex:3]];
                        [tiPoint setY:[point objectAtIndex:4]];
                        CGPoint cgcurve2 = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                        CGPathAddCurveToPoint(path_, NULL, cgcurve.x, cgcurve.y, cgcurve2.x, cgcurve2.y, cgpoint.x, cgpoint.y);
                    } else if ([point count] == 4) {
                        [tiPoint setX:[point objectAtIndex:0]];
                        [tiPoint setY:[point objectAtIndex:1]];
                        CGPoint cgpoint = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                        [tiPoint setX:[point objectAtIndex:1]];
                        [tiPoint setY:[point objectAtIndex:2]];
                        CGPoint cgcurve = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                        CGPathAddQuadCurveToPoint(path_, NULL, cgcurve.x, cgcurve.y, cgpoint.x, cgpoint.y);
                    } else if ([point count] == 2) {
                        [tiPoint setX:[point objectAtIndex:0]];
                        [tiPoint setY:[point objectAtIndex:1]];
                        CGPoint cgpoint = [self computePoint:tiPoint withAnchor:anchor inSize:_parentBounds.size decale:CGSizeZero];
                        CGPathAddLineToPoint(path_, NULL, cgpoint.x, cgpoint.y);
                    }
                }
            }
        }
        default:
            break;
    }
}

-(void)applyOperations:(NSArray*)ops toPath:(CGMutablePathRef)path_
{
    [ops enumerateObjectsUsingBlock:^(NSDictionary* op, NSUInteger index, BOOL *stop) {
        id obj = [op valueForKey:@"type"];
        int opType = -1;
        if ([obj isKindOfClass:[NSString class]]) {
            opType = [self opFromString:[TiUtils stringValue:obj]];
        }
        else {
            opType = [TiUtils intValue:[op valueForKey:@"type"]];
        }
        if (opType >= 0 && opType < ShapeOperationNb) {
            [self applyOperation:opType toPath:path_ withProperties:op];
        }
    }];
}

-(void)updatePath:(CGMutablePathRef)path_
{
    if (type >= 0 && type < ShapeOperationNb) {
        [self applyOperation:type toPath:path_ withProperties:[self allProperties]];
    }
    else if (_operations) {
        [self applyOperations:_operations toPath:path_];
    }
}

-(void)updatePath:(CGMutablePathRef)path_ forAnimation:(TiShapeAnimation*)animation
{
    if (type >= 0 && type < ShapeOperationNb) {
        NSMutableDictionary* animProps = [NSMutableDictionary dictionaryWithDictionary:[animation allProperties]];
        [[self allProperties] enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
            if (![animProps valueForKey:key]) {
                [animProps setObject:[self valueForKey:key] forKey:key];
            }
        }];
        [self applyOperation:type toPath:path_ withProperties:animProps];
    }
    else if ([animation valueForKey:@"operations"]) {
        id obj = [animation valueForKey:@"operations"];
        ENSURE_TYPE_OR_NIL(obj, NSArray)
        [self applyOperations:obj toPath:path_];
    }
}

-(void) _initWithProperties:(NSDictionary *)properties
{
    _configurationSet = NO;
    [super _initWithProperties:properties];
    _configurationSet = YES;
}

-(CGFloat*) arrayFromNSArray:(NSArray*)array_
{
    int count = [array_ count];
    CGFloat* result = (CGFloat *) malloc(sizeof(CGFloat) * count);
    for (int index = 0; index < count; index++) {
        result[index] = [[array_ objectAtIndex: index] floatValue];
    }
    return result;
}

-(CGPathRef)getBoudingPath:(CGPathRef)path_
{
    CGPathRef result = CGPathCreateCopyByStrokingPath(path_ , NULL,
                                                      _strokeLayer.lineWidth,
                                                      _strokeLayer.lineCap,
                                                      _strokeLayer.lineJoin,
                                                      _strokeLayer.miterLimit);
    if ([self valueForKey:@"dashPattern"]) {
        NSArray* pattern = _strokeLayer.lineDashPattern;
        CGFloat* array = [self arrayFromNSArray:pattern];
        result = CGPathCreateCopyByDashingPath(result, NULL, _strokeLayer.lineDashPhase, array, [pattern count]);
        free(array);
    }
    return result;
}

-(CGRect)getBoudingBox
{
    CGPathRef path_ = path;
    if (_strokeLayer)
    {
        path_ = [self getBoudingPath:path_];
//        if (_strokeGradientLayer) {
//            ((CAShapeLayer*)_strokeGradientLayer.mask).path = path_;
//        }
    }
    return CGPathGetBoundingBox(path_);
}


-(void)setCurrentBounds:(CGRect)currentBounds
{
    CGRect roundedBounds = CGRectMake(roundf(currentBounds.origin.x), roundf(currentBounds.origin.y), roundf(currentBounds.size.width), roundf(currentBounds.size.height));
    if (CGRectEqualToRect(_currentBounds, roundedBounds)) return;
    _currentBounds = roundedBounds;
    for (int i = 0; i < [mShapes count]; i++) {
        ShapeProxy* shapeProxy = [mShapes objectAtIndex:i];
        [shapeProxy boundsChanged:_currentBounds];
    }
}

-(void)updateRect:(CGRect) parentBounds
{
    _parentBounds = parentBounds;
    _layer.frame = _parentBounds;
    if (_strokeLayer)
        _strokeLayer.frame = _layer.bounds;
    if (_fillLayer)
        _fillLayer.frame = _layer.bounds;
    
//    if (_strokeGradientLayer)
//        _strokeGradientLayer.frame = _layer.bounds;
//    if (_fillGradientLayer)
//        _fillGradientLayer.frame = _layer.bounds;
    
    [self updatePath];
    
    self.currentBounds = [self getBoudingBox];
    
    [self updateRealTransform];
    if (!CGAffineTransformIsIdentity(_realTransform)) {
        path = CGPathCreateMutableCopyByTransformingPath(path, &_realTransform);
        if (_strokeLayer) {
            _strokeLayer.path = path;
        }
        if (_fillLayer) {
            _fillLayer.path = path;
        }
    }
    
}

-(void)boundsChanged:(CGRect)bounds
{
    if (CGSizeEqualToSize(bounds.size,CGSizeZero)) return;
    [self updateRect:bounds];
    
    if (pendingAnimation_ != nil) {
        [self animate:pendingAnimation_];
        RELEASE_TO_NIL(pendingAnimation_);
    }
//    [CATransaction commit];
}

-(CAShapeLayer*) getOrCreateStrokeLayer
{
    if (_strokeLayer == nil) {
        _strokeLayer = [[CAShapeLayer alloc] init];
        _strokeLayer.masksToBounds = NO;
        _strokeLayer.opaque= NO;
        _strokeLayer.shouldRasterize = YES;
        _strokeLayer.rasterizationScale = [[UIScreen mainScreen] scale];
        _strokeLayer.fillColor= [UIColor clearColor].CGColor;
        _strokeLayer.frame = _layer.frame;
        if (_fillLayer) {
            [_layer insertSublayer:_strokeLayer above:_fillLayer];
        }
        else {
            [_layer addSublayer:_strokeLayer];
        }
    }
    return _strokeLayer;
}

-(CAShapeLayer*) getOrCreateFillLayer
{
    if (_fillLayer == nil) {
        _fillLayer = [[CAShapeLayer alloc] init];
        _fillLayer.masksToBounds = NO;
        _fillLayer.opaque= NO;
        _fillLayer.rasterizationScale = [[UIScreen mainScreen] scale];
        _fillLayer.shouldRasterize = YES;
//        _fillLayer.fillColor= [UIColor clearColor].CGColor;
//        _fillLayer.delegate = self;
        _fillLayer.frame = [_layer frame];
        _fillLayer.contentsScale = [[UIScreen mainScreen] scale];
        if (_strokeLayer) {
            [_layer insertSublayer:_fillLayer below:_strokeLayer];
        }
        else {
            [_layer addSublayer:_fillLayer];
        }
    }
    return _fillLayer;
}

//-(TiGradientLayer*) getOrCreateStrokeGradientLayer
//{
//    if (_strokeGradientLayer == nil) {
//        _strokeGradientLayer = [[TiGradientLayer alloc] init];
//        _strokeGradientLayer.masksToBounds = YES;
//        _strokeGradientLayer.opaque= NO;
//        _strokeGradientLayer.needsDisplayOnBoundsChange = YES;
//        _strokeGradientLayer.frame = _currentBounds;
//        _strokeGradientLayer.mask = [CAShapeLayer layer];
//        [[self getOrCreateStrokeLayer] addSublayer:_strokeGradientLayer];
//    }
//    return _strokeGradientLayer;
//}
//
//-(TiGradientLayer*) getOrCreateFillGradientLayer
//{
//    if (_fillGradientLayer == nil) {
//        _fillGradientLayer = [[TiGradientLayer alloc] init];
//        _fillGradientLayer.masksToBounds = YES;
//        _fillGradientLayer.opaque= NO;
//        _fillGradientLayer.frame = _currentBounds;
//        [[self getOrCreateFillLayer] addSublayer:_fillGradientLayer];
//    }
//    return _fillGradientLayer;
//}

-(NSString*)lineCapToString:(int)value
{
    switch (value) {
        case kCGLineCapRound:
            return @"round";
        case kCGLineCapSquare:
            return @"square";
        default:
        case kCGLineCapButt:
            return @"butt";
    }
}

-(NSString*)lineJoinToString:(int)value
{
    switch (value) {
        case kCGLineJoinBevel:
            return @"bevel";
        case kCGLineJoinRound:
            return @"round";
        default:
        case kCGLineJoinMiter:
            return @"miter";
    }
}

-(int)opFromString:(NSString*)value
{
    if (value == nil) return -1;
	if ([value isEqualToString:@"circle"])
	{
		return ShapeOpCircle;
	}
	else if ([value isEqualToString:@"rect"])
	{
		return ShapeOpRect;
	}
    else if ([value isEqualToString:@"roundedrect"])
	{
		return ShapeOpRoundedRect;
	}
    else if ([value isEqualToString:@"arc"])
	{
		return ShapeOpArc;
	}
    else if ([value isEqualToString:@"ellipse"])
	{
		return ShapeOpEllipse;
	}
    else if ([value isEqualToString:@"points"])
	{
		return ShapeOpPoints;
	}
	return -1;
}

-(void)setLineColor:(id)color
{
    [self getOrCreateStrokeLayer].strokeColor = [[TiUtils colorValue:color] _color].CGColor;
	[self replaceValue:color forKey:@"lineColor" notification:YES];
}

-(void)setFillColor:(id)color
{
    [self getOrCreateFillLayer].fillColor = [[TiUtils colorValue:color] _color].CGColor;
	[self replaceValue:color forKey:@"fillColor" notification:YES];
}

//-(void)setLineGradient:(id)arg
//{
//    [self getOrCreateStrokeGradientLayer].gradient = [TiGradient gradientFromObject:arg proxy:self];
//	[self replaceValue:arg forKey:@"lineGradient" notification:YES];
//}
//
//-(void)setFillGradient:(id)arg
//{
//    [self getOrCreateFillGradientLayer].gradient = [TiGradient gradientFromObject:arg proxy:self];
//	[self replaceValue:arg forKey:@"fillGradient" notification:YES];
//}

-(void)setLineWidth:(id)arg
{
    [self getOrCreateStrokeLayer].lineWidth = [TiUtils floatValue:arg def:1];
	[self replaceValue:arg forKey:@"lineWidth" notification:YES];
}


-(void)setLineJoin:(id)arg
{
    if ([arg isKindOfClass:[NSNumber class]]) {
        [self getOrCreateStrokeLayer].lineJoin = [self lineJoinToString:[TiUtils intValue:arg def:kCGLineJoinMiter]];
    }
    else {
        [self getOrCreateStrokeLayer].lineJoin = [TiUtils stringValue:arg];
    }
	[self replaceValue:arg forKey:@"lineJoin" notification:YES];
}

-(void)setLineCap:(id)arg
{
    if ([arg isKindOfClass:[NSNumber class]]) {
        [self getOrCreateStrokeLayer].lineCap = [self lineCapToString:[TiUtils intValue:arg def:kCGLineCapButt]];
    }
    else {
        [self getOrCreateStrokeLayer].lineCap = [TiUtils stringValue:arg];
    }
	[self replaceValue:arg forKey:@"lineCap" notification:YES];
}

-(void)setLineOpacity:(id)arg
{
    [self getOrCreateStrokeLayer].opacity = [TiUtils floatValue:arg def:1.0f];
	[self replaceValue:arg forKey:@"lineOpacity" notification:YES];
}

-(void)setFillOpacity:(id)arg
{
    [self getOrCreateFillLayer].opacity = [TiUtils floatValue:arg def:1.0f];
	[self replaceValue:arg forKey:@"fillOpacity" notification:YES];
}

-(void)setLineShadow:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    [TiUIHelper applyShadow:arg toLayer:[self getOrCreateStrokeLayer]];
    [self replaceValue:arg forKey:@"lineShadow" notification:YES];
}

-(void)setLineDash:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    
    if ([arg objectForKey:@"pattern"]) {
        NSArray* value  = [arg objectForKey:@"pattern"];
        [self getOrCreateStrokeLayer].lineDashPattern = value;
    }
    if ([arg objectForKey:@"phase"]) {
        [self getOrCreateStrokeLayer].lineDashPhase = [TiUtils floatValue:[arg objectForKey:@"phase"]];
    }
	[self replaceValue:arg forKey:@"lineDash" notification:YES];
}

-(void)setFillShadow:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    [TiUIHelper applyShadow:arg toLayer:[self getOrCreateFillLayer]];
    [self replaceValue:arg forKey:@"fillShadow" notification:YES];
}

-(void)update
{
    if (!_configurationSet)  return;
    ENSURE_UI_THREAD_0_ARGS
    [CATransaction begin];
    [CATransaction setDisableActions: YES];
    [self boundsChanged:_parentBounds];
    [CATransaction commit];
}

-(void)update:(id)arg {
    [self update];
}

-(void)add:(id) arg {
	ShapeProxy * proxy = [ShapeProxy shapeFromArg:arg context:[self executionContext]];
    if (proxy != nil && [mShapes indexOfObject:proxy] == NSNotFound) {
        [mShapes addObject:proxy];
        [_layer addSublayer:[proxy layer]];
        if (_strokeLayer)
            _strokeLayer.frame = _parentBounds;
        if (_fillLayer)
            _fillLayer.frame = _parentBounds;
        if (_shapeViewProxy != nil) {
            [proxy setShapeViewProxy:self.shapeViewProxy];
            [proxy boundsChanged:_currentBounds];
        }
    }
}

-(void)remove:(id) proxy {
    ENSURE_SINGLE_ARG_OR_NIL(proxy, ShapeProxy)
    if ([mShapes indexOfObject:proxy] != NSNotFound) {
        [mShapes removeObject:proxy];
        [[proxy layer] removeFromSuperlayer];
        [proxy setShapeViewProxy:nil];
    }
}

-(CGMutablePathRef)pathForAnimation:(TiShapeAnimation*)animation
{
    CGMutablePathRef path_ = CGPathCreateMutable();
    [self updatePath:path_ forAnimation:animation];
    return path_;
}

-(void)cancelAllAnimations:(id)arg
{
	[CATransaction begin];
    if (_strokeLayer) {
        [_strokeLayer removeAllAnimations];
    }
    if (_fillLayer) {
        [_fillLayer removeAllAnimations];
    }
	[CATransaction commit];
}

-(void)animate:(id)arg
{
	TiShapeAnimation * newAnimation = [TiShapeAnimation animationFromArg:arg context:[self executionContext] create:NO];
	[self rememberProxy:newAnimation];
    if ([self.shapeViewProxy view])
        [self handleAnimation:newAnimation];
    else {
        pendingAnimation_ = [newAnimation retain];
    }
}

-(void)animationDidComplete:(TiShapeAnimation*)animation
{
	[self forgetProxy:animation];
}

-(void)handleAnimation:(TiShapeAnimation*)animation
{
	ENSURE_UI_THREAD(handleAnimation,animation)
    CGFloat duration = animation.duration/1000;
    BOOL autoreverse = animation.autoreverse;
    BOOL restartFromBeginning = animation.restartFromBeginning;
    int repeat = animation.repeat - 1;
    NSMutableArray* strokeAnimations = [ NSMutableArray array];
    NSMutableArray* fillAnimations = [ NSMutableArray array];
    CABasicAnimation* animSkeleton = [[CABasicAnimation alloc] init];
    
    if (restartFromBeginning) {
        [self cancelAllAnimations:nil];
    }

    animation.animatedProxy = self;
    CGMutablePathRef path_ = [self pathForAnimation:animation];
    if (path_ != nil) {
        CABasicAnimation *caAnim = [animSkeleton copy];
        caAnim.keyPath = @"path";
        caAnim.toValue = (id)path_;
        if (restartFromBeginning) caAnim.fromValue = (id)(_strokeLayer?_strokeLayer.path:_fillLayer.path);
        
        [strokeAnimations addObject:caAnim];
        [fillAnimations addObject:caAnim];
        CFRelease(path_);
    }
    
    if ([animation valueForKey:@"lineDash"]) {
        NSDictionary* lineDash = [animation valueForKey:@"lineDash"];
        if ([lineDash objectForKey:@"pattern"]) {
            CABasicAnimation *caAnim = [animSkeleton copy];
            caAnim.keyPath = @"lineDashPattern";
            caAnim.toValue = [lineDash objectForKey:@"pattern"];
            if (restartFromBeginning) caAnim.fromValue = (id)[_strokeLayer lineDashPattern];

            [strokeAnimations addObject:caAnim];
        }
        if ([lineDash objectForKey:@"phase"]) {
            CABasicAnimation *caAnim = [animSkeleton copy];
            caAnim.keyPath = @"lineDashPhase";
            caAnim.toValue = [lineDash objectForKey:@"phase"];
            if (restartFromBeginning) caAnim.fromValue = (id)[NSNumber numberWithFloat:[_strokeLayer lineDashPhase]];
            [strokeAnimations addObject:caAnim];
        }
    }
    if ([animation valueForKey:@"lineJoin"]) {
        CABasicAnimation *caAnim = [animSkeleton copy];
        caAnim.keyPath = @"lineJoin";
        caAnim.toValue = [animation valueForKey:@"lineJoin"];
        if (restartFromBeginning) caAnim.fromValue = (id)[NSNumber numberWithInt:[_strokeLayer lineJoin]];
        [strokeAnimations addObject:caAnim];
    }
    
    if ([animation valueForKey:@"lineColor"]) {
        UIColor* color = [[TiUtils colorValue:[animation valueForKey:@"lineColor"]] _color];
        if (color == nil) color = [UIColor clearColor];
        CABasicAnimation *caAnim = [animSkeleton copy];
        caAnim.keyPath = @"strokeColor";
        caAnim.toValue = (id)color.CGColor;
        if (restartFromBeginning) caAnim.fromValue = (id)[_strokeLayer strokeColor];
        [strokeAnimations addObject:caAnim];
    }
    if ([animation valueForKey:@"fillColor"]) {
        UIColor* color = [[TiUtils colorValue:[animation valueForKey:@"fillColor"]] _color];
        if (color == nil) color = [UIColor clearColor];
        CABasicAnimation *caAnim = [animSkeleton copy];
        caAnim.keyPath = @"fillColor";
        caAnim.toValue = (id)color.CGColor;
        if (restartFromBeginning) caAnim.fromValue = (id)[_fillLayer fillColor];
        [fillAnimations addObject:caAnim];
    }
    
    if (_strokeLayer) {
        CAAnimationGroup *group = [CAAnimationGroup animation];
        group.animations = [strokeAnimations copy];
        group.delegate = animation;
        group.duration = duration;
        group.autoreverses = autoreverse;
        group.repeatCount = repeat;
        group.fillMode = kCAFillModeForwards;
        [_strokeLayer addAnimation:group forKey:nil];
    }
    if (_fillLayer) {
        CAAnimationGroup *group = [CAAnimationGroup animation];
        group.animations = [fillAnimations copy];
        group.delegate = animation;
        group.duration = duration;
        group.autoreverses = autoreverse;
        group.repeatCount = repeat;
        group.fillMode = kCAFillModeForwards;
        [_fillLayer addAnimation:group forKey:nil];
    }
    [animSkeleton release];
}

@end
