#import "TiSelectableBackgroundLayer.h"
#import "TiGradient.h"
#import "TiSVGImage.h"

@interface TiDrawable()
{
    UIImage* _bufferImage;
}
-(void)updateInLayer:(TiSelectableBackgroundLayer*)layer onlyCreateImage:(BOOL)onlyCreate;

@end
@implementation TiDrawable
@synthesize gradient, color, image, svg, imageRepeat;

- (id)init {
    if (self = [super init])
    {
        imageRepeat = NO;
    }
    return self;
}

- (void) dealloc
{
    RELEASE_TO_NIL(_bufferImage)
    RELEASE_TO_NIL(gradient)
    RELEASE_TO_NIL(color)
    RELEASE_TO_NIL(image)
    RELEASE_TO_NIL(svg)
    RELEASE_TO_NIL(shadow)
	[super dealloc];
}

-(void)setInLayer:(TiSelectableBackgroundLayer*)layer onlyCreateImage:(BOOL)onlyCreate animated:(BOOL)animated
{
    
    if (_bufferImage == nil && (gradient != nil ||
                                color != nil ||
                                image != nil ||
                                svg != nil)) {
        if (gradient == nil && color == nil && image != nil) {
            _bufferImage = [image retain];
        }
        else {
            if (CGRectEqualToRect(layer.frame, CGRectZero))
                return;
            [self drawBufferFromLayer:layer];
            
        }
    }
    if (onlyCreate) return;

    if (_bufferImage == nil) {
        if (layer.contents != nil) {
            [layer setContents:nil];
        }
    } else {
        if (image != nil) {
            layer.contentsScale = image.scale;
            layer.contentsCenter = TiDimensionLayerContentCenterFromInsents(image.capInsets, [image size]);
        }
        else {
            layer.contentsScale = [[UIScreen mainScreen] scale];
            layer.contentsCenter = CGRectMake(0, 0, 1, 1);
        }
        if (!CGPointEqualToPoint(layer.contentsCenter.origin,CGPointZero)) {
            layer.magnificationFilter = @"nearest";
        } else {
            layer.magnificationFilter = @"linear";
        }

        [layer setContents:(id)_bufferImage.CGImage];
    }
}
-(void)drawBufferFromLayer:(CALayer*)layer
{
    CGRect rect = layer.bounds;
    UIGraphicsBeginImageContextWithOptions(rect.size, NO, 0.0);
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    if (ctx == 0) {
        UIGraphicsEndImageContext();
        return;
    }
    [self drawInContext:UIGraphicsGetCurrentContext() inRect:rect];
    _bufferImage = [UIGraphicsGetImageFromCurrentImageContext() retain];
    UIGraphicsEndImageContext();
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
            CGContextTranslateCTM(ctx, 0, rect.size.height);
            CGContextScaleCTM(ctx, 1.0, -1.0);
            CGRect imageRect = CGRectMake(0, 0, image.size.width, image.size.height);
            CGContextDrawTiledImage(ctx, imageRect, image.CGImage);
        }
        else {
            UIGraphicsPushContext(ctx);
            [image drawInRect:rect];
            UIGraphicsPopContext();
        }
    }
    if (svg) {
        CGSize scale = CGSizeMake( rect.size.width /  svg.size.width, rect.size.height / svg.size.height);
        CGContextScaleCTM( ctx, scale.width, scale.height );
        [svg.CALayerTree renderInContext:ctx];
    }
    CGContextRestoreGState(ctx);
}

-(void)updateInLayer:(TiSelectableBackgroundLayer*)layer  onlyCreateImage:(BOOL)onlyCreate
{
    RELEASE_TO_NIL(_bufferImage);
    [self setInLayer:layer  onlyCreateImage:onlyCreate animated:NO];
}

@end

@interface TiSelectableBackgroundLayer()
{
    TiDrawable* currentDrawable;
    UIControlState currentState;
    BOOL _animateTransition;
    BOOL _needsToSetDrawables;
}
@end

@implementation TiSelectableBackgroundLayer
@synthesize stateLayers, stateLayersMap, imageRepeat = _imageRepeat, readyToCreateDrawables, animateTransition = _animateTransition;

//- (id) initWithLayer:(id)layer {
//    if(self = [super initWithLayer:layer]) {
//        if([layer isKindOfClass:[TiSelectableBackgroundLayer class]]) {
//            TiSelectableBackgroundLayer *other = (TiSelectableBackgroundLayer*)layer;
//            self.imageRepeat = other.imageRepeat;
//            stateLayersMap = [[NSMutableDictionary dictionaryWithDictionary:other.stateLayersMap] retain];
//            stateLayers = [[NSMutableArray arrayWithArray:other.stateLayers] retain];
//            currentState = [other getState];
//            readyToCreateDrawables = YES;
//            currentDrawable = [self getOrCreateDrawableForState:currentState];
//        }
//    }
//    return self;
//}

- (id)init {
    if (self = [super init])
    {
        stateLayersMap = [[NSMutableDictionary dictionaryWithCapacity:4] retain];
        currentDrawable = [self getOrCreateDrawableForState:UIControlStateNormal];
        stateLayers = [[NSMutableArray array] retain];
        currentState = UIControlStateNormal;
        _imageRepeat = NO;
        readyToCreateDrawables = NO;
        _needsToSetDrawables = NO;
        _animateTransition = NO;
        self.masksToBounds = NO;
//        self.needsDisplayOnBoundsChange = YES;
        self.shouldRasterize = YES;
        self.contentsScale = self.rasterizationScale = [UIScreen mainScreen].scale;
//        self.actions = [NSDictionary dictionaryWithObjectsAndKeys:
//                        [NSNull null], @"bounds",
//                        nil];
    }
    return self;
}

