/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILABEL

#import "TiUILabelProxy.h"
#import "TiUILabel.h"
#import "TiUtils.h"
#import "DTCoreText/DTCoreText.h"

#define kDefaultFontSize 12.0

//static inline CTTextAlignment UITextAlignmentToCTTextAlignment(UITextAlignment alignment)
//{
//    switch (alignment) {
//        case UITextAlignmentLeft:
//            return kCTLeftTextAlignment;
//        case UITextAlignmentRight:
//            return kCTRightTextAlignment;
//        default:
//            return kCTCenterTextAlignment;
//            break;
//    }
//}


static inline CTLineBreakMode NSLineBreakModeToCTLineBreakMode(NSLineBreakMode linebreak)
{
    switch (linebreak) {
        case NSLineBreakByClipping:
            return kCTLineBreakByClipping;
        case NSLineBreakByCharWrapping:
            return kCTLineBreakByCharWrapping;
        case NSLineBreakByTruncatingHead:
            return kCTLineBreakByTruncatingHead;
        case NSLineBreakByTruncatingTail:
            return kCTLineBreakByTruncatingTail;
        case NSLineBreakByTruncatingMiddle:
            return kCTLineBreakByTruncatingMiddle;
        case NSLineBreakByWordWrapping:
        default:
            return kCTLineBreakByWordWrapping;
            break;
    }
}

@implementation TiUILabelProxy
{
    UIEdgeInsets _padding;
}

-(NSString*)apiName
{
    return @"Ti.UI.Label";
}

-(NSString*)defaultSystemFontFamily
{
    static NSString *defaultSystemFontFamily = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        defaultSystemFontFamily = [UIFont systemFontOfSize:[UIFont systemFontSize]].familyName;
    });
    return defaultSystemFontFamily;
}


static NSDictionary* htmlOptions;
-(NSDictionary *)htmlOptions
{
    if (htmlOptions == nil)
    {
        htmlOptions = [@{
                        DTDefaultTextAlignment:@(kCTLeftTextAlignment),
                        DTDefaultFontStyle:@(0),
                        DTIgnoreLinkStyleOption:@(NO),
                        DTDefaultFontFamily:[self defaultSystemFontFamily],
                        NSFontAttributeName:[self defaultSystemFontFamily],
                        NSTextSizeMultiplierDocumentOption:@(17 / kDefaultFontSize),
                        DTUseiOS6Attributes:@YES,
                        DTDocumentPreserveTrailingSpaces:@(YES),
                        DTDefaultLineBreakMode:@(kCTLineBreakByWordWrapping)} retain];
    }
    return htmlOptions;
}

-(id)init
{
    if (self = [super init]) {
        _padding = UIEdgeInsetsZero;
        attributeTextNeedsUpdate = YES;
        options = [[self htmlOptions] mutableCopy];
    }
    return self;
}


-(void)dealloc
{
    RELEASE_TO_NIL(contentString);
    RELEASE_TO_NIL(options);
    RELEASE_TO_NIL(_realLabelContent);
    [super dealloc];
}


- (void)updateAttributeText {
    
    if (!configSet) {
        attributeTextNeedsUpdate = YES;
        return; // lazy init
    }
    
    RELEASE_TO_NIL(_realLabelContent);
    if (contentString) {
        switch (_contentType) {
            case kContentTypeHTML:
            {
                //            if ([TiUtils isIOS7OrGreater]) {
                //                _realLabelContent = [[NSAttributedString alloc] initWithData:[contentString dataUsingEncoding:NSUTF8StringEncoding] options:options documentAttributes:nil error:nil];
                //            }
                //            else {
                _realLabelContent = [[NSAttributedString alloc] initWithHTMLData:[contentString dataUsingEncoding:NSUTF8StringEncoding] options:options documentAttributes:nil];
                //            }
                break;
            }
            default:
            {
                _realLabelContent = [contentString retain];
                break;
            }
        }
    }
   
    if (view!=nil) {
        [(TiUILabel*)view setAttributedTextViewContent];
    }
    [self contentsWillChange];
    attributeTextNeedsUpdate = NO;
}

