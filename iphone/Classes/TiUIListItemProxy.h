/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiViewProxy.h"

@class TiUIListItem;
@class TiUIListViewProxy;

@interface TiUIListItemProxy : TiViewProxy < TiViewEventOverrideDelegate >

@property (nonatomic, readwrite, assign) TiUIListItem *listItem;
@property (nonatomic, readwrite, retain) NSIndexPath *indexPath;

- (id)initWithListViewProxy:(TiUIListViewProxy *)listViewProxy inContext:(id<TiEvaluator>)context;
//- (NSDictionary *)bindings;
- (void)setDataItem:(NSDictionary *)dataItem;
-(CGFloat)sizeWidthForDecorations:(CGFloat)oldWidth forceResizing:(BOOL)force;
-(void)cleanup;

-(void)deregisterProxy:(id<TiEvaluator>)context;
-(BOOL)shouldHighlight;
-(TiUIListViewProxy*)listViewProxy;
@end

#endif
