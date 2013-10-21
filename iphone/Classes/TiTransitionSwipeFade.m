#import "TiTransitionSwipeFade.h"
#import "ADSwipeFadeTransition.h"

@implementation TiTransitionSwipeFade

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADSwipeFadeTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
//    if (position >1 || position < -1) {
//        view.alpha = 0;
//        return;
//    }
    BOOL before = (position < 0);
    float multiplier = 1;
    float dest = 0;
    if (![self isTransitionPush]) {
        multiplier = -1;
        before = !before;
    }
    
    int viewWidth = view.frame.size.width;
    int viewHeight = view.frame.size.height;
    
    float alpha = 1;
    if (before) { //out
        dest = multiplier* ABS(position)*(1.0f-kSwipeFadeTranslate);
        alpha = 1.0f - ABS(position);
    }
    
    view.alpha = alpha;
    if (adjust) {
        if ([self isTransitionVertical]) {
           view.layer.transform = CATransform3DMakeTranslation(0.0f, viewWidth * dest, 0.0f);
        }
        else {
            view.layer.transform = CATransform3DMakeTranslation(viewHeight * dest, 0.0f, 0.0f);
        }
    }
    
}

-(BOOL)needsReverseDrawOrder
{
    return ![self isTransitionPush];
}

@end
