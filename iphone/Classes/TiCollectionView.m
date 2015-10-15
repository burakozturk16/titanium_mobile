/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW
#import "TiCollectionView.h"
#import "TiBase.h"
#import "TiUIListView.h"
#import "TiUIHelper.h"


@implementation TiCollectionView
{
    BOOL _shouldHighlightCurrentItem;
    CGPoint touchPoint;
}

- (id)init
{
    self = [super init];
    if (self) {
        _shouldHighlightCurrentItem = YES;
    }
    return self;
}

- (id)initWithFrame:(CGRect)frame {
    self = [super initWithFrame:frame];
    if (self) {
        _shouldHighlightCurrentItem = YES;
    }
    return self;
}


-(BOOL)shouldHighlightCurrentCollectionItem {
    return _shouldHighlightCurrentItem;
}

-(TiViewProxy*)findFirstViewProxyAsParent:(UIView*)view {
    if (view == nil) return nil;
    if ([view isKindOfClass:[TiUIView class]]) {
        return (TiViewProxy*)[(TiUIView*)view proxy];
    }
    return [self findFirstViewProxyAsParent:view.superview];
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
    UITouch *touch = [touches anyObject];
    UIView* view = touch.view;
    CGPoint touchPointInView = [[touches anyObject] locationInView:view];
    touchPoint = [view convertPoint:touchPointInView toView:self];
    TiViewProxy *viewProxy = [self findFirstViewProxyAsParent:view];
    if ([viewProxy isKindOfClass:[TiViewProxy class]] && [viewProxy preventListViewSelection]) {
        //    TiViewProxy *viewProxy = [TiUIHelper findViewProxyWithBindIdUnder:view containingPoint:touchPointInView];
        //    if (viewProxy && [viewProxy preventListViewSelection]) {
        _shouldHighlightCurrentItem = NO;
    }
    [super touchesBegan:touches withEvent:event];
    _shouldHighlightCurrentItem = YES;
}

-(CGPoint) touchPoint
{
    return touchPoint;
}

@end
#endif
