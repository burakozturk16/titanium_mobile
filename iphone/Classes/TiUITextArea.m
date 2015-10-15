/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITEXTAREA

#import "TiUITextArea.h"
#import "TiUITextAreaProxy.h"

#import "TiUtils.h"
#import "Webcolor.h"
#import "TiApp.h"

#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v) ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)
#define is_iOS7 SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"7.0")
#define is_iOS8 SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(@"8.0")

@interface TiUITextViewImpl()
- (void)handleTextViewDidChange;
@property(nonatomic,assign) BOOL inLayout;
@property(nonatomic,assign) BOOL ignoreSystemContentOffset;
@end


@implementation TiUITextViewImpl
{
    BOOL becameResponder;
    BOOL _nonSystemContentOffset;
}

-(id)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame]) {
        _ignoreSystemContentOffset = YES;
    }
    return self;
}

- (void)setContentOffset:(CGPoint)contentOffset
{
    [super setContentOffset:contentOffset];
}

- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated
{
    [super setContentOffset:contentOffset animated:animated];
}
- (void)scrollRectToVisible:(CGRect)rect animated:(BOOL)animated
{
    [super scrollRectToVisible:rect animated:animated];
}
-(void)setTouchHandler:(TiUIView*)handler
{
    //Assign only. No retain
    touchHandler = handler;
}

