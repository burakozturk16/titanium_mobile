#import "TiScrollingView.h"
#import "TiApp.h"
#import "TiViewProxy.h"

@implementation TiScrollingView
{
    BOOL canFireScrollStart;
    BOOL canFireScrollEnd;
    BOOL isScrollingToTop;
    BOOL _scrollHidesKeyboard;
    
    UIView *_pullViewWrapper;
    CGFloat pullThreshhold;
    BOOL _pullViewVisible;
    BOOL _hasPullView;
    BOOL pullActive;
    
    UIView *_pullBottomViewWrapper;
    CGFloat pullBottomThreshhold;
    BOOL _pullBottomViewVisible;
    BOOL _hasPullBottomView;
    BOOL pullBottomActive;

}

- (id)init
{
    self = [super init];
    if (self) {
        _hasPullView = NO;
        _hasPullBottomView = NO;
        canFireScrollEnd = NO;
        canFireScrollStart = YES;
        _scrollHidesKeyboard = NO;
    }
    return self;
}

- (void)didMoveToSuperview
{
	[super didMoveToSuperview];
    [self updateKeyboardInset];
}

-(UIScrollView*)scrollview {
    return nil;
}

- (void)dealloc
{
    RELEASE_TO_NIL(_pullViewWrapper)
    RELEASE_TO_NIL(_pullBottomViewWrapper)
//    UIScrollView* scrollView = [self scrollview];
//    [scrollView ins_removeInfinityScroll];
//    [scrollView ins_removePullToRefresh];
    [super dealloc];
}

-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop
{
}

-(void)scrollToShowView:(UIView *)firstResponderView withKeyboardHeight:(CGFloat)keyboardTop
{
}

- (BOOL) topView:(UIView*)topView containsView:(UIView *) view  {
    for (UIView * theView in [topView subviews]){
        if (theView == view || [self topView:theView containsView:view]) {
            return YES;
        }
    }
    return NO;
}

- (void)updateKeyboardInset  {
    UIView* inputView = [[TiApp app] controller].keyboardActiveInput;
    CGFloat keyboardHeight = [[TiApp app] controller].keyboardHeight;
    
    
    [self keyboardDidShowAtHeight:keyboardHeight];
    
    if (inputView && [self topView:self containsView:inputView]) {
        double delayInSeconds = 0.3;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [self scrollToShowView:inputView withKeyboardHeight:keyboardHeight];
        });
    }
}

-(Class)scrollPanClass
{
    static Class scrollPanClass = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        scrollPanClass = [NSClassFromString(@"UIScrollViewPanGestureRecognizer") retain];;
    });
    return scrollPanClass;
}

//- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer shouldRecognizeSimultaneouslyWithGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer
//{
//
//    if (([gestureRecognizer isKindOfClass:[self scrollPanClass]] &&
//         otherGestureRecognizer == panRecognizer) || ([otherGestureRecognizer isKindOfClass:[self scrollPanClass]] &&
//                                                 gestureRecognizer == panRecognizer)) {
//        return NO;
//    }
//    return YES;
//}
//- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
//shouldBeRequiredToFailByGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
//    if ([otherGestureRecognizer isKindOfClass:[self scrollPanClass]] &&
//         gestureRecognizer == panRecognizer) {
//        return YES;
//    }
//    return NO;
//}

//- (BOOL)gestureRecognizer:(UIGestureRecognizer *)gestureRecognizer
//shouldRequireFailureOfGestureRecognizer:(UIGestureRecognizer *)otherGestureRecognizer {
//    if ([otherGestureRecognizer isKindOfClass:[self scrollPanClass]] &&
//        gestureRecognizer == panRecognizer) {
//        return YES;
//    }
//    return NO;
//}