- (void) dealloc
{
    currentDrawable = nil;
	[stateLayersMap release];
	[stateLayers release];
	[super dealloc];
}

//-(void)setFrame:(CGRect)frame
//{
//    BOOL needsToUpdate = (frame.size.width != 0 && frame.size.height!= 0 && (!CGSizeEqualToSize(frame.size, self.frame.size) || _needsToSetDrawables));
//    
//	[super setFrame:frame];
//    if (needsToUpdate) {
//        CGSize size = self.frame.size;
//        _needsToSetDrawables = NO;
//        [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
//            if (drawable != nil) {
//                [drawable updateInLayer:self onlyCreateImage:(drawable != currentDrawable)];
//            }
//        }];
//    }
//}

-(void)setBounds:(CGRect)bounds
{
    bounds = CGRectIntegral(bounds);
    BOOL needsToUpdate = (bounds.size.width != 0 && bounds.size.height!= 0 && (!CGSizeEqualToSize(bounds.size, self.bounds.size) || _needsToSetDrawables));
    
	[super setBounds:bounds];
    if (needsToUpdate) {
        _needsToSetDrawables = NO;
        [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
            if (drawable != nil) {
                [drawable updateInLayer:self onlyCreateImage:(drawable != currentDrawable)];
            }
        }];
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

- (void)setState:(UIControlState)state animated:(BOOL)animated
{
    if (state == currentState) return;
    
    TiDrawable* newDrawable = (TiDrawable*)[stateLayersMap objectForKey:[[NSNumber numberWithInt:state] stringValue]];
    if (newDrawable == nil && state != UIControlStateNormal) {
        newDrawable = (TiDrawable*)[stateLayersMap objectForKey:[[NSNumber numberWithInt:UIControlStateNormal] stringValue]];
        state = UIControlStateNormal;
    }
    if (newDrawable != nil && newDrawable != currentDrawable) {
        currentDrawable = newDrawable;
        [currentDrawable setInLayer:self onlyCreateImage:NO animated:animated];
        if (currentDrawable.shadow) {
            self.shadowOpacity = 1.0f;
            self.shadowColor = ((UIColor*)currentDrawable.shadow.shadowColor).CGColor;
            self.shadowOffset = currentDrawable.shadow.shadowOffset;
        }
        else {
            self.shadowOpacity = 0.0f;
        }
    }
    else {
        self.shadowOpacity = 0.0f;
    }
//    self.shadowOpacity = 1.0f;
//    self.shadowColor = [UIColor blackColor].CGColor;
    currentState = state;
}

- (void)setState:(UIControlState)state
{
    [self setState:state animated:_animateTransition];
}

- (UIControlState)getState
{
    return currentState;
    
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
        if (currentDrawable == nil && state == currentState) {
            currentDrawable = drawable;
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
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    drawable.color = color;
    if (readyToCreateDrawables) {
        [drawable updateInLayer:self onlyCreateImage:(state != currentState)];
    }
}


- (void)setImage:(id)image forState:(UIControlState)state
{
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    if ([image isKindOfClass:[UIImage class]])
        drawable.image = image;
    else if ([image isKindOfClass:[TiSVGImage class]])
        drawable.svg = image;
    else return;
    if (readyToCreateDrawables) {
        [drawable updateInLayer:self onlyCreateImage:(state != currentState)];
    }
}

- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    drawable.gradient = gradient;
    if (readyToCreateDrawables) {
        [drawable updateInLayer:self onlyCreateImage:(state != currentState)];
    }
}


- (void)setShadow:(NSShadow*)shadow forState:(UIControlState)state
{
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    drawable.shadow = shadow;
    if (readyToCreateDrawables) {
        [drawable updateInLayer:self onlyCreateImage:(state != currentState)];
    }
}

- (void)setReadyToCreateDrawables:(BOOL)value
{
    if (value != readyToCreateDrawables) {
        readyToCreateDrawables = value;
        if (readyToCreateDrawables) {
            if (self.frame.size.width != 0 && self.frame.size.height!= 0) {
                [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
                    if (drawable != nil) {
                        [drawable updateInLayer:self onlyCreateImage:(drawable != currentDrawable)];
                    }
                }];
            }
            else {
                _needsToSetDrawables = YES;
            }
            
        }
    }
}

//
//static NSArray *animationKeys;
//+ (NSArray *)animationKeys
//{
//    if (!animationKeys)
//        animationKeys = [[NSArray arrayWithObjects:@"bounds",@"contents",nil] retain];
//    
//    return animationKeys;
//}
//
//+(BOOL)needsDisplayForKey:(NSString*)key
//{
//    if ([key isEqualToString:@"contents"] || [key isEqualToString:@"bounds"])
//        return YES;
//    return [super needsDisplayForKey:key];
//}

//
//- (void)drawInContext:(CGContextRef)ctx
//{
//    [currentDrawable drawInContext:ctx inRect:self.bounds];
//}

//
- (id<CAAction>)actionForKey:(NSString *)event
{
    id action  = [super actionForKey:event];
    if ([event isEqualToString:@"contents"])
    {
        CATransition *transition = [CATransition animation];
        if (_animateTransition && transition.duration == 0)
        {
            transition.duration = 0.2;
            transition.type = kCATransitionReveal;
            transition.subtype = kCATransitionFade;
        }
        [self addAnimation:transition forKey:nil];
    }

    return action;
}


@end