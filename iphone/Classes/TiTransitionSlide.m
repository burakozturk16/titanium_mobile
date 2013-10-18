#import "TiTransitionSlide.h"
#import "ADSlideTransition.h"

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
}

-(void)prepareViewHolder:(UIView*)holder
{
    CATransform3D sublayerTransform = CATransform3DIdentity;
    sublayerTransform.m34 = 1.0 / kPerspective;
    holder.layer.sublayerTransform = sublayerTransform;
}

-(BOOL)needsReverseDrawOrder
{
    return [self isTransitionPush];
}
@end