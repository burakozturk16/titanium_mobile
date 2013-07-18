#import "TiChartsLineAndTextLayer.h"

/**
 *  @brief A Core Animation layer that displays a line and text.
 **/
@implementation TiChartsLineAndTextLayer
@synthesize textDisplacement;
#pragma mark -
#pragma mark Init/Dealloc

#define kDefaultTextDisplacementX 4.0
#define kDefaultTextDisplacementY 14.0


/** @brief Initializes a newly allocated CPTLineLayer object with the provided direction and style. This is the designated initializer.
 *  @param direction direction of the line.
 *  @param newStyle The text style used to draw the text.
 *  @return The initialized CPTLineLayer object.
 **/
-(id)initWithDirection:(TiChartsLineDirection)newDirection style:(CPTLineStyle *)newStyle
{
    if ( (self = [super initWithDirection:newDirection style:newStyle]) ) {
        textLayer = [[CPTTextLayer alloc] initWithFrame:CGRectZero];
        textLayer.paddingLeft = kDefaultTextDisplacementX;
        textLayer.paddingBottom = kDefaultTextDisplacementY;
        [self addSublayer:textLayer];
        self.needsDisplayOnBoundsChange = YES;
//        [self sizeToFit];
    }
    
    return self;
}

-(id)initWithDirection:(TiChartsLineDirection)newDirection style:(CPTLineStyle *)newStyle text:(NSString*)text textStyle:(CPTTextStyle*)textStyle
{
    if ( (self = [self initWithDirection:newDirection style:newStyle]) ) {
        textLayer = [[CPTTextLayer alloc] initWithText:text style:textStyle];
        textLayer.paddingLeft = kDefaultTextDisplacementX;
        textLayer.paddingBottom = kDefaultTextDisplacementY;
        self.needsDisplayOnBoundsChange = YES;
        [self addSublayer:textLayer];
//        [self sizeToFit];
    }
    
    return self;
}

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
    return [super initWithFrame:newFrame];
}

/// @}

/// @cond

-(void)dealloc
{
    [textLayer release];
    [super dealloc];
}

/// @endcond

#pragma mark -
#pragma mark NSCoding Methods

/// @cond

-(void)encodeWithCoder:(NSCoder *)coder
{
    [super encodeWithCoder:coder];
    [textLayer encodeWithCoder:coder];
}

-(id)initWithCoder:(NSCoder *)coder
{
    if ( (self = [super initWithCoder:coder]) ) {
        textLayer = [[[CPTTextLayer alloc] initWithFrame:CGRectZero] initWithCoder:coder];
        [self addSublayer:textLayer];
    }
    return self;
}

/// @endcond

#pragma mark -
#pragma mark Accessors

/// @cond

-(void)setTextShadow:(CPTShadow *)newShadow
{
    [textLayer setShadow:newShadow];
    [self sizeToFit];
}

-(void)setText:(NSString *)newValue
{
    [textLayer setText:newValue];
    [self sizeToFit];
}

-(void)setTextStyle:(CPTTextStyle *)newStyle
{
    [textLayer setTextStyle:newStyle];
    [self sizeToFit];
}

-(void)setAttributedText:(NSAttributedString *)newValue
{
    [textLayer setAttributedText:newValue];
    [self sizeToFit];
}

-(void)setTextDisplacement:(CGPoint)displacement
{
    CGFloat realYDisp = displacement.y - kDefaultTextDisplacementY;
    if (realYDisp < 0)
    {
        textLayer.paddingTop = 0;
        textLayer.paddingBottom = -realYDisp;
    }
    else {
        textLayer.paddingTop = realYDisp;
        textLayer.paddingBottom = 0;
    }
    
    CGFloat realXDisp = displacement.x - kDefaultTextDisplacementX;
    if (realXDisp < 0)
    {
        textLayer.paddingLeft = 0;
        textLayer.paddingRight = -realXDisp;
    }
    else {
        textLayer.paddingLeft = realXDisp;
        textLayer.paddingRight = 0;
    }
    [self sizeToFit];
}

/// @endcond

#pragma mark -
#pragma mark Layout

/**
 *  @brief Determine the minimum size needed to fit the line
 **/
-(CGSize)sizeThatFits
{
    CGSize mySize  = CGSizeZero;
    CGSize lineSize  = [super sizeThatFits];
    CGSize textSize  = textLayer.frame.size;
    
    if (direction == CPTLineDirectionHorizontal) {
        mySize.height = MAX(lineSize.height, textSize.height);
        mySize.width = lineSize.width;
    }
    else {
        mySize.width = MAX(lineSize.width,  textSize.width);
        mySize.height = lineSize.height;
    }
    return mySize;
}

/**
 *  @brief Resizes the layer to fit its contents leaving a narrow margin on all four sides.
 **/
-(void)sizeToFit
{
        [textLayer sizeToFit];
        CGSize sizeThatFits = [self sizeThatFits];
        CGRect newBounds    = self.bounds;
        newBounds.size         = sizeThatFits;
        newBounds.size.width  += self.paddingLeft + self.paddingRight;
        newBounds.size.height += self.paddingTop + self.paddingBottom;
        
        self.bounds = newBounds;
        [self setNeedsLayout];
        [self setNeedsDisplay];
}

@end