-(void)setPullView_:(id)args
{
    UIScrollView* scrollView = [self scrollview];
    if (scrollView.bounds.size.width==0)
    {
        [self performSelector:@selector(setPullView_:) withObject:args afterDelay:0.1];
        return;
    }
    id vp = [[self viewProxy] addObjectToHold:args forKey:@"pullView"];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        TiViewProxy* viewproxy = (TiViewProxy*)vp;
        _hasPullView = YES;
        if (_pullViewWrapper == nil) {
            _pullViewWrapper = [[UIView alloc] init];
            _pullViewWrapper.backgroundColor = [UIColor clearColor];
            [scrollView addSubview:_pullViewWrapper];
        }
        CGSize refSize = scrollView.bounds.size;
        [_pullViewWrapper setFrame:CGRectMake(0.0, 0.0 - refSize.height, refSize.width, refSize.height)];
        LayoutConstraint *viewLayout = [viewproxy layoutProperties];
        //If height is not dip, explicitly set it to SIZE
        if (viewLayout->height.type != TiDimensionTypeDip) {
            viewLayout->height = TiDimensionAutoSize;
        }
        //If bottom is not dip set it to 0
        if (viewLayout->bottom.type != TiDimensionTypeDip) {
            viewLayout->bottom = TiDimensionZero;
        }
        //Remove other vertical positioning constraints
        viewLayout->top = TiDimensionUndefined;
        viewLayout->centerY = TiDimensionUndefined;
        
        [viewproxy setCanBeResizedByFrame:YES];
        [viewproxy setProxyObserver:self];
        [_pullViewWrapper addSubview:[viewproxy getAndPrepareViewForOpening:_pullViewWrapper.bounds]];
        if (_pullViewVisible) {
            [self showPullView:@(NO)];
        }
    } else {
        _hasPullView = NO;
        [_pullViewWrapper removeFromSuperview];
        RELEASE_TO_NIL(_pullViewWrapper);
    }
    
}

-(void)setPullBottomView_:(id)args
{
    UIScrollView* scrollView = [self scrollview];
    if (scrollView.bounds.size.width==0)
    {
        [self performSelector:@selector(setPullBottomView_:) withObject:args afterDelay:0.1];
        return;
    }
    id vp = [[self viewProxy] addObjectToHold:args forKey:@"pullBottomView"];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        TiViewProxy* viewproxy = (TiViewProxy*)vp;
        _hasPullBottomView = YES;
        if (_pullBottomViewWrapper == nil) {
            _pullBottomViewWrapper = [[UIView alloc] init];
            _pullBottomViewWrapper.backgroundColor = [UIColor clearColor];
            [scrollView addSubview:_pullBottomViewWrapper];
        }
        CGSize refSize = scrollView.bounds.size;
        [_pullBottomViewWrapper setFrame:CGRectMake(0.0, scrollView.contentSize.height, refSize.width, refSize.height)];
        LayoutConstraint *viewLayout = [viewproxy layoutProperties];
        //If height is not dip, explicitly set it to SIZE
        if (viewLayout->height.type != TiDimensionTypeDip) {
            viewLayout->height = TiDimensionAutoSize;
        }
        //If bottom is not dip set it to 0
        if (viewLayout->top.type != TiDimensionTypeDip) {
            viewLayout->top = TiDimensionZero;
        }
        //Remove other vertical positioning constraints
        viewLayout->bottom = TiDimensionUndefined;
        viewLayout->centerY = TiDimensionUndefined;
        
        [viewproxy setCanBeResizedByFrame:YES];
        [viewproxy setProxyObserver:self];
        [_pullBottomViewWrapper addSubview:[viewproxy getAndPrepareViewForOpening:_pullBottomViewWrapper.bounds]];
        if (_pullBottomViewVisible) {
            [self showPullBottomView:@(NO)];
        }
    } else {
        _hasPullBottomView = NO;
        [_pullBottomViewWrapper removeFromSuperview];
        RELEASE_TO_NIL(_pullBottomViewWrapper);
    }
    
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
    [super frameSizeChanged:frame bounds:bounds];
    if (_pullViewWrapper != nil && _hasPullView) {
        _pullViewWrapper.frame = CGRectMake(0.0f, 0.0f - bounds.size.height, bounds.size.width, bounds.size.height);
    }
    if (_pullBottomViewWrapper != nil && _hasPullBottomView) {
        _pullBottomViewWrapper.frame = CGRectMake(0.0f, [self scrollview].contentSize.height, bounds.size.width, bounds.size.height);
    }
