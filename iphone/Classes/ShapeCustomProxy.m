//
//  ShapeCustomProxy.m
//  Titanium
//
//  Created by Martin Guillon on 24/08/13.
//
//

#import "ShapeCustomProxy.h"
#import "CustomShapeLayer.h"
#import "TiShapeViewProxy.h"
#import "ImageLoader.h"

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


@implementation ShapeCustomProxy
@synthesize center = _center, anchor, clockwise;
+ (Class)layerClass {
    return [CustomShapeLayer class];
}

- (id)init {
    if (self = [super init])
    {
        ((CustomShapeLayer*)_layer).proxy = self;
        _center = [[TiPoint alloc] initWithObject:[NSDictionary dictionaryWithObjectsAndKeys:@"0%", @"x", @"0%", @"y", nil]];
        anchor = ShapeAnchorCenter;
        clockwise = YES;
    }
    return self;
}

- (UIBezierPath *)getBPath
{
    return [UIBezierPath bezierPath];
}

-(void)boundsChanged:(CGRect)bounds
{
    BOOL animating = self.shapeViewProxy && [self.shapeViewProxy animating];
    if (!animating) {
        [CATransaction begin];
        [CATransaction setDisableActions: YES];
    }
    [super boundsChanged:bounds];
    if (!animating) {
        [CATransaction commit];
    }
}

-(void)updateRect:(CGRect) parentBounds
{
    _layer.frame = _parentBounds = parentBounds;
    
    CGFloat width = parentBounds.size.width;
    CGFloat height = parentBounds.size.height;
    CGSize radius = [self getRadius:parentBounds.size inProperties:[self allProperties]];
    CGPoint cgCenter = [self computePoint:_center withAnchor:self.anchor inSize:parentBounds.size decale:radius];
    
    [_layer setValue:[NSValue valueWithCGSize:radius] forKey:kAnimRadius];
    [_layer setValue:[NSValue valueWithCGPoint:cgCenter] forKey:kAnimCenter];
}

- (void) dealloc
{
    RELEASE_TO_NIL(_center)
	[super dealloc];
}


-(CGLineJoin)lineJoinFromString:(NSString*)value
{
	if ([value isEqualToString:@"miter"])
	{
		return kCGLineJoinMiter;
	}
	else if ([value isEqualToString:@"round"])
	{
		return kCGLineJoinRound;
	}
	return kCGLineJoinBevel;
}

-(CGLineCap)lineCapFromString:(NSString*)value
{
	if ([value isEqualToString:@"square"])
	{
		return kCGLineCapSquare;
	}
	else if ([value isEqualToString:@"round"])
	{
		return kCGLineCapRound;
	}
	return kCGLineCapButt;
}


-(void)setLayerValue:(id)value forKey:(NSString*)key {
    if (![NSThread isMainThread]) {
        TiThreadPerformOnMainThread(^{
            [self setLayerValue:value forKey:key];
        },NO);
    }
    else {
        [CATransaction begin];
        [CATransaction setDisableActions: YES];
        [_layer setValue:value forKey:key];
        [_layer setNeedsDisplay];
        [CATransaction commit];
    }
}

-(void)setCenter:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    RELEASE_TO_NIL(_center)
    _center = [[TiUtils tiPointValue:arg def:[self defaultCenter]] retain];
	[self replaceValue:arg forKey:kAnimCenter notification:YES];
}

-(void)setLineColor:(id)color
{
    [self setLayerValue:(id)[[TiUtils colorValue:color] cgColor] forKey:kAnimLineColor];
	[self replaceValue:color forKey:kAnimLineColor notification:YES];
}

-(void)setFillColor:(id)color
{
    [self setLayerValue:(id)[[TiUtils colorValue:color] cgColor] forKey:kAnimFillColor];
	[self replaceValue:color forKey:kAnimFillColor notification:YES];
}

-(void)setLineJoin:(id)arg
{
    int result;
    if ([arg isKindOfClass:[NSString class]]) {
        result = [self lineJoinFromString:[TiUtils stringValue:arg]];
    }
    else {
        result = [TiUtils intValue:arg def:kCGLineJoinMiter];
    }
    [self setLayerValue:[NSNumber numberWithInt:result] forKey:kAnimLineJoin];
	[self replaceValue:arg forKey:kAnimLineJoin notification:YES];
}

-(void)setLineCap:(id)arg
{
    int result;
    if ([arg isKindOfClass:[NSString class]]) {
        result = [self lineCapFromString:[TiUtils stringValue:arg]];
    }
    else {
        result = [TiUtils intValue:arg def:kCGLineCapButt];
    }
    [self setLayerValue:[NSNumber numberWithInt:result] forKey:kAnimLineCap];
	[self replaceValue:arg forKey:kAnimLineCap notification:YES];
}

