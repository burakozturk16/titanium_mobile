#import "TiProxy.h"

@interface TiParentingProxy : TiProxy
{
    #pragma mark Parent/Children relationships
@protected
    TiParentingProxy *parent;
    pthread_rwlock_t childrenLock;
    NSMutableArray *children;
    NSUInteger childrenCount;
    NSMutableDictionary* _holdedProxies;
    pthread_rwlock_t _holdedProxiesLock;
}

/**
 Returns children view proxies for the proxy.
 */
@property(nonatomic,readonly) NSArray *children;

/**
 Provides access to parent proxy of the view proxy.
 @see add:
 @see remove:
 @see children
 */
@property(nonatomic, assign) TiParentingProxy *parent;
@property(nonatomic, assign) TiParentingProxy *parentForBubbling;

-(BOOL)_hasListeners:(NSString *)type checkParent:(BOOL)check;
-(BOOL)_hasListenersIgnoreBubble:(NSString *)type;
/**
 Tells the view proxy to add a child proxy.
 @param arg A single proxy to add or NSArray of proxies.
 */
-(void)add:(id)arg;

/**
 Subclass can directly use that method to handle it all!
 */

-(void)addProxy:(id)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout;
- (TiProxy *)createChildFromObject:(id)object;
- (TiProxy *)createChildFromObject:(id)object rootProxy:(TiParentingProxy*)rootProxy;

/**
 Tells the view proxy to remove a child proxy.
 @param arg A single proxy to remove.
 */
-(void)remove:(id)arg;
-(void)removeProxy:(id)child;
-(void)removeProxy:(id)child shouldDetach:(BOOL)shouldDetach;

/**
 Tells the view proxy to remove all child proxies.
 @param arg Ignored.
 */
-(void)removeAllChildren:(id)arg;

-(void)childAdded:(TiProxy*)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout;
-(void)childRemoved:(TiProxy*)child wasChild:(BOOL)wasChild shouldDetach:(BOOL)shouldDetach;
/**
 get the next children of a certain class starting from a child
 @param class The child class looked for
 @param child The child view
 */
-(id)getNextChildrenOfClass:(Class)theClass afterChild:(TiProxy*)child;
-(BOOL)containsChild:(TiProxy*)child;
-(TiProxy*)childAt:(NSInteger)index;
-(NSUInteger)childrenCount;
-(void)runBlock:(void (^)(TiProxy* proxy))block recursive:(BOOL)recursive;
-(void)makeChildrenPerformSelector:(SEL)selector withObject:(id)object;

-(id)addObjectToHold:(id)value forKey:(NSString*)key;
-(id)addObjectToHold:(id)value forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout;
-(TiProxy*)addProxyToHold:(TiProxy*)proxy forKey:(NSString*)key;
-(TiProxy*)addProxyToHold:(TiProxy*)proxy forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout;
-(TiProxy*)addProxyToHold:(TiProxy*)proxy setParent:(BOOL)setParent forKey:(NSString*)key shouldRelayout:(BOOL)shouldRelayout;
-(id)removeHoldedProxyForKey:(NSString*)key;
-(id)holdedProxyForKey:(NSString*)key;
-(NSArray*)allKeysForHoldedProxy:(id)object;
@end
