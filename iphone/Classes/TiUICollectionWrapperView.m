/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiUICollectionWrapperView.h"
#import "TiUICollectionWrapperViewProxy.h"
#import "TiBase.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "Webcolor.h"
#import "ImageLoader.h"


#define GROUP_ROUND_RADIUS 6

@implementation TiUICollectionWrapperView {
    TiUICollectionWrapperViewProxy *_proxy;
    NSDictionary *_dataItem;
    TiUIView* _viewHolder;
    TiCap imageCap;
    BOOL _needsLayout;
    BOOL configurationSet;
    BOOL _unHighlightOnSelect;
}

@synthesize proxy = _proxy;
@synthesize dataItem = _dataItem;
@synthesize viewHolder = _viewHolder;

DEFINE_EXCEPTIONS

- (id)initWithProxy:(TiUICollectionWrapperViewProxy *)proxy
{
    //we are retained by the collectionView
    _proxy = [proxy retain];
    self.viewHolder = [[TiUIView alloc] initWithFrame:self.bounds];
    _proxy.sandboxBounds = self.bounds;
    _viewHolder.shouldHandleSelection = NO;
    [_viewHolder setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
    [_viewHolder setClipsToBounds: YES];
    [_viewHolder.layer setMasksToBounds: YES];
    [self initialize];
}

-(void)setViewHolder:(TiUIView *)viewHolder
{
    if (_viewHolder) {
        _viewHolder.proxy = nil;
        [_viewHolder removeFromSuperview];
        RELEASE_TO_NIL(_viewHolder);
    }
    _viewHolder = [viewHolder retain];
    _viewHolder.proxy = _proxy;
    _viewHolder.frame = self.bounds;
    [self addSubview:_viewHolder];
}

-(void)updateProxy:(TiUICollectionWrapperViewProxy *)viewProxy forIndexPath:(NSIndexPath*)indexPath
{
    self.proxy = viewProxy;
    self.viewHolder = viewProxy.view;
    viewProxy.wrapperView = self ;
    viewProxy.indexPath = indexPath;
}

-(void) initialize
{
    self.backgroundColor = [UIColor clearColor];
    self.backgroundColor = [UIColor clearColor];
    self.opaque = NO;
    
    _proxy.wrapperView = self;
    configurationSet = NO;
    [_proxy dirtyItAll];
}

-(void)configurationStart
{
    configurationSet = NO;
    [_viewHolder configurationStart];
}

-(void)configurationSet
{
    // can be used to trigger things after all properties are set
    configurationSet = YES;
    [_viewHolder configurationSet];

}

- (void)dealloc
{
    [_proxy detachView];
    [_proxy deregisterProxy:[_proxy pageContext]];
    _proxy.wrapperView = nil;
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

-(void)propertyChanged:(NSString*)key oldValue:(id)oldValue newValue:(id)newValue proxy:(TiProxy*)proxy_
{
    DoProxyDelegateChangedValuesWithProxy(_viewHolder, key, oldValue, newValue, proxy_);
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
        [(TiViewProxy*)self.proxy dirtyItAll];
    }
    _dataItem = [dataItem retain];
    [_proxy setDataItem:_dataItem];
}

-(void)checkBoundsForChange:(CGRect)bounds
{
    if (!CGRectIsEmpty(bounds))
    {
        CGRect currentbounds = [_viewHolder bounds];
        CGRect newBounds = CGRectMake(0, 0, bounds.size.width, bounds.size.height);
        if (!CGRectEqualToRect(newBounds, currentbounds))
        {
            [(TiViewProxy*)self.proxy setSandboxBounds:newBounds];
//            [(TiViewProxy*)self.proxy dirtyItAll];
        }
    }
}

-(void)setFrame:(CGRect)frame
{
    [self checkBoundsForChange:frame];
    [super setFrame:frame];
//
}
//
-(void)setBounds:(CGRect)bounds
{
    [self checkBoundsForChange:bounds];
    [super setBounds:bounds];
//
}

//- (void)layoutSubviews
//{
//    [super layoutSubviews];
//    TiViewAnimationStep* anim = [_proxy runningAnimation];
//    if (anim)
//    {
//        [_proxy setRunningAnimationRecursive:anim];
//        [_proxy refreshViewIfNeeded:YES];
//        [_proxy setRunningAnimationRecursive:nil];
//    }
//    else {
//        [_proxy refreshViewIfNeeded:YES];
//    }
//}

- (void)layoutSubviews
{
//    if (_templateStyle == TiUICollectionItemTemplateStyleCustom) {
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
//    }
    [super layoutSubviews];
}

@end

#endif