-(void)setLineDash:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    
    if ([arg objectForKey:@"pattern"]) {
        [self setLayerValue:[arg objectForKey:@"pattern"] forKey:@"dashPattern"];
    }
    if ([arg objectForKey:@"phase"]) {
        [self setLayerValue:[NSNumber numberWithFloat:[TiUtils floatValue:[arg objectForKey:@"phase"]]] forKey:@"dashPhase"];
    }
	[self replaceValue:arg forKey:@"lineDash" notification:YES];
}

-(void)setLineWidth:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSNumber);
    [self setLayerValue:(arg!=nil)?arg:[NSNumber numberWithFloat:1.0f] forKey:kAnimLineWidth];
	[self replaceValue:arg forKey:kAnimLineWidth notification:YES];
}

-(void)setLineShadow:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    ShadowDef shadow = [TiUIHelper getShadow:arg];
    [self setLayerValue:(id)shadow.color forKey:@"lineShadowColor"];
    [self setLayerValue:[NSValue valueWithCGSize:shadow.offset]  forKey:@"lineShadowOffset"];
    [self setLayerValue:[NSNumber numberWithFloat:shadow.radius]  forKey:@"lineShadowRadius"];
    [self replaceValue:arg forKey:@"lineShadow" notification:YES];
}

-(void)setFillShadow:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    ShadowDef shadow = [TiUIHelper getShadow:arg];
    [self setLayerValue:(id)shadow.color forKey:@"fillShadowColor"];
    [self setLayerValue:[NSValue valueWithCGSize:shadow.offset]  forKey:@"fillShadowOffset"];
    [self setLayerValue:[NSNumber numberWithFloat:shadow.radius]  forKey:@"fillShadowRadius"];
    [self replaceValue:arg forKey:@"fillShadow" notification:YES];
}

-(void)setLineGradient:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    [self setLayerValue:[TiGradient gradientFromObject:arg proxy:self] forKey:kAnimLineGradient];
	[self replaceValue:arg forKey:kAnimLineGradient notification:YES];
}

-(void)setFillGradient:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary);
    [self setLayerValue:[TiGradient gradientFromObject:arg proxy:self] forKey:kAnimFillGradient];
	[self replaceValue:arg forKey:kAnimFillGradient notification:YES];
}


-(UIImage*)loadImage:(id)arg
{
    if (arg==nil) return nil;
    UIImage *image = nil;
	
    if ([arg isKindOfClass:[TiBlob class]]) {
        TiBlob *blob = (TiBlob*)arg;
        image = [blob image];
    }
    else if ([arg isKindOfClass:[UIImage class]]) {
		// called within this class
        image = (UIImage*)arg;
    }
    else {
        NSURL *url;
        if ([arg isKindOfClass:[TiFile class]]) {
            TiFile *file = (TiFile*)arg;
            url = [NSURL fileURLWithPath:[file path]];
        }
        else {
            url = [TiUtils toURL:arg proxy:self];
        }
        image =  [[ImageLoader sharedLoader]loadImmediateImage:url];
    }
	return image;
}

-(void)setLineImage:(id)arg
{
    [self setLayerValue:[self loadImage:arg] forKey:kAnimLineImage];
	[self replaceValue:arg forKey:kAnimLineImage notification:YES];
}

-(void)setFillImage:(id)arg
{
    [self setLayerValue:[self loadImage:arg] forKey:kAnimFillImage];
	[self replaceValue:arg forKey:kAnimFillImage notification:YES];
}

-(void)setLineInversed:(id)arg
{
    [self setLayerValue:arg forKey:kAnimLineInversed];
	[self replaceValue:arg forKey:kAnimLineInversed notification:YES];
}

-(void)setFillInversed:(id)arg
{
    [self setLayerValue:arg forKey:kAnimFillInversed];
	[self replaceValue:arg forKey:kAnimFillInversed notification:YES];
}

-(void)setLineClipped:(id)arg
{
    [self setLayerValue:arg forKey:@"lineClipped"];
	[self replaceValue:arg forKey:@"lineClipped" notification:YES];
}

-(CABasicAnimation *)animationForKeyPath:(NSString*)keyPath_ value:(id)value_ restartFromBeginning:(BOOL)restartFromBeginning_
{
    CABasicAnimation *caAnim = [self animation];
    caAnim.keyPath = keyPath_;
    caAnim.toValue = value_;
    if (restartFromBeginning_) caAnim.fromValue = [_layer valueForKeyPath:keyPath_];
    return caAnim;
}

