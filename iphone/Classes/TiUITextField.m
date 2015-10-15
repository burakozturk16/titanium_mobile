/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITEXTFIELD

#import "TiUITextField.h"
#import "TiUITextFieldProxy.h"

#import "TiUtils.h"
#import "TiViewProxy.h"
#import "TiApp.h"
#import "TiUITextWidget.h"
#if defined (USE_TI_UIATTRIBUTEDSTRING)
#import "TiUIAttributedStringProxy.h"
#endif

@implementation TiTextField
{
    UIEdgeInsets _padding;
}
@synthesize padding = _padding, becameResponder;


-(void)configure
{
    _padding = UIEdgeInsetsMake(0, 5, 0, 5);
    [super setLeftViewMode:UITextFieldViewModeAlways];
    [super setRightViewMode:UITextFieldViewModeAlways];
//    _hintColor = nil;
}

-(void)dealloc
{
    [super dealloc];
}

-(void)setTouchHandler:(TiUIView*)handler
{
    touchHandler = handler;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    [touchHandler processTouchesBegan:touches withEvent:event];
    [super touchesBegan:touches withEvent:event];
}
- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    [touchHandler processTouchesMoved:touches withEvent:event];
    [super touchesMoved:touches withEvent:event];
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [touchHandler processTouchesEnded:touches withEvent:event];
    [super touchesEnded:touches withEvent:event];
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [touchHandler processTouchesCancelled:touches withEvent:event];
    [super touchesCancelled:touches withEvent:event];
}

-(UIView*)newPadView:(CGFloat)width height:(CGFloat)height
{
    UIView *view = [[UIView alloc] initWithFrame:CGRectMake(0, 0, width, height)];
    view.backgroundColor = [UIColor clearColor];
    return view;
}

-(void)setPadding:(UIEdgeInsets)value
{
    _padding = value;
    [self setNeedsLayout];
}
-(BOOL)canBecomeFirstResponder
{
    return self.isEnabled;
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
    if (self.isEnabled && self.canBecomeFirstResponder) {
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

- (CGRect)textRectForBounds:(CGRect)bounds
{
    CGRect result = UIEdgeInsetsInsetRect(bounds, _padding);
    if ([self leftView]){
        CGRect rect = [self leftViewRectForBounds:result];
        CGFloat width = rect.origin.x + rect.size.width;
        result.origin.x +=width;
        result.size.width -= width;
    }
    if ([self rightView]){
        CGRect rect = [self rightViewRectForBounds:result];
        CGFloat width = bounds.size.width - rect.origin.x;
        result.size.width -= width;
    }
    return result;
}
- (CGRect)editingRectForBounds:(CGRect)bounds
{
    return [self textRectForBounds:bounds];
}

- (CGRect)leftViewRectForBounds:(CGRect)bounds
{
    UIView* view = [self leftView];
    if ([view isKindOfClass:[TiUIView class]]){
        TiViewProxy* proxy  = (TiViewProxy*)[((TiUIView*)view) proxy];
        return [proxy computeBoundsForParentBounds:bounds];
    }
    return [super leftViewRectForBounds:bounds];
}

- (CGRect)rightViewRectForBounds:(CGRect)bounds
{
    UIView* view = [self rightView];
    if ([view isKindOfClass:[TiUIView class]]){
        TiViewProxy* proxy  = (TiViewProxy*)[((TiUIView*)view) proxy];
        return [proxy computeBoundsForParentBounds:bounds];
    }
    return [super rightViewRectForBounds:bounds];
}
//
//- (void)drawPlaceholderInRect:(CGRect)rect {
//    if (!_hintColor) {
//        [super drawPlaceholderInRect:rect];
//    }
//    else{
//        [_hintColor setFill];
//        // Get the size of placeholder text. We will use height to calculate frame Y position
//        CGSize size = [self.placeholder sizeWithFont:self.font];
//        
//        // Vertically centered frame
//        CGRect placeholderRect;
//        switch (self.contentVerticalAlignment) {
//            case UIControlContentVerticalAlignmentTop:
//                placeholderRect = rect;
//                //strangely needed :s
//                placeholderRect.origin.y = _padding.top;
//                placeholderRect.size.height -= _padding.top + _padding.bottom;
//                break;
//            case UIControlContentVerticalAlignmentCenter:
//                placeholderRect = CGRectMake(rect.origin.x, (rect.size.height - size.height)/2, rect.size.width, size.height);
//                break;
//            case UIControlContentVerticalAlignmentBottom:
//                placeholderRect = CGRectMake(rect.origin.x, rect.size.height - size.height, rect.size.width, size.height);
//                break;
//            default:
//                break;
//        }
//        
//        // Check if OS version is 7.0+ and draw placeholder a bit differently
//        if (IOS_7) {
//            
//            NSMutableParagraphStyle *style = [[NSMutableParagraphStyle alloc] init];
//            style.lineBreakMode = NSLineBreakByTruncatingTail;
//            style.alignment = self.textAlignment;
//            NSDictionary *attr = [NSDictionary dictionaryWithObjectsAndKeys:style,NSParagraphStyleAttributeName, self.font, NSFontAttributeName, _hintColor, NSForegroundColorAttributeName, nil];
//            
//            [self.placeholder drawInRect:placeholderRect withAttributes:attr];
//            
//            [style release];
//        } else {
//            [self.placeholder drawInRect:placeholderRect
//                                withFont:self.font
//                           lineBreakMode:NSLineBreakByTruncatingTail
//                               alignment:self.textAlignment];
//        }
//    }
//}

@end



@implementation TiUITextField
{
    BOOL _editing;
}

#pragma mark Internal

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [TiUtils setView:textWidgetView positionRect:bounds];
    [super frameSizeChanged:frame bounds:bounds];
}

- (void) dealloc
{
    WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UITextFieldTextDidChangeNotification object:nil];
    [super dealloc];
}


