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
#import "UIControl+TiUIView.h"


@implementation TiUIButton

#pragma mark Internal

-(void)dealloc
{
	[button removeTarget:self action:NULL forControlEvents:UIControlEventAllTouchEvents];
	RELEASE_TO_NIL(button);
	[super dealloc];
}


-(BOOL)hasTouchableListener
{
	// since this guy only works with touch events, we always want them
	// just always return YES no matter what listeners we have registered
	return YES;
}

-(UIView*)viewForHitTest
{
    return button;
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
    if ((fireActionEvent != nil) && [self.proxy _hasListeners:fireActionEvent]) {
        [self.proxy fireEvent:fireActionEvent withObject:evt checkForListener:NO];
    }
	if ([self.proxy _hasListeners:fireEvent]) {
		[self.proxy fireEvent:fireEvent withObject:evt checkForListener:NO];
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
		button.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        
        [button setTiUIView:self];
        [self addSubview:button];
    }
	return button;
}

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
	UIImage *image = [self loadImage:value];
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
    [super setEnabled_:value];
	[[self button] setEnabled:[self interactionEnabled]];
}

-(void)setExclusiveTouch:(BOOL)value
{
    [super setExclusiveTouch:value];
	[[self button] setExclusiveTouch:value];
}

-(void)setSelected_:(id)value
{
	[[self button] setSelected:[TiUtils boolValue:value]];
}


-(void)setTitle_:(id)value
{
	[[self button] setTitle:[TiUtils stringValue:value] forState:UIControlStateNormal];
}

//-(void)setBackgroundImage_:(id)value
//{
//    if (!configurationSet) {
//        needsToSetBackgroundImage = YES;
//        return;
//    }
//	[backgroundImageCache release];
//	RELEASE_TO_NIL(backgroundImageUnstretchedCache);
//	backgroundImageCache = [[self loadImage:value] retain];
////    self.backgroundImage = value;
//	[self updateBackgroundImage];
//}

//-(void)setBackgroundHighlightedImage_:(id)value
//{
//    if (!configurationSet) {
//        needsToSetBackgroundImage = YES;
//        return;
//    }
//	[[self button] setBackgroundImage:[self loadImage:value] forState:UIControlStateHighlighted];
//}

//-(void)setBackgroundSelectedImage_:(id)value
//{
//    if (!configurationSet) {
//        needsToSetBackgroundImage = YES;
//        return;
//    }
//    UIImage* image = [self loadImage:value];
//	[[self button] setBackgroundImage:image forState:UIControlStateHighlighted];
//	[[self button] setBackgroundImage:image forState:UIControlStateSelected];
//}
//
//-(void)setBackgroundDisabledImage_:(id)value
//{
//    if (!configurationSet) {
//        needsToSetBackgroundImage = YES;
//        return;
//    }
//	[[self button] setBackgroundImage:[self loadImage:value] forState:UIControlStateDisabled];
//}

//-(void)setBackgroundFocusedImage_:(id)value
//{
//    if (!configurationSet) {
//        needsToSetBackgroundImage = YES;
//        return;
//    }
//	[[self button] setBackgroundImage:[self loadImage:value] forState:UIControlStateSelected];
//}
//
//
//-(void)setBackgroundColor_:(id)value
//{
//	if (value!=nil)
//	{
//		TiColor *color = [TiUtils colorValue:value];
//		[[self button] setBackgroundDefaultColor:[color _color]];
//	}
//}

//-(void)setBackgroundSelectedColor_:(id)value
//{
//    if (value!=nil)
//    {
//        TiColor *color = [TiUtils colorValue:value];
//        [[self button] setBackgroundSelectedColor:[color _color]];
//        [[self button] updateBackgroundColor];
//    }
//}

//-(void)setBackgroundHighlightedColor_:(id)value
//{
//    if (value!=nil)
//    {
//        TiColor *color = [TiUtils colorValue:value];
//        [[self button] setBackgroundHighlightedColor:[color _color]];
//        [[self button] updateBackgroundColor];
//    }
//}
//
//-(void)setBackgroundDisabledColor_:(id)value
//{
//    if (value!=nil)
//    {
//        TiColor *color = [TiUtils colorValue:value];
//        [[self button] setBackgroundDisabledColor:[color _color]];
//        [[self button] updateBackgroundColor];
//    }
//}

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

-(void)setDisabledColor_:(id)color
{
	if (color!=nil)
	{
		TiColor *selColor = [TiUtils colorValue:color];
		UIButton *b = [self button];
		if (selColor!=nil)
		{
            UIColor* uicolor = [selColor _color];
			[b setTitleColor:uicolor forState:UIControlStateDisabled];
		}
		else if (b.buttonType==UIButtonTypeCustom)
		{
            UIColor* uicolor = [UIColor whiteColor];
			[b setTitleColor:uicolor forState:UIControlStateDisabled];
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

-(void)setPadding:(UIEdgeInsets)inset
{
	[button setTitleEdgeInsets:inset];
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

-(CGSize)contentSizeForSize:(CGSize)value
{
    CGSize result = [[self button] sizeThatFits:value];
    result.width += [self button].titleEdgeInsets.left + [self button].titleEdgeInsets.right;
    result.height += [self button].titleEdgeInsets.top + [self button].titleEdgeInsets.bottom;

    return result;
}


@end

#endif
