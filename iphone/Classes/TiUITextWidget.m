/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITEXTWIDGET) || defined(USE_TI_UITEXTAREA) || defined(USE_TI_UITEXTFIELD)

#import "TiUITextWidget.h"
#import "TiUITextWidgetProxy.h"
#import "TiViewProxy.h"
#import "TiApp.h"
#import "TiUtils.h"
#if defined (USE_TI_UIATTRIBUTEDSTRING)
#import "TiUIAttributedStringProxy.h"
#endif

@interface TiUITextWidgetProxy()
- (TiViewProxy *)keyboardAccessoryProxy;
@end


@implementation TiUITextWidget

- (id) init
{
	self = [super init];
	if (self != nil)
	{
		suppressReturn = YES;
		maxLength = -1;
        [self textWidgetView];
	}
	return self;
}

-(UIView*)viewForHitTest
{
    return [self textWidgetView];
}

#if defined (USE_TI_UIATTRIBUTEDSTRING)
-(void)setAttributedString_:(id)arg
{
	ENSURE_SINGLE_ARG(arg, TiUIAttributedStringProxy);
	[(id)[self textWidgetView] setAttributedText:[arg attributedString]];
}
#endif

-(void)setValue_:(id)value
{
    NSString* string = [TiUtils stringValue:value];
    if (string == nil)
	{
		string = @"";
	}
    if (maxLength > -1 && [string length] > maxLength) {
        string = [string substringToIndex:maxLength];
    }
    if ([string isEqualToString:[(id)[self textWidgetView] text]]) return;
    [(id)[self textWidgetView] setText:string];
}

-(void)setMaxLength_:(id)value
{
    maxLength = [TiUtils intValue:value def:-1];
    [self setValue_:[[self proxy] valueForUndefinedKey:@"value"]];
}

-(void)setSuppressReturn_:(id)value
{
	suppressReturn = [TiUtils boolValue:value def:YES];
}

- (void) dealloc
{
	//Because text fields MUST be played with on main thread, we cannot release if there's the chance we're on a BG thread
#ifdef TI_USE_KROLL_THREAD
	TiThreadRemoveFromSuperviewOnMainThread(textWidgetView, YES);
	TiThreadReleaseOnMainThread(textWidgetView, NO);
	textWidgetView = nil;	//Wasted action, yes.
#else
    TiThreadPerformOnMainThread(^{
        [textWidgetView removeFromSuperview];
        RELEASE_TO_NIL(textWidgetView);
    }, YES);
#endif
	[super dealloc];
}

-(BOOL)hasTouchableListener
{
	// since this guy only works with touch events, we always want them
	// just always return YES no matter what listeners we have registered
	return YES;
}

-(BOOL)resignFirstResponder
{
    [super resignFirstResponder];
    return [[self textWidgetView] resignFirstResponder];
}

-(BOOL)becomeFirstResponder
{
    return [[self textWidgetView] becomeFirstResponder];
}

-(BOOL)isFirstResponder
{
    return [textWidgetView isFirstResponder];
}


#pragma mark Must override
-(BOOL)hasText
{
	return NO;
}

-(UIView<UITextInputTraits>*)textWidgetView
{
	return nil;
}

- (id)accessibilityElement
{
	return [self textWidgetView];
}

#pragma mark Common values

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
//	[textWidgetView setFrame:[self bounds]];
    [super frameSizeChanged:frame bounds:bounds];
}

-(void)setColor_:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
	[(id)[self textWidgetView] setTextColor:(newColor != nil)?newColor:[UIColor darkTextColor]];
}

-(void)setHintColor_:(id)value
{
    [[self textWidgetView] setValue:[[TiUtils colorValue:value] color] forKeyPath:@"_placeholderLabel.textColor"];
}

-(void)setFont_:(id)font
{
	[(id)[self textWidgetView] setFont:[[TiUtils fontValue:font] font]];
}

// <0.9 is textAlign
-(void)setTextAlign_:(id)alignment
{
	[(id)[self textWidgetView] setTextAlignment:[TiUtils textAlignmentValue:alignment]];
}

-(void)setReturnKeyType_:(id)value
{
	[[self textWidgetView] setReturnKeyType:[TiUtils intValue:value]];
}

-(void)setEnableReturnKey_:(id)value
{
	[[self textWidgetView] setEnablesReturnKeyAutomatically:[TiUtils boolValue:value]];
}

-(void)refreshInputView
{
    [[self textWidgetView] reloadInputViews]; // does not work on iOS8 !
    
    if ([[self textWidgetView] isFirstResponder]) {
        [[self textWidgetView] resignFirstResponder];
        [[self textWidgetView] becomeFirstResponder];
    }
}

-(void)setKeyboardType_:(id)value
{
	[[self textWidgetView] setKeyboardType:[TiUtils intValue:value]];
    [self refreshInputView];
}

