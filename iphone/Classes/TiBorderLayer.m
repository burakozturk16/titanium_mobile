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


CGPathRef CGPathCreateRoundiiRect( const CGRect rect, const CGFloat* radii, CGFloat width )
{
    // create a mutable path
    CGMutablePathRef path = CGPathCreateMutable();
   
    // get the 4 corners of the rect
    CGPoint topLeft = CGPointMake(rect.origin.x, rect.origin.y);
    CGPoint topRight = CGPointMake(rect.origin.x + rect.size.width, rect.origin.y);
    CGPoint bottomRight = CGPointMake(rect.origin.x + rect.size.width, rect.origin.y + rect.size.height);
    CGPoint bottomLeft = CGPointMake(rect.origin.x, rect.origin.y + rect.size.height);
    
    // move to top left
    CGPathMoveToPoint(path, NULL, topLeft.x + radii[0], topLeft.y);
    
    // add top line
    CGPathAddLineToPoint(path, NULL, topRight.x - radii[2], topRight.y);
    
    // add top right curve
    CGPathAddQuadCurveToPoint(path, NULL, topRight.x, topRight.y, topRight.x, topRight.y + radii[3]);
    
    // add right line
    CGPathAddLineToPoint(path, NULL, bottomRight.x, bottomRight.y - radii[4]);
    
    // add bottom right curve
    CGPathAddQuadCurveToPoint(path, NULL, bottomRight.x, bottomRight.y, bottomRight.x - radii[5], bottomRight.y);
    
    // add bottom line
    CGPathAddLineToPoint(path, NULL, bottomLeft.x + radii[6], bottomLeft.y);
    
    // add bottom left curve
    CGPathAddQuadCurveToPoint(path, NULL, bottomLeft.x, bottomLeft.y, bottomLeft.x, bottomLeft.y - radii[7]);
    
    // add left line
    CGPathAddLineToPoint(path, NULL, topLeft.x, topLeft.y + radii[0]);
    
    // add top left curve
    CGPathAddQuadCurveToPoint(path, NULL, topLeft.x, topLeft.y, topLeft.x + radii[1], topLeft.y);
    
    // return the path
    return path;
}

@implementation TiBorderLayer
{
    CGPathRef _path;
    CGPathRef _borderPath;
    CGFloat* radii;
    CGFloat _borderWidth;
    UIColor* _borderColor;
    UIEdgeInsets _borderPadding;
}
@synthesize borderWidth = _borderWidth;
@synthesize borderColor = _borderColor;
@synthesize path = _path;
@synthesize borderPadding = _borderPadding;
-(id)init
{
    if (self = [super init])
    {
        self.needsDisplayOnBoundsChange = YES;
        self.shouldRasterize = YES;
        self.contentsScale = self.rasterizationScale = [UIScreen mainScreen].scale;
        _path = nil;
        _borderPath = nil;
        radii = NULL;
    }
    return self;
}

-(void)dealloc
{
    if (_path != nil)
    {
        CGPathRelease(_path);
        _path = nil;
    }
    if (_borderPath != nil)
    {
        CGPathRelease(_borderPath);
        _borderPath = nil;
    }
    if (radii != NULL) {
        free(radii);
        radii = NULL;
    }
    [super dealloc];
}

-(void)drawInContext:(CGContextRef)ctx
{
    if (_borderPath == nil) return;
    CGContextSaveGState(ctx);
    CGContextSetAllowsAntialiasing(ctx, true);
    CGContextSetShouldAntialias(ctx, true);
    CGContextSetStrokeColorWithColor(ctx, _borderColor.CGColor);
    CGContextSetLineWidth(ctx, 2*_borderWidth);
    CGContextAddPath(ctx, _borderPath);
    CGContextClip(ctx);
    CGContextAddPath(ctx, _borderPath);
    
    CGContextDrawPath(ctx, kCGPathStroke);
    CGContextRestoreGState(ctx);
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
-(void)updateBorder
{
    [self updateBorderRect:[self bounds]];
}

-(void)updateBorderRect:(CGRect)bounds
{
    if (CGRectIsEmpty(bounds)) return;
    if (_borderPath != nil) {
        CGPathRelease(_borderPath);
        _borderPath = nil;
    }
    if (_path != nil) {
        CGPathRelease(_path);
        _path = nil;
    }
    if(radii != NULL) {
        _borderPath = CGPathCreateRoundiiRect(UIEdgeInsetsInsetRect(bounds, _borderPadding), radii, _borderWidth);
        _path = CGPathCreateRoundiiRect(bounds, radii, _borderWidth);
    }
    else {
        _borderPath = CGPathCreateWithRect(UIEdgeInsetsInsetRect(bounds, _borderPadding), NULL);
        _path = CGPathCreateWithRect(bounds, NULL);
    }
    [self setNeedsDisplay];
}

-(void)setBorderWidth:(CGFloat)width
{
    _borderWidth = width;
    [self updateBorder];
}

-(void)setBorderColor:(UIColor *)color
{
    _borderColor = [color retain];
    [self updateBorder];
}

-(void)setBorderPadding:(UIEdgeInsets)inset
{
    _borderPadding = inset;
    [self updateBorder];
}
@end
