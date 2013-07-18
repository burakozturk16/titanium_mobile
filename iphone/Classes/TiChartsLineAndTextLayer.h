#import "TiChartsLineLayer.h"
#import "CPTTextLayer.h"

/// @file


@interface TiChartsLineAndTextLayer : TiChartsLineLayer {
@private
    CPTTextLayer *textLayer;
    CGPoint textDisplacement;
}
@property (readwrite, assign, nonatomic) CGPoint textDisplacement;

/// @name Layout
/// @{
-(CGSize)sizeThatFits;
-(void)sizeToFit;
/// @}

-(id)initWithDirection:(TiChartsLineDirection)newDirection style:(CPTLineStyle *)newStyle text:(NSString*)text textStyle:(CPTTextStyle*)textStyle;


-(void)setTextShadow:(CPTShadow *)newShadow;
-(void)setText:(NSString *)newValue;
-(void)setTextStyle:(CPTTextStyle *)newStyle;
-(void)setAttributedText:(NSAttributedString *)newValue;
-(void)setDisplacement:(CGPoint)displacement;
@end
