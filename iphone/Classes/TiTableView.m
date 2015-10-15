/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UILISTVIEW
#import "TiTableView.h"
#import "TiBase.h"
#import "TiUIListView.h"
#import "TiUIHelper.h"


@implementation TiTableView
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

- (id)initWithFrame:(CGRect)frame style:(UITableViewStyle)style {
    self = [super initWithFrame:frame style:style];
    if (self) {
        _shouldHighlightCurrentItem = YES;
    }
    return self;
}



//- (void)setContentOffset:(CGPoint)contentOffset animated:(BOOL)animated
//{
////    if (IOS_7) {
////        //we have to delay it on ios7 :s
////        double delayInSeconds = 0.01;
////        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
////        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
////            [super setContentOffset:contentOffset animated:animated];
////        });
////    }
////    else {
//        [super setContentOffset:contentOffset animated:animated];
////    }
//}


-(BOOL)shouldHighlightCurrentListItem {
    return _shouldHighlightCurrentItem;
}

-(TiViewProxy*)findFirstViewProxyAsParent:(UIView*)view {
    if (view == nil) return nil;
    if ([view isKindOfClass:[TiUIView class]]) {
        return (TiViewProxy*)[(TiUIView*)view proxy];
    }
    return [self findFirstViewProxyAsParent:view.superview];
}

-(void)setDelaysContentTouches:(BOOL)delaysContentTouches
{
    [super setDelaysContentTouches:delaysContentTouches];
    // iterate over all the UITableView's subviews
    if ([TiUtils isIOS8OrGreater]) {
        for (id view in self.subviews)
        {
            // looking for a UITableViewWrapperView
            if ([NSStringFromClass([view class]) isEqualToString:@"UITableViewWrapperView"])
            {
                // this test is necessary for safety and because a "UITableViewWrapperView" is NOT a UIScrollView in iOS7
                if([view isKindOfClass:[UIScrollView class]])
                {
                    // turn OFF delaysContentTouches in the hidden subview
                    UIScrollView *scroll = (UIScrollView *) view;
                    scroll.delaysContentTouches = delaysContentTouches;
                }
                break;
            }
        }
    }
}

- (BOOL)touchesShouldCancelInContentView:(UIView *)view {
    // Because we set delaysContentTouches = NO, we return YES for UIButtons
    // so that scrolling works correctly when the scroll gesture
    // starts in the UIButtons.
    BOOL exclusive = [view isExclusiveTouch];
    if (exclusive) {
        return NO;
    }
    return [super touchesShouldCancelInContentView:view];
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
//    _shouldHighlightCurrentItem = YES;
}

- (void)touchesEnded:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesEnded:touches withEvent:event];
    _shouldHighlightCurrentItem = YES;
}

- (void)touchesCancelled:(NSSet *)touches withEvent:(UIEvent *)event
{
    [super touchesCancelled:touches withEvent:event];
    _shouldHighlightCurrentItem = YES;
}
-(CGPoint) touchPoint
{
    return touchPoint;
}

-(void)processBlock:(void(^)(UITableView * tableView))block animated:(BOOL)animated {
    [UIView setAnimationsEnabled:animated];
//    [CATransaction begin];
    [self beginUpdates];
    block(self);
    [self endUpdates];
//    [CATransaction commit];
    [UIView setAnimationsEnabled:YES];
}

@end

#endif
