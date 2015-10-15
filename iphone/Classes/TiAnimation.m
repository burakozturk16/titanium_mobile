/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiAnimation.h"
#import "TiViewProxy.h"
#import "KrollCallback.h"
#import "TiHLSAnimation+Friend.h"
#import "CAMediaTimingFunction+AdditionalEquations.h"

#ifdef DEBUG 
	#define ANIMATION_DEBUG 0
#endif

@interface TiAnimation()
{
    CAMediaTimingFunction* _curve;
}

@end

@implementation TiAnimation
@synthesize callback, duration, reverseDuration, repeat, autoreverse, delay, restartFromBeginning, cancelRunningAnimations, dontApplyOnFinish;
@synthesize curve = _curve, reverseCurve = _reverseCurve;
@synthesize animation, animatedProxy;
@synthesize animated, transition, view;

static NSArray *animProps;

-(id)initWithDictionary:(NSDictionary*)properties context:(id<TiEvaluator>)context_ callback:(KrollCallback*)callback_
{
	if (self = [super _initWithPageContext:context_])
	{
        autoreverse = NO;
        repeat = [NSNumber numberWithInt:1];
        duration = 0;
        reverseDuration = 0;
        _curve = [[TiAnimation timingFunctionForCurve:kTiAnimCurveEaseInOut] retain];
        restartFromBeginning = NO;
        transition = UIViewAnimationTransitionNone;
        animated = NO;
        dontApplyOnFinish = NO;
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
	return [self initWithDictionary:properties context:context_ callback:nil];
}

-(void)dealloc
{
	RELEASE_TO_NIL(callback);
	RELEASE_TO_NIL(animatedProxy);
	RELEASE_TO_NIL(animation);
	RELEASE_TO_NIL(view);
	RELEASE_TO_NIL(_curve);
    self.delegate = nil;
	[super dealloc];
}

-(BOOL)shouldBeginFromCurrentState {
    return ![self valueForKey:@"from"] && (!restartFromBeginning && !cancelRunningAnimations);
}

-(void)setCallBack:(KrollCallback*)callback_ context:(id<TiEvaluator>)context_
{
    RELEASE_TO_NIL(callback);
    if (context_ != nil && callback_ != nil) {
        callback = [[ListenerEntry alloc] initWithListener:callback_ context:context_ proxy:self];
    }
}
+(TiAnimation*)animationFromArg:(id)args context:(id<TiEvaluator>)context create:(BOOL)yn
{
    id arg = nil;
	BOOL isArray = NO;
	
	if ([args isKindOfClass:[TiAnimation class]])
	{
		return args;
	}
	else if ([args isKindOfClass:[NSArray class]])
	{
        if ([args count] == 0) {
            return nil;
        }
		isArray = YES;
		arg = [args objectAtIndex:0];
		if ([arg isKindOfClass:[TiAnimation class]])
		{
            if ([args count] > 1) {
                KrollCallback *cb = [args objectAtIndex:1];
                ENSURE_TYPE(cb, KrollCallback);
                [(TiAnimation*)arg setCallBack:cb context:context];
            }
			return arg;
		}
	}
	else
	{
		arg = args;
	}
    
	if ([arg isKindOfClass:[NSDictionary class]] && [arg count] > 0)
	{
		NSDictionary *properties = arg;
		KrollCallback *cb = nil;
		
		if (isArray && [args count] > 1)
		{
			cb = [args objectAtIndex:1];
			ENSURE_TYPE(cb,KrollCallback);
		}
        
        BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
        if (!animated) return nil;
		
		return [[[TiAnimation alloc] initWithDictionary:properties context:context callback:cb] autorelease];
	}
	
	if (yn)
	{
		return [[[TiAnimation alloc] _initWithPageContext:context] autorelease];
	}
	return nil;
}

-(NSDictionary*)propertiesForAnimation:(TiHLSAnimation*)anim
{
    NSDictionary *properties = [self allProperties];
    if (anim.isReversed) {
        id<NSFastEnumeration> keys = [self allKeys];
        NSMutableDictionary* reverseProps = [[NSMutableDictionary alloc]initWithCapacity:[(NSArray*)keys count]];
        for (NSString* key in keys) {
            id value = [anim.animatedProxy valueForUndefinedKey:key];
            if (value) [reverseProps setObject:value forKey:key];
            else {
                [reverseProps setObject:[NSNull null] forKey:key];
            }
        }
        properties = [NSDictionary dictionaryWithDictionary:reverseProps];
        [reverseProps release];
    }
    return properties;
}

-(NSDictionary*)propertiesForAnimation:(TiAnimatableProxy*)animProxy destination:(BOOL)destination reverse:(BOOL)reversed
{
    if (destination) {
        NSDictionary* result = [self valueForUndefinedKey:@"to"];
        if (!result) {
            NSDictionary* from = [self valueForUndefinedKey:@"from"];
            if (from) {
                result = [[[NSMutableDictionary alloc]initWithCapacity:[from count]] autorelease];
                for (NSString* key in [from allKeys]) {
                    id value = [animProxy valueForUndefinedKey:key];
                    if (value) [(NSMutableDictionary*)result setObject:value forKey:key];
                    else {
                        [(NSMutableDictionary*)result setObject:[NSNull null] forKey:key];
                    }
                }
            } else {
                result = [self allProperties];
            }
        }
        return result;
    } else {
        NSDictionary* result = [self valueForUndefinedKey:@"from"];
        if (!result) {
            if (reversed) {
                id<NSFastEnumeration> keys = [self allKeys];
                NSMutableDictionary* reverseProps = [[NSMutableDictionary alloc] initWithCapacity:[(NSArray*)keys count]];
                for (NSString* key in keys) {
                    id value = [animProxy valueForUndefinedKey:key];
                    if (value) [reverseProps setObject:value forKey:key];
                    else {
                        [reverseProps setObject:[NSNull null] forKey:key];
                    }
                }
                result = [NSDictionary dictionaryWithDictionary:reverseProps];
                [reverseProps release];
            } else {
                result = [animProxy allProperties];
            }
        }
        return result;
    }
}


-(NSDictionary*)propertiesForAnimation:(TiHLSAnimation*)anim destination:(BOOL)destination
{
    return [self propertiesForAnimation:anim.animatedProxy destination:destination reverse:anim.isReversed];
}

-(NSDictionary*)fromPropertiesForAnimation:(TiHLSAnimation*)anim
{
    return [self propertiesForAnimation:anim destination:anim.isReversed];
}

-(NSDictionary*)toPropertiesForAnimation:(TiHLSAnimation*)anim
{
    return [self propertiesForAnimation:anim destination:!anim.isReversed];
}

-(NSDictionary*)fromPropertiesForAnimatableProxy:(TiAnimatableProxy*)animProxy
{
    return [self propertiesForAnimation:animProxy destination:false reverse:true];
}

-(NSDictionary*)toPropertiesForAnimatableProxy:(TiAnimatableProxy*)animProxy
{
    return [self propertiesForAnimation:animProxy destination:true reverse:false];
}

-(void)updateProxyProperties
{
    NSDictionary* props = [self toPropertiesForAnimatableProxy:animatedProxy];
    if (props) [animatedProxy applyProperties:props];
}


-(void)resetProxyProperties
{
    [animatedProxy resetProxyPropertiesForAnimation:self];
}

-(void)handleCompletedAnimation:(BOOL)finished
{
    if (!finished) return;
    if (!autoreverse)[self updateProxyProperties];
	
	// fire the event and call the callback
	if ([self _hasListeners:@"complete"])
	{
		[self fireEvent:@"complete" withObject:nil];
	}
	
	if (self.callback!=nil && [self.callback context]!=nil)
	{
		[self _fireEventToListener:@"animated" withObject:self listener:[self.callback listener] thisObject:nil];
	}
    RELEASE_TO_NIL(animatedProxy);
}


-(void)simulateFinish:(TiAnimatableProxy*)proxy
{
    self.animatedProxy = proxy;
    [self handleCompletedAnimation:!autoreverse];
}

-(CGFloat) getDuration {
    return duration/1000;
}

-(CGFloat) getReverseDuration {
    return reverseDuration/1000;
}

-(CGFloat) delay {
    return delay/1000;
}

-(NSUInteger) repeatCount {
    if ([repeat doubleValue] != HUGE_VALF) {
        return [repeat intValue];
    }
    else {
        return NSUIntegerMax;
    }
}

-(BOOL)isTransitionAnimation
{
    if (transition!=0 && transition!=UIViewAnimationTransitionNone)
    {
        return YES;
    }
	return NO;
}

-(NSTimeInterval)getAnimationDuration
{
    if (self.duration!=0)
	{
		return [self getDuration];
	}
    return ([self isTransitionAnimation]) ? 1 : 0.2;
}

-(NSTimeInterval)getAnimationReverseDuration
{
    if (self.reverseDuration!=0)
    {
        return [self getReverseDuration];
    }
    return [self getAnimationDuration];
}

+(CAMediaTimingFunction*) timingFunctionForCurve:(int)curve_
{
    switch (curve_) {
        case kTiAnimCurveEaseInOut: return [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseInEaseOut];
        case kTiAnimCurveEaseIn: return [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseIn];
        case kTiAnimCurveEaseOut: return [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionEaseOut];
        case kTiAnimCurveLinear: return [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionLinear];
        case kTiAnimCurveEaseOutCirc: return [CAMediaTimingFunction easeOutCirc];
        case kTiAnimCurveEaseInOutCirc: return [CAMediaTimingFunction easeInOutCirc];
        case kTiAnimCurveEaseInCubic: return [CAMediaTimingFunction easeInCubic];
        case kTiAnimCurveEaseOutCubic: return [CAMediaTimingFunction easeOutCubic];
        case kTiAnimCurveEaseInOutCubic: return [CAMediaTimingFunction easeInOutCubic];
        case kTiAnimCurveEaseInExpo: return [CAMediaTimingFunction easeInExpo];
        case kTiAnimCurveEaseOutExpo: return [CAMediaTimingFunction easeOutExpo];
        case kTiAnimCurveEaseInOutExpo: return [CAMediaTimingFunction easeInOutExpo];
        case kTiAnimCurveEaseInQuad: return [CAMediaTimingFunction easeInQuad];
        case kTiAnimCurveEaseOutQuad: return [CAMediaTimingFunction easeOutQuad];
        case kTiAnimCurveEaseInOutQuad: return [CAMediaTimingFunction easeInOutQuad];
        case kTiAnimCurveEaseInQuart: return [CAMediaTimingFunction easeInQuart];
        case kTiAnimCurveEaseOutQuart: return [CAMediaTimingFunction easeOutQuart];
        case kTiAnimCurveEaseInOutQuart: return [CAMediaTimingFunction easeInOutQuart];
        case kTiAnimCurveEaseInQuint: return [CAMediaTimingFunction easeInQuint];
        case kTiAnimCurveEaseOutQuint: return [CAMediaTimingFunction easeOutQuint];
        case kTiAnimCurveEaseInOutQuint: return [CAMediaTimingFunction easeInOutQuint];
        case kTiAnimCurveEaseInSine: return [CAMediaTimingFunction easeInSine];
        case kTiAnimCurveEaseOutSine: return [CAMediaTimingFunction easeOutSine];
        case kTiAnimCurveEaseInOutSine: return [CAMediaTimingFunction easeInOutSine];
        case kTiAnimCurveEaseInBack: return [CAMediaTimingFunction easeInBack];
        case kTiAnimCurveEaseOutBack: return [CAMediaTimingFunction easeOutBack];
        case kTiAnimCurveEaseInOutBack: return [CAMediaTimingFunction easeInOutBack];
        default: return [CAMediaTimingFunction functionWithName:kCAMediaTimingFunctionDefault];
    }
}

+(CAMediaTimingFunction*)reverseCurve:(CAMediaTimingFunction*)curve_
{
    float coords1[2];
    float coords2[2];
    [curve_ getControlPointAtIndex:1 values:coords1];
    [curve_ getControlPointAtIndex:2 values:coords2];
    CAMediaTimingFunction* function = [CAMediaTimingFunction functionWithControlPoints:coords2[0] :coords1[1] :coords1[0] :coords2[1]];
    return function;
}

+ (CAMediaTimingFunction *)inverseFunction:(CAMediaTimingFunction*)function
{
    float values1[2];
    memset(values1, 0, sizeof(values1));
    [function getControlPointAtIndex:1 values:values1];
    
    float values2[2];
    memset(values2, 0, sizeof(values2));
    [function getControlPointAtIndex:2 values:values2];
    
    // Flip the original curve around the y = 1 - x axis
    // Refer to the "Introduction to Animation Types and Timing Programming Guide"
    return [CAMediaTimingFunction functionWithControlPoints:1.f - values2[0] :1.f - values2[1] :1.f - values1[0] :1.f - values1[1]];
}

-(void)cancelMyselfBeforeStarting
{
    TiAnimatableProxy* proxy = [animatedProxy retain];
    TiThreadPerformOnMainThread(^{
        //we use terminate when we canceling ourself
        [animation terminate];
    }, YES);
    if (proxy != nil) {
        [proxy cancelAnimation:self shouldReset:self.restartFromBeginning];
	}
	[proxy release];
}

-(void)cancel:(id)args
{
    [self cancelWithReset:YES];
}

-(void)cancelWithReset:(BOOL)reset
{
    TiAnimatableProxy* proxy = [animatedProxy retain];
    if (proxy != nil) {
        //animation will actually be cancelled in in animationDidComplete
        //we need to do this to make sure things are done in order
        [proxy cancelAnimation:self shouldReset:reset];
	}
	[proxy release];
}


-(void)animate:(id)args
{
	UIView *theview = nil;
	
	if ([args isKindOfClass:[NSArray class]])
	{
		//
		// this is something like:
		//
		// animation.animate(view)
		//
		// vs.
		//
		// view.animate(animation)
		//
		// which is totally fine, just hand it to the view and let him callback
		//
		id proxy = [args objectAtIndex:0];
		ENSURE_TYPE(proxy,TiAnimatableProxy);
		[(TiAnimatableProxy*)theview animate:[NSArray arrayWithObject:self]];
		return;
	}
	else if ([args isKindOfClass:[TiAnimatableProxy class]])
	{
		// called by the view to cause himself to be animated
		[(TiAnimatableProxy*)args animate:[NSArray arrayWithObject:self]];
	}
}

-(void)setCurve:(id)value
{
    RELEASE_TO_NIL(_curve);
    if ([value isKindOfClass:[NSNumber class]])
    {
        _curve = [[TiAnimation timingFunctionForCurve:[value intValue]] retain];
    }
    else if ([value isKindOfClass:[NSArray class]])
    {
        NSArray* array = (NSArray*)value;
        NSUInteger count = [array count];
        if (count == 4)
        {
            _curve = [[CAMediaTimingFunction functionWithControlPoints: [[array objectAtIndex:0] doubleValue] : [[array objectAtIndex:1] doubleValue] : [[array objectAtIndex:2] doubleValue] : [[array objectAtIndex:3] doubleValue]] retain];
        }
    }
    [self replaceValue:value forKey:@"curve" notification:NO];
}

-(void)setReverseCurve:(id)value
{
    RELEASE_TO_NIL(_curve);
    if ([value isKindOfClass:[NSNumber class]])
    {
        _reverseCurve = [[TiAnimation timingFunctionForCurve:[value intValue]] retain];
    }
    else if ([value isKindOfClass:[NSArray class]])
    {
        NSArray* array = (NSArray*)value;
        NSUInteger count = [array count];
        if (count == 4)
        {
            _reverseCurve = [[CAMediaTimingFunction functionWithControlPoints: [[array objectAtIndex:0] doubleValue] : [[array objectAtIndex:1] doubleValue] : [[array objectAtIndex:2] doubleValue] : [[array objectAtIndex:3] doubleValue]] retain];
        }
    }
    [self replaceValue:value forKey:@"reverseCurve" notification:NO];
}

#pragma mark -
#pragma mark HLSAnimationDelegate

/**
 * Called right before the first animation step is executed, but after any delay which might have been set
 */
- (void)animationWillStart:(HLSAnimation *)animation animated:(BOOL)animated_
{
    if (self.delegate!=nil && [self.delegate respondsToSelector:@selector(animationWillStart:)])
	{
		[self.delegate animationWillStart:self];
	}
}

/**
 * Called right after the last animation step has been executed. You can check -terminating or -cancelling
 * to find if the animation ended normally
 */
- (void)animationDidStop:(HLSAnimation *)animation_ animated:(BOOL)animated_
{
    if (self.delegate!=nil && [self.delegate respondsToSelector:@selector(animationDidComplete:)])
	{
		[self.delegate animationDidComplete:self];
	}
    if (!dontApplyOnFinish) {
        [self handleCompletedAnimation:animated_];
    }
}

/**
 * Called when a step has been executed. Since animation steps are deeply copied when assigned to an animation,
 * you must not use animation step pointers to identify animation steps when implementing this method. Use
 * animation step tags instead
 */
- (void)animation:(HLSAnimation *)animation didFinishStep:(HLSAnimationStep *)animationStep animated:(BOOL)animated_
{
    if (!autoreverse && [repeat integerValue] > 1) {
        [self resetProxyProperties];
    }
}

@end