//    if (_hasPullView) {
//        TiViewProxy* vp = [[self viewProxy] holdedProxyForKey:@"pullView"];
//        if (vp) {
//            [vp setSandboxBounds:bounds];
//            [vp refreshView];
//            UIScrollView* scrollView = [self scrollview];
//            scrollView.ins_pullToRefreshBackgroundView.frame = [[vp view] bounds];
//        }
//    }
    [self updateKeyboardInset];
}

-(void)proxyDidRelayout:(id)sender
{
    NSArray* keys = [[self viewProxy] allKeysForHoldedProxy:sender];
    if ([keys count] > 0) {
        NSString* key = [keys objectAtIndex:0];
        if ([key isEqualToString:@"pullView"]) {
            CGRect frame = [(TiViewProxy*)sender view].frame;
            pullThreshhold = -[self scrollview].contentInset.top + ([(TiViewProxy*)sender view].frame.origin.y - _pullViewWrapper.bounds.size.height);
        } else if ([key isEqualToString:@"pullBottomView"]) {
            CGRect frame = [(TiViewProxy*)sender view].frame;
            UIScrollView* scrollView = [self scrollview];
            pullBottomThreshhold = scrollView.contentSize.height + scrollView.contentInset.bottom + ([(TiViewProxy*)sender view].frame.size.height);
        }
    }
}

-(void)closePullView:(NSNumber*)anim
{
    if (!_hasPullView || !_pullViewVisible) return;
    _pullViewVisible = NO;
    BOOL animated = YES;
    if (anim != nil)
        animated = [anim boolValue];
    
    UIScrollView* scrollView = [self scrollview];
    CGFloat offset = -scrollView.contentInset.top;
    if (IOS_7) {
        //we have to delay it on ios7 :s
        double delayInSeconds = 0.01;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [scrollView setContentOffset:CGPointMake(scrollView.contentOffset.x,offset) animated:animated];
        });
    }
    else {
        [scrollView setContentOffset:CGPointMake(scrollView.contentOffset.x,offset) animated:animated];
    }
    
}

-(void)showPullView:(NSNumber*)anim
{
    if (!_hasPullBottomView || _pullBottomViewVisible) {
        return;
    }
    _pullBottomViewVisible = YES;
    BOOL animated = YES;
    UIScrollView* scrollView = [self scrollview];
    if (anim != nil)
        animated = [anim boolValue];
    CGFloat offset = pullThreshhold;
    [scrollView setContentOffset:CGPointMake(scrollView.contentOffset.x,offset) animated:animated];
}

-(void)closePullBottomView:(NSNumber*)anim
{
    if (!_hasPullBottomView || !_pullBottomViewVisible) return;
    _pullBottomViewVisible = NO;
    BOOL animated = YES;
    if (anim != nil)
        animated = [anim boolValue];
    
    UIScrollView* scrollView = [self scrollview];
    
    CGFloat offset = scrollView.contentSize.height - scrollView.bounds.size.height + scrollView.contentInset.bottom;
    if (IOS_7) {
        //we have to delay it on ios7 :s
        double delayInSeconds = 0.01;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [scrollView setContentOffset:CGPointMake(scrollView.contentOffset.x,offset) animated:animated];
        });
    }
    else {
        [scrollView setContentOffset:CGPointMake(scrollView.contentOffset.x,offset) animated:animated];
    }
    
}

-(void)showPullBottomView:(NSNumber*)anim
{
    if (!_hasPullView || _pullViewVisible) {
        return;
    }
    _pullViewVisible = YES;
    BOOL animated = YES;
    UIScrollView* scrollView = [self scrollview];
    if (anim != nil)
        animated = [anim boolValue];
    CGFloat offset = pullThreshhold;
    [scrollView setContentOffset:CGPointMake(scrollView.contentOffset.x,offset) animated:animated];
}


-(void)setDisableBounce_:(id)value
{
    [[self scrollview] setBounces:![TiUtils boolValue:value]];
}

-(void)setBouncesZoom_:(id)value
{
    [[self scrollview] setBouncesZoom:[TiUtils boolValue:value]];
}

-(void)setCanCancelEvents_:(id)args
{
    [[self scrollview] setCanCancelContentTouches:[TiUtils boolValue:args def:YES]];
}

-(void)setDelaysContentTouches_:(id)value
{
    [[self scrollview] setDelaysContentTouches:[TiUtils boolValue:value def:YES]];
}

