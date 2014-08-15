/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITABLEVIEW) || defined(USE_TI_UILISTVIEW)
#ifndef USE_TI_UISEARCHBAR
#define USE_TI_UISEARCHBAR
#endif
#endif

#ifdef USE_TI_UISEARCHBAR

#import "TiUISearchBarProxy.h"
#import "TiUISearchBar.h"

@interface TiViewProxy(Private)
-(UIViewController*)getContentController;
@end

@interface TiUISearchBar()
-(void)setShowCancel_:(id)value withObject:(id)object;
@end

@implementation TiUISearchBarProxy
@synthesize canHaveSearchDisplayController;


NSArray* keySequence;

-(NSArray*)keySequence
{
	if (keySequence == nil) {
		keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"barColor"]] retain];
	}
	return keySequence;
}


#pragma mark Method forwarding

-(NSString*)apiName
{
    return @"Ti.UI.SearchBar";
}

-(void)_configure
{
    canHaveSearchDisplayController = NO;
	[super _configure];
}


-(void)blur:(id)args
{
	[self makeViewPerformSelector:@selector(blur:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(void)focus:(id)args
{
	[self makeViewPerformSelector:@selector(focus:) withObject:args createIfNeeded:YES waitUntilDone:NO];
}

-(void)setShowCancel:(id)value withObject:(id)object
{
	//ViewAttached gives a false negative when not attached to a window.
	TiThreadPerformOnMainThread(^{
		[(TiUISearchBar*)[self view] setShowCancel_:value withObject:object];
	}, NO);
}


-(void)setDelegate:(id<UISearchBarDelegate>)delegate
{
    [self makeViewPerformSelector:@selector(setDelegate:) withObject:delegate createIfNeeded:(delegate!=nil) waitUntilDone:YES];
}

-(UISearchBar*)searchBar
{
	return [(TiUISearchBar*)[self view] searchBar];
}

-(void)ensureSearchBarHeirarchy
{
    WARN_IF_BACKGROUND_THREAD;
    if ([self viewAttached]) {
        UISearchBar* searchBar = [self searchBar];
        if ([searchBar superview] != view) {
            [view addSubview:searchBar];
            [searchBar setFrame:[view bounds]];
        }
    }
    
}


-(NSMutableDictionary*)langConversionTable
{
    return [NSMutableDictionary dictionaryWithObjectsAndKeys:@"prompt",@"promptid",@"hintText",@"hinttextid",nil];
}

-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

-(TiSearchDisplayController*)searchController {
    return [(TiUISearchBar*)[self view] searchController];
}


-(UIViewController*)getContentController
{
    if (canHaveSearchDisplayController) {
        return [super getContentController];
    }
    return nil;
}

USE_VIEW_FOR_CONTENT_SIZE
@end

#endif
