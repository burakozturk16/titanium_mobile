//
//  TiShapeRoundRectProxy.m
//  Titanium
//
//  Created by Martin Guillon on 26/08/13.
//
//

#import "TiShapeRoundedRectProxy.h"
#import "CustomShapeLayer.h"
#import "UIBezierPath+Additions.h"

static NSString * const kAnimCornerRadii = @"cornerRadii";

@interface CustomRoundedRectShapeLayer: CustomShapeLayer
@property(nonatomic,assign) CGFloat cornerRadii;
@property(nonatomic,assign) int corners;
@end

@implementation CustomRoundedRectShapeLayer
@synthesize cornerRadii, corners;

- (id)init {
    //a trick to make sure we add our animationKeys
    if (self = [super init])
    {
        cornerRadii = 0.0f;
        corners = UIRectCornerAllCorners;
    }
    return self;
}

- (id) initWithLayer:(id)layer
{
    self = [super initWithLayer:layer];
    if (self) {
        CustomRoundedRectShapeLayer *customLayer = (CustomRoundedRectShapeLayer *)layer;
        self.cornerRadii = customLayer.cornerRadii;
        self.corners = customLayer.corners;
    }
    return self;
}

static NSArray *animationKeys;
+ (NSArray *)animationKeys
{
    if (!animationKeys)
        animationKeys = [[[super animationKeys] arrayByAddingObjectsFromArray:[NSArray arrayWithObjects:kAnimCornerRadii,nil]] retain];
    
    return animationKeys;
}

+ (BOOL)needsDisplayForKey:(NSString *)key {
    if ([[self  animationKeys] indexOfObject:key] != NSNotFound)
        return YES;
    return [super needsDisplayForKey:key];
}

- (UIBezierPath *)getBPath
{
    return [UIBezierPath bezierPathWithCustomRoundedRect:[_proxy computeRect:self.center radius:self.radius] byRoundingCorners:self.corners cornerRadii:CGSizeMake(self.cornerRadii, self.cornerRadii)];
}
@end

@implementation TiShapeRoundedRectProxy

+ (Class)layerClass {
    return [CustomRoundedRectShapeLayer class];
}

-(void)prepareAnimation:(TiShapeAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps {
    
    [super prepareAnimation:animation holder:animations animProps:animProps];
    BOOL restartFromBeginning = animation.restartFromBeginning;
    
    [self addAnimationForKeyPath:kAnimCornerRadii restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
}

-(void)setCornerRadius:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSNumber);
    [self setLayerValue:(arg!=nil)?arg:[NSNumber numberWithFloat:0.0f] forKey:kAnimCornerRadii];
	[self replaceValue:arg forKey:@"cornerRadius" notification:YES];
}

-(void)setCorners:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSNumber);
    [self setLayerValue:(arg!=nil)?arg:[NSNumber numberWithInt:UIRectCornerAllCorners] forKey:@"corners"];
	[self replaceValue:arg forKey:@"corners" notification:YES];
}

@end