-(void)setScrollingEnabled_:(id)enabled
{
    scrollingEnabled = [TiUtils boolValue:enabled];
    [[self scrollview] setScrollEnabled:scrollingEnabled];
}
- (void)setWillScrollOnStatusTap_:(id)value
{
    [[self scrollview] setScrollsToTop:[TiUtils boolValue:value def:YES]];
}


- (void)zoomToPoint:(CGPoint)touchPoint withScale: (CGFloat)scale animated: (BOOL)animated
{
    UIScrollView* scrollView = [self scrollview];
    scale = MAX(MIN(scrollView.maximumZoomScale, scale), scrollView.minimumZoomScale);
    if (scrollView.zoomScale == scale) {
        return;
    }
    CGFloat touchX = touchPoint.x;
    CGFloat touchY = touchPoint.y;
    touchX *= 1/scrollView.zoomScale;
    touchY *= 1/scrollView.zoomScale;
    touchX += scrollView.contentOffset.x;
    touchY += scrollView.contentOffset.y;
    
    CGFloat xsize = scrollView.bounds.size.width / scale;
    CGFloat ysize = self.bounds.size.height / scale;
    [scrollView zoomToRect:CGRectMake(touchX - xsize/2, touchY - ysize/2, xsize, ysize) animated:animated];
}

-(void)setContentOffsetToTop:(NSInteger)top animated:(BOOL)animated
{
    UIScrollView *currScrollView = [self scrollview];
   [currScrollView setContentOffset:CGPointMake(0,top - currScrollView.contentInset.top) animated:animated];
}


-(void)setContentOffsetToBottom:(NSInteger)bottom animated:(BOOL)animated
{
    /*
     * Calculate the bottom height & width and, sets the offset from the
     * content view’s origin that corresponds to the receiver’s origin.
     */
    UIScrollView *currScrollView = [self scrollview];
    
    CGSize svContentSize = currScrollView.contentSize;
    CGSize svBoundSize = currScrollView.bounds.size;
    CGFloat svBottomInsets = currScrollView.contentInset.bottom;
    
    CGFloat bottomHeight = svContentSize.height - svBoundSize.height + svBottomInsets;
    
    if (bottomHeight > currScrollView.frame.size.height)
    {
        CGPoint newOffset = CGPointMake(currScrollView.contentOffset.x,bottomHeight - bottom);
        [currScrollView setContentOffset:newOffset animated:animated];
    }
}




-(void)setMaxZoomScale_:(id)args
{
    UIScrollView* scrollView = [self scrollview];
    CGFloat val = [TiUtils floatValue:args def:1.0];
    [[self scrollview] setMaximumZoomScale:val];
    if ([scrollView zoomScale] > val) {
        [self setZoomScale_:args withObject:nil];
    }
    else if ([scrollView zoomScale] < [scrollView minimumZoomScale]){
        [self setZoomScale_:[NSNumber numberWithFloat:[scrollView minimumZoomScale]] withObject:nil];
    }
}

-(void)setMinZoomScale_:(id)args
{
    UIScrollView* scrollView = [self scrollview];
    CGFloat val = [TiUtils floatValue:args def:1.0];
    [scrollView setMinimumZoomScale:val];
    if ([scrollView zoomScale] < val) {
        [self setZoomScale_:args withObject:nil];
    }
}

-(void)setZoomScale_:(id)value withObject:(id)property
{
    UIScrollView* scrollView = [self scrollview];
    CGFloat scale = [TiUtils floatValue:value def:1.0];
    BOOL animated = [TiUtils boolValue:@"animated" properties:property def:NO];
    if ([property valueForKey:@"point"]) {
        [self zoomToPoint:[TiUtils pointValue:@"point" properties:property] withScale:scale animated:animated];
    } else {
        [scrollView setZoomScale:scale animated:animated];
        
    }
    
    scale = [scrollView zoomScale]; //Why are we doing this? Because of minZoomScale or maxZoomScale.
    if (!animated) {
        [self scrollViewDidEndZooming:scrollView withView:scrollView atScale:scale];
    }

}

