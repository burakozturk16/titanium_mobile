//
//  CABorderLayer.h
//  Titanium
//
//  Created by Martin Guillon on 24/12/13.
//
//

#import <QuartzCore/QuartzCore.h>

@interface TiBorderLayer : CALayer
@property(nonatomic,readonly) CGPathRef path;
@property(nonatomic,retain) UIColor* borderColor;
@property(nonatomic,assign) CGFloat borderWidth;
@property(nonatomic,assign) UIEdgeInsets borderPadding;
-(void)setRadius:(id)value;
@end
