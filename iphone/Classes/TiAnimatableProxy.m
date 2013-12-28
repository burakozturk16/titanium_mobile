#import "TiAnimatableProxy.h"

@interface TiAnimatableProxy()
{
    NSMutableArray* _pendingAnimations;
    NSMutableArray* _runningAnimations;
	pthread_rwlock_t runningLock;
	pthread_rwlock_t pendingLock;
}

@end

@implementation TiAnimatableProxy

-(id)init
{
    self = [super init];
    if (self != nil) {
		pthread_rwlock_init(&runningLock, NULL);
		pthread_rwlock_init(&pendingLock, NULL);
        _pendingAnimations = [[NSMutableArray alloc] init];
        _runningAnimations = [[NSMutableArray alloc] init];
    }
    return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(_pendingAnimations);
	RELEASE_TO_NIL(_runningAnimations);
	pthread_rwlock_destroy(&runningLock);
	pthread_rwlock_destroy(&pendingLock);
	[super dealloc];
}

-(BOOL)animating
{
    return ([_runningAnimations count] > 0);
}

-(void)addRunningAnimation:(TiAnimation *)animation
{
	pthread_rwlock_rdlock(&runningLock);
    [_runningAnimations addObject:animation];
	pthread_rwlock_unlock(&runningLock);
}
-(void)removeRunningAnimation:(TiAnimation *)animation
{
    [self forgetProxy:animation];
	pthread_rwlock_rdlock(&runningLock);
    [_runningAnimations removeObject:animation];
	pthread_rwlock_unlock(&runningLock);
}

-(void)cancelAllAnimations:(id)arg
{
    pthread_rwlock_rdlock(&pendingLock);
    [_pendingAnimations removeAllObjects];
    pthread_rwlock_unlock(&pendingLock);
	pthread_rwlock_rdlock(&runningLock);
    NSArray* running = [[NSArray alloc] initWithArray:_runningAnimations];
    [_runningAnimations removeAllObjects];
	pthread_rwlock_unlock(&runningLock);
    for (TiAnimation* animation in running) {
        [self removeRunningAnimation:animation];
        [animation cancel:nil];
    }
    [running release];
}

-(void)cancelAnimation:(TiAnimation *)animation shouldReset:(BOOL)reset
{
    if (reset) [self resetProxyPropertiesForAnimation:animation];
    [self animationDidComplete:animation];
}

-(HLSAnimation*)animationForAnimation:(TiAnimation*)animation
{
    return [HLSAnimation animationWithAnimationSteps:nil];
}


-(void)cancelAnimation:(TiAnimation *)animation
{
    [self cancelAnimation:animation shouldReset:YES];
}

-(void)animationWillStart:(TiAnimation *)animation
{
    
}

-(void)animationWillComplete:(TiAnimation *)animation
{
    
}

-(void)animationDidStart:(TiAnimation*)animation
{
}

-(void)animationDidComplete:(TiAnimation*)animation
{
    animation.animation = nil;
    [self removeRunningAnimation:animation];
}

-(void)handlePendingAnimation
{
    pthread_rwlock_rdlock(&pendingLock);
    
    if ([_pendingAnimations count] == 0) {
        pthread_rwlock_unlock(&pendingLock);
        return;
    }
    
    NSArray* pending = [[NSArray alloc] initWithArray:_pendingAnimations];
    [_pendingAnimations removeAllObjects];
	pthread_rwlock_unlock(&pendingLock);
    for (TiAnimation* anim in pending) {
        [self handlePendingAnimation:anim];
    }
    [pending release];
}

-(void)handlePendingAnimation:(TiAnimation*)pendingAnimation
{
	pthread_rwlock_rdlock(&pendingLock);
    [_pendingAnimations removeObject:pendingAnimation]; //make sure we dont handle this one anymore
	pthread_rwlock_unlock(&pendingLock);
    if (pendingAnimation.cancelRunningAnimations) {
        [self cancelAllAnimations:nil];
    }
    else {
        pthread_rwlock_rdlock(&runningLock);
        BOOL needsCancelling = [_runningAnimations containsObject:pendingAnimation];
        if (needsCancelling) {
            [_runningAnimations removeObject:pendingAnimation];
            [pendingAnimation cancelMyselfBeforeStarting];
        }
        pthread_rwlock_unlock(&runningLock);
    }
    
    [self addRunningAnimation:pendingAnimation];
    
    [self handleAnimation:pendingAnimation];
}

-(void)playAnimation:(HLSAnimation*)animation withRepeatCount:(NSUInteger)repeatCount afterDelay:(double)delay
{
    [animation playWithRepeatCount:repeatCount afterDelay:delay];
}

-(BOOL)handlesAutoReverse
{
    return FALSE;
}

-(void)handleAnimation:(TiAnimation*)animation
{
    [self handleAnimation:animation witDelegate:self];
}

-(void)handleAnimation:(TiAnimation*)animation witDelegate:(id)delegate
{
    animation.animatedProxy = self;
    animation.delegate = delegate;
    
    HLSAnimation* hlsAnimation  = [self animationForAnimation:animation];
    if (animation.autoreverse && ![self handlesAutoReverse]) {
        hlsAnimation = [hlsAnimation loopAnimation];
    }
    hlsAnimation.delegate = animation;
    hlsAnimation.lockingUI = NO;
    animation.animation = hlsAnimation;
    [self playAnimation:hlsAnimation withRepeatCount:[animation repeatCount] afterDelay:[animation delay]];
}

-(void)animate:(id)arg
{
	TiAnimation * newAnimation = [TiAnimation animationFromArg:arg context:[self executionContext] create:NO];
    if (newAnimation == nil) return;
    [self rememberProxy:newAnimation];
	pthread_rwlock_rdlock(&pendingLock);
    [_pendingAnimations addObject:newAnimation];
	pthread_rwlock_unlock(&pendingLock);
    [self handlePendingAnimation];
}

-(void)resetProxyPropertiesForAnimation:(TiAnimation*)animation
{
    NSMutableSet* props = [NSMutableSet setWithArray:(NSArray *)[animation allKeys]];
    id<NSFastEnumeration> keySeq = props;

    for (NSString* key in keySeq) {
        id value = [self valueForKey:key];
        [self setValue:value?value:[NSNull null] forKey:key];
    }
}

@end
