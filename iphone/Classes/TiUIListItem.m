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
#import "ImageLoader.h"

@interface TiUIListItem ()
{
    CGFloat cornersRadius;
    UIRectCorner roundedCorners;
}

@end

#define GROUP_ROUND_RADIUS 6

@implementation TiUIListItem {
	TiUIListItemProxy *_proxy;
	NSInteger _templateStyle;
	NSDictionary *_dataItem;
    int _positionMask;
    BOOL _grouped;
    TiUIView* _viewHolder;
}

@synthesize templateStyle = _templateStyle;
@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;
@synthesize viewHolder = _viewHolder;

- (id)initWithStyle:(UITableViewCellStyle)style position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier proxy:(TiUIListItemProxy *)proxy
{
    self = [super initWithStyle:style reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = style;
        self.textLabel.backgroundColor = [UIColor clearColor];
        self.detailTextLabel.backgroundColor = [UIColor clearColor];
		_proxy = [proxy retain];
        [self initialize];
        [self setGrouped:grouped];
        _positionMask = position;
        [self updateBackgroundLayerCorners];
    }
    return self;
}

- (id)initWithProxy:(TiUIListItemProxy *)proxy position:(int)position grouped:(BOOL)grouped reuseIdentifier:(NSString *)reuseIdentifier
{
    self = [super initWithStyle:UITableViewCellStyleDefault reuseIdentifier:reuseIdentifier];
    if (self) {
		_templateStyle = TiUIListItemTemplateStyleCustom;
		_proxy = [proxy retain];
        [self initialize];
        [self setGrouped:grouped];
        _positionMask = position;
        [self updateBackgroundLayerCorners];
    }
    return self;
}

