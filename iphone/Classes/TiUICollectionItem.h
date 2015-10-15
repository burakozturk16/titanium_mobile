/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import <UIKit/UIKit.h>
#import "TiUICollectionView.h"
#import "TiUICollectionItemProxy.h"

enum {
	TiUICollectionItemTemplateStyleCustom = -1
};

typedef enum
{
    TiGroupedCollectionItemPositionTop,
    TiGroupedCollectionItemPositionMiddle,
    TiGroupedCollectionItemPositionBottom,
	TiGroupedCollectionItemPositionSingleLine
} TiGroupedCollectionItemPosition;


@interface TiUICollectionItem : UICollectionViewCell<TiProxyDelegate>
{
}

@property (nonatomic, readonly) NSInteger templateStyle;
@property (nonatomic, readonly) TiUICollectionItemProxy *proxy;
@property (nonatomic, readonly) TiUIView *viewHolder;
@property (nonatomic, readwrite, retain) NSDictionary *dataItem;

- (id)initWithProxy:(TiUICollectionItemProxy *)proxy;

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
-(void)configurationStart;
-(void)configurationSet;
//- (BOOL) hasSwipeButtons;
@end

#endif
