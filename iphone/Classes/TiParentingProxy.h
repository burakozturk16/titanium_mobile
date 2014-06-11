#import "TiProxy.h"

@interface TiParentingProxy : TiProxy
{
    #pragma mark Parent/Children relationships
    TiParentingProxy *parent;
    pthread_rwlock_t childrenLock;
    NSMutableArray *children;
    int childrenCount;
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

/**
 Tells the view proxy to add a child proxy.
 @param arg A single proxy to add or NSArray of proxies.
 */
-(void)add:(id)arg;

/**
 Subclass can directly use that method to handle it all!
 */

-(void)addProxy:(id)child atIndex:(NSInteger)position shouldRelayout:(BOOL)shouldRelayout;

/**
 Tells the view proxy to remove a child proxy.
 @param arg A single proxy to remove.
 */
-(void)remove:(id)arg;
-(void)removeProxy:(id)child;

/**
 Tells the view proxy to remove all child proxies.
 @param arg Ignored.
 */
-(void)removeAllChildren:(id)arg;

@end
