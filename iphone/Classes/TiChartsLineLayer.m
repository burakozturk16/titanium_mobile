#import "TiChartsLineLayer.h"

#import "CPTPlatformSpecificCategories.h"
#import "CPTUtilities.h"

/**
 *  @brief A Core Animation layer that displays a simple line.
 **/
@implementation TiChartsLineLayer


/** @property CPTLineStyle *lineStyle
 *  @brief The line style used to draw the line.
 *
 **/
@synthesize lineStyle, direction, parentLayer;

#pragma mark -
#pragma mark Init/Dealloc

/** @brief Initializes a newly allocated CPTLineLayer object with the provided direction and style. This is the designated initializer.
 *  @param direction direction of the line.
 *  @param newStyle The text style used to draw the text.
 *  @return The initialized CPTLineLayer object.
 **/
-(id)initWithDirection:(TiChartsLineDirection)newDirection style:(CPTLineStyle *)newStyle
{
    if ( (self = [super initWithFrame:CGRectZero]) ) {
        lineStyle      = [newStyle retain];
        direction      = newDirection;
        parentLayer    = nil;
        self.needsDisplayOnBoundsChange = NO;
    }
    
    return self;
}

/** @brief Initializes a newly allocated CPTLineLayer object with the provided direction and style. This is the designated initializer.
 *  @param direction direction of the line.
 *  @return The initialized CPTLineLayer object.
 **/
-(id)initWithDirection:(TiChartsLineDirection)newDirection
{
    return [self initWithDirection:newDirection style:[CPTLineStyle lineStyle]];
}

/// @cond

-(id)initWithLayer:(id)layer
{
    if ( (self = [super initWithLayer:layer]) ) {
        TiChartsLineLayer *theLayer = (TiChartsLineLayer *)layer;
        
        lineStyle      = [theLayer->lineStyle retain];
        direction      = theLayer->direction;
    }
    return self;
}

/// @endcond

/// @name Initialization
/// @{

/** @brief Initializes a newly allocated CPTLineLayer object with the provided frame rectangle.
 *
 *  The initialized layer will have the following properties:
 *  - @ref direction = @CPTLineDirectionHorizontal
 *  - @ref lineStyle = @nil
 *
 *  @param newFrame The frame rectangle.
 *  @return The initialized CPTLineLayer object.
 **/
-(id)initWithFrame:(CGRect)newFrame
{
    return [self initWithDirection:CPTLineDirectionHorizontal style:nil];
}

/// @}

/// @cond

-(void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [lineStyle release];
    [parentLayer release];
    
    [super dealloc];
}

/// @endcond

#pragma mark -
#pragma mark NSCoding Methods

/// @cond

-(void)encodeWithCoder:(NSCoder *)coder
{
    [super encodeWithCoder:coder];
    
    [coder encodeObject:self.lineStyle forKey:@"CPTLineLayer.lineStyle"];
    [coder encodeObject:[NSNumber numberWithInt:self.direction] forKey:@"CPTLineLayer.direction"];
}

-(id)initWithCoder:(NSCoder *)coder
{
    if ( (self = [super initWithCoder:coder]) ) {
        lineStyle      = [[coder decodeObjectForKey:@"CPTLineLayer.lineStyle"] retain];
        direction      = (TiChartsLineDirection)[[coder decodeObjectForKey:@"CPTLineLayer.direction"] intValue];
        parentLayer = nil;
    }
    return self;
}

/// @endcond

#pragma mark -
#pragma mark Accessors

/// @cond

-(void)setDirection:(TiChartsLineDirection)newDirection
{
    if ( direction != newDirection ) {
        direction = newDirection;
        [self sizeToFit];
    }
}

-(void)setLineStyle:(CPTLineStyle *)newStyle
{
    if ( lineStyle != newStyle ) {
        [lineStyle release];
        lineStyle = [newStyle retain];
        [self sizeToFit];
    }
}

-(void)setParentLayer:(CPTLayer *)newParentLayer
{
    if ( parentLayer != newParentLayer ) {
        [parentLayer release];
        parentLayer = [newParentLayer retain];
        [self sizeToFit];
    }
}

-(void)setShadow:(CPTShadow *)newShadow
{
    if ( newShadow != self.shadow ) {
        [super setShadow:newShadow];
        [self sizeToFit];
    }
}

