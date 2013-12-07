/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILABEL

#import "TiUIView.h"
#import "TTTAttributedLabel.h"


@interface TiUILabel : TiUIView<LayoutAutosizing, TTTAttributedLabelDelegate> {
@private
	TDTTTAttributedLabel *label;
    CGRect padding;
    UIEdgeInsets textPadding;
    CGRect initialLabelFrame;
    BOOL needsUpdateBackgroundImageFrame;
    BOOL needsPadLabel;
    BOOL needsSetText;
}

@property(nonatomic,getter=isHighlighted) BOOL     highlighted;          // default is NO

-(void)setAttributedTextViewContent;
- (CGSize)suggestedFrameSizeToFitEntireStringConstraintedToSize:(CGSize)size;
-(TDTTTAttributedLabel*)label;
@end


#endif
