/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UITEXTFIELD

#import "TiUITextFieldProxy.h"
#import "TiUITextField.h"

@implementation TiUITextFieldProxy
{
    UIEdgeInsets _padding;
}

+(NSSet*)transferableProperties
{
    NSSet *common = [TiUITextWidgetProxy transferableProperties];
    return [common setByAddingObjectsFromSet:[NSSet setWithObjects:@"paddingLeft",
                                              @"paddingRight",@"leftButtonPadding",@"rightButtonPadding",
                                              @"editable", @"enabled", @"hintText", @"minimumFontSize",
                                              @"clearOnEdit", @"borderStyle", @"clearButtonMode",
                                              @"leftButton", @"leftButtonMode", @"verticalAlign", 
                                              @"rightButton", @"rightButtonMode",
                                              @"backgroundDisabledImage", nil]];
}

#pragma mark Defaults

DEFINE_DEF_PROP(value,@"");
DEFINE_DEF_BOOL_PROP(enabled,YES);
DEFINE_DEF_BOOL_PROP(enableReturnKey,NO);
DEFINE_DEF_BOOL_PROP(editable,YES);
DEFINE_DEF_BOOL_PROP(autocorrect,NO);
DEFINE_DEF_BOOL_PROP(clearOnEdit,NO);
DEFINE_DEF_BOOL_PROP(passwordMask,NO);
DEFINE_DEF_NULL_PROP(backgroundImage);
DEFINE_DEF_NULL_PROP(backgroundDisabledImage);
DEFINE_DEF_NULL_PROP(color);
DEFINE_DEF_NULL_PROP(font);
DEFINE_DEF_NULL_PROP(hintText);
DEFINE_DEF_NULL_PROP(leftButton);
DEFINE_DEF_NULL_PROP(rightButton);
DEFINE_DEF_NULL_PROP(keyboardToolbar);
DEFINE_DEF_NULL_PROP(keyboardToolbarColor);
DEFINE_DEF_INT_PROP(keyboardToolbarHeight,0);
DEFINE_DEF_INT_PROP(textAlign,UITextAlignmentLeft);
DEFINE_DEF_INT_PROP(verticalAlign,UIControlContentVerticalAlignmentCenter);
DEFINE_DEF_INT_PROP(returnKeyType,UIReturnKeyDefault);
DEFINE_DEF_INT_PROP(keyboardType,UIKeyboardTypeDefault);
DEFINE_DEF_INT_PROP(borderStyle,UITextBorderStyleLine);
DEFINE_DEF_INT_PROP(clearButtonMode,UITextFieldViewModeNever);
DEFINE_DEF_INT_PROP(leftButtonMode,UITextFieldViewModeNever);
DEFINE_DEF_INT_PROP(rightButtonMode,UITextFieldViewModeNever);
DEFINE_DEF_INT_PROP(appearance,UIKeyboardAppearanceDefault);
DEFINE_DEF_INT_PROP(autocapitalization,UITextAutocapitalizationTypeNone);
DEFINE_DEF_INT_PROP(maxLength,-1);

-(NSString*)apiName
{
    return @"Ti.UI.TextField";
}

-(id)init
{
    if (self = [super init]) {
        _padding = UIEdgeInsetsZero;
    }
    return self;
}

-(void)configurationSet
{
    [super configurationSet];
    [(TiUITextField*)view setPadding:_padding];
}

-(void)setPadding:(id)value
{
    _padding = [TiUtils insetValue:value];
    if (view != nil)
        [(TiUITextField*)view setPadding:_padding];
    [self contentsWillChange];
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    if (view != nil)
        return [(TiUITextWidget*)view contentSizeForSize:size];
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
        maxSize.height = font.lineHeight; //textfield has one line
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