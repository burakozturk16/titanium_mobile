#import "TiSelectableBackgroundLayer.h"

@implementation TiDrawable
@synthesize gradient, color, image, imageRepeat;

- (id)init {
    if (self = [super init])
    {
        imageRepeat = NO;
    }
    return self;
}

- (void) dealloc
{
	[gradient release];
	[color release];
	[image release];
	[super dealloc];
}

-(void)drawInContext:(CGContextRef)ctx inRect:(CGRect)rect
{
    CGContextSaveGState(ctx);
    
    CGContextSetAllowsAntialiasing(ctx, true);
    if (color) {
        CGContextSetFillColorWithColor(ctx, [color CGColor]);
        CGContextFillRect(ctx, rect);
    }
    if (gradient){
        [gradient paintContext:ctx bounds:rect];
    }
    
    if (image){
        if (imageRepeat) {
            CGRect imageRect = CGRectMake(0, 0, image.size.width, image.size.height);
            CGContextDrawTiledImage(ctx, imageRect, image.CGImage);
        }
        else {
            UIGraphicsPushContext(ctx);
            [image drawInRect:rect];
            UIGraphicsPopContext();
        }
    }
    CGContextRestoreGState(ctx);
}

@end

@interface TiSelectableBackgroundLayer()
{
    TiDrawable* currentLayer;
    UIControlState currentState;
}
@end

@implementation TiSelectableBackgroundLayer
@synthesize stateLayers, stateLayersMap, imageRepeat = _imageRepeat, animateTransition;

- (id) initWithLayer:(id)layer {
    if(self = [super initWithLayer:layer]) {
        if([layer isKindOfClass:[TiSelectableBackgroundLayer class]]) {
            TiSelectableBackgroundLayer *other = (TiSelectableBackgroundLayer*)layer;
        }
    }
    return self;
}


- (id)init {
    if (self = [super init])
    {
        stateLayersMap = [[NSMutableDictionary dictionaryWithCapacity:4] retain];
        stateLayers = [[NSMutableArray array] retain];
        currentState = UIControlStateNormal;
        animateTransition = NO;
        _imageRepeat = NO;
        self.masksToBounds=YES;
        self.needsDisplayOnBoundsChange = YES;
        self.contentsScale = [[UIScreen mainScreen] scale];
   }
    return self;
}

- (void) dealloc
{
    currentLayer = nil;
	[stateLayersMap release];
	[stateLayers release];
	[maskLayer release];
	[super dealloc];
}
- (void)drawInContext:(CGContextRef)ctx
{
    [self drawInContext:ctx inRect:[self bounds]];
    if (currentLayer) {
        [currentLayer drawInContext:ctx inRect:[self bounds]];
    }
}

- (void)drawInContext:(CGContextRef)ctx inRect:(CGRect)rect
{
    if (currentLayer) {
        [currentLayer drawInContext:ctx inRect:[self bounds]];
    }
}

-(void)setImageRepeat:(BOOL)imageRepeat
{
    _imageRepeat = imageRepeat;
    
    [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
        if (drawable != nil) {
            drawable.imageRepeat = _imageRepeat;
        }
    }];
}

-(void)setFrame:(CGRect)frame
{
    [super setFrame:frame];
    if (maskLayer != nil) {
        maskLayer.frame = self.bounds;
    }
    [self updateMask];
}

- (void)updateMask
{
    if (maskLayer == nil) return;
    UIBezierPath *maskPath = [UIBezierPath bezierPathWithRoundedRect:maskLayer.bounds
                                                   byRoundingCorners:roundedCorners
                                                         cornerRadii:CGSizeMake(cornersRadius, cornersRadius)];
    maskLayer.path = maskPath.CGPath;
}

- (void)setRoundedRadius:(CGFloat)radius inCorners:(UIRectCorner)corners
{
    if (maskLayer == nil) {
        maskLayer = [[CAShapeLayer alloc] init];
        maskLayer.frame = self.bounds;
        [self setMask:maskLayer];
    }
    cornersRadius = radius;
    roundedCorners = corners;
    [self updateMask];
}

- (void)setState:(UIControlState)state
{
    if (state == currentState) return;
    currentLayer = (TiDrawable*)[stateLayersMap objectForKey:[[NSNumber numberWithInt:state] stringValue]];
    currentState = state;
    if (currentLayer == nil) {
        currentLayer = (TiDrawable*)[stateLayersMap objectForKey:[[NSNumber numberWithInt:UIControlStateNormal] stringValue]];
        currentState = UIControlStateNormal;
    }
    [self setNeedsDisplay];
}

- (UIControlState)getState
{
    return currentState;
    
}

- (id<CAAction>)actionForKey:(NSString *)event
{
    if (!animateTransition && [event isEqualToString:@"contents"])
        return nil;
    return [super actionForKey:event];
}


-(TiDrawable*) getOrCreateDrawableForState:(UIControlState)state
{
    NSString* key = [[NSNumber numberWithInt:state] stringValue];
    TiDrawable* drawable = (TiDrawable*)[stateLayersMap objectForKey:key];
    if (drawable == nil) {
        drawable = [[TiDrawable alloc] init];
        drawable.imageRepeat = _imageRepeat;
        [stateLayersMap setObject:drawable forKey:key];
        [drawable release];
        if (currentLayer == nil && state == currentState) {
            currentLayer = drawable;
        }
    }
    return drawable;
}


-(TiDrawable*) getDrawableForState:(UIControlState)state
{
    NSString* key = [[NSNumber numberWithInt:state] stringValue];
    TiDrawable* drawable = (TiDrawable*)[stateLayersMap objectForKey:key];
    return drawable;
}

- (void)setColor:(UIColor*)color forState:(UIControlState)state
{
    [self getOrCreateDrawableForState:state].color = color;
    if (state == currentState) {
        [self setNeedsDisplay];
    }
}


- (void)setImage:(UIImage*)image forState:(UIControlState)state
{
    [self getOrCreateDrawableForState:state].image = image;
    if (state == currentState) {
        [self setNeedsDisplay];
    }
}

- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    [self getOrCreateDrawableForState:state].gradient = gradient;
    if (state == currentState) {
        [self setNeedsDisplay];
    }
}


@end