#import "TiTransitionFlip.h"
#import "ADFlipTransition.h"
#define kAngle M_PI

@implementation TiTransitionFlip


-(Class) adTransitionClass {
    return [ADFlipTransition class];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size
{
    if (position >1 || position < -1) {
        view.layer.hidden = YES;
        view.layer.transform = CATransform3DIdentity;
        return;
    }
    
    float multiplier = -1;
    if (![self isTransitionPush]) {
        multiplier = 1;
    }
    CGFloat realAngle = -kAngle * position * multiplier;
    BOOL hidden = (fabs(realAngle - M_PI) < 0.1);
    if (hidden) {
        view.layer.hidden = YES;
        view.layer.transform = CATransform3DIdentity;
    } else {
        view.layer.hidden = NO;
        CATransform3D transform = CATransform3DIdentity;
        if ([self isTransitionVertical]) {
            //            CGFloat translateY = -position * view.bounds.size.height;
            //            if (adjust) transform = CATransform3DTranslate(transform, 0, translateY,0);
            transform = CATransform3DRotate(transform, 0, realAngle, 1, 0);
            
        }
        else {
            //            CGFloat translateX = -position * view.bounds.size.width;
            //            if (adjust) transform = CATransform3DTranslate(transform, translateX, 0,0);
            transform = CATransform3DRotate(transform, realAngle, 0, 1, 0);
        }
        //        NSLog(@"transformView view %f\n%@", radiansToDegrees(realAngle), NSStringFromCATransform3D(transform))
        view.layer.transform = transform;
        view.layer.doubleSided = NO;
    }
    
    
}
@end
