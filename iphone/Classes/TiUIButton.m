/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIBUTTON

#import "TiUIButton.h"
#import "TiUIButtonProxy.h"

#import "TiUtils.h"
#import "ImageLoader.h"
#import "TiButtonUtil.h"
#import "UIButton+BackgroundColors.h"
#import "TiUIView.h"

@implementation TiUIButton

#pragma mark Internal

-(void)dealloc
{
	[button removeTarget:self action:NULL forControlEvents:UIControlEventAllTouchEvents];
	RELEASE_TO_NIL(button);
	RELEASE_TO_NIL(backgroundImageCache)
	RELEASE_TO_NIL(backgroundImageUnstretchedCache);
	[super dealloc];
}

-(void)configurationSet
{
    [super configurationSet];
    
    if (needsToSetBackgroundImage)
    {
        // to prevent multiple calls because of topCap and leftCap

        id value = [[self proxy] valueForKey:@"backgroundImage"];
        if (value)
            [self setBackgroundImage_:value];
        value = [[self proxy] valueForKey:@"backgroundSelectedImage"];
        if (value)
            [self setBackgroundSelectedImage_:value];
        
        value = [[self proxy] valueForKey:@"backgroundHighlightedImage"];
        if (value)
            [self setBackgroundHighlightedImage_:value];
        
        value = [[self proxy] valueForKey:@"backgroundFocusedImage"];
        if (value)
            [self setBackgroundFocusedImage_:value];

        value = [[self proxy] valueForKey:@"backgroundDisabledImage"];
        if (value)
            [self setBackgroundDisabledImage_:value];
    }
}

-(BOOL)hasTouchableListener
{
	// since this guy only works with touch events, we always want them
	// just always return YES no matter what listeners we have registered
	return YES;
}

-(void)setHighlighting:(BOOL)isHiglighted
{
	for (TiUIView * thisView in [self subviews])
	{
		if ([thisView respondsToSelector:@selector(setHighlighted:)])
		{
			[(id)thisView setHighlighted:isHiglighted];
		}
	}
}

-(void)updateBackgroundImage
{
	CGRect bounds = [self bounds];
	[button setFrame:bounds];
	if ((backgroundImageCache == nil) || (bounds.size.width == 0) || (bounds.size.height == 0)) {
		[button setBackgroundImage:nil forState:UIControlStateNormal];
		return;
	}
	CGSize imageSize = [backgroundImageCache size];
	if((bounds.size.width>=imageSize.width) && (bounds.size.height>=imageSize.height)){
		[button setBackgroundImage:backgroundImageCache forState:UIControlStateNormal];
		return;
	}
    //If the bounds are smaller than the image size render it in an imageView and get the image of the view.
    //Should be pretty inexpensive since it happens rarely. TIMOB-9166
    CGSize unstrechedSize = (backgroundImageUnstretchedCache != nil) ? [backgroundImageUnstretchedCache size] : CGSizeZero;
    if (backgroundImageUnstretchedCache == nil || !CGSizeEqualToSize(unstrechedSize,bounds.size) ) {
        UIImageView* theView = [[UIImageView alloc] initWithFrame:bounds];
        [theView setImage:backgroundImageCache];
        UIGraphicsBeginImageContextWithOptions(bounds.size, [theView.layer isOpaque], 0.0);
        [theView.layer renderInContext:UIGraphicsGetCurrentContext()];
        UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
        RELEASE_TO_NIL(backgroundImageUnstretchedCache);
        backgroundImageUnstretchedCache = [image retain];
        [theView release];
    }
	[button setBackgroundImage:backgroundImageUnstretchedCache forState:UIControlStateNormal];	
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
	[super frameSizeChanged:frame bounds:bounds];
	[self updateBackgroundImage];
}

- (void)controlAction:(id)sender forEvent:(UIEvent *)event
{
    UITouch *touch = [[event allTouches] anyObject];
    NSString *fireEvent;
    NSString * fireActionEvent = nil;
    NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];

    switch (touch.phase) {
        case UITouchPhaseBegan:
            if (touchStarted) {
                return;
            }
            touchStarted = YES;
            fireEvent = @"touchstart";
            break;
        case UITouchPhaseMoved:
            fireEvent = @"touchmove";
            break;
        case UITouchPhaseEnded:
            touchStarted = NO;
            fireEvent = @"touchend";
            if (button.highlighted) {
                if ([touch tapCount] == 2 && [self.proxy _hasListeners:@"dblclick" ]) {
                    [self.proxy fireEvent:@"dblclick"  withObject:evt];
                }
                fireActionEvent = @"click";
            }
            break;
        case UITouchPhaseCancelled:
            touchStarted = NO;
            fireEvent = @"touchcancel";
            break;
        default:
            return;
    }
    [self setHighlighting:button.highlighted];
    if ((fireActionEvent != nil) && [self.proxy _hasListeners:fireActionEvent]) {
        [self.proxy fireEvent:fireActionEvent withObject:evt];
    }
	if ([self.proxy _hasListeners:fireEvent]) {
		[self.proxy fireEvent:fireEvent withObject:evt];
	}
}

