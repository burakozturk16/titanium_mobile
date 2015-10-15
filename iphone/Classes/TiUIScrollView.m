/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UISCROLLVIEW

#import "TiUIScrollView.h"
#import "TiUIScrollViewProxy.h"
#import "TiUtils.h"



@implementation TiUIScrollView
{
    BOOL _flexibleContentWidth;
    BOOL _flexibleContentHeight;
}
//@synthesize contentWidth;

- (void) dealloc
{
	RELEASE_WITH_DELEGATE(scrollview);
#ifndef TI_USE_AUTOLAYOUT
	RELEASE_TO_NIL(wrapperView);
#endif
	[super dealloc];
}

- (id) init
{
    self = [super init];
    if (self != nil)
    {
        _flexibleContentWidth = NO;
        _flexibleContentHeight = NO;
    }
    return self;
}


#ifndef TI_USE_AUTOLAYOUT
-(UIView *)wrapperView
{
	if (wrapperView == nil)
	{
		CGRect wrapperFrame;
		wrapperFrame.size = [[self scrollview] contentSize];
		wrapperFrame.origin = CGPointZero;
		wrapperView = [[UIView alloc] initWithFrame:wrapperFrame];
		[wrapperView setUserInteractionEnabled:YES];
		[scrollview addSubview:wrapperView];
	}
	return wrapperView;
}
#endif

