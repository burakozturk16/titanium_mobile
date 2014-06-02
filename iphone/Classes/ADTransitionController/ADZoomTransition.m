//
//  ADZoomTransition.m
//  AppLibrary
//
//  Created by Romain Goyet on 14/03/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "ADZoomTransition.h"

CGPoint ADRectCenter(CGRect rect) {
    return CGPointMake(rect.origin.x + rect.size.width/2.0f, rect.origin.y + rect.size.height/2.0f);
}

@implementation ADZoomTransition

- (id)initWithSourceRect:(CGRect)sourceRect andTargetRect:(CGRect)targetRect forDuration:(double)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed {
    CABasicAnimation * zoomAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    CATransform3D transform = CATransform3DIdentity;
    CGPoint sourceCenter = ADRectCenter(sourceRect);
    CGPoint targetCenter = ADRectCenter(targetRect);
    transform = CATransform3DTranslate(transform, sourceCenter.x - targetCenter.x, sourceCenter.y - targetCenter.y, 0.0f);
    transform = CATransform3DScale(transform, sourceRect.size.width/targetRect.size.width, sourceRect.size.height/targetRect.size.height, 1.0f);
    zoomAnimation.fromValue = [NSValue valueWithCATransform3D:transform];
    zoomAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    zoomAnimation.duration = duration;
    
    if (reversed) {
        NSValue* old = zoomAnimation.fromValue;
        zoomAnimation.fromValue = zoomAnimation.toValue;
        zoomAnimation.toValue = old;
    }

    CABasicAnimation * outAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outAnimation.fromValue = @-0.001;
    outAnimation.toValue = @-0.001;
    outAnimation.duration = duration;

    return [super initWithInAnimation:zoomAnimation andOutAnimation:outAnimation orientation:orientation reversed:reversed];
}

- (id)initWithScale:(CGFloat)scale forDuration:(double)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed {
    CABasicAnimation * zoomAnimation = [CABasicAnimation animationWithKeyPath:@"transform"];
    CATransform3D transform = CATransform3DIdentity;
    transform = CATransform3DScale(transform, scale, scale, 1.0f);
    zoomAnimation.fromValue = [NSValue valueWithCATransform3D:transform];
    zoomAnimation.toValue = [NSValue valueWithCATransform3D:CATransform3DIdentity];
    zoomAnimation.duration = duration;
    
    if (reversed) {
        NSValue* old = zoomAnimation.fromValue;
        zoomAnimation.fromValue = zoomAnimation.toValue;
        zoomAnimation.toValue = old;
    }
    
    CABasicAnimation * outAnimation = [CABasicAnimation animationWithKeyPath:@"zPosition"];
    outAnimation.fromValue = @-0.001;
    outAnimation.toValue = @-0.001;
    outAnimation.duration = duration;
    
    return [super initWithInAnimation:zoomAnimation andOutAnimation:outAnimation orientation:orientation reversed:reversed];
}

@end
