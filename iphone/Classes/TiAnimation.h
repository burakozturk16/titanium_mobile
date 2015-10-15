/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"
#import "TiPoint.h"
#import "TiColor.h"
#import "ListenerEntry.h"
#import "LayoutConstraint.h"

#import "HLSAnimation.h"

enum TiAnimCurve
{
	kTiAnimCurveEaseInOut,
	kTiAnimCurveEaseIn,
	kTiAnimCurveEaseOut,
	kTiAnimCurveLinear,
	kTiAnimCurveEaseInCirc,
    kTiAnimCurveEaseOutCirc,
    kTiAnimCurveEaseInOutCirc,
    kTiAnimCurveEaseInCubic,
    kTiAnimCurveEaseOutCubic,
    kTiAnimCurveEaseInOutCubic,
    kTiAnimCurveEaseInExpo,
    kTiAnimCurveEaseOutExpo,
    kTiAnimCurveEaseInOutExpo,
    kTiAnimCurveEaseInQuad,
    kTiAnimCurveEaseOutQuad,
    kTiAnimCurveEaseInOutQuad,
    kTiAnimCurveEaseInQuart,
    kTiAnimCurveEaseOutQuart,
    kTiAnimCurveEaseInOutQuart,
    kTiAnimCurveEaseInQuint,
    kTiAnimCurveEaseOutQuint,
    kTiAnimCurveEaseInOutQuint,
    kTiAnimCurveEaseInSine,
    kTiAnimCurveEaseOutSine,
    kTiAnimCurveEaseInOutSine,
    kTiAnimCurveEaseInBack,
    kTiAnimCurveEaseOutBack,
    kTiAnimCurveEaseInOutBack
};


@class TiAnimation;
/**
 Protocol for animation delegate.
 */
@protocol TiAnimationDelegate

@optional

/**
 Whether or not the animation should transition.
 
// The method is only called if the animation is a transition animation type.
// @param animation The animation this delegate is assigned to.
// @return _YES_ if the animation should transition, _NO_ otherwise.
// */
//-(BOOL)animationShouldTransition:(TiAnimation *)animation;

/**
 Tells the delegate that the animation will start.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationWillStart:(TiAnimation *)animation;

/**
 Tells the delegate that the animation did start.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationDidStart:(TiAnimation *)animation;

/**
 Tells the delegate that the animation will complete.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationWillComplete:(TiAnimation *)animation;

/**
 Tells the delegate that the animation did complete.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationDidComplete:(TiAnimation *)animation;

@end


@class TiAnimatableProxy, TiHLSAnimation, HLSAnimation;
/**
 A type of proxy representing an animation to apply to a view. 
 */
@interface TiAnimation : TiProxy<HLSAnimationDelegate> {
@protected
	ListenerEntry *callback;
}

/**
 Provides access to animation delegate object.
 */
@property(nonatomic,assign) NSObject<TiAnimationDelegate> *delegate;

@property(nonatomic,readonly) ListenerEntry* callback;

// properties that control the animation 
@property(nonatomic,retain) CAMediaTimingFunction* curve;
@property(nonatomic,retain) CAMediaTimingFunction* reverseCurve;
@property(nonatomic,retain) NSNumber* repeat;
@property(nonatomic,assign) BOOL autoreverse;
@property(nonatomic,assign) BOOL restartFromBeginning;
@property(nonatomic,assign) BOOL cancelRunningAnimations;
@property(nonatomic,assign) BOOL dontApplyOnFinish;
@property(nonatomic,readonly) BOOL shouldBeginFromCurrentState;
@property(nonatomic,assign) CGFloat delay;
@property(nonatomic,assign) CGFloat duration;
@property(nonatomic,assign) CGFloat reverseDuration;
@property(nonatomic,retain) TiAnimatableProxy	*animatedProxy;
@property(nonatomic,retain) HLSAnimation	*animation;


//transition properties (to be removed)
@property(nonatomic,assign) BOOL animated;
@property(nonatomic,assign) int transition;
@property(nonatomic,retain) TiViewProxy	*view;

-(void)cancel:(id)args;

+(TiAnimation*)animationFromArg:(id)args context:(id<TiEvaluator>)context create:(BOOL)yn;
-(id)initWithDictionary:(NSDictionary*)properties context:(id<TiEvaluator>)context_ callback:(KrollCallback*)callback_;
-(BOOL)isTransitionAnimation;
-(NSTimeInterval)getAnimationDuration;
-(NSTimeInterval)getAnimationReverseDuration;
-(NSUInteger) repeatCount;
+(CAMediaTimingFunction*) timingFunctionForCurve:(int)curve_;
+(CAMediaTimingFunction*)reverseCurve:(CAMediaTimingFunction*)curve_;
+ (CAMediaTimingFunction *)inverseFunction:(CAMediaTimingFunction*)function;
-(NSDictionary*)fromPropertiesForAnimation:(TiHLSAnimation*)anim;
-(NSDictionary*)toPropertiesForAnimation:(TiHLSAnimation*)anim;
-(NSDictionary*)toPropertiesForAnimatableProxy:(TiAnimatableProxy*)animProxy;
-(NSDictionary*)fromPropertiesForAnimatableProxy:(TiAnimatableProxy*)animProxy;
-(void)cancelMyselfBeforeStarting;
-(void)simulateFinish:(TiAnimatableProxy*)proxy;
-(void)cancelWithReset:(BOOL)reset;

@end