-(TDUIScrollView *)scrollview
{
	if(scrollview == nil)
	{
#ifdef TI_USE_AUTOLAYOUT
		scrollview = [[TDUIScrollView alloc] init];
		contentView = [[TiLayoutView alloc] init];
        [contentView setTranslatesAutoresizingMaskIntoConstraints:NO];
        [scrollview setTranslatesAutoresizingMaskIntoConstraints:NO];
		[contentView setViewName:@"TiScrollView.ContentView"];
        
        [contentView setDefaultHeight:TiDimensionAutoSize];
        [contentView setDefaultWidth:TiDimensionAutoSize];
        
        [scrollview addSubview:contentView];
#else
		scrollview = [[TDUIScrollView alloc] initWithFrame:[self bounds]];
		[scrollview setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
#endif
		[scrollview setBackgroundColor:[UIColor clearColor]];
		[scrollview setShowsHorizontalScrollIndicator:NO];
		[scrollview setShowsVerticalScrollIndicator:NO];
		[scrollview setDelegate:self];
        [scrollview setTouchDelegate:self];
		[self addSubview:scrollview];
	}
	return scrollview;
}

-(UIView*)viewForHitTest
{
    return wrapperView;
}

-(UIView*)parentViewForChildren
{
    return [self wrapperView];
}

-(void)setClipChildren_:(id)arg
{
    [super setClipChildren_:arg];
    [self scrollview].clipsToBounds = self.clipsToBounds;
}

- (id)accessibilityElement
{
	return [self scrollView];
}

-(BOOL)flexibleContentWidth
{
    return _flexibleContentWidth;
}

-(BOOL)flexibleContentHeight
{
    return _flexibleContentHeight;
}

-(void)setNeedsHandleContentSizeIfAutosizing
{
#ifndef TI_USE_AUTOLAYOUT
	if ([self flexibleContentWidth] || [self flexibleContentHeight])
	{
		[self setNeedsHandleContentSize];
	}
#endif
}

-(void)setNeedsHandleContentSize
{
#ifndef TI_USE_AUTOLAYOUT
	if (!needsHandleContentSize)
	{
		needsHandleContentSize = YES;
        if (configurationSet) {
            [self handleContentSize];
        }
	}
#endif
}


-(BOOL)handleContentSizeIfNeeded
{
#ifndef TI_USE_AUTOLAYOUT
	if (needsHandleContentSize)
	{
		[self handleContentSize];
		return YES;
	}
#endif
    return NO;
}

-(void)handleContentSize
{
 #ifndef TI_USE_AUTOLAYOUT
   if (!needsHandleContentSize) {
        return;
    }
    CGSize newContentSize = [self bounds].size;
    CGFloat scale = [scrollview zoomScale];
    
    CGSize autoSize;
    
    if ([self flexibleContentWidth] || [self flexibleContentHeight])
    {
        autoSize = [(TiViewProxy *)[self proxy] autoSizeForSize:newContentSize ignoreMinMax:YES];
    }
    
    switch (contentWidth.type)
    {
        case TiDimensionTypeDip:
        case TiDimensionTypePercent:
        {
            newContentSize.width = TiDimensionCalculateValue(contentWidth, newContentSize.width);
            break;
        }
        case TiDimensionTypeAutoSize:
        case TiDimensionTypeAuto: // TODO: This may break the layout spec for content "auto"
        {
            newContentSize.width = MAX(newContentSize.width,autoSize.width);
            break;
        }
        case TiDimensionTypeUndefined:
        case TiDimensionTypeAutoFill: // Assume that "fill" means "fill scrollview bounds"; not in spec
        default: {
            break;
        }
    }
    
    switch (contentHeight.type)
    {
        case TiDimensionTypeDip:
        case TiDimensionTypePercent:
        {
            minimumContentHeight = TiDimensionCalculateValue(contentHeight, newContentSize.height);
            break;
        }
        case TiDimensionTypeAutoSize:
        case TiDimensionTypeAuto: // TODO: This may break the layout spec for content "auto"
        {
            minimumContentHeight = MAX(newContentSize.height,autoSize.height);
            break;
        }
        case TiDimensionTypeUndefined:
        case TiDimensionTypeAutoFill: // Assume that "fill" means "fill scrollview bounds"; not in spec
        default:
            minimumContentHeight = newContentSize.height;
            break;
    }
    newContentSize.width *= scale;
    newContentSize.height = scale * minimumContentHeight;
    CGSize oldContentSize = scrollview.contentSize;
    if (oldContentSize.width != newContentSize.width ||
        oldContentSize.height != newContentSize.height) {
        CGRect wrapperBounds;
        wrapperBounds.origin = CGPointZero;
        wrapperBounds.size = newContentSize;
        [wrapperView setFrame:wrapperBounds];
        [scrollview setContentSize:newContentSize];
        [self scrollViewDidZoom:scrollview];
    }
    
    [(TiUIScrollViewProxy *)[self proxy] layoutChildrenAfterContentSize:NO];
    needsHandleContentSize = NO;
#endif
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)visibleBounds
{
    if ([self flexibleContentWidth] || [self flexibleContentHeight])
	{
		needsHandleContentSize = YES;
	}
    [super frameSizeChanged:frame bounds:visibleBounds];
}



#ifndef TI_USE_AUTOLAYOUT
-(void)setContentWidth_:(id)value
{
	contentWidth = [TiUtils dimensionValue:value];
    _flexibleContentWidth = TiDimensionIsAuto(contentWidth) || TiDimensionIsAutoSize(contentWidth);
    [self setNeedsHandleContentSize];
}

-(void)setContentHeight_:(id)value
{
	contentHeight = [TiUtils dimensionValue:value];
    _flexibleContentHeight = TiDimensionIsAuto(contentHeight) || TiDimensionIsAutoSize(contentHeight);
    [self setNeedsHandleContentSize];
}
#endif

- (void)layoutSubviews {
    [super layoutSubviews];
    
    // Center the image as it becomes smaller than the size of the screen
    CGSize boundsSize = self.bounds.size;
    CGRect frameToCenter = wrapperView.frame;
    
    // Horizontally
    if (frameToCenter.size.width < boundsSize.width) {
        frameToCenter.origin.x = floorf((boundsSize.width - frameToCenter.size.width) / 2.0);
    } else {
        frameToCenter.origin.x = 0;
    }
    
    // Vertically
    if (frameToCenter.size.height < boundsSize.height) {
        frameToCenter.origin.y = floorf((boundsSize.height - frameToCenter.size.height) / 2.0);
    } else {
        frameToCenter.origin.y = 0;
    }
    
    // Center
    if (!CGRectEqualToRect(wrapperView.frame, frameToCenter)) {
        wrapperView.frame = frameToCenter;
    }
}

- (void)zoomToPoint:(CGPoint)touchPoint withScale: (CGFloat)scale animated: (BOOL)animated
{
    touchPoint.x -= wrapperView.frame.origin.x;
    touchPoint.y -= wrapperView.frame.origin.y;
    [super zoomToPoint:touchPoint withScale:scale animated:animated];
}
#pragma mark scrollView delegate stuff

- (UIView *)viewForZoomingInScrollView:(UIScrollView *)scrollView
{
	return [self wrapperView];
}

- (void)scrollViewDidZoom:(UIScrollView *)scrollView_
{
    [self setNeedsLayout];
    [self layoutIfNeeded];
    [super scrollViewDidZoom:scrollView_];
}



- (void)scrollViewDidEndZooming:(UIScrollView *)scrollView_ withView:(UIView *)view atScale:(CGFloat)scale
{
    [super scrollViewDidEndZooming:scrollView_ withView:view atScale:scale];
}

-(id)zoomScale_ {
    return @(scrollview.zoomScale);
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView
{
    [super scrollViewDidScroll:scrollView];
}

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
    [super scrollViewWillBeginDragging:scrollView];
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
    [super scrollViewDidEndDragging:scrollView willDecelerate:decelerate];
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
    [super scrollViewDidEndDecelerating:scrollView];
}

- (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView
{
    return [super scrollViewShouldScrollToTop:scrollView];
}

- (void)scrollViewDidScrollToTop:(UIScrollView *)scrollView
{
    return [super scrollViewDidScrollToTop:scrollView];
}

#pragma mark Keyboard delegate stuff

-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop
{
	InsetScrollViewForKeyboard(scrollview,keyboardTop,minimumContentHeight);
}

-(void)scrollToShowView:(UIView *)firstResponderView withKeyboardHeight:(CGFloat)keyboardTop
{
#ifndef TI_USE_AUTOLAYOUT
    if ([scrollview isScrollEnabled]) {
        CGRect responderRect = [wrapperView convertRect:[firstResponderView bounds] fromView:firstResponderView];
        OffsetScrollViewForRect(scrollview,keyboardTop,minimumContentHeight,responderRect);
    }
#endif
}

@end

#endif
