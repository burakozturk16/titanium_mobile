	/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UISCROLLABLEVIEW

#import "TiUIScrollableView.h"
#import "TiUIScrollableViewProxy.h"
#import "TiUtils.h"
#import "TiViewProxy.h"
#import "TiTransition.h"

@interface TiUIScrollableView()
{
    TiDimension pageDimension;
    TiDimension pageOffset;
    TiTransition* _transition;
    BOOL _reverseDrawOrder;
    NSMutableArray* _wrappers;
    BOOL _updatePageDuringScroll;
}
@property(nonatomic,readonly)	TiUIScrollableViewProxy * proxy;
@end

@implementation TiUIScrollableView
@synthesize switchPageAnimationDuration;
#pragma mark Internal 

-(void)dealloc
{
	RELEASE_TO_NIL(scrollview);
	RELEASE_TO_NIL(pageControl);
    RELEASE_TO_NIL(pageControlBackgroundColor);
    RELEASE_TO_NIL(_transition);
    RELEASE_TO_NIL(_wrappers);
	[super dealloc];
}

-(id)init
{
	if (self = [super init]) {
        _updatePageDuringScroll = NO;
        _reverseDrawOrder = NO;
        pageDimension = TiDimensionFromObject(@"100%");
        pageOffset = TiDimensionFromObject(@"50%");
        verticalLayout = NO;
        switchPageAnimationDuration = 250;
        cacheSize = 3;
        pageControlHeight=20;
        pageControlBackgroundColor = [[UIColor blackColor] retain];
        pagingControlOnTop = NO;
        overlayEnabled = NO;
        pagingControlAlpha = 1.0;
        showPageControl = YES;
        _wrappers = [[NSMutableArray alloc] init];
	}
	return self;
}

-(void)initializeState
{
    [super initializeState];
    verticalLayout = self.proxy.verticalLayout;
}

-(CGRect)pageControlRect
{
	
    if (!pagingControlOnTop) {
        CGRect boundsRect = [self bounds];
        if (verticalLayout) {
            return CGRectMake(boundsRect.origin.x + boundsRect.size.width - pageControlHeight,
                              boundsRect.origin.y,
                              pageControlHeight,
                              boundsRect.size.height);
        }
        else {
            return CGRectMake(boundsRect.origin.x,
                          boundsRect.origin.y + boundsRect.size.height - pageControlHeight,
                          boundsRect.size.width, 
                          pageControlHeight);
        }
    }
    else {
        CGRect boundsRect = [self bounds];
        if (verticalLayout) {
            return CGRectMake(0,0,
                              pageControlHeight,
                              boundsRect.size.height);
        }
        else {
            return CGRectMake(0,0,
                              boundsRect.size.width,
                              pageControlHeight);
        }
    }
    
}

-(UIPageControl*)pagecontrol 
{
	if (pageControl==nil)
	{
		pageControl = [[UIPageControl alloc] initWithFrame:[self pageControlRect]];
		[pageControl setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleTopMargin];
		[pageControl addTarget:self action:@selector(pageControlTouched:) forControlEvents:UIControlEventValueChanged];
		[pageControl setBackgroundColor:pageControlBackgroundColor];
		[self addSubview:pageControl];
	}
	return pageControl;
}


-(UIView*)hitTest:(CGPoint)point withEvent:(UIEvent*)event
{
    UIView* child = nil;
    if ((child = [super hitTest:point withEvent:event]) == self)
    	return [self scrollview];
    return child;
}

-(NSArray*)wrappers
{
    return [NSArray arrayWithArray:_wrappers];
}

-(UIScrollView*)scrollview 
{
	if (scrollview==nil)
	{
		scrollview = [[UIScrollView alloc] initWithFrame:[self bounds]];
		[scrollview setAutoresizingMask:UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight];
		[scrollview setPagingEnabled:YES];
		[scrollview setDelegate:self];
		[scrollview setBackgroundColor:[UIColor clearColor]];
		[scrollview setShowsVerticalScrollIndicator:NO];
		[scrollview setShowsHorizontalScrollIndicator:NO];
		[scrollview setDelaysContentTouches:NO];
		[scrollview setCanCancelContentTouches:YES];
		[scrollview setScrollsToTop:NO];
		[scrollview setClipsToBounds:NO];
        [self setClipsToBounds:YES];
		[self insertSubview:scrollview atIndex:0];
	}
	return scrollview;
}

