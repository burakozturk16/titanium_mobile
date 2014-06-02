#import "TiTransitionSwipe.h"
#import "ADSwipeTransition.h"

@implementation TiTransitionSwipe
-(Class) adTransitionClass {
    return [ADSwipeTransition class];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
//    BOOL before = (position < 0);
//    float multiplier = 1;
//    float dest = 0;
//    if (![TiTransitionHelper isTransitionPush:self]) {
//        multiplier = -1;
//        before = !before;
//    }
//    float alpha = 1;
//    if (before) { //out
//        dest = multiplier* ABS(position);
//    }
//    
//    if ([TiTransitionHelper isTransitionVertical:self]) {
//        view.layer.transform = CATransform3DMakeTranslation(0.0f, view.frame.size.height * dest, 0.0f);
//    }
//    else {
//        view.layer.transform = CATransform3DMakeTranslation(view.frame.size.width * dest, 0.0f, 0.0f);
//    }
}

@end
