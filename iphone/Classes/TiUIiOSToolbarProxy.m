/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UIIOSTOOLBAR) || defined(USE_TI_UITOOLBAR)


#import "TiUIiOSToolbarProxy.h"
#import "TiUIiOSToolbar.h"

@implementation TiUIiOSToolbarProxy

-(NSArray *)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"barColor"]] retain];;
    });
    return keySequence;
}


//USE_VIEW_FOR_VERIFY_HEIGHT

//-(UIViewAutoresizing)verifyAutoresizing:(UIViewAutoresizing)suggestedResizing
//{
//	return suggestedResizing & ~UIViewAutoresizingFlexibleHeight;
//}

-(NSString*)apiName
{
    return @"Ti.UI.iOS.Toolbar";
}

-(UIToolbar*)toolbar
{
	TiUIiOSToolbar *theview = (TiUIiOSToolbar*) [self view];
	return [theview toolBar];
}

-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

USE_VIEW_FOR_CONTENT_SIZE

@end

#endif