-(void)setPaddingLeft:(CGFloat)newPadding
{
    if ( newPadding != self.paddingLeft ) {
        [super setPaddingLeft:newPadding];
        [self sizeToFit];
    }
}

-(void)setPaddingRight:(CGFloat)newPadding
{
    if ( newPadding != self.paddingRight ) {
        [super setPaddingRight:newPadding];
        [self sizeToFit];
    }
}

-(void)setPaddingTop:(CGFloat)newPadding
{
    if ( newPadding != self.paddingTop ) {
        [super setPaddingTop:newPadding];
        [self sizeToFit];
    }
}

-(void)setPaddingBottom:(CGFloat)newPadding
{
    if ( newPadding != self.paddingBottom ) {
        [super setPaddingBottom:newPadding];
        [self sizeToFit];
    }
}

/// @endcond

#pragma mark -
#pragma mark Layout

/**
 *  @brief Determine the minimum size needed to fit the line
 **/
-(CGSize)sizeThatFits
{
    CGSize lineSize  = CGSizeZero;
    
    if ( lineStyle && parentLayer) {
        if (direction == CPTLineDirectionHorizontal) {
            lineSize.height = lineStyle.lineWidth;
            lineSize.width = parentLayer.frame.size.width;
        }
        else {
            lineSize.width = lineStyle.lineWidth;
            lineSize.height = parentLayer.frame.size.height;
        }

    }
    
    return lineSize;
}

/**
 *  @brief Resizes the layer to fit its contents leaving a narrow margin on all four sides.
 **/
-(void)sizeToFit
{
    if ( lineStyle && parentLayer ) {
        CGSize sizeThatFits = [self sizeThatFits];
        CGRect newBounds    = self.bounds;
        newBounds.size         = sizeThatFits;
        if (direction == CPTLineDirectionHorizontal) {
            newBounds.size.height += self.paddingTop + self.paddingBottom;
        }
        else {
            newBounds.size.width  += self.paddingLeft + self.paddingRight;
        }
        
        self.bounds = newBounds;
        [self setNeedsLayout];
        [self setNeedsDisplay];
    }
}

#pragma mark -
#pragma mark Drawing of text

-(CGPathRef) getPath
{
    CGMutablePathRef pathRef = CGPathCreateMutable();
    CGFloat inset      = self.lineStyle.lineWidth * CPTFloat(0.5);
    if (direction == CPTLineDirectionHorizontal) {
        float yPos = (self.bounds.size.height - self.lineStyle.lineWidth) / 2.0f;
        CGPathMoveToPoint(pathRef, NULL, self.paddingLeft, yPos);
        CGPathAddLineToPoint(pathRef, NULL, self.frame.size.width - self.paddingLeft - self.paddingRight, yPos);
    }
    else {
        float xPos = (self.bounds.size.width - self.lineStyle.lineWidth) / 2.0f;
        CGPathMoveToPoint(pathRef, NULL, xPos, self.paddingTop);
        CGPathAddLineToPoint(pathRef, NULL, xPos, self.frame.size.height - self.paddingTop - self.paddingBottom);
    }
    CGPathCloseSubpath(pathRef);
    return pathRef;
}

/// @cond

-(void)renderAsVectorInContext:(CGContextRef)context
{
    if ( self.hidden ) {
        return;
    }
    
    [super renderAsVectorInContext:context];
    
    CPTLineStyle *theLineStyle = self.lineStyle;
    if ( theLineStyle ) {
        CGPathRef path = [self getPath];
        CGFloat inset      = self.lineStyle.lineWidth * CPTFloat(0.5);
        if (direction == CPTLineDirectionHorizontal) {
            float yPos = (self.bounds.size.height - self.lineStyle.lineWidth) / 2.0f;
            CGContextMoveToPoint(context, self.paddingLeft, yPos);
            CGContextAddLineToPoint(context, self.frame.size.width - self.paddingLeft - self.paddingRight, yPos);
        }
        else {
            float xPos = (self.bounds.size.width - self.lineStyle.lineWidth) / 2.0f;
            CGContextMoveToPoint(context, xPos, self.paddingTop);
            CGContextAddLineToPoint(context, xPos, self.frame.size.height - self.paddingTop - self.paddingBottom);
        }

        [theLineStyle setLineStyleInContext:context];
        [theLineStyle strokePathWithinRect:self.bounds inContext:context];
    }
}

/// @endcond

@end