- (BOOL)touchesShouldBegin:(NSSet *)touches withEvent:(UIEvent *)event inContentView:(UIView *)view
{
    //If the content view is of type TiUIView touch events will automatically propagate
    //If it is not of type TiUIView we will fire touch events with ourself as source
    if ([view isKindOfClass:[TiUIView class]]) {
        touchedContentView= view;
    }
    else {
        touchedContentView = nil;
    }
    return [super touchesShouldBegin:touches withEvent:event inContentView:view];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    //When userInteractionEnabled is false we do nothing since touch events are automatically
    //propagated. If it is dragging do not do anything.
    //The reason we are not checking tracking (like in scrollview) is because for some
    //reason UITextView always returns true for tracking after the initial focus
    if (!self.dragging && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [touchHandler processTouchesBegan:touches withEvent:event];
 	}
	[super touchesBegan:touches withEvent:event];
}
- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (!self.dragging && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [touchHandler processTouchesMoved:touches withEvent:event];
    }
	[super touchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (!self.dragging && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [touchHandler processTouchesEnded:touches withEvent:event];
    }
	[super touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    if (!self.dragging && self.userInteractionEnabled && (touchedContentView == nil) ) {
        [touchHandler processTouchesCancelled:touches withEvent:event];
    }
    [super touchesCancelled:touches withEvent:event];
}

-(BOOL)canBecomeFirstResponder
{
	return self.isEditable;
}

-(BOOL)resignFirstResponder
{
    if ([super resignFirstResponder])
    {
        if (becameResponder) {
            becameResponder = NO;
        }
        return YES;
    }
    
	return NO;
}

-(BOOL)becomeFirstResponder
{
    if (self.isEditable && self.canBecomeFirstResponder) {
        if ([super becomeFirstResponder])
        {
            becameResponder = YES;
            return YES;
        }
    }
    return NO;
}


-(BOOL)isFirstResponder
{
	if (becameResponder) return YES;
	return [super isFirstResponder];
}

- (void)handleTextViewDidChange {
    
    // Display (or not) the placeholder string
    
    BOOL wasDisplayingPlaceholder = self.displayPlaceHolder;
    self.displayPlaceHolder = self.text.length == 0;
	
    if (wasDisplayingPlaceholder != self.displayPlaceHolder) {
        [self setNeedsDisplay];
    }
    [self updateKeyboardInsetWithScroll:NO animated:NO];
}

- (void)setText:(NSString *)text {
    [super setText:text];
    self.displayPlaceHolder = text.length == 0;
}

-(void)setNonSystemContentOffset:(CGPoint)offset {
    self.ignoreSystemContentOffset = NO;
    [self setContentOffset:offset];
    self.ignoreSystemContentOffset = YES;
}

- (void)updateKeyboardInsetWithScroll:(BOOL)shouldScroll animated:(BOOL)animated  {
    CGRect keyboardRect = [[[TiApp app] controller] getKeyboardFrameInView:self];
    if (!CGRectIsEmpty(keyboardRect)) {
        CGFloat keyboardOriginY = keyboardRect.origin.y - self.bounds.origin.y;
        CGPoint offset = self.contentOffset;
        
        CGRect rectEnd = [self caretRectForPosition:self.endOfDocument];
        rectEnd.origin.y -= self.contentOffset.y;
        CGRect rectSelected = [self caretRectForPosition:self.selectedTextRange.end];
        rectSelected.origin.y -= self.contentOffset.y;
        
        UIEdgeInsets inset = self.contentInset;
        CGFloat overflow = rectSelected.origin.y + rectSelected.size.height -  keyboardOriginY;
        BOOL shouldUpdate = ( (inset.bottom == 0 && rectEnd.origin.y + rectEnd.size.height > keyboardOriginY) ||
                             overflow > 0);
        
        if (shouldUpdate) {
            CGFloat height = self.bounds.size.height;
            //set the contentInset for the full text (to be scrollable correctly)
            UIEdgeInsets newInset = UIEdgeInsetsMake(0, 0, height - keyboardOriginY, 0);
            self.contentInset = newInset;
            
            //if we scroll only scroll to caret
            if (overflow > 0) {
                CGPoint offset = self.contentOffset;
                offset.y += overflow + 7; // leave 7 pixels margin
                                          // Cannot animate with setContentOffset:animated: or caret will not appear
                [UIView animateWithDuration:.1 animations:^{
                    [self setNonSystemContentOffset:offset];
                }];
                return;
            }
            
        }
    } else {
        self.contentInset = UIEdgeInsetsZero;
        
    }
    if (shouldScroll) {
        [self scrollRectToVisible:[self caretRectForPosition:self.selectedTextRange.end] animated:animated];
    }
    else {
        CGFloat height = self.bounds.size.height;
        CGPoint contentOffset = self.contentOffset;
        CGRect rectSelected = [self caretRectForPosition:self.selectedTextRange.end];
        rectSelected.origin.y -= contentOffset.y;
        BOOL needsChange = NO;
        if (!self.isScrollEnabled && rectSelected.origin.y < 0) {
            contentOffset.y += rectSelected.origin.y;
            needsChange = YES;
        }
        else if (rectSelected.origin.y + rectSelected.size.height > height) {
            contentOffset.y += rectSelected.origin.y + rectSelected.size.height - height + 7; // leave 7 pixels margin
                                          // Cannot animate with setContentOffset:animated: or caret will not appear
            needsChange = YES;
        }
        if (needsChange) {
            [UIView animateWithDuration:.1 animations:^{
                [self setNonSystemContentOffset:contentOffset];
            }];
        }
    }
}


- (void)drawRect:(CGRect)rect
{
    [super drawRect:rect];
    if (self.displayPlaceHolder && self.placeholder && self.placeholderColor)
    {
        if ([self respondsToSelector:@selector(snapshotViewAfterScreenUpdates:)])
        {
            NSMutableParagraphStyle *paragraphStyle = [[NSMutableParagraphStyle alloc] init];
            paragraphStyle.alignment = self.textAlignment;
            [self.placeholder drawInRect:UIEdgeInsetsInsetRect(self.bounds, self.textContainerInset) withAttributes:@{NSFontAttributeName:self.font, NSForegroundColorAttributeName:self.placeholderColor, NSParagraphStyleAttributeName:paragraphStyle}];
            [paragraphStyle release];
        }
        else {
            [self.placeholderColor set];
            [self.placeholder drawInRect:self.bounds withFont:self.font];
        }
    }
}

-(void)setPlaceholder:(NSString *)placeholder
{
	_placeholder = placeholder;
	
	[self setNeedsDisplay];
}

@end


@interface TiUITextArea()

@property (nonatomic, strong) NSString *currentText;

@end

@implementation TiUITextArea
{
    NSTimer* _caretVisibilityTimer;
    UIEdgeInsets _padding;
    BOOL suppressReturn;
    NSInteger _maxLines;
}


@synthesize becameResponder, padding = _padding;

-(id)init {
    if (self = [super init])
    {
        _padding = UIEdgeInsetsZero;
        _maxLines = -1;
    }
    return self;
}

#pragma mark Internal

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    TiUITextViewImpl* ourView = (TiUITextViewImpl*)textWidgetView;
	[TiUtils setView:ourView positionRect:UIEdgeInsetsInsetRect(bounds, _padding)];
	[super frameSizeChanged:frame bounds:bounds];
    [ourView updateKeyboardInsetWithScroll:NO animated:NO];
    
}