-(void)refreshPageControl
{
	if (showPageControl)
	{
		UIPageControl *pg = [self pagecontrol];
		[pg setFrame:[self pageControlRect]];
        [pg setNumberOfPages:[[self proxy] viewCount]];
        [pg setBackgroundColor:pageControlBackgroundColor];
		pg.currentPage = currentPage;
        pg.alpha = pagingControlAlpha;
        pg.backgroundColor = pageControlBackgroundColor;
	}	
}

-(void)layoutSubviews
{
////	[super layoutSubviews];
////	[self checkBounds];
}


-(void)renderView:(TiViewProxy*)viewProxy forIndex:(int)index withRefresh:(BOOL)refresh
{
	int svSubviewsCount = [_wrappers count];
    
	if ((index < 0) || (index >= svSubviewsCount))
	{
		return;
	}

	UIView *wrapper = [_wrappers objectAtIndex:index];
	TiViewProxy *viewproxy = [[self proxy] viewAtIndex:index];
    if (![viewproxy viewAttached]) {
        if ([[viewproxy view] superview] != wrapper) {
            [wrapper addSubview:[viewproxy getAndPrepareViewForOpening:[wrapper bounds]]];
        }
    } else if (!CGRectEqualToRect([viewproxy sandboxBounds], [wrapper bounds])) {
        [self.proxy layoutChild:viewproxy optimize:NO withMeasuredBounds:[wrapper bounds]];
    }
}

-(NSRange)cachedFrames:(int)page
{
    int startPage;
    int endPage;
	int viewsCount = [[self proxy] viewCount];
    
    // Step 1: Check to see if we're actually smaller than the cache range:
    if (cacheSize >= viewsCount) {
        startPage = 0;
        endPage = viewsCount - 1;
    }
    else {
		startPage = (page - (cacheSize - 1) / 2);
		endPage = (page + (cacheSize - 1) / 2);
		
        // Step 2: Check to see if we're rendering outside the bounds of the array, and if so, adjust accordingly.
        if (startPage < 0) {
            endPage -= startPage;
            startPage = 0;
        }
        if (endPage >= viewsCount) {
            int diffPage = endPage - viewsCount;
            endPage = viewsCount -  1;
            startPage += diffPage;
        }
		if (startPage > endPage) {
			startPage = endPage;
		}
    }
    
	return NSMakeRange(startPage, endPage - startPage + 1);
}

-(void)manageCache:(int)page withRefresh:(BOOL)refresh
{
    if ([(TiUIScrollableViewProxy *)[self proxy] viewCount] == 0) {
        return;
    }
    
    if (!configurationSet) {
        needsToRefreshScrollView = YES;
        return;
    }
    
    NSRange renderRange = [self cachedFrames:page];
	int viewsCount = [[self proxy] viewCount];
    
    for (int i=0; i < viewsCount; i++) {
        TiViewProxy* viewProxy = [[self proxy] viewAtIndex:i];
        if (i >= renderRange.location && i < NSMaxRange(renderRange)) {
            [self renderView:viewProxy forIndex:i withRefresh:refresh];
        }
        else {
            if ([viewProxy viewAttached]) {
                [viewProxy windowWillClose];
                [viewProxy windowDidClose];
            }
        }
    }
}

-(void)manageCache:(int)page
{
    [self manageCache:page withRefresh:NO];
}

-(void)listenerAdded:(NSString*)event count:(int)count
{
    [super listenerAdded:event count:count];
    NSArray * childrenArray = [[[self proxy] views] retain];
    for (id child in childrenArray) {
        if ([child respondsToSelector:@selector(parentListenersChanged)]) {
            [child performSelector:@selector(parentListenersChanged)];
        }
    }
    [childrenArray release];
}

-(void)listenerRemoved:(NSString*)event count:(int)count
{
    [super listenerRemoved:event count:count];
    NSArray * childrenArray = [[[self proxy] views] retain];
    for (id child in childrenArray) {
        if ([child respondsToSelector:@selector(parentListenersChanged)]) {
            [child performSelector:@selector(parentListenersChanged)];
        }
    }
    [childrenArray release];
}

