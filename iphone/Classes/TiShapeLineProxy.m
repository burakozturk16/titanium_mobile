//
//  TiShapeRoundRectProxy.m
//  Titanium
//
//  Created by Martin Guillon on 26/08/13.
//
//

#import "TiShapeLineProxy.h"
#import "CustomShapeLayer.h"
#import "UIBezierPath+Additions.h"

static NSString * const kAnimPoints = @"points";
@interface CustomLineShapeLayer: CustomShapeLayer
@property (nonatomic, readwrite, strong) NSDictionary *points;
@end

@implementation CustomLineShapeLayer
@synthesize points;

- (id)init {
    //a trick to make sure we add our animationKeys
    if (self = [super init])
    {
        self.points = [NSDictionary dictionary];
    }
    return self;
}

- (id) initWithLayer:(id)layer
{
    self = [super initWithLayer:layer];
    if (self) {
        CustomLineShapeLayer *customLayer = (CustomLineShapeLayer *)layer;
        self.points = [[[NSDictionary alloc] initWithDictionary:customLayer.points copyItems:YES] autorelease];

    }
    return self;
}

static NSArray *animationKeys;
+ (NSArray *)animationKeys
{
    if (!animationKeys)
        animationKeys = [[[super animationKeys] arrayByAddingObject:kAnimPoints] retain];
    
    return animationKeys;
}

+ (BOOL)needsDisplayForKey:(NSString *)key {
    if ([[self  animationKeys] indexOfObject:key] != NSNotFound)
        return YES;
    return [super needsDisplayForKey:key];
}

- (UIBezierPath *)getBPath
{
    NSArray *keys = [points allKeys];
    NSArray *sKeys = [keys sortedArrayUsingComparator: ^(id num1, id num2) {
        
        int v1 = [num1 intValue];
        int v2 = [num2 intValue];
        if (v1 < v2)
            return NSOrderedAscending;
        else if (v1 > v2)
            return NSOrderedDescending;
        else
            return NSOrderedSame;
    }];
    NSMutableArray *sValues = [[[NSMutableArray alloc] init] autorelease];
    
    for(id k in sKeys) {
        id val = [points objectForKey:k];
        [sValues addObject:val];
    }

    return [UIBezierPath bezierPathWithPoints:sValues];
}
@end

@implementation TiShapeLineProxy

+ (Class)layerClass {
    return [CustomLineShapeLayer class];
}


- (id)init {
    if (self = [super init])
    {
        self.anchor = ShapeAnchorLeftMiddle;
    }
    return self;
}


-(void)updateLayerPoints
{
    CGSize size = _layer.bounds.size;
    NSMutableDictionary* bezierPoints = [NSMutableDictionary dictionary];
    
    [_points enumerateKeysAndObjectsUsingBlock:^(NSString* key, NSDictionary* point, BOOL *stop) {
        BezierPoint* bezierPoint = [[[BezierPoint alloc] init] autorelease];
        bezierPoint.point = [self computePoint:[point objectForKey:@"point"] withAnchor:self.anchor inSize:size decale:CGSizeZero];
        if ([point objectForKey:@"curvePoint1"]) {
            bezierPoint.curvePoint1 = [self computePoint:[point objectForKey:@"curvePoint1"] withAnchor:self.anchor inSize:size decale:CGSizeZero];
        }
        if ([point objectForKey:@"curvePoint2"]) {
            bezierPoint.curvePoint2 = [self computePoint:[point objectForKey:@"curvePoint2"] withAnchor:self.anchor inSize:size decale:CGSizeZero];
        }
        [bezierPoints setObject:bezierPoint forKey:key];
    }];
    [_layer setValue:bezierPoints forKey:kAnimPoints];
    [_layer setNeedsDisplay];

}

-(void)updateRect:(CGRect) parentBounds
{
    [super updateRect:parentBounds];
    [self updateLayerPoints];
}

