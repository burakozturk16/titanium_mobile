/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITEXTFIELD

#import "TiUITextWidget.h"


@interface TiTextField : UITextField
{
	UITextFieldViewMode leftMode;
	UITextFieldViewMode rightMode;
	
	BOOL becameResponder;
    TiUIView * touchHandler;
}

@property(nonatomic,readwrite,assign) UIEdgeInsets padding;

@property(nonatomic,readonly) BOOL becameResponder;

-(void)setTouchHandler:(TiUIView*)handler;

@end

@interface TiUITextField : TiUITextWidget <UITextFieldDelegate>
{
@private
}

#pragma mark Internal 

-(TiTextField*)textWidgetView;
-(void)setPadding:(UIEdgeInsets)inset;

@end

#endif