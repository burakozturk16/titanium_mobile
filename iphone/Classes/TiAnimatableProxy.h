#import "TiProxy.h"
#import "TiAnimation.h"

@interface TiAnimatableProxy : TiProxy<TiAnimationDelegate>
@property(nonatomic,assign) BOOL animating;

-(void)animate:(id)arg;
-(void)cancelAllAnimations:(id)arg;

-(HLSAnimation*)animationForAnimation:(TiAnimation*)animation;

-(void)handleAnimation:(TiAnimation*)animation;
-(void)handleAnimation:(TiAnimation*)animation witDelegate:(id)delegate;
-(void)handlePendingAnimation:(TiAnimation*)pendingAnimation;
-(void)handlePendingAnimation;
-(void)handleAnimation:(TiAnimation*)animation;
-(void)cancelAnimation:(TiAnimation *)animation;
-(void)cancelAnimation:(TiAnimation *)animation shouldReset:(BOOL)reset;

-(void)addRunningAnimation:(TiAnimation *)animation;
-(void)removeRunningAnimation:(TiAnimation *)animation;

-(BOOL)animating;
-(void)playAnimation:(HLSAnimation*)animation withRepeatCount:(NSUInteger)repeatCount afterDelay:(double)delay;

#pragma Public API
-(void)cancelAllAnimations:(id)arg;
-(void)resetProxyPropertiesForAnimation:(TiAnimation*)animation;
-(void)clearAnimations;
-(void)removePendingAnimation:(TiAnimation *)animation;

@end
