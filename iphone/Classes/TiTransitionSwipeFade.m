#import "TiTransitionSwipeFade.h"
#import "ADSwipeFadeTransition.h"

@implementation TiTransitionSwipeFade

-(Class) adTransitionClass {
    return [ADSwipeFadeTransition class];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
//    if (position >1 || position < -1) {
//        view.alpha = 0;
//        return;
//    }
    BOOL before = (position < 0);
    float multiplier = 1;
    float dest = 0;
    float decale = 1 - kSwipeFadeTranslate;
    if (![self isTransitionPush]) {
        multiplier = -1;
        before = !before;
    }
    
    int viewWidth = view.frame.size.width;
    int viewHeight = view.frame.size.height;
    
    float translate = position;
    float alpha = 1;
    if (adjust) {
        translate += -position;
    }
    
    if (before) { //out
        translate += ABS(position)*decale;
        alpha = 1.0f - ABS(position);
    }
    translate *= multiplier;
    
    view.alpha = alpha;
    if ([self isTransitionVertical]) {
        translate *= viewHeight;
        view.layer.transform = CATransform3DMakeTranslation(0.0f, translate, 0.0f);
    }
    else {
        translate *= viewWidth;
        view.layer.transform = CATransform3DMakeTranslation(translate, 0.0f, 0.0f);
    }
    
}

-(BOOL)needsReverseDrawOrder
{
    return ![self isTransitionPush];
}

@end
