/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiUICollectionView.h"

@class TiUICollectionWrapperViewProxy;
@interface TiUICollectionWrapperView : UICollectionReusableView<TiProxyDelegate>
{
}

@property (nonatomic,readwrite,retain) TiUICollectionWrapperViewProxy *proxy;
@property (nonatomic, retain) TiUIView *viewHolder;
@property (nonatomic, readwrite, retain) NSDictionary *dataItem;

- (id)initWithProxy:(TiUICollectionWrapperViewProxy *)proxy;
-(void)updateProxy:(TiUICollectionWrapperViewProxy *)viewProxy forIndexPath:(NSIndexPath*)indexPath;

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
-(void)configurationStart;
-(void)configurationSet;
@end

#endif