#import "TiSelectableBackgroundLayer.h"

@implementation TiDrawable
@synthesize gradient, color, image;


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
        CGContextDrawImage(ctx, rect, image.CGImage);
    }
    CGContextRestoreGState(ctx);
}

@end

@interface TiSelectableBackgroundLayer()
{
    TiDrawable* currentLayer;
}
@end

@implementation TiSelectableBackgroundLayer
@synthesize stateLayers, stateLayersMap;

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
        self.masksToBounds=YES;
   }
    return self;
}

- (void) dealloc
{
    currentLayer = nil;
	[stateLayersMap release];
	[stateLayers release];
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

- (void)setState:(UIControlState)state
{
    currentLayer = (TiDrawable*)[stateLayersMap objectForKey:[[NSNumber numberWithInt:state] stringValue]];
    [self setNeedsDisplay];
}

-(TiDrawable*) getOrCreateDrawableForState:(UIControlState)state
{
    NSString* key = [[NSNumber numberWithInt:state] stringValue];
    TiDrawable* drawable = (TiDrawable*)[stateLayersMap objectForKey:key];
    if (drawable == nil) {
        drawable = [[TiDrawable alloc] init];
        [stateLayersMap setObject:drawable forKey:key];
        [drawable release];
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
}


- (void)setImage:(UIImage*)image forState:(UIControlState)state
{
    [self getOrCreateDrawableForState:state].image = image;
}

- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    [self getOrCreateDrawableForState:state].gradient = gradient;
}


@end