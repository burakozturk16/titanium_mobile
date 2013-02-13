/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UISWITCH

#import "TiUISwitchProxy.h"

@implementation TiUISwitchProxy

+(NSSet*)transferableProperties
{
    NSSet *common = [TiViewProxy transferableProperties];
    return [common setByAddingObjectsFromSet:[[NSSet alloc] initWithObjects:@"enabled",
                                              @"value", nil]];
}

-(UIViewAutoresizing)verifyAutoresizing:(UIViewAutoresizing)suggestedResizing
{
	return suggestedResizing & ~(UIViewAutoresizingFlexibleHeight|UIViewAutoresizingFlexibleWidth);
}

USE_VIEW_FOR_VERIFY_HEIGHT
USE_VIEW_FOR_VERIFY_WIDTH

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