-(int)currentPage
{
	int result = currentPage;
    if (scrollview != nil) {
        CGSize scrollFrame = [self bounds].size;
        if (scrollFrame.width != 0 && scrollFrame.height != 0) {
            float nextPageAsFloat = [self getPageFromOffset:scrollview.contentOffset];
            result = MIN(floor(nextPageAsFloat - 0.5) + 1, [[self proxy] viewCount] - 1);
        }
    }
//    [pageControl setCurrentPage:result];
    return result;
}

- (void)depthSortViews
{
	UIScrollView *sv = [self scrollview];
    for (UIView *view in _wrappers)
    {
        if (_reverseDrawOrder)
            [sv sendSubviewToBack:view];
        else
            [sv bringSubviewToFront:view];
    }
}

-(void)refreshScrollView:(BOOL)readd
{
    [self refreshScrollView:[self scrollview].bounds readd:readd];
}

-(void)resetWrapperView:(UIView*)wrapper
{
    // we need to reset it after transitions
    wrapper.layer.transform = CATransform3DIdentity;
    wrapper.layer.hidden = NO;
    wrapper.alpha = 1;
}

-(void)refreshScrollView:(CGRect)visibleBounds readd:(BOOL)readd
{
    if (CGSizeEqualToSize(visibleBounds.size, CGSizeZero)) return;
	CGRect viewBounds;
	viewBounds.size.width = visibleBounds.size.width;
	viewBounds.size.height = visibleBounds.size.height;
    viewBounds.origin = CGPointMake(0, 0);
    
    if(!overlayEnabled || !showPageControl ) {
        if (verticalLayout) {
            if(pagingControlOnTop) viewBounds.origin = CGPointMake(pageControlHeight, 0);
            viewBounds.size.width -= (showPageControl ? pageControlHeight : 0);
        }
        else {
            if(pagingControlOnTop) viewBounds.origin = CGPointMake(0, pageControlHeight);
            viewBounds.size.height -= (showPageControl ? pageControlHeight : 0);
        }
    }
	UIScrollView *sv = [self scrollview];
	
    int page = [self currentPage];
    
	[self refreshPageControl];
	
	if (readd)
	{
		for (UIView *view in _wrappers)
		{
			[view removeFromSuperview];
		}
        [_wrappers removeAllObjects];
        
		for (TiViewProxy* theView in [[self proxy] views]) {
			[theView windowWillClose];
			[theView windowDidClose];
		}
	}
	
	int viewsCount = [[self proxy] viewCount];
	/*
	Reset readd here since refreshScrollView is called from
	frameSizeChanged with readd false and the views might 
	not yet have been added on first launch
	*/
	readd = ([_wrappers count] == 0);
	
	for (int i=0;i<viewsCount;i++)
	{
        if (verticalLayout) {
            viewBounds.origin.y = i*viewBounds.size.height;
        }
        else {
            viewBounds.origin.x = i*viewBounds.size.width;
        }
		
		if (readd)
		{
			UIView *view = [[UIView alloc] initWithFrame:viewBounds];
			[sv addSubview:view];
            [_wrappers addObject:view];
			[view release];
		}
		else 
		{
			UIView *view = [_wrappers objectAtIndex:i];
            [self resetWrapperView:view];
			view.frame = viewBounds;
		}
	}
    if (readd && _transition) {
        [self depthSortViews];
    }
    
	[self manageCache:page];
	
	CGRect contentBounds;
//	contentBounds.origin.x = viewBounds.origin.x;
//	contentBounds.origin.y = viewBounds.origin.y;
	contentBounds.size.width = viewBounds.size.width;
	contentBounds.size.height = viewBounds.size.height;
    
    if (verticalLayout) {
        contentBounds.size.height *= viewsCount;

    }
    else {
        contentBounds.size.width *= viewsCount;
    }
	
	
	[sv setContentSize:contentBounds.size];
    [self didScroll];
}