-(UIView<UITextInputTraits>*)textWidgetView
{
    if (textWidgetView==nil)
    {
        TiUITextViewImpl *textViewImpl = [[TiUITextViewImpl alloc] initWithFrame:CGRectZero];
        textViewImpl.delaysContentTouches = NO;
        [textViewImpl setTouchHandler:self];
        textViewImpl.delegate = self;
        textViewImpl.displayPlaceHolder = YES;
        textViewImpl.placeholderColor = [UIColor grayColor];
        textViewImpl.backgroundColor = [UIColor clearColor];
        textViewImpl.textContainer.lineFragmentPadding = 0;
        textViewImpl.textContainerInset = UIEdgeInsetsZero;
        textViewImpl.contentMode = UIViewContentModeRedraw;
        [self addSubview:textViewImpl];
        [textViewImpl setContentInset:UIEdgeInsetsZero];
        self.clipsToBounds = YES;
        

        //Temporarily setting text to a blank space, to set the editable property [TIMOB-10295]
        //This is a workaround for a Apple Bug.
        textViewImpl.text = @" ";
        textViewImpl.editable = YES;
        
        textViewImpl.text = nil; //Setting TextArea text to empty string
        
        textWidgetView = textViewImpl;
        
    }
    return textWidgetView;
}

-(void)adjustOffsetIfRequired:(UITextView*)tv
{
    CGFloat contentHeight = tv.contentSize.height;
    CGFloat boundsHeight = tv.bounds.size.height;
    CGFloat lineHeight = tv.font.lineHeight;
    
    if (contentHeight >= (boundsHeight - lineHeight)) {
        CGPoint curOffset = tv.contentOffset;
        curOffset.y = curOffset.y + lineHeight;
        [tv setContentOffset:curOffset animated:NO];
    }
}

-(void)setExclusiveTouch:(BOOL)value
{
    [super setExclusiveTouch:value];
	[[self textWidgetView] setExclusiveTouch:value];
}

#pragma mark Public APIs

-(void)setShowUndoRedoActions_:(id)value
{
    if(![TiUtils isIOS9OrGreater]){
        return;
    }
#if IS_XCODE_7
    UITextView *tv = (UITextView *)[self textWidgetView];
    if([TiUtils boolValue:value] == YES) {
        tv.inputAssistantItem.leadingBarButtonGroups = self.inputAssistantItem.leadingBarButtonGroups;
        tv.inputAssistantItem.trailingBarButtonGroups = self.inputAssistantItem.trailingBarButtonGroups;
    } else {
        tv.inputAssistantItem.leadingBarButtonGroups = @[];
        tv.inputAssistantItem.trailingBarButtonGroups = @[];
    }
#endif
}

-(void)setCustomUserInteractionEnabled:(BOOL)value
{
    [super setCustomUserInteractionEnabled:value];
	[(UITextView *)[self textWidgetView] setEditable:[self interactionEnabled]];
}

-(void)setScrollingEnabled_:(id)value
{
	[(UITextView *)[self textWidgetView] setScrollEnabled:[TiUtils boolValue:value]];
}

-(void)setEditable_:(id)value
{
    BOOL _trulyEnabled = ([TiUtils boolValue:value def:YES] && [TiUtils boolValue:[[self proxy] valueForUndefinedKey:@"enabled"] def:YES]);
    [(UITextView *)[self textWidgetView] setEditable:_trulyEnabled];
}

-(void)setAutoLink_:(id)type_
{
	[(UITextView *)[self textWidgetView] setDataDetectorTypes:[TiUtils intValue:type_ def:UIDataDetectorTypeNone]];
}

-(void)setBorderStyle_:(id)value
{
	//TODO
}

-(void)setScrollsToTop_:(id)value
{
	[(UITextView *)[self textWidgetView] setScrollsToTop:[TiUtils boolValue:value def:YES]];
}



-(void)setPadding:(UIEdgeInsets)value
{
    _padding = value;
    if (textWidgetView) {
        CGRect bounds = self.bounds;
        [TiUtils setView:textWidgetView positionRect:UIEdgeInsetsInsetRect(bounds, _padding)];
        [(TiViewProxy*)self.proxy contentsWillChange];
    }
}


-(void)setHintText_:(id)value
{
	[(TiUITextViewImpl*)[self textWidgetView] setPlaceholder:[TiUtils stringValue:value]];
}

