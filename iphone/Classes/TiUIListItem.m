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
#import "TiSVGImage.h"

@interface TiUIView()
-(void)sanitycheckListeners;
-(void)setBackgroundGradient:(TiGradient*)gradient forState:(UIControlState)state;
-(void)setBackgroundImage:(id)image forState:(UIControlState)state;
-(void)setBackgroundColor:(UIColor*)color forState:(UIControlState)state;
-(void)setBorderGradient:(TiGradient*)gradient forState:(UIControlState)state;
-(void)setBorderImage:(id)image forState:(UIControlState)state;
-(void)setBorderColor:(UIColor*)color forState:(UIControlState)state;
@end

@interface TiUIListItem()
-(UIView *)backgroundWrapperView;
-(void)disableSelectionStyle;
@end

@interface TiUIListItemContentView:TiUIView
@property (nonatomic, assign) TiUIListItem *listItem;

@end
@implementation TiUIListItemContentView

-(TiViewAnimationStep*)runningAnimation
{
    if ([super runningAnimation]) {
        return [super runningAnimation];
    }
    return [((TiUIListItemProxy*)self.proxy).listViewProxy runningAnimation];
}


-(UIView *)backgroundWrapperView
{
    return [_listItem backgroundWrapperView];
}

-(void)setBackgroundGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    [super setBackgroundGradient:gradient forState:state];
    if (state == UIControlStateHighlighted) {
        [_listItem disableSelectionStyle];
    }
}

-(void)setBackgroundImage:(id)image forState:(UIControlState)state
{
    [super setBackgroundImage:image forState:state];
    if (state == UIControlStateHighlighted) {
        [_listItem disableSelectionStyle];
    }
}

-(void)setBackgroundColor:(UIColor*)color forState:(UIControlState)state
{
    [super setBackgroundColor:color forState:state];
    if (state == UIControlStateHighlighted) {
        [_listItem disableSelectionStyle];
    }
}

-(void)setBorderGradient:(TiGradient*)gradient forState:(UIControlState)state
{
    [super setBorderGradient:gradient forState:state];
    if (state == UIControlStateHighlighted) {
        [_listItem disableSelectionStyle];
    }
}

-(void)setBorderImage:(id)image forState:(UIControlState)state
{
    [super setBorderImage:image forState:state];
    if (state == UIControlStateHighlighted) {
        [_listItem disableSelectionStyle];
    }
}

-(void)setBorderColor:(UIColor*)color forState:(UIControlState)state
{
    [super setBorderColor:color forState:state];
    if (state == UIControlStateHighlighted) {
        [_listItem disableSelectionStyle];
    }
}
@end

#define GROUP_ROUND_RADIUS 6

@implementation TiUIListItem {
	TiUIListItemProxy *_proxy;
	NSInteger _templateStyle;
	NSDictionary *_dataItem;
    int _positionMask;
    BOOL _grouped;
    TiUIListItemContentView* _viewHolder;
//    TiCellBackgroundView* _bgSelectedView;
    TiCellBackgroundView* _bgView;
    TiCap imageCap;
    BOOL _needsLayout;
    BOOL configurationSet;
    BOOL _unHighlightOnSelect;
    BOOL _customBackground;
    
    UITableViewCellSelectionStyle _selectionStyle;
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
//        self.textLabel.backgroundColor = [UIColor clearColor];
//        self.detailTextLabel.backgroundColor = [UIColor clearColor];
        _selectionStyle = UITableViewCellSelectionStyleDefault;
		_proxy = [proxy retain];
        [self initialize];
        _viewHolder.canKeepBackgroundColor = YES;
        [self.contentView sendSubviewToBack:_viewHolder];
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
        _selectionStyle = UITableViewCellSelectionStyleDefault;
        [self initialize];
        [self setGrouped:grouped];
        _positionMask = position;
        [self disableSelectionStyle];
    }
    return self;
}

