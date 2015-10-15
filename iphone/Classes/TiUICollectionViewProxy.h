/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiScrollingViewProxy.h"
#import "TiUICollectionSectionProxy.h"

@class TiTableView;
@class TiUICollectionItemProxy;
@interface TiUICollectionViewProxy : TiScrollingViewProxy < TiUICollectionViewDelegate >

@property (nonatomic, readonly) NSArray *sections;
@property (nonatomic, readonly) NSNumber *sectionCount;
@property (nonatomic, readonly) NSDictionary *propertiesForItems;
@property (nonatomic, assign) BOOL autoResizeOnImageLoad;
@property (nonatomic, readonly) NSDictionary *measureProxies;
@property (nonatomic, readonly) NSDictionary *templates;

- (TiUICollectionSectionProxy *)sectionForIndex:(NSUInteger)index;
- (void) deleteSectionAtIndex:(NSUInteger)index;
- (void) setMarker:(id)args;
-(BOOL)shouldHighlightCurrentCollectionItem;
- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath;
-(TiTableView*)tableView;
-(void)didOverrideEvent:(NSString*)type forItem:(TiUICollectionItemProxy*)item;
@end

@interface TiUICollectionViewProxy (internal)
-(void)willDisplayCell:(NSIndexPath*)indexPath;
@end
#endif