-(void)setEllipsize_:(id)value
{
    UITextView *view = (UITextView*)[self textWidgetView];
    [[view textContainer] setLineBreakMode:[TiUtils intValue:value]];
}

-(void)setHintColor_:(id)value
{
	[(TiUITextViewImpl*)[self textWidgetView] setPlaceholderColor:[[TiUtils colorValue:value] color]];
}

-(void)setMaxLines_:(id)value
{
    _maxLines = [TiUtils intValue:value def:0];
    if (textWidgetView) {
        TiUITextViewImpl* textView = ((TiUITextViewImpl*)[self textWidgetView]);
        NSUInteger numLines = textView.contentSize.height/textView.font.lineHeight;
        if (_maxLines > -1 && numLines > _maxLines ) {
            
        }
    }
}


-(void)setDisableBounce_:(id)value
{
	[(TiUITextViewImpl*)[self textWidgetView] setBounces:![TiUtils boolValue:value]];
}


#pragma mark Public Method

-(BOOL)hasText
{
	return [(UITextView *)[self textWidgetView] hasText];
}

//TODO: scrollRangeToVisible

#pragma mark UITextViewDelegate

- (BOOL)textView:(UITextView *)textView shouldInteractWithURL:(NSURL *)URL inRange:(NSRange)characterRange
{
    BOOL handleLinksSet = ([[self proxy] valueForUndefinedKey:@"handleLinks"] != nil);
    if([(TiViewProxy*)[self proxy] _hasListeners:@"link" checkParent:NO]) {
        NSDictionary *eventDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                   [URL absoluteString], @"url",
                                   [NSArray arrayWithObjects:NUMUINTEGER(characterRange.location), NUMUINTEGER(characterRange.length),nil],@"range",
                                   nil];
        [[self proxy] fireEvent:@"link" withObject:eventDict propagate:NO reportSuccess:NO errorCode:0 message:nil];
    }
    if (handleLinksSet) {
        return handleLinks;
    } else {
        return YES;
    }
}

- (void)scrollCaretToVisible
{
    if (_caretVisibilityTimer) {
        [_caretVisibilityTimer invalidate];
        _caretVisibilityTimer = nil;
    }
    TiUITextViewImpl* ourView = (TiUITextViewImpl*)[self textWidgetView];
    [(TiUITextViewImpl*)textWidgetView updateKeyboardInsetWithScroll:YES animated:NO];
}

- (void)textViewDidBeginEditing:(UITextView *)tv
{
	[self textWidget:tv didFocusWithText:[tv text]];
    TiUITextViewImpl* ourView = (TiUITextViewImpl*)[self textWidgetView];
    
    //it does not work to instantly scroll to the caret so let's delay it
    _caretVisibilityTimer = [NSTimer scheduledTimerWithTimeInterval:0.3 target:self selector:@selector(scrollCaretToVisible) userInfo:nil repeats:NO];
}

- (void)textViewDidEndEditing:(UITextView *)tv
{
	NSString * text = [(UITextView *)textWidgetView text];
    
    if (_caretVisibilityTimer) {
        [_caretVisibilityTimer invalidate];
        _caretVisibilityTimer = nil;
    }
    
    if (returnActive && [(TiViewProxy*)self.proxy _hasListeners:@"change" checkParent:NO])
	{
		[self.proxy fireEvent:@"return" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"] propagate:NO checkForListener:NO];
	}
    
	returnActive = NO;
    
	[self textWidget:tv didBlurWithText:text];
    [(TiUITextViewImpl*)textWidgetView updateKeyboardInsetWithScroll:NO animated:NO];
}

- (void)textViewDidChange:(UITextView *)tv
{
    if (_maxLines > -1) {
        NSLayoutManager *layoutManager = [tv layoutManager];
        NSUInteger numberOfLines, index, numberOfGlyphs = [layoutManager numberOfGlyphs];
        NSRange lineRange;
        for (numberOfLines = 0, index = 0; index < numberOfGlyphs; numberOfLines++)
        {
            (void) [layoutManager lineFragmentRectForGlyphAtIndex:index
                                                   effectiveRange:&lineRange];
            index = NSMaxRange(lineRange);
        }
        
        if (numberOfLines > _maxLines)
        {
            // roll back
            [self setValue_:self.currentText];
            return;
        }
        else
        {
            // change accepted
            self.currentText = tv.text;
        }
    }
    
    if ([tv isKindOfClass:[TiUITextViewImpl class]]) {
        [(TiUITextViewImpl*)tv handleTextViewDidChange];
    }
    
	[(TiUITextAreaProxy *)[self proxy] noteValueChange:[(UITextView *)textWidgetView text]];
}

#pragma mark Keyboard delegate stuff


- (void)textViewDidChangeSelection:(UITextView *)tv
{
	if ([self.proxy _hasListeners:@"selected"])
	{
		NSRange range = tv.selectedRange;
        NSDictionary* rangeDict = [NSDictionary dictionaryWithObjectsAndKeys:NUMUINTEGER(range.location),@"location",
                                   NUMUINTEGER(range.length),@"length", nil];
		NSDictionary *event = [NSDictionary dictionaryWithObject:rangeDict forKey:@"range"];
		[self.proxy fireEvent:@"selected" withObject:event];
	}
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 0.1 * NSEC_PER_SEC),
                   dispatch_get_main_queue(), ^
                   {
                       [(TiUITextViewImpl*)textWidgetView updateKeyboardInsetWithScroll:NO animated:NO];
                   });
}

