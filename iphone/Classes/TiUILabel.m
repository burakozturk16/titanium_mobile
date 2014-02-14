/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILABEL

#import "TiUILabel.h"
#import "TiUILabelProxy.h"
#import "TiUtils.h"
#import "UIImage+Resize.h"
#import <CoreText/CoreText.h>
#ifdef USE_TI_UIIOSATTRIBUTEDSTRING
#import "TiUIiOSAttributedStringProxy.h"
#endif
#import "DTCoreText.h"
#import "TiTransitionHelper.h"
#import "TiTransition.h"

@implementation TiLabel

-(void)setFrame:(CGRect)frame
{
    [super setFrame:CGRectIntegral(frame)];
}

@end


@interface TiUILabel()
{
    BOOL _reusing;
}
@property(nonatomic,retain) NSDictionary *transition;
@end


@implementation TiUILabel

#pragma mark Internal

-(id)init
{
    if (self = [super init]) {
        self.transition = nil;
    }
    return self;
}

-(void)dealloc
{
	RELEASE_TO_NIL(_transition);
    RELEASE_TO_NIL(label);
    [super dealloc];
}

-(UIView*)viewForHitTest
{
    return label;
}

- (CGSize)suggestedFrameSizeToFitEntireStringConstraintedToSize:(CGSize)size
{
    CGSize maxSize = CGSizeMake(size.width<=0 ? 10000 : size.width, 10000);
    maxSize.width -= label.viewInsets.left + label.viewInsets.right;
    
    CGSize result = [[self label] sizeThatFits:maxSize];
    if (size.width > 0) result.width = MIN(result.width,  size.width);
    if (size.height > 0) result.height = MIN(result.height,  size.height);
    //padding
    result.width += label.viewInsets.left+ label.viewInsets.right;
    result.height += label.viewInsets.top + label.viewInsets.bottom;
    
    CGSize shadowOffset = [label shadowOffset];
    result.width += abs(shadowOffset.width);
    result.height += abs(shadowOffset.height);
    return result;
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    return [self suggestedFrameSizeToFitEntireStringConstraintedToSize:size];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [label setFrame:bounds];
    [super frameSizeChanged:frame bounds:bounds];
}

- (void)configurationStart {
    [super configurationStart];
    needsSetText = NO;
}

- (void)configurationSet {
    
    [super configurationSet];
    if (needsSetText)
        [self setAttributedTextViewContent];
}

-(TTTAttributedLabel*)label
{
	if (label==nil)
	{
        label = [[TiLabel alloc] initWithFrame:CGRectZero];
        label.backgroundColor = [UIColor clearColor];
        label.numberOfLines = 0;//default wordWrap to True
        label.lineBreakMode = UILineBreakModeWordWrap; //default ellipsis to none
        label.layer.shadowRadius = 0; //for backward compatibility
        label.layer.shadowOffset = CGSizeZero;
		label.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        label.touchDelegate = self;
        label.strokeColorAttributeProperty = DTBackgroundStrokeColorAttribute;
        label.strokeWidthAttributeProperty = DTBackgroundStrokeWidthAttribute;
        label.cornerRadiusAttributeProperty = DTBackgroundCornerRadiusAttribute;
        label.paddingAttributeProperty = DTPaddingAttribute;
        if ([TiUtils isIOS6OrGreater])
        {
            label.strikeOutAttributeProperty = NSStrikethroughStyleAttributeName;
            label.backgroundColorAttributeProperty = NSBackgroundColorAttributeName;
        }
        else {
            label.strikeOutAttributeProperty = DTStrikeOutAttribute;
            label.backgroundColorAttributeProperty = DTBackgroundColorAttribute;
        }
        label.delegate = self;
        [self addSubview:label];
	}
	return label;
}

-(NSURL *)checkLinkAttributeForString:(NSAttributedString*)theString atPoint:(CGPoint)p
{

    CFIndex idx = [label characterIndexAtPoint:p];
    if (idx != NSNotFound) {
        if(idx >= theString.string.length) {
            return NO;
        }
        NSRange theRange = NSMakeRange(0, 0);
        NSURL *url = [theString attribute:DTLinkAttribute atIndex:idx effectiveRange:&theRange];
        return url;
    }
    return nil;
}

- (id)accessibilityElement
{
	return [self label];
}

- (void)setAttributedTextViewContent {
    if (!configurationSet) {
        needsSetText = YES;
        return; // lazy init
    }
    if (![NSThread isMainThread])
    {
        TiThreadPerformOnMainThread(^{
            [self setAttributedTextViewContent];
        }, YES);
        return;
    }
    id content = [(TiUILabelProxy*)[self proxy] getLabelContent];
    [self transitionToText:content];
}


- (TiLabel*) cloneView:(TiLabel*)source {
    NSData *archivedViewData = [NSKeyedArchiver archivedDataWithRootObject: source];
    TiLabel* clone = [NSKeyedUnarchiver unarchiveObjectWithData:archivedViewData];
    
    //seems to be duplicated < ios7
    clone.font = source.font;
    clone.textColor = source.textColor;
    clone.highlightedTextColor = source.highlightedTextColor;
    //
    
    clone.touchDelegate = source.touchDelegate;
    clone.delegate = source.delegate;
    return clone;
}

