/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITABLEVIEW) || defined(USE_TI_UILISTVIEW)
 
#import "TiSelectableBackgroundLayer.h"

typedef enum
{
    TiCellBackgroundViewPositionTop,
    TiCellBackgroundViewPositionMiddle,
    TiCellBackgroundViewPositionBottom,
	TiCellBackgroundViewPositionSingleLine
} TiCellBackgroundViewPosition;


@interface TiSelectedCellBackgroundView : UIView
{
    TiCellBackgroundViewPosition position;
	UIColor *fillColor;
	BOOL grouped;
    TiSelectableBackgroundLayer* bgdLayer;
}
@property(nonatomic) TiCellBackgroundViewPosition position;
@property(nonatomic,retain) UIColor *fillColor;
@property(nonatomic) BOOL grouped;


- (void)setState:(UIControlState)state;
- (void)setColor:(UIColor*)color forState:(UIControlState)state;
- (void)setImage:(UIImage*)image forState:(UIControlState)state;
- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state;
- (void)setSelected:(BOOL)selected animated:(BOOL)animated;
- (void)setHighlighted:(BOOL)selected animated:(BOOL)animated;
-(void)setBackgroundOpacity:(CGFloat)opacity;
-(void)setBorderRadius:(CGFloat)radius;
@end

#endif