/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import <objc/runtime.h>

#import "TiProxy.h"
#import "TiHost.h"
#import "KrollCallback.h"
#import "KrollContext.h"
#import "KrollBridge.h"
#import "TiModule.h"
#import "ListenerEntry.h"
#import "TiComplexValue.h"
#import "TiViewProxy.h"
#import "TiBindingEvent.h"

#include <libkern/OSAtomic.h>

//Common exceptions to throw when the function call was improper
NSString * const TiExceptionInvalidType = @"Invalid type passed to function";
NSString * const TiExceptionNotEnoughArguments = @"Invalid number of arguments to function";
NSString * const TiExceptionRangeError = @"Value passed to function exceeds allowed range";


NSString * const TiExceptionOSError = @"The iOS reported an error";


//Should be rare, but also useful if arguments are used improperly.
NSString * const TiExceptionInternalInconsistency = @"Value was not the value expected";

//Rare exceptions to indicate a bug in the titanium code (Eg, method that a subclass should have implemented)
NSString * const TiExceptionUnimplementedFunction = @"Subclass did not implement required method";

NSString * const TiExceptionMemoryFailure = @"Memory allocation failed";


NSString * SetterStringForKrollProperty(NSString * key)
{
    return [NSString stringWithFormat:@"set%@%@_:", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
}

SEL SetterForKrollProperty(NSString * key)
{
    NSString *method = SetterStringForKrollProperty(key);
    return NSSelectorFromString(method);
}

SEL SetterWithObjectForKrollProperty(NSString * key)
{
    NSString *method = [SetterStringForKrollProperty(key) stringByAppendingString:@"withObject:"];
    return NSSelectorFromString(method);
}

void DoProxyDelegateChangedValuesWithProxy(id<TiProxyDelegate> target, NSString * key, id oldValue, id newValue, TiProxy * proxy)
{
    // default implementation will simply invoke the setter property for this object
    // on the main UI thread
    
    // first check to see if the property is defined by a <key>:withObject: signature
    SEL sel = SetterWithObjectForKrollProperty(key);
    if ([target respondsToSelector:sel])
    {
        id firstarg = newValue;
        id secondarg = nil;
        
        if ([firstarg isKindOfClass:[TiComplexValue class]])
        {
            firstarg = [(TiComplexValue*)newValue value];
            secondarg = [(TiComplexValue*)newValue properties];
        }
        TiThreadPerformBlockOnMainThread(^{
            [target performSelector:sel withObject:firstarg withObject:secondarg];
        }, YES);
        //		if ([NSThread isMainThread])
        //		{
        //			[target performSelector:sel withObject:firstarg withObject:secondarg];
        //		}
        //		else
        //		{
        //			if (![key hasPrefix:@"set"])
        //			{
        //				key = [NSString stringWithFormat:@"set%@%@_", [[key substringToIndex:1] uppercaseString], [key substringFromIndex:1]];
        //			}
        //			NSArray *arg = [NSArray arrayWithObjects:key,firstarg,secondarg,target,nil];
        //			TiThreadPerformOnMainThread(^{[proxy _dispatchWithObjectOnUIThread:arg];}, YES);
        //		}
        return;
    }
    
    sel = SetterForKrollProperty(key);
    if ([target respondsToSelector:sel])
    {
        TiThreadPerformBlockOnMainThread(^{
            [target performSelector:sel withObject:newValue];
        }, YES);
    }
}

void DoProxyDispatchToSecondaryArg(id<TiProxyDelegate> target, SEL sel, NSString *key, id newValue, TiProxy * proxy)
{
    id firstarg = newValue;
    id secondarg = nil;
    
    if ([firstarg isKindOfClass:[TiComplexValue class]])
    {
        firstarg = [(TiComplexValue*)newValue value];
        secondarg = [(TiComplexValue*)newValue properties];
    }
    
    if ([NSThread isMainThread])
    {
        [target performSelector:sel withObject:firstarg withObject:secondarg];
    }
    else
    {
        NSArray *arg = [NSArray arrayWithObjects:NSStringFromSelector(sel),firstarg,secondarg,target,nil];
        TiThreadPerformOnMainThread(^{[proxy _dispatchWithObjectOnUIThread:arg];}, YES);
    }
}

void DoProxyDelegateReadKeyFromProxy(id<TiProxyDelegate> target, NSString *key, TiProxy * proxy, NSNull * nullValue, BOOL useThisThread)
{
    // use valueForUndefined since this should really come from dynprops
    // not against the real implementation
    id value = [proxy valueForUndefinedKey:key];
    if (value == nil)
    {
        return;
    }
    if (value == nullValue)
    {
        value = nil;
    }
    NSString* method = SetterStringForKrollProperty(key);
    SEL sel = NSSelectorFromString([method stringByAppendingString:@"withObject:"]);
    if ([target respondsToSelector:sel])
    {
        DoProxyDispatchToSecondaryArg(target,sel,key,value,proxy);
        return;
    }
    sel = NSSelectorFromString(method);
    if (![target respondsToSelector:sel])
    {
        return;
    }
    if (useThisThread)
    {
        [target performSelector:sel withObject:value];
    }
    else
    {
        TiThreadPerformOnMainThread(^{[target performSelector:sel withObject:value];}, YES);
    }
}

void DoProxyDelegateReadValuesWithKeysFromProxy(id<TiProxyDelegate> target, id<NSFastEnumeration> keys, TiProxy * proxy)
{
    BOOL isMainThread = [NSThread isMainThread];
    NSNull * nullObject = [NSNull null];
    BOOL viewAttached = YES;
    
    NSArray * keySequence = [proxy keySequence];
    
    // assume if we don't have a view that we can send on the
    // main thread to the proxy
    if ([target isKindOfClass:[TiViewProxy class]])
    {
        viewAttached = [(TiViewProxy*)target viewAttached];
    }
    
    BOOL useThisThread = isMainThread==YES || viewAttached==NO;
    
    for (NSString * thisKey in keySequence)
    {
        DoProxyDelegateReadKeyFromProxy(target, thisKey, proxy, nullObject, useThisThread);
    }
    
    
    for (NSString * thisKey in keys)
    {
        if ([keySequence containsObject:thisKey])
        {
            continue;
        }
        DoProxyDelegateReadKeyFromProxy(target, thisKey, proxy, nullObject, useThisThread);
    }
}

typedef struct {
    Class class;
    SEL selector;
} TiClassSelectorPair;

void TiClassSelectorFunction(TiBindingRunLoop runloop, void * payload)
{
    TiClassSelectorPair * pair = payload;
    [(Class)(pair->class) performSelector:(SEL)(pair->selector) withObject:runloop];
}

@implementation TiProxy
{
    BOOL _createdFromDictionary;
    BOOL _aboutToBeBridge;
}

+(void)performSelectorDuringRunLoopStart:(SEL)selector
{
    TiClassSelectorPair * pair = malloc(sizeof(TiClassSelectorPair));
    pair->class = [self class];
    pair->selector = selector;
    TiBindingRunLoopCallOnStart(TiClassSelectorFunction,pair);
}

@synthesize pageContext, executionContext;
@synthesize modelDelegate;
@synthesize eventOverrideDelegate = eventOverrideDelegate;
@synthesize createdFromDictionary = _createdFromDictionary;
@synthesize fakeApplyProperties = _fakeApplyProperties;

#pragma mark Private

-(id)init
{
    if (self = [super init])
    {
        _aboutToBeBridge = YES;
        _bubbleParent = YES;
        _bubbleParentDefined = NO;
        _shouldRetainModelDelegate = YES;
        _createdFromDictionary = NO;
        _fakeApplyProperties = NO;
#if PROXY_MEMORY_TRACK == 1
        NSLog(@"[DEBUG] INIT: %@ (%d)",self,[self hash]);
#endif
        pthread_rwlock_init(&listenerLock, NULL);
        pthread_rwlock_init(&dynpropsLock, NULL);
    }
    return self;
}

-(void)internalSetCreatedFromDictionary
{
    _createdFromDictionary = YES;
}

-(void)initializeProperty:(NSString*)name defaultValue:(id)value
{
    pthread_rwlock_wrlock(&dynpropsLock);
    if (dynprops == nil) {
        dynprops = [[NSMutableDictionary alloc] init];
    }
    if ([dynprops valueForKey:name] == nil) {
        [dynprops setValue:((value == nil) ? [NSNull null] : value) forKey:name];
    }
    pthread_rwlock_unlock(&dynpropsLock);
}

-(void)initializeProperties:(NSDictionary*)defaultValues
{
    pthread_rwlock_wrlock(&dynpropsLock);
    if (dynprops == nil) {
        dynprops = [[NSMutableDictionary alloc] init];
    }
    [defaultValues enumerateKeysAndObjectsUsingBlock:^(id key, id obj, BOOL *stop) {
        if ([dynprops valueForKey:key] == nil) {
            [dynprops setValue:obj forKey:key];
        }
    }];
    pthread_rwlock_unlock(&dynpropsLock);
}

+(BOOL)shouldRegisterOnInit
{
    return YES;
}

-(id)_initWithPageContext:(id<TiEvaluator>)context
{
    if (self = [self init])
    {
        pageContext = (id)context; // do not retain
        executionContext = context; //To ensure there is an execution context during _configure.
        if([[self class] shouldRegisterOnInit]) // && ![NSThread isMainThread])
        {
            [context.krollContext invokeBlockOnThread:^{
                [pageContext registerProxy:self];
            }];
            // allow subclasses to configure themselves
        }
        [self _configure];
        executionContext = nil;
    }
    return self;
}

-(void)setModelDelegate:(id <TiProxyDelegate>)md
{
    // TODO; revisit modelDelegate/TiProxy typing issue
    if (_shouldRetainModelDelegate && (void*)modelDelegate != self) {
        RELEASE_TO_NIL(modelDelegate);
    }
    
    if (_shouldRetainModelDelegate && (void*)md != self) {
        modelDelegate = [md retain];
    }
    else {
        modelDelegate = md;
    }
}

/*
 *	Currently, Binding/unbinding bridges does nearly nothing but atomically
 *	increment or decrement. In the future, error checking could be done, or
 *	when unbinding from the pageContext, to clear it. This might also be a
 *	replacement for contextShutdown and friends, as contextShutdown is
 *	called ONLY when a proxy is still registered with a context as it
 *	is shutting down.
 */

-(void)boundBridge:(id<TiEvaluator>)newBridge withKrollObject:(KrollObject *)newKrollObject
{
    OSAtomicIncrement32(&bridgeCount);
    if (newBridge == pageContext) {
        pageKrollObject = newKrollObject;
    }
    _aboutToBeBridge = NO;
}

-(void)unboundBridge:(id<TiEvaluator>)oldBridge
{
    if(OSAtomicDecrement32(&bridgeCount)<0)
    {
        DeveloperLog(@"[WARN] BridgeCount for %@ is now at %d",self,bridgeCount);
    }
    if(oldBridge == pageContext) {
        pageKrollObject = nil;
    }
}

-(void)contextWasShutdown:(id<TiEvaluator>)context
{
}

-(void)contextShutdown:(id)sender
{
    id<TiEvaluator> context = (id<TiEvaluator>)sender;
    
    [self contextWasShutdown:context];
    if(pageContext == context){
        //TODO: Should we really stay bug compatible with the old behavior?
        //I think we should instead have it that the proxy stays around until
        //it's no longer referenced by any contexts at all.
        [self _destroy];
        pageContext = nil;
        pageKrollObject = nil;
    }
}

-(void)invokeBlockOnJSThread:(void (^)())block {
    if ([self getContext] != nil)
    {
        [[self getContext].krollContext invokeBlockOnThread:block];
    }
}

-(void)setExecutionContext:(id<TiEvaluator>)context
{
    // the execution context is different than the page context
    //
    // the page context is the owning context that created (and thus owns) the proxy
    //
    // the execution context is the context which is executing against the context when
    // this proxy is being touched.  since objects can be referenced from one context
    // in another, the execution context should be used to resolve certain things like
    // paths, etc. so that the proper context can be contextualized which is different
    // than the owning context (page context).
    //
    
    /*
     *	In theory, if two contexts are both using the proxy at the same time,
     *	bad things could happen since this value will be overwritten.
     *	TODO: Investigate thread safety of this, or to moot it.
     */
    
    executionContext = context; //don't retain
}

-(void)_initWithProperties:(NSDictionary*)properties
{
    [self setValuesForKeysWithDictionary:properties];
}

-(void)_initWithCallback:(KrollCallback*)callback
{
}

-(void)_configure
{
    // for subclasses
}


-(id)_initWithPageContext:(id<TiEvaluator>)context_ args:(NSArray*)args withPropertiesInit:(BOOL)init
{
    if (self = [self _initWithPageContext:context_])
    {
        // If we are being created with a page context, assume that this is also
        // the execution context during creation so that recursively-made
        // proxies have the same page context.
        executionContext = context_;
        id a = nil;
        NSUInteger count = [args count];
        
        if (count > 0 && [[args objectAtIndex:0] isKindOfClass:[NSDictionary class]])
        {
            a = [args objectAtIndex:0];
        }
        
        if (count > 1 && [[args objectAtIndex:1] isKindOfClass:[KrollCallback class]])
        {
            [self _initWithCallback:[args objectAtIndex:1]];
        }
        if (init)
            [self _initWithProperties:a];
        executionContext = nil;
    }
    return self;
}

-(id)_initWithPageContext:(id<TiEvaluator>)context_ args:(NSArray*)args
{
    return [self _initWithPageContext:context_ args:args withPropertiesInit:YES];
}

-(void)_destroy
{
    if (destroyed)
    {
        return;
    }
    
    destroyed = YES;
    
#if PROXY_MEMORY_TRACK == 1
    NSLog(@"[DEBUG] DESTROY: %@ (%d)",self,[self hash]);
#endif
    
    if ((bridgeCount == 1) && (pageKrollObject != nil) && (pageContext != nil))
    {
        [pageContext unregisterProxy:self];
    }
    else if (bridgeCount > 1)
    {
        NSArray * pageContexts = [KrollBridge krollBridgesUsingProxy:self];
        for (id thisPageContext in pageContexts)
        {
            [thisPageContext unregisterProxy:self];
        }
    }
    
    if (executionContext!=nil)
    {
        executionContext = nil;
    }
    RELEASE_TO_NIL(_proxyBindings);
    
    // remove all listeners JS side proxy
    pthread_rwlock_wrlock(&listenerLock);
    RELEASE_TO_NIL(listeners);
    RELEASE_TO_NIL(listenersOnce);
    pthread_rwlock_unlock(&listenerLock);
    
    pthread_rwlock_wrlock(&dynpropsLock);
    RELEASE_TO_NIL(dynprops);
    pthread_rwlock_unlock(&dynpropsLock);
    
    RELEASE_TO_NIL(evaluators);
    RELEASE_TO_NIL(baseURL);
    RELEASE_TO_NIL(krollDescription);
    if ((void*)modelDelegate != self) {
#ifdef TI_USE_KROLL_THREAD
		TiThreadReleaseOnMainThread(modelDelegate, YES);
        modelDelegate = nil;
#else
        TiThreadPerformOnMainThread(^{
            RELEASE_TO_NIL(modelDelegate);
        }, YES);
#endif
    }
    pageContext=nil;
    pageKrollObject = nil;
}

-(BOOL)destroyed
{
    return destroyed;
}

-(void)dealloc
{
#if PROXY_MEMORY_TRACK == 1
    NSLog(@"[DEBUG] DEALLOC: %@ (%d)",self,[self hash]);
#endif
    [self _destroy];
    pthread_rwlock_destroy(&listenerLock);
    pthread_rwlock_destroy(&dynpropsLock);
    [super dealloc];
}

-(TiHost*)_host
{
    if (pageContext==nil && executionContext==nil)
    {
        return nil;
    }
    if (pageContext!=nil)
    {
        TiHost *h = [pageContext host];
        if (h!=nil)
        {
            return h;
        }
    }
    if (executionContext!=nil)
    {
        return [executionContext host];
    }
    return nil;
}

-(TiProxy*)currentWindow
{
    return [[self pageContext] preloadForKey:@"currentWindow" name:@"UI"];
}

-(NSURL*)_baseURL
{
    if (baseURL==nil)
    {
        TiProxy *currentWindow = [self currentWindow];
        if (currentWindow!=nil)
        {
            // cache it
            [self _setBaseURL:[currentWindow _baseURL]];
            return baseURL;
        }
        return [[self _host] baseURL];
    }
    return baseURL;
}

-(void)_setBaseURL:(NSURL*)url
{
    if (url!=baseURL)
    {
        RELEASE_TO_NIL(baseURL);
        baseURL = [[url absoluteURL] retain];
    }
}

-(void)setReproxying:(BOOL)yn
{
    reproxying = yn;
}

-(BOOL)inReproxy
{
    return reproxying;
}


-(BOOL)_hasListeners:(NSString*)type
{
    pthread_rwlock_rdlock(&listenerLock);
    //If listeners is nil at this point, result is still false.
    BOOL result = [[listeners objectForKey:type] intValue]>0 || [[evaluators objectForKey:type] count] > 0;
    pthread_rwlock_unlock(&listenerLock);
    return result;
}

-(BOOL)_hasAnyListeners:(NSArray*)types
{
    pthread_rwlock_rdlock(&listenerLock);
    //If listeners is nil at this point, result is still false.
    for (NSString* key in types) {
        if ([[listeners objectForKey:key] intValue]>0) {
            pthread_rwlock_unlock(&listenerLock);
            return true;
        }
    }
    pthread_rwlock_unlock(&listenerLock);
    return false;
}

-(void)_fireEventToListener:(NSString*)type withObject:(id)obj listener:(KrollCallback*)listener thisObject:(TiProxy*)thisObject_
{
    TiHost *host = [self _host];
    
    KrollContext* context = [listener context];
    if (context!=nil)
    {
        id<TiEvaluator> evaluator = (id<TiEvaluator>)context.delegate;
        [host fireEvent:listener withObject:obj remove:NO context:evaluator thisObject:thisObject_];
    }
}

-(void)_listenerAdded:(NSString*)type count:(NSInteger)count
{
    // for subclasses
}

-(void)_listenerRemoved:(NSString*)type count:(NSInteger)count
{
    // for subclasses
}

-(TiProxy *)parentForBubbling
{
    return nil;
}

// this method will allow a proxy to return a different object back
// for itself when the proxy serialization occurs from native back
// to the bridge layer - the default is to just return ourselves, however,
// in some concrete implementations you really want to return a different
// representation which this will allow. the resulting value should not be
// retained
-(id)_proxy:(TiProxyBridgeType)type
{
    return self;
}

#pragma mark Public

-(id<NSFastEnumeration>)allKeys
{
    pthread_rwlock_rdlock(&dynpropsLock);
    id<NSFastEnumeration> keys = [dynprops allKeys];
    pthread_rwlock_unlock(&dynpropsLock);
    
    return keys;
}

-(NSNumber*)bubbleParent
{
    return NUMBOOL(_bubbleParent);
}

-(void)setBubbleParent:(id)arg
{
    _bubbleParentDefined = YES;
    _bubbleParent = [TiUtils boolValue:arg def:YES];
}

/*
 *	In views where the order in which keys are applied matter (I'm looking at you, TableView), this should be
 *  an array of which keys go first, and in what order. Otherwise, this is nil.
 */
-(NSArray *)keySequence
{
    return nil;
}

-(KrollObject *)krollObjectForBridge:(KrollBridge *)bridge
{
    if ((pageContext == bridge) && (pageKrollObject != NULL))
    {
        return pageKrollObject;
    }
    
    if (bridgeCount == 0) {
        return nil;
    }
    
    if(![bridge usesProxy:self])
    {
        DeveloperLog(@"[DEBUG] Proxy %@ may be missing its javascript representation.", self);
    }
    
    return [bridge krollObjectForProxy:self];
}

-(KrollObject *)krollObjectForContext:(KrollContext *)context
{
    if ([pageKrollObject context] == context)
    {
        return pageKrollObject;
    }
    
    if (bridgeCount == 0) {
        return nil;
    }
    
    KrollBridge * ourBridge = (KrollBridge *)[context delegate];
    
    if(![ourBridge usesProxy:self])
    {
        DeveloperLog(@"[DEBUG] Proxy %@ may be missing its javascript representation.", self);
    }
    
    return [ourBridge krollObjectForProxy:self];
}

- (int) bindingRunLoopCount
{
    return bridgeCount;
}
- (TiBindingRunLoop) primaryBindingRunLoop
{
    if (pageContext != nil) {
        return [pageContext krollContext];
    }
    return nil;
}
- (NSArray *) bindingRunLoopArray
{
    return [[KrollBridge krollBridgesUsingProxy:self] valueForKeyPath:@"krollContext"];
}

-(BOOL)retainsJsObjectForKey:(NSString *)key
{
    return YES;
}

-(void)rememberProxy:(TiProxy *)rememberedProxy
{
    if (rememberedProxy == nil)
    {
        return;
    }
    KrollContext* context = [self getContext].krollContext;
    if ([rememberedProxy bindingRunLoopCount] == 0 && ![context isKJSThread]) {
        [rememberedProxy invokeBlockOnJSThread:^{
            [self rememberProxy:rememberedProxy];
        }];
        return;
    }
    if ((bridgeCount == 1) && (pageKrollObject != nil))
    {
        if (rememberedProxy == self) {
            [pageKrollObject protectJsobject];
            return;
        }
        [pageKrollObject retain];
        [pageKrollObject noteKeylessKrollObject:[rememberedProxy krollObjectForBridge:(KrollBridge*)pageContext]];
        [pageKrollObject release];
        return;
    }
    if (bridgeCount < 1)
    {
        DeveloperLog(@"[DEBUG] Proxy %@ is missing its javascript representation needed to remember %@.",self,rememberedProxy);
        return;
    }
    
    for (KrollBridge * thisBridge in [KrollBridge krollBridgesUsingProxy:self])
    {
        if(rememberedProxy == self)
        {
            KrollObject * thisObject = [thisBridge krollObjectForProxy:self];
            [thisObject protectJsobject];
            continue;
        }
        
        if(![thisBridge usesProxy:rememberedProxy])
        {
            continue;
        }
        [[thisBridge krollObjectForProxy:self] noteKeylessKrollObject:[thisBridge krollObjectForProxy:rememberedProxy]];
    }
}


-(void)forgetProxy:(TiProxy *)forgottenProxy
{
    if (forgottenProxy == nil)
    {
        return;
    }
    if ((bridgeCount == 1) && (pageKrollObject != nil))
    {
        if (forgottenProxy == self) {
            [pageKrollObject unprotectJsobject];
            return;
        }
        [pageKrollObject forgetKeylessKrollObject:[forgottenProxy krollObjectForBridge:(KrollBridge*)pageContext]];
        return;
    }
    if (bridgeCount < 1)
    {
        //While this may be of concern and there used to be a
        //warning here, too many false alarms were raised during
        //multi-context cleanups.
        return;
    }
    
    for (KrollBridge * thisBridge in [KrollBridge krollBridgesUsingProxy:self])
    {
        if(forgottenProxy == self)
        {
            KrollObject * thisObject = [thisBridge krollObjectForProxy:self];
            [thisObject unprotectJsobject];
            continue;
        }
        
        if(![thisBridge usesProxy:forgottenProxy])
        {
            continue;
        }
        [[thisBridge krollObjectForProxy:self] forgetKeylessKrollObject:[thisBridge krollObjectForProxy:forgottenProxy]];
    }
}

-(void)rememberSelf
{
    [self rememberProxy:self];
}

-(void)forgetSelf
{
    [self forgetProxy:self];
}

-(void)setCallback:(KrollCallback *)eventCallback forKey:(NSString *)key
{
    BOOL isCallback = [eventCallback isKindOfClass:[KrollCallback class]]; //Also check against nil.
    if ((bridgeCount == 1) && (pageKrollObject != nil)) {
        if (!isCallback || ([eventCallback context] != [pageKrollObject context]))
        {
            [pageKrollObject forgetCallbackForKey:key];
        }
        else
        {
            [pageKrollObject noteCallback:eventCallback forKey:key];
        }
        return;
    }
    
    KrollBridge * blessedBridge = (KrollBridge*)[[eventCallback context] delegate];
    NSArray * bridges = [KrollBridge krollBridgesUsingProxy:self];
    
    for (KrollBridge * currentBridge in bridges)
    {
        KrollObject * currentKrollObject = [currentBridge krollObjectForProxy:self];
        if(!isCallback || (blessedBridge != currentBridge))
        {
            [currentKrollObject forgetCallbackForKey:key];
        }
        else
        {
            [currentKrollObject noteCallback:eventCallback forKey:key];
        }
    }
    
}
-(void)fireCallback:(NSString*)type withArg:(NSDictionary *)argDict withSource:(id)source
{
    [self fireCallback:type withArg:argDict withSource:source withHandler:nil];
}
-(void)fireCallback:(NSString*)type withArg:(NSDictionary *)argDict withSource:(id)source withHandler:(void(^)(id result))block
{
	NSMutableDictionary* eventObject = [NSMutableDictionary dictionaryWithObjectsAndKeys:type,@"type",self,@"source",nil];
	if ([argDict isKindOfClass:[NSDictionary class]])
	{
		[eventObject addEntriesFromDictionary:argDict];
	}

	if ((bridgeCount == 1) && (pageKrollObject != nil)) {
		[pageKrollObject invokeCallbackForKey:type withObject:eventObject thisObject:source onDone:block];
		return;
	}
	
	
	NSArray * bridges = [KrollBridge krollBridgesUsingProxy:self];
	for (KrollBridge * currentBridge in bridges)
	{
		KrollObject * currentKrollObject = [currentBridge krollObjectForProxy:self];
		[currentKrollObject invokeCallbackForKey:type withObject:eventObject thisObject:source onDone:nil];
	}
}

-(id)internalAddEventListener:(NSString *)type withListener:(id)listener onlyOnce:(BOOL)onlyOnce
{
    if (![listener isKindOfClass:[KrollWrapper class]] &&
        ![listener isKindOfClass:[KrollCallback class]]) {
        if (IS_OF_CLASS(listener, NSDictionary)) {
            if(evaluators==nil){
                evaluators = [[NSMutableDictionary alloc] initWithCapacity:3];
            }
            NSMutableArray* theListeners = [evaluators objectForKey:type];
            if (!theListeners) {
                theListeners = [NSMutableArray array];
                [evaluators setObject:theListeners forKey:type];
            }
            if (onlyOnce) {
                listener = [NSMutableDictionary dictionaryWithDictionary:listener];
                [listener setValue:@(YES) forKey:@"__once__"];
            }
            [theListeners addObject:listener];
            [self _listenerAdded:type count:[theListeners count]];
            return self;
        } else {
            ENSURE_TYPE(listener,KrollCallback);
        }
    }
    
    [[self getContext].krollContext invokeBlockOnThread:^{
        KrollObject * ourObject = [self krollObjectForContext:([listener isKindOfClass:[KrollCallback class]] ? [(KrollCallback *)listener context] : [(KrollWrapper *)listener bridge].krollContext)];
        [ourObject storeListener:listener forEvent:type];
    }];
    
    //TODO: You know, we can probably nip this in the bud and do this at a lower level,
    //Or make this less onerous.
    int ourCallbackCount = 0;
    
    pthread_rwlock_wrlock(&listenerLock);
    if (onlyOnce) {
        if(listenersOnce==nil){
            listenersOnce = [[NSMutableDictionary alloc] initWithCapacity:3];
        }
        NSMutableArray* theListenersOnce = [listenersOnce objectForKey:type];
        if (!theListenersOnce) {
            theListenersOnce = [NSMutableArray array];
            [listenersOnce setObject:theListenersOnce forKey:type];
        }
        [theListenersOnce addObject:listener];
    }
    ourCallbackCount = [[listeners objectForKey:type] intValue] + 1;
    if(listeners==nil){
        listeners = [[NSMutableDictionary alloc] initWithCapacity:3];
    }
    [listeners setObject:NUMINT(ourCallbackCount) forKey:type];
    pthread_rwlock_unlock(&listenerLock);
    
    [self _listenerAdded:type count:ourCallbackCount];
    return self;
}

-(void)checkForListenerOnce:(NSString *)type withListener:(TiObjectRef)callbackFunction
{
    pthread_rwlock_wrlock(&listenerLock);
    if(listenersOnce == nil) {
        return;
    }
    NSMutableArray* theListenersOnce = [listenersOnce objectForKey:type];
    if (theListenersOnce == nil) {
        return;
    }
    __block BOOL unlocked = NO;
    [theListenersOnce enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
        TiObjectRef function = (IS_OF_CLASS(obj, KrollCallback) ? [(KrollCallback *)obj function] : [(KrollWrapper *)obj jsobject]);
        if (function == callbackFunction) {
            *stop = YES;
            pthread_rwlock_unlock(&listenerLock);
            unlocked = YES;
            [self removeEventListener:@[type, obj]];
        }
    }];
    if (!unlocked) {
        pthread_rwlock_unlock(&listenerLock);
    }
}


