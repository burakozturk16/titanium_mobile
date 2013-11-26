//
//  ADModernPushTransition.m
//  AppLibrary
//
//  Created by Martin Guillon on 23/09/13.
//
//

#import "ADModernPushTransition.h"
#import "CAMediaTimingFunction+AdditionalEquations.h"

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

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect {
    self.orientation = orientation;
    const CGFloat viewWidth = sourceRect.size.width;
    const CGFloat viewHeight = sourceRect.size.height;

    CABasicAnimation * inSwipeAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    inSwipeAnimation.timingFunction = [CAMediaTimingFunction easeOutExpo];
    inSwipeAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    switch (orientation) {
        case ADTransitionRightToLeft:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionLeftToRight:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(- viewWidth, 0.0f, 0.0f)];
        }
            break;
        case ADTransitionTopToBottom:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, - viewHeight, 0.0f)];
        }
            break;
        case ADTransitionBottomToTop:
        {
            inSwipeAnimation.fromValue = [NSValue valueWithCATransform3D:CATransform3DMakeTranslation(0.0f, viewHeight, 0.0f)];
        }
            break;
        default:
            NSAssert(FALSE, @"Unhandled ADTransitionOrientation");
            break;
    }
    inSwipeAnimation.duration = duration;

    CABasicAnimation * outPositionAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outPositionAnimation.fromValue = @-0.001;
    outPositionAnimation.toValue = @-0.001;
    outPositionAnimation.duration = duration;

    CAAnimationGroup * outAnimation = [CAAnimationGroup animation];
    [outAnimation setAnimations:@[outPositionAnimation]];
    outAnimation.duration = duration;
    
    

    self = [super initWithInAnimation:inSwipeAnimation andOutAnimation:outAnimation];
    return self;
}

- (ADTransition *)reverseTransition {
    ADDualTransition *reverse = (ADDualTransition*)[super reverseTransition];
    reverse.outAnimation.timingFunction = [CAMediaTimingFunction functionWithName:kCAAnimationLinear];
    reverse.outAnimation.duration = reverse.outAnimation.duration/2;
    return reverse;
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
            fadeView.frame = viewIn.bounds;
            [viewIn addSubview:fadeView];
        }
        
    }
    else if (viewOut){
        fadeView.frame = viewOut.bounds;
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
    CABasicAnimation* fadeAnimation = [CABasicAnimation animationWithKeyPath:@"opacity"];
    CGFloat finalValue = 0.2f;
    if (self.isReversed) {
        fadeAnimation.fromValue = [NSNumber numberWithFloat:finalValue];
        fadeAnimation.toValue = @0.0f;
    }
    else {
        fadeAnimation.fromValue = @0.0f;
        fadeAnimation.toValue = [NSNumber numberWithFloat:finalValue];
    }
    fadeAnimation.duration = _outAnimation.duration;
    fadeAnimation.fillMode = kCAFillModeBoth;
    
    [fadeView.layer addAnimation:fadeAnimation forKey:@"opacity"];
    if (self.isReversed) {
        fadeView.layer.opacity= 0.0f;
    }
    else {
        fadeView.layer.opacity= finalValue;
    }
}

@end
