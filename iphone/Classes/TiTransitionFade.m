//
//  TiTransitionFade.m
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "TiTransitionFade.h"
#import "TiTransitionHelper.h"

@implementation TiTransitionFade
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
        if ([TiTransitionHelper isTransitionVertical:self]) {
            transform = CATransform3DTranslate(transform, 0.0f, -position * viewHeight, 0.0f); // cancel scroll
        }
        else {
            transform = CATransform3DTranslate(transform, -position * viewWidth, 0.0f, 0.0f); // cancel scroll
        }
        view.layer.transform = transform;
    }
    view.alpha = 1 - percent;
    
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust
{
    [self transformView:view withPosition:position adjustTranslation:adjust size:view.bounds.size];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position size:(CGSize)size
{
    [self transformView:view withPosition:position adjustTranslation:NO size:size];
}
-(void)transformView:(UIView*)view withPosition:(CGFloat)position
{
    [self transformView:view withPosition:position adjustTranslation:NO size:view.bounds.size];
}

-(BOOL)needsReverseDrawOrder
{
    return NO;
}
-(void)prepareViewHolder:(UIView*)holder{}
@end
