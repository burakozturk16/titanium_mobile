/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiBase.h"
#import "TiUIView.h"
#import "ECSlidingViewController.h"

/** @constant PanningMode */
typedef enum {
    PanningModeNone,
    PanningModeFullscreen,
    PanningModeNavBar,
    PanningModeNonScrollView,
    PanningModeBorders
} PanningMode;

@interface TiUISlideMenu : TiUIView{

@private
	ECSlidingViewController *controller;
    CALayer* shadowLayer;
    CGFloat shadowWidth;
    PanningMode panningMode;
    
    TiViewProxy* leftView;
    TiViewProxy* rightView;
    TiViewProxy* centerView;
}
-(ECSlidingViewController*)controller;

@end
