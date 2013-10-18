#import "TiTransitionSlide.h"
#import "ADSlideTransition.h"

#define kScaleFactor 0.7f

@implementation TiTransitionSlide


- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADSlideTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
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
        outView = !outView;
    }
    
    int viewWidth = view.frame.size.width;
    int viewHeight = view.frame.size.height;
    
    float alpha = 1;
    
    double currentPercent = percent - floorf(percent); // between 0 and 1
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
    else if (currentPercent > 0.66f) // first half
    {
        float percent = 3*currentPercent - 2;
        alpha = 0.0f;
        scaleFactor = kScaleFactor;
        if ([self isTransitionVertical]) {
            translateY += -1;
        }
        else {
            translateX += +1;
        }
    }
    else {
        float percent = 3*currentPercent - 1;
        scaleFactor = kScaleFactor;
        alpha  = (1-percent)*0.5f;
        if (outView)percent  = -percent;
        if ([self isTransitionVertical]) {
            translateY += percent;
        }
        else {
            translateX += -percent;
        }
    }
    translateX *= multiplier;
    translateY *= multiplier;
    if ([self isTransitionVertical]) {
        if (adjust) {
            translateY += position;
        }
    }
    else {
        if (adjust) {
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