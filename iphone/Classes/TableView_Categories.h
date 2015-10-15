
#ifdef USE_TI_UITABLEVIEW
#import "TiViewProxy.h"

@interface TiUIView(TiUITableView)
/*
 Tells the view to change its proxy to the new one provided.
 @param newProxy The new proxy to set on the view.
 @param deep true for deep transfer
 */
-(void)transferProxy:(TiViewProxy*)newProxy deep:(BOOL)deep;
-(void)transferProxy:(TiViewProxy*)newProxy;
-(void)transferProxy:(TiViewProxy*)newProxy withBlockBefore:(void (^)(TiViewProxy* proxy))blockBefore
withBlockAfter:(void (^)(TiViewProxy* proxy))blockAfter deep:(BOOL)deep;
/*
 Returns whether the view tree matches proxy tree for later transfer.
 @param proxy The proxy to validate view tree with.
 @param deep true for deep validation
 */
-(BOOL)validateTransferToProxy:(TiViewProxy*)proxy deep:(BOOL)deep;
@end
#endif