-(void)setPoints:(id)points {
    NSMutableDictionary* result = [NSMutableDictionary dictionary];
    if (points) {
        for (int i = 0; i < [points count]; i++) {
            NSArray* point = [points objectAtIndex:i];
            NSMutableDictionary* bezierPoint = [NSMutableDictionary dictionary];
            if ([point count] >= 2) {
                TiPoint* tiPoint = [[[TiPoint alloc] init] autorelease];
                [tiPoint setX:[point objectAtIndex:0]];
                [tiPoint setY:[point objectAtIndex:1]];
                [bezierPoint setObject:tiPoint forKey:@"point"];
                if ([point count] >= 4) {
                    TiPoint* tiPoint = [[[TiPoint alloc] init] autorelease];
                    [tiPoint setX:[point objectAtIndex:2]];
                    [tiPoint setY:[point objectAtIndex:3]];
                    [bezierPoint setObject:tiPoint forKey:@"curvePoint1"];
                    if ([point count] >= 6) {
                        TiPoint* tiPoint = [[[TiPoint alloc] init] autorelease];
                        [tiPoint setX:[point objectAtIndex:4]];
                        [tiPoint setY:[point objectAtIndex:5]];
                        [bezierPoint setObject:tiPoint forKey:@"curvePoint2"];
                    }
                }
                
            }
            
            [result setObject:bezierPoint forKey:[NSString stringWithFormat:@"%d", i]];
        }
    }
    _points = [result copy];
	[self replaceValue:points forKey:@"points" notification:YES];
}

-(void)animationDidComplete:(TiAnimation*)animation
{
	[super animationDidComplete:animation];
    if (animation.autoreverse) {
        [self updateLayerPoints];
        [_layer setNeedsDisplay];
    }
}


-(void)prepareAnimation:(TiAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps {
    
    [super prepareAnimation:animation holder:animations animProps:animProps];
    BOOL restartFromBeginning = animation.restartFromBeginning;
    
    if ([animation valueForKey:kAnimPoints]) {
        CGSize size = _layer.bounds.size;
        NSArray* points = [animation valueForKey:kAnimPoints];
        if ([points count] == [_points count]) {
            for (int i = 0; i < [points count]; i++) {
                NSString* key = [NSString stringWithFormat:@"%d", i];
                NSArray* point = [points objectAtIndex:i];
                BezierPoint* currentPoint = ((BezierPoint*)[((CustomLineShapeLayer*)_layer).points objectForKey:key]);
                TiPoint* tiPoint = [[[TiPoint alloc] init] autorelease];
                if ([point count] >= 2) {
                    [tiPoint setX:[point objectAtIndex:0]];
                    [tiPoint setY:[point objectAtIndex:1]];
                    CGPoint newPoint = [self computePoint:tiPoint withAnchor:self.anchor inSize:size decale:CGSizeZero];
                    if (!CGPointEqualToPoint(currentPoint.point, newPoint)) {
                        NSString* keyPath = [NSString stringWithFormat:@"%@.%@.point", kAnimPoints, key];
                        [animations addObject:[self animationForKeyPath:keyPath value:[NSValue valueWithCGPoint:newPoint] restartFromBeginning:restartFromBeginning]];
                    }
                    if ([point count] >= 4) {
                        [tiPoint setX:[point objectAtIndex:2]];
                        [tiPoint setY:[point objectAtIndex:3]];
                        CGPoint newPoint = [self computePoint:tiPoint withAnchor:self.anchor inSize:size decale:CGSizeZero];
                        if (!CGPointEqualToPoint(currentPoint.curvePoint1, newPoint)) {
                            NSString* keyPath = [NSString stringWithFormat:@"%@.%@.curvePoint1", kAnimPoints, key];
                            [animations addObject:[self animationForKeyPath:keyPath value:[NSValue valueWithCGPoint:newPoint] restartFromBeginning:restartFromBeginning]];
                        }
                        if ([point count] >= 6) {
                            [tiPoint setX:[point objectAtIndex:4]];
                            [tiPoint setY:[point objectAtIndex:5]];
                            CGPoint newPoint = [self computePoint:tiPoint withAnchor:self.anchor inSize:size decale:CGSizeZero];
                            if (!CGPointEqualToPoint(currentPoint.curvePoint2, newPoint)) {
                                NSString* keyPath = [NSString stringWithFormat:@"%@.%@.curvePoint2", kAnimPoints, key];
                                [animations addObject:[self animationForKeyPath:keyPath value:[NSValue valueWithCGPoint:newPoint] restartFromBeginning:restartFromBeginning]];
                            }
                        }
                    }
                    
                }
                
            }
        }
        
    }
}


@end