/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiScrollingView.h"
#import "TiUIListViewProxy.h"
#import "MGSwipeTableCell.h"

@interface TiUIListView : TiScrollingView <MGSwipeTableCellDelegate, UITableViewDelegate, UITableViewDataSource, UIGestureRecognizerDelegate, UISearchBarDelegate, UISearchDisplayDelegate, TiScrolling, TiProxyObserver, TiUIListViewDelegateView >

#pragma mark - Private APIs

@property (nonatomic, readonly) TiTableView *tableView;
@property (nonatomic, readonly) BOOL isSearchActive;
@property (nonatomic, readonly) BOOL editing;

- (void)setContentInsets_:(id)value withObject:(id)props;
- (void)deselectAll:(BOOL)animated;
- (void)updateIndicesForVisibleRows;

+ (UITableViewRowAnimation)animationStyleForProperties:(NSDictionary*)properties;
-(BOOL)shouldHighlightCurrentListItem;
- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath;
-(NSMutableArray*)visibleCellsProxies;

@end

#endif
