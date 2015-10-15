#import "TiTransitionSwipeDualFade.h"
#import "ADSwipeDualFadeTransition.h"

@implementation TiTransitionSwipeDualFade

-(Class) adTransitionClass {
    return [ADSwipeDualFadeTransition class];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size
{
//    if (position >1 || position < -1) {
//        view.alpha = 0;
//        return;
//    }
    BOOL before = (position < 0);
    float multiplier = 1;
//    float dest = 0;
    if (![self isTransitionPush]) {
        multiplier = -1;
        before = !before;
    }
    
    int viewWidth = view.frame.size.width;
    int viewHeight = view.frame.size.height;
    
    float alpha = 1;
//    if (before) { //out
//        dest = multiplier* ABS(position)*(1.0f-kSwipeDualFadeTranslate);
//    }
    alpha = 1.0f - ABS(position);
    
    float translate = position;
    //    if (adjust) {
    //        translate += -position;
    //    }
    
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
//    if (adjust && dest != 0) {
//        if ([self isTransitionVertical]) {
//           view.layer.transform = CATransform3DMakeTranslation(0.0f, viewWidth * dest, 0.0f);
//        }
//        else {
//            view.layer.transform = CATransform3DMakeTranslation(viewHeight * dest, 0.0f, 0.0f);
//        }
//    }
    
}

-(BOOL)needsReverseDrawOrder
{
    return ![self isTransitionPush];
}

@end
