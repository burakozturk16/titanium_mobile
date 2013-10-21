/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITABLEVIEWSEPARATORSTYLE) || defined(USE_TI_UIIPHONELISTVIEWSEPARATORSTYLE)

#import "TiUITableViewSeparatorStyleProxy.h"

@implementation TiUITableViewSeparatorStyleProxy

-(NSString*)apiName
{
    return @"Ti.UI.iPhone.TableViewSeparatorStyle";
}

MAKE_SYSTEM_PROP(NONE,UITableViewCellSeparatorStyleNone);
MAKE_SYSTEM_PROP(SINGLE_LINE,UITableViewCellSeparatorStyleSingleLine);


@end

#endif