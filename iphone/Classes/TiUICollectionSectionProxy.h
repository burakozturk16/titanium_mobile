/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiParentingProxy.h"

@class TiViewProxy;
@class TiUICollectionView;
@protocol TiUICollectionViewDelegate <NSObject>
@required

- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block;
- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block animated:(BOOL)animated;
- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block maintainPosition:(BOOL)maintain;
- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block animated:(BOOL)animated maintainPosition:(BOOL)maintain;
- (void)dispatchBlock:(void(^)(UICollectionView *tableView))block;
- (id)dispatchBlockWithResult:(id(^)(void))block;
//-(void)hideDeleteButton:(id)args;

@end

@interface TiUICollectionSectionProxy : TiParentingProxy < TiUICollectionViewDelegate >

@property (nonatomic, readwrite, assign) id<TiUICollectionViewDelegate> delegate;
@property (nonatomic, readwrite, assign) NSUInteger sectionIndex;

// Private API. Used by CollectionView directly. Not for public comsumption
- (NSDictionary *)itemAtIndex:(NSUInteger)index;
- (void) deleteItemAtIndex:(NSUInteger)index;
- (void) addItem:(NSDictionary*)item atIndex:(NSUInteger)index;
- (BOOL)isHidden;
-(id)length;

- (void)appendItems:(id)args;
- (void)insertItemsAt:(id)args;
- (void)replaceItemsAt:(id)args;
- (void)deleteItemsAt:(id)args;
- (void)updateItemAt:(id)args;
- (id)getItemAt:(id)args;
-(TiViewProxy*)sectionViewForLocation:(NSString*)location inCollectionView:(TiUICollectionView*)listView;

// Public API
@property (nonatomic, readonly) NSUInteger itemCount;
@property (nonatomic, readonly) NSArray *items;
@property (nonatomic, readwrite, copy) NSString *headerTitle;
@property (nonatomic, readwrite, copy) NSString *footerTitle;

@end

#endif