-(void)configurationStart:(BOOL)recursive
{
    configSet = NO;
    [super configurationStart:recursive];
}

-(void)configurationSet:(BOOL)recursive
{
    configSet = YES;
    [(TiUILabel*)view setPadding:_padding];
//    [(TiUILabel *)[self view] setReusing:NO];
    if (attributeTextNeedsUpdate)
        [self updateAttributeText];
    [super configurationSet:recursive];
}


//- (void)prepareForReuse
//{
//    [(TiUILabel *)[self view] setReusing:YES];
//    [super prepareForReuse];
//}

-(void)setPadding:(id)value
{
    _padding = [TiUtils insetValue:value];
    if (view != nil)
        [(TiUILabel*)view setPadding:_padding];
    [self contentsWillChange];
    [self replaceValue:value forKey:@"padding" notification:NO];
}

-(id)padding {
    return [self valueForUndefinedKey:@"padding"];
}

-(CGSize) suggestedSizeForSize:(CGSize)size
{
    if (view != nil)
        return [(TiUILabel*)view suggestedFrameSizeToFitEntireStringConstraintedToSize:size];
    else
    {
        if (_realLabelContent != nil)
        {
            CGSize result = CGSizeZero;
            CGSize maxSize = CGSizeMake(size.width<=0 ? 480 : size.width, size.height<=0 ? 10000 : size.height);
            maxSize.width -= _padding.left + _padding.right;
            if ([_realLabelContent isKindOfClass:[NSAttributedString class]])
            {
                NSInteger numberOfLines = 0;
                if ([self valueForKey:@"maxLines"])
                {
                    numberOfLines = [TiUtils intValue:[self valueForKey:@"maxLines"]];
                }
                result = [TDTTTAttributedLabel sizeThatFitsAttributedString:_realLabelContent withConstraints:maxSize limitedToNumberOfLines:numberOfLines];
            }
            else
            {
                NSLineBreakMode breakMode = NSLineBreakByWordWrapping;
                if ([self valueForKey:@"ellipsize"])
                    breakMode = [TiUtils intValue:[self valueForKey:@"ellipsize"]];
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
                NSInteger numberOfLines = 0;
                if ([self valueForKey:@"maxLines"])
                {
                    numberOfLines = [TiUtils intValue:[self valueForKey:@"maxLines"]];
                }
//                font.lineHeight;
                result = [(NSString*)_realLabelContent sizeWithFont:font constrainedToSize:maxSize lineBreakMode:breakMode];
                if (numberOfLines > 0)
                {
                    CGFloat fontHeight = font.lineHeight;
                    int currentNbLines = result.height / fontHeight;
                    if (currentNbLines > numberOfLines)
                    {
                        result.height = numberOfLines * fontHeight;
                    }
                }
           }
            result.width = ceilf(result.width); //use ceilf to get same result as sizeThatFits
            result.height = ceilf(result.height); //use ceilf to get same result as sizeThatFits
            result.width += _padding.left + _padding.right;
            result.height += _padding.top + _padding.bottom;
            if (size.width > 0) result.width = MIN(result.width,  size.width);
            if (size.height > 0) result.height = MIN(result.height,  size.height);
            return result;
        }
    }
    return CGSizeZero;
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    return [self suggestedSizeForSize:size];
}

//-(CGFloat) verifyWidth:(CGFloat)suggestedWidth
//{
//	int width = ceil(suggestedWidth);
//	if (width != suggestedWidth && width & 0x01)
//	{
//		width ++;
//	}
//	return width;
//}
//
//-(CGFloat) verifyHeight:(CGFloat)suggestedHeight
//{
//	int height = ceil(suggestedHeight);
//	if (height != suggestedHeight && height & 0x01)
//	{
//		height ++;
//	}
//	return height;
//}


-(NSArray *)keySequence
{
	static NSArray *labelKeySequence = nil;
	static dispatch_once_t onceToken;
	dispatch_once(&onceToken, ^{
		labelKeySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"font",@"color",@"textAlign",@"multiLineEllipsize", @"disableLinkStyle"]] retain];
	});
	return labelKeySequence;
}

