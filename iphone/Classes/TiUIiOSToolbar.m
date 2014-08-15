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
        if ([TiUtils isIOS7OrGreater]) {
            id extendVal = [[self proxy] valueForUndefinedKey:@"extendBackground"];
            extendsBackground = [TiUtils boolValue:extendVal def:NO];
            if (extendsBackground) {
                [toolBar setDelegate:(id<UIToolbarDelegate>)self];
                [self setClipsToBounds:NO];
                return toolBar;
            }
        }

        [self setClipsToBounds:YES];
    }
    return toolBar;
}

- (NSInteger)positionForBar:(id)bar
{
    if (extendsBackground) {
#if defined(DEBUG) || defined(DEVELOPER)
        TiDimension myTop = ((TiViewProxy*)[self proxy]).layoutProperties->top;
        if (!TiDimensionEqual(myTop, TiDimensionMake(TiDimensionTypeDip, 20))) {
            NSLog(@"extendBackground is true but top is not 20");
        }
#endif
        return UIBarPositionTopAttached;
    }
    return UIBarPositionAny;
}

- (id)accessibilityElement
{
	return [self toolBar];
}

-(void)drawRect:(CGRect)rect
{
	[super drawRect:rect];
	if (!showBottomBorder || [TiUtils isIOS7OrGreater])
	{
		return;
	}

	CGRect toolFrame = [self bounds];

    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextSetGrayStrokeColor(context, 0.0, 1.0);
	CGContextSetLineWidth(context, 1.0);
	CGContextSetShouldAntialias(context,false);
	CGPoint bottomBorder[2];
	
	CGFloat x = toolFrame.origin.x;
	CGFloat y = toolFrame.origin.y+toolFrame.size.height;
	if ([self respondsToSelector:@selector(contentScaleFactor)] && [self contentScaleFactor] > 1.0)
	{ //Yes, this seems very hackish. Very low priority would be to use something more elegant.
		y -= 0.5;
	}
	bottomBorder[0]=CGPointMake(x,y);
	x += toolFrame.size.width;
	bottomBorder[1]=CGPointMake(x,y);
	CGContextStrokeLineSegments(context,bottomBorder,2);
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
                    TiViewProxy* p = (TiViewProxy*)[item performSelector:@selector(proxy)];
                    [p removeBarButtonView];
                    [p windowDidClose];
                    [self.proxy forgetProxy:p];
                }
			}
		}
		[toolbar setItems:nil];
	}
}

-(void)setBorderTop_:(id)value
{
    if (![TiUtils isIOS7OrGreater]) {
        hideTopBorder = ![TiUtils boolValue:value def:YES];
        [(TiViewProxy *)[self proxy] willChangeSize];
    }
}

-(void)setBorderBottom_:(id)value
{
    if (![TiUtils isIOS7OrGreater]) {
        showBottomBorder = [TiUtils boolValue:value def:NO];
        [(TiViewProxy *)[self proxy] willChangeSize];
    }
}

-(void)setBackgroundImage_:(id)arg
{
    if ([TiUtils isIOS7OrGreater]) {
        UIImage *image = [self loadImage:arg];
        [[self toolBar] setBackgroundImage:image forToolbarPosition:(extendsBackground?UIBarPositionTopAttached:UIBarPositionAny) barMetrics:UIBarMetricsDefault];
    } else {
        [super setBackgroundImage_:arg];
    }
}

-(void)setBarColor_:(id)value
{
	TiColor * newBarColor = [TiUtils colorValue:value];
	
	[[self toolBar] setBarStyle:[TiUtils barStyleForColor:newBarColor]];
	[toolBar setTranslucent:[TiUtils barTranslucencyForColor:newBarColor]];
	UIColor* barColor = [TiUtils barColorForColor:newBarColor];

	if ([TiUtils isIOS7OrGreater]) {
		[toolBar performSelector:@selector(setBarTintColor:) withObject:barColor];
	} else {
		[toolBar setTintColor:barColor];
	}
}

-(void)setTintColor_:(id)color
{
    if ([TiUtils isIOS7OrGreater]) {
        TiColor *ticolor = [TiUtils colorValue:color];
        UIColor* theColor = [ticolor _color];
        [[self toolBar] performSelector:@selector(setTintColor:) withObject:theColor];
        [self performSelector:@selector(setTintColor:) withObject:theColor];
    }
}

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
