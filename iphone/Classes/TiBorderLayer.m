//
//  CABorderLayer.m
//  Titanium
//
//  Created by Martin Guillon on 24/12/13.
//
//

#import "TiBorderLayer.h"
#import "TiBase.h"
#import "TiUtils.h"
#import "TiAnimation.h"
#import "TiViewAnimationStep.h"

CGFloat* innerRadiusFromPadding(const CGFloat* radii, const CGRect  rect, float _decale)
{
    CGFloat maxPadding = MIN(rect.size.width / 4, rect.size.height / 4);
    CGFloat padding = MIN(_decale, maxPadding);
    CGFloat* result = malloc(8 * sizeof(CGFloat *));
    for (int i = 0; i < 8; i++) {
        result[i] = MAX(radii[i] - padding, 0);
    }
    return result;
}

CGFloat* decaleRadius(const CGFloat* radii, float _decale)
{
    CGFloat* result = malloc(8 * sizeof(CGFloat *));
    for (int i = 0; i < 8; i++) {
        result[i] = radii[i] + _decale;
    }
    return result;
}

CGPathRef CGPathCreateRoundiiRect( const CGRect rect, const CGFloat* radii, CGFloat _decale)
{
    if (radii == NULL) {
        return CGPathCreateWithRect(CGRectInset(rect, _decale, _decale), NULL);
    }
    radii = innerRadiusFromPadding(radii, rect, _decale);
    // create a mutable path
    CGMutablePathRef path = CGPathCreateMutable();
   
    // get the 4 corners of the rect
    CGPoint topLeft = CGPointMake(rect.origin.x + _decale, rect.origin.y + _decale);
    CGPoint topRight = CGPointMake(rect.origin.x + rect.size.width - _decale, rect.origin.y + _decale);
    CGPoint bottomRight = CGPointMake(rect.origin.x + rect.size.width - _decale, rect.origin.y + rect.size.height - _decale);
    CGPoint bottomLeft = CGPointMake(rect.origin.x + _decale, rect.origin.y + rect.size.height -_decale);
    
    // move to top left
    CGPathMoveToPoint(path, NULL, topLeft.x + radii[0], topLeft.y);
    
    
    
    if (radii[2] == radii[3]) {
        CGFloat radius = radii[2];
        CGPathAddRelativeArc(path, NULL, topRight.x - radius, topRight.y + radius, radius, -M_PI_2, M_PI_2);
    }
    else {
        // add top line
        CGPathAddLineToPoint(path, NULL, topRight.x - radii[2], topRight.y);
        // add top right curve
        CGPathAddQuadCurveToPoint(path, NULL, topRight.x, topRight.y, topRight.x, topRight.y + radii[3]);
    }
    
    
    if (radii[4] == radii[5]) {
        CGFloat radius = radii[4];
        CGPathAddRelativeArc(path, NULL, bottomRight.x - radius, bottomRight.y - radius, radius, 0, M_PI_2);
    }
    else {
        // add right line
        CGPathAddLineToPoint(path, NULL, bottomRight.x, bottomRight.y - radii[4]);
        
        // add bottom right curve
        CGPathAddQuadCurveToPoint(path, NULL, bottomRight.x, bottomRight.y, bottomRight.x - radii[5], bottomRight.y);
    }
    
    if (radii[6] == radii[7]) {
        CGFloat radius = radii[6];
        CGPathAddRelativeArc(path, NULL, bottomLeft.x + radius, bottomLeft.y - radius, radius, M_PI_2, M_PI_2);
    }
    else {
        // add bottom line
        CGPathAddLineToPoint(path, NULL, bottomLeft.x + radii[6], bottomLeft.y);
        
        // add bottom left curve
        CGPathAddQuadCurveToPoint(path, NULL, bottomLeft.x, bottomLeft.y, bottomLeft.x, bottomLeft.y - radii[7]);
    }
    if (radii[0] == radii[1]) {
        CGFloat radius = radii[0];
        CGPathAddRelativeArc(path, NULL, topLeft.x + radius, topLeft.y + radius, radius, M_PI, M_PI_2);
    }
    else {
        // add left line
        CGPathAddLineToPoint(path, NULL, topLeft.x, topLeft.y + radii[0]);
        
        // add top left curve
        CGPathAddQuadCurveToPoint(path, NULL, topLeft.x, topLeft.y, topLeft.x + radii[1], topLeft.y);
    }
    
    // return the path
    return path;
}

@implementation TiBorderLayer
{
    CGPathRef _clippingPath;
    CGFloat* radii;
    CGFloat _theWidth;
    UIEdgeInsets _thePadding;
    CGRect _lastRect;
}
@synthesize clippingPath = _clippingPath;
@synthesize thePadding = _thePadding;

