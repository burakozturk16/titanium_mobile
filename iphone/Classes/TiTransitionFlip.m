//
//  TiTransitionFlip.m
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "TiTransitionFlip.h"
#import "TiTransitionHelper.h"

#define kPerspective -500
#define kAngle M_PI

@implementation TiTransitionFlip
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
    if (position >1 || position < -1) return;
    
    float multiplier = 1;
    if (![TiTransitionHelper isTransitionPush:self]) {
        multiplier = -1;
    }
    
    float percent = ABS(position);
    int viewWidth = view.bounds.size.width;
    int viewHeight = view.bounds.size.height;
    CATransform3D transform = CATransform3DIdentity;
    if ([TiTransitionHelper isTransitionVertical:self]) {
        
    }
    else {
        CGFloat halfWidth = viewWidth / 2;
        CGFloat realAngle = -kAngle * position * multiplier;
        CGFloat translateX = -position * viewWidth * multiplier;
        if (adjust) transform = CATransform3DTranslate(transform, translateX, 0,0);
        transform = CATransform3DRotate(transform, realAngle, 0, 1, 0);
        view.layer.hidden = (fabs(realAngle) > M_PI_2);
    }
    view.layer.transform = transform;
    view.layer.doubleSided = NO;
    
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
-(void)prepareViewHolder:(UIView*)holder
{
    CATransform3D sublayerTransform = CATransform3DIdentity;
    sublayerTransform.m34 = 1.0 / kPerspective;
    holder.layer.sublayerTransform = sublayerTransform;
}

-(BOOL)needsReverseDrawOrder
{
    return NO;
}

@end
