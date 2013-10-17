//
//  TiTransitionSwipe.m
//  Titanium
//
//  Created by Martin Guillon on 14/10/13.
//
//

#import "TiTransitionSwipe.h"
#import "TiTransitionHelper.h"

@implementation TiTransitionSwipe
-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
//    BOOL before = (position < 0);
//    float multiplier = 1;
//    float dest = 0;
//    if (![TiTransitionHelper isTransitionPush:self]) {
//        multiplier = -1;
//        before = !before;
//    }
//    float alpha = 1;
//    if (before) { //out
//        dest = multiplier* ABS(position);
//    }
//    
//    if ([TiTransitionHelper isTransitionVertical:self]) {
//        view.layer.transform = CATransform3DMakeTranslation(0.0f, view.frame.size.height * dest, 0.0f);
//    }
//    else {
//        view.layer.transform = CATransform3DMakeTranslation(view.frame.size.width * dest, 0.0f, 0.0f);
//    }
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
    return ![TiTransitionHelper isTransitionPush:self];
}
-(void)prepareViewHolder:(UIView*)holder{}
@end
