
#import <UIKit/UIKit.h>
#import "TiGradient.h"

@interface TiDrawable : NSObject
{
    NSArray* states;
    UIColor* color;
    UIImage* image;
    TiGradient* gradient;
    BOOL imageRepeat;
}
@property(nonatomic,retain) UIColor *color;
@property(nonatomic,retain) UIImage *image;
@property(nonatomic,retain) TiGradient *gradient;
@property(nonatomic,assign) BOOL imageRepeat;
@end

@interface TiSelectableBackgroundLayer : CALayer
{
    CAShapeLayer* maskLayer;
    NSMutableDictionary* stateLayersMap;
    NSMutableArray* stateLayers;
    CGFloat cornersRadius;
    UIRectCorner roundedCorners;
    BOOL _imageRepeat;
}
@property(nonatomic,assign) BOOL imageRepeat;
@property(nonatomic,readonly) NSDictionary *stateLayersMap;
@property(nonatomic,readonly) NSArray *stateLayers;

- (void)drawInContext:(CGContextRef)ctx inRect:(CGRect)rect;

- (void)setState:(UIControlState)state;
-(TiDrawable*) getOrCreateDrawableForState:(UIControlState)state;
- (void)setColor:(UIColor*)color forState:(UIControlState)state;
- (void)setImage:(UIImage*)image forState:(UIControlState)state;
- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state;
- (void)setRoundedRadius:(CGFloat)radius inCorners:(UIRectCorner)corners;
@end
