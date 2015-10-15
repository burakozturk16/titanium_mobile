/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_UITABLEVIEW) || defined(USE_TI_UILISTVIEW)
#ifndef USE_TI_UISEARCHBAR
#define USE_TI_UISEARCHBAR
#endif
#endif

#ifdef USE_TI_UISEARCHBAR

#import "TiUtils.h"
#import "TiUISearchBarProxy.h"
#import "TiUISearchBar.h"
#import "ImageLoader.h"


@implementation TiSearchDisplayController

/**
 *  Overwrite the `setActive:animated:` method to make sure the UINavigationBar
 *  does not get hidden and the SearchBar does not add space for the statusbar height.
 *
 *  @param visible   `YES` to display the search interface if it is not already displayed; NO to hide the search interface if it is currently displayed.
 *  @param animated  `YES` to use animation for a change in visible state, otherwise NO.
 */
- (void)setActive:(BOOL)visible animated:(BOOL)animated
{
    BOOL oldStatusBar = [[UIApplication sharedApplication] isStatusBarHidden];
    BOOL oldNavBar = [self.searchContentsController.navigationController isNavigationBarHidden];
    //    [[UIApplication sharedApplication] setStatusBarHidden:YES];
    //    [self.searchContentsController.navigationController setNavigationBarHidden:YES animated:NO];
    
    [super setActive:visible animated:animated];
    
    if (self.preventHiddingNavBar) {
        [self.searchContentsController.navigationController setNavigationBarHidden:oldNavBar animated:NO];
        [[UIApplication sharedApplication] setStatusBarHidden:oldStatusBar];
    }
    
}

@end


@implementation TiUISearchBar
{
    TiSearchDisplayController *searchController;
    BOOL hideNavBarWithSearch;
    BOOL showsCancelButton;
    UITextField *searchBarTextField;
}

-(void)releaseSearchController {
    if (searchController) {
        searchController.searchResultsDataSource = nil;
        searchController.searchResultsDelegate = nil;
        searchController.delegate = nil;
        RELEASE_TO_NIL(searchController)
    }
}


- (id)init
{
    self = [super init];
    if (self) {
        hideNavBarWithSearch = YES;
        showsCancelButton = NO;
    }
    return self;
}


-(void)dealloc
{
    delegate = nil;
	[searchView setDelegate:nil];
    RELEASE_TO_NIL(searchBarTextField)
	RELEASE_TO_NIL(searchView);
	[self releaseSearchController];
	[super dealloc];
}

-(CGSize)contentSizeForSize:(CGSize)size
{
   return [[self searchBar] sizeThatFits:size];
}

-(UITextField*)textWidgetView {
    if (!searchBarTextField) {
        for (UIView *subView in self.searchBar.subviews)
        {
            for (UIView *secondLevelSubview in subView.subviews){
                if ([secondLevelSubview isKindOfClass:[UITextField class]])
                {
                    searchBarTextField = [(UITextField *)secondLevelSubview retain];
                    break;
                }
            }
        }
    }
    return searchBarTextField;
}

-(UISearchBar*)searchBar
{
	if (searchView==nil)
	{
		searchView = [[UISearchBar alloc] initWithFrame:CGRectZero];
		[searchView setAutoresizingMask:UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight];
		[searchView setDelegate:self];
		[self addSubview:searchView];
	}
	return searchView;
}

-(UIView*)viewForHitTest
{
    return searchView;
}

- (void)didMoveToSuperview
{
	[super didMoveToSuperview];
    if (self.superview == nil) {
        [self releaseSearchController];
    }
    else {
        RELEASE_TO_NIL(searchController)
        [self searchController];
    }
}

-(TiSearchDisplayController*)searchController {
    if (!searchController && ((TiUISearchBarProxy*)proxy).canHaveSearchDisplayController && [self superview] != nil) {
        UIViewController* controller = [self getContentController];
        if (controller) {
            searchController = [[TiSearchDisplayController alloc] initWithSearchBar:[self searchBar] contentsController:controller];
            searchController.preventHiddingNavBar = !hideNavBarWithSearch;
            if ([delegate conformsToProtocol:@protocol(UITableViewDataSource)]) {
                searchController.searchResultsDataSource = delegate;
            }
            if ([delegate conformsToProtocol:@protocol(UITableViewDelegate)]) {
                searchController.searchResultsDelegate = delegate;
            }
            //all is optional so we can do this
            searchController.delegate = delegate;
        }
    }
    return searchController;
}

- (id)accessibilityElement
{
	return [self searchBar];
}

-(void)setBounds:(CGRect)bounds
{
	[super setBounds:bounds];
}

-(void)setFrame:(CGRect)frame
{
	[super setFrame:frame];
}


-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
	[[self searchBar] setFrame:bounds];
    [[self searchBar] setShowsCancelButton:showsCancelButton animated:NO];
    [super frameSizeChanged:frame bounds:bounds];
}

