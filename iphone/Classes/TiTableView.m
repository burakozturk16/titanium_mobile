//
//  TiTableView.m
//  Titanium
//
//  Created by Martin Guillon on 20/12/13.
//
//

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
//    if (IOS_7) {
//        //we have to delay it on ios7 :s
//        double delayInSeconds = 0.01;
//        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
//        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
//            [super setContentOffset:contentOffset animated:animated];
//        });
//    }
//    else {
//        [super setContentOffset:contentOffset animated:animated];
//    }
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
