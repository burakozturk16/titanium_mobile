//
//  TiShapeAnimation.m
//  Titanium
//
//  Created by Martin Guillon on 21/08/13.
//
//

#import "TiShapeAnimation.h"
#import "ShapeProxy.h"

#ifdef DEBUG
#define ANIMATION_DEBUG 0
#endif

@implementation TiShapeAnimation
@synthesize animatedProxy = _animatedProxy;
@synthesize delegate;
@synthesize duration, repeat, autoreverse, delay, restartFromBeginning;
@synthesize callback;

-(id)initWithDictionary:(NSDictionary*)properties context:(id<TiEvaluator>)context_ callback:(KrollCallback*)callback_
{
	if (self = [super _initWithPageContext:context_])
	{
        autoreverse = NO;
        restartFromBeginning = YES;
        delay = 0;
        repeat = 1;
        duration = 0;
        [super _initWithProperties:properties];
         if (context_!=nil)
         {
             [self setCallBack:callback_ context:context_];
         }
    }
    return self;
}

-(id)initWithDictionary:(NSDictionary*)properties context:(id<TiEvaluator>)context_
{
	if (self = [self initWithDictionary:properties context:context_ callback:nil])
	{
	}
	return self;
}


-(void)dealloc
{
	RELEASE_TO_NIL(callback);
	RELEASE_TO_NIL(_animatedProxy);
	RELEASE_TO_NIL(delegate);
	[super dealloc];
}

-(void)setCallBack:(KrollCallback*)callback_ context:(id<TiEvaluator>)context_
{
    RELEASE_TO_NIL(callback);
    if (context_ != nil) {
        callback = [[ListenerEntry alloc] initWithListener:callback_ context:context_ proxy:self];
    }
}

+(TiShapeAnimation*)animationFromArg:(id)args context:(id<TiEvaluator>)context create:(BOOL)yn
{
	id arg = nil;
	BOOL isArray = NO;
	
	if ([args isKindOfClass:[TiShapeAnimation class]])
	{
		return (TiShapeAnimation*)args;
	}
	else if ([args isKindOfClass:[NSArray class]])
	{
		isArray = YES;
		arg = [args objectAtIndex:0];
		if ([arg isKindOfClass:[TiShapeAnimation class]])
		{
            if ([args count] > 1) {
                KrollCallback *cb = [args objectAtIndex:1];
                ENSURE_TYPE(cb, KrollCallback);
                [(TiShapeAnimation*)arg setCallBack:cb context:context];
            }
			return (TiShapeAnimation*)arg;
		}
	}
	else
	{
		arg = args;
	}
    
	if ([arg isKindOfClass:[NSDictionary class]])
	{
		NSDictionary *properties = arg;
		KrollCallback *cb = nil;
		
		if (isArray && [args count] > 1)
		{
			cb = [args objectAtIndex:1];
			ENSURE_TYPE(cb,KrollCallback);
		}
		
		return [[[TiShapeAnimation alloc] initWithDictionary:properties context:context callback:cb] autorelease];
	}
	
	if (yn)
	{
		return [[[TiShapeAnimation alloc] _initWithPageContext:context] autorelease];
	}
	return nil;
}

-(void)updateProxyProperties
{
    NSMutableDictionary* props = [NSMutableDictionary dictionaryWithDictionary:[self allProperties]];
    [_animatedProxy applyProperties:props];
}

-(void)handleCompletedAnimation:(TiShapeAnimation*)animation withFinished:(BOOL)finished
{
    if (_animatedProxy == nil || !finished) return;
    
	if (animation.delegate!=nil && [animation.delegate respondsToSelector:@selector(animationWillComplete:)])
	{
		[animation.delegate animationWillComplete:self];
	}
    
    if (!autoreverse)[self updateProxyProperties];
	
	// fire the event and call the callback
	if ([animation _hasListeners:@"complete"])
	{
		[animation fireEvent:@"complete" withObject:nil];
	}
	
	if (animation.callback!=nil && [animation.callback context]!=nil)
	{
		[animation _fireEventToListener:@"animated" withObject:animation listener:[animation.callback listener] thisObject:nil];
	}
	
	// tell our view that we're done
	if ([_animatedProxy respondsToSelector:@selector(animationDidComplete:)]) {
		[_animatedProxy animationDidComplete:self];
	}
	
	if (animation.delegate!=nil && [animation.delegate respondsToSelector:@selector(animationDidComplete:)])
	{
		[animation.delegate animationDidComplete:animation];
	}
    RELEASE_TO_NIL(_animatedProxy);
}


-(void)onAnimationCompleted:(NSString *)animationID finished:(NSNumber *)finished animation:(TiShapeAnimation *)animation_
{
#if ANIMATION_DEBUG==1
	NSLog(@"[DEBUG] ANIMATION: COMPLETED %@, %@",self,(id)proxy_);
#endif
	
    [self handleCompletedAnimation:animation_ withFinished:[finished boolValue]];
	
}

- (void)animationDidStop:(CAAnimation *)anim finished:(BOOL)flag
{
    [self handleCompletedAnimation:self withFinished:flag];
//    [self animationCompleted:@"" finished:[NSNumber numberWithBool:flag] context:anim.delegate];
}

-(void)simulateFinish:(TiProxy<TiAnimatableProxy>*)proxy
{
    
    [self handleCompletedAnimation:self withFinished:!autoreverse];
}

@end
