/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIBUTTON

#import "TiUIButtonProxy.h"
#import "TiUIButton.h"
#import "TiUINavBarButton.h"
#import "TiUtils.h"

@implementation TiUIButtonProxy
{
    UIEdgeInsets _padding;
}

-(id)init
{
    if (self = [super init]) {
        _padding = UIEdgeInsetsZero;
    }
    return self;
}

-(void)_destroy
{
	RELEASE_TO_NIL(button);
    toolbar = nil;
	[super _destroy];
}

-(void)_configure
{	
	[self setValue:NUMBOOL(YES) forKey:@"enabled"];
	[self setValue:NUMBOOL(NO) forKey:@"selected"];
	[super _configure];
}

-(NSMutableDictionary*)langConversionTable
{
    return [NSMutableDictionary dictionaryWithObject:@"title" forKey:@"titleid"];
}

-(void)setStyle:(id)value
{
	styleCache = [TiUtils intValue:value def:UIButtonTypeCustom];
	[self replaceValue:value forKey:@"style" notification:YES];
}

-(NSString*)apiName
{
    return @"Ti.UI.Button";
}

-(UIBarButtonItem*)barButtonItem
{
    /*
	id backgroundImageValue = [self valueForKey:@"backgroundImage"];
	if (!IS_NULL_OR_NIL(backgroundImageValue))
	{
		return [super barButtonItem];
	}
	*/
    
	if (button==nil || !isUsingBarButtonItem)
	{
		isUsingBarButtonItem = YES;
        if (button == nil) {
            button = [[TiUINavBarButton alloc] initWithProxy:self];
        }
	}
	return button;
}

-(UIButton*)button {
    return [(TiUIButton*)view button];
}

-(UIViewAutoresizing) verifyAutoresizing:(UIViewAutoresizing)suggestedResizing
{
	switch ((int)styleCache)
	{
		case UITitaniumNativeItemInfoLight:
		case UITitaniumNativeItemInfoDark:
		case UITitaniumNativeItemDisclosure:
			return suggestedResizing & ~(UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
		default: {
			break;
		}
	}
	return suggestedResizing;
}

-(BOOL)optimizeSubviewInsertion
{
    return YES;
}

-(void)removeBarButtonView
{
    // If we remove the button here, it could be the case that the system
    // sends a message to a released UIControl on the interior of the button,
    // causing a crash. Very timing-dependent.
    
    //	RELEASE_TO_NIL(button);
    if (button) {
        [button detachProxy];
    }
    [super removeBarButtonView];
}

-(void)setToolbar:(id<TiToolbar>)toolbar_
{
	toolbar = toolbar_;
}

-(id<TiToolbar>)toolbar
{
	return [[toolbar retain] autorelease];
}

-(BOOL)attachedToToolbar
{
	return toolbar!=nil;
}

-(void)fireEvent:(NSString*)type withObject:(id)obj propagate:(BOOL)propagate reportSuccess:(BOOL)report errorCode:(NSInteger)code message:(NSString*)message checkForListener:(BOOL)checkForListener
{
	if (![TiUtils boolValue:[self valueForKey:@"enabled"] def:YES])
	{
		//Rogue event. We're supposed to be disabled!
		return;
	}
	[super fireEvent:type withObject:obj propagate:propagate reportSuccess:report errorCode:code message:message checkForListener:checkForListener];
}

-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoSize;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

-(void)setPadding:(id)value
{
    _padding = [TiUtils insetValue:value];
    if (view != nil)
        [(TiUIButton*)view setPadding:_padding];
    [self contentsWillChange];
    [self replaceValue:value forKey:@"padding" notification:NO];
}

-(id)padding {
    return [self valueForUndefinedKey:@"padding"];
}


-(void)configurationSet
{
    [super configurationSet];
    [(TiUIButton*)view setPadding:_padding];
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    if (view != nil)
        return [(TiUIButton*)view contentSizeForSize:size];
    else
    {
        NSString* text = [TiUtils stringValue:[self valueForKey:@"title"]];
        CGSize resultSize = CGSizeZero;
        CGSize maxSize = CGSizeMake(size.width<=0 ? 480 : size.width, size.height<=0 ? 10000 : size.height);
        maxSize.width -= _padding.left + _padding.right;
        
        NSLineBreakMode breakMode = NSLineBreakByWordWrapping;
        id fontValue = [self valueForKey:@"font"];
        UIFont * font;
        if (fontValue!=nil)
        {
            font = [[TiUtils fontValue:fontValue] font];
        }
        else
        {
            font = [UIFont systemFontOfSize:17];
        }
        resultSize = [text sizeWithFont:font constrainedToSize:maxSize lineBreakMode:breakMode];
        resultSize.width = roundf(resultSize.width);
        resultSize.height = roundf(resultSize.height);
        resultSize.width += _padding.left + _padding.right;
        resultSize.height += _padding.top + _padding.bottom;
        return resultSize;
    }
    return CGSizeZero;
}

@end

#endif