-(UIView<UITextInputTraits>*)textWidgetView
{
    if (textWidgetView==nil)
    {
        TiTextField* textfield = [[TiTextField alloc] initWithFrame:CGRectZero];
        textWidgetView = textfield;
        //        textfield.backgroundColor = [UIColor clearColor];
        textfield.delegate = self;
        textfield.text = @"";
        textfield.textAlignment = UITextAlignmentLeft;
        textfield.contentVerticalAlignment = UIControlContentVerticalAlignmentCenter;
        [textfield configure];
        [textfield setTouchHandler:self];
        [self addSubview:textWidgetView];
        self.clipsToBounds = YES;
        WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
        NSNotificationCenter * theNC = [NSNotificationCenter defaultCenter];
        [theNC addObserver:self selector:@selector(textFieldDidChange:) name:UITextFieldTextDidChangeNotification object:textWidgetView];
    }
    return textWidgetView;
}


#pragma mark Public APIs

-(void)setShowUndoRedoActions_:(id)value
{
    if(![TiUtils isIOS9OrGreater]){
        return;
    }
#if IS_XCODE_7
    TiTextField* tv = (TiTextField*)[self textWidgetView];
    
    [[self proxy] replaceValue:value forKey:@"showUndoRedoActions" notification:NO];

    if([TiUtils boolValue:value] == YES) {
        tv.inputAssistantItem.leadingBarButtonGroups = self.inputAssistantItem.leadingBarButtonGroups;
        tv.inputAssistantItem.trailingBarButtonGroups = self.inputAssistantItem.trailingBarButtonGroups;
    } else {
        tv.inputAssistantItem.leadingBarButtonGroups = @[];
        tv.inputAssistantItem.trailingBarButtonGroups = @[];
    }
#endif
}

-(void)setExclusiveTouch:(BOOL)value
{
    [super setExclusiveTouch:value];
    [[self textWidgetView] setExclusiveTouch:value];
}

#pragma mark Public APIs

-(void)setPadding:(UIEdgeInsets)inset
{
    [self textWidgetView].padding = inset;
}

-(void)setEditable_:(id)value
{
    BOOL _trulyEnabled = ([TiUtils boolValue:value def:YES] && [TiUtils boolValue:[[self proxy] valueForUndefinedKey:@"enabled"] def:YES]);
    [[self textWidgetView] setEnabled:_trulyEnabled];
    [self setBgState:UIControlStateNormal];
}

-(BOOL) enabledForBgState {
    return [self textWidgetView].enabled && [super enabledForBgState];
}

-(void)setCustomUserInteractionEnabled:(BOOL)value
{
    BOOL _trulyEnabled = (value && [TiUtils boolValue:[[self proxy] valueForUndefinedKey:@"editable"] def:YES]);
    [super setCustomUserInteractionEnabled:_trulyEnabled];
    [[self textWidgetView] setEnabled:_trulyEnabled];
}

-(void)setHintText_:(id)value
{
    [[self textWidgetView] setPlaceholder:[TiUtils stringValue:value]];
}

#if defined (USE_TI_UIATTRIBUTEDSTRING)
-(void)setAttributedHintText_:(id)value
{
	ENSURE_SINGLE_ARG(value,TiUIAttributedStringProxy);
	[[self textWidgetView] setAttributedPlaceholder:[value attributedString]];
}
#endif

-(void)setMinimumFontSize_:(id)value
{
    CGFloat newSize = [TiUtils floatValue:value];
    if (newSize < 4) {
        [[self textWidgetView] setAdjustsFontSizeToFitWidth:NO];
        [[self textWidgetView] setMinimumFontSize:0.0];
    }
    else {
        [[self textWidgetView] setAdjustsFontSizeToFitWidth:YES];
        [[self textWidgetView] setMinimumFontSize:newSize];
    }
}

-(void)setClearOnEdit_:(id)value
{
    [[self textWidgetView] setClearsOnBeginEditing:[TiUtils boolValue:value]];
}

-(void)setBorderStyle_:(id)value
{
    TiThreadPerformOnMainThread(^{
        [[self textWidgetView] setBorderStyle:[TiUtils intValue:value]];
    }, NO);
}

-(void)setClearButtonMode_:(id)value
{
    [[self textWidgetView] setClearButtonMode:[TiUtils intValue:value]];
}

//TODO: rename