-(void) transitionToText:(id)text
{
    TiTransition* transition = [TiTransitionHelper transitionFromArg:self.transition containerView:self];
    if (transition != nil) {
        TiLabel *oldView = [self label];
        TiLabel *newView = [self cloneView:oldView];
        newView.text = text;
        [TiTransitionHelper transitionfromView:oldView toView:newView insideView:self withTransition:transition prepareBlock:^{
        } completionBlock:^{
            [oldView release];
        }];
        label = [newView retain];
	}
    else {
        [[self label] setText:text];
    }
}

-(void)setHighlighted:(BOOL)newValue animated:(BOOL)animated
{
    [super setHighlighted:newValue animated:animated];
    [[self label] setHighlighted:newValue];
}

- (void)didMoveToSuperview
{
	[self setHighlighted:NO];
	[super didMoveToSuperview];
}

- (void)didMoveToWindow
{
    /*
     * See above
     */
    [self setHighlighted:NO];
    [super didMoveToWindow];
}

-(BOOL)isHighlighted
{
    return [[self label] isHighlighted];
}

-(void)setExclusiveTouch:(BOOL)value
{
    [super setExclusiveTouch:value];
	[[self label] setExclusiveTouch:value];
}

#pragma mark Public APIs

-(void)setEnabled_:(id)value
{
    [super setEnabled_:value];
	[[self label] setEnabled:[self interactionEnabled]];
}

-(void)setVerticalAlign_:(id)value
{
    UIControlContentVerticalAlignment verticalAlign = [TiUtils contentVerticalAlignmentValue:value];
    [[self label] setVerticalAlignment:(TTTAttributedLabelVerticalAlignment)verticalAlign];
}
-(void)setAutoLink_:(id)value
{
    [[self label] setDataDetectorTypes:[TiUtils intValue:value]];
    //we need to update the text
    [self setAttributedTextViewContent];
}

-(void)setColor_:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
    if (newColor == nil)
        newColor = [UIColor darkTextColor];
	[[self label] setTextColor:newColor];
}

-(void)setText_:(id)value
{
    needsSetText = YES;
}

-(void)setHtml_:(id)value
{
    needsSetText = YES;
}

-(void)setHighlightedColor_:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
	[[self label] setHighlightedTextColor:(newColor != nil)?newColor:[UIColor lightTextColor]];
}

-(void)setSelectedColor_:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
	[[self label] setHighlightedTextColor:(newColor != nil)?newColor:[UIColor lightTextColor]];
}

-(void)setDisabledColor_:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
	[[self label] setDisabledColor:newColor];
}

-(void)setFont_:(id)fontValue
{
    UIFont * font;
    if (fontValue!=nil)
    {
        font = [[TiUtils fontValue:fontValue] font];
    }
    else
    {
        font = [UIFont systemFontOfSize:[UIFont labelFontSize]];
    }
	[[self label] setFont:font];
}

-(void)setMinimumFontSize_:(id)size
{
    CGFloat newSize = [TiUtils floatValue:size];
    if (newSize < 4) { // Beholden to 'most minimum' font size
        [[self label] setAdjustsFontSizeToFitWidth:NO];
        [[self label] setMinimumFontSize:0.0];
    }
    else {
        [[self label] setAdjustsFontSizeToFitWidth:YES];
        [[self label] setMinimumFontSize:newSize];
    }
    [self updateNumberLines];   
}

-(void)setAttributedString_:(id)arg
{
#ifdef USE_TI_UIIOSATTRIBUTEDSTRING
    ENSURE_SINGLE_ARG(arg, TiUIiOSAttributedStringProxy);
    [[self proxy] replaceValue:arg forKey:@"attributedString" notification:NO];
    [[self label] setAttributedText:[arg attributedString]];
    [(TiViewProxy *)[self proxy] contentsWillChange];
#endif
}



-(void)setTextAlign_:(id)alignment
{
	[[self label] setTextAlignment:[TiUtils textAlignmentValue:alignment]];
}

-(void)setShadowColor_:(id)color
{
	if (color==nil)
	{
		[[[self label] layer]setShadowColor:nil];
	}
	else
	{
		color = [TiUtils colorValue:color];
		[[self label] setShadowColor:[color _color]];
	}
}

-(void)setShadowRadius_:(id)arg
{
    [[self label] setShadowRadius:[TiUtils floatValue:arg]];
}
-(void)setShadowOffset_:(id)value
{
	CGPoint p = [TiUtils pointValue:value];
	CGSize size = {p.x,p.y};
	[[self label] setShadowOffset:size];
}

-(void)setPadding:(UIEdgeInsets)inset
{
    [self label].viewInsets = inset;
}