-(void) initialize
{
    self.contentView.backgroundColor = [UIColor clearColor];
    self.contentView.opaque = NO;
    _viewHolder = [[TiUIView alloc] initWithFrame:self.contentView.bounds];
    _viewHolder.proxy = _proxy;
    _viewHolder.shouldHandleSelection = NO;
    CGRect bounds = _viewHolder.bounds;
    [_viewHolder setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
    [_viewHolder setClipsToBounds: YES];
    [_viewHolder.layer setMasksToBounds: YES];
    [self.contentView addSubview:_viewHolder];
    _proxy.listItem = self;
    _proxy.modelDelegate = self;
}

-(void)setGrouped:(BOOL)grouped
{
    _grouped = grouped && ![TiUtils isIOS7OrGreater];
}

-(void)setFrame:(CGRect)frame
{
    if(!CGSizeEqualToSize([self bounds].size, frame.size)) {
        [self updateMaskOnLayer:_viewHolder.layer];
    }
   [super setFrame:frame];
}

- (void)updateMaskOnLayer:(CALayer*)layer
{
    if (layer.mask == nil) return;
    UIBezierPath *maskPath = [UIBezierPath bezierPathWithRoundedRect:layer.bounds
                                                   byRoundingCorners:roundedCorners
                                                         cornerRadii:CGSizeMake(cornersRadius, cornersRadius)];
    ((CAShapeLayer*)layer.mask).path = maskPath.CGPath;
}
- (void)setRoundedRadius:(CGFloat)radius inCorners:(UIRectCorner)corners onLayer:(CALayer*)layer
{
    CAShapeLayer* maskLayer = [[CAShapeLayer alloc] init];
    maskLayer.frame = self.bounds;
    layer.mask = maskLayer;
    [maskLayer release];
    cornersRadius = radius;
    roundedCorners = corners;
    [self updateMaskOnLayer:layer];
}

- (void)dealloc
{
	_proxy.listItem = nil;
    [_viewHolder release];
    [_dataItem release];
	[_proxy release];
	[super dealloc];
}

- (void)prepareForReuse
{
	RELEASE_TO_NIL(_dataItem);
	[super prepareForReuse];
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
	if (_templateStyle != TiUIListItemTemplateStyleCustom && ([key isEqualToString:@"accessoryType"] ||
                                                              [key isEqualToString:@"selectionStyle"] ||
                                                              [key isEqualToString:@"title"] ||
                                                              [key isEqualToString:@"subtitle"] ||
                                                              [key isEqualToString:@"color"] ||
                                                              [key isEqualToString:@"image"] ||
                                                              [key isEqualToString:@"font"])) {
        DoProxyDelegateChangedValuesWithProxy(self, key, oldValue, newValue, proxy_);
    }
    else if ([key isEqualToString:@"selectedBackgroundColor"]) {
        DoProxyDelegateChangedValuesWithProxy(_viewHolder, @"backgroundSelectedColor", oldValue, newValue, proxy_);
        self.selectionStyle = UITableViewCellSelectionStyleNone;
    } else if ([key isEqualToString:@"selectedBackgroundGradient"]) {
        DoProxyDelegateChangedValuesWithProxy(_viewHolder, @"backgroundSelectedGradient", oldValue, newValue, proxy_);
        self.selectionStyle = UITableViewCellSelectionStyleNone;
    } else if ([key isEqualToString:@"selectedBackgroundImage"]) {
        DoProxyDelegateChangedValuesWithProxy(_viewHolder, @"backgroundSelectedImage", oldValue, newValue, proxy_);
        self.selectionStyle = UITableViewCellSelectionStyleNone;
	} else {
        DoProxyDelegateChangedValuesWithProxy(_viewHolder, key, oldValue, newValue, proxy_);
    }
}

#pragma mark - Background Support
-(BOOL) selectedOrHighlighted
{
	return [self isSelected] || [self isHighlighted];
}

-(void)unHighlight:(NSArray*)views
{
    for (UIView *subview in views) {
        if ([subview isKindOfClass:[UIControl class] ])
        {
            [(UIControl*)subview setHighlighted:NO];
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
    
    if (_viewHolder.backgroundLayer != nil) {
        BOOL realSelected = yn|[self isHighlighted];
        if (self.selectionStyle != UITableViewCellSelectionStyleNone) {
            [_viewHolder.backgroundLayer setHidden:yn animated:animated];
       }
        else {
            UIControlState state = realSelected?UIControlStateSelected:UIControlStateNormal;
            [_viewHolder.backgroundLayer setState:state animated:animated];
        }
    }
}

-(void)setHighlighted:(BOOL)yn animated:(BOOL)animated
{
    [super setHighlighted:yn animated:animated];
    [self unHighlight:[self subviews]];
    if (_viewHolder.backgroundLayer != nil) {
        BOOL realSelected = yn|[self isSelected];
        if (self.selectionStyle != UITableViewCellSelectionStyleNone) {
            [_viewHolder.backgroundLayer setHidden:yn animated:animated];
        }
        else {
            UIControlState state = realSelected?UIControlStateSelected:UIControlStateNormal;
            [_viewHolder.backgroundLayer setState:state animated:animated];
        }
    }
}

-(void)setPosition:(int)position isGrouped:(BOOL)grouped
{
    if (position == _positionMask && grouped == _grouped) return;
    _positionMask = position;
    [self setGrouped:grouped];
    [self updateBackgroundLayerCorners];
}

-(void) updateBackgroundLayerCorners {
    
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
            [self setRoundedRadius:GROUP_ROUND_RADIUS inCorners:corners onLayer:_viewHolder.layer];
        }
        else {
            _viewHolder.layer.mask = nil;
        }
    }
    [self setNeedsDisplay];
}

- (BOOL)canApplyDataItem:(NSDictionary *)otherItem;
{
	id template = [_dataItem objectForKey:@"template"];
	id otherTemplate = [otherItem objectForKey:@"template"];
	BOOL same = (template == otherTemplate) || [template isEqual:otherTemplate];
	return same;
}

- (void)setDataItem:(NSDictionary *)dataItem
{
//    [_viewHolder configurationStart];
	_dataItem = [dataItem retain];
    [_proxy setDataItem:_dataItem];
    [_viewHolder configurationSet];
}

-(void)setAccessoryType_:(id)newValue
{
    self.accessoryType = [TiUtils intValue:newValue def:UITableViewCellAccessoryNone];
}

-(void)setSelectionStyle_:(id)newValue
{
    self.selectionStyle = [TiUtils intValue:newValue def:UITableViewCellSelectionStyleBlue];
}

-(void)setColor_:(id)newValue
{
    UIColor *color = newValue != nil ? [[TiUtils colorValue:newValue] _color] : [UIColor blackColor];
    [self.textLabel setTextColor:color];
}

-(void)setFont_:(id)fontValue
{
    UIFont *font = (fontValue != nil) ? [[TiUtils fontValue:fontValue] font] : nil;
    [self.textLabel setFont:font];
}

-(void)setImage_:(id)imageValue
{
    NSURL *imageUrl = [TiUtils toURL:imageValue proxy:_proxy];
    UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:imageUrl];
    self.imageView.image = image;
}


-(void)setTitle_:(id)newValue
{
    self.textLabel.text = [newValue description];
}

-(void)setSubtitle_:(id)newValue
{
    self.detailTextLabel.text = [newValue description];
}

@end

#endif
