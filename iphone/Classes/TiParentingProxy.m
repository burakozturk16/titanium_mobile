#import "TiParentingProxy.h"
#import "TiUtils.h"
#import "TiProxyTemplate.h"

@implementation TiParentingProxy
{
    BOOL _unarchiving;
}
@synthesize children;
@synthesize parent;


-(id)init
{
	if ((self = [super init]))
	{
        _unarchiving = NO;
		pthread_rwlock_init(&childrenLock, NULL);
        childrenCount = 0;
	}
	return self;
}

-(void)dealloc
{
    parent = nil;
    _parentForBubbling = nil;
	[super dealloc];
}

-(TiProxy *)parentForBubbling
{
	return _parentForBubbling?_parentForBubbling:parent;
}

-(void)_initWithProperties:(NSDictionary*)properties
{
    if (!_unarchiving && ([properties objectForKey:@"properties"] || [properties objectForKey:@"childTemplates"] || [properties objectForKey:@"events"])) {
//        [self invokeBlockOnJSThread:^{
        [self rememberSelf];
        [self unarchiveFromDictionary:properties rootProxy:self];
        [self forgetSelf];
//        }];
        return;
    }
	[super _initWithProperties:properties];
}

-(void)_destroy
{
    [super _destroy]; // call first so that destroyed is set
	pthread_rwlock_wrlock(&childrenLock);
    [self releaseChildOnDestroy:children];
    RELEASE_TO_NIL(children);
	pthread_rwlock_unlock(&childrenLock);
	pthread_rwlock_destroy(&childrenLock);
    
    pthread_rwlock_wrlock(&_holdedProxiesLock);
    [self releaseChildOnDestroy:_holdedProxies];
    RELEASE_TO_NIL(_holdedProxies);
    pthread_rwlock_unlock(&_holdedProxiesLock);
    pthread_rwlock_destroy(&_holdedProxiesLock);
    
}


-(void)releaseChildOnDestroy:(id)child {
    if (!child) return;
    if (IS_OF_CLASS(child, NSArray)) {
        [child enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [self releaseChildOnDestroy:obj];
        }];
        return;
    } else if (IS_OF_CLASS(child, NSDictionary)) {
        [[child allValues] enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [self releaseChildOnDestroy:obj];
        }];
        return;
    }
    [self childRemoved:child wasChild:YES shouldDetach:YES];
}

-(BOOL)_hasListeners:(NSString *)type checkParent:(BOOL)check
{
    BOOL returnVal = [super _hasListeners:type];
    if (_bubbleParentDefined) {
        check = _bubbleParent;
    }
    if (!returnVal && check) {
        returnVal = [[self parentForBubbling] _hasListeners:type];
    }
	return returnVal;
}

-(BOOL)_hasListenersIgnoreBubble:(NSString *)type
{
    return [super _hasListeners:type];
}

-(BOOL)_hasListeners:(NSString *)type
{
	return [self _hasListeners:type checkParent:YES];
}

-(NSArray*)children
{
    if (childrenCount == 0) {
        return [NSMutableArray array];
    }
//    if (![NSThread isMainThread]) {
//        __block NSArray* result = nil;
//        TiThreadPerformOnMainThread(^{
//            result = [[self children] retain];
//        }, YES);
//        return [result autorelease];
//    }
    
	pthread_rwlock_rdlock(&childrenLock);
    NSArray* copy = [children mutableCopy];
	pthread_rwlock_unlock(&childrenLock);
	return ((copy != nil) ? [copy autorelease] : [NSMutableArray array]);
}

-(TiProxy*)childAt:(NSInteger)index
{
    TiProxy* result = nil;
    if (index >= 0 && index < childrenCount)
    {
        pthread_rwlock_rdlock(&childrenLock);
        result = [children objectAtIndex:index];
        pthread_rwlock_unlock(&childrenLock);
    }
    return result;
}

-(NSUInteger)childrenCount {
    return childrenCount;
}