-(void) updateNumberLines
{
    if ([[self label] minimumFontSize] >= 4.0)
    {
        [[self label] setNumberOfLines:1];
    }
    else if ([[self proxy] valueForKey:@"maxLines"])
        [[self label] setNumberOfLines:([[[self proxy] valueForKey:@"maxLines"] integerValue])];
    else
    {
        BOOL shouldWordWrap = [TiUtils boolValue:[[self proxy] valueForKey:@"wordWrap"] def:YES];
        if (shouldWordWrap)
        {
            [[self label] setNumberOfLines:0];
        }
        else
        {
            [[self label] setNumberOfLines:1];
        }
    }
}

-(void)setWordWrap_:(id)value
{
    [self updateNumberLines];
}

-(void)setMaxLines_:(id)value
{
	[self updateNumberLines];
}

-(void)setEllipsize_:(id)value
{
    [[self label] setLineBreakMode:[TiUtils intValue:value]];
}


-(void)setMultiLineEllipsize_:(id)value
{
    int multilineBreakMode = [TiUtils intValue:value];
    if (multilineBreakMode != UILineBreakModeWordWrap)
    {
        [[self label] setLineBreakMode:UILineBreakModeWordWrap];
    }
}

-(void)setTransition_:(id)arg
{
    ENSURE_SINGLE_ARG_OR_NIL(arg, NSDictionary)
    self.transition = arg;
}

#pragma mark -
#pragma mark DTAttributedTextContentViewDelegate

- (void)attributedLabel:(TTTAttributedLabel *)label
   didSelectLinkWithURL:(NSURL *)url
{
    if ([(TiViewProxy*)[self proxy] _hasListeners:@"link" checkParent:NO]) {
        NSDictionary *eventDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                   url, @"url",
                                   nil];
        [[self proxy] fireEvent:@"link" withObject:eventDict propagate:NO];
    }
    else [[UIApplication sharedApplication] openURL:url];
}

- (void)attributedLabel:(TTTAttributedLabel *)label
didSelectLinkWithAddress:(NSDictionary *)addressComponents
{
    NSMutableString* address = [NSMutableString string];
    NSString* temp = nil;
    if((temp = [addressComponents objectForKey:NSTextCheckingStreetKey]))
        [address appendString:temp];
    if((temp = [addressComponents objectForKey:NSTextCheckingCityKey]))
        [address appendString:[NSString stringWithFormat:@"%@%@", ([address length] > 0) ? @", " : @"", temp]];
    if((temp = [addressComponents objectForKey:NSTextCheckingStateKey]))
        [address appendString:[NSString stringWithFormat:@"%@%@", ([address length] > 0) ? @", " : @"", temp]];
    if((temp = [addressComponents objectForKey:NSTextCheckingZIPKey]))
        [address appendString:[NSString stringWithFormat:@" %@", temp]];
    if((temp = [addressComponents objectForKey:NSTextCheckingCountryKey]))
        [address appendString:[NSString stringWithFormat:@"%@%@", ([address length] > 0) ? @", " : @"", temp]];
    NSString* urlString = [NSString stringWithFormat:@"http://maps.google.com/maps?q=%@", [address stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
    if ([(TiViewProxy*)[self proxy] _hasListeners:@"link" checkParent:NO]) {
        NSDictionary *eventDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                   urlString, @"url",
                                   nil];
        [[self proxy] fireEvent:@"link" withObject:eventDict propagate:NO];
    }
    else [[UIApplication sharedApplication] openURL:[NSURL URLWithString:urlString]];
}

- (void)attributedLabel:(TTTAttributedLabel *)label
didSelectLinkWithPhoneNumber:(NSString *)phoneNumber
{
    NSURL* url = [NSURL URLWithString:[NSString stringWithFormat:@"tel://%@", phoneNumber]];
    if ([(TiViewProxy*)[self proxy] _hasListeners:@"link" checkParent:NO]) {
        NSDictionary *eventDict = [NSDictionary dictionaryWithObjectsAndKeys:
                                   url, @"url",
                                   nil];
        [[self proxy] fireEvent:@"link" withObject:eventDict propagate:NO];
    }
    else [[UIApplication sharedApplication] openURL:url];
}

-(void)setReusing:(BOOL)value
{
    _reusing = value;
}

-(NSDictionary*)dictionaryFromTouch:(UITouch*)touch
{
    NSDictionary* event = [super dictionaryFromTouch:touch];
    NSAttributedString* attString = label.attributedText;
    if (attString != nil) {
        CGPoint localPoint = [touch locationInView:label];
        NSURL* url = [self checkLinkAttributeForString:attString atPoint:localPoint];
        if (url){
            event = [[NSMutableDictionary alloc]initWithDictionary:event];
            [(NSMutableDictionary*)event setObject:url forKey:@"link"];
        }
    }
    return event;
}

-(NSDictionary*)dictionaryFromGesture:(UIGestureRecognizer*)gesture
{
    NSDictionary* event = [super dictionaryFromGesture:gesture];
    
    NSAttributedString* attString = label.attributedText;
    if (attString != nil) {
        CGPoint localPoint = [gesture locationInView:label];
        NSURL* url = [self checkLinkAttributeForString:attString atPoint:localPoint];
        if (url){
            event = [[NSMutableDictionary alloc]initWithDictionary:event];
            [(NSMutableDictionary*)event setObject:url forKey:@"link"];
        }
    }
    return event;
}
@end

#endif
