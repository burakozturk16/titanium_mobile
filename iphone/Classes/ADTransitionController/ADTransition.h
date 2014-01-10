//
//  ADTransition.h
//  Transition
//
//  Created by Patrick Nollet on 21/02/11.
//  Copyright 2011 Applidium. All rights reserved.
//

// Abstract class

#import <Foundation/Foundation.h>
#import <QuartzCore/CoreAnimation.h>
#define kAdKey @"adAnimation"
#import "CAMediaTimingFunction+AdditionalEquations.h"

extern NSString * ADTransitionAnimationKey;
extern NSString * ADTransitionAnimationInValue;
extern NSString * ADTransitionAnimationOutValue;

@class ADTransition;
@protocol ADTransitionDelegate
@optional
- (void)pushTransitionDidFinish:(ADTransition *)transition;
- (void)popTransitionDidFinish:(ADTransition *)transition;
@end

typedef enum {
    ADTransitionTypeNull,
    ADTransitionTypePush,
    ADTransitionTypePop
} ADTransitionType;

typedef enum {
    ADTransitionRightToLeft,
    ADTransitionLeftToRight,
    ADTransitionTopToBottom,
    ADTransitionBottomToTop
} ADTransitionOrientation;


@interface ADTransition : NSObject {
    id <ADTransitionDelegate> _delegate;
    ADTransitionType _type;
    ADTransitionOrientation _orientation;
    CGFloat _duration;
}

@property (nonatomic, assign) id <ADTransitionDelegate> delegate;
@property (nonatomic, assign) ADTransitionType type;
@property (nonatomic, assign) ADTransitionOrientation orientation;
@property (nonatomic, assign) BOOL isReversed;
@property (nonatomic, readonly) NSTimeInterval duration; // abstract

+ (ADTransition *)nullTransition;
- (ADTransition *)reverseTransition;
- (NSArray *)getCircleApproximationTimingFunctions;
- (void)transitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewOlder;
-(void)prepareTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer;
-(void)finishedTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer;
-(void)startTransitionFromView:(UIView *)viewOut toView:(UIView *)viewIn inside:(UIView *)viewContainer;
@end
