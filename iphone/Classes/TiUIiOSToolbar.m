/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UIIOSTOOLBAR) || defined(USE_TI_UITOOLBAR)

#import "TiUIiOSToolbar.h"
#import "TiViewProxy.h"
#import "TiUtils.h"
#import "TiColor.h"
#import "TiToolbarButton.h"
#import "TiToolbar.h"

@implementation TiUIiOSToolbar

-(void)dealloc
{
	[self performSelector:@selector(setItems_:) withObject:nil];
	RELEASE_TO_NIL(toolBar);
	[super dealloc];
}

-(UIToolbar *)toolBar
{
    if (toolBar == nil) {
        toolBar = [[UIToolbar alloc] initWithFrame:[self bounds]];
        [toolBar setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleBottomMargin];

        [self addSubview:toolBar];
        id extendVal = [[self proxy] valueForUndefinedKey:@"extendBackground"];
        extendsBackground = [TiUtils boolValue:extendVal def:NO];
        if (extendsBackground) {
            [toolBar setDelegate:(id<UIToolbarDelegate>)self];
            [self setClipsToBounds:NO];
            return toolBar;
        }

        [self setClipsToBounds:YES];
    }
    return toolBar;
}

- (NSInteger)positionForBar:(id)bar
{
    if (extendsBackground) {
#ifndef TI_USE_AUTOLAYOUT
#if defined(DEBUG) || defined(DEVELOPER)
        TiDimension myTop = ((TiViewProxy*)[self proxy]).layoutProperties->top;
        if (!TiDimensionEqual(myTop, TiDimensionMake(TiDimensionTypeDip, 20))) {
            NSLog(@"extendBackground is true but top is not 20");
        }
#endif
#endif
        return UIBarPositionTopAttached;
    }
    return UIBarPositionAny;
}

- (id)accessibilityElement
{
	return [self toolBar];
}

-(void)layoutSubviews
{
	CGRect ourBounds = [self bounds];
	CGFloat height = ourBounds.size.height;	
	if (height != [self verifyHeight:height])
	{
		[(TiViewProxy *)[self proxy] willChangeSize];
		return;
	}


	CGRect toolBounds;
	toolBounds.size = [[self toolBar] sizeThatFits:ourBounds.size];
	toolBounds.origin.x = 0.0;
	toolBounds.origin.y = hideTopBorder?-1.0:0.0;
	[toolBar setFrame:toolBounds];
}

-(void)setItems_:(id)value
{
	ENSURE_TYPE_OR_NIL(value,NSArray);
	if (value!=nil)
	{
		NSMutableArray * result = [NSMutableArray arrayWithCapacity:[value count]];
		Class proxyClass = [TiViewProxy class];
		for (id object in value) {
            TiViewProxy* vp = ( TiViewProxy*)[(TiViewProxy*)self.proxy createChildFromObject:object];
            if (!vp) continue;
			if ([vp conformsToProtocol:@protocol(TiToolbarButton)])
			{
				[(id<TiToolbarButton>)vp setToolbar:(id<TiToolbar>)self.proxy];
			}
            [vp setParent:(TiParentingProxy*)self.proxy];
            [vp windowWillOpen];
			[result addObject:[vp barButtonItem]];
            [vp windowDidOpen];
		}
		[[self toolBar] setItems:result];
	}
	else 
	{
		UIToolbar *toolbar = [self toolBar];
		if (toolbar!=nil)
		{
			for (id item in [toolbar items])
			{
                if ([item respondsToSelector:@selector(proxy)])
                {
                    TiViewProxy* p = [(TiViewProxy*)[item performSelector:@selector(proxy)] retain];
                    [p removeBarButtonView];
                    [p windowDidClose];
                    [self.proxy forgetProxy:p];
                    [p release];
                }
			}
		}
		[toolbar setItems:nil];
	}
}

-(void)setBarColor_:(id)value
{
	TiColor * newBarColor = [TiUtils colorValue:value];
	
	[[self toolBar] setBarStyle:[TiUtils barStyleForColor:newBarColor]];
	[toolBar setTranslucent:[TiUtils barTranslucencyForColor:newBarColor]];
	UIColor* barColor = [TiUtils barColorForColor:newBarColor];

    [toolBar performSelector:@selector(setBarTintColor:) withObject:barColor];
}

//-(void)setTintColor_:(id)color
//{
//    [super setTintColor_:color];
//    TiColor *ticolor = [TiUtils colorValue:color];
//    UIColor* theColor = [ticolor _color];
//    [[self toolBar] performSelector:@selector(setTintColor:) withObject:theColor];
//}

-(void)setTranslucent_:(id)value
{
	[[self toolBar] setTranslucent:[TiUtils boolValue:value]];
}

-(void)setStyle_:(id)value
{
	[[self toolBar] setBarStyle:[TiUtils intValue:value def:[self toolBar].barStyle]];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [[self toolBar] setFrame:bounds];
	[super frameSizeChanged:frame bounds:bounds];

}

-(CGSize)contentSizeForSize:(CGSize)size
{
    return [[self toolBar] sizeThatFits:size];
}

-(CGFloat)verifyHeight:(CGFloat)suggestedHeight
{
	CGFloat result = suggestedHeight;
	if (hideTopBorder)
	{
		result -= 1.0;
	}
	if (showBottomBorder)
	{
		result += 1.0;
	}
	return result;
}

@end

#endif
