/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiViewProxy.h"

@class TiUICollectionItem;
@class TiUICollectionViewProxy;

@interface TiUICollectionItemProxy : TiViewProxy < TiViewEventOverrideDelegate >

@property (nonatomic, readwrite, assign) TiUICollectionItem *listItem;
@property (nonatomic, readwrite, retain) NSIndexPath *indexPath;

- (id)initWithCollectionViewProxy:(TiUICollectionViewProxy *)listViewProxy inContext:(id<TiEvaluator>)context;
//- (NSDictionary *)bindings;
- (void)setDataItem:(NSDictionary *)dataItem;
-(CGFloat)sizeWidthForDecorations:(CGFloat)oldWidth forceResizing:(BOOL)force;
-(void)cleanup;

-(void)deregisterProxy:(id<TiEvaluator>)context;
-(BOOL)shouldHighlight;
@end

#endif
