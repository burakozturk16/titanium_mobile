#import <QuartzCore/QuartzCore.h>

@class TiGradient;
@class TiSVGImage;
@class TiViewAnimationStep;
@interface TiDrawable : NSObject
{
    NSArray* states;
    UIColor* color;
    UIImage* image;
    TiSVGImage* svg;
    TiGradient* gradient;
    NSShadow* _shadow;
    BOOL imageRepeat;
}
@property(nonatomic,retain) UIColor *color;
@property(nonatomic,retain) UIImage *image;
@property(nonatomic,retain) TiGradient *gradient;
@property(nonatomic,retain) TiSVGImage *svg;
@property(nonatomic,retain) NSShadow *shadow;
@property(nonatomic,retain) NSArray *innerShadows;
@property(nonatomic,assign) BOOL imageRepeat;
@end

@interface TiSelectableBackgroundLayer : CALayer
{
    NSMutableDictionary* stateLayersMap;
    BOOL _imageRepeat;
    BOOL readyToCreateDrawables;
    BOOL _needsToSetAllDrawablesOnNextSize;
    TiViewAnimationStep *runningAnimation;
    CGFloat _clipWidth;
}
@property(nonatomic,assign) BOOL imageRepeat;
@property(nonatomic,readonly) NSDictionary *stateLayersMap;
@property(nonatomic,assign) BOOL readyToCreateDrawables;
@property(nonatomic,assign) BOOL animateTransition;
@property(nonatomic,assign) CGFloat clipWidth;
@property(nonatomic,assign) CGFloat customPropAnim;

- (void)setState:(UIControlState)state;
- (void)setState:(UIControlState)state animated:(BOOL)animated;
- (UIControlState)getState;
-(TiDrawable*) getOrCreateDrawableForState:(UIControlState)state;
- (void)setColor:(UIColor*)color forState:(UIControlState)state;
- (void)setImage:(id)image forState:(UIControlState)state;
- (void)setGradient:(TiGradient*)gradient forState:(UIControlState)state;
- (void)setInnerShadows:(NSArray*)shadows forState:(UIControlState)state;
-(void)setNonRetina:(BOOL)value;
-(void)setFrame:(CGRect)frame withinAnimation:(TiViewAnimationStep*) runningAnimation;
@end