-(void)setAutocorrect_:(id)value
{
	[[self textWidgetView] setAutocorrectionType:[TiUtils boolValue:value] ? UITextAutocorrectionTypeYes : UITextAutocorrectionTypeNo];
}

#pragma mark Responder methods
//These used to be blur/focus, but that's moved to the proxy only.
//The reason for that is so checking the toolbar can use UIResponder methods.

-(void)setPasswordMask_:(id)value
{
	[[self textWidgetView] setSecureTextEntry:[TiUtils boolValue:value]];
}

-(void)setAppearance_:(id)value
{
	[[self textWidgetView] setKeyboardAppearance:[TiUtils intValue:value]];
}

-(void)setAutocapitalization_:(id)value
{
	[[self textWidgetView] setAutocapitalizationType:[TiUtils intValue:value]];
}

-(void)setKeyboardToolbar_:(id)value
{
    ((UITextView*)[self textWidgetView]).inputAccessoryView = [[(TiUITextWidgetProxy*)self.proxy keyboardAccessoryProxy] getAndPrepareViewForOpening:[TiUtils appFrame]];
}

#pragma mark Keyboard Delegates

-(void)textWidget:(UIView<UITextInputTraits>*)tw didFocusWithText:(NSString *)value
{
	TiUITextWidgetProxy * ourProxy = (TiUITextWidgetProxy *)[self proxy];

//	[[TiApp controller] didKeyboardFocusOnProxy:(TiViewProxy<TiKeyboardFocusableView> *)ourProxy];

	if ([ourProxy suppressFocusEvents]) {
		return;
	}

	if ([ourProxy _hasListeners:@"focus" checkParent:NO])
	{
		[ourProxy fireEvent:@"focus" withObject:[NSDictionary dictionaryWithObject:value forKey:@"value"] propagate:NO checkForListener:NO];
	}
}

-(void)textWidget:(UIView<UITextInputTraits>*)tw didBlurWithText:(NSString *)value
{
	TiUITextWidgetProxy * ourProxy = (TiUITextWidgetProxy *)[self proxy];

//	[[TiApp controller] didKeyboardBlurOnProxy:(TiViewProxy<TiKeyboardFocusableView> *)ourProxy];

	if ([ourProxy suppressFocusEvents]) {
		return;
	}
	
	if ([ourProxy _hasListeners:@"blur" checkParent:NO])
	{
		[ourProxy fireEvent:@"blur" withObject:[NSDictionary dictionaryWithObject:value forKey:@"value"] propagate:NO checkForListener:NO];
	}
	
	// In order to capture gestures properly, we need to force the root view to become the first responder.
	[self makeRootViewFirstResponder];
}

-(NSDictionary*)selectedRange
{
    id<UITextInput> textView = (id<UITextInput>)[self textWidgetView];
    if ([textView conformsToProtocol:@protocol(UITextInput)]) {
        UITextRange* theRange = [textView selectedTextRange];
        if (theRange != nil) {
            UITextPosition *beginning = textView.beginningOfDocument;
            UITextPosition* start = theRange.start;
            UITextPosition* end = theRange.end;
            NSInteger startPos = [textView offsetFromPosition:beginning toPosition:start];
            NSInteger endPos = [textView offsetFromPosition:beginning toPosition:end];
            NSInteger length = endPos - startPos;
            
            return [NSDictionary dictionaryWithObjectsAndKeys:NUMINTEGER(startPos),@"location",NUMINTEGER(length),@"length",nil];
        }
    }
    return nil;
}

-(void)setSelectionFrom:(id)start to:(id)end
{
    UIView<UITextInput>* textView = (UIView<UITextInput>*)[self textWidgetView];
    if ([textView conformsToProtocol:@protocol(UITextInput)]) {
        if([textView becomeFirstResponder] || [textView isFirstResponder]) {
            UITextPosition *beginning = textView.beginningOfDocument;
            UITextPosition *startPos = [textView positionFromPosition:beginning offset:[TiUtils intValue: start]];
            UITextPosition *endPos = [textView positionFromPosition:beginning offset:[TiUtils intValue: end]];
            UITextRange *textRange;
            textRange = [textView textRangeFromPosition:startPos toPosition:endPos];
            [textView setSelectedTextRange:textRange];
        }
    } else {
        DebugLog(@"TextWidget does not conform with UITextInput protocol. Ignore");
    }
}


#pragma mark - Titanium Internal Use Only
-(void)updateKeyboardStatus
{
    if ( ([[[TiApp app] controller] keyboardVisible]) && ([[[TiApp app] controller] keyboardFocusedProxy] == [self proxy]) ) {
        [[[TiApp app] controller] performSelector:@selector(updateKeyboardStatus) withObject:nil afterDelay:0.0];
    }
}

@end

#endif