-(void)setParent:(TiParentingProxy *)newParent
{
    if (parent) {
        //we need to remove the child from his current parent children's list
        // or it might get detached when the old parent gets released
        [parent _removeChild:self];
        RELEASE_TO_NIL(parent)
    }
    parent = [newParent retain];
}

-(BOOL)containsChild:(TiProxy*)child
{
    if (child == self)return YES;
    NSArray* subproxies = [self children];
    for (TiProxy * thisChildProxy in subproxies)
    {
        if (thisChildProxy == child || ([thisChildProxy isKindOfClass:[TiParentingProxy class]] &&
            [(TiParentingProxy*)thisChildProxy containsChild:child])) {
            return YES;
            
        }
    }
    return NO;
}

-(void)add:(id)arg
{
    [self addInternal:arg atIndex:-1 shouldRelayout:YES];
}
-(void)add:(id)arg atIndex:(NSInteger)position {
    [self addInternal:arg atIndex:position shouldRelayout:YES];
}


-(void)addInternal:(id)arg atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
	// allow either an array of arrays or an array of single proxy
	if ([arg isKindOfClass:[NSArray class]])
	{
        if ( [arg count] == 2 && [[arg objectAtIndex:1] isKindOfClass:[NSNumber class]]) {
            [self addInternal:[arg objectAtIndex:0] atIndex:[TiUtils intValue:[arg objectAtIndex:1]] shouldRelayout:shouldRelayout];
        } else {
            NSInteger newPos = position;
            for (id a in arg)
            {
                [self addInternal:a atIndex:newPos shouldRelayout:shouldRelayout];
            }
        }
        
		return;
	}
    TiProxy *child = [self createChildFromObject:arg];
    if (child != nil) {
        [self addProxy:child atIndex:position shouldRelayout:shouldRelayout];
    }
}

-(void)childAdded:(TiProxy*)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    if ([child respondsToSelector:@selector(setParent:)]) {
        [(id)child setParent:self];
    }
}

-(void)addProxy:(id)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    ENSURE_SINGLE_ARG_OR_NIL(child, TiProxy)
    if (child == nil) return;
    [self rememberProxy:child];
    pthread_rwlock_wrlock(&childrenLock);
    if (children==nil)
    {
        children = [[NSMutableArray alloc] initWithObjects:child,nil];
    }
    else
    {
        if(position < 0 || position > [children count]) {
            position = [children count];
        }
        [children insertObject:child atIndex:position];
    }
    childrenCount = [children count];
    pthread_rwlock_unlock(&childrenLock);
    [self childAdded:child atIndex:position shouldRelayout:shouldRelayout];
}

-(void)insertAt:(id)args
{
	ENSURE_SINGLE_ARG(args, NSDictionary);
    [self addInternal:[args objectForKey:@"view"] atIndex:[TiUtils intValue:[args objectForKey:@"position"] def:-1] shouldRelayout:YES];
}

-(void)replaceAt:(id)args
{
	ENSURE_SINGLE_ARG(args, NSDictionary);
	NSInteger position = [TiUtils intValue:[args objectForKey:@"position"] def:-1];
	NSArray *childrenArray = [self children];
	if(childrenArray != nil && position > -1 && [childrenArray count] > position) {
		TiProxy *childToRemove = [[childrenArray objectAtIndex:position] retain];
		[self insertAt:args];
		[self remove: childToRemove];
		[childToRemove autorelease];
	}
}

-(void)childRemoved:(TiProxy*)child wasChild:(BOOL)wasChild shouldDetach:(BOOL)shouldDetach
{
    if (wasChild && [child respondsToSelector:@selector(setParent:)]) {
        [(id)child setParent:nil];
    }
    [self forgetProxy:child];
    if (child.createdFromDictionary) {
        [child forgetSelf];
    }
}


