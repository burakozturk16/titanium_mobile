/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UINAVIGATIONWINDOW
#import "TiUINavigationWindow.h"
#import "TiUINavigationWindowProxy.h"

@implementation TiUINavigationWindow

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [(TiUINavigationWindowProxy*)[self proxy] setFrame:bounds];
    [super frameSizeChanged:frame bounds:bounds];
}

@end
#endif