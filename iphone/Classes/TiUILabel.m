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

@implementation TiUILabel

#pragma mark Internal

-(id)init
{
    if (self = [super init]) {
        padding = CGRectZero;
        textPadding = CGRectZero;
        initialLabelFrame = CGRectZero;
    }
    return self;
}

-(void)dealloc
{
    RELEASE_TO_NIL(label);
    [super dealloc];
}

//- (BOOL)interactionDefault
//{
//	// by default, labels don't have any interaction unless you explicitly add
//	// it via addEventListener
//	return NO;
//}


- (CGSize)suggestedFrameSizeToFitEntireStringConstraintedToWidth:(CGFloat)suggestedWidth
{
    CGSize maxSize = CGSizeMake(suggestedWidth<=0 ? 480 : suggestedWidth, 10000);
    maxSize.width -= textPadding.origin.x + textPadding.size.width;
    CGFloat textWidth = [[self label] sizeThatFits:maxSize].width;
    textWidth = MIN(textWidth,  maxSize.width);
    CGRect textRect = [[self label] textRectForBounds:CGRectMake(0,0,textWidth, maxSize.height) limitedToNumberOfLines:label.numberOfLines];
    
    textRect.size.height -= 2*textRect.origin.y;
    textRect.origin.y = 0;
    textRect.size.width += textPadding.origin.x + textPadding.size.width;
    textRect.size.height += textPadding.origin.y + textPadding.size.height;

    return textRect.size;
}

-(CGFloat)contentWidthForWidth:(CGFloat)suggestedWidth withHeight:(CGFloat)calculatedHeight
{
    return [self suggestedFrameSizeToFitEntireStringConstraintedToWidth:suggestedWidth].width;
}

-(CGFloat)contentHeightForWidth:(CGFloat)width
{
    return [self suggestedFrameSizeToFitEntireStringConstraintedToWidth:width].height;
}

-(void)padLabel
{
    if (!configurationSet) {
        needsPadLabel = YES;
        return; // lazy init
    }
	CGRect	initFrame = CGRectMake(initialLabelFrame.origin.x + textPadding.origin.x
                                   , initialLabelFrame.origin.y + textPadding.origin.y
                                   , initialLabelFrame.size.width - textPadding.origin.x - textPadding.size.width
                                   , initialLabelFrame.size.height - textPadding.origin.y - textPadding.size.height);
    
//    if(CGRectEqualToRect (label.frame, initFrame)); return;
    [label setFrame:initFrame];
    
    if ([self backgroundLayer] != nil && !CGRectIsEmpty(initialLabelFrame))
    {
        [self updateBackgroundImageFrameWithPadding];
    }
	[(TiViewProxy *)[self proxy] contentsWillChange];
	return;
}

-(void)setCenter:(CGPoint)newCenter
{
	[super setCenter:CGPointMake(floorf(newCenter.x), floorf(newCenter.y))];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
	initialLabelFrame = bounds;
    
    [self padLabel];
    
    [super frameSizeChanged:frame bounds:bounds];
}

- (void)configurationStart {
    [super configurationStart];
    needsUpdateBackgroundImageFrame = needsPadLabel = needsSetText = NO;
}

- (void)configurationSet {
    
    [super configurationSet];
    if (needsPadLabel)
        [self padLabel];
    
    if (needsUpdateBackgroundImageFrame)
        [self updateBackgroundImageFrameWithPadding];
    
//    if (needsSetText)
    [self setAttributedTextViewContent];
}

-(TTTAttributedLabel*)label
{
	if (label==nil)
	{
        label = [[TTTAttributedLabel alloc] initWithFrame:CGRectZero];
        label.backgroundColor = [UIColor clearColor];
        label.numberOfLines = 0;//default wordWrap to True
        label.lineBreakMode = UILineBreakModeWordWrap; //default ellipsis to none
        label.layer.shadowRadius = 0; //for backward compatibility
        label.layer.shadowOffset = CGSizeZero;
		label.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        label.delegate = self;
        [self addSubview:label];
	}
	return label;
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
        }, NO);
        return;
    }
    
    id attr = [(TiUILabelProxy*)[self proxy] getLabelContent];
//    if ([attr isKindOfClass:[NSAttributedString class]])
//        [[self label] setText:attr];
//    else
    [[self label] setText:attr];
    
    
}