-(void) initialize
{
    self.selectionStyle = _selectionStyle;
    _viewHolder = [[TiUIListItemContentView alloc] initWithFrame:self.contentView.bounds];
    _viewHolder.proxy = _proxy;
    _viewHolder.listItem = self;
    _viewHolder.shouldHandleSelection = NO;
    [_viewHolder setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
    [_viewHolder setClipsToBounds: YES];
    [_viewHolder.layer setMasksToBounds: YES];
    //    [_viewHolder selectableLayer].animateTransition = YES;
    [self.contentView addSubview:_viewHolder];
    [_viewHolder setAlwaysUseBackgroundLayer:YES];
    _unHighlightOnSelect = YES;
    _customBackground = NO;
    _proxy.listItem = self;
    _proxy.modelDelegate = self;
    configurationSet = NO;
    [_proxy dirtyItAll];
}

-(void)setGrouped:(BOOL)grouped
{
    _grouped = grouped;
}

-(void)configurationStart
{
    configurationSet = NO;
    [_viewHolder configurationStart];
//    if (_bgSelectedView) {
//        [_bgSelectedView selectableLayer].readyToCreateDrawables = configurationSet;
//    }
//    if (_bgView) {
//        [_bgView selectableLayer].readyToCreateDrawables = configurationSet;
//    }
}

-(void)configurationSet
{
	// can be used to trigger things after all properties are set
    configurationSet = YES;
    [_viewHolder sanitycheckListeners];
    [_viewHolder configurationSet];
//    if (_bgSelectedView) {
//        [_bgSelectedView selectableLayer].readyToCreateDrawables = configurationSet;
//    }
//    if (_bgView) {
//        [_bgView selectableLayer].readyToCreateDrawables = configurationSet;
//    }
    BOOL newValue = (_templateStyle == TiUIListItemTemplateStyleCustom) || [[_viewHolder backgroundLayer] willDrawForState:UIControlStateNormal];
    if (_customBackground != newValue) {
        _customBackground = newValue;
        if (_customBackground) {
            self.contentView.backgroundColor = [UIColor clearColor];
            self.backgroundColor = [UIColor clearColor];
            self.contentView.opaque = NO;
        } else {
            self.contentView.backgroundColor = [UIColor whiteColor];
            self.backgroundColor = [UIColor whiteColor];
            self.contentView.opaque = YES;
        }
    }
}

-(void)disableSelectionStyle {
    if (_selectionStyle != UITableViewCellSelectionStyleNone) {
        _selectionStyle = UITableViewCellSelectionStyleNone;
        self.selectionStyle = _selectionStyle;
        self.selectedBackgroundView = [[[UIView alloc] initWithFrame:CGRectZero] autorelease];
        self.multipleSelectionBackgroundView = [[[UIView alloc] initWithFrame:CGRectZero] autorelease];
    }
}

//-(void) updateBackgroundLayerCorners:(TiCellBackgroundView*)view {
//    if (_grouped && ![TiUtils isIOS7OrGreater]) {
//        UIRectCorner corners = -10;
//        switch (_positionMask) {
//            case TiGroupedListItemPositionBottom:
//                corners = (UIRectCornerBottomLeft | UIRectCornerBottomRight);
//                break;
//            case TiGroupedListItemPositionTop:
//                corners = (UIRectCornerTopLeft | UIRectCornerTopRight);
//                break;
//            case TiGroupedListItemPositionSingleLine:
//                corners = UIRectCornerAllCorners;
//                break;
//            default:
//                break;
//        }
//        [view setRoundedRadius:GROUP_ROUND_RADIUS inCorners:corners];
//    }
//}

//TIMOB-17373. Workaround for separators disappearing on iOS7 and above
- (void) ensureVisibleSelectorWithTableView:(UITableView*)tableView
{
    if ([self selectedOrHighlighted]) {
        return;
    }
    UITableView* attachedTableView = tableView;
    UIView* superView = [self superview];
    while (attachedTableView == nil && superView != nil) {
        if ([superView isKindOfClass:[UITableView class]]) {
            attachedTableView = (UITableView*)superView;
        }
        superView = [superView superview];
    }
    
    if (attachedTableView != nil && attachedTableView.separatorStyle != UITableViewCellSeparatorStyleNone) {
        for (UIView *subview in self.contentView.superview.subviews) {
            if ([NSStringFromClass(subview.class) hasSuffix:@"SeparatorView"]) {
                subview.hidden = NO;
            }
        }
    }
}


//-(TiCellBackgroundView*)getOrCreateSelectedBackgroundView
//{
//    if (_bgSelectedView != nil) {
//        return _bgSelectedView;
//    }
//    
//    self.selectedBackgroundView = [[[TiCellBackgroundView alloc] initWithFrame:self.bounds] autorelease];
//    _bgSelectedView = (TiCellBackgroundView*)self.selectedBackgroundView;
//    [_bgSelectedView selectableLayer].animateTransition = YES;
//    _bgSelectedView.alpha = self.contentView.alpha;
//    _bgSelectedView.backgroundColor = [UIColor redColor];
//
//    [self updateBackgroundLayerCorners:_bgSelectedView];
//    [_bgSelectedView selectableLayer].readyToCreateDrawables = configurationSet;
//    return _bgSelectedView;
//}

-(void)setBackgroundView:(UIView*)view
{
    
    [super setBackgroundView:view];
    if (view == nil) {
        RELEASE_TO_NIL(_bgView);
    }
    if (_bgView && ![view isKindOfClass:[TiCellBackgroundView class]]){
        [_bgView setFrame:view.bounds];
        [view addSubview:_bgView];
//        [self updateBackgroundLayerCorners:_bgView];
    }
}

-(void)setSelectedBackgroundView:(UIView *)selectedBackgroundView
{
//    if (_templateStyle == TiUIListItemTemplateStyleCustom) {
        [super setSelectedBackgroundView:nil];
//    } else {
//        [super setSelectedBackgroundView:selectedBackgroundView];
//    }
}

-(TiCellBackgroundView*)getOrCreateBackgroundView
{
    if (_bgView == nil) {
        _bgView = [[TiCellBackgroundView alloc] initWithFrame:self.bounds];
//        if (!_grouped || [TiUtils isIOS7OrGreater]) {
        self.backgroundView = _bgView;
        
//        }
//        else if(self.backgroundView !=nil){
//            [_bgView setFrame:self.backgroundView.bounds];
//            [self.backgroundView addSubview:_bgView];
//        }
//        [self updateBackgroundLayerCorners:_bgView];
        _bgView.alpha = self.contentView.alpha;
    }

    return _bgView;
}

-(UIView *)backgroundWrapperView
{
    return [self getOrCreateBackgroundView];
}

//-(void) setBackgroundGradient_:(id)newGradientDict
//{
//    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
//    [[self getOrCreateBackgroundView].selectableLayer setGradient:newGradient forState:UIControlStateNormal];
//}
//
//-(void) setBackgroundSelectedGradient_:(id)newGradientDict
//{
//    TiGradient * newGradient = [TiGradient gradientFromObject:newGradientDict proxy:self.proxy];
//    [[self getOrCreateSelectedBackgroundView].selectableLayer setGradient:newGradient forState:UIControlStateNormal];
//}
//
//-(void) setBackgroundColor_:(id)color
//{
//    UIColor* uicolor;
//	if ([color isKindOfClass:[UIColor class]])
//	{
//        uicolor = (UIColor*)color;
//	}
//	else
//	{
//		uicolor = [[TiUtils colorValue:color] _color];
//	}
//    [[self getOrCreateBackgroundView].selectableLayer setColor:uicolor forState:UIControlStateNormal];
//    
//}

//-(void) setBackgroundSelectedColor_:(id)color
//{
//    UIColor* uiColor = [TiUtils colorValue:color].color;
//    [[self getOrCreateSelectedBackgroundView].selectableLayer setColor:uiColor forState:UIControlStateNormal];
//}


-(void)setImageCap_:(id)arg
{
    imageCap = [TiUtils capValue:arg def:TiCapUndefined];
}

//-(UIImage*)loadImage:(id)arg
//{
//    if (arg==nil) return nil;
//    id result = nil;
//    if (TiCapIsUndefined(imageCap)) {
//        result =  [TiUtils loadBackgroundImage:arg forProxy:_proxy];
//    }
//    else {
//        result =  [TiUtils loadBackgroundImage:arg forProxy:_proxy withCap:imageCap];
//    }
//    if ([result isKindOfClass:[UIImage class]]) return result;
//    else if ([result isKindOfClass:[TiSVGImage class]]) return [((TiSVGImage*)result) fullImage];
//    return nil;
//}

//-(void) setBackgroundImage_:(id)image
//{
//    UIImage* bgImage = [self loadImage:image];
//    [[self getOrCreateBackgroundView].selectableLayer setImage:bgImage forState:UIControlStateNormal];
//}
//
//-(void) setBackgroundSelectedImage_:(id)image
//{
//    UIImage* bgImage = [self loadImage:image];
//    [[self getOrCreateSelectedBackgroundView].selectableLayer setImage:bgImage forState:UIControlStateNormal];
//}
//
//-(void)setBackgroundOpacity_:(id)opacity
//{
//    [self getOrCreateBackgroundView].selectableLayer.opacity = [TiUtils floatValue:opacity def:1.0f];
//}

-(void)setOpacity_:(id)opacity
{
 	ENSURE_UI_THREAD_1_ARG(opacity);
    
	self.contentView.alpha = [TiUtils floatValue:opacity];
    if (_bgView)
    {
        _bgView.alpha = self.contentView.alpha;
    }
//    if (_bgSelectedView)
//    {
//        _bgSelectedView.alpha = self.contentView.alpha;
//    }
}


- (void)dealloc
{
	[_proxy detachView];
	[_proxy deregisterProxy:[_proxy pageContext]];
	_proxy.listItem = nil;
	_proxy.modelDelegate = nil;
    _viewHolder.proxy = nil;
    _viewHolder.listItem = nil;
    
    RELEASE_TO_NIL(_viewHolder)
    RELEASE_TO_NIL(_dataItem)
    RELEASE_TO_NIL(_bgView)
//    RELEASE_TO_NIL(_bgSelectedView)
    RELEASE_TO_NIL(_proxy)
	[super dealloc];
}

- (void)prepareForReuse
{
    [_proxy prepareForReuse];
	[super prepareForReuse];
}

static NSArray* handledKeys;
-(NSArray *)handledKeys
{
    if (handledKeys == nil)
    {
        handledKeys = [@[@"selectionStyle", @"title", @"accessoryType", @"subtitle", @"color", @"image", @"font"
                         , @"unHighlightOnSelect"] retain];
    }
    return handledKeys;
}

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
    if (_viewHolder && [[self handledKeys] indexOfObject:key] == NSNotFound)
    {
        DoProxyDelegateChangedValuesWithProxy(_viewHolder, key, oldValue, newValue, proxy_);
    } else {
        DoProxyDelegateChangedValuesWithProxy(self, key, oldValue, newValue, proxy_);
    }
}


