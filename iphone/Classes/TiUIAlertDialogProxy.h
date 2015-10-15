/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"

@class TiAlertController;

@interface TiUIAlertDialogProxy : TiProxy<UIAlertViewDelegate> {
@private
    UIAlertView *alert;
    TiAlertController* alertController;
    BOOL persistentFlag;
    BOOL hideOnClick;
    NSInteger cancelIndex;
    NSInteger destructiveIndex;
    NSInteger style;
}

-(void)show:(id)args;
-(void)hide:(id)args;

@end
