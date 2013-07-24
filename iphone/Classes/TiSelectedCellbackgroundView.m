/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITABLEVIEW) || defined(USE_TI_UILISTVIEW)

#import "TiSelectedCellbackgroundView.h"

#define ROUND_SIZE 10


@implementation TiSelectedCellBackgroundView

@synthesize position,fillColor,grouped;

- (id)initWithFrame:(CGRect)frame {
    if (self = [super initWithFrame:frame])
    {
        position = TiCellBackgroundViewPositionMiddle;
        self.backgroundColor = [UIColor clearColor];
        self.contentMode = UIViewContentModeRedraw;
        self.clipsToBounds = YES;
        self.layer.masksToBounds=YES;
    }
    return self;
}

-(void)dealloc
{
    [bgdLayer removeFromSuperlayer];
	[bgdLayer release];
	[super dealloc];
}

- (void)layoutSubviews
{

    if (bgdLayer != nil){
        if ([bgdLayer superlayer] == nil) {
            [[self layer] addSublayer:bgdLayer];
        }
        bgdLayer.frame = self.bounds;
    }
    [super layoutSubviews];
}

-(void)drawRect:(CGRect)rect
{
    CGContextRef ctx = UIGraphicsGetCurrentContext();
    
//    CGContextSaveGState(ctx);
//    CGContextSetFillColorWithColor(ctx, [self.backgroundColor CGColor]);
//    CGContextSetStrokeColorWithColor(ctx, [[UIColor grayColor] CGColor]);
//    CGContextSetLineWidth(ctx, 2);
//	
//    if (grouped && position == TiCellBackgroundViewPositionTop)
//	{
//        CGFloat minx = CGRectGetMinX(rect), midx = CGRectGetMidX(rect), maxx = CGRectGetMaxX(rect) ;
//        CGFloat miny = CGRectGetMinY(rect), maxy = CGRectGetMaxY(rect);
//		
//        CGContextMoveToPoint(ctx, minx, maxy);
//        CGContextAddArcToPoint(ctx, minx, miny, midx, miny, ROUND_SIZE);
//        CGContextAddArcToPoint(ctx, maxx, miny, maxx, maxy, ROUND_SIZE);
//        CGContextAddLineToPoint(ctx, maxx, maxy);
//        CGContextClosePath(ctx);
//		CGContextDrawPath(ctx, kCGPathFillStroke);
//    }
//	else if (grouped && position == TiCellBackgroundViewPositionBottom)
//	{
//        CGFloat minx = CGRectGetMinX(rect) , midx = CGRectGetMidX(rect), maxx = CGRectGetMaxX(rect) ;
//        CGFloat miny = CGRectGetMinY(rect) , maxy = CGRectGetMaxY(rect) ;
//		
//        CGContextMoveToPoint(ctx, minx, miny);
//        CGContextAddArcToPoint(ctx, minx, maxy, midx, maxy, ROUND_SIZE);
//        CGContextAddArcToPoint(ctx, maxx, maxy, maxx, miny, ROUND_SIZE);
//        CGContextAddLineToPoint(ctx, maxx, miny);
//        CGContextClosePath(ctx);
//		CGContextDrawPath(ctx, kCGPathFillStroke);
//    }
//	else if (grouped || position == TiCellBackgroundViewPositionMiddle)
//	{
//        CGFloat minx = CGRectGetMinX(rect), maxx = CGRectGetMaxX(rect);
//        CGFloat miny = CGRectGetMinY(rect), maxy = CGRectGetMaxY(rect);
//        CGContextMoveToPoint(ctx, minx, miny);
//        CGContextAddLineToPoint(ctx, maxx, miny);
//        CGContextAddLineToPoint(ctx, maxx, maxy);
//        CGContextAddLineToPoint(ctx, minx, maxy);
//        CGContextClosePath(ctx);
//		CGContextDrawPath(ctx, kCGPathFillStroke);
//    }
//	else if (grouped && position == TiCellBackgroundViewPositionSingleLine)
//	{
//		CGContextBeginPath(ctx);
//		addRoundedRectToPath(ctx, rect, ROUND_SIZE*1.5, ROUND_SIZE*1.5);
//		CGContextFillPath(ctx);
//		
//		CGContextSetLineWidth(ctx, 2);
//		CGContextBeginPath(ctx);
//		addRoundedRectToPath(ctx, rect, ROUND_SIZE*1.5, ROUND_SIZE*1.5);
//		CGContextStrokePath(ctx);
//	}
//    CGContextClip(ctx);
    
//    CGContextRestoreGState(ctx);
    
	[super drawRect:rect];
    if (bgdLayer != nil){
        [bgdLayer drawInContext:ctx];
    }
}

-(void)setPosition:(TiCellBackgroundViewPosition)inPosition
{
	if(position != inPosition)
	{
		position = inPosition;
		[self setNeedsDisplay];
	}
}


- (void)setState:(UIControlState)state
{
    if (bgdLayer) {
        [bgdLayer setState:state];
    }
}



- (void)setSelected:(BOOL)selected animated:(BOOL)animated
{
    UIControlState state = selected?UIControlStateSelected:UIControlStateNormal;
    [bgdLayer setState:state];
}



- (void)setHighlighted:(BOOL)selected animated:(BOOL)animated
{
    UIControlState state = selected?UIControlStateHighlighted:UIControlStateNormal;
    [bgdLayer setState:state];
}


-(TiSelectableBackgroundLayer*) bgdLayer
{
    if (bgdLayer == nil) {
        bgdLayer = [[TiSelectableBackgroundLayer alloc] init];
		[bgdLayer setFrame:[self bounds]];
        bgdLayer.masksToBounds = YES;
        [bgdLayer setNeedsDisplayOnBoundsChange:YES];
		[[self layer] addSublayer:bgdLayer];
    }
    return bgdLayer;
}

- (void)setColor:(UIColor*)color forState:(UIControlState)state
{
    [[self bgdLayer] setColor:color forState:state];
}


- (void)setImage:(UIImage*)image forState:(UIControlState)state
{
    [[self bgdLayer] setImage:image forState:state];
}

- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    [[self bgdLayer] setGradient:gradient forState:state];
}

-(void)setBackgroundOpacity:(CGFloat)opacity
{
    if (bgdLayer!=nil) {
        bgdLayer.opacity = opacity;
    }
}
-(void)setBorderRadius:(CGFloat)radius
{
    if (bgdLayer!=nil) {
        bgdLayer.cornerRadius = radius;
    }
}
@end

#endif