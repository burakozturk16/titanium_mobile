//
//  TITransitionPerspective.m
//  Titanium
//
//  Created by Martin Guillon on 21/11/13.
//
//

#import "TiTransitionPerspective.h"
@interface TiTransitionPerspective()
{
    CATransform3D _oldSublayerTransform;
}
@end
@implementation TiTransitionPerspective

- (id)init
{
    if (self = [super init]) {
        _oldSublayerTransform = CATransform3DIdentity;
    }
    return self;
}

-(void)prepareViewHolder:(UIView*)holder
{
    CATransform3D sublayerTransform = CATransform3DIdentity;
    sublayerTransform.m34 = 1.0 / kPerspective;
    _oldSublayerTransform = holder.layer.sublayerTransform;
    holder.layer.sublayerTransform = sublayerTransform;
}

-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    viewContainer.layer.sublayerTransform = _oldSublayerTransform;
}

@end
