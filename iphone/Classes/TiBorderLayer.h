//
//  CABorderLayer.h
//  Titanium
//
//  Created by Martin Guillon on 24/12/13.
//
//

#import "TiSelectableBackgroundLayer.h"

@class TiViewAnimationStep;
@interface TiBorderLayer : TiSelectableBackgroundLayer
-(void)updateBorderPath:(const CGFloat*)radii inBounds:(CGRect)bounds;
-(void)setRadii:(CGFloat*)radii;
-(void)swithToContentBorder;
-(void)setBorderPadding:(UIEdgeInsets)padding;
@end
