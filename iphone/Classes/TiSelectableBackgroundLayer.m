#import "TiSelectableBackgroundLayer.h"
#import "TiGradient.h"
#import "TiSVGImage.h"

@interface TiDrawable()
{
    UIImage* _bufferImage;
    NSArray* _innerShadows;
    BOOL _needsUpdate;
}
-(void)updateInLayer:(TiSelectableBackgroundLayer*)layer onlyCreateImage:(BOOL)onlyCreate;

@end
@implementation TiDrawable
@synthesize gradient, color, image, svg, imageRepeat, shadow = _shadow, innerShadows = _innerShadows;

- (id)init {
    if (self = [super init])
    {
        imageRepeat = NO;
        _innerShadows = nil;
        _needsUpdate = YES;
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
    RELEASE_TO_NIL(_shadow)
    RELEASE_TO_NIL(_innerShadows)
	[super dealloc];
}

-(void)setNeedsUpdate
{
    _needsUpdate = YES;
}

-(void)setInLayer:(TiSelectableBackgroundLayer*)layer onlyCreateImage:(BOOL)onlyCreate animated:(BOOL)animated
{
    
    if ((_needsUpdate || _bufferImage == nil) && (gradient != nil ||
                                color != nil ||
                                image != nil ||
                                _innerShadows != nil ||
                                svg != nil)) {
        _needsUpdate = NO;
        if (gradient == nil && color == nil && _innerShadows == nil && image != nil) {
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
-(void)drawBufferFromLayer:(TiSelectableBackgroundLayer*)layer
{
    CGRect rect = layer.bounds;
    UIGraphicsBeginImageContextWithOptions(rect.size, NO, 0.0);
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    if (ctx == 0) {
        UIGraphicsEndImageContext();
        return;
    }
    [self drawInContext:ctx inRect:rect forLayer:layer];

    _bufferImage = [UIGraphicsGetImageFromCurrentImageContext() retain];
    UIGraphicsEndImageContext();
}

-(void)drawInContext:(CGContextRef)ctx inRect:(CGRect)rect forLayer:(TiSelectableBackgroundLayer*)layer
{
    CGContextSetAllowsAntialiasing(ctx, true);
    CGContextSetShouldAntialias(ctx, true);
    if (layer.shadowPath) {
        CGContextAddPath(ctx, layer.shadowPath);
        if (layer.clipWidth > 0) {
            CGContextSetLineWidth(ctx, layer.clipWidth);
            CGContextReplacePathWithStrokedPath(ctx);
        }
        CGContextClip(ctx);
    }
    else if (layer.clipWidth > 0)
    {
        CGFloat halfWidth = layer.clipWidth / 2.0f;
        CGContextAddRect(ctx, CGRectInset(rect, halfWidth, halfWidth));
        CGContextSetLineWidth(ctx, layer.clipWidth);
        CGContextReplacePathWithStrokedPath(ctx);
        CGContextClip(ctx);
    }

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
    if(_innerShadows) {
        UIBezierPath* path = [UIBezierPath bezierPathWithRect:CGRectInfinite];
        if (layer.shadowPath) {
            [path appendPath:[UIBezierPath bezierPathWithCGPath:layer.shadowPath]];
        }
        else {
            [path appendPath:[UIBezierPath bezierPathWithRect:CGRectInset(rect, -1, -1)]];
        }
        path.usesEvenOddFillRule = YES;
        for (NSShadow* shadow in _innerShadows) {
            CGSize offset = shadow.shadowOffset;
            CGFloat blur = shadow.shadowBlurRadius;
            CGColorRef shadowColor = ((UIColor*)shadow.shadowColor).CGColor;
            CGContextSetShadowWithColor(ctx, offset, blur, shadowColor);
            CGContextSetFillColorWithColor(ctx, shadowColor);
            [path fill]; // to get the shadow
        }
    }
}

-(void)updateInLayer:(TiSelectableBackgroundLayer*)layer  onlyCreateImage:(BOOL)onlyCreate
{
    [self updateInLayer:layer onlyCreateImage:onlyCreate forceChange:YES];
}

-(void)updateInLayer:(TiSelectableBackgroundLayer*)layer  onlyCreateImage:(BOOL)onlyCreate forceChange:(BOOL)force
{
    if (!force && !layer.shadowPath && _bufferImage && (color || image) && gradient == nil && _innerShadows == nil) return;
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
    CGFloat _clipWidth;
    CGPathRef _clippingPath;
    BOOL _needsToSetAllDrawablesOnNextSize;
}
@end

@implementation TiSelectableBackgroundLayer
@synthesize stateLayers, stateLayersMap, imageRepeat = _imageRepeat, readyToCreateDrawables, animateTransition = _animateTransition, clipWidth = _clipWidth, clippingPath = _clippingPath;

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
        _needsToSetAllDrawablesOnNextSize = NO;
        _animateTransition = NO;
        self.contentsScale = self.rasterizationScale = [UIScreen mainScreen].scale;
        _clipWidth = 0.0f;
    }
    return self;
}

- (void) dealloc
{
    currentDrawable = nil;
	[stateLayersMap release];
	[stateLayers release];
    if (_clippingPath)
    {
        CGPathRelease(_clippingPath);
        _clippingPath = nil;
    }
	[super dealloc];
}

-(void)setBounds:(CGRect)bounds
{
    
    bounds = CGRectIntegral(bounds);
    CGRect currentRect = [self bounds];
	[super setBounds:bounds];
    if (CGRectIsEmpty(bounds)) return;
    
    BOOL needsToUpdate = ((_needsToSetAllDrawablesOnNextSize || readyToCreateDrawables) && (!CGSizeEqualToSize(bounds.size, currentRect.size) || _needsToSetDrawables));
    
    if (needsToUpdate) {
        if (readyToCreateDrawables)
        {
            [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
                if (drawable != nil && drawable == currentDrawable) {
                    [drawable updateInLayer:self onlyCreateImage:NO forceChange:_needsToSetDrawables];
                }
                else {
                    [drawable setNeedsUpdate];
                }
            }];
        }
        else {
            self.readyToCreateDrawables = YES;
        }
        
        _needsToSetDrawables = NO;
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
        if (readyToCreateDrawables) [currentDrawable setInLayer:self onlyCreateImage:NO animated:animated];
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
        [self updateDrawable:drawable];
    }
    else {
        _needsToSetDrawables = YES;
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
        [self updateDrawable:drawable];
    }
    else {
        _needsToSetDrawables = YES;
    }
}

- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    drawable.gradient = gradient;
    if (readyToCreateDrawables) {
        [self updateDrawable:drawable];
    }
    else {
        _needsToSetDrawables = YES;
    }
}


