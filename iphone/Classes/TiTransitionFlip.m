#import "TiTransitionFlip.h"
#import "ADFlipTransition.h"
#define kAngle M_PI

@implementation TiTransitionFlip

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADFlipTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
    if (position >1 || position < -1) {
        view.layer.hidden = YES;
        return;
    }
    
    float multiplier = -1;
    if (![self isTransitionPush]) {
        multiplier = 1;
    }
    
    float percent = ABS(position);
    int viewWidth = view.bounds.size.width;
    int viewHeight = view.bounds.size.height;
    CATransform3D transform = CATransform3DIdentity;
    if ([self isTransitionVertical]) {
        
    }
    else {
        CGFloat halfWidth = viewWidth / 2;
        CGFloat realAngle = -kAngle * position * multiplier;
        CGFloat translateX = -position * viewWidth;
        if (adjust) transform = CATransform3DTranslate(transform, translateX, 0,0);
        transform = CATransform3DRotate(transform, realAngle, 0, 1, 0);
        view.layer.hidden = (fabs(realAngle) > M_PI_2);
    }
    view.layer.transform = transform;
    view.layer.doubleSided = NO;
    
}
@end