-(void)setDelegate:(id<UISearchBarDelegate>)delegate_
{
	delegate = delegate_;
    if (searchController) {
        RELEASE_TO_NIL(searchController)
        [self searchController];
    }
}

#pragma mark View controller stuff

-(void)blur:(id)args
{
	[searchView resignFirstResponder];
}

-(void)focus:(id)args
{
	[searchView becomeFirstResponder];
}


-(void)setValue_:(id)value
{
	[[self searchBar] setText:[TiUtils stringValue:value]];
}

-(void)setShowBookmark_:(id)value
{
	UISearchBar *search = [self searchBar];
	[search setShowsBookmarkButton:[TiUtils boolValue:value]];
	[search sizeToFit];
}

-(void)setShowCancel_:(id)value withObject:(id)object
{
	BOOL boolValue = [TiUtils boolValue:value];
	BOOL animated = [TiUtils boolValue:@"animated" properties:object def:NO];
	//TODO: Value checking and exception generation, if necessary.
    
	showsCancelButton = boolValue;
    
	//ViewAttached gives a false negative when not attached to a window.
    UISearchBar *search = [self searchBar];
    [search setShowsCancelButton:showsCancelButton animated:animated];
    [search sizeToFit];
}

-(void)setHintText_:(id)value
{
	[[self searchBar] setPlaceholder:[TiUtils stringValue:value]];
}

-(void)setKeyboardType_:(id)value
{
	[[self searchBar] setKeyboardType:[TiUtils intValue:value]];
}

-(void)setPrompt_:(id)value
{
	[[self searchBar] setPrompt:[TiUtils stringValue:value]];
}

-(void)setAutocorrect_:(id)value
{
	[[self searchBar] setAutocorrectionType:[TiUtils boolValue:value] ? UITextAutocorrectionTypeYes : UITextAutocorrectionTypeNo];
}

-(void)setAutocapitalization_:(id)value
{
	[[self searchBar] setAutocapitalizationType:[TiUtils intValue:value]];
}

//-(void)setTintColor_:(id)color
//{
//    [super setTintColor_:color];
//    TiColor *ticolor = [TiUtils colorValue:color];
//    UIColor* theColor = [ticolor _color];
//    [[self searchBar] setTintColor:theColor];
//}

-(void)setBarColor_:(id)value
{
	TiColor * newBarColor = [TiUtils colorValue:value];
	UISearchBar *search = [self searchBar];
	
	[search setBarStyle:[TiUtils barStyleForColor:newBarColor]];
	[search setTranslucent:[TiUtils barTranslucencyForColor:newBarColor]];
	UIColor* theColor = [TiUtils barColorForColor:newBarColor];
	[search setBarTintColor:theColor];
}

-(void)setTranslucent_:(id)value
{
	[[self searchBar] setTranslucent:[TiUtils boolValue:value]];
}

-(void)setBarStyle_:(id)value
{
	[[self searchBar] setBarStyle:[TiUtils intValue:value def:[self searchBar].barStyle]];
}


-(void)setSearchBarStyle_:(id)value
{
    [[self searchBar] setSearchBarStyle:[TiUtils intValue:value def:[self searchBar].searchBarStyle]];
}


-(UIImage *)imageWithColor:(UIColor *)color andHeight:(int)height {
    CGRect rect = CGRectMake(0, 0, height, height);
    UIGraphicsBeginImageContextWithOptions(rect.size, NO, 0);
    [color setFill];
    UIRectFill(rect);
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return image;
}

-(void)setBackgroundImage_:(id)arg
{
    UIImage *image = [self loadImage:arg];
    UISearchBar* searchBar = [self searchBar];
    // reset the image to nil so we can check if the next statement sets it
    [searchBar setBackgroundImage:nil];
    
    // try to set the image with UIBarMetricsDefaultPrompt barMetrics
    //
    // Checking for the `prompt` property is not reliable, even if it's not set, the height
    // of the searchbar determines wheather the barMetrics is `DefaultPrompt` or just `Default`
    [searchBar setBackgroundImage:image forBarPosition:UIBarPositionAny barMetrics:UIBarMetricsDefaultPrompt];
    
    // check that the image has been set, otherwise try the other barMetrics
    if([searchBar backgroundImage] == nil) {
        [searchBar setBackgroundImage:image forBarPosition:UIBarPositionAny barMetrics:UIBarMetricsDefault];
    }
//    self.backgroundImage = arg;
}
//
//-(void) setBackgroundColor_:(id)color
//{
//    [super setBackgroundColor_:color];
//    
//	UISearchBar *searchBar = [self searchBar];
//    UIImage* clearImg = [self imageWithColor:[UIColor clearColor] andHeight:32.0f];
//    [searchBar setBackgroundImage:clearImg];
//    [searchBar setSearchFieldBackgroundImage:clearImg forState:UIControlStateNormal];
//    [searchBar setSearchFieldBackgroundImage:clearImg forState:UIControlStateHighlighted];
//    [searchBar setSearchFieldBackgroundImage:clearImg forState:UIControlStateSelected];
//    [searchBar setBackgroundColor:[UIColor clearColor]];
//}


