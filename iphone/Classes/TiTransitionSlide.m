#import "TiTransitionSlide.h"
#import "ADSlideTransition.h"

#define kScaleFactor 0.7f

@implementation TiTransitionSlide


-(Class) adTransitionClass {
    return [ADSlideTransition class];
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
    if (position >1 || position < -1) {
        view.alpha = 0;
        return;
    }
    BOOL outView = (position < 0);
    float percent = fabs(position);
    float multiplier = -1;
    float dest = 0;
    if (![self isTransitionPush]) {
        multiplier = 1;
//        outView = !outView;
    }
    
    float alpha = 1;
    
    double currentPercent = percent;
    if (percent > 1) {
        currentPercent -= floorf(percent); // between 0 and 1
    }
    CATransform3D transform = CATransform3DIdentity;
    
    
    float scaleFactor = 1.0f;
    float translateX = 0.0f;
    float translateY = 0.0f;
    
    if (currentPercent <= 0.33f) // first half
    {
        float percent = 3*currentPercent;
        alpha  = (1-percent/2);
        scaleFactor = 1 - percent*(1-kScaleFactor);
    }
    else
    {
        float percent;
        if (currentPercent > 0.66f) { // first half
            percent = 3*currentPercent - 2;
            alpha = 0.0f;
            percent = 1;
        }
        else {
            percent = 3*currentPercent - 1;
            scaleFactor = kScaleFactor;
            alpha  = (1-percent)*0.5f;
        }
        if (outView) percent = -percent;
        if ([self isTransitionVertical]) {
            translateY += percent;
        }
        else {
            translateX += -percent;
        }
    }

    translateX *= multiplier;
    translateY *= multiplier;
    if (adjust) {
        if ([self isTransitionVertical]) {
                translateY += position;
        }
        else {
                translateX += -position;
        }
    }
    translateX *= size.width;
    translateY *= size.height;
    transform = CATransform3DTranslate(transform, translateX, translateY, 0);
    transform = CATransform3DScale(transform, scaleFactor, scaleFactor, 1.0f);
    
    view.alpha = alpha;
    view.layer.transform = transform;
}

-(BOOL)needsReverseDrawOrder
{
    return [self isTransitionPush];
}
@end