/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIBUTTON

#import "TiUIView.h"

@interface UIButton (backgroundColors)
@property(nonatomic,retain)	UIColor* backgroundSelectedColor;
@property(nonatomic,retain)	UIColor* backgroundDisabledColor;
@property(nonatomic,retain)	UIColor* backgroundHighlightedColor;

-(void)updateBackgroundColor;
@end

@interface TiUIButton : TiUIView {
@private
	UIButton *button;
	
	UIImage * backgroundImageCache;
	UIImage * backgroundImageUnstretchedCache;

	int style;
	
    BOOL touchStarted;

	UIEdgeInsets titlePadding;
    
}

-(UIButton*)button;

-(void)setEnabled_:(id)value;

@end

#endif