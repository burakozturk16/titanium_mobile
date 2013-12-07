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
        attributeTextNeedsUpdate = YES;
        UIColor* color = [UIColor darkTextColor];
        options = [[NSMutableDictionary dictionaryWithObjectsAndKeys:
                    [NSNumber numberWithInt:kCTLeftTextAlignment], DTDefaultTextAlignment,
                    color, DTDefaultTextColor,
                    color, DTDefaultLinkColor,
                    [NSNumber numberWithInt:0], DTDefaultFontStyle,
                    @"Helvetica", DTDefaultFontFamily,
                     [NSNumber numberWithFloat:(17 / kDefaultFontSize)], NSTextSizeMultiplierDocumentOption,
                    [NSNumber numberWithInt:kCTLineBreakByWordWrapping], DTDefaultLineBreakMode, nil] retain];
        
        if ([TiUtils isIOS6OrGreater])
        {
            [options setObject:@YES forKey:DTUseiOS6Attributes];
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
            _realLabelContent = [[NSAttributedString alloc] initWithHTMLData:[contentString dataUsingEncoding:NSUTF8StringEncoding] options:options documentAttributes:nil];
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
        [self contentsWillChange];
    }
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
    if (attributeTextNeedsUpdate)
        [self updateAttributeText];
    [super configurationSet:recursive];
}

-(CGSize) suggestedSizeForSize:(CGSize)size
{
    if (view != nil)
        return [(TiUILabel*)view suggestedFrameSizeToFitEntireStringConstraintedToSize:size];
    else
    {
        if (_realLabelContent != nil)
        {
            CGSize resultSize = CGSizeZero;
            CGRect textPadding = CGRectZero;
            if ([self valueForKey:@"padding"]) {
                NSDictionary* paddingDict = (NSDictionary*)[self valueForKey:@"padding"];
                if ([paddingDict objectForKey:@"left"]) {
                    textPadding.origin.x = [TiUtils floatValue:[paddingDict objectForKey:@"left"]];
                }
                if ([paddingDict objectForKey:@"right"]) {
                    textPadding.size.width = [TiUtils floatValue:[paddingDict objectForKey:@"right"]];
                }
                if ([paddingDict objectForKey:@"top"]) {
                    textPadding.origin.y = [TiUtils floatValue:[paddingDict objectForKey:@"top"]];
                }
                if ([paddingDict objectForKey:@"bottom"]) {
                    textPadding.size.height = [TiUtils floatValue:[paddingDict objectForKey:@"bottom"]];
                };
            }
            CGSize maxSize = CGSizeMake(size.width<=0 ? 480 : size.width, size.height<=0 ? 10000 : size.height);
            maxSize.width -= textPadding.origin.x + textPadding.size.width;
            if ([_realLabelContent isKindOfClass:[NSAttributedString class]])
            {
                
                if ([[NSAttributedString class] instancesRespondToSelector:@selector(boundingRectWithSize:options:context:)])
                {
                    resultSize = [(NSAttributedString*)_realLabelContent boundingRectWithSize:maxSize options:(NSStringDrawingUsesLineFragmentOrigin|NSStringDrawingUsesFontLeading) context:nil].size;
                }else {
                    CTFramesetterRef framesetter = CTFramesetterCreateWithAttributedString((CFAttributedStringRef)_realLabelContent);
                    resultSize = CTFramesetterSuggestFrameSizeWithConstraints(framesetter, CFRangeMake(0, [_realLabelContent length]), NULL, maxSize, NULL);
                    CFRelease(framesetter);
                }
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
                resultSize = [(NSString*)_realLabelContent sizeWithFont:font constrainedToSize:maxSize lineBreakMode:breakMode];
            }
            resultSize.width += textPadding.origin.x + textPadding.size.width;
            resultSize.height += textPadding.origin.y + textPadding.size.height;
            return resultSize;
        }
    }
    return CGSizeZero;
}


-(CGSize)contentSizeForSize:(CGSize)size
{
    return [self suggestedSizeForSize:size];
}

-(CGFloat) verifyWidth:(CGFloat)suggestedWidth
{
	int width = ceil(suggestedWidth);
	if (width & 0x01)
	{
		width ++;
	}
	return width;
}

-(CGFloat) verifyHeight:(CGFloat)suggestedHeight
{
	int height = ceil(suggestedHeight);
	if (height & 0x01)
	{
		height ++;
	}
	return height;
}

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
    [self setAttributedTextViewContent:[TiUtils stringValue:value] ofType:kContentTypeText];
	[self replaceValue:value forKey:@"text" notification:NO];
}

-(void)setHtml:(id)value
{
    [self setAttributedTextViewContent:[TiUtils stringValue:value] ofType:kContentTypeHTML];
	[self replaceValue:value forKey:@"html" notification:NO];
}

-(void)setColor:(id)color
{
	UIColor * newColor = [[TiUtils colorValue:color] _color];
    if (newColor == nil)
        newColor = [UIColor darkTextColor];
    [options setValue:newColor forKey:DTDefaultTextColor];
    [options setValue:newColor forKey:DTDefaultLinkColor];
    
    //we need to reset the text to update default paragraph settings
	[self replaceValue:color forKey:@"color" notification:YES];
    [self updateAttributeText];
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
    else
        [options setValue:@"Helvetica" forKey:DTDefaultFontFamily];
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