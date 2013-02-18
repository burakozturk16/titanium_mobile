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
#import "TiUIView.h"
#import <objc/runtime.h>

NSString * const kDefaultColorKey = @"kDefaultBackgroundColorKey";
NSString * const kHighlightedColorKey = @"kBackgroundHighlightedColorKey";
NSString * const kSelectedColorKey = @"kBackgroundSelectedColorKey";
NSString * const kDisabledColorKey = @"kBackgroundDisabledColorKey";

@implementation UIButton (backgroundColors)

- (void)setDefaultBackgroundColor:(UIColor *)color
{
	objc_setAssociatedObject(self, kDefaultColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    [super setBackgroundColor:color];
}

- (void)setBackgroundHighlightedColor:(UIColor *)color
{
	objc_setAssociatedObject(self, kHighlightedColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)setBackgroundSelectedColor:(UIColor *)color
{
	objc_setAssociatedObject(self, kSelectedColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (void)setBackgroundDisabledColor:(UIColor *)color
{
	objc_setAssociatedObject(self, kDisabledColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (UIColor*)backgroundHighlightedColor
{
	return objc_getAssociatedObject(self, kHighlightedColorKey);
}

- (UIColor*)backgroundSelectedColor
{
	return objc_getAssociatedObject(self, kSelectedColorKey);
}

- (UIColor*)backgroundDisabledColor
{
	return objc_getAssociatedObject(self, kDisabledColorKey);
}

- (UIColor*)defaultBackgroundColor
{
	return objc_getAssociatedObject(self, kDefaultColorKey);
}

-(void)updateBackgroundColor
{
    if (self.enabled)
    {
        if(self.highlighted && [self backgroundHighlightedColor])
            self.backgroundColor = [self backgroundHighlightedColor];
        else if(self.selected && [self backgroundSelectedColor])
            self.backgroundColor = [self backgroundSelectedColor];
        else if([self defaultBackgroundColor])
            self.backgroundColor = [self defaultBackgroundColor];
        else
            self.backgroundColor = [UIColor clearColor];
    }
    else
    {
        if ([self backgroundDisabledColor])
            self.backgroundColor = [self backgroundDisabledColor];
        else if([self defaultBackgroundColor])
            self.backgroundColor = [self defaultBackgroundColor];
        else
            self.backgroundColor = [UIColor clearColor];
    }
}

-(void)setSelected:(BOOL)selected
{
    [super setSelected:selected];
    [self updateBackgroundColor];
}

-(void)setHighlighted:(BOOL)highlighted
{
    [super setHighlighted:highlighted];
    [self updateBackgroundColor];
}

-(void)setEnabled:(BOOL)enabled
{
    [super setEnabled:enabled];
    [self updateBackgroundColor];
}

@end

@implementation TiUIButton

#pragma mark Internal

-(void)dealloc
{
	[button removeTarget:self action:NULL forControlEvents:UIControlEventAllTouchEvents];
	RELEASE_TO_NIL(button);
	RELEASE_TO_NIL(viewGroupWrapper);
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
        id value = [[self proxy] valueForKey:@"backgroundSelectedImage"];
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

-(UIView *) hitTest:(CGPoint)point withEvent:(UIEvent *)event {
	UIView *superResult = [super hitTest:point withEvent:event];
	
	if(superResult == nil) {
		return nil;
	}
	
	if((viewGroupWrapper == superResult) || ([superResult isKindOfClass:[TiUIView class]] 
	   && ![(TiUIView*)superResult touchEnabled])) {
		return [self button];
	}

	return superResult;
}

-(BOOL)hasTouchableListener
{
	// since this guy only works with touch events, we always want them
	// just always return YES no matter what listeners we have registered
	return YES;
}

-(void)setHighlighting:(BOOL)isHiglighted
{
	for (TiUIView * thisView in [viewGroupWrapper subviews])
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
    NSMutableDictionary *evt = [NSMutableDictionary dictionaryWithDictionary:[TiUtils pointToDictionary:[touch locationInView:self]]];

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
		
        UIButtonType defaultType = (hasImage==YES) ? UIButtonTypeCustom : UIButtonTypeRoundedRect;
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
	}
	if ((viewGroupWrapper != nil) && ([viewGroupWrapper	superview]!=button)) {
		[viewGroupWrapper setFrame:[button bounds]];
		[button addSubview:viewGroupWrapper];
	}
	return button;
}

- (id)accessibilityElement
{
	return [self button];
}

-(UIView *) viewGroupWrapper
{
	if (viewGroupWrapper == nil) {
		viewGroupWrapper = [[UIView alloc] initWithFrame:[self bounds]];
		[viewGroupWrapper setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
	}
	if (button != [viewGroupWrapper superview]) {
		if (button != nil) {
			[viewGroupWrapper setFrame:[button bounds]];
			[button addSubview:viewGroupWrapper];
		}
		else {
			[viewGroupWrapper removeFromSuperview];
		}
	}
	return viewGroupWrapper;
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
    if (!configurationSet) return;
	[backgroundImageCache release];
	RELEASE_TO_NIL(backgroundImageUnstretchedCache);
	backgroundImageCache = [[self loadImage:value] retain];
    self.backgroundImage = value;
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
		[[self button] setDefaultBackgroundColor:[color _color]];
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

-(void)setTitlePaddingLeft_:(id)left
{
	titlePadding.origin.x = [TiUtils floatValue:left];
	[button setTitleEdgeInsets:UIEdgeInsetsMake(titlePadding.origin.y, titlePadding.origin.x, titlePadding.size.height, titlePadding.size.width)];
}

-(void)setTitlePaddingRight_:(id)right
{
	titlePadding.size.width = [TiUtils floatValue:right];
	[button setTitleEdgeInsets:UIEdgeInsetsMake(titlePadding.origin.y, titlePadding.origin.x, titlePadding.size.height, titlePadding.size.width)];
}

-(void)setTitlePaddingTop_:(id)top
{
	titlePadding.origin.y = [TiUtils floatValue:top];
	[button setTitleEdgeInsets:UIEdgeInsetsMake(titlePadding.origin.y, titlePadding.origin.x, titlePadding.size.height, titlePadding.size.width)];
}

-(void)setTitlePaddingBottom_:(id)bottom
{
	titlePadding.size.height = [TiUtils floatValue:bottom];
	[button setTitleEdgeInsets:UIEdgeInsetsMake(titlePadding.origin.y, titlePadding.origin.x, titlePadding.size.height, titlePadding.size.width)];
}

-(void)setWordWrap_:(id)value
{
	BOOL shouldWordWrap = [TiUtils boolValue:value def:YES];
	if (shouldWordWrap)
		[[button titleLabel] setLineBreakMode:UILineBreakModeWordWrap];
	else 
		[[button titleLabel] setLineBreakMode:UILineBreakModeTailTruncation];
}

-(void)setVerticalAlign_:(id)alignment
{
	[button setContentVerticalAlignment:[TiUtils contentVerticalAlignmentValue:alignment]];
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
