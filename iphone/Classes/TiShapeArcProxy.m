//
//  TiShapeArcProxy.m
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import "TiShapeArcProxy.h"

static NSString * const kAnimSweepAngle = @"sweepAngle";
static NSString * const kAnimStartAngle = @"startAngle";

@implementation CustomArcShapeLayer
@synthesize startAngle, sweepAngle;

- (id)init {
    //a trick to make sure we add our animationKeys
    if (self = [super init])
    {
    }
    return self;
}

- (id) initWithLayer:(id)layer
{
    self = [super initWithLayer:layer];
    if (self) {
        CustomArcShapeLayer *customLayer = (CustomArcShapeLayer *)layer;
        self.startAngle = customLayer.startAngle;
        self.sweepAngle = customLayer.sweepAngle;
    }
    return self;
}

static NSArray *animationKeys;
+ (NSArray *)animationKeys
{
    if (!animationKeys)
        animationKeys = [[[super animationKeys] arrayByAddingObjectsFromArray:[NSArray arrayWithObjects:kAnimStartAngle,kAnimSweepAngle,nil]] retain];
     
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
    return [UIBezierPath bezierPathWithArcCenter:self.center radius:self.radius.width startAngle:realStart endAngle:realEnd clockwise:_proxy.clockwise];
}
@end

@implementation TiShapeArcProxy

+ (Class)layerClass {
    return [CustomArcShapeLayer class];
}

-(void)prepareAnimation:(TiShapeAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps {
    
    [super prepareAnimation:animation holder:animations animProps:animProps];
    BOOL restartFromBeginning = animation.restartFromBeginning;

    [self addAnimationForKeyPath:kAnimSweepAngle restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
    [self addAnimationForKeyPath:kAnimStartAngle restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
    
}

-(void)setStartAngle:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSNumber);
    [self setLayerValue:(arg!=nil)?arg:[NSNumber numberWithFloat:1.0f] forKey:kAnimStartAngle];
	[self replaceValue:arg forKey:kAnimStartAngle notification:YES];
}

-(void)setSweepAngle:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSNumber);
    [self setLayerValue:(arg!=nil)?arg:[NSNumber numberWithFloat:1.0f] forKey:kAnimSweepAngle];
	[self replaceValue:arg forKey:kAnimSweepAngle notification:YES];
}

@end