-(void)setHideNavBarWithSearch_:(id)yn
{
    hideNavBarWithSearch = [TiUtils boolValue:yn def:YES];
    if (searchController) {
        searchController.preventHiddingNavBar = !hideNavBarWithSearch;
    }
}

#pragma mark Delegate 
- (BOOL)searchBarShouldBeginEditing:(UISearchBar *)searchBar
{
    if (delegate!=nil && [delegate respondsToSelector:@selector(searchBarShouldBeginEditing:)]) {
        [delegate searchBarShouldBeginEditing:searchBar];
    }
    return YES;
}

// called when text starts editing
- (void)searchBarTextDidBeginEditing:(UISearchBar *)searchBar                    
{
	NSString * text = [searchBar text];
	[self.proxy replaceValue:text forKey:@"value" notification:NO];
	
	//No need to setValue, because it's already been set.
	if ([[self viewProxy] _hasListeners:@"focus"  checkParent:NO])
	{
		[self.proxy fireEvent:@"focus" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"] propagate:NO checkForListener:NO];
	}
	
	if (delegate!=nil && [delegate respondsToSelector:@selector(searchBarTextDidBeginEditing:)])
	{
		[delegate searchBarTextDidBeginEditing:searchBar];
	}
}

// called when text ends editing
- (void)searchBarTextDidEndEditing:(UISearchBar *)searchBar                       
{
	NSString * text = [searchBar text];
	[self.proxy replaceValue:text forKey:@"value" notification:NO];
	
	//No need to setValue, because it's already been set.
	if ([[self viewProxy] _hasListeners:@"blur"  checkParent:NO])
	{
		[self.proxy fireEvent:@"blur" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"] propagate:NO checkForListener:NO];
	}

	if (delegate!=nil && [delegate respondsToSelector:@selector(searchBarTextDidEndEditing:)])
	{
		[delegate searchBarTextDidEndEditing:searchBar];
	}
}

// called when text changes (including clear)
- (void)searchBar:(UISearchBar *)searchBar textDidChange:(NSString *)searchText   
{
	NSString * text = [searchBar text];
	[self.proxy replaceValue:text forKey:@"value" notification:NO];
	
	//No need to setValue, because it's already been set.
    if ([(TiViewProxy*)self.proxy _hasListeners:@"change" checkParent:NO])
	{
		[self.proxy fireEvent:@"change" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"] propagate:NO checkForListener:NO];
	}

	if (delegate!=nil && [delegate respondsToSelector:@selector(searchBar:textDidChange:)])
	{
		[delegate searchBar:searchBar textDidChange:searchText];
	}
}

// called when keyboard search button pressed
- (void)searchBarSearchButtonClicked:(UISearchBar *)searchBar                     
{
	NSString * text = [searchBar text];
	[self.proxy replaceValue:text forKey:@"value" notification:NO];
	
	//No need to setValue, because it's already been set.
    if ([(TiViewProxy*)self.proxy _hasListeners:@"return" checkParent:NO])
	{
		[self.proxy fireEvent:@"return" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"] propagate:NO checkForListener:NO];
	}

	if (delegate!=nil && [delegate respondsToSelector:@selector(searchBarSearchButtonClicked:)])
	{
		[delegate searchBarSearchButtonClicked:searchBar];
	}
}

// called when bookmark button pressed
- (void)searchBarBookmarkButtonClicked:(UISearchBar *)searchBar                   
{	
	NSString * text = @"";
	
	if ([searchBar text]!=nil)
	{
		text = [searchBar text];
	}
	
	[self.proxy replaceValue:text forKey:@"value" notification:NO];
	
	if ([self.proxy _hasListeners:@"bookmark"])
	{
		[self.proxy fireEvent:@"bookmark" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"]];
	}
	
	if (delegate!=nil && [delegate respondsToSelector:@selector(searchBarBookmarkButtonClicked:)])
	{
		[delegate searchBarBookmarkButtonClicked:searchBar];
	}
	
}

// called when cancel button pressed
- (void)searchBarCancelButtonClicked:(UISearchBar *) searchBar                    
{
	NSString * text = [searchBar text];
	[self.proxy replaceValue:text forKey:@"value" notification:NO];
	
	if ([self.proxy _hasListeners:@"cancel"])
	{
		[self.proxy fireEvent:@"cancel" withObject:[NSDictionary dictionaryWithObject:text forKey:@"value"]];
	}
	
	if (delegate!=nil && [delegate respondsToSelector:@selector(searchBarCancelButtonClicked:)])
	{
		[delegate searchBarCancelButtonClicked:searchBar];
	}
}


@end

#endif
