/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiScrollingView.h"
#import "TiUICollectionViewProxy.h"
@class TiCollectionView;
@interface TiUICollectionView : TiScrollingView <UICollectionViewDelegate, UICollectionViewDataSource, UIGestureRecognizerDelegate, UISearchBarDelegate, UISearchDisplayDelegate, TiScrolling, TiProxyObserver>
{
    BOOL allowsSelection;
}
#pragma mark - Private APIs

@property (nonatomic, readonly) TiCollectionView *tableView;
@property (nonatomic, readonly) BOOL isSearchActive;

- (void)updateSearchResults:(id)unused;
- (void)setContentInsets_:(id)value withObject:(id)props;
- (void)deselectAll:(BOOL)animated;
- (void)updateIndicesForVisibleRows;

+ (UITableViewRowAnimation)animationStyleForProperties:(NSDictionary*)properties;
-(BOOL)shouldHighlightCurrentCollectionItem;
- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath;

@end

#endif
