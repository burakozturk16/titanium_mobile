//
//  ADModernPushTransition.m
//  AppLibrary
//
//  Created by Martin Guillon on 23/09/13.
//
//

#import "ADModernPushTransition.h"
#import "CAMediaTimingFunction+AdditionalEquations.h"

#define outScale -0.3f

@interface ADModernPushTransition()
{
    UIView* fadeView;
}
@end

@implementation ADModernPushTransition

-(void) dealloc
{
    [super dealloc];
    if (fadeView) {
        [fadeView release];
        fadeView = nil;
    }
}

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect reversed:(BOOL)reversed {
    const CGFloat viewWidth = sourceRect.size.width;
    const CGFloat viewHeight = sourceRect.size.height;
    
    CABasicAnimation * inAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    inAnimation.timingFunction = [CAMediaTimingFunction functionWithControlPoints:0.1: 0.7: 0.1: 1];
    inAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    
    CABasicAnimation * outSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    outSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    
    ADTransitionOrientation rOrient = reversed?[ADTransition reversedOrientation:orientation]:orientation;
    switch (rOrient) {
        case ADTransitionRightToLeft:
        {
            inAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(outScale*viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionLeftToRight:
        {
            inAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- viewWidth, 0.0f, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- outScale*viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionTopToBottom:
        {
            inAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - outScale*viewHeight, 0.0f)];
        }
            break;
        case ADTransitionBottomToTop:
        {
            inAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight, 0.0f)];
            outSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, outScale*viewHeight, 0.0f)];
        }
            break;
        default:
            NSAssert(FALSE, @"Unhandled ADTransitionOrientation");
            break;
    }
    inAnimation.duration = duration;
    outSwipeAnimation.duration = duration;
    
    CABasicAnimation * outPositionAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outPositionAnimation.fromValue = @-0.001;
    outPositionAnimation.toValue = @-0.001;
    outPositionAnimation.duration = duration;
    
    //    CABasicAnimation * outOpacityAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    //    outOpacityAnimation.fromValue = @1.0f;
    //    outOpacityAnimation.toValue = @0.9f;
    //    outOpacityAnimation.duration = duration;
    
    CAAnimationGroup * outAnimation = [CAAnimationGroup animation];
    [outAnimation setAnimations:@[outSwipeAnimation, outPositionAnimation]];
    outAnimation.duration = duration;
    
    return [super initWithInAnimation:inAnimation andOutAnimation:outAnimation orientation:orientation reversed:reversed];
}

-(void)prepareTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer
{
    [super prepareTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    if (fadeView == nil) {
        fadeView = [[UIView alloc] initWithFrame:CGRectZero];
        fadeView.backgroundColor = [UIColor blackColor];
    }
    if (self.isReversed) {
        if (viewIn) {
            fadeView.frame = viewContainer.bounds;
            [viewIn addSubview:fadeView];
            //            [viewContainer sendSubviewToBack:viewIn];
        }
        if (viewOut) {
            //            [viewContainer bringSubviewToFront:viewOut];
        }
        
    }
    else if (viewOut){
        CGRect bounds =viewContainer.bounds;
        fadeView.frame = bounds;
        //        fadeView.layer.opacity= 0.0f;
        [viewOut addSubview:fadeView];
    }
}

-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer
{
    [fadeView removeFromSuperview];
    [super finishedTransitionFromView:viewOut toView:viewIn inside:viewContainer];
}

-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer {
    [super startTransitionFromView:viewOut toView:viewIn inside:viewContainer];
    CGFloat finalValue = 0.2f;
    fadeView.alpha = self.isReversed?finalValue:0.0f;
    [UIView animateWithDuration:self.duration animations:^{
        fadeView.alpha = self.isReversed?0.0f:finalValue;
    }];
}

@end
