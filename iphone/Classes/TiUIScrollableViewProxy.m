/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UISCROLLABLEVIEW

#import "TiUIScrollableViewProxy.h"
#import "TiUIScrollableView.h"

@implementation TiUIScrollableViewProxy
@synthesize viewProxies, verticalLayout;

-(NSArray *)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"views",@"currentPage"]] retain];;
    });
    return keySequence;
}

-(void)_initWithProperties:(NSDictionary *)properties
{
    pthread_rwlock_init(&viewsLock, NULL);
    verticalLayout = NO;
    [self initializeProperty:@"currentPage" defaultValue:NUMINT(0)];
    [self initializeProperty:@"pagingControlColor" defaultValue:@"black"];
    [self initializeProperty:@"pagingControlHeight" defaultValue:NUMINT(20)];
    [self initializeProperty:@"showPagingControl" defaultValue:NUMBOOL(NO)];
    [self initializeProperty:@"pagingControlAlpha" defaultValue:NUMFLOAT(1.0)];
    [self initializeProperty:@"overlayEnabled" defaultValue:NUMBOOL(NO)];
    [self initializeProperty:@"pagingControlOnTop" defaultValue:NUMBOOL(NO)];
    [super _initWithProperties:properties];
}

// Special handling to try and avoid Apple's detection of private API 'layout'
-(void)setValue:(id)value forUndefinedKey:(NSString *)key
{
    if ([key isEqualToString:[@"lay" stringByAppendingString:@"out"]]) {
        verticalLayout = ([value isKindOfClass:[NSString class]] && [value caseInsensitiveCompare:@"vertical"]==NSOrderedSame);
        if ([self view] != nil) {
            TiUIScrollableView * ourView = (TiUIScrollableView *)[self view];
            [ourView setVerticalLayout:verticalLayout];
        }
        [self replaceValue:value forKey:[@"lay" stringByAppendingString:@"out"] notification:YES];
        return;
    }
    [super setValue:value forUndefinedKey:key];
}

- (void) dealloc
{
	pthread_rwlock_destroy(&viewsLock);
	[viewProxies makeObjectsPerformSelector:@selector(setParent:) withObject:nil];
	[viewProxies release];
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.UI.ScrollableView";
}

-(void)lockViews
{
	pthread_rwlock_rdlock(&viewsLock);
}

-(void)lockViewsForWriting
{
	pthread_rwlock_wrlock(&viewsLock);
}

-(void)unlockViews
{
	pthread_rwlock_unlock(&viewsLock);
}

-(NSArray *)views
{
	[self lockViews];
	NSArray * result = [viewProxies copy];
	[self unlockViews];
	return [result autorelease];
}

-(NSUInteger)viewCount
{
	[self lockViews];
	NSUInteger result = [viewProxies count];
	[self unlockViews];
	return result;
}

-(void)setViews:(id)args
{
    ENSURE_ARRAY(args);
    NSMutableArray* newViews = [NSMutableArray array];
    for (id arg in args)
    {
        //dont set rootproxy that the view itself becomes the rootproxy
        TiProxy *child = [self createChildFromObject:arg rootProxy:nil];
        if (child) {
            [self rememberProxy:child];
            [self childAdded:child atIndex:-1 shouldRelayout:NO];
            [newViews addObject:child];
        }
        
    }
    [self lockViewsForWriting];
    for (id oldViewProxy in viewProxies)
    {
        if (![args containsObject:oldViewProxy])
        {
            [self childRemoved:oldViewProxy wasChild:NO shouldDetach:YES];
        }
    }
    [viewProxies release];
    viewProxies = [newViews retain];
#ifdef TI_USE_AUTOLAYOUT
    for (TiViewProxy* proxy in viewProxies)
    {
        [[self view] addSubview:[proxy view]];
    }
#endif
    [self unlockViews];
    [self replaceValue:args forKey:@"views" notification:YES];
}

-(void)makeChildrenPerformSelector:(SEL)selector withObject:(id)object
{
    [super makeChildrenPerformSelector:selector withObject:object];
    [self lockViewsForWriting];
    [viewProxies makeObjectsPerformSelector:selector withObject:object];
    [self unlockViews];
}

-(id)getView:(id)args
{
    ENSURE_SINGLE_ARG(args, NSNumber)
    NSInteger index = [TiUtils intValue:args def:-1];
    if (index >= 0 && index < [viewProxies count]) {
        return [viewProxies objectAtIndex:index];
    }
    return nil;
}