-(void)setLeftButton_:(id)value
{
    UIView* leftView = nil;
    id vp = [[self viewProxy] addObjectToHold:value forKey:@"leftButton"];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        // to make sure the proxy is not released
        //we need the proxy to be retained to compute the rect
        LayoutConstraint* constraint = [(TiViewProxy*)vp layoutProperties];
        if (TiDimensionIsUndefined(constraint->left))
        {
            constraint->left = TiDimensionDip(0);
        }
        leftView = [(TiViewProxy*)vp getAndPrepareViewForOpening:[self textWidgetView].bounds];
        
    }
    [[self textWidgetView] setLeftView:leftView];
}

-(void)setLeftButtonMode_:(id)value
{
    [[self textWidgetView] setLeftViewMode:[TiUtils intValue:value]];
}

-(void)setRightButton_:(id)value
{
    UIView* rightView = nil;
    id vp = [[self viewProxy] addObjectToHold:value forKey:@"rightButton"];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        LayoutConstraint* constraint = [(TiViewProxy*)vp layoutProperties];
        if (TiDimensionIsUndefined(constraint->right))
        {
            constraint->right = TiDimensionDip(0);
        }
        rightView = [(TiViewProxy*)vp getAndPrepareViewForOpening:[self textWidgetView].bounds];
        
    }
    [[self textWidgetView] setRightView:rightView];
}

-(void)setRightButtonMode_:(id)value
{
    [[self textWidgetView] setRightViewMode:[TiUtils intValue:value]];
}

-(void)setVerticalAlign_:(id)value
{
    UIControlContentVerticalAlignment verticalAlign = [TiUtils contentVerticalAlignmentValue:value];
    [[self textWidgetView] setContentVerticalAlignment:verticalAlign];
}

#pragma mark Public Method

-(BOOL)hasText
{
    UITextField *f = [self textWidgetView];
    return [[f text] length] > 0;
}

#pragma mark UITextFieldDelegate

- (void)textFieldDidBeginEditing:(UITextField *)tf
{
    _editing = YES;
    TiUITextWidgetProxy * ourProxy = (TiUITextWidgetProxy *)[self proxy];
    
    //TIMOB-14563. Set the right text value.
    if ([ourProxy suppressFocusEvents]) {
        NSString* theText = [ourProxy valueForKey:@"value"];
        [tf setText:theText];
    }
    [self setViewState:UIControlStateHighlighted];
    
    [self textWidget:tf didFocusWithText:[tf text]];
    [self performSelector:@selector(textFieldDidChange:) onThread:[NSThread currentThread] withObject:nil waitUntilDone:NO];
}


#pragma mark Keyboard Delegates

- (BOOL)textFieldShouldBeginEditing:(UITextField *)textField;        // return NO to disallow editing.
{
    return YES;
}

- (BOOL)textField:(UITextField *)tf shouldChangeCharactersInRange:(NSRange)range replacementString:(NSString *)string
{
    //sanity check for undo bug. Does nothing if undo pressed for certain keyboardsTypes and under some conditions.
    if (range.length + range.location > [[tf text] length]) {
        return NO;
    }
    
    NSString *curText = [[tf text] stringByReplacingCharactersInRange:range withString:string];
    
    if ( (maxLength > -1) && ([curText length] > maxLength) ) {
        [self setValue_:curText];
        return NO;
    }
    return YES;
}

- (void)textFieldDidEndEditing:(UITextField *)tf
{
    if (!_editing) return;
    _editing = NO;
    [self setViewState:-1];
    [self textWidget:tf didBlurWithText:[tf text]];
	//TIMOB-18365. Value not updated when autocorrect is up and return is pressed
	[self textFieldDidChange:nil];
}

- (void)textFieldDidChange:(NSNotification *)notification
{
    TiUITextWidgetProxy * ourProxy = (TiUITextWidgetProxy *)[self proxy];
    
    
    //TIMOB-14563. This is incorrect when passowrd mark is used. Just ignore.
    if ([ourProxy suppressFocusEvents]) {
        return;
    }
    [ourProxy noteValueChange:[(UITextField *)textWidgetView text]];
}

- (BOOL)textFieldShouldEndEditing:(UITextField *)tf
{
    return YES;
}

- (BOOL)textFieldShouldClear:(UITextField *)tf
{
    // we notify proxy so he can serialize in the model
    [(TiUITextFieldProxy *)self.proxy noteValueChange:@""];
    return YES;
}

-(BOOL)textFieldShouldReturn:(UITextField *)tf
{
    if ([(TiViewProxy*)self.proxy _hasListeners:@"return" checkParent:NO])
    {
        [self.proxy fireEvent:@"return" withObject:[NSDictionary dictionaryWithObject:[tf text] forKey:@"value"] propagate:NO checkForListener:NO];
    }
    
    if ([self textWidgetView].returnKeyType == UIReturnKeyNext)
    {
        return ![(TiUITextWidgetProxy *)self.proxy selectNextTextWidget];
    }
    
    if (suppressReturn)
    {
        [tf resignFirstResponder];
        return NO;
    }
    return YES;
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    return [[self textWidgetView] sizeThatFits:size];
}

@end

#endif