-(NSMutableDictionary*)langConversionTable
{
    return [NSMutableDictionary dictionaryWithObject:@"text" forKey:@"textid"];
}

-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoSize;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}


//we do it in the proxy for faster performances in tableviews
-(void)setText:(id)value
{
    //the test is for listview measurement when a same template is used for text and html
    if (value || _contentType == kContentTypeText)
        [self setAttributedTextViewContent:[TiUtils stringValue:value] ofType:kContentTypeText];
	[self replaceValue:value forKey:@"text" notification:NO];
}

-(void)setHtml:(id)value
{
    //the test is for listview measurement when a same template is used for text and html
    if (value || _contentType == kContentTypeHTML)
        [self setAttributedTextViewContent:[TiUtils stringValue:value] ofType:kContentTypeHTML];
	[self replaceValue:value forKey:@"html" notification:NO];
}

-(void)setFont:(id)font
{
    WebFont* webFont =[TiUtils fontValue:font];
    int traitsDefault = 0;
    if (webFont.isItalicStyle)
        traitsDefault |= kCTFontItalicTrait;
    if (webFont.isBoldWeight)
        traitsDefault |= kCTFontBoldTrait;
    [options setValue:[NSNumber numberWithInt:traitsDefault] forKey:DTDefaultFontStyle];
    if (webFont.family)
        [options setValue:webFont.family forKey:DTDefaultFontFamily];
    else {
        [options setObject:[self defaultSystemFontFamily] forKey:NSFontAttributeName];
        [options setValue:[self defaultSystemFontFamily] forKey:DTDefaultFontFamily];
    }
    [options setValue:[NSNumber numberWithFloat:(webFont.size / kDefaultFontSize)] forKey:NSTextSizeMultiplierDocumentOption];
    
    //we need to reset the text to update default paragraph settings
	[self replaceValue:font forKey:@"font" notification:YES];
    [self updateAttributeText];
}

-(void)setTextAlign:(id)alignment
{
    [options setValue:NUMINTEGER(DTNSTextAlignmentToCTTextAlignment([TiUtils textAlignmentValue:alignment])) forKey:DTDefaultTextAlignment];
    
    //we need to reset the text to update default paragraph settings
	[self replaceValue:alignment forKey:@"textAlign" notification:YES];
    [self updateAttributeText];
}

-(void)setDisableLinkStyle:(id)value
{
    [options setValue:@([TiUtils boolValue:value def:NO]) forKey:DTIgnoreLinkStyleOption];
    
    //we need to reset the text to update default paragraph settings
	[self replaceValue:value forKey:@"disableLinkStyle" notification:YES];
    [self updateAttributeText];
}

-(void)setMultiLineEllipsize:(id)value
{
    NSInteger multilineBreakMode = [TiUtils intValue:value];
    if (multilineBreakMode != NSLineBreakByWordWrapping)
    {
        [options setValue:NUMINTEGER(NSLineBreakModeToCTLineBreakMode(multilineBreakMode)) forKey:DTDefaultLineBreakMode];
        
        //we need to update the text
        [self updateAttributeText];
    }
	[self replaceValue:value forKey:@"multiLineEllipsize" notification:YES];
}

- (void)setAttributedTextViewContent:(id)newContentString ofType:(ContentType)contentType {
    if ((newContentString == nil && contentString == nil) || [newContentString isEqual:contentString])
    {
        return;
    }
    RELEASE_TO_NIL(contentString);
    contentString = [newContentString retain];
    _contentType = contentType;
    [self updateAttributeText];
}

-(id)getLabelContent
{
    return _realLabelContent;
}


-(id)characterIndexAtPoint:(id)args
{
    NSInteger result = -1;
    if (view!=nil) {
        ENSURE_SINGLE_ARG(args, NSDictionary)
        CGPoint point = [TiUtils pointValue:args];
        result = [(TiUILabel*)view characterIndexAtPoint:point];
    }
    return NUMINTEGER(result);
}


@end

#endif
