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


CGRect invertInsetRect(CGRect rect, UIEdgeInsets inset)
{
    return CGRectMake(rect.origin.x - inset.left, rect.origin.y - inset.top, rect.size.width + inset.left + inset.right, rect.size.width + inset.top + inset.bottom);
}

CGFloat* decaleRadius(const CGFloat* radii, float _decale)
{
    CGFloat* result = malloc(8 * sizeof(CGFloat *));
    for (int i = 0; i < 8; i++) {
        result[i] = radii[i] + _decale;
    }
    return result;
}

CGPathRef CGPathCreateRoundiiRectWithDecale( const CGRect rect, const CGFloat* radii, CGFloat _decale)
{
    if (radii == NULL) {
        CGRect result = CGRectInset(rect, _decale, _decale);
        return CGPathCreateWithRect(result, NULL);
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
    BOOL _usingDefaultBorderStyle;
    CGFloat* _radii;//we do not hold that value!
    UIEdgeInsets _borderPadding;
}

-(id)init
{
    if (self = [super init])
    {
        _usingDefaultBorderStyle = YES;
        _clipWidth = 1;
        self.zPosition = 0.01;
        _borderPadding = UIEdgeInsetsZero;
    }
    return self;
}

-(void)dealloc
{

    [super dealloc];
}



-(CGPathRef)borderPath:(const CGFloat*)radii forBounds:(CGRect)bounds
{
    return CGPathCreateRoundiiRectWithDecale(UIEdgeInsetsInsetRect(bounds, _borderPadding), radii, self.clipWidth/2);
}

-(void)updateBorderPath:(const CGFloat*)radii inBounds:(CGRect)bounds
{
    if (_usingDefaultBorderStyle) {
        self.cornerRadius = radii?radii[0]:0;
        return;
    }
    if (!readyToCreateDrawables && !_needsToSetAllDrawablesOnNextSize) return;
    CGPathRef path = self.shadowPath = [self borderPath:radii forBounds:bounds];
    CGPathRelease(path);
}

-(void)updateBorderPath
{
    [self updateBorderPath:_radii inBounds:self.bounds];
}

-(void)swithToContentBorder
{
    if (_usingDefaultBorderStyle == NO) return;
    _usingDefaultBorderStyle = NO;
    if (self.borderWidth > 0)
    {
        self.clipWidth = self.borderWidth;
        [self setColor:[UIColor colorWithCGColor:self.borderColor] forState:UIControlStateNormal];
        self.borderWidth = 0;
        self.cornerRadius = 0.0f;
        [self updateBorderPath];
    }
}

-(void)setClipWidth:(CGFloat)width
{
    if (_usingDefaultBorderStyle) {
        self.borderWidth = width;
        return;
    }
    if (width == self.clipWidth) return;
    [self updateBorderPath];
    [super setClipWidth:width];
}

-(void)setCornerRadius:(CGFloat)cornerRadius
{
    if (_usingDefaultBorderStyle)
    {
        [super setCornerRadius:cornerRadius];
    }
}

-(void)setRadii:(CGFloat*)radii
{
    if (radii == _radii) return;
    _radii = radii;
    [self updateBorderPath];
}

-(void)setFrame:(CGRect)frame
{
    if (_usingDefaultBorderStyle){
        [super setFrame:UIEdgeInsetsInsetRect(frame, _borderPadding)];
    }
    else {
        [super setFrame:frame];
    }
}

-(void)setBounds:(CGRect)bounds
{
    [self updateBorderPath:_radii inBounds:bounds];
    [super setBounds:bounds];
}


- (void)setColor:(UIColor*)color forState:(UIControlState)state
{
    if (state == UIControlStateNormal && _usingDefaultBorderStyle)
    {
        self.borderWidth = MAX(self.borderWidth,1);
        self.borderColor = color.CGColor;
    }
    else {
        [self swithToContentBorder];
        [super setColor:color forState:state];
        
    }
}

-(void)setBorderPadding:(UIEdgeInsets)padding
{
    _borderPadding = padding;
    [self updateBorderPath];
}


-(TiDrawable*) getOrCreateDrawableForState:(UIControlState)state
{
    [self swithToContentBorder];
    return [super getOrCreateDrawableForState:state];
}

@end