-(void) updateScrollViewFrame:(CGRect)visibleBounds
{
    if (verticalLayout) {
        CGFloat pageWidth = TiDimensionCalculateValue(pageDimension, visibleBounds.size.height);
        CGRect bounds = visibleBounds;
        bounds.size.height = pageWidth;
        CGFloat offset = TiDimensionCalculateValue(pageOffset, visibleBounds.size.height - bounds.size.height);
        bounds.origin.y = offset;
        [scrollview setFrame:bounds];
    } else {
        CGFloat pageWidth = TiDimensionCalculateValue(pageDimension, visibleBounds.size.width);
        CGRect bounds = visibleBounds;
        bounds.size.width = pageWidth;
        CGFloat offset = TiDimensionCalculateValue(pageOffset, visibleBounds.size.width - bounds.size.width);
        bounds.origin.x = offset;
        [scrollview setFrame:bounds];
    }
}


// We have to cache the current page because we need to scroll to the new (logical) position of the view
// within the scrollable view.  Doing so, if we're resizing to a SMALLER frame, causes a content offset
// reset internally, which screws with the currentPage number (since -[self scrollViewDidScroll:] is called).
// Looks a little ugly, though...
//-(void)setFrame:(CGRect)frame_
//{
//    lastPage = [self currentPage];
//    enforceCacheRecalculation = YES;
//    [super setFrame:frame_];
////    [self updateScrollViewFrame:frame_];
//    [self setCurrentPage_:[NSNumber numberWithInt:lastPage]];
//    enforceCacheRecalculation = NO;
//}


-(void)setBounds:(CGRect)bounds_
{
    lastPage = currentPage;
    enforceCacheRecalculation = YES;
    [super setBounds:bounds_];
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)visibleBounds
{
    if (CGSizeEqualToSize(visibleBounds.size, CGSizeZero)) return;
	
    [self updateScrollViewFrame:visibleBounds];
    [self setCurrentPage_:[NSNumber numberWithInt:lastPage]];
    enforceCacheRecalculation = NO;
    [self refreshScrollView:NO];
    [self setContentOffsetForPage:currentPage animated:NO];
    [self manageCache:[self currentPage]];
	
    //To make sure all subviews are properly resized.
//    UIScrollView *sv = [self scrollview];
//    for(UIView *view in _wrappers){
//        for (TiUIView *sView in [view subviews]) {
//                [sView checkBounds];
//        }
//    }
    
    [super frameSizeChanged:frame bounds:visibleBounds];
}

-(void)configurationStart
{
    [super configurationStart];
    needsToRefreshScrollView = NO;
}

-(void)configurationSet
{
    [super configurationSet];
    
    if (needsToRefreshScrollView)
    {
        [self manageCache:[self currentPage]];
    }
}

#pragma mark Public APIs

-(void)setCacheSize_:(id)args
{
    ENSURE_SINGLE_ARG(args, NSNumber);
    int newCacheSize = [args intValue];
    if (newCacheSize < 3) {
        // WHAT.  Let's make it something sensible.
        newCacheSize = 3;
    }
    if (newCacheSize % 2 == 0) {
        DebugLog(@"[WARN] Even scrollable cache size %d; setting to %d", newCacheSize, newCacheSize-1);
        newCacheSize -= 1;
    }
    cacheSize = newCacheSize;
    [self manageCache:[self currentPage]];
}

-(void)setPageWidth_:(id)args
{
    pageDimension = TiDimensionFromObject(args);
    if ((scrollview!=nil) && ([[scrollview subviews] count]>0)) {
        //No need to readd. Just set up the correct frame bounds
        [self refreshScrollView:NO];
    }
}

-(void)setViews_:(id)args
{
	if ((scrollview!=nil) && ([scrollview subviews]>0))
	{
		[self refreshScrollView:YES];
	}
}

-(void)setShowPagingControl_:(id)args
{
	showPageControl = [TiUtils boolValue:args];
    
	if (pageControl!=nil)
	{
		if (showPageControl==NO)
		{
			[pageControl removeFromSuperview];
			RELEASE_TO_NIL(pageControl);
		}
	}
	
    if ((scrollview!=nil) && ([[scrollview subviews] count]>0)) {
        //No need to readd. Just set up the correct frame bounds
        [self refreshScrollView:NO];
    }
	
}

-(void)setPagingControlHeight_:(id)args
{
	pageControlHeight = [TiUtils floatValue:args def:20.0];
	if (pageControlHeight < 5.0)
	{
		pageControlHeight = 20.0;
	}
    
    if (showPageControl && (scrollview!=nil) && ([[scrollview subviews] count]>0)) {
        //No need to readd. Just set up the correct frame bounds
        [self refreshScrollView:NO];
    }
}

