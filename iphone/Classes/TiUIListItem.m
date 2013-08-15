/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListItem.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "Webcolor.h"
#import "TiSelectableBackgroundLayer.h"

#define GROUP_ROUND_RADIUS 8

@implementation TiUIListItem {
	TiUIListItemProxy *_proxy;
	NSInteger _templateStyle;
	NSDictionary *_dataItem;
    int _positionMask;
    BOOL _grouped;
    TiSelectableBackgroundLayer* _bgLayer;
}

@synthesize templateStyle = _templateStyle;
@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;

- (id)initWithStyle:(UITableViewCellStyle)style position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier proxy:(TiUIListItemProxy *)proxy
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = style;
		_proxy = [proxy retain];
        self.contentView.backgroundColor = [UIColor grayColor];
		_proxy.listItem = self;
        [self setGrouped:grouped];
        _positionMask = position;
    }
    return self;
}

- (id)initWithProxy:(TiUIListItemProxy *)proxy position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = TiUIListItemTemplateStyleCustom;
		_proxy = [proxy retain];
		_proxy.listItem = self;
        [self setGrouped:grouped];
        _positionMask = position;
        [proxy applyCellProps];
    }
    return self;
}

-(void)setGrouped:(BOOL)grouped
{
    _grouped = grouped && ![TiUtils isIOS7OrGreater];
}

- (void)dealloc
{
	_proxy.listItem = nil;
    [_dataItem release];
	[_proxy release];
    [_bgLayer removeFromSuperlayer];
    [_bgLayer release];
	[super dealloc];
}

- (void)prepareForReuse
{
	RELEASE_TO_NIL(_dataItem);
	[super prepareForReuse];
}

- (void)layoutSubviews
{
	[super layoutSubviews];
    if (_bgLayer != nil) {
        if (!CGRectEqualToRect(_bgLayer.bounds, self.contentView.frame)) {
            _bgLayer.frame = self.contentView.bounds;
        }
        [self.contentView.layer insertSublayer:_bgLayer atIndex:0];
    }
    
	if (_templateStyle == TiUIListItemTemplateStyleCustom) {
		// prevent any crashes that could be caused by unsupported layouts
		_proxy.layoutProperties->layoutStyle = TiLayoutRuleAbsolute;
		[_proxy layoutChildren:NO];
	}
}

-(void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ( ([[event touchesForView:self.contentView] count] > 0) || ([[event touchesForView:self.accessoryView] count] > 0)
        || ([[event touchesForView:self.imageView] count] > 0) || ([[event touchesForView:self.proxy.view] count]> 0 )) {
        if ([_proxy _hasListeners:@"touchstart"])
        {
        	UITouch *touch = [touches anyObject];
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            [_proxy fireEvent:@"touchstart" withObject:evt propagate:YES];
        }
    }
    
    [super touchesBegan:touches withEvent:event];
}

- (void)touchesMoved:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ([[event touchesForView:self.contentView] count] > 0 || ([[event touchesForView:self.accessoryView] count] > 0)
        || ([[event touchesForView:self.imageView] count] > 0) || ([[event touchesForView:self.proxy.view] count]> 0 )) {
        if ([_proxy _hasListeners:@"touchmove"])
        {
            UITouch *touch = [touches anyObject];
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            [_proxy fireEvent:@"touchmove" withObject:evt propagate:YES];
        }
    }
    [super touchesMoved:touches withEvent:event];
}

-(void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    if ( ([[event touchesForView:self.contentView] count] > 0) || ([[event touchesForView:self.accessoryView] count] > 0)
        || ([[event touchesForView:self.imageView] count] > 0) || ([[event touchesForView:self.proxy.view] count]> 0 )) {
        if ([_proxy _hasListeners:@"touchend"])
        {
            UITouch *touch = [touches anyObject];
            NSDictionary *evt = [TiUtils dictionaryFromTouch:touch inView:self];
            [_proxy fireEvent:@"touchend" withObject:evt propagate:YES];
        }
    }
    [super touchesEnded:touches withEvent:event];
}

-(void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
}

#pragma mark - Background Support
-(BOOL) selectedOrHighlighted
{
	return [self isSelected] || [self isHighlighted];
}

-(void)unHighlight:(NSArray*)views
{
    for (UIView *subview in views) {
        if ([subview isKindOfClass:[UIButton class] ])
        {
            [(UIButton*)subview setHighlighted:NO];
        }
        // Get the subviews of the view
        NSArray *subviews = [subview subviews];
        if ([subviews count] > 0)
            [self unHighlight:subviews];
    }
}

-(void)setHighlighted:(BOOL)yn
{
    [super setHighlighted:yn];
    [self unHighlight:[self subviews]];
}

-(void)setSelected:(BOOL)yn
{
    [super setSelected:yn];
    [self unHighlight:[self subviews]];
}


-(void)setSelected:(BOOL)yn animated:(BOOL)animated
{
    [super setSelected:yn animated:animated];
    [self unHighlight:[self subviews]];
    if (_bgLayer != nil) {
        BOOL realSelected = yn|[self isHighlighted];
        UIControlState state = realSelected?UIControlStateSelected:UIControlStateNormal;
        [_bgLayer setState:state];
    }
}