-(void)_removeChild:(id)child
{
    pthread_rwlock_wrlock(&childrenLock);
    if ([children containsObject:child]) {
        [children removeObject:child];
    }
    pthread_rwlock_unlock(&childrenLock);
}


-(void)removeProxy:(id)child shouldDetach:(BOOL)shouldDetach
{
    ENSURE_SINGLE_ARG_OR_NIL(child, TiProxy)
    BOOL wasChild = false;
    pthread_rwlock_wrlock(&childrenLock);
	if ([children containsObject:child]) {
		[children removeObject:child];
        wasChild = true;
	}
	pthread_rwlock_unlock(&childrenLock);
    
    [self childRemoved:child wasChild:wasChild shouldDetach:shouldDetach];
}

-(void)removeProxy:(id)child
{
    if (IS_OF_CLASS(child, TiParentingProxy)) {
        TiParentingProxy* childParent = ((TiParentingProxy*)child).parent;
        if (childParent && childParent != self) {
            return;
        }
    }
    [self removeProxy:child shouldDetach:YES];
}

-(void)remove:(id)arg
{
	ENSURE_UI_THREAD_1_ARG(arg);
    
    if ([arg isKindOfClass:[NSArray class]])
	{
		for (id a in arg)
		{
            [self remove:a];
		}
		return;
	}
    [self removeProxy:arg];
}

-(void)removeFromParent:(id)arg
{
    if (parent)
        [parent remove:self];
}

-(void)removeAllChildren:(id)arg
{
    pthread_rwlock_wrlock(&childrenLock);
    NSMutableArray* childrenCopy = [children mutableCopy];
    [children removeAllObjects];
    childrenCount = 0;
    RELEASE_TO_NIL(children);
    pthread_rwlock_unlock(&childrenLock);
    for (TiProxy* theChild in childrenCopy) {
        [self childRemoved:theChild wasChild:YES shouldDetach:YES];
        [self forgetProxy:theChild];
    }
	[childrenCopy release];
}

-(void)setViews:(id)args
{
    [self removeAllChildren:nil];
    [self add:args];
}

-(void)_listenerAdded:(NSString*)type count:(NSInteger)count
{
    //TIMOB-15991 Update children as well
	NSArray* childrenArray = [[self children] retain];
    for (id child in childrenArray) {
        
        if ([child respondsToSelector:@selector(parentListenersChanged)]) {
            [child performSelector:@selector(parentListenersChanged)];
        }
    }
	[childrenArray release];
}

-(void)_listenerRemoved:(NSString*)type count:(NSInteger)count
{
    //TIMOB-15991 Update children as well
    NSArray* childrenArray = [[self children] retain];
    for (id child in childrenArray) {
        if ([child respondsToSelector:@selector(parentListenersChanged)]) {
            [child performSelector:@selector(parentListenersChanged)];
        }
    }
    [childrenArray release];
}


- (TiProxy *)createChildFromObject:(id)object
{
    return [self createChildFromObject:object rootProxy:self];
}

- (TiProxy *)createChildFromObject:(id)object rootProxy:(TiParentingProxy*)rootProxy
{
    TiProxy *child = nil;
    NSString* bindId = nil;
    if ([object isKindOfClass:[NSDictionary class]]) {
        bindId = [object valueForKey:@"bindId"];
        id<TiEvaluator> context = self.executionContext;
        if (context == nil) {
            context = self.pageContext;
        }
        child = [[self class] createFromDictionary:object rootProxy:rootProxy inContext:context];
        [self rememberProxy:child];
//        [context.krollContext invokeBlockOnThread:^{
//        }];
    }
    else if(([object isKindOfClass:[TiProxy class]]))
    {
        child = (TiProxy *)object;
        bindId = [object valueForUndefinedKey:@"bindId"];
    }
    if (child && bindId) {
        [child setValue:bindId forUndefinedKey:@"bindId"];
        [rootProxy addBinding:child forKey:bindId];
    }
    return child;
}