-(void)setPageControlHeight_:(id)arg
{
	// for 0.8 backwards compat, renamed all for consistency
     DEPRECATED_REPLACED(@"ScrollableView.PageControlHeight()", @"2.1.0", @"Ti.ScrollableView.PagingControlHeight()");
	[self setPagingControlHeight_:arg];
}

-(void)setPagingControlColor_:(id)args
{
    TiColor* val = [TiUtils colorValue:args];
    if (val != nil) {
        RELEASE_TO_NIL(pageControlBackgroundColor);
        pageControlBackgroundColor = [[val _color] retain];
        if (showPageControl && (scrollview!=nil) && ([_wrappers count]>0)) {
            [[self pagecontrol] setBackgroundColor:pageControlBackgroundColor];
        }
    }
}
-(void)setPagingControlAlpha_:(id)args
{
    pagingControlAlpha = [TiUtils floatValue:args def:1.0];
    if(pagingControlAlpha > 1.0){
        pagingControlAlpha = 1;
    }    
    if(pagingControlAlpha < 0.0 ){
        pagingControlAlpha = 0;
    }
    if (showPageControl && (scrollview!=nil) && ([_wrappers count] > 0)) {
        [[self pagecontrol] setAlpha:pagingControlAlpha];
    }
    
}
-(void)setPagingControlOnTop_:(id)args
{
    pagingControlOnTop = [TiUtils boolValue:args def:NO];
    if (showPageControl && (scrollview!=nil) && ([_wrappers count] > 0)) {
        //No need to readd. Just set up the correct frame bounds
        [self refreshScrollView:NO];
    }
}

-(void)setOverlayEnabled_:(id)args
{
    overlayEnabled = [TiUtils boolValue:args def:NO];
    if (showPageControl && (scrollview!=nil) && ([_wrappers count] > 0)) {
        //No need to readd. Just set up the correct frame bounds
        [self refreshScrollView:NO];
    }
}

-(void)setContentOffsetForPage:(int)pageNumber animated:(BOOL)animated
{
    CGPoint offset;
    if (verticalLayout) {
        float pageHeight = [scrollview bounds].size.height;
        offset = CGPointMake(0, pageHeight * pageNumber);
    }
    else {
        float pageWidth = [scrollview bounds].size.width;
        offset = CGPointMake(pageWidth * pageNumber, 0);
    }
    [scrollview setContentOffset:offset animated:animated];
    [self didScroll];
}

-(int)pageNumFromArg:(id)args
{
	int pageNum = 0;
	if ([args isKindOfClass:[TiViewProxy class]])
	{
		[[self proxy] lockViews];
		pageNum = [[[self proxy] viewProxies] indexOfObject:args];
		[[self proxy] unlockViews];
	}
	else
	{
		pageNum = [TiUtils intValue:args];
	}
	
	return pageNum;
}

-(void)scrollToView:(id)args
{
    id data = nil;
    NSNumber* anim = nil;
    BOOL animated = YES;
    ENSURE_ARG_AT_INDEX(data, args, 0, NSObject);
    ENSURE_ARG_OR_NIL_AT_INDEX(anim, args, 1, NSNumber);
	int pageNum = [self pageNumFromArg:data];
	if (anim != nil)
		animated = [anim boolValue];
    
    [self manageCache:pageNum];

    if (animated)
    {
        [UIView animateWithDuration:switchPageAnimationDuration/1000
                              delay:0.00
                            options:UIViewAnimationCurveLinear
                         animations:^{[self setContentOffsetForPage:pageNum animated:NO];}
                         completion:^(BOOL finished){
                             [self scrollViewDidEndDecelerating:[self scrollview]];
                            } ];
    }
    else{
        [self setContentOffsetForPage:pageNum animated:NO];
    }
    [self updateCurrentPage:pageNum andPageControl:YES];
}

-(void)moveNext:(id)args
{
	int page = [self currentPage];
	int pageCount = [[self proxy] viewCount];

	if (page < pageCount-1)
	{
		NSArray* scrollArgs = [NSArray arrayWithObjects:[NSNumber numberWithInt:(page+1)], args, nil];
		[self scrollToView:scrollArgs];
	}
}