#pragma mark - Background Support
-(BOOL) selectedOrHighlighted
{
	return [self isSelected] || [self isHighlighted];
}

-(BOOL)isUserInteractionEnabled {
    if (_viewHolder) {
        return _viewHolder.userInteractionEnabled;
    }
    return super.userInteractionEnabled;
}

-(void)unHighlight:(NSArray*)views
{
    for (UIView *subview in views) {
		if ([(id)subview respondsToSelector:@selector(setHighlighted:)])
        {
            [(id)subview setHighlighted:NO];
        }
        else {
            NSArray *subviews = [subview subviews];
            if ([subviews count] > 0)
                [self unHighlight:subviews];
        }
        // Get the subviews of the view
    
    }
}

-(void)unHighlight
{
    if (_viewHolder)
    {
        [self unHighlight:[_viewHolder subviews]];
    }
    else {
        [self unHighlight:[self subviews]];
    }
}

-(void)setSelected:(BOOL)yn animated:(BOOL)animated
{
    [super setSelected:yn animated:animated];
    if ([self.proxy shouldHighlight]) {
        [_viewHolder setSelected:yn animated:animated];
    }
    if (_unHighlightOnSelect && yn)[self unHighlight];
}

-(void)setHighlighted:(BOOL)yn animated:(BOOL)animated
{
    [super setHighlighted:yn animated:animated];
    if (self.isSelected && !yn) {
        return;
    }
    if ([self.proxy shouldHighlight]) {
        [_viewHolder setHighlighted:yn animated:animated];
    }
    if (_unHighlightOnSelect && yn)[self unHighlight];
}