-(void)setContentOffset_:(id)value withObject:(id)property
{
    UIScrollView* scrollView = [self scrollview];
    CGPoint newOffset = [TiUtils pointValue:value];
    BOOL animated = [TiUtils boolValue:@"animated" properties:property def:(scrollView !=nil)];
    [scrollView setContentOffset:newOffset animated:animated];
}


-(void)setScrollsToTop_:(id)value
{
    [[self scrollview] setScrollsToTop:[TiUtils boolValue:value def:YES]];
}

-(void)setHorizontalBounce_:(id)value
{
    [[self scrollview] setAlwaysBounceHorizontal:[TiUtils boolValue:value]];
}

-(void)setVerticalBounce_:(id)value
{
    [[self scrollview] setAlwaysBounceVertical:[TiUtils boolValue:value]];
}


-(void)setDecelerationRate_:(id)value
{
    [[self scrollview] setDecelerationRate:[TiUtils floatValue:value def:UIScrollViewDecelerationRateNormal]];
}
-(void)setShowHorizontalScrollIndicator_:(id)value
{
    [[self scrollview] setShowsHorizontalScrollIndicator:[TiUtils boolValue:value]];
}

-(void)setShowVerticalScrollIndicator_:(id)value
{
    [[self scrollview] setShowsVerticalScrollIndicator:[TiUtils boolValue:value]];
}

-(void)setScrollIndicatorStyle_:(id)value
{
    [[self scrollview] setIndicatorStyle:[TiUtils intValue:value def:UIScrollViewIndicatorStyleDefault]];
}
-(void)setDirectionalLockEnabled_:(id)value
{
    [[self scrollview] setDirectionalLockEnabled:[TiUtils boolValue:value]];
}


-(void)setScrollHidesKeyboard_:(id)value
{
    _scrollHidesKeyboard = [TiUtils boolValue:value def:_scrollHidesKeyboard];
}


- (NSMutableDictionary *) eventObjectForScrollView: (UIScrollView *) scrollView
{
    return [NSMutableDictionary dictionaryWithObjectsAndKeys:
            [TiUtils pointToDictionary:scrollView.contentOffset],@"contentOffset",
            @(scrollView.zoomScale),@"zoomScale",
            @([scrollView isZooming]),@"zooming",
            @([scrollView isDecelerating]),@"decelerating",
            @([scrollView isDragging]),@"dragging",
            [TiUtils sizeToDictionary:scrollView.contentSize], @"contentSize",
            [TiUtils sizeToDictionary:scrollView.bounds.size], @"size",
            nil];
}


// For now, this is fired on `scrollstart` and `scrollend`
- (void)fireScrollEvent:(NSString*)eventName forScrollView:(UIScrollView*)scrollView withAdditionalArgs:(NSDictionary*)args
{
    if([[self viewProxy] _hasListeners:eventName checkParent:NO])
    {
        NSMutableDictionary* eventArgs = [self eventObjectForScrollView:scrollView];
        
        [eventArgs setValuesForKeysWithDictionary:args];
        
        [[self proxy] fireEvent:eventName withObject:eventArgs propagate:NO checkForListener:NO];
    }
}


- (void)fireScrollEvent:(UIScrollView *)scrollView {
    [self fireScrollEvent:@"scroll" forScrollView:scrollView withAdditionalArgs:nil];
}

- (void)scrollViewDidZoom:(UIScrollView *)scrollView_
{
}