-(void)movePrevious:(id)args
{
	int page = [self currentPage];

	if (page > 0)
	{
		NSArray* scrollArgs = [NSArray arrayWithObjects:[NSNumber numberWithInt:(page-1)], args, nil];
		[self scrollToView:scrollArgs];
	}
}

-(void)updateCurrentPage:(int)newPage
{
    [self updateCurrentPage:newPage andPageControl:YES];
}

-(void)updateCurrentPage:(int)newPage andPageControl:(BOOL)updatePageControl
{
    if (newPage == currentPage) return;
    currentPage = newPage;
    if (updatePageControl) {
        [pageControl setCurrentPage:newPage];
    }
    [self.proxy replaceValue:NUMINT(newPage) forKey:@"currentPage" notification:NO];
    if ([self.proxy _hasListeners:@"change" checkParent:NO])
	{
		[self.proxy fireEvent:@"change" withObject:[NSDictionary dictionaryWithObjectsAndKeys:
                                                   NUMINT(newPage),@"currentPage",
                                                   [[self proxy] viewAtIndex:newPage],@"view",nil] propagate:NO];
	}
}

-(void)addView:(id)viewproxy
{
	[self refreshScrollView:YES];
}

-(void)removeView:(id)args
{
	int page = [self currentPage];
	int pageCount = [[self proxy] viewCount];
	if (page==pageCount)
	{
        [self updateCurrentPage:pageCount-1];
	}
	[self refreshScrollView:YES];
}

-(void)setCurrentPage_:(id)page
{
	
	int newPage = [TiUtils intValue:page];
	int viewsCount = [[self proxy] viewCount];

	if (newPage >=0 && newPage < viewsCount)
	{
        [self setContentOffsetForPage:newPage animated:NO];
		lastPage = newPage;
		[self updateCurrentPage:newPage];
		
        [self manageCache:newPage];
        [self didScroll];
        
	}
}


-(void)setVerticalLayout:(BOOL)value
{
    verticalLayout = value;
    [self refreshScrollView:NO];
}


-(void)setScrollingEnabled_:(id)enabled
{
    scrollingEnabled = [TiUtils boolValue:enabled];
    [[self scrollview] setScrollEnabled:scrollingEnabled];
}

-(void)setDisableBounce_:(id)value
{
	[[self scrollview] setBounces:![TiUtils boolValue:value]];
}

-(void)setTransition_:(id)value
{
    
    UIScrollView* sv = [self scrollview];
    ENSURE_SINGLE_ARG_OR_NIL(value, NSDictionary)
    RELEASE_TO_NIL(_transition);
    _transition = [[TiTransitionHelper transitionFromArg:value containerView:sv] retain];
    if (_transition) {
        _reverseDrawOrder = [_transition needsReverseDrawOrder];
        [_transition prepareViewHolder:sv];
    }
	[self refreshScrollView:YES];
	[self depthSortViews];
}

#pragma mark Rotation

-(void)manageRotation
{
    if ([scrollview isDecelerating] || [scrollview isDragging]) {
        rotatedWhileScrolling = YES;
    }
}

#pragma mark Delegate calls

-(void)pageControlTouched:(id)sender
{
	int pageNum = [(UIPageControl *)sender currentPage];
    [self setContentOffsetForPage:pageNum animated:YES];
	handlingPageControlEvent = YES;
	
    lastPage = pageNum;
    [self updateCurrentPage:pageNum];
	[self manageCache:pageNum];
    
    [self fireEventWithData:@"click" propagate:YES];
}


-(CGFloat)getPageFromOffset:(CGPoint)offset
{
    float nextPageAsFloat;
    if (verticalLayout) {
        CGFloat pageHeight = scrollview.frame.size.height;
        nextPageAsFloat = ((offset.y - pageHeight / 2) / pageHeight) + 0.5;
    }
    else {
        CGFloat pageWidth = scrollview.frame.size.width;
        nextPageAsFloat = ((offset.x - pageWidth / 2) / pageWidth) + 0.5;
    }
    return nextPageAsFloat;
}

