//
//  TiLabel.m
//  Titanium
//
//  Created by Martin Guillon on 19/12/13.
//
//

#import "TiLabel.h"

@interface TiLabel (PrivateMethods)
- (void)redrawLabel;
- (void)resetGradient;
@end


@implementation TiLabel
{
    BOOL _hasStroke;
    UIColor* _strokeColor;
}
@synthesize hasStroke = _hasStroke;
@synthesize strokeColor = _strokeColor;
- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        //initialize common properties
        self.hasStroke = NO;
        self.strokeColor = [UIColor blackColor];
                
        self.backgroundColor = [UIColor clearColor];
        
        [self redrawLabel];
    }
    return self;
}

-(void)dealloc
{
    [_strokeColor release];
    [super dealloc];
}

- (void)redrawLabel {
    //draw gradient
    [self resetGradient];
}

- (void)resetGradient {
    if (!CGRectEqualToRect(self.frame, CGRectZero)) {
    }
}

#pragma mark Overriden/custom setters/getters

- (void)setHasStroke:(BOOL)stroke {
    _hasStroke = stroke;
    [self setNeedsDisplay];
}

- (void)setStrokeColor:(UIColor *)color {
    [_strokeColor release];
    _strokeColor = [color retain];
    //enable/disable stroke
    self.hasStroke = (_strokeColor == nil);
}


- (void)setText:(NSString *)text {
    [super setText:text];
    [self redrawLabel];
}

- (void)setFont:(UIFont *)font {
    [super setFont:font];
    [self redrawLabel];
}

- (void)setFrame:(CGRect)frame {
    [super setFrame:frame];
    //draw gradient
    [self resetGradient];
}

#pragma mark -


- (void)drawTextInRect:(CGRect)rect
{
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    //draw stroke
    if (self.hasStroke) {
        CGContextSetRGBStrokeColor(context, 0.0, 0.0, 0.0, 1.0);
        CGContextSetTextDrawingMode(context, kCGTextFillStroke);
    }
    
    [super drawTextInRect:rect];
}


@end
