/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiScrollingViewProxy.h"
#import "TiUIListSectionProxy.h"

@class TiTableView;
@class TiUIListItemProxy;
@interface TiUIListViewProxy : TiScrollingViewProxy < TiUIListViewDelegate >

@property (nonatomic, readonly) NSArray *sections;
@property (nonatomic, readonly) NSNumber *sectionCount;
@property (nonatomic, readonly) NSDictionary *propertiesForItems;
@property (nonatomic, readonly) NSDictionary *measureProxies;
@property (nonatomic, readonly) NSDictionary *templates;
@property (nonatomic, assign) BOOL autoResizeOnImageLoad;

- (TiUIListSectionProxy *)sectionForIndex:(NSUInteger)index;
- (void) deleteSectionAtIndex:(NSUInteger)index;
- (void) setMarker:(id)args;
-(BOOL)shouldHighlightCurrentListItem;
-(BOOL)isEditing;
- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath;
-(TiTableView*)tableView;
-(void)didOverrideEvent:(NSString*)type forItem:(TiUIListItemProxy*)item;
@end

@interface TiUIListViewProxy (internal)
-(void)willDisplayCell:(NSIndexPath*)indexPath;
@end
#endif
