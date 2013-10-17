//
//  TiTransitionFold.m
//  Titanium
//
//  Created by Martin Guillon on 17/10/13.
//
//

#import "TiTransitionFold.h"
#import "TiTransitionHelper.h"

@implementation TiTransitionFold


-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
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
    return [TiTransitionHelper isTransitionPush:self];
}
@end

