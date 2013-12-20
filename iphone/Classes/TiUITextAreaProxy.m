/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITEXTAREA

#import "TiUITextAreaProxy.h"
#import "TiUITextArea.h"

@implementation TiUITextAreaProxy
{
    UIEdgeInsets _padding;
}

+(NSSet*)transferableProperties
{
    NSSet *common = [TiUITextWidgetProxy transferableProperties];
    return [common setByAddingObjectsFromSet:[NSSet setWithObjects:@"enabled",
                                              @"scrollable",@"editable",@"autoLink",
                                              @"borderStyle", nil]];
}

#pragma mark Defaults

DEFINE_DEF_PROP(value,@"");
DEFINE_DEF_PROP(scrollsToTop,@YES);
DEFINE_DEF_INT_PROP(maxLength,-1);

-(NSString*)apiName
{
    return @"Ti.UI.TextArea";
}

-(id)init
{
    if (self = [super init]) {
        _padding = UIEdgeInsetsZero;
    }
    return self;
}

-(void)setPadding:(id)value
{
    _padding = [TiUtils insetValue:value];
    if (view != nil)
        [(TiUITextArea*)view setPadding:_padding];
    [self contentsWillChange];
}

-(void)configurationSet
{
    [super configurationSet];
    [(TiUITextArea*)view setPadding:_padding];
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    if (view != nil)
        return [(TiUITextArea*)view contentSizeForSize:size];
    else
    {
        NSString* text = [TiUtils stringValue:[self valueForKey:@"value"]];
        CGSize resultSize = CGSizeZero;
        CGSize maxSize = CGSizeMake(size.width<=0 ? 480 : size.width, size.height<=0 ? 10000 : size.height);
        maxSize.width -= _padding.left + _padding.right;
        
        UILineBreakMode breakMode = UILineBreakModeWordWrap;
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