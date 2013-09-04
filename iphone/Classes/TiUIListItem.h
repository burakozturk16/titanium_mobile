/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import <UIKit/UIKit.h>
#import "TiUIListView.h"
#import "TiUIListItemProxy.h"

enum {
	TiUIListItemTemplateStyleCustom = -1
};

typedef enum
{
    TiGroupedListItemPositionTop,
    TiGroupedListItemPositionMiddle,
    TiGroupedListItemPositionBottom,
	TiGroupedListItemPositionSingleLine
} TiGroupedListItemPosition;


@interface TiUIListItem : UITableViewCell<TiProxyDelegate>
{
}

@property (nonatomic, readonly) NSInteger templateStyle;
@property (nonatomic, readonly) TiUIListItemProxy *proxy;
@property (nonatomic, readonly) TiUIView *viewHolder;
@property (nonatomic, readwrite, retain) NSDictionary *dataItem;

- (id)initWithStyle:(UITableViewCellStyle)style position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier proxy:(TiUIListItemProxy *)proxy;
- (id)initWithProxy:(TiUIListItemProxy *)proxy position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier;

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
- (void)setPosition:(int)position isGrouped:(BOOL)grouped;


/**
 The convenience method to raise an exception for the view.
 @param reason The exception reason.
 @param subreason The exception subreason.
 @param location The exception location.
 */
+(void)throwException:(NSString *) reason subreason:(NSString*)subreason location:(NSString *)location;

-(void)throwException:(NSString *) reason subreason:(NSString*)subreason location:(NSString *)location;
@end

#endif
