/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UISCROLLVIEW

#import "TiUIScrollViewProxy.h"
#import "TiUIScrollView.h"

#import "TiUtils.h"

@implementation TiUIScrollViewProxy

+(NSSet*)transferableProperties
{
    NSSet *common = [TiViewProxy transferableProperties];
    return [common setByAddingObjectsFromSet:[NSSet setWithObjects:@"contentOffset",
                                              @"minZoomScale",@"maxZoomScale",@"zoomScale",
                                              @"canCancelEvents",@"contentWidth",@"contentHeight",
                                              @"showHorizontalScrollIndicator",@"showVerticalScrollIndicator",
                                              @"scrollIndicatorStyle", @"scrollsToTop", @"horizontalBounce",
                                              @"verticalBounce", @"scrollingEnabled", @"disableBounce", nil]];
}

static NSArray* scrollViewKeySequence;
-(NSArray *)keySequence
{
    if (scrollViewKeySequence == nil)
    {
        //URL has to be processed first since the spinner depends on URL being remote
        scrollViewKeySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"minZoomScale",@"maxZoomScale",@"zoomScale"]] retain];
    }
    return scrollViewKeySequence;
}

-(void)_initWithProperties:(NSDictionary *)properties
{
    [self initializeProperty:@"minZoomScale" defaultValue:NUMFLOAT(1.0)];
    [self initializeProperty:@"maxZoomScale" defaultValue:NUMFLOAT(1.0)];
    [self initializeProperty:@"zoomScale" defaultValue:NUMFLOAT(1.0)];
    [self initializeProperty:@"canCancelEvents" defaultValue:NUMBOOL(YES)];
    [self initializeProperty:@"scrollingEnabled" defaultValue:NUMBOOL(YES)];
    [super _initWithProperties:properties];
}

-(NSString*)apiName
{
    return @"Ti.UI.ScrollView";
}

-(TiPoint *) contentOffset{
    if([self viewAttached]){
        TiThreadPerformOnMainThread(^{
                   contentOffset = [[TiPoint alloc] initWithPoint:CGPointMake(
                                        [(TiUIScrollView *)[self view] scrollView].contentOffset.x,
                                        [(TiUIScrollView *)[self view] scrollView].contentOffset.y)] ; 
          }, YES);
    }
    else{
        contentOffset = [[TiPoint alloc] initWithPoint:CGPointMake(0,0)];
    }
    return [contentOffset autorelease];
}

-(void)windowWillOpen
{
    [super windowWillOpen];
    //Since layout children is overridden in scrollview need to make sure that 
    //a full layout occurs atleast once if view is attached
    if ([self viewAttached]) {
        [self contentsWillChange];
    }
}

-(void)contentsWillChange
{
	if ([self viewAttached] && parentVisible)
	{
		[(TiUIScrollView *)[self view] setNeedsHandleContentSize];
	}
	[super contentsWillChange];
}

-(void)willChangeSize
{
	if ([self viewAttached] && parentVisible)
	{
		[(TiUIScrollView *)[self view] setNeedsHandleContentSizeIfAutosizing];
	}
	[super willChangeSize];
}


-(void)layoutChildren:(BOOL)optimize
{
	if (![self viewAttached] || !parentVisible)
	{
		return;
	}

	[(TiUIScrollView *)[self view] handleContentSizeIfNeeded];
}

-(void)layoutChildrenAfterContentSize:(BOOL)optimize
{
	[super layoutChildren:optimize];	
}

-(CGSize)autoSizeForSize:(CGSize)size
{
    CGSize contentSize = CGSizeMake(size.width,size.height);
    if ([(TiUIScrollView *)[self view] flexibleContentWidth]) {
        contentSize.width = 0; //let the child be as wide as it wants.
    }
    if ([(TiUIScrollView *)[self view] flexibleContentHeight]) {
        contentSize.height = 0; //let the child be as high as it wants.
    }
    return [super autoSizeForSize:contentSize];
    }
    