-(UIButton*)button
{
	if (button==nil)
	{
        BOOL hasImage = [self.proxy valueForKey:@"backgroundImage"]!=nil;
        BOOL hasBgdColor = [self.proxy valueForKey:@"backgroundColor"]!=nil;
		
        UIButtonType defaultType = (hasImage==YES || hasBgdColor==YES) ? UIButtonTypeCustom : UIButtonTypeRoundedRect;
		style = [TiUtils intValue:[self.proxy valueForKey:@"style"] def:defaultType];
		UIView *btn = [TiButtonUtil buttonWithType:style];
		button = (UIButton*)[btn retain];
		[button titleLabel].lineBreakMode = UILineBreakModeWordWrap; //default wordWrap to True
        [[[button titleLabel] layer] setShadowRadius:0]; //default like label
		[self addSubview:button];
		if (style==UIButtonTypeCustom)
		{
			[button setTitleColor:[UIColor whiteColor] forState:UIControlStateHighlighted];
		}
		[button addTarget:self action:@selector(controlAction:forEvent:) forControlEvents:UIControlEventAllTouchEvents];
		button.exclusiveTouch = YES;
        [self addSubview:button];
    }
	return button;
}

//-(UIView *) hitTest:(CGPoint)point withEvent:(UIEvent *)event {
//	if ([[self button] pointInside:point withEvent:event]) {
//		return [self button];
//    }
//    
//	return [super hitTest:point withEvent:event];
//}

- (id)accessibilityElement
{
	return [self button];
}

#pragma mark Public APIs

-(void)setStyle_:(id)style_
{
	int s = [TiUtils intValue:style_ def:UIButtonTypeCustom];
	if (s == style)
	{
		return;
	}
	style = s;

	
	if (button==nil)
	{
		return;
	}

	RELEASE_TO_NIL(button);
	[self button];
}

-(void)setImage_:(id)value
{
	UIImage *image = value==nil ? nil : [TiUtils image:value proxy:(TiProxy*)self.proxy];
	if (image!=nil)
	{
		[[self button] setImage:image forState:UIControlStateNormal];
		[(TiViewProxy *)[self proxy] contentsWillChange];
	}
	else
	{
		[[self button] setImage:nil forState:UIControlStateNormal];
	}
}

-(void)setEnabled_:(id)value
{
	[[self button] setEnabled:[TiUtils boolValue:value]];
    [self setBgState:[self button].enabled];
}


-(void)setSelected_:(id)value
{
	[[self button] setSelected:[TiUtils boolValue:value]];
}


-(void)setTitle_:(id)value
{
	[[self button] setTitle:[TiUtils stringValue:value] forState:UIControlStateNormal];
}

-(void)setBackgroundImage_:(id)value
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
	[backgroundImageCache release];
	RELEASE_TO_NIL(backgroundImageUnstretchedCache);
	backgroundImageCache = [[self loadImage:value] retain];
//    self.backgroundImage = value;
	[self updateBackgroundImage];
}

-(void)setBackgroundHighlightedImage_:(id)value
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
	[[self button] setBackgroundImage:[self loadImage:value] forState:UIControlStateHighlighted];
}

-(void)setBackgroundSelectedImage_:(id)value
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
    UIImage* image = [self loadImage:value];
	[[self button] setBackgroundImage:image forState:UIControlStateHighlighted];
	[[self button] setBackgroundImage:image forState:UIControlStateSelected];
}

-(void)setBackgroundDisabledImage_:(id)value
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
	[[self button] setBackgroundImage:[self loadImage:value] forState:UIControlStateDisabled];
}

-(void)setBackgroundFocusedImage_:(id)value
{
    if (!configurationSet) {
        needsToSetBackgroundImage = YES;
        return;
    }
	[[self button] setBackgroundImage:[self loadImage:value] forState:UIControlStateSelected];
}

-(void)setBackgroundColor_:(id)value
{
	if (value!=nil)
	{
		TiColor *color = [TiUtils colorValue:value];
		[[self button] setBackgroundDefaultColor:[color _color]];
	}
}

-(void)setBackgroundSelectedColor_:(id)value
{
    if (value!=nil)
    {
        TiColor *color = [TiUtils colorValue:value];
        [[self button] setBackgroundSelectedColor:[color _color]];
        [[self button] updateBackgroundColor];
    }
}

-(void)setBackgroundHighlightedColor_:(id)value
{
    if (value!=nil)
    {
        TiColor *color = [TiUtils colorValue:value];
        [[self button] setBackgroundHighlightedColor:[color _color]];
        [[self button] updateBackgroundColor];
    }
}

-(void)setBackgroundDisabledColor_:(id)value
{
    if (value!=nil)
    {
        TiColor *color = [TiUtils colorValue:value];
        [[self button] setBackgroundDisabledColor:[color _color]];
        [[self button] updateBackgroundColor];
    }
}