-(void)setPosition:(int)position isGrouped:(BOOL)grouped
{
    if (position == _positionMask && grouped == _grouped) return;
    _positionMask = position;
    [self setGrouped:grouped];
    
//    if (_bgView != nil) {
//        [self updateBackgroundLayerCorners:_bgView];
//    }
//    if (_bgSelectedView != nil) {
//        [self updateBackgroundLayerCorners:_bgSelectedView];
//    }
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
    if (dataItem == (_dataItem)) return;
    if (_dataItem) {
        RELEASE_TO_NIL(_dataItem)
//        [(TiViewProxy*)self.proxy dirtyItAll];
    }
	_dataItem = [dataItem mutableCopy];
    [_proxy setDataItem:dataItem];
}

-(void)setUnHighlightOnSelect_:(id)newValue
{
    _unHighlightOnSelect = [TiUtils boolValue:newValue def:YES];
}

-(void)setAccessoryType_:(id)newValue
{
    self.accessoryType = [TiUtils intValue:newValue def:UITableViewCellAccessoryNone];
}

-(void)setSelectionStyle_:(id)newValue
{
    _selectionStyle = [TiUtils intValue:newValue def:UITableViewCellSelectionStyleNone];
    if (! [self isEditing]) {
        self.selectionStyle = _selectionStyle;
    }
}

-(void)setColor_:(id)newValue
{
    UIColor *color = newValue != nil ? [[TiUtils colorValue:newValue] _color] : [UIColor blackColor];
    [self.textLabel setTextColor:color];
}