- (void)unarchiveFromDictionary:(NSDictionary*)dictionary rootProxy:(TiParentingProxy*)rootProxy
{
	if (dictionary == nil) {
		return;
	}
	_unarchiving = YES;
	id<TiEvaluator> context = self.executionContext;
	if (context == nil) {
		context = self.pageContext;
	}
	[super unarchiveFromDictionary:dictionary rootProxy:rootProxy];
    
    NSArray* childTemplates = (NSArray*)[dictionary objectForKey:@"childTemplates"];
	
	[childTemplates enumerateObjectsUsingBlock:^(id childTemplate, NSUInteger idx, BOOL *stop) {
        TiProxy *child = [self createChildFromObject:childTemplate rootProxy:rootProxy];
		if (child != nil) {
            [context.krollContext invokeBlockOnThread:^{
                [self rememberProxy:child];
				[child forgetSelf];
			}];
            [self addProxy:child atIndex:-1 shouldRelayout:NO];
//            if (IS_OF_CLASS(childTemplate, NSDictionary)) {
//                NSDictionary* events = (NSDictionary*)[childTemplate objectForKey:@"events"];
//                if ([events count] > 0) {
//                    [context.krollContext invokeBlockOnThread:^{
//                        [events enumerateKeysAndObjectsUsingBlock:^(NSString *eventName, KrollCallback *listener, BOOL *stop) {
//                            if ([listener isKindOfClass:[KrollCallback class]]) {
//                                KrollWrapper *wrapper = ConvertKrollCallbackToWrapper(listener);
//                                [wrapper protectJsobject];
//                                [child addEventListener:[NSArray arrayWithObjects:eventName, wrapper, nil]];
//                            } else {
//                                [child addEventListener:[NSArray arrayWithObjects:eventName, listener, nil]];
//                            }
//                        }];
//                    }];
//                }
//            }
		}
	}];
	_unarchiving = NO;
}

// Returns protected proxy, caller should do forgetSelf.
//+ (TiProxy *)unarchiveFromTemplate:(id)viewTemplate_ inContext:(id<TiEvaluator>)context withEvents:(BOOL)withEvents
//{
//    TiProxyTemplate *viewTemplate = [TiProxyTemplate templateFromViewTemplate:viewTemplate_];
//    if (viewTemplate == nil) {
//        return;
//    }
//    
//    if (viewTemplate.type != nil) {
//        TiProxy *proxy = [[self class] createProxy:[[self class] proxyClassFromString:viewTemplate.type] withProperties:nil inContext:context];
//        if (proxy) {
//            [context.krollContext invokeBlockOnThread:^{
//                [context registerProxy:proxy];
//                [proxy rememberSelf];
//            }];
//            [proxy unarchiveFromTemplate:viewTemplate withEvents:withEvents];
//            
//        }
//        return proxy;
//    }
//    return nil;
//}

- (void)unarchiveFromTemplate:(id)viewTemplate_ withEvents:(BOOL)withEvents rootProxy:(TiProxy*)rootProxy
{
	TiProxyTemplate *viewTemplate = [TiProxyTemplate templateFromViewTemplate:viewTemplate_];
	if (viewTemplate == nil) {
		return;
	}
	[super unarchiveFromTemplate:viewTemplate withEvents:withEvents rootProxy:rootProxy];
    id<TiEvaluator> context = [rootProxy getContext];
	[viewTemplate.childTemplates enumerateObjectsUsingBlock:^(TiProxyTemplate *childTemplate, NSUInteger idx, BOOL *stop) {
        if (childTemplate.type != nil) {
            TiProxy *child = [[self class] createProxy:[[self class] proxyClassFromString:childTemplate.type] withProperties:nil inContext:context];
            if (child) {
                [context.krollContext invokeBlockOnThread:^{
                    [context registerProxy:child];
                    [child rememberSelf];
                }];
                [child unarchiveFromTemplate:childTemplate withEvents:withEvents rootProxy:rootProxy];
                [context.krollContext invokeBlockOnThread:^{
                    [self rememberProxy:child];
                    [child forgetSelf];
                }];
                [self addProxy:child atIndex:-1 shouldRelayout:NO];
                id bindId = [child valueForUndefinedKey:@"bindId"];
                if (bindId) {
                    [rootProxy addBinding:child forKey:bindId];
                }
            }
//            return proxy;
        }
//
//        TiProxy *child = [[self class] unarchiveFromTemplate:childTemplate inContext:context withEvents:withEvents];
//		if (child != nil) {
//            
//		}
	}];
}

