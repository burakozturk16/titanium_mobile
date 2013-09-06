//
//  TiShapeAnimation.h
//  Titanium
//
//  Created by Martin Guillon on 21/08/13.
//
//

#import "TiProxy.h"
#import "ListenerEntry.h"

@class TiShapeAnimation;
/**
 Protocol for animation delegate.
 */
@protocol TiAnimatableProxy

@required

-(void)handleAnimation:(TiShapeAnimation*)animation;

@optional
/**
 Tells the delegate that the animation will start.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationWillStart:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation did start.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationDidStart:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation will complete.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationWillComplete:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation did complete.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationDidComplete:(TiShapeAnimation *)animation;

@end

/**
 Protocol for animation delegate.
 */
@protocol TiShapeAnimationDelegate

@optional

/**
 Whether or not the animation should transition.
 
 The method is only called if the animation is a transition animation type.
 @param animation The animation this delegate is assigned to.
 @return _YES_ if the animation should transition, _NO_ otherwise.
 */
-(BOOL)animationShouldTransition:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation will start.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationWillStart:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation did start.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationDidStart:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation will complete.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationWillComplete:(TiShapeAnimation *)animation;

/**
 Tells the delegate that the animation did complete.
 @param animation The animation this delegate is assigned to.
 */
-(void)animationDidComplete:(TiShapeAnimation *)animation;

@end

@interface TiShapeAnimation : TiProxy
{
    TiProxy<TiAnimatableProxy>* _animatedProxy;
    
	NSObject<TiShapeAnimationDelegate> *delegate;
    
    // this is a temporary function passed in
	ListenerEntry *callback;
}
/**
 Provides access to animation delegate object.
 */
@property(nonatomic,assign) NSObject<TiShapeAnimationDelegate> *delegate;
@property(nonatomic,readonly) ListenerEntry* callback;
@property(nonatomic,retain) TiProxy<TiAnimatableProxy>* animatedProxy;

@property(nonatomic,assign) BOOL repeat;
@property(nonatomic,assign) BOOL autoreverse;
@property(nonatomic,assign) BOOL restartFromBeginning;
@property(nonatomic,assign) CGFloat delay;
@property(nonatomic,assign) CGFloat duration;

+(TiShapeAnimation*)animationFromArg:(id)args context:(id<TiEvaluator>)context create:(BOOL)yn;
-(id)initWithDictionary:(NSDictionary*)properties context:(id<TiEvaluator>)context;
-(id)initWithDictionary:(NSDictionary*)properties context:(id<TiEvaluator>)context callback:(KrollCallback*)callback;
-(void)simulateFinish:(TiProxy<TiAnimatableProxy>*)proxy;
-(void)setCallBack:(KrollCallback*)callback_ context:(id<TiEvaluator>)context_;

@end
