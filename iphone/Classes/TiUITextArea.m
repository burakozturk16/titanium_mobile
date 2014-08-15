/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
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
@end


@implementation TiUITextViewImpl
{
    BOOL becameResponder;
    BOOL settingText;
    BOOL _inLayout;
}

- (void)setContentOffset:(CGPoint)contentOffset
{
    if (_inLayout) return;
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

-(void)layoutSubviews {
    _inLayout = YES;
    [super layoutSubviews];
    _inLayout = NO;
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
            [touchHandler makeRootViewFirstResponder];
        }
        return YES;
	}
	return NO;
}

-(BOOL)becomeFirstResponder
{
    if (self.isEditable && self.canBecomeFirstResponder) {
        [(TiUITextWidget*)touchHandler willBecomeFirstResponder];
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
    
//    if (is_iOS7 && !is_iOS8 && !settingText) {
//        if ([self.text hasSuffix:@"\n"]) {
//            [CATransaction setCompletionBlock:^{
//                [self scrollToCaretAnimated:NO];
//            }];
//        } else {
//            [self scrollToCaretAnimated:NO];
//        }
//    }
}

- (void)setText:(NSString *)text {
    settingText = YES;
    [super setText:text];
    self.displayPlaceHolder = text.length == 0;
    settingText = NO;
}

- (void)scrollToCaretAnimated:(BOOL)animated {
    CGRect rect = [self caretRectForPosition:self.selectedTextRange.end];
    rect.size.height += self.contentInset.bottom;
    [self scrollRectToVisible:rect animated:animated];
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
    int _maxLines;
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
	[TiUtils setView:textWidgetView positionRect:UIEdgeInsetsInsetRect(bounds, _padding)];
    
    //It seems that the textWidgetView are not layed out correctly
    //without this
//    [textWidgetView layoutSubviews];
    [self updateInsentForKeyboard];
    
	[super frameSizeChanged:frame bounds:bounds];
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
        
        lastSelectedRange.location = 0;
        lastSelectedRange.length = 0;
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

-(void)setCustomUserInteractionEnabled:(BOOL)value
{
    [super setCustomUserInteractionEnabled:value];
	[(UITextView *)[self textWidgetView] setEditable:[self interactionEnabled]];
}

-(void)setScrollingEnabled_:(id)value
{
	[(UITextView *)[self textWidgetView] setScrollEnabled:[TiUtils boolValue:value]];
}

-(void)setEditable_:(id)editable
{
	[(UITextView *)[self textWidgetView] setEditable:[TiUtils boolValue:editable]];
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


-(void)setHintColor_:(id)value
{
	[(TiUITextViewImpl*)[self textWidgetView] setPlaceholderColor:[[TiUtils colorValue:value] color]];
}

-(void)setMaxLines_:(id)value
{
    _maxLines = [TiUtils intValue:value];
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
                                   [NSArray arrayWithObjects:NUMINT(characterRange.location), NUMINT(characterRange.length),nil],@"range",
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
    [ourView scrollToCaretAnimated:NO];
}

-(void)updateInsentForKeyboard {
    TiUITextViewImpl* ourView = (TiUITextViewImpl*)[self textWidgetView];
    CGRect keyboardRect = [[TiApp app] controller].currentKeyboardFrame;
    keyboardRect = [self convertRect:keyboardRect fromView:nil];
    UIEdgeInsets contentInset = ourView.contentInset;
    contentInset.bottom = self.frame.size.height - keyboardRect.origin.y;
    ourView.contentInset = contentInset;
}


- (void)textViewDidBeginEditing:(UITextView *)tv
{
	[self textWidget:tv didFocusWithText:[tv text]];
    TiUITextViewImpl* ourView = (TiUITextViewImpl*)[self textWidgetView];
    
    [self updateInsentForKeyboard];
    
    //it does not work to instantly scroll to the caret so let's delay it
    _caretVisibilityTimer = [NSTimer scheduledTimerWithTimeInterval:0.3 target:self selector:@selector(scrollCaretToVisible) userInfo:nil repeats:NO];
}

- (void)textViewDidEndEditing:(UITextView *)tv
{
	NSString * text = [(UITextView *)textWidgetView text];
    
    UIEdgeInsets contentInset = tv.contentInset;
    contentInset.bottom = 0;
    tv.contentInset = contentInset;
    
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
        NSDictionary* rangeDict = [NSDictionary dictionaryWithObjectsAndKeys:NUMINT(range.location),@"location",
                                   NUMINT(range.length),@"length", nil];
		NSDictionary *event = [NSDictionary dictionaryWithObject:rangeDict forKey:@"range"];
		[self.proxy fireEvent:@"selected" withObject:event];
	}
    //TIMOB-15401. Workaround for UI artifact
    if ((tv == textWidgetView) && (!NSEqualRanges(tv.selectedRange, lastSelectedRange))) {
        lastSelectedRange.location = tv.selectedRange.location;
        lastSelectedRange.length = tv.selectedRange.length;
        [tv scrollRangeToVisible:lastSelectedRange];
    }
}

- (BOOL)textViewShouldEndEditing:(UITextView *)tv
{
	return YES;
}

- (BOOL)textView:(UITextView *)tv shouldChangeTextInRange:(NSRange)range replacementText:(NSString *)text
{
    self.currentText = [tv text];
	NSString* curText = [self.currentText stringByReplacingCharactersInRange:range withString:text];
    
    if ( _maxLines > -1) {
        CGSize sizeThatShouldFitTheContent = [tv sizeThatFits:tv.frame.size];
        NSUInteger numLines = floorf(sizeThatShouldFitTheContent.height/tv.font.lineHeight);
        if (numLines>_maxLines) {
            return NO;
        }
    }
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
    [[self proxy] replaceValue:NUMBOOL(handleLinks) forKey:@"handleLinks" notification:NO];
}

/*
Text area constrains the text event though the content offset and edge insets are set to 0 
*/
#define TXT_OFFSET 20

-(CGSize)contentSizeForSize:(CGSize)size
{
	UITextView* ourView = (UITextView*)[self textWidgetView];
    NSString* txt = ourView.text;
    //sizeThatFits does not seem to work properly.
    CGFloat height = [ourView sizeThatFits:CGSizeMake(size.width, 1E100)].height;
    CGFloat txtWidth = [txt sizeWithFont:ourView.font constrainedToSize:CGSizeMake(size.width, 1E100) lineBreakMode:UILineBreakModeWordWrap].width;
    if (size.width - txtWidth >= TXT_OFFSET) {
        return CGSizeMake((txtWidth + TXT_OFFSET), height);
    }
    return CGSizeMake(txtWidth + 2 * self.layer.borderWidth, height);
}


- (void)scrollViewDidScroll:(id)scrollView
{
    //Ensure that system messages that cause the scrollView to
    //scroll are ignored if scrollable is set to false
    UITextView* ourView = (UITextView*)[self textWidgetView];
    if (![ourView isScrollEnabled]) {
        CGPoint origin = [scrollView contentOffset]; 
        if ( (origin.x != 0) || (origin.y != 0) ) {
            [scrollView setContentOffset:CGPointZero animated:NO];
        }
    }
}

@end

#endif
