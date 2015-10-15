/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIBUTTON

#import "TiUINavBarButton.h"
#import "TiUtils.h"
#import "ImageLoader.h"
#import "TiUIButtonProxy.h"
#import "TiUIButton.h"
#import "TiButtonUtil.h"
#import "TiUIView.h"
#import "TiBlob.h"

#define NAVBAR_MEMORY_DEBUG 0

@implementation TiUINavBarButton
@synthesize proxy;

DEFINE_EXCEPTIONS

#if NAVBAR_MEMORY_DEBUG==1
-(id)retain
{
	NSLog(@"[DEBUG] Retaining %X (%d)",self,[self retainCount]);
	return [super retain];
}

-(void)release
{
	NSLog(@"[DEBUG] Releasing %X (%d)",self,[self retainCount]);
	[super release];
}
#endif

-(void)dealloc
{
#if NAVBAR_MEMORY_DEBUG==1
	NSLog(@"[DEBUG] Deallocing %X (%d)",self,[self retainCount]);
#endif
	RELEASE_TO_NIL(activityDelegate);
    RELEASE_TO_NIL(proxy)
	[super dealloc];
}

-(void)detachProxy
{
    RELEASE_TO_NIL(proxy)
}

-(UIBarButtonItemStyle)style:(TiUIButtonProxy*)proxy_
{
	id value = [proxy_ valueForKey:@"style"];
	if (value==nil)
	{
		return UIBarButtonItemStylePlain;
	}
	return [TiUtils intValue:value];
}

-(NSString*)title:(TiUIButtonProxy*)proxy_
{
	NSString *title = [TiUtils stringValue:[proxy_ valueForKey:@"title"]];
	return title == nil ? @"" : title;
}

-(id)initWithProxy:(TiUIButtonProxy*)proxy_
{
	self = [super init];
	id systemButton = [proxy_ valueForKey:@"systemButton"];
	if (systemButton!=nil)
	{
		NSInteger type = [TiUtils intValue:systemButton];
		UIView *button = [TiButtonUtil systemButtonWithType:type];
		if (button!=nil)
		{
			if ([button isKindOfClass:[UIActivityIndicatorView class]])
			{
				// we need to wrap our activity indicator view into a UIView that will delegate
				// to our proxy
				activityDelegate = [[TiUIView alloc] initWithFrame:button.frame];
				[activityDelegate addSubview:button];
				activityDelegate.proxy = (TiViewProxy*)proxy_;
				button = [activityDelegate autorelease];
			}
			self = [super initWithCustomView:button];
			self.target = self;
			self.action = @selector(clicked:);
			if ([button isKindOfClass:[UIControl class]])
			{
				[(UIControl*)button addTarget:self action:@selector(clicked:) forControlEvents:UIControlEventTouchUpInside];
			}
		}
		else
		{
			self = [super initWithBarButtonSystemItem:type target:self action:@selector(clicked:)];
		}
	}
    else 
    {
        id image = [proxy_ valueForKey:@"image"];
        id background = [proxy_ valueForKey:@"backgroundImage"];
        if (background != nil) {
            self = [super initWithCustomView:[proxy_ getOrCreateView]];
            self.target = self;
            self.action = @selector(clicked:);

            if ([[proxy_ view] isKindOfClass:[UIControl class]])
            { 
                [(UIControl*)[proxy_ view] addTarget:self action:@selector(clicked:) forControlEvents:UIControlEventTouchUpInside];
            }
            //Sanity check. If the view bounds are zero set the bounds to auto dimensions
            CGRect bounds = [[proxy_ view] bounds];
            if (bounds.size.width == 0 || bounds.size.height == 0) {
#ifdef TI_USE_AUTOLAYOUT
                bounds.size = [[proxy_ view] sizeThatFits:CGSizeMake(1000, 1000)];
#else
                bounds.size = [proxy_ autoSizeForSize:CGSizeMake(1000, 1000)];
#endif
                
            }
            [[proxy_ view] setBounds:bounds];
        }
        else if (image!=nil) {
            NSURL *url = [TiUtils toURL:image proxy:proxy_];
            UIImage *theimage = [[ImageLoader sharedLoader] loadImmediateStretchableImage:url];
            self = [super initWithImage:theimage style:[self style:proxy_] target:self action:@selector(clicked:)];
        }
        else {
            self = [super initWithTitle:[self title:proxy_] style:[self style:proxy_] target:self action:@selector(clicked:)];
        }
    }
	
    proxy = [proxy_ retain];
    proxy.modelDelegate = self;
    self.accessibilityLabel = [proxy_ valueForUndefinedKey:@"accessibilityLabel"];
    
    id<NSFastEnumeration> values = [proxy allKeys];
    [self readProxyValuesWithKeys:values];
	
    return self;
}