-(id)addEventListener:(NSArray*)args
{
    NSString *type = [args objectAtIndex:0];
    id listener = [args objectAtIndex:1];
    return [self internalAddEventListener:type withListener:listener onlyOnce:NO];
}

-(id)on:(NSArray*)args
{
    return [self addEventListener:args];
}

-(id)once:(NSArray*)args
{
    NSString *type = [args objectAtIndex:0];
    id listener = [args objectAtIndex:1];
    return [self internalAddEventListener:type withListener:listener onlyOnce:YES];
}

-(id)removeEventListener:(NSArray*)args
{
    NSString *type = [args objectAtIndex:0];
    KrollCallback* listener = [args objectAtIndex:1];
    if (IS_OF_CLASS(listener, NSDictionary)) {
        if(evaluators){
            NSMutableArray* theEvaluators = [evaluators objectForKey:type];
            if (theEvaluators) {
                for (NSDictionary* theEvaluator in theEvaluators) {
                    if ([theEvaluator isEqualToDictionary:(NSDictionary*)listener]) {
                        [theEvaluators removeObject:listener];
                        [self _listenerRemoved:type count:[theEvaluators count]];
                        break;
                    }
                }
            }
        }
        return self;
    } else {
        ENSURE_TYPE(listener,KrollCallback);
    }
    
    [[self getContext].krollContext invokeBlockOnThread:^{
        KrollObject * ourObject = [self krollObjectForContext:[listener context]];
        [ourObject removeListener:listener forEvent:type];
        
    }];
    //TODO: You know, we can probably nip this in the bud and do this at a lower level,
    //Or make this less onerous.
    int ourCallbackCount = 0;
    
    pthread_rwlock_wrlock(&listenerLock);
    if(listenersOnce){
        NSMutableArray* theListenersOnce = [listenersOnce objectForKey:type];
        if (theListenersOnce) {
            [theListenersOnce removeObject:listener];
            if ([theListenersOnce count] == 0) {
                [listenersOnce removeObjectForKey:type];
                if ([listenersOnce count] == 0) {
                    RELEASE_TO_NIL(listenersOnce);
                }
            }
        }
    }
    if ([listeners objectForKey:type]) {
        ourCallbackCount = [[listeners objectForKey:type] intValue] - 1;
    }
    [listeners setObject:NUMINT(ourCallbackCount) forKey:type];
    pthread_rwlock_unlock(&listenerLock);
    
    [self _listenerRemoved:type count:ourCallbackCount];
    return self;
}

