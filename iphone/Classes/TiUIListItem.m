/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListItem.h"
#import "TiBase.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "Webcolor.h"
#import "TiSelectableBackgroundLayer.h"
#import "TiCellBackgroundView.h"
#import "ImageLoader.h"


#define GROUP_ROUND_RADIUS 6

@implementation TiUIListItem {
	TiUIListItemProxy *_proxy;
	NSInteger _templateStyle;
	NSDictionary *_dataItem;
    int _positionMask;
    BOOL _grouped;
    TiUIView* _viewHolder;
    TiCellBackgroundView* _bgSelectedView;
    TiCellBackgroundView* _bgView;
    TiDimension leftCap;
    TiDimension topCap;
    TiDimension bottomCap;
    TiDimension rightCap;
    BOOL _needsLayout;
}

@synthesize templateStyle = _templateStyle;
@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;
@synthesize viewHolder = _viewHolder;

DEFINE_EXCEPTIONS

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
    }
    return self;
}

-(void) initialize
{
    self.contentView.backgroundColor = [UIColor clearColor];
    if ([TiUtils isIOS7OrGreater]) {
        self.backgroundColor = [UIColor clearColor];
    }
    self.contentView.opaque = NO;
    _viewHolder = [[TiUIView alloc] initWithFrame:self.contentView.bounds];
    _viewHolder.proxy = _proxy;
    _viewHolder.shouldHandleSelection = NO;
    [_viewHolder setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
    [_viewHolder setClipsToBounds: YES];
    [_viewHolder.layer setMasksToBounds: YES];
    [self.contentView addSubview:_viewHolder];
    _proxy.listItem = self;
    _proxy.modelDelegate = [self autorelease]; //without the autorelease we got a memory leak
    [_proxy dirtyItAll];
}

-(void)setGrouped:(BOOL)grouped
{
    _grouped = grouped && ![TiUtils isIOS7OrGreater];
}

-(void) updateBackgroundLayerCorners:(TiCellBackgroundView*)view {
    if (_grouped) {
        UIRectCorner corners = -10;
        switch (_positionMask) {
            case TiGroupedListItemPositionBottom:
                corners = (UIRectCornerBottomLeft | UIRectCornerBottomRight);
                break;
            case TiGroupedListItemPositionTop:
                corners = (UIRectCornerTopLeft | UIRectCornerTopRight);
                break;
            case TiGroupedListItemPositionSingleLine:
                corners = UIRectCornerAllCorners;
                break;
            default:
                break;
        }
        [view setRoundedRadius:GROUP_ROUND_RADIUS inCorners:corners];
    }
}


-(TiCellBackgroundView*)getOrCreateSelectedBackgroundView
{
    if (_bgSelectedView != nil) {
        return _bgSelectedView;
    }
    
    self.selectedBackgroundView = [[[TiCellBackgroundView alloc] initWithFrame:CGRectZero] autorelease];
    _bgSelectedView = (TiCellBackgroundView*)self.selectedBackgroundView;
    [_bgSelectedView selectableLayer].animateTransition = YES;
    _bgSelectedView.alpha = self.contentView.alpha;

    [self updateBackgroundLayerCorners:_bgSelectedView];
    return _bgSelectedView;
}

-(void)setBackgroundView:(UIView*)view
{
    
    [super setBackgroundView:view];
    if (view == nil) {
        RELEASE_TO_NIL(_bgView);
    }
    if (_bgView && ![view isKindOfClass:[TiCellBackgroundView class]]){
        [_bgView setFrame:view.bounds];
        [view addSubview:_bgView];
        [self updateBackgroundLayerCorners:_bgView];
    }
}

-(TiCellBackgroundView*)getOrCreateBackgroundView
{
    if (_bgView == nil) {
        _bgView = [[TiCellBackgroundView alloc] initWithFrame:CGRectZero];
        if (!_grouped || [TiUtils isIOS7OrGreater]) {
            self.backgroundView = _bgView;
        }
        else if(self.backgroundView !=nil){
            [_bgView setFrame:self.backgroundView.bounds];
            [self.backgroundView addSubview:_bgView];
        }
        [self updateBackgroundLayerCorners:_bgView];
        _bgView.alpha = self.contentView.alpha;
    }

    return _bgView;
}

-(void) setBackgroundGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateBackgroundView].selectableLayer setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedGradient_:(id)newGradientDict
{
    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
    [[self getOrCreateSelectedBackgroundView].selectableLayer setGradient:newGradient forState:UIControlStateNormal];
}

-(void) setBackgroundColor_:(id)color
{
    UIColor* uicolor;
	if ([color isKindOfClass:[UIColor class]])
	{
        uicolor = (UIColor*)color;
	}
	else
	{
		uicolor = [[TiUtils colorValue:color] _color];
	}
    [[self getOrCreateBackgroundView].selectableLayer setColor:uicolor forState:UIControlStateNormal];
    
}

-(void) setBackgroundSelectedColor_:(id)color
{
    UIColor* uiColor = [TiUtils colorValue:color].color;
    [[self getOrCreateSelectedBackgroundView].selectableLayer setColor:uiColor forState:UIControlStateNormal];
}


