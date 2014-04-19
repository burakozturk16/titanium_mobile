#import "TiTransitionFade.h"
#import "ADFadeTransition.h"

@implementation TiTransitionFade


-(Class) adTransitionClass {
    return [ADFadeTransition class];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
    if (position >1 || position < -1) {
        view.alpha = 0;
        return;
    }
    
    float percent = ABS(position);
    int viewWidth = view.frame.size.width;
    int viewHeight = view.frame.size.height;
    
    if (adjust)  {
        CATransform3D transform = CATransform3DIdentity;
        if ([self isTransitionVertical]) {
            transform = CATransform3DTranslate(transform, 0.0f, -position * viewHeight, 0.0f); // cancel scroll
        }
        else {
            transform = CATransform3DTranslate(transform, -position * viewWidth, 0.0f, 0.0f); // cancel scroll
        }
        view.layer.transform = transform;
    }
    view.alpha = 1 - percent;
    
}

@end