- (void)setShadow:(NSShadow*)shadow forState:(UIControlState)state
{
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    drawable.shadow = shadow;
    if (readyToCreateDrawables) {
        [self updateDrawable:drawable];
    }
    else {
        _needsToSetDrawables = YES;
    }
}

- (void)setInnerShadows:(NSArray*)shadows forState:(UIControlState)state
{
    TiDrawable* drawable = [self getOrCreateDrawableForState:state];
    drawable.innerShadows = shadows;
    if (readyToCreateDrawables) {
        [self updateDrawable:drawable];
    }
    else {
        _needsToSetDrawables = YES;
    }
}

-(void)update
{
    if (currentDrawable != nil) {
        [currentDrawable updateInLayer:self onlyCreateImage:NO forceChange:_needsToSetDrawables];
    }
}

-(void)updateDrawable:(TiDrawable*)drawable
{
    if (drawable == currentDrawable) {
        [currentDrawable updateInLayer:self onlyCreateImage:NO forceChange:_needsToSetDrawables];
    }
    else {
        [drawable setNeedsUpdate];
    }
}

-(void)setClipWidth:(CGFloat)width
{
    if (width == _clipWidth) return;
    //the 0.5f compensate the 0.5f applied to the clippingPath
    _clipWidth = width;
    if (readyToCreateDrawables) {
        [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
            if (drawable != nil && drawable == currentDrawable) {
                [drawable updateInLayer:self onlyCreateImage:NO forceChange:_needsToSetDrawables];
            }
            else {
                [drawable setNeedsUpdate];
            }
        }];
    }
    else {
        _needsToSetDrawables = YES;
    }
}



- (void)setReadyToCreateDrawables:(BOOL)value
{
    if (value && CGRectIsEmpty(self.bounds)) {
        _needsToSetAllDrawablesOnNextSize = YES;
        return;
    }
    if (value != readyToCreateDrawables) {
        readyToCreateDrawables = value;
        if (readyToCreateDrawables) {
            if (_needsToSetDrawables) {
                [stateLayersMap enumerateKeysAndObjectsUsingBlock: ^(id key, TiDrawable* drawable, BOOL *stop) {
                    if (drawable != nil) {
                        [drawable updateInLayer:self onlyCreateImage:(drawable != currentDrawable) forceChange:_needsToSetDrawables];
                    }
                }];
                _needsToSetDrawables = NO;
            }
        }
    }
}

-(void)setFrame:(CGRect)frame
{
    [super setFrame:frame];
    if (self.mask) {
        self.mask.frame = frame;
    }
}

- (id<CAAction>)actionForKey:(NSString *)event
{
    id action  = [super actionForKey:event];
    if ([event isEqualToString:@"contents"])
    {
        if (_animateTransition) {
            CATransition *transition = [CATransition animation];
            if (transition.duration == 0)
            {
                transition.duration = 0.2;
                transition.type = kCATransitionReveal;
                transition.subtype = kCATransitionFade;
            }
            [self addAnimation:transition forKey:nil];
        }
        else return  nil;
    }

    return action;
}

-(void)setClippingPath:(CGPathRef)newPath
{
    if ( newPath != _clippingPath ) {
        CGPathRelease(_clippingPath);
        _clippingPath = CGPathRetain(newPath);
    }
}

@end
