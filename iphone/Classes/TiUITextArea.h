/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITEXTAREA

#import "TiUITextWidget.h"

@interface TiUITextViewImpl : UITextView {
@private
    TiUIView * touchHandler;
    UIView * touchedContentView;
}
@property (nonatomic, strong) NSString *placeholder;
@property (nonatomic, strong) UIColor *placeholderColor;
@property (nonatomic) BOOL displayPlaceHolder;
@end

@interface TiUITextArea : TiUITextWidget <UITextViewDelegate>
{
@private
	BOOL becameResponder;
    BOOL returnActive;
    BOOL handleLinks;
    NSRange lastSelectedRange;
}

@property(nonatomic,readonly) BOOL becameResponder;
@property(nonatomic,assign) UIEdgeInsets padding;
@end

#endif