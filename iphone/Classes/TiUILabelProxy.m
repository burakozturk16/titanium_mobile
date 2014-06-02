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
#import "DTCoreText.h"

#define kDefaultFontSize 12.0

static inline CTTextAlignment UITextAlignmentToCTTextAlignment(UITextAlignment alignment)
{
    switch (alignment) {
        case UITextAlignmentLeft:
            return kCTLeftTextAlignment;
        case UITextAlignmentRight:
            return kCTRightTextAlignment;
        default:
            return kCTCenterTextAlignment;
            break;
    }
}

static inline CTLineBreakMode UILineBreakModeToCTLineBreakMode(UILineBreakMode linebreak)
{
    switch (linebreak) {
        case UILineBreakModeClip:
            return kCTLineBreakByClipping;
        case UILineBreakModeCharacterWrap:
            return kCTLineBreakByCharWrapping;
        case UILineBreakModeHeadTruncation:
            return kCTLineBreakByTruncatingHead;
        case UILineBreakModeTailTruncation:
            return kCTLineBreakByTruncatingTail;
        case UILineBreakModeMiddleTruncation:
            return kCTLineBreakByTruncatingMiddle;
        case UILineBreakModeWordWrap:
        default:
            return kCTLineBreakByWordWrapping;
            break;
    }
}

@implementation TiUILabelProxy
{
    UIEdgeInsets _padding;
}

+(NSSet*)transferableProperties
{
    NSSet *common = [TiViewProxy transferableProperties];
    return [common setByAddingObjectsFromSet:[NSSet setWithObjects:@"text",@"html",
                                              @"color", @"highlightedColor", @"autoLink",
                                              @"verticalAlign", @"textAlign", @"font",
                                              @"minimumFontSize", @"backgroundPaddingLeft",
                                              @"backgroundPaddingRight", @"backgroundPaddingBottom", @"backgroundPaddingTop", @"shadowOffset",
                                              @"shadowRadius", @"shadowColor",
                                              @"padding",
                                              @"wordWrap", @"borderWidth", @"maxLines",
                                              @"ellipsize", @"multiLineEllipsize", nil]];
}

-(NSString*)apiName
{
    return @"Ti.UI.Label";
}

-(id)init
{
    if (self = [super init]) {
        _padding = UIEdgeInsetsZero;
        attributeTextNeedsUpdate = YES;
        options = [[NSMutableDictionary dictionaryWithObjectsAndKeys:
                    NSHTMLTextDocumentType, NSDocumentTypeDocumentAttribute,
                    [NSNumber numberWithInt:kCTLeftTextAlignment], DTDefaultTextAlignment,
                    [NSNumber numberWithInt:0], DTDefaultFontStyle,
                    @"Helvetica", DTDefaultFontFamily,
                    @"Helvetica", NSFontAttributeName,
                    [NSNumber numberWithFloat:(17 / kDefaultFontSize)], NSTextSizeMultiplierDocumentOption,
                    [NSNumber numberWithInt:kCTLineBreakByWordWrapping], DTDefaultLineBreakMode, nil] retain];
        if ([TiUtils isIOS6OrGreater])
        {
            [options setObject:@YES forKey:DTUseiOS6Attributes];
            if ([TiUtils isIOS7OrGreater])
            {
            }
        }
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
    [(TiUILabel *)[self view] setReusing:NO];
    if (attributeTextNeedsUpdate)
        [self updateAttributeText];
    [super configurationSet:recursive];
}


- (void)prepareForReuse
{
    [(TiUILabel *)[self view] setReusing:YES];
    [super prepareForReuse];
}

-(void)setPadding:(id)value
{
    _padding = [TiUtils insetValue:value];
    if (view != nil)
        [(TiUILabel*)view setPadding:_padding];
    [self contentsWillChange];
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
                int numberOfLines = 0;
                if ([self valueForKey:@"maxLines"])
                {
                    numberOfLines = [TiUtils intValue:[self valueForKey:@"maxLines"]];
                }
                result = [TDTTTAttributedLabel sizeThatFitsAttributedString:_realLabelContent withConstraints:maxSize limitedToNumberOfLines:numberOfLines];
            }
            else
            {
                UILineBreakMode breakMode = UILineBreakModeWordWrap;
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
                int numberOfLines = 0;
                if ([self valueForKey:@"maxLines"])
                {
                    numberOfLines = [TiUtils intValue:[self valueForKey:@"maxLines"]];
                }
                font.lineHeight;
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
		labelKeySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"font",@"color",@"textAlign",@"multiLineEllipsize"]] retain];
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
        [options setObject:@"Helvetica" forKey:NSFontAttributeName];
        [options setValue:@"Helvetica" forKey:DTDefaultFontFamily];
    }
    [options setValue:[NSNumber numberWithFloat:(webFont.size / kDefaultFontSize)] forKey:NSTextSizeMultiplierDocumentOption];
    
    //we need to reset the text to update default paragraph settings
	[self replaceValue:font forKey:@"font" notification:YES];
    [self updateAttributeText];
}

-(void)setTextAlign:(id)alignment
{
    [options setValue:[NSNumber numberWithInt:UITextAlignmentToCTTextAlignment([TiUtils textAlignmentValue:alignment])] forKey:DTDefaultTextAlignment];
    
    //we need to reset the text to update default paragraph settings
	[self replaceValue:alignment forKey:@"textAlign" notification:YES];
    [self updateAttributeText];
}

-(void)setMultiLineEllipsize:(id)value
{
    int multilineBreakMode = [TiUtils intValue:value];
    if (multilineBreakMode != UILineBreakModeWordWrap)
    {
        [options setValue:[NSNumber numberWithInt:UILineBreakModeToCTLineBreakMode(multilineBreakMode)]  forKey:DTDefaultLineBreakMode];
        
        //we need to update the text
        [self updateAttributeText];
    }
	[self replaceValue:value forKey:@"multiLineEllipsize" notification:YES];
}

- (void)setAttributedTextViewContent:(id)newContentString ofType:(ContentType)contentType {
    if (newContentString == nil) {
        RELEASE_TO_NIL(contentString);
        RELEASE_TO_NIL(_realLabelContent);
        _contentHash = 0;
        [self updateAttributeText];
        return;
    }
    
    // we don't preserve the string but compare it's hash
	NSUInteger newHash = [newContentString hash];
	
	if (newHash == _contentHash)
	{
		return;
	}
    _contentHash = newHash;
    RELEASE_TO_NIL(contentString);
    contentString = [newContentString retain];
    _contentType = contentType;
    [self updateAttributeText];
}

-(id)getLabelContent
{
    return _realLabelContent;
}


@end

#endif
