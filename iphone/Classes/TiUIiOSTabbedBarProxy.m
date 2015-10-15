/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UIIOSTABBEDBAR) || defined(USE_TI_UITABBEDBAR)
#import "TiUIiOSTabbedBarProxy.h"
#import "TiUIButtonBar.h"

@implementation TiUIiOSTabbedBarProxy


-(NSArray*)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"labels",@"style"]] retain];;
    });
    return keySequence;
}


-(NSString*)apiName
{
    return @"Ti.UI.iOS.TabbedBar";
}

-(TiUIView*)newView
{
	TiUIButtonBar * result = [[TiUIButtonBar alloc] init];
	[result setTabbedBar:YES];
	return result;
}

USE_VIEW_FOR_CONTENT_SIZE

-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoSize;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

@end
#endif