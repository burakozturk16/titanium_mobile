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
@property(nonatomic,assign) UIEdgeInsets thePadding;
-(void)updateBorderPath:(const CGFloat*)radii inBounds:(CGRect)bounds;
-(void)setRadii:(CGFloat*)radii;
-(void)swithToContentBorder;
@end
