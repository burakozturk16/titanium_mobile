#import "TiTransitionCarousel.h"
#import "ADCarrouselTransition.h"

#define kArc M_PI * 2.0f

@interface TiTransitionCarousel()

@end
@implementation TiTransitionCarousel

-(Class) adTransitionClass {
    return [ADCarrouselTransition class];
}

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed
{
    if (self = [super initWithDuration:duration orientation:orientation sourceRect:sourceRect reversed:reversed]) {
        _faceNb = 4;
    }
    return self;
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
    if (position >1 || position < -1) return;
    
    float multiplier = 1;
    if (![self isTransitionPush]) {
        multiplier = -1;
    }
    
    int viewWidth = view.bounds.size.width;
    int viewHeight = view.bounds.size.height;
    CATransform3D transform = CATransform3DIdentity;
    if (!adjust) transform.m34 = 1.0 / kPerspective;
    if ([self isTransitionVertical]) {
        CGFloat radius = -fmaxf(0.01f, viewHeight / 2.0f / tanf(kArc/2.0f/_faceNb));
        CGFloat angle = -position / _faceNb * kArc;
        CGFloat translateY = -position * viewHeight * multiplier;
        if (adjust) transform = CATransform3DTranslate(transform, 0, -translateY, 0);
        transform = CATransform3DTranslate(transform, 0.0f, 0.0f, -radius);
        transform = CATransform3DRotate(transform, angle, -1.0f, 0.0f, 0.0f);
        transform =  CATransform3DTranslate(transform, 0.0f, 0.0f, radius+ 0.01f);
    }
    else {
        CGFloat radius = -fmaxf(0.01f, viewWidth / 2.0f / tanf(kArc/2.0f/_faceNb));
        CGFloat angle = -position / _faceNb * kArc;
        CGFloat translateX = -position * viewWidth * multiplier;
        if (adjust) transform = CATransform3DTranslate(transform, translateX, 0, 0);
        transform = CATransform3DTranslate(transform, 0.0f, 0.0f, -radius);
        transform = CATransform3DRotate(transform, angle, 0.0f, 1.0f, 0.0f);
        transform =  CATransform3DTranslate(transform, 0.0f, 0.0f, radius+ 0.01f);

    }
    view.layer.transform = transform;

}

-(BOOL)needsReverseDrawOrder
{
    return [self isTransitionPush];
}
@end