-(void)setTintColor_:(id)newValue
{
//    [_viewHolder setTintColor_:newValue];
    UIColor *color = newValue != nil ? [[TiUtils colorValue:newValue] _color] : [UIColor blackColor];
    [self setTintColor:color];
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
    self.textLabel.text = [TiUtils stringValue:newValue];
}

-(void)setSubtitle_:(id)newValue
{
    self.detailTextLabel.text = [TiUtils stringValue:newValue];
}


-(BOOL)canSwipeLeft {
    __block BOOL canSwipe = NO;
    NSArray* buttons = [self.proxy valueForKey:@"leftSwipeButtons"];
    [buttons enumerateObjectsUsingBlock:^(TiViewProxy* button, NSUInteger idx, BOOL *stop) {
        if (!button.isHidden) {
            canSwipe = YES;
            *stop = YES;
        }
    }];
    return canSwipe;
}

-(BOOL)canSwipeRight {
    __block BOOL canSwipe = NO;
    NSArray* buttons = [self.proxy valueForKey:@"rightSwipeButtons"];
    [buttons enumerateObjectsUsingBlock:^(TiViewProxy* button, NSUInteger idx, BOOL *stop) {
        if (!button.isHidden) {
            canSwipe = YES;
            *stop = YES;
        }
    }];
    return canSwipe;
}
//-(void)setFrame:(CGRect)frame
//{
//	// this happens when a controller resizes its view
//    
//
////    NSArray* animationKyes = self.layer.animationKeys;
//    if (_templateStyle == TiUIListItemTemplateStyleCustom) {
//        TiViewAnimationStep* anim = [_proxy runningAnimation];
////            if (!CGRectIsEmpty(frame))
////        	{
////                CGRect currentbounds = [_viewHolder bounds];
////                CGRect newBounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
////                if (!CGRectEqualToRect(newBounds, currentbounds))
////                {
////        //            [(TiViewProxy*)self.proxy setSandboxBounds:newBounds];
////                    [(TiViewProxy*)self.proxy dirtyItAll];
////                }
////        	}
//        if (anim)
//        {
//            [_proxy setRunningAnimationRecursive:anim];
////            [_proxy refreshViewIfNeeded:YES];
//            [super setFrame:frame];
//            [_proxy setRunningAnimationRecursive:nil];
//            return;
//        }
//    }
//    [super setFrame:frame];
//}


-(void) setEditing:(BOOL)editing animated:(BOOL)animated
{
    [super setEditing:editing animated:animated];

    // Change the selection style based on if the cell is being edited or not
    if (editing) {
        self.selectionStyle = UITableViewCellSelectionStyleDefault;
    } else {
        self.selectionStyle = _selectionStyle;
    }
}

- (void)layoutSubviews
{
    if (_templateStyle == TiUIListItemTemplateStyleCustom) {
//        TiViewAnimationStep* anim = [_proxy runningAnimation];
//        if (anim)
//        {
//            [_proxy setRunningAnimationRecursive:anim];
////            [_proxy refreshViewIfNeeded:YES];
//            [super layoutSubviews];
//            [_proxy setRunningAnimationRecursive:nil];
//            return;
//////        }
////        else {
        if (_proxy.sandboxBounds.size.width == 0 || _proxy.sandboxBounds.size.height == 0) {
            [UIView performWithoutAnimation:^{
                [_proxy refreshViewIfNeeded:YES];
            }];
        } else {
            [_proxy refreshViewIfNeeded:YES];
        }
//        }
    }
    [super layoutSubviews];
}


//override to get the correct backgroundColor
-(UIColor *) backgroundColorForSwipe
{
    if (self.swipeBackgroundColor) {
        return self.swipeBackgroundColor; //user defined color
    }
    return [[_viewHolder backgroundLayer] getColorForState:UIControlStateNormal];
}

-(void)setDelaysContentTouches:(BOOL)value
{
    _delaysContentTouches = value;
    // iterate over all the UITableView's subviews
    if (![TiUtils isIOS8OrGreater]) {
        for (id view in self.subviews)
        {
            // looking for a UITableViewCellScrollView
            if ([NSStringFromClass([view class]) isEqualToString:@"UITableViewCellScrollView"])
            {
                // this test is here for safety only, also there is no UITableViewCellScrollView in iOS8
                if([view isKindOfClass:[UIScrollView class]])
                {
                    // turn OFF delaysContentTouches in the hidden subview
                    UIScrollView *scroll = (UIScrollView *) view;
                    scroll.delaysContentTouches = _delaysContentTouches;
                }
                break;
            }
        }
    }
}


@end

#endif
