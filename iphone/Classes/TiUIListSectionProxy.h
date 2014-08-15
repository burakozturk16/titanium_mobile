/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiParentingProxy.h"

@class TiViewProxy;
@class TiUIListView;
@protocol TiUIListViewDelegate <NSObject>
@required

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block;
- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block animated:(BOOL)animated;
- (id)dispatchBlockWithResult:(id(^)(void))block;

@end

@interface TiUIListSectionProxy : TiParentingProxy < TiUIListViewDelegate >

@property (nonatomic, readwrite, assign) id<TiUIListViewDelegate> delegate;
@property (nonatomic, readwrite, assign) NSUInteger sectionIndex;

// Private API. Used by ListView directly. Not for public comsumption
- (NSDictionary *)itemAtIndex:(NSUInteger)index;
- (void) deleteItemAtIndex:(NSUInteger)index;
- (void) addItem:(NSDictionary*)item atIndex:(NSUInteger)index;
- (BOOL)isHidden;

- (void)appendItems:(id)args;
- (void)insertItemsAt:(id)args;
- (void)replaceItemsAt:(id)args;
- (void)deleteItemsAt:(id)args;
- (void)updateItemAt:(id)args;
- (id)getItemAt:(id)args;
-(TiViewProxy*)sectionViewForLocation:(NSString*)location inListView:(TiUIListView*)listView;

// Public API
@property (nonatomic, readonly) NSUInteger itemCount;
@property (nonatomic, readonly) NSArray *items;
@property (nonatomic, readwrite, copy) NSString *headerTitle;
@property (nonatomic, readwrite, copy) NSString *footerTitle;

@end

#endif
