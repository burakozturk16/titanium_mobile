/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiUICollectionItem.h"
#import "TiBase.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "Webcolor.h"
#import "TiSelectableBackgroundLayer.h"
#import "TiCellBackgroundView.h"
#import "ImageLoader.h"


#define GROUP_ROUND_RADIUS 6

@implementation TiUICollectionItem {
	TiUICollectionItemProxy *_proxy;
	NSInteger _templateStyle;
	NSDictionary *_dataItem;
    TiUIView* _viewHolder;
    BOOL _needsLayout;
    BOOL configurationSet;
    BOOL _unHighlightOnSelect;
}

@synthesize templateStyle = _templateStyle;
@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;
@synthesize viewHolder = _viewHolder;

DEFINE_EXCEPTIONS

- (id)initWithProxy:(TiUICollectionItemProxy *)proxy
{
		_templateStyle = TiUICollectionItemTemplateStyleCustom;
		_proxy = [proxy retain];
    
        _viewHolder = [[TiUIView alloc] initWithFrame:self.contentView.bounds];
        _viewHolder.proxy = _proxy;
        _viewHolder.shouldHandleSelection = NO;
    [_viewHolder setBackgroundColor_:[UIColor redColor]];
        [_viewHolder setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
        [_viewHolder setClipsToBounds: YES];
        [_viewHolder.layer setMasksToBounds: YES];
        [self.contentView addSubview:_viewHolder];
        [self initialize];
}

-(void) initialize
{
    _unHighlightOnSelect = YES;
    
    _proxy.listItem = self;
    _proxy.modelDelegate = self;
    configurationSet = NO;
    [_proxy dirtyItAll];
}

-(void)configurationStart
{
    [_viewHolder configurationStart];
}

-(void)configurationSet
{
    configurationSet = YES;
    [_viewHolder configurationSet];
}

- (void)dealloc
{
	[_proxy detachView];
	[_proxy cleanup];
	[_proxy deregisterProxy:[_proxy pageContext]];
	_proxy.listItem = nil;
	_proxy.modelDelegate = nil;
    _viewHolder.proxy = nil;
    
    RELEASE_TO_NIL(_viewHolder)
    RELEASE_TO_NIL(_dataItem)
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
    if ([[self handledKeys] indexOfObject:key] == NSNotFound)
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

-(void)setSelected:(BOOL)yn
{
    [super setSelected:yn];
    if ([self.proxy shouldHighlight]) {
        [_viewHolder setSelected:yn animated:NO];
    }
    if (_unHighlightOnSelect && yn)[self unHighlight];
}

-(void)setHighlighted:(BOOL)yn
{
    [super setHighlighted:yn];
    if (self.isSelected && !yn) {
        return;
    }
    if ([self.proxy shouldHighlight]) {
        [_viewHolder setHighlighted:yn animated:NO];
    }
    if (_unHighlightOnSelect && yn)[self unHighlight];
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
	_dataItem = [dataItem retain];
    [_proxy setDataItem:_dataItem];
}

-(void)setUnHighlightOnSelect_:(id)newValue
{
    _unHighlightOnSelect = [TiUtils boolValue:newValue def:YES];
}

-(void)touchSetHighlighted:(BOOL)highlighted
{
    if (!_unHighlightOnSelect) return;
    [self setHighlighted:highlighted];
}

//-(void)setFrame:(CGRect)frame
//{
//	// this happens when a controller resizes its view
//    
//    if (!CGRectIsEmpty(frame))
//	{
//        CGRect currentbounds = [_viewHolder bounds];
//        CGRect newBounds = CGRectMake(0, 0, frame.size.width, frame.size.height);
//        if (!CGRectEqualToRect(newBounds, currentbounds))
//        {
//            [(TiViewProxy*)self.proxy dirtyItAll];
//        }
//	}
//    [super setFrame:frame];
//	
//}

//- (void)layoutSubviews
//{
//    [super layoutSubviews];
//    if (_templateStyle == TiUICollectionItemTemplateStyleCustom) {
//        TiViewAnimationStep* anim = [_proxy runningAnimation];
//        if (anim)
//        {
//            [_proxy setRunningAnimationRecursive:anim];
//            [_proxy refreshViewIfNeeded:YES];
//            [_proxy setRunningAnimationRecursive:nil];
//        }
//        else {
//            [_proxy refreshViewIfNeeded:YES];
//        }
//    }
//}

- (void)layoutSubviews
{
    if (_templateStyle == TiUICollectionItemTemplateStyleCustom) {
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

@end

#endif