- (void)transformItemView:(UIView *)view atIndex:(NSInteger)index withCurrentPage:(CGFloat)currentPageAsFloat
{
    //calculate offset
    CGFloat offset = index - currentPageAsFloat;
    [_transition transformView:view withPosition:offset adjustTranslation:YES];
}

- (void)transformViews
{
    
    int index = 0;
    float currentPageAsFloat = [self getPageFromOffset:scrollview.contentOffset];
    for (TiViewProxy* viewProxy in [[self proxy] viewProxies]) {
        if ([viewProxy viewAttached]) {
            [self transformItemView:[_wrappers objectAtIndex:index] atIndex:index withCurrentPage:currentPageAsFloat];
		}
        index ++ ;
    }
}

#pragma mark -
#pragma mark Scrolling

- (void)didScroll
{
    if (_transition != nil){
        [self transformViews];
    }
}

-(void)fireEventWithData:(NSString*)event andPageAsFloat:(CGFloat)pageAsFloat propagate:(BOOL)propagate
{
    if ([self.proxy _hasListeners:event checkParent:propagate])
	{
		[self.proxy fireEvent:event withObject:[NSDictionary dictionaryWithObjectsAndKeys:
                                                    NUMINT(currentPage), @"currentPage",
                                                    NUMFLOAT(pageAsFloat), @"currentPageAsFloat",
                                                    [[self proxy] viewAtIndex:currentPage], @"view", nil] propagate:propagate];
        
	}
}

-(void)fireEventWithData:(NSString*)event andPageAsFloat:(CGFloat)pageAsFloat
{
    [self fireEventWithData:event andPageAsFloat:pageAsFloat propagate:NO];
}

-(void)fireEventWithData:(NSString*)event propagate:(BOOL)propagate
{
    [self fireEventWithData:event andPageAsFloat:currentPage propagate:propagate];
}

-(void)fireEventWithData:(NSString*)event
{
    [self fireEventWithData:event propagate:NO];
}

-(void)scrollViewDidScroll:(UIScrollView *)scrollView
{
	//switch page control at 50% across the center - this visually looks better
    int page = currentPage;
    float nextPageAsFloat = [self getPageFromOffset:scrollview.contentOffset];
    int nextPage = MIN(floor(nextPageAsFloat - 0.5) + 1, [[self proxy] viewCount] - 1);
    if (page != nextPage) {
        int curCacheSize = cacheSize;
        int minCacheSize = cacheSize;
        if (enforceCacheRecalculation) {
            minCacheSize = ABS(page - nextPage)*2 + 1;
            if (minCacheSize < cacheSize) {
                minCacheSize = cacheSize;
            }
        }
        pageChanged = YES;
        cacheSize = minCacheSize;
        if (_updatePageDuringScroll) [self updateCurrentPage:nextPage];
        cacheSize = curCacheSize;
    }
	[self fireEventWithData:@"scroll" andPageAsFloat:nextPageAsFloat];
    [self didScroll];
}

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
    _updatePageDuringScroll = YES;
	[self fireEventWithData:@"scrollstart"];
    if (pageChanged) {
        [self manageCache:currentPage];
    }
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
    _updatePageDuringScroll = NO;
    //Since we are now managing cache at end of scroll, ensure quick scroll is disabled to avoid blank screens.
    if (pageChanged) {
        [scrollview setUserInteractionEnabled:!decelerate];
    }
}

-(void)scrollViewDidEndScrollingAnimation:(UIScrollView *)scrollView
{
    _updatePageDuringScroll = NO;
	// called when setContentOffset/scrollRectVisible:animated: finishes. not called if not animating
	[self scrollViewDidEndDecelerating:scrollView];
    [self didScroll];
}


-(void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
    _updatePageDuringScroll = NO;
    if (rotatedWhileScrolling) {
        [self setContentOffsetForPage:[self currentPage] animated:YES];
        rotatedWhileScrolling = NO;
    }

	// At the end of scroll animation, reset the boolean used when scrolls originate from the UIPageControl
	int pageNum = [self currentPage];
	handlingPageControlEvent = NO;
    
    lastPage = pageNum;
    [self updateCurrentPage:pageNum];

	[self fireEventWithData:@"scrollend"];
	[self manageCache:currentPage];
	pageChanged = NO;
	[scrollview setUserInteractionEnabled:YES];
    [self didScroll];
}

@end

#endif