-(void)setHighlighted:(BOOL)yn animated:(BOOL)animated
{
    [super setHighlighted:yn animated:animated];
    [self unHighlight:[self subviews]];
    if (_bgLayer != nil) {
        BOOL realSelected = yn|[self isHighlighted];
        UIControlState state = realSelected?UIControlStateSelected:UIControlStateNormal;
        [_bgLayer setState:state];
    }
}

-(void)setBackgroundOpacity_:(id)opacity
{
    CGFloat backgroundOpacity = [TiUtils floatValue:opacity def:1.0f];
    if (_bgLayer!=nil) {
        _bgLayer.opacity = backgroundOpacity;
    }
}

-(void)setBorderRadius_:(id)radius
{
	self.layer.cornerRadius = [TiUtils floatValue:radius];
    if (_bgLayer!=nil) {
        _bgLayer.cornerRadius= self.layer.cornerRadius;
    }
}

-(void) setBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setSelectedBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateCustomBackgroundLayer] setGradient:newGradient forState:UIControlStateSelected];
    self.selectionStyle = UITableViewCellSelectionStyleNone;
}

-(void) setBackgroundColor_:(id)color
{
    self.backgroundColor = [TiUtils colorValue:color].color;
}

-(void) setSelectedBackgroundColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    if (uiColor == nil) {
        switch (self.selectionStyle) {
            case UITableViewCellSelectionStyleBlue:uiColor = [Webcolor webColorNamed:@"#0272ed"];break;
            case UITableViewCellSelectionStyleGray:uiColor = [Webcolor webColorNamed:@"#bbb"];break;
            case UITableViewCellSelectionStyleNone:uiColor = [UIColor clearColor];break;
            default:uiColor = [TiUtils isIOS7OrGreater]?[Webcolor webColorNamed:@"#e0e0e0"]:[Webcolor webColorNamed:@"#0272ed"];break;
        }
    }
    [[self getOrCreateCustomBackgroundLayer] setColor:uiColor forState:UIControlStateSelected];
    self.selectionStyle = UITableViewCellSelectionStyleNone;
}


-(void) setBackgroundImage_:(id)image
{
    UIImage* bgImage = [TiUtils loadBackgroundImage:image forProxy:_proxy];
    [[self getOrCreateCustomBackgroundLayer] setImage:bgImage forState:UIControlStateNormal];
}

-(void) setSelectedBackgroundImage_:(id)image
{
    UIImage* bgImage = [TiUtils loadBackgroundImage:image forProxy:_proxy];
    [[self getOrCreateCustomBackgroundLayer] setImage:bgImage forState:UIControlStateSelected];
    self.selectionStyle = UITableViewCellSelectionStyleNone;
}

-(void)setPosition:(int)position isGrouped:(BOOL)grouped
{
    _positionMask = position;
    [self setGrouped:grouped];
    [self updateBackgroundLayerCorners];
    [self setNeedsDisplay];
}

-(void) updateBackgroundLayerCorners {
    if (_bgLayer == nil) {
        return;
    }
    if (_grouped) {
        UIRectCorner corners = -1;
        switch (_positionMask) {
            case TiGroupedListItemPositionBottom:
                corners = UIRectCornerBottomRight | UIRectCornerBottomLeft;
                break;
            case TiGroupedListItemPositionTop:
                corners = UIRectCornerTopRight | UIRectCornerTopLeft;
                break;
            case TiGroupedListItemPositionSingleLine:
                corners = UIRectCornerAllCorners;
                break;
            default:
                break;
        }
        if (corners != -1) {
            [_bgLayer setRoundedRadius:GROUP_ROUND_RADIUS inCorners:corners];
        }
    }
}

-(TiSelectableBackgroundLayer*)getOrCreateCustomBackgroundLayer
{
    if (_bgLayer != nil) {
        return _bgLayer;
    }
    
    _bgLayer = [[TiSelectableBackgroundLayer alloc] init];
    _bgLayer.frame = self.contentView.bounds;
    _bgLayer.animateTransition = YES;
    [self.contentView.layer insertSublayer:_bgLayer atIndex:0];
    
    [self updateBackgroundLayerCorners];

    CGFloat backgroundOpacity = [TiUtils floatValue:[_proxy valueForKey:@"backgroundOpacity"] def:1.0f];
    _bgLayer.opacity = backgroundOpacity;
    return _bgLayer;
}

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
{
	id template = [_dataItem objectForKey:@"template"];
	id otherTemplate = [otherItem objectForKey:@"template"];
	BOOL same = (template == otherTemplate) || [template isEqual:otherTemplate];
//	if (same) {
//		id propertiesValue = [_dataItem objectForKey:@"properties"];
//		NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
//		id heightValue = [properties objectForKey:@"height"];
//		
//		propertiesValue = [otherItem objectForKey:@"properties"];
//		properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
//		id otherHeightValue = [properties objectForKey:@"height"];
//		same = (heightValue == otherHeightValue) || [heightValue isEqual:otherHeightValue];
//	}
	return same;
}

- (void)setDataItem:(NSDictionary *)dataItem
{
	_dataItem = [dataItem retain];
    [_proxy setDataItem:_dataItem];
}


@end

#endif
