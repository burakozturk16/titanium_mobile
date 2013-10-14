//
//  TiTransitionSwipeFade.m
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "TiTransitionSwipeFade.h"
#import "TiTransitionHelper.h"

@implementation TiTransitionSwipeFade
-(void)transformView:(UIView*)view withPosition:(CGFloat)position
{
    BOOL before = (position < 0);
    float multiplier = 1;
    float dest = 0;
    if (![TiTransitionHelper isTransitionPush:self]) {
        multiplier = -1;
        before = !before;
    }
    float alpha = 1;
    if (before) { //out
        dest = multiplier* ABS(position)*(1.0f-kSwipeFadeTranslate);
        alpha = 1.0f - ABS(position);
    }
    
    view.alpha = alpha;
    if ([TiTransitionHelper isTransitionVertical:self]) {
        view.layer.transform = CATransform3DMakeTranslation(0.0f, view.frame.size.height * dest, 0.0f);
    }
    else {
        view.layer.transform = CATransform3DMakeTranslation(view.frame.size.width * dest, 0.0f, 0.0f);
    }
}
-(BOOL)needsReverseDrawOrder
{
    return ![TiTransitionHelper isTransitionPush:self];
}

@end