-(CABasicAnimation *)addAnimationForKeyPath:(NSString*)keyPath_ restartFromBeginning:(BOOL)restartFromBeginning_ animation:(TiShapeAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps
{
    if ([animation valueForKey:keyPath_]) {
        [animations addObject:[self animationForKeyPath:keyPath_ value:[animProps objectForKey:keyPath_] restartFromBeginning:restartFromBeginning_]];
    }
}
-(void)prepareAnimation:(TiShapeAnimation*)animation holder:(NSMutableArray*)animations animProps:(NSDictionary*)animProps {
 
    BOOL restartFromBeginning = animation.restartFromBeginning;
    if ([animation valueForKey:kAnimLineColor]) {
        UIColor* color = [[TiUtils colorValue:[animProps objectForKey:kAnimLineColor]] _color];
        if (color == nil) color = [UIColor clearColor];
        [animations addObject:[self animationForKeyPath:kAnimLineColor value:(id)color.CGColor restartFromBeginning:restartFromBeginning]];
    }
    if ([animation valueForKey:kAnimFillColor]) {
        UIColor* color = [[TiUtils colorValue:[animProps objectForKey:kAnimFillColor]] _color];
        if (color == nil) color = [UIColor clearColor];
        [animations addObject:[self animationForKeyPath:kAnimFillColor value:(id)color.CGColor restartFromBeginning:restartFromBeginning]];
    }
    
    [self addAnimationForKeyPath:kAnimLineWidth restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
    [self addAnimationForKeyPath:kAnimLineOpacity restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
    [self addAnimationForKeyPath:kAnimFillOpacity restartFromBeginning:restartFromBeginning animation:animation holder:animations animProps:animProps];
    
    if ([animation valueForKey:kAnimCenter] || [animation valueForKey:kAnimRadius]) {
        CGFloat width = _layer.bounds.size.width;
        CGFloat height = _layer.bounds.size.height;
        CGSize radius = [self getRadius:_layer.bounds.size inProperties:animProps];
        TiPoint* center_ = [self tiPointValue:kAnimCenter properties:animProps def:[self defaultCenter]];
        CGPoint cgCenter = [self computePoint:center_ withAnchor:anchor inSize:_parentBounds.size decale:radius];
        
        if ( !CGPointEqualToPoint(cgCenter, ((CustomShapeLayer*)_layer).center)) {
            [animations addObject:[self animationForKeyPath:kAnimCenter value:[NSValue valueWithCGPoint:cgCenter] restartFromBeginning:restartFromBeginning]];
        }
        if ( !CGSizeEqualToSize(radius, ((CustomShapeLayer*)_layer).radius)) {
            [animations addObject:[self animationForKeyPath:kAnimRadius value:[NSValue valueWithCGSize:radius] restartFromBeginning:restartFromBeginning]];
        }
    }
}

-(CABasicAnimation*) animation
{
    CABasicAnimation *anim = [CABasicAnimation animation];
    anim.fillMode = kCAFillModeBoth;
    return anim;
}

-(void)cancelAllAnimations:(id)arg
{
	[CATransaction begin];
	[[_layer presentationLayer] removeAllAnimations];
	[CATransaction commit];
}

-(void)handleAnimation:(TiShapeAnimation*)animation
{
	ENSURE_UI_THREAD(handleAnimation,animation)
    
    NSMutableArray* animations = [ NSMutableArray array];
    animation.animatedProxy = self;
    CGFloat duration = animation.duration/1000;
    BOOL autoreverse = animation.autoreverse;
    BOOL restartFromBeginning = animation.restartFromBeginning;
    int repeat = animation.repeat - 1;
    
    if (restartFromBeginning) {
        [self cancelAllAnimations:nil];
    }
    
    NSMutableDictionary* animProps = [NSMutableDictionary dictionaryWithDictionary:[animation allProperties]];
    [[self allProperties] enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        if (![animProps valueForKey:key]) {
            [animProps setObject:[self valueForKey:key] forKey:key];
        }
    }];
    
    [self prepareAnimation:animation holder:animations animProps:animProps];
    
    if ([animations count] > 0) {
        CAAnimationGroup *group = [CAAnimationGroup animation];
        group.animations = animations;
        group.delegate = animation;
        group.duration = duration;
        group.autoreverses = autoreverse;
        group.repeatCount = repeat;
        group.fillMode = kCAFillModeBoth;
        [[_layer presentationLayer] addAnimation:group forKey:nil];
    }
}

@end
