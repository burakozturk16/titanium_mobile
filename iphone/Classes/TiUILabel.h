/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILABEL

#import "TiUIView.h"
#import "TiLabel.h"

@interface TiUILabel : TiUIView<LayoutAutosizing, TTTAttributedLabelDelegate> {
@private
	TiLabel *label;
    BOOL needsSetText;
}

-(void)setAttributedTextViewContent;
- (CGSize)suggestedFrameSizeToFitEntireStringConstraintedToSize:(CGSize)size;
-(TiLabel*)label;
-(void)setPadding:(UIEdgeInsets)inset;
//-(void)setReusing:(BOOL)value;
-(NSInteger)characterIndexAtPoint:(CGPoint)p;
@end

#endif