-(void)addView:(id)args
{
	ENSURE_SINGLE_ARG(args,TiViewProxy);

	[self lockViewsForWriting];
	[self rememberProxy:args];
	[args setParent:self];
	if (viewProxies != nil)
	{
		[viewProxies addObject:args];
	}
	else
	{
		viewProxies = [[NSMutableArray alloc] initWithObjects:args,nil];
	}
	[self unlockViews];	
	[self makeViewPerformSelector:@selector(addView:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(void)removeView:(id)args
{	//TODO: Refactor this properly.
	ENSURE_SINGLE_ARG(args,NSObject);

	[self lockViewsForWriting];
	TiViewProxy * doomedView;
	if ([args isKindOfClass:[TiViewProxy class]])
	{
		doomedView = args;

		if (![viewProxies containsObject:doomedView])
		{
			[self unlockViews];
			[self throwException:@"view not in the scrollableView" subreason:nil location:CODELOCATION];
			return;
		}
	}
	else if ([args respondsToSelector:@selector(intValue)])
	{
		int doomedIndex = [args intValue];
		if ((doomedIndex >= 0) && (doomedIndex < [viewProxies count]))
		{
			doomedView = [viewProxies objectAtIndex:doomedIndex];
		}
		else
		{
			[self unlockViews];
			[self throwException:TiExceptionRangeError subreason:@"invalid view index" location:CODELOCATION];
			return;
		}
	}
	else
	{
		[self unlockViews];
		[self throwException:TiExceptionInvalidType subreason:
				[NSString stringWithFormat:@"argument needs to be a number or view, but was %@ instead.",
				[args class]] location:CODELOCATION];
		return;
	}

	TiThreadPerformOnMainThread(^{[doomedView detachView];}, NO);
	[self forgetProxy:doomedView];
	[viewProxies removeObject:doomedView];
	[self unlockViews];	
	[self makeViewPerformSelector:@selector(removeView:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(NSInteger)indexFromArg:(id)args
{
	NSInteger pageNum = 0;
	if ([args isKindOfClass:[TiViewProxy class]])
	{
		[self lockViews];
		pageNum = [[self viewProxies] indexOfObject:args];
		[self unlockViews];
	}
	else
	{
		pageNum = [TiUtils intValue:args];
	}
	
	return pageNum;
}


-(void)scrollToView:(id)args
{	//TODO: Refactor this properly.
	[self makeViewPerformSelector:@selector(scrollToView:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(void) willChangeSize
{
    //Ensure the size change signal goes to children 
    NSArray *curViews = [self views];
    for (TiViewProxy *child in curViews) {
        [child parentSizeWillChange];
    }
    [super willChangeSize];
}

-(void)childWillResize:(TiViewProxy *)child withinAnimation:(TiViewAnimationStep*)animation
{
	BOOL hasChild = [[self children] containsObject:child];

	if (!hasChild)
	{
		return;
		//In the case of views added with addView, as they are not part of children, they should be ignored.
	}
	[super childWillResize:child withinAnimation:animation];
}

-(TiViewProxy *)viewAtIndex:(NSInteger)index
{
	[self lockViews];
	// force index to be in range in case the scrollable view is rotated while scrolling
	if (index < 0) {
		index = 0;
	} else if (index >= [viewProxies count]) {
		index = [viewProxies count] - 1;
	}
	TiViewProxy * result = [viewProxies objectAtIndex:index];
	[self unlockViews];
	return result;
}

#ifndef TI_USE_AUTOLAYOUT
-(UIView *)parentViewForChild:(TiViewProxy *)child
{
	[self lockViews];
	NSUInteger index = [viewProxies indexOfObject:child];
	[self unlockViews];
	
	if (index != NSNotFound)
	{
		TiUIScrollableView * ourView = (TiUIScrollableView *)[self view];
		NSArray * scrollWrappers = [ourView wrappers];
		if (index < [scrollWrappers count])
		{
			return [scrollWrappers objectAtIndex:index];
		}
		//Hideous hack is hideous. This should stave off the bugs until layout is streamlined
		[ourView refreshScrollView:[[self view] bounds] readd:YES];
		scrollWrappers = [ourView wrappers];
		if (index < [scrollWrappers count])
		{
			return [scrollWrappers objectAtIndex:index];
		}
	}
	//Adding the view to a scrollable view is invalid.
	return nil;
}
#endif
-(CGSize)autSizeForSize:(CGSize)size
{
    CGSize result = CGSizeZero;
    NSArray* theChildren = [self views];
    for (TiViewProxy * thisChildProxy in theChildren) {
        CGSize thisSize = [thisChildProxy minimumParentSizeForSize:size];
        if (result.width < thisSize.width) {
            result.width = thisSize.width;
        }
        if (result.height < thisSize.height) {
            result.height = thisSize.height;
        }
    }
    return result;
}

- (void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    if ([self viewAttached]) {
        [(TiUIScrollableView*)[self view] manageRotation];
    }
}

-(void)moveNext:(id)args
{
	ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
	[self makeViewPerformSelector:@selector(moveNext:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(void)movePrevious:(id)args
{
	ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
	[self makeViewPerformSelector:@selector(movePrevious:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(void)setScrollAnimationDuration:(id)value
{
    [(TiUIScrollableView*)[self view] setSwitchPageAnimationDuration:[TiUtils intValue:value]];
}

-(NSNumber*)scrollAnimationDuration
{
	return NUMINT([(TiUIScrollableView*)[self view]switchPageAnimationDuration]);
}

@end

#endif