- (void)scrollViewDidEndZooming:(UIScrollView *)scrollView_ withView:(UIView *)view atScale:(CGFloat)scale
{
    if (scrollView_.zoomScale == scrollView_.minimumZoomScale) {
        scrollView_.scrollEnabled = NO;
    }else {
        scrollView_.scrollEnabled = scrollingEnabled;
    }
    [self.proxy replaceValue:NUMFLOAT(scale) forKey:@"zoomScale" notification:NO];
    
    [self fireScrollEvent:@"scale" forScrollView:scrollView_ withAdditionalArgs:nil];
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView
{
//    if (scrollView.isDragging || scrollView.isDecelerating)
//    {
        [self fireScrollEvent:scrollView];
//    }
    if ([scrollView isTracking]) {
        if ( _hasPullView) {
            BOOL pullChanged = NO;
            CGFloat offsetY = scrollView.contentOffset.y;
            if ( (offsetY < pullThreshhold) && (pullActive == NO) ) {
                pullActive = YES;
                pullChanged = YES;
            } else if ( (offsetY > pullThreshhold) && (pullActive == YES) ) {
                pullActive = NO;
                pullChanged = YES;
            }
            if (pullChanged) {
                [self fireScrollEvent:@"pullchanged" forScrollView:scrollView withAdditionalArgs:@{@"active": @(pullActive)}];
            }
            CGFloat delta = pullThreshhold - offsetY;
            if (delta >= pullThreshhold) {
                CGFloat progress = fabs(1 - fabs( (delta / pullThreshhold)));
                [self fireScrollEvent:@"pull" forScrollView:scrollView withAdditionalArgs:@{@"active": @(pullActive)}];
            }
        }
        if ( _hasPullBottomView) {
            BOOL pullChanged = NO;
            CGFloat offsetY = scrollView.contentOffset.y + scrollView.bounds.size.height;
            CGFloat contentSizeH = scrollView.contentSize.height;
            if ( (offsetY > contentSizeH) && (pullBottomActive == NO) ) {
                pullBottomActive = YES;
                pullChanged = YES;
            } else if ( (offsetY < contentSizeH) && (pullBottomActive == YES) ) {
                pullBottomActive = NO;
                pullChanged = YES;
            }
            if (pullChanged) {
                [self fireScrollEvent:@"pullchanged" forScrollView:scrollView withAdditionalArgs:@{
                                                                                                   @"active": @(pullBottomActive),
                                                                                                   @"bottom": @(YES)}];
            }
            CGFloat delta = pullBottomThreshhold - offsetY;
            if (delta <= 0) {
                CGFloat progress = fabs(1 - fabs( (delta / pullBottomThreshhold)));
                [self fireScrollEvent:@"pullbottom" forScrollView:scrollView withAdditionalArgs:@{@"active": @(pullBottomActive),
                                                                                                  @"bottom": @(YES)}];
            }
        }
        
    }
}

- (void)fireScrollEnd:(UIScrollView*)scrollView
{
//    if(canFireScrollEnd) {
//        canFireScrollEnd = NO;
//        canFireScrollStart = YES;
        [self fireScrollEvent:@"scrollend" forScrollView:scrollView withAdditionalArgs:nil];
//    }
}
- (void)fireScrollStart:(UIScrollView *)scrollView
{
//    if(canFireScrollStart) {
//        canFireScrollStart = NO;
//        canFireScrollEnd = YES;
        [self fireScrollEvent:@"scrollstart" forScrollView:scrollView withAdditionalArgs:nil];
//    }
}



- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
    if (_scrollHidesKeyboard) {
        [scrollView endEditing:YES];
    }
    [self fireScrollStart:scrollView];
    [self fireScrollEvent:@"dragstart" forScrollView:scrollView withAdditionalArgs:nil];
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
    if(!decelerate) {
        [self fireScrollEnd:(UITableView *)scrollView];
    }
    if ([(TiViewProxy*)self.proxy _hasListeners:@"dragend" checkParent:NO])
    {
        [self fireScrollEvent:@"dragend" forScrollView:scrollView withAdditionalArgs:nil];
    }
    
    
    if ( _hasPullView && (pullActive == YES) ) {
        pullActive = NO;
        [self fireScrollEvent:@"pullend" forScrollView:scrollView withAdditionalArgs:nil];
    }
    if ( _hasPullBottomView && (pullBottomActive == YES) ) {
        pullBottomActive = NO;
        [self fireScrollEvent:@"pullend" forScrollView:scrollView withAdditionalArgs:@{@"bottom": @(YES)}];
    }
}



- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
    // resume image loader when we're done scrolling
    [self fireScrollEvent:scrollView];
    if(isScrollingToTop) {
        isScrollingToTop = NO;
    } else {
        [self fireScrollEnd:scrollView];
    }
}

- (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView
{
    isScrollingToTop = YES;
    [self fireScrollStart:scrollView];
    return YES;
}

- (void)scrollViewDidScrollToTop:(UIScrollView *)scrollView
{
    [self fireScrollEnd:scrollView];
}


@end