-(void)setFont_:(id)font
{
	if (font!=nil)
	{
		WebFont *f = [TiUtils fontValue:font def:nil];
		[[[self button] titleLabel] setFont:[f font]];
	}
}

-(void)setColor_:(id)color
{
	if (color!=nil)
	{
		TiColor *c = [TiUtils colorValue:color];
		UIButton *b = [self button];
		if (c!=nil)
		{
			[b setTitleColor:[c _color] forState:UIControlStateNormal];
		}
		else if (b.buttonType==UIButtonTypeCustom)
		{
			[b setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
		}
	}
}

-(void)setHighlightedColor_:(id)color
{
	if (color!=nil)
	{
		TiColor *selColor = [TiUtils colorValue:color];
		UIButton *b = [self button];
		if (selColor!=nil)
		{
			[b setTitleColor:[selColor _color] forState:UIControlStateHighlighted];
		}
		else if (b.buttonType==UIButtonTypeCustom)
		{
			[b setTitleColor:[UIColor whiteColor] forState:UIControlStateHighlighted];
		}
	}
}

-(void)setSelectedColor_:(id)color
{
	if (color!=nil)
	{
		TiColor *selColor = [TiUtils colorValue:color];
		UIButton *b = [self button];
		if (selColor!=nil)
		{
            UIColor* uicolor = [selColor _color];
			[b setTitleColor:uicolor forState:UIControlStateSelected];
			[b setTitleColor:uicolor forState:UIControlStateHighlighted];
		}
		else if (b.buttonType==UIButtonTypeCustom)
		{
            UIColor* uicolor = [UIColor whiteColor];
			[b setTitleColor:uicolor forState:UIControlStateSelected];
			[b setTitleColor:uicolor forState:UIControlStateHighlighted];
		}
	}
}

-(void)setTextAlign_:(id)alignment
{
    UIButton *b = [self button];
    [[b titleLabel] setTextAlignment:[TiUtils textAlignmentValue:alignment]];
    [b setContentHorizontalAlignment:[TiUtils contentHorizontalAlignmentValueFromTextAlignment:alignment]];
    [b setNeedsLayout];
}

-(void)setShadowColor_:(id)color
{
	UIButton *b = [self button];
	if (color==nil)
	{
		[[b titleLabel] setShadowColor:nil];
	}
	else
	{
        color = [TiUtils colorValue:color];
        CGFloat alpha = CGColorGetAlpha([color _color].CGColor);
		[[[b titleLabel] layer] setShadowColor:[color _color].CGColor];
		[[[b titleLabel] layer] setShadowOpacity:alpha];
	}
}

-(void)setShadowOffset_:(id)value
{
	UIButton *b = [self button];
	CGPoint p = [TiUtils pointValue:value];
	CGSize size = {p.x,p.y};
	[[[b titleLabel] layer] setShadowOffset:size];
}

-(void)setShadowRadius_:(id)arg
{
	UIButton *b = [self button];
	[[[b titleLabel] layer] setShadowRadius:[TiUtils floatValue:arg]];
}

-(void)setTitlePadding_:(id)value
{
	ENSURE_SINGLE_ARG(value,NSDictionary);
    NSDictionary* padding = (NSDictionary*)value;
    if ([padding objectForKey:@"left"]) {
        titlePadding.left = [TiUtils floatValue:[padding objectForKey:@"left"]];
    }
    if ([padding objectForKey:@"right"]) {
        titlePadding.right = [TiUtils floatValue:[padding objectForKey:@"right"]];
    }
    if ([padding objectForKey:@"top"]) {
        titlePadding.top = [TiUtils floatValue:[padding objectForKey:@"top"]];
    }
    if ([padding objectForKey:@"bottom"]) {
        titlePadding.bottom = [TiUtils floatValue:[padding objectForKey:@"bottom"]];
    }
	[button setTitleEdgeInsets:titlePadding];
    [button setNeedsLayout];
}

-(void)setWordWrap_:(id)value
{
	BOOL shouldWordWrap = [TiUtils boolValue:value def:YES];
	if (shouldWordWrap)
		[[button titleLabel] setLineBreakMode:UILineBreakModeWordWrap];
	else 
		[[button titleLabel] setLineBreakMode:UILineBreakModeTailTruncation];
    [button setNeedsLayout];
}

-(void)setVerticalAlign_:(id)alignment
{
	[button setContentVerticalAlignment:[TiUtils contentVerticalAlignmentValue:alignment]];
    [button setNeedsLayout];
}

-(CGFloat)contentWidthForWidth:(CGFloat)value
{
	return [[self button] sizeThatFits:CGSizeMake(value, 0)].width;
}

-(CGFloat)contentHeightForWidth:(CGFloat)value
{
	return [[self button] sizeThatFits:CGSizeMake(value, 0)].height;
}

@end

#endif
