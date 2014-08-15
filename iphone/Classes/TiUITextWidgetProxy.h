/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITEXTWIDGET) || defined(USE_TI_UITEXTAREA) || defined(USE_TI_UITEXTFIELD)

#import "TiViewProxy.h"

@interface TiUITextWidgetProxy : TiViewProxy<TiKeyboardFocusableView> {
	BOOL _suppressFocusEvents;
@private

}

//Internal values
-(void)noteValueChange:(NSString *)newValue;

-(BOOL)selectNextTextWidget;

@property(nonatomic,readwrite,assign)	BOOL suppressFocusEvents;

@end

#endif
