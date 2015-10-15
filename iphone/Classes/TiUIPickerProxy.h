/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIPICKER

#import "TiViewProxy.h"

@class TiUIPicker;
@interface TiUIPickerProxy : TiViewProxy {
}

-(void)reloadColumn:(id)column atIndex:(NSUInteger)index;
-(TiUIPicker*)picker;

@end

#endif