-(id)off:(NSArray*)args
{
    return [self removeEventListener:args];
}

-(void)fireEvent:(id)args
{
    NSString *type = nil;
    NSDictionary * params = nil;
    if ([args isKindOfClass:[NSArray class]])
    {
        type = [args objectAtIndex:0];
        if ([args count] > 1)
        {
            params = [args objectAtIndex:1];
        }
        if ([params isKindOfClass:[NSNull class]]) {
            DebugLog(@"[WARN]fireEvent of type %@ called with two parameters but second parameter is null. Ignoring. Check your code",type);
            params = nil;
        }
    }
    else if ([args isKindOfClass:[NSString class]])
    {
        type = (NSString*)args;
    }
    id bubbleObject = [params objectForKey:@"bubbles"];
    BOOL bubble = [TiUtils boolValue:bubbleObject def:NO];
    if((bubbleObject != nil) && ([params count]==1)){
        params = nil; //No need to propagate when we already have this information
    }
    [self fireEvent:type withObject:params withSource:self propagate:bubble reportSuccess:NO errorCode:0 message:nil];
}

-(void)emit:(id)args
{
    [self fireEvent:args];
}

-(void)fireEvent:(NSString*)type propagate:(BOOL)yn
{
    [self fireEvent:type withObject:nil propagate:yn reportSuccess:NO errorCode:0 message:nil];
}
-(void)fireEvent:(NSString*)type propagate:(BOOL)yn checkForListener:(BOOL)checkForListener
{
    [self fireEvent:type withObject:nil propagate:yn reportSuccess:NO errorCode:0 message:nil checkForListener:checkForListener];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj
{
    [self fireEvent:type withObject:obj propagate:YES reportSuccess:NO errorCode:0 message:nil];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj checkForListener:(BOOL)checkForListener
{
    [self fireEvent:type withObject:obj propagate:YES reportSuccess:NO errorCode:0 message:nil checkForListener:checkForListener];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)yn
{
    [self fireEvent:type withObject:obj withSource:self propagate:yn];
}
-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)propagate
{
    [self fireEvent:type withObject:obj withSource:source propagate:propagate reportSuccess:NO errorCode:0 message:nil];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)yn checkForListener:(BOOL)checkForListener
{
    [self fireEvent:type withObject:obj propagate:yn reportSuccess:NO errorCode:0 message:nil checkForListener:checkForListener];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)yn checkForListener:(BOOL)checkForListener
{
    [self fireEvent:type withObject:obj withSource:source propagate:yn reportSuccess:NO errorCode:0 message:nil checkForListener:checkForListener];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj errorCode:(NSInteger)code message:(NSString*)message;
{
    [self fireEvent:type withObject:obj propagate:YES reportSuccess:YES errorCode:code message:message];
}

//What classes should actually use.
-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(NSInteger)code message:(NSString*)message checkForListener:(BOOL)checkForListener
{
    [self fireEvent:type withObject:obj withSource:self propagate:propagate reportSuccess:report errorCode:code message:message checkForListener:checkForListener];
}

//What classes should actually use.
-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(NSInteger)code message:(NSString*)message checkForListener:(BOOL)checkForListener
{
//    if (bridgeCount == 0 && _aboutToBeBridge) {
//        KrollContext* context = [self getContext].krollContext;
//        if (![context isKJSThread]) {
//            [context invokeBlockOnThread:^{
//                [self fireEvent:type withObject:obj withSource:self propagate:propagate reportSuccess:report errorCode:code message:message checkForListener:checkForListener];
//            }];
//            return;
//        }
//    }
    if (checkForListener && ![self _hasListeners:type])
    {
        return;
    }
    
    if (_bubbleParentDefined) {
        propagate = _bubbleParent;
    }
    
    NSMutableDictionary* eventObject = (IS_OF_CLASS(obj, NSDictionary))? [NSMutableDictionary dictionaryWithDictionary:obj] : [NSMutableDictionary dictionary];
    if (eventOverrideDelegate != nil) {
        [eventOverrideDelegate overrideEventObject:eventObject forEvent:type fromViewProxy:self];
    }
    
    // common event properties for all events we fire.. IF they're undefined.
    if ([eventObject objectForKey:@"type"] == nil) {
        [eventObject setObject:type forKey:@"type"];
    }
    
    id realSource = [eventObject objectForKey:@"source"];
    if (!realSource) {
        realSource = source;
        [eventObject setObject:realSource forKey:@"source"];
    }
    id bindId = [realSource valueForKey:@"bindId"];
    if (bindId != nil) {
        [eventObject setObject:bindId forKey:@"bindId"];
    }
    
    NSArray* theEvaluators = [evaluators valueForKey:type];
    if (theEvaluators) {
        for (NSDictionary* theEvaluator in theEvaluators) {
            [TiUtils applyMathDict:theEvaluator forEvent:eventObject fromProxy:realSource];
            if ([[theEvaluator valueForKey:@"__once__"] boolValue]) {
                [self removeEventListener:@[type, theEvaluator]];
            }
        }
    }
    
#ifndef TI_USE_KROLL_THREAD
    dispatch_async(dispatch_get_main_queue(), ^{
#endif
        TiBindingEvent ourEvent;
        ourEvent = TiBindingEventCreateWithNSObjects(self, self, type, eventObject);
        if (report || (code != 0))
        {
            TiBindingEventSetErrorCode(ourEvent, code);
        }
        if (message != nil)
        {
            TiBindingEventSetErrorMessageWithNSString(ourEvent, message);
        }
        TiBindingEventSetBubbles(ourEvent, propagate);
        TiBindingEventFire(ourEvent);
#ifndef TI_USE_KROLL_THREAD
    });
#endif
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(NSInteger)code message:(NSString*)message;
{
    [self fireEvent:type withObject:obj withSource:self propagate:propagate reportSuccess:report errorCode:code message:message];
}

-(void)fireEvent:(NSString*)type withObject:(id)obj withSource:(id)source propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(NSInteger)code message:(NSString*)message
{
    [self fireEvent:type withObject:obj withSource:source propagate:propagate reportSuccess:report errorCode:code message:message checkForListener:YES];
}

-(void)handleSetValue:(id)value forKey:(NSString*)key {
    if ([key isEqualToString:@"events"] || [key isEqualToString:@"childTemplates"]) {
        return;
    }
    if ((bridgeCount == 1) && (pageKrollObject != nil)) {
        if([value isKindOfClass:[TiProxy class]] && [pageContext usesProxy:value])
        {
            [pageKrollObject noteKrollObject:[value krollObjectForBridge:(KrollBridge*)pageContext] forKey:key];
        }
    }
    else
    {
        for (KrollBridge * currentBridge in [KrollBridge krollBridgesUsingProxy:self])
        {
            KrollObject * currentKrollObject = [currentBridge krollObjectForProxy:self];
            if([value isKindOfClass:[TiProxy class]] && [currentBridge usesProxy:value])
            {
                [currentKrollObject noteKrollObject:[currentBridge krollObjectForProxy:value] forKey:key];
            }
        }
    }
    if (value == nil) //Dictionary doesn't have this key. Skip.
    {
        return;
    }
    if (value == [NSNull null])
    {
        //When a null, we want to write a nil.
        value = nil;
    }
    [self setValue:value forKey:key];
}

- (void)setValuesForKeysWithDictionary:(NSDictionary *)keyedValues
{
    NSArray * keySequence = [self keySequence];
    
    for (NSString * thisKey in keySequence)
    {
        [self handleSetValue:[keyedValues objectForKey:thisKey] forKey:thisKey];
    }
    
    for (NSString * thisKey in keyedValues)
    {
        // don't set if already set above
        if ([keySequence containsObject:thisKey]) continue;
        [self handleSetValue:[keyedValues objectForKey:thisKey] forKey:thisKey];

    }
}

DEFINE_EXCEPTIONS

- (id) valueForUndefinedKey: (NSString *) key
{
    if ([key isEqualToString:@"toString"] || [key isEqualToString:@"valueOf"])
    {
        return [self description];
    }
    if (dynprops != nil)
    {
        pthread_rwlock_rdlock(&dynpropsLock);
        // In some circumstances this result can be replaced at an inconvenient time,
        // releasing the returned value - so we retain/autorelease.
        id result = [[[dynprops objectForKey:key] retain] autorelease];
        pthread_rwlock_unlock(&dynpropsLock);
        
        // if we have a stored value as complex, just unwrap
        // it and return the internal value
        if ([result isKindOfClass:[TiComplexValue class]])
        {
            TiComplexValue *value = (TiComplexValue*)result;
            return [value value];
        }
        return result;
    }
    //NOTE: we need to return nil here since in JS you can ask for properties
    //that don't exist and it should return undefined, not an exception
    return nil;
}

- (void)replaceValue:(id)value forKey:(NSString*)key notification:(BOOL)notify
{
    if (destroyed) {
        return;
    }
    BOOL isCallback = [value isKindOfClass:[KrollCallback class]];
    if(isCallback){
        [self setCallback:value forKey:key];
        //As a wrapper, we hold onto a KrollWrapper tuple so that other contexts
        //may access the function.
        KrollWrapper * newValue = [[[KrollWrapper alloc] init] autorelease];
        [newValue setBridge:(KrollBridge*)[[(KrollCallback*)value context] delegate]];
        [newValue setJsobject:[(KrollCallback*)value function]];
        [newValue protectJsobject];
        value = newValue;
    }
    
    id current = nil;
    
    pthread_rwlock_wrlock(&dynpropsLock);
    if (dynprops!=nil)
    {
        // hold it for this invocation since set may cause it to be deleted
        current = [[[dynprops objectForKey:key] retain] autorelease];
        if (current==[NSNull null])
        {
            current = nil;
        }
    }
    else
    {
        dynprops = [[NSMutableDictionary alloc] init];
    }
    
    // TODO: Clarify internal difference between nil/NSNull
    // (which represent different JS values, but possibly consistent internal behavior)
    
    id propvalue = (value == nil) ? [NSNull null] : value;
    
    BOOL newValue = (current != propvalue && ![current isEqual:propvalue]);
    
    
    if (isCallback || !_fakeApplyProperties) {
        // We need to stage this out; the problem at hand is that some values
        // we might store as properties (such as NSArray) use isEqual: as a
        // strict address/hash comparison. So the notification must always
        // occur, and it's up to the delegate to make sense of it (for now).
        if (newValue) {
            // Remember any proxies set on us so they don't get GC'd
            if ([propvalue isKindOfClass:[TiProxy class]]) {
                [self rememberProxy:propvalue];
            }
            [dynprops setValue:propvalue forKey:key];
        }
    }
    pthread_rwlock_unlock(&dynpropsLock);
    
    
    if (self.modelDelegate!=nil && notify)
    {
        NSObject* delegate = [self.modelDelegate retain];
        [self.modelDelegate propertyChanged:key
                                   oldValue:current
                                   newValue:propvalue
                                      proxy:self];
        [delegate autorelease];
    }
    
    [self handleUpdatedValue:propvalue forKey:key];
    
    
    if (isCallback || !_fakeApplyProperties) {
        // Forget any old proxies so that they get cleaned up
        if (newValue && [current isKindOfClass:[TiProxy class]]) {
            [self forgetProxy:current];
        }
    }
}

-(void)handleUpdatedValue:(id)value forKey:(NSString*)key {
    //to be overriden
}

// TODO: Shouldn't we be forgetting proxies and unprotecting callbacks and such here?
- (void) deleteKey:(NSString*)key
{
    pthread_rwlock_wrlock(&dynpropsLock);
    if (dynprops!=nil)
    {
        [dynprops removeObjectForKey:key];
    }
    pthread_rwlock_unlock(&dynpropsLock);
}

- (void) setValue:(id)value forUndefinedKey: (NSString *) key
{
    [self replaceValue:value forKey:key notification:YES];
}

- (void) setValue:(id)value forKeyPath: (NSString *) key
{
    id bindedValue = [_proxyBindings objectForKey:key];
    if (bindedValue)
    {
        [bindedValue setValuesForKeysWithDictionary:value];
    }
    else {
        [super setValue:value forKeyPath:key];
    }
}

- (id) valueForKey: (NSString *) key
{
    id bindedValue = [_proxyBindings objectForKey:key];
    if (bindedValue)
    {
        return bindedValue;
    }
    return [super valueForKey:key];
}


-(void)applyProperties:(id)args
{
    if (!args) return;
    ENSURE_SINGLE_ARG(args, NSDictionary)
    NSArray * keySequence = [self keySequence];
    
    for (NSString * thisKey in keySequence)
    {
        id thisValue = [args objectForKey:thisKey];
        if (thisValue == nil) //Dictionary doesn't have this key. Skip.
        {
            continue;
        }
        if (thisValue == [NSNull null])
        {
            //When a null, we want to write a nil.
            thisValue = nil;
        }
        [self setValue:thisValue forKey:thisKey];
    }
    [args enumerateKeysAndObjectsUsingBlock:^(id key, id value, BOOL *stop) {
        if ([keySequence containsObject:key]) return;
        id obj = [_proxyBindings valueForKey:key];
        if ([obj isKindOfClass:[TiProxy class]] && [value isKindOfClass:[NSDictionary class]]) {
            [obj applyProperties:value];
        }
        else {
            [self setValue:value forKey:key];
        }
    }];
}

-(NSDictionary*)allProperties
{
    pthread_rwlock_rdlock(&dynpropsLock);
    NSDictionary* props = [[dynprops copy] autorelease];
    pthread_rwlock_unlock(&dynpropsLock);
    
    return props;
}

-(id)sanitizeURL:(id)value
{
    if (value == [NSNull null])
    {
        return nil;
    }
    
    if([value isKindOfClass:[NSString class]])
    {
        NSURL * result = [TiUtils toURL:value proxy:self];
        if (result != nil)
        {
            return result;
        }
    }
    
    return value;
}

#pragma mark Memory Management

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
    //FOR NOW, we're not dropping anything but we'll want to do before release
    //subclasses need to call super if overriden
}

#pragma mark Dispatching Helper

//TODO: Now that we have TiThreadPerform, we should optimize this out.
-(void)_dispatchWithObjectOnUIThread:(NSArray*)args
{
    //NOTE: this is called by ENSURE_UI_THREAD_WITH_OBJ and will always be on UI thread when we get here
    id selector = [args objectAtIndex:0];
    id firstobj = [args count] > 1 ? [args objectAtIndex:1] : nil;
    id secondobj = [args count] > 2 ? [args objectAtIndex:2] : nil;
    id target = [args count] > 3 ? [args objectAtIndex:3] : self;
    if (firstobj == [NSNull null])
    {
        firstobj = nil;
    }
    if (secondobj == [NSNull null])
    {
        secondobj = nil;
    }
    [target performSelector: NSSelectorFromString(selector) withObject:firstobj withObject:secondobj];
}

#pragma mark Description for nice toString in JS

-(id)toString:(id)args
{
    if (krollDescription==nil)
    {
        // if we have a cached id, use it for our identifier
        id temp = [self valueForUndefinedKey:@"id"];
        NSString *cn =nil;
        if (temp==nil||![temp isKindOfClass:[NSString class]]){
            cn = NSStringFromClass([self class]);
        }
        else {
            cn = temp;
        }
        krollDescription = [[NSString stringWithFormat:@"[object %@]",[cn stringByReplacingOccurrencesOfString:@"Proxy" withString:@""]] retain];
    }
    
    return krollDescription;
}

-(id)description
{
    return [self toString:nil];
}

-(id)toJSON
{
    // this is called in the case you try and use JSON.stringify and an object is a proxy
    // since you can't serialize a proxy as JSON, just return null
    return [NSNull null];
}

//For subclasses to override
-(NSString*)apiName
{
    DebugLog(@"[ERROR] Subclasses must override the apiName API endpoint.");
    return @"Ti.Proxy";
}

+(CFMutableDictionaryRef)classNameLookup
{
    static dispatch_once_t onceToken;
    static CFMutableDictionaryRef classNameLookup;
    dispatch_once(&onceToken, ^{
        classNameLookup = CFDictionaryCreateMutable(kCFAllocatorDefault, 1, &kCFTypeDictionaryKeyCallBacks, NULL);
    });
    return classNameLookup;
}


+(Class)proxyClassFromString:(NSString*)qualifiedName
{
    Class proxyClass = (Class)CFDictionaryGetValue([TiProxy classNameLookup], qualifiedName);
    if (proxyClass == nil) {
        NSString *titanium = [NSString stringWithFormat:@"%@%s",@"Ti","tanium."];
        if ([qualifiedName hasPrefix:titanium]) {
            qualifiedName = [qualifiedName stringByReplacingCharactersInRange:NSMakeRange(2, 6) withString:@""];
        }
        NSString *className = [[qualifiedName stringByReplacingOccurrencesOfString:@"." withString:@""] stringByAppendingString:@"Proxy"];
        proxyClass = NSClassFromString(className);
        if (proxyClass==nil) {
            DebugLog(@"[WARN] Attempted to load %@: Could not find class definition.", className);
            //			@throw [NSException exceptionWithName:@"org.appcelerator.module"
            //                                           reason:[NSString stringWithFormat:@"Class not found: %@", qualifiedName]
            //                                         userInfo:nil];
            return nil;
        }
        CFDictionarySetValue([TiProxy classNameLookup], qualifiedName, proxyClass);
    }
    return proxyClass;
}

+ (id)createProxy:(Class)proxyClass withProperties:(NSDictionary*)properties inContext:(id<TiEvaluator>)context
{
    if (proxyClass) {
        NSArray *args = properties != nil ? [NSArray arrayWithObject:properties] : nil;
        return [[[proxyClass alloc] _initWithPageContext:context args:args
                 ] autorelease];
    }
    return nil;
}

-(id)objectOfClass:(Class)theClass fromArg:(id)arg {
    id result = [TiProxy objectOfClass:theClass fromArg:arg inContext:[self getContext]];
    if (IS_OF_CLASS(result, TiProxy)) {
        id bindId = [result valueForUndefinedKey:@"bindId"];
        if (bindId) {
            [self addBinding:result forKey:bindId];
        }
    }
    return result;
}

+(id)objectOfClass:(Class)theClass fromArg:(id)arg inContext:(id<TiEvaluator>)context_ {
    if ([arg isKindOfClass:[NSArray class]] && [arg count] >0) {
        arg = [arg objectAtIndex:0];
    }
    if ([arg isKindOfClass:[theClass class]])
    {
        return arg;
    }
    if (arg == nil || ![arg isKindOfClass:[NSDictionary class]]) return nil;
    return [[[[theClass class] alloc] _initWithPageContext:context_ args:[NSArray arrayWithObject:arg]] autorelease];
}


#pragma mark - View Templates

-(id<TiEvaluator>)getContext {
    id<TiEvaluator> context = self.executionContext;
    if (context == nil) {
        context = self.pageContext;
    }
    return context;
}

- (void)unarchiveFromTemplate:(id)viewTemplate_ withEvents:(BOOL)withEvents
{
    
    [self unarchiveFromTemplate:viewTemplate_ withEvents:withEvents rootProxy:self];
}


- (void)unarchiveFromTemplate:(id)viewTemplate_ withEvents:(BOOL)withEvents rootProxy:(TiParentingProxy*)rootProxy
{
    TiProxyTemplate *viewTemplate = [TiProxyTemplate templateFromViewTemplate:viewTemplate_];
    if (viewTemplate == nil) {
        return;
    }
    
    [self _initWithProperties:viewTemplate.properties];
    if (withEvents && [viewTemplate.events count] > 0) {
        //        [context.krollContext invokeBlockOnThread:^{
        [viewTemplate.events enumerateKeysAndObjectsUsingBlock:^(NSString *eventName, NSArray *list, BOOL *stop) {
            [list enumerateObjectsUsingBlock:^(KrollWrapper *wrapper, NSUInteger idx, BOOL *stop) {
                [self addEventListener:[NSArray arrayWithObjects:eventName, wrapper, nil]];
            }];
        }];
        //        }];
    }
}

+ (TiProxy *)createFromDictionary:(NSDictionary*)dictionary rootProxy:(TiParentingProxy*)rootProxy inContext:(id<TiEvaluator>)context
{
    return [[self class] createFromDictionary:dictionary rootProxy:rootProxy inContext:context defaultType:nil];
}

// Returns protected proxy, caller should do forgetSelf.
+ (TiProxy *)createFromDictionary:(NSDictionary*)dictionary rootProxy:(TiParentingProxy*)rootProxy inContext:(id<TiEvaluator>)context defaultType:(NSString*)defaultType
{
    if (dictionary == nil) {
        return nil;
    }
    NSString* type = [dictionary objectForKey:@"type"];
    
    if (defaultType == nil) defaultType = @"Ti.UI.View";
    if (type == nil) type = defaultType;
    TiProxy *proxy = proxy = [[self class] createProxy:[[self class] proxyClassFromString:type] withProperties:nil inContext:context];
    [proxy internalSetCreatedFromDictionary]; //private access
    [proxy rememberSelf];
    if (proxy) {
        if (!rootProxy && IS_OF_CLASS(proxy, TiParentingProxy)) {
            rootProxy = (TiParentingProxy*)proxy;
        }
        [proxy unarchiveFromDictionary:dictionary rootProxy:rootProxy];
    }
    
    return proxy;
}

- (void)unarchiveFromDictionary:(NSDictionary*)dictionary rootProxy:(TiParentingProxy*)rootProxy
{
    if (dictionary == nil) {
        return;
    }
    
    id<TiEvaluator> context = self.executionContext;
    if (context == nil) {
        context = self.pageContext;
    }
    NSDictionary* properties = (NSDictionary*)[dictionary objectForKey:@"properties"];
    if (properties == nil) properties = dictionary;
    [self _initWithProperties:properties];
    NSDictionary* events = (NSDictionary*)[dictionary objectForKey:@"events"];
    if ([events count] > 0) {
        [context.krollContext invokeBlockOnThread:^{
            [events enumerateKeysAndObjectsUsingBlock:^(NSString *eventName, id listener, BOOL *stop) {
                [self addEventListener:listener forEventType:eventName];
            }];
        }];
    }
}

-(void)addEventListener:(id)listener forEventType:(NSString*)eventName
{
    if (IS_OF_CLASS(listener, NSArray)) {
        [listener enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [self addEventListener:obj forEventType:eventName];
        }];
    } else if (IS_OF_CLASS(listener, KrollCallback)) {
        KrollWrapper *wrapper = ConvertKrollCallbackToWrapper(listener);
        [wrapper protectJsobject];
        [self addEventListener:[NSArray arrayWithObjects:eventName, wrapper, nil]];
    } else {
        [self addEventListener:[NSArray arrayWithObjects:eventName, listener, nil]];
    }
}

-(BOOL)canBeNextResponder
{
    return YES;
}


-(void)addBinding:(TiProxy*)proxy forKey:(NSString*)key
{
    if (!_proxyBindings) {
        _proxyBindings = [[NSMutableDictionary alloc] init];
    }
    [_proxyBindings setObject:proxy forKey:key];
}

-(void)removeBindingForKey:(NSString*)key
{
    if (_proxyBindings) {
        [_proxyBindings removeObjectForKey:key];
    }
}

-(void)removeBindingsForProxy:(TiProxy*)proxy
{
    if (_proxyBindings) {
        NSArray *keys = [_proxyBindings allKeysForObject:proxy];
        [_proxyBindings removeObjectsForKeys:keys];
    }
}

-(TiProxy*)bindingForKey:(NSString*)key
{
    return [_proxyBindings objectForKey:key];
}
@end