-(void)clicked:(id)event
{
	if ([proxy _hasListeners:@"click"])
	{
		[proxy fireEvent:@"click" withObject:nil checkForListener:NO];
	}
}

-(void)setTitle_:(id)obj
{
	[super setTitle:[TiUtils stringValue:obj]];
}

-(void)setFont_:(id)font
{
    if (font!=nil)
	{
		WebFont *f = [TiUtils fontValue:font def:nil];
        NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateNormal]];
        [dict setObject:[f font] forKey:UITextAttributeFont];
        [super setTitleTextAttributes:dict forState:UIControlStateNormal];
	}
}

-(void)setColor_:(id)color
{
    UIColor * newColor = [[TiUtils colorValue:color] _color];
    if (newColor == nil) {
        newColor = [UIColor darkTextColor];
    }
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateNormal]];
    [dict setObject:newColor forKey:UITextAttributeTextColor];
    [super setTitleTextAttributes:dict forState:UIControlStateNormal];
}

-(void)setHighlightedColor_:(id)color
{
    UIColor * newColor = [[TiUtils colorValue:color] _color];
    if (newColor == nil) {
        newColor = [UIColor lightTextColor];
    }
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateHighlighted]];
    [dict setObject:newColor forKey:NSForegroundColorAttributeName];
    [super setTitleTextAttributes:dict forState:UIControlStateHighlighted];
}

-(void)setSelectedColor_:(id)color
{
    UIColor * newColor = [[TiUtils colorValue:color] _color];
    if (newColor == nil) {
        newColor = [UIColor lightTextColor];
    }
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateHighlighted]];
    [dict setObject:newColor forKey:NSForegroundColorAttributeName];
    [super setTitleTextAttributes:dict forState:UIControlStateHighlighted];
    dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateSelected]];
    [dict setObject:newColor forKey:NSForegroundColorAttributeName];
    [super setTitleTextAttributes:dict forState:UIControlStateSelected];
}

-(void)setDisabledColor_:(id)color
{
    UIColor * newColor = [[TiUtils colorValue:color] _color];
    if (newColor == nil) {
        newColor = [UIColor lightTextColor];
    }
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateDisabled]];
    [dict setObject:newColor forKey:NSForegroundColorAttributeName];
    [super setTitleTextAttributes:dict forState:UIControlStateDisabled];
}


-(void)setShadowColor_:(id)color
{
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateNormal]];
    color = [[TiUtils colorValue:color] _color];
	if (color==nil)
	{
        [dict removeObjectForKey:UITextAttributeTextShadowColor];
	}
	else
	{
        [dict setObject:color forKey:UITextAttributeTextShadowColor];
	}
    [super setTitleTextAttributes:dict forState:UIControlStateNormal];
}

-(void)setShadowOffset_:(id)value
{
	CGPoint p = [TiUtils pointValue:value];
	CGSize size = {p.x,p.y};
    NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithDictionary:[self titleTextAttributesForState:UIControlStateNormal]];
    [dict setObject:[NSValue valueWithCGSize:size] forKey:UITextAttributeTextShadowOffset];
    [super setTitleTextAttributes:dict forState:UIControlStateNormal];
}

-(void)setImage_:(id)obj
{
	if (obj == nil) {
		[super setImage:nil];
		return;
	}
	
	if ([obj isKindOfClass:[TiBlob class]]) {
		[super setImage:[(TiBlob*)obj image]];
	}
	else if ([obj isKindOfClass:[NSString class]]) {
		[super setImage:[TiUtils image:obj proxy:proxy]];
	}
	else if ([obj isKindOfClass:[UIImage class]]) {
		[super setImage:obj];
	}
	else {
		[self throwException:[NSString stringWithFormat:@"Unexpected object of type %@ provided for image",[obj class]]
				   subreason:nil
					location:CODELOCATION];
	}
}

-(void)setWidth_:(id)obj
{
	CGFloat width = [TiUtils floatValue:obj];
	[self setWidth:width];
}

-(void)setEnabled_:(id)value
{
	UIView * buttonView = [self customView];

	if ([buttonView isKindOfClass:[TiUIButton class]])
	{
		//TODO: when using a TiUIButton, for some reason the setEnabled doesn't work.
		//So we're just going to let it do all the work of updating.
		[(TiUIButton *)buttonView setEnabled_:value];
	}
	else
	{
		BOOL enabled = [TiUtils boolValue:value];
		[self setEnabled:enabled];
	}
}

-(void)readProxyValuesWithKeys:(id<NSFastEnumeration>)keys
{
	DoProxyDelegateReadValuesWithKeysFromProxy(self, keys, proxy);
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
	// Take into account whether or not we're a custom view
	id changeView = (self.customView != nil) ? (id)self.customView : (id)self;
	DoProxyDelegateChangedValuesWithProxy(changeView, key, oldValue, newValue, proxy_);
}

@end

#endif
