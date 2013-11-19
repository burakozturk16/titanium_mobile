//
//  TiShapeRoundRectProxy.m
//  Titanium
//
//  Created by Martin Guillon on 26/08/13.
//
//

#import "TiShapePieSliceProxy.h"
#import "UIBezierPath+Additions.h"

static NSString * const kAnimInnerRadius = @"innerRadius";

@interface CustomPieSliceShapeLayer: CustomArcShapeLayer
@property(nonatomic,assign) CGFloat innerRadius;
@end

@implementation CustomPieSliceShapeLayer
@synthesize innerRadius;

- (id)init {
    //a trick to make sure we add our animationKeys
    if (self = [super init])
    {
        innerRadius = 0.0f;
    }
    return self;
}


- (void) dealloc
{
    [super dealloc];
}

- (id) initWithLayer:(id)layer
{
    self = [super initWithLayer:layer];
    if (self) {
        CustomPieSliceShapeLayer *customLayer = (CustomPieSliceShapeLayer *)layer;
        self.innerRadius = customLayer.innerRadius;
    }
    return self;
}

static NSArray *animationKeys;
+ (NSArray *)animationKeys
{
    if (!animationKeys)
        animationKeys = [[[super animationKeys] arrayByAddingObject:kAnimInnerRadius] retain];
    
    return animationKeys;
}

+ (BOOL)needsDisplayForKey:(NSString *)key {
    if ([[self  animationKeys] indexOfObject:key] != NSNotFound)
        return YES;
    return [super needsDisplayForKey:key];
}

- (UIBezierPath *)getBPath
{
    CGFloat realStart = (self.startAngle - 90)*M_PI /180;
    CGFloat realEnd = _proxy.clockwise?(realStart + self.sweepAngle*M_PI /180):(realStart - self.sweepAngle*M_PI /180);
    return [UIBezierPath bezierPathWithPieSliceCenter:self.center radius:self.radius.width innerRadius:innerRadius startAngle:realStart endAngle:realEnd];
}
@end

@implementation TiShapePieSliceProxy

+ (Class)layerClass {
    return [CustomPieSliceShapeLayer class];
}


-(TiPoint *)defaultInnerRadius
{
	static TiPoint * defaultInnerRadius;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		defaultInnerRadius = [[TiPoint alloc] initWithObject:[NSDictionary dictionaryWithObjectsAndKeys:@"0%", @"x", nil]];
	});
	return defaultInnerRadius;
}



-(CGSize) getInnerRadius:(CGSize)size inProperties:(NSDictionary*)properties
{
    CGSize radius = CGSizeZero;
    TiPoint* radiusPoint = [TiPoint pointWithObject:nil];
    
    BOOL needsMin = NO;
    id obj = [properties objectForKey:kAnimInnerRadius];
    if (obj == nil) {
        radiusPoint = [self defaultInnerRadius];
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

-(void)updateRect:(CGRect) parentBounds
{
    [super updateRect:parentBounds];
    
    CGSize radius = [self getInnerRadius:_layer.bounds.size inProperties:[self allProperties]];
    
    [_layer setValue:[NSNumber numberWithFloat:radius.width] forKey:kAnimInnerRadius];
}

-(void)prepareAnimation:(TiAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps {
    
    [super prepareAnimation:animation holder:animations animProps:animProps];
    BOOL restartFromBeginning = animation.restartFromBeginning;
    
    if ([animation valueForKey:kAnimInnerRadius]) {
        CGFloat innerRadius = [self getInnerRadius:_layer.bounds.size inProperties:animProps].width;
        if ( innerRadius != ((CustomPieSliceShapeLayer*)_layer).innerRadius) {
            [animations addObject:[self animationForKeyPath:kAnimInnerRadius value:[NSNumber numberWithFloat:innerRadius] restartFromBeginning:restartFromBeginning]];
        }
    }
    
    [self addAnimationForKeyPath:kAnimInnerRadius restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
}


@end