-(id)getNextChildrenOfClass:(Class)theClass afterChild:(TiProxy*)child
{
    id result = nil;
    NSArray* subproxies = [self children];
    NSInteger index=[subproxies indexOfObject:child];
    if(NSNotFound != index) {
        for (NSInteger i = index + 1; i < [subproxies count] ; i++) {
            TiProxy* obj = [subproxies objectAtIndex:i];
            if ([obj isKindOfClass:theClass] && [obj canBeNextResponder]) {
                    return obj;
            }
        }
    }
    return result;
}

-(void)runBlock:(void (^)(TiProxy* proxy))block recursive:(BOOL)recursive
{
//    if (recursive)
//    {
    pthread_rwlock_rdlock(&childrenLock);
    NSArray* subproxies = [self children];
    pthread_rwlock_unlock(&childrenLock);
    for (TiProxy * thisChildProxy in subproxies)
    {
        block(thisChildProxy);
        if (recursive && IS_OF_CLASS(thisChildProxy, TiParentingProxy)) {
            [(TiParentingProxy*)thisChildProxy runBlock:block recursive:recursive];
        }
    }
//    }
}

-(void)makeChildrenPerformSelector:(SEL)selector withObject:(id)object
{
    [[self children] makeObjectsPerformSelector:selector withObject:object];
}

-(id)addObjectToHold:(id)value forKey:(NSString*)key
{
    return [self addObjectToHold:value forKey:key shouldRelayout:NO];
}
-(id)addObjectToHold:(id)value forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout
{
    if (IS_OF_CLASS(value, NSArray)) {
        NSMutableArray* proxies = [NSMutableArray arrayWithCapacity:[value count]];
        [value enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            TiProxy* theProxy = [self createChildFromObject:obj];
            if (theProxy) {
                [proxies addObject:theProxy];
            }
        }];
        return [self addProxiesArrayToHold:proxies forKey:key shouldRelayout:shouldRelayout];
    }
    TiProxy* theProxy = [self createChildFromObject:value];
    return [self addProxyToHold:theProxy forKey:key shouldRelayout:shouldRelayout];
}
-(TiProxy*)addProxyToHold:(TiProxy*)proxy forKey:(NSString*)key
{
    [self addProxyToHold:proxy forKey:key shouldRelayout:NO];
}
-(TiProxy*)addProxyToHold:(TiProxy*)proxy forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout
{
    return [self addProxyToHold:proxy setParent:YES forKey:key shouldRelayout:shouldRelayout];
}

-(TiProxy*)addProxyToHold:(TiProxy*)proxy setParent:(BOOL)setParent forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout
{
    
    TiProxy* oldProxy = [_holdedProxies objectForKey:key];
    if (oldProxy) {
        if (oldProxy == proxy) return proxy;
        if (IS_OF_CLASS(oldProxy, TiParentingProxy) && ((TiParentingProxy*)oldProxy).parent != self) {
            [((TiParentingProxy*)oldProxy).parent removeProxy:oldProxy shouldDetach:YES];
        } else {
            [self childRemoved:oldProxy wasChild:setParent shouldDetach:YES];
        }
    }
    if (proxy) {
        [self rememberProxy:proxy];
        if (!_holdedProxies) {
            pthread_rwlock_init(&_holdedProxiesLock, NULL);
            _holdedProxies = [[NSMutableDictionary alloc] init];
        }
        pthread_rwlock_wrlock(&_holdedProxiesLock);
        [_holdedProxies setValue:proxy forKey:key];
        if (setParent) {
            [self childAdded:proxy atIndex:-1 shouldRelayout:shouldRelayout];
        }
        pthread_rwlock_unlock(&_holdedProxiesLock);
    } else if (_holdedProxies) {
        pthread_rwlock_wrlock(&_holdedProxiesLock);
        [_holdedProxies removeObjectForKey:key];
        pthread_rwlock_unlock(&_holdedProxiesLock);
    }
    return proxy;
}

