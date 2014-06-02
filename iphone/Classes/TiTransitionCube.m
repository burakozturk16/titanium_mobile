#import "TiTransitionCube.h"
#import "ADCubeTransition.h"

#define kArc M_PI * 2.0f

@interface TiTransitionCube()

@end
@implementation TiTransitionCube


-(Class) adTransitionClass {
    return [ADCubeTransition class];
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
    if (fabs(position) >= _faceNb - 1)
    {
        view.layer.hidden = YES;
        return;
    }
    view.layer.hidden = NO;
    view.layer.doubleSided = NO;
    
    float multiplier = 1;
    if (![self isTransitionPush]) {
        multiplier = -1;
    }
    CATransform3D transform = CATransform3DIdentity;
    if (!adjust) {
        transform.m34 = 1.0 / kPerspective;
    }
    if ([self isTransitionVertical]) {
        CGFloat radius = fmaxf(0.0f, size.height / 2.0f / tanf(kArc/2.0f/_faceNb));
        CGFloat angle = position / _faceNb * kArc;
        if (adjust) {
            CGFloat translateY = -position * size.height * multiplier;
            transform = CATransform3DTranslate(transform, 0, -translateY, 0);
        }
        transform = CATransform3DTranslate(transform, 0.0f, 0.0f, -radius);
        transform = CATransform3DRotate(transform, angle, -1.0f, 0.0f, 0.0f);
        transform =  CATransform3DTranslate(transform, 0.0f, 0.0f, radius);
    }
    else {
        CGFloat radius = fmaxf(0.0f, size.width / 2.0f / tanf(kArc/2.0f/_faceNb));
        CGFloat angle = position * kArc / _faceNb;
        if (adjust) {
            CGFloat translateX = -position * size.width * multiplier;
            transform = CATransform3DTranslate(transform, translateX, 0, 0);
        }
        transform = CATransform3DTranslate(transform, 0.0f, 0.0f, -radius);
        transform = CATransform3DRotate(transform, angle, 0.0f, 1.0f, 0.0f);
        transform =  CATransform3DTranslate(transform, 0.0f, 0.0f, radius);
    }
    
    view.layer.transform = transform;
    
}

-(BOOL)needsReverseDrawOrder
{
    return [self isTransitionPush];
}
@end