-(void)setImageCap_:(id)arg
{
    ENSURE_SINGLE_ARG(arg,NSDictionary);
    NSDictionary* dict = (NSDictionary*)arg;
    if ([dict objectForKey:@"left"]) {
        leftCap = TiDimensionFromObject([dict objectForKey:@"left"]);
    }
    if ([dict objectForKey:@"right"]) {
        rightCap = TiDimensionFromObject([dict objectForKey:@"right"]);
    }
    if ([dict objectForKey:@"top"]) {
        topCap = TiDimensionFromObject([dict objectForKey:@"top"]);
    }
    if ([dict objectForKey:@"bottom"]) {
        bottomCap = TiDimensionFromObject([dict objectForKey:@"bottom"]);
    }
}

-(UIImage*)loadImage:(id)arg
{
    if (arg==nil) return nil;
    UIImage *image = nil;
	if (TiDimensionIsUndefined(leftCap) && TiDimensionIsUndefined(topCap) &&
        TiDimensionIsUndefined(rightCap) && TiDimensionIsUndefined(bottomCap)) {
        image =  [TiUtils loadBackgroundImage:arg forProxy:_proxy];
    }
    else {
        image =  [TiUtils loadBackgroundImage:arg forProxy:_proxy withLeftCap:leftCap topCap:topCap rightCap:rightCap bottomCap:bottomCap];
    }
	return image;
}

-(void) setBackgroundImage_:(id)image
{
    UIImage* bgImage = [self loadImage:image];
    [[self getOrCreateBackgroundView].selectableLayer setImage:bgImage forState:UIControlStateNormal];
}

-(void) setBackgroundSelectedImage_:(id)image
{
    UIImage* bgImage = [self loadImage:image];
    [[self getOrCreateSelectedBackgroundView].selectableLayer setImage:bgImage forState:UIControlStateNormal];
}

-(void)setBackgroundOpacity_:(id)opacity
{
    [self getOrCreateBackgroundView].selectableLayer.opacity = [TiUtils floatValue:opacity def:1.0f];
}

-(void)setOpacity_:(id)opacity
{
 	ENSURE_UI_THREAD_1_ARG(opacity);
    
	self.contentView.alpha = [TiUtils floatValue:opacity];
    if (_bgView)
    {
        _bgView.alpha = self.contentView.alpha;
    }
    if (_bgSelectedView)
    {
        _bgSelectedView.alpha = self.contentView.alpha;
    }
}


- (void)dealloc
{
	_proxy.listItem = nil;
	_proxy.modelDelegate = nil;
    _viewHolder.proxy = nil;
    
    RELEASE_TO_NIL(_viewHolder)
    RELEASE_TO_NIL(_dataItem)
    RELEASE_TO_NIL(_bgView)
    RELEASE_TO_NIL(_proxy)
	[super dealloc];
}

- (void)prepareForReuse
{
	RELEASE_TO_NIL(_dataItem);
    [_proxy prepareForReuse];
	[super prepareForReuse];
}

static NSArray* handledKeys;
-(NSArray *)handledKeys
{
    if (handledKeys == nil)
    {
        handledKeys = [@[@"selectionStyle", @"title", @"accessoryType", @"subtitle", @"color", @"image", @"font", @"opacity", @"backgroundGradient", @"backgroundImage", @"backgroundOpacity", @"backgroundColor"
                         , @"backgroundSelectedGradient", @"backgroundSelectedImage", @"backgroundSelectedColor"] retain];
    }
    return handledKeys;
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
	if ([[self handledKeys] indexOfObject:key] != NSNotFound) {
        DoProxyDelegateChangedValuesWithProxy(self, key, oldValue, newValue, proxy_);
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
		if ([(id)subview respondsToSelector:@selector(setHighlighted:)])
        {
            [(id)subview setHighlighted:NO];
        }
        // Get the subviews of the view
        NSArray *subviews = [subview subviews];
        if ([subviews count] > 0)
            [self unHighlight:subviews];
    }
}

-(void)setSelected:(BOOL)yn animated:(BOOL)animated
{
    [super setSelected:yn animated:animated];
    [self unHighlight:[self subviews]];
}

-(void)setHighlighted:(BOOL)yn animated:(BOOL)animated
{
    [super setHighlighted:yn animated:animated];
    [self unHighlight:[self subviews]];
}

-(void)setPosition:(int)position isGrouped:(BOOL)grouped
{
    if (position == _positionMask && grouped == _grouped) return;
    _positionMask = position;
    [self setGrouped:grouped];
    
    if (_bgView != nil) {
        [self updateBackgroundLayerCorners:_bgView];
    }
    if (_bgSelectedView != nil) {
        [self updateBackgroundLayerCorners:_bgSelectedView];
    }
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
	_dataItem = [dataItem retain];
    [_proxy setDataItem:_dataItem];
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

-(void)setFrame:(CGRect)frame
{
	// this happens when a controller resizes its view
    
    if (!CGRectIsEmpty(frame))
	{
        CGRect currentbounds = [self bounds];
        CGRect newBounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
        if (!CGRectEqualToRect(newBounds, currentbounds))
        {
            [(TiViewProxy*)self.proxy setSandboxBounds:newBounds];
        }
	}
    [super setFrame:frame];
	
}

- (void)layoutSubviews
{
    [super layoutSubviews];
    if (_templateStyle == TiUIListItemTemplateStyleCustom) {
        [_proxy refreshViewIfNeeded:YES];
    }
}
@end

#endif