-(NSArray*)addProxiesArrayToHold:(NSArray*)proxies forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout
{
    if (!_holdedProxies) {
        pthread_rwlock_init(&_holdedProxiesLock, NULL);
        _holdedProxies = [[NSMutableDictionary alloc] init];
    }
    pthread_rwlock_wrlock(&_holdedProxiesLock);
    NSArray* oldProxies = [_holdedProxies objectForKey:key];
    if (oldProxies) {
        if ([oldProxies isEqual:proxies]) {
            pthread_rwlock_wrlock(&_holdedProxiesLock);
            return proxies;
        }
        [oldProxies enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [self forgetProxy:obj];
            if (IS_OF_CLASS(obj, TiParentingProxy) && ((TiParentingProxy*)obj).parent != self) {
                [((TiParentingProxy*)obj).parent removeProxy:obj shouldDetach:YES];
            } else {
                [self childRemoved:obj wasChild:YES shouldDetach:YES];
            }
        }];
        [_holdedProxies removeObjectForKey:key];
    }
    if (proxies) {
        
        [_holdedProxies setValue:proxies forKey:key];
        [proxies enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            [self rememberProxy:obj];
            [self childAdded:obj atIndex:-1 shouldRelayout:shouldRelayout];
        }];
    }
    pthread_rwlock_unlock(&_holdedProxiesLock);
    return proxies;
}
-(id)removeHoldedProxyForKey:(NSString*)key
{
    if (!key || !_holdedProxies) return;
    pthread_rwlock_wrlock(&_holdedProxiesLock);
    id object = [_holdedProxies objectForKey:key];
    if (!object) {
        NSLog(@"[WARN] there is no holded proxy for the key %@", key);
        pthread_rwlock_unlock(&_holdedProxiesLock);
        return nil;
    }
    if (IS_OF_CLASS(object, NSArray)) {
        [object enumerateObjectsUsingBlock:^(id obj, NSUInteger idx, BOOL *stop) {
            if (IS_OF_CLASS(obj, TiParentingProxy) && ((TiParentingProxy*)obj).parent != self) {
                [((TiParentingProxy*)obj).parent removeProxy:obj shouldDetach:YES];
            } else {
                [self childRemoved:obj wasChild:YES shouldDetach:YES];
            }
        }];
    } else {
        if (IS_OF_CLASS(object, TiParentingProxy) && ((TiParentingProxy*)object).parent != self) {
            [((TiParentingProxy*)object).parent removeProxy:object shouldDetach:YES];
        } else {
            [self childRemoved:object wasChild:YES shouldDetach:YES];
        }
    }
    [_holdedProxies removeObjectForKey:key];
    pthread_rwlock_unlock(&_holdedProxiesLock);
    return object;
}

-(NSArray*)allKeysForHoldedProxy:(id)object
{
    pthread_rwlock_wrlock(&_holdedProxiesLock);
    NSArray* result = [_holdedProxies allKeysForObject:object];
    pthread_rwlock_unlock(&_holdedProxiesLock);
    return result;
}

-(id)holdedProxyForKey:(NSString*)key
{
    pthread_rwlock_wrlock(&_holdedProxiesLock);
    id result = [_holdedProxies objectForKey:key];
    pthread_rwlock_unlock(&_holdedProxiesLock);
    return result;
}


@end