-(void)setHighlighted:(BOOL)newValue
{
    [super setHighlighted:newValue];
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

#pragma mark Public APIs

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

//-(void)setText_:(id)value
//{
//	[self setAttributedTextViewContent];
//}
//
//-(void)setHtml_:(id)value
//{
//	[self setAttributedTextViewContent];
//}


-(void)setHighlightedColor_:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
	[[self label] setHighlightedTextColor:(newColor != nil)?newColor:[UIColor lightTextColor]];
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

-(void)setBackgroundImageLayerBounds:(CGRect)bounds
{
    if ([self backgroundLayer] != nil)
    {
        CGRect backgroundFrame = CGRectMake(bounds.origin.x - padding.origin.x,
                                            bounds.origin.y - padding.origin.y,
                                            bounds.size.width + padding.origin.x + padding.size.width,
                                            bounds.size.height + padding.origin.y + padding.size.height);
        [self backgroundLayer].frame = backgroundFrame;
    }
}

-(void) updateBackgroundImageFrameWithPadding
{
    if (!configurationSet){
        needsUpdateBackgroundImageFrame = YES;
        return; // lazy init
    }
    [self setBackgroundImageLayerBounds:self.bounds];
}

-(void)setBackgroundImage_:(id)url
{
    [super setBackgroundImage_:url];
    //if using padding we must not mask to bounds.
    [self backgroundLayer].masksToBounds = CGRectEqualToRect(padding, CGRectZero) ;
    [self updateBackgroundImageFrameWithPadding];
}

-(void)setBackgroundPaddingLeft_:(id)left
{
    padding.origin.x = [TiUtils floatValue:left];
    [self updateBackgroundImageFrameWithPadding];
}

-(void)setBackgroundPaddingRight_:(id)right
{
    padding.size.width = [TiUtils floatValue:right];
    [self updateBackgroundImageFrameWithPadding];
}

-(void)setBackgroundPaddingTop_:(id)top
{
    padding.origin.y = [TiUtils floatValue:top];
    [self updateBackgroundImageFrameWithPadding];
}

-(void)setBackgroundPaddingBottom_:(id)bottom
{
    padding.size.height = [TiUtils floatValue:bottom];
    [self updateBackgroundImageFrameWithPadding];
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

-(void)setTextPadding_:(id)value
{
	ENSURE_SINGLE_ARG(value,NSDictionary);
    NSDictionary* paddingDict = (NSDictionary*)value;
    if ([paddingDict objectForKey:@"left"]) {
        textPadding.origin.x = [TiUtils floatValue:[paddingDict objectForKey:@"left"]];
    }
    if ([paddingDict objectForKey:@"right"]) {
        textPadding.size.width = [TiUtils floatValue:[paddingDict objectForKey:@"right"]];
    }
    if ([paddingDict objectForKey:@"top"]) {
        textPadding.origin.y = [TiUtils floatValue:[paddingDict objectForKey:@"top"]];
    }
    if ([paddingDict objectForKey:@"bottom"]) {
        textPadding.size.height = [TiUtils floatValue:[paddingDict objectForKey:@"bottom"]];
    }
    [self padLabel];
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


#pragma mark -
#pragma mark DTAttributedTextContentViewDelegate

- (void)attributedLabel:(TTTAttributedLabel *)label
   didSelectLinkWithURL:(NSURL *)url
{
    [[UIApplication sharedApplication] openURL:url];
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
    [[UIApplication sharedApplication] openURL:[NSURL URLWithString:urlString]];
}

- (void)attributedLabel:(TTTAttributedLabel *)label
didSelectLinkWithPhoneNumber:(NSString *)phoneNumber
{
    NSURL* url = [NSURL URLWithString:[NSString stringWithFormat:@"tel://%@", phoneNumber]];
    [[UIApplication sharedApplication] openURL:url];
}

//- (void)attributedLabel:(TTTAttributedLabel *)label
//  didSelectLinkWithDate:(NSDate *)date
//{
//    [[UIApplication sharedApplication] openURL:url];
//}
//
//- (void)attributedLabel:(TTTAttributedLabel *)label
//  didSelectLinkWithDate:(NSDate *)date
//               timeZone:(NSTimeZone *)timeZone
//               duration:(NSTimeInterval)duration
//{
//    [[UIApplication sharedApplication] openURL:url];
//}

@end

#endif