//-(CGRect)computeChildSandbox:(TiViewProxy*)child withBounds:(CGRect)bounds
//{
//    CGRect contentSize = CGRectMake(bounds.origin.x, bounds.origin.y, bounds.size.width, bounds.size.height);
//    if ([(TiUIScrollView *)[self view] flexibleContentWidth]) {
//        contentSize.size.width = 0; //let the child be as wide as it wants.
//    }
//    if ([(TiUIScrollView *)[self view] flexibleContentHeight]) {
//        contentSize.size.height = 0; //let the child be as high as it wants.
//    }
//    
//    return [super computeChildSandbox:child withBounds:contentSize];
//}
-(void)childWillResize:(TiViewProxy *)child withinAnimation:(TiViewAnimationStep*)animation
{
	[super childWillResize:child withinAnimation:animation];
	[(TiUIScrollView *)[self view] setNeedsHandleContentSizeIfAutosizing];
}

-(BOOL)optimizeSubviewInsertion
{
    return YES;
}

-(UIView *)parentViewForChild:(TiViewProxy *)child
{
	return [(TiUIScrollView *)[self view] wrapperView];
}

-(void)scrollTo:(id)args
{
	ENSURE_ARG_COUNT(args,2);
	TiPoint * offset = [[TiPoint alloc] initWithPoint:CGPointMake(
			[TiUtils floatValue:[args objectAtIndex:0]],
			[TiUtils floatValue:[args objectAtIndex:1]])];

	[self setContentOffset:offset withObject:Nil];
	[offset release];
}

-(void)scrollToBottom:(id)args
{
    TiThreadPerformOnMainThread(^{
        [(TiUIScrollView *)[self view] scrollToBottom];
    }, YES);
}

-(void) setContentOffset:(id)value withObject:(id)animated
{
    TiThreadPerformOnMainThread(^{
        [(TiUIScrollView *)[self view] setContentOffset_:value withObject:animated];
    }, YES);
}

-(void) setZoomScale:(id)value withObject:(id)animated
{
    TiThreadPerformOnMainThread(^{
        [(TiUIScrollView *)[self view] setZoomScale_:value withObject:animated];
    }, YES);
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView_               // scrolling has ended
{
	if ([self _hasListeners:@"scrollend" checkParent:NO])
	{
		[self fireEvent:@"scrollend" propagate:NO checkForListener:NO];
	}
}

-(void)scrollViewDidScroll:(UIScrollView *)scrollView
{
    CGPoint offset = [scrollView contentOffset];
	if ([self _hasListeners:@"scroll" checkParent:NO])
	{
        [self fireEvent:@"scroll" withObject:[NSDictionary dictionaryWithObjectsAndKeys:
                NUMFLOAT(offset.x),@"x",
                NUMFLOAT(offset.y),@"y",
                NUMFLOAT(scrollView.zoomScale),@"curZoomScale",
                NUMBOOL([scrollView isZooming]),@"zooming",
                NUMBOOL([scrollView isDecelerating]),@"decelerating",
                NUMBOOL([scrollView isDragging]),@"dragging",
				nil] propagate:NO checkForListener:NO];
    }
}

- (void)scrollViewDidEndZooming:(UIScrollView *)scrollView withView:(UIView *)view atScale:(float)scale
{
	[self replaceValue:NUMFLOAT(scale) forKey:@"zoomScale" notification:NO];
	
	if ([self _hasListeners:@"scale" checkParent:NO])
	{
		[self fireEvent:@"scale" withObject:[NSDictionary dictionaryWithObjectsAndKeys:
											  NUMFLOAT(scale),@"scale",
											  nil] propagate:NO checkForListener:NO];
	}
}

-(void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
	if([self _hasListeners:@"dragstart" checkParent:NO])
	{
		[self fireEvent:@"dragstart" propagate:NO checkForListener:NO];
	}
}

//listerner which tells when dragging ended in the scroll view.

-(void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
	if([self _hasListeners:@"dragend" checkParent:NO])
	{
		[self fireEvent:@"dragend" withObject:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:decelerate],@"decelerate",nil] propagate:NO checkForListener:NO];
	}
}

DEFINE_DEF_PROP(scrollsToTop,@YES);

@end

#endif