- (BOOL)textViewShouldEndEditing:(UITextView *)tv
{
	return YES;
}

- (BOOL)textView:(UITextView *)tv shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text
{
    self.currentText = [tv text];
	NSString* curText = [self.currentText stringByReplacingCharactersInRange:range withString:text];
    
//    if ( _maxLines > -1) {
//        CGSize sizeThatShouldFitTheContent = [tv sizeThatFits:tv.frame.size];
//        NSUInteger numLines = floorf(sizeThatShouldFitTheContent.height/tv.font.lineHeight);
//        if (numLines>_maxLines) {
//            return NO;
//        }
//    }
	if ([text isEqualToString:@"\n"])
	{
        if ([(TiViewProxy*)self.proxy _hasListeners:@"change" checkParent:NO])
        {
            [self.proxy fireEvent:@"return" withObject:[NSDictionary dictionaryWithObject:[(UITextView *)textWidgetView text] forKey:@"value"] propagate:NO checkForListener:NO];
        }
		if (suppressReturn)
		{
			[tv resignFirstResponder];
			return NO;
		}
	}
	
    if ( (maxLength > -1 && [curText length] > maxLength)) {
        [self setValue_:curText];
        return NO;
    }
    
    //TIMOB-15401. Workaround for UI artifact
    if (![(TiViewProxy*)self.proxy heightIsAutoSize] && [tv isScrollEnabled] && [text isEqualToString:@"\n"]) {
        if (curText.length - tv.selectedRange.location == 1) {
            //Last line. Adjust
            [self adjustOffsetIfRequired:tv];
        }
    }

	[(TiUITextAreaProxy *)self.proxy noteValueChange:curText];
	return TRUE;
}

-(void)setHandleLinks_:(id)args
{
    ENSURE_SINGLE_ARG(args, NSNumber);
    handleLinks = [TiUtils boolValue:args];
}

/*
 Text area constrains the text event though the content offset and edge insets are set to 0
 */
#define TXT_OFFSET 20

-(CGSize)contentSizeForSize:(CGSize)size
{
    UITextView* ourView = (UITextView*)[self textWidgetView];
    NSAttributedString* theString = [ourView attributedText];
	CGRect rect = [theString boundingRectWithSize:CGSizeMake(size.width, CGFLOAT_MAX)
                                               options:NSStringDrawingUsesLineFragmentOrigin
                                               context:nil];
    CGFloat txtWidth = ceilf(rect.size.width);
    CGFloat height = ceilf(rect.size. height);
    if (size.width - txtWidth >= TXT_OFFSET) {
        return CGSizeMake((txtWidth + TXT_OFFSET), height);
    }
    return CGSizeMake(txtWidth + 2 * self.layer.borderWidth, height);
}


- (void)scrollViewDidScroll:(id)scrollView
{
    //Ensure that system messages that cause the scrollView to
    //scroll are ignored if scrollable is set to false
    TiUITextViewImpl* ourView = (TiUITextViewImpl*)[self textWidgetView];
    if (![ourView isScrollEnabled] && ourView.ignoreSystemContentOffset) {
        CGPoint origin = [scrollView contentOffset];
        if ( (origin.x != 0) || (origin.y != 0) ) {
            [scrollView setContentOffset:CGPointZero animated:NO];
        }
    }
}

-(void)updateCaretPosition
{
    [(TiUITextViewImpl*)textWidgetView updateKeyboardInsetWithScroll:NO animated:NO];
}

@end

#endif
