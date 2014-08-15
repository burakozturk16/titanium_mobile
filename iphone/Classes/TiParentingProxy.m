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

	[super dealloc];
}


-(void)_initWithProperties:(NSDictionary*)properties
{
    if (!_unarchiving && ([properties objectForKey:@"properties"] || [properties objectForKey:@"childTemplates"])) {
        [self unarchiveFromDictionary:properties rootProxy:self];
        return;
    }
	[super _initWithProperties:properties];
}

-(void)_destroy
{
	pthread_rwlock_wrlock(&childrenLock);
	[children makeObjectsPerformSelector:@selector(setParent:) withObject:nil];
	RELEASE_TO_NIL(children);
	pthread_rwlock_unlock(&childrenLock);
	pthread_rwlock_destroy(&childrenLock);
	[super _destroy];
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

-(BOOL)_hasListeners:(NSString *)type
{
	return [self _hasListeners:type checkParent:YES];
}

-(NSArray*)children
{
    if (childrenCount == 0) {
        return [NSMutableArray array];
    }
    if (![NSThread isMainThread]) {
        __block NSArray* result = nil;
        TiThreadPerformOnMainThread(^{
            result = [[self children] retain];
        }, YES);
        return [result autorelease];
    }
    
	pthread_rwlock_rdlock(&childrenLock);
    NSArray* copy = [children mutableCopy];
	pthread_rwlock_unlock(&childrenLock);
	return ((copy != nil) ? [copy autorelease] : [NSMutableArray array]);
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
}

-(void)addProxy:(id)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout
{
    ENSURE_SINGLE_ARG_OR_NIL(child, TiProxy)
    [self rememberProxy:child];
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
    [child setParent:self];
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

-(void)childRemoved:(TiProxy*)child
{
}

-(void)removeProxy:(id)child
{
    ENSURE_SINGLE_ARG_OR_NIL(child, TiProxy)
    pthread_rwlock_wrlock(&childrenLock);
	if ([children containsObject:child]) {
		[children removeObject:child];
	}
	pthread_rwlock_unlock(&childrenLock);
    
	[child setParent:nil];
    [self childRemoved:child];
   	[self forgetProxy:child];
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
        [self childRemoved:theChild];
        [self forgetProxy:theChild];
    }
	[childrenCopy release];
}


-(void)_listenerAdded:(NSString*)type count:(int)count
{
    //TIMOB-15991 Update children as well
	NSArray* childrenArray = [[self children] retain];
    for (id child in childrenArray) {
        if ([child respondsToSelector:@selector(parentListenersChanged)]) {
            [child parentListenersChanged];
        }
    }
	[childrenArray release];
}

-(void)_listenerRemoved:(NSString*)type count:(int)count
{
    //TIMOB-15991 Update children as well
    NSArray* childrenArray = [[self children] retain];
    for (id child in childrenArray) {
        if ([child respondsToSelector:@selector(parentListenersChanged)]) {
            [child parentListenersChanged];
        }
    }
    [childrenArray release];
}


- (TiProxy *)createChildFromObject:(id)object
{
    TiProxy *child = nil;
    NSString* bindId = nil;
    if ([object isKindOfClass:[NSDictionary class]]) {
        bindId = [object valueForKey:@"bindId"];
        id<TiEvaluator> context = self.executionContext;
        if (context == nil) {
            context = self.pageContext;
        }
        child = [[self class] createFromDictionary:object rootProxy:self inContext:context];
        [self rememberProxy:child];
        [context.krollContext invokeBlockOnThread:^{
            [child forgetSelf];
        }];
   }
    else if(([object isKindOfClass:[TiProxy class]]))
    {
        child = (TiProxy *)object;
        bindId = [object valueForUndefinedKey:@"bindId"];
    }
    if (child && bindId) {
        [self setValue:child forKey:bindId];
    }
    return child;
}


- (void)unarchiveFromDictionary:(NSDictionary*)dictionary rootProxy:(TiProxy*)rootProxy
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
        TiProxy *child = [rootProxy createChildFromObject:childTemplate];
		if (child != nil) {
			[self addProxy:child atIndex:-1 shouldRelayout:NO];
            [context.krollContext invokeBlockOnThread:^{
				[child forgetSelf];
			}];
		}
	}];
	_unarchiving = NO;
}

// Returns protected proxy, caller should do forgetSelf.
+ (TiProxy *)unarchiveFromTemplate:(id)viewTemplate_ inContext:(id<TiEvaluator>)context withEvents:(BOOL)withEvents
{
	TiProxyTemplate *viewTemplate = [TiProxyTemplate templateFromViewTemplate:viewTemplate_];
	if (viewTemplate == nil) {
		return;
	}
	
	if (viewTemplate.type != nil) {
		TiProxy *proxy = [[self class] createProxy:[[self class] proxyClassFromString:viewTemplate.type] withProperties:nil inContext:context];
		[context.krollContext invokeBlockOnThread:^{
			[context registerProxy:proxy];
			[proxy rememberSelf];
		}];
		[proxy unarchiveFromTemplate:viewTemplate withEvents:withEvents];
		return proxy;
	}
	return nil;
}

- (void)unarchiveFromTemplate:(id)viewTemplate_ withEvents:(BOOL)withEvents inContext:(id<TiEvaluator>)context
{
	TiProxyTemplate *viewTemplate = [TiProxyTemplate templateFromViewTemplate:viewTemplate_];
	if (viewTemplate == nil) {
		return;
	}
	[super unarchiveFromTemplate:viewTemplate withEvents:withEvents inContext:context];
	
	[viewTemplate.childTemplates enumerateObjectsUsingBlock:^(TiProxyTemplate *childTemplate, NSUInteger idx, BOOL *stop) {
		TiProxy *child = [[self class] unarchiveFromTemplate:childTemplate inContext:context withEvents:withEvents];
		if (child != nil) {
    
			[self addProxy:child atIndex:-1 shouldRelayout:NO];
            [context.krollContext invokeBlockOnThread:^{
				[child forgetSelf];
			}];
		}
	}];
}

-(id)getNextChildrenOfClass:(Class)theClass afterChild:(TiProxy*)child
{
    id result = nil;
    NSArray* subproxies = [self children];
    NSInteger index=[subproxies indexOfObject:child];
    if(NSNotFound != index) {
        for (int i = index + 1; i < [subproxies count] ; i++) {
            TiProxy* obj = [subproxies objectAtIndex:i];
            if ([obj isKindOfClass:theClass] && [obj canBeNextResponder]) {
                    return obj;
            }
        }
    }
    return result;
}

@end