-(id)init
{
    if (self = [super init])
    {
        self.needsDisplayOnBoundsChange = YES;
        self.masksToBounds = NO;
        self.shouldRasterize = YES;
        self.contentsScale = self.rasterizationScale = [UIScreen mainScreen].scale;
        _clippingPath = nil;
        radii = NULL;
        _lastRect = CGRectZero;
    }
    return self;
}

-(void)dealloc
{
    if (_clippingPath != nil)
    {
        CGPathRelease(_clippingPath);
        _clippingPath = nil;
    }
    if (radii != NULL) {
        free(radii);
        radii = NULL;
    }
    [super dealloc];
}

-(void)setRadius:(id)value
{
    if ([value isKindOfClass:[NSArray class]]) {
        radii =(CGFloat*)malloc(8*sizeof(CGFloat));
        NSArray* array = (NSArray*)value;
        int count = [array count];
        if (count == 4)
        {
            for (int i = 0; i < count; ++i){
                radii[2*i] = radii[2*i+1] = [TiUtils floatValue:[array objectAtIndex:i] def:0.0f];
            }
        } else  if (count == 8)
        {
            for (int i = 0; i < count; ++i){
                radii[i] = [TiUtils floatValue:[array objectAtIndex:i] def:0.0f];
            }
        }
    }
    else
    {
        radii = (CGFloat*)malloc(8*sizeof(CGFloat));
        CGFloat radius = [TiUtils floatValue:value def:0.0f];
        for (int i = 0; i < 8; ++i){
            radii[i] = radius;
        }
    }
}

-(void)setFrame:(CGRect)frame
{
    [super setFrame:frame];
    [self updateBorder];
}

-(void)setFrame:(CGRect)frame withinAnimation:(TiViewAnimationStep*)runningAnimation
{
    [super setFrame:frame];
    if (runningAnimation) {
        CGPathRef newBorderPath = [self borderPathForBounds:[self bounds]];
        CABasicAnimation *animation = [CABasicAnimation animationWithKeyPath:@"path"];
        animation.fromValue = (id)[self getOrCreateLayerMask].path;
        animation.toValue = (id)newBorderPath;
        animation.duration = [runningAnimation duration];
        animation.timingFunction = [runningAnimation curve];
        animation.fillMode = kCAFillModeBoth;
        [self getOrCreateLayerMask].path = newBorderPath;
        [[self getOrCreateLayerMask] addAnimation:animation forKey:nil];
        CGPathRelease(newBorderPath);
      
        CGPathRef newClippingPath = [self pathForClippingForBounds:[self bounds]];
        self.clippingPath = newClippingPath;
        CGPathRelease(newClippingPath);
    }
    else
        [self updateBorderRect:[self bounds]];//dont update if same rect
}

-(void)updateBorder
{
    //updateBorder will force the update.
    _lastRect = CGRectZero;
    [self updateBorderRect:[self bounds]];
}

-(CGPathRef)pathForClippingForBounds:(CGRect)bounds
{
    //the 0.5f is there to have a clean border where you don't see the background
    return CGPathCreateRoundiiRect(bounds, radii, 0.0f);
}
-(CGPathRef)borderPathForBounds:(CGRect)bounds
{
    return CGPathCreateRoundiiRect(UIEdgeInsetsInsetRect(bounds, _thePadding), radii, _theWidth/2);
}

-(CAShapeLayer*)getOrCreateLayerMask
{
    [self setOrCreateMaskOnLayer:self];
    return (CAShapeLayer*)self.mask;
}

-(void)setOrCreateMaskOnLayer:(CALayer*)layer
{
    if (layer.mask == nil)
    {
        layer.mask = [CAShapeLayer layer];
        ((CAShapeLayer*)layer.mask).fillColor = [[UIColor clearColor] CGColor];
        ((CAShapeLayer*)layer.mask).strokeColor = [[UIColor blackColor] CGColor];
    }
}

-(CGPathRef)updateBorderRect:(CGRect)bounds
{
    if (CGRectIsEmpty(bounds) || CGRectEqualToRect(bounds, _lastRect)) return;
    _lastRect = bounds;
    CGPathRef path = [self getOrCreateLayerMask].path = [self borderPathForBounds:bounds];
    CGPathRelease(path);
     path = self.clippingPath = self.shadowPath = [self pathForClippingForBounds:bounds];
    CGPathRelease(path);
    return _clippingPath;
}

-(void)setTheWidth:(CGFloat)width
{
    if (width == _theWidth) return;
    //the 0.5f compensate the 0.5f applied to the clippingPath
    _theWidth = [self getOrCreateLayerMask].lineWidth = width;
    [self updateBorder];
}

-(void)setThePadding:(UIEdgeInsets)inset
{
    _thePadding = inset;
    [self updateBorder];
}


-(void)setClippingPath:(CGPathRef)clippingPath
{
    if (_clippingPath != nil)
    {
        CGPathRelease(_clippingPath);
        _clippingPath = nil;
    }
    if (clippingPath)
        _clippingPath = CGPathRetain(clippingPath);
}

@end
