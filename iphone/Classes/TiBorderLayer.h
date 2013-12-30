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
@property(nonatomic,readonly) CGPathRef clippingPath;
@property(nonatomic,assign) CGFloat theWidth;
@property(nonatomic,assign) UIEdgeInsets thePadding;
-(void)setRadius:(id)value;
-(void)setFrame:(CGRect)frame withinAnimation:(TiViewAnimationStep*)animation;
-(CGPathRef)updateBorderRect:(CGRect)bounds;
@end
