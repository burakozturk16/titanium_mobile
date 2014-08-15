/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListView.h"
#import "TiUIListSectionProxy.h"
#import "TiUIListItem.h"
#import "TiUIListItemProxy.h"
#import "TiUILabelProxy.h"
#import "TiUISearchBarProxy.h"
#import "ImageLoader.h"
#ifdef USE_TI_UIREFRESHCONTROL
#import "TiUIRefreshControlProxy.h"
#endif
#import "TiTableView.h"
#import "TiUIHelper.h"

#define GROUPED_MARGIN_WIDTH 18.0

@interface WrapperViewProxy: TiViewProxy
@end

@implementation WrapperViewProxy

- (id)initWithVerticalLayout:(BOOL)vertical
{
    self = [super init];
    if (self) {
        self.canBeResizedByFrame = YES;
        LayoutConstraint* viewLayout = [self layoutProperties];
        viewLayout->width = TiDimensionAutoFill;
        viewLayout->height = TiDimensionAutoSize;
        if (TiDimensionIsUndefined(viewLayout->top))
        {
            viewLayout->top = TiDimensionDip(0);
        }
        if (vertical) {
            viewLayout->layoutStyle = TiLayoutRuleVertical;
        }
    }
    return self;
}
- (void)dealloc
{
    [super dealloc];
}
@end

@interface TiUIView(eventHandler);
-(void)handleListenerRemovedWithEvent:(NSString *)event;
-(void)handleListenerAddedWithEvent:(NSString *)event;
@end


@interface TiUIListView ()
@property (nonatomic, readonly) TiUIListViewProxy *listViewProxy;
@property (nonatomic,copy,readwrite) NSString * searchString;
@end


@implementation TiUIListView {
    TiTableView *_tableView;
    NSDictionary *_templates;
    id _defaultItemTemplate;
    BOOL hideOnSearch;
    BOOL searchViewAnimating;

    TiDimension _rowHeight;
    TiDimension _minRowHeight;
    TiDimension _maxRowHeight;
    TiViewProxy *_headerViewProxy;
    TiViewProxy *_searchWrapper;
    TiViewProxy *_headerWrapper;
    TiViewProxy *_footerViewProxy;
    TiViewProxy *_pullViewProxy;
#ifdef USE_TI_UIREFRESHCONTROL
    TiUIRefreshControlProxy* _refreshControlProxy;
#endif

    TiUISearchBarProxy *searchViewProxy;
    UITableViewController *tableController;

    NSMutableArray * sectionTitles;
    NSMutableArray * sectionIndices;
    NSMutableArray * filteredTitles;
    NSMutableArray * filteredIndices;

    UIView *_pullViewWrapper;
    CGFloat pullThreshhold;
    BOOL _pullViewVisible;

    BOOL pullActive;
//    CGPoint tapPoint;
    BOOL editing;
    BOOL pruneSections;

    BOOL caseInsensitiveSearch;
    NSString* _searchString;
    BOOL searchActive;
	BOOL searchHidden;
    BOOL keepSectionsInSearch;
    NSMutableArray* _searchResults;
    UIEdgeInsets _defaultSeparatorInsets;
    
    NSMutableDictionary* _measureProxies;
    BOOL _scrollSuspendImageLoading;
    BOOL _scrollHidesKeyboard;
    BOOL hasOnDisplayCell;
}

static NSDictionary* replaceKeysForRow;
-(NSDictionary *)replaceKeysForRow
{
	if (replaceKeysForRow == nil)
	{
		replaceKeysForRow = [@{@"rowHeight":@"height"} retain];
	}
	return replaceKeysForRow;
}

-(NSString*)replacedKeyForKey:(NSString*)key
{
    NSString* result = [[self replaceKeysForRow] objectForKey:key];
    return result?result:key;
}

- (id)init
{
    self = [super init];
    if (self) {
        _defaultItemTemplate = [[NSNumber numberWithUnsignedInteger:UITableViewCellStyleDefault] retain];
        allowsSelection = YES;
        _defaultSeparatorInsets = UIEdgeInsetsZero;
        _scrollSuspendImageLoading = NO;
        _scrollHidesKeyboard = YES;
        hideOnSearch = NO;
        searchViewAnimating = NO;
    }
    return self;
}

- (void)dealloc
{
    _tableView.delegate = nil;
    _tableView.dataSource = nil;
    RELEASE_TO_NIL(_tableView)
    RELEASE_TO_NIL(_templates)
    RELEASE_TO_NIL(_defaultItemTemplate)
    RELEASE_TO_NIL(_searchString)
    RELEASE_TO_NIL(_searchResults)
    RELEASE_TO_NIL(_pullViewWrapper)
    RELEASE_TO_NIL(_searchWrapper)
    RELEASE_TO_NIL(_headerWrapper)
    if (_pullViewProxy)
    {
		[_pullViewProxy setProxyObserver:nil];
        [_pullViewProxy detachView];
        RELEASE_TO_NIL(_pullViewProxy)
    }
    if (_headerViewProxy)
    {
		[_headerViewProxy setProxyObserver:nil];
        [_headerViewProxy detachView];
        RELEASE_TO_NIL(_headerViewProxy)
    }
    if (_footerViewProxy)
    {
		[_footerViewProxy setProxyObserver:nil];
        [_footerViewProxy detachView];
        RELEASE_TO_NIL(_footerViewProxy)
    }
    if (searchViewProxy)
    {
		[searchViewProxy setProxyObserver:nil];
        [searchViewProxy detachView];
        RELEASE_TO_NIL(searchViewProxy)
    }
    RELEASE_TO_NIL(tableController)
    RELEASE_TO_NIL(sectionTitles)
    RELEASE_TO_NIL(sectionIndices)
    RELEASE_TO_NIL(filteredTitles)
    RELEASE_TO_NIL(filteredIndices)
    RELEASE_TO_NIL(_measureProxies)
#ifdef USE_TI_UIREFRESHCONTROL
    RELEASE_TO_NIL(_refreshControlProxy)
#endif
    [super dealloc];
}

-(TiViewProxy*)initWrapperProxyWithVerticalLayout:(BOOL)vertical
{
    WrapperViewProxy* theProxy = [[WrapperViewProxy alloc] initWithVerticalLayout:vertical];
    [theProxy setParent:(TiParentingProxy*)self.proxy];
    return [theProxy autorelease];
}

-(TiViewProxy*)initWrapperProxy
{
    return [self initWrapperProxyWithVerticalLayout:NO];
}

-(void)setHeaderFooter:(TiViewProxy*)theProxy isHeader:(BOOL)header
{
    [theProxy setProxyObserver:self];
    if (header) {
        [self.tableView setTableHeaderView:[theProxy getAndPrepareViewForOpening:CGRectZero]];
    } else {
        [self.tableView setTableFooterView:[theProxy getAndPrepareViewForOpening:CGRectZero]];
    }
}

-(void)configureFooter
{
    if (_footerViewProxy == nil) {
        _footerViewProxy = [[self initWrapperProxy] retain];
        [self setHeaderFooter:_footerViewProxy isHeader:NO];
    }
}

-(TiViewProxy*)getOrCreateFooterHolder
{
    if (_footerViewProxy == nil) {
        _footerViewProxy = [[self initWrapperProxy] retain];
        [self setHeaderFooter:_footerViewProxy isHeader:NO];
    }
    return _footerViewProxy;
}

-(TiViewProxy*)getOrCreateHeaderHolder
{
    if (_headerViewProxy == nil) {
        _headerViewProxy = [[self initWrapperProxyWithVerticalLayout:YES] retain];
        
        _searchWrapper = [[self initWrapperProxy] retain];
        _headerWrapper = [[self initWrapperProxy] retain];
        
        [_headerViewProxy add:@[_searchWrapper, _headerWrapper]];
        
        [self setHeaderFooter:_headerViewProxy isHeader:YES];
    }
    return _headerViewProxy;
}
-(TiViewProxy*)getOrCreateSearchWrapper
{
    if (_searchWrapper == nil) {
        [self getOrCreateHeaderHolder];
    }
    return _searchWrapper;
}
-(TiViewProxy*)getOrCreateHeaderWrapper
{
    if (_headerWrapper == nil) {
        [self getOrCreateHeaderHolder];
    }
    return _headerWrapper;
}

- (TiTableView *)tableView
{
    if (_tableView == nil) {
        UITableViewStyle style = UITableViewStylePlain;
        if (![TiUtils isIOS7OrGreater]) {
            style = [TiUtils intValue:[self.proxy valueForKey:@"style"] def:style];
        }

        _tableView = [[TiTableView alloc] initWithFrame:self.bounds style:style];
        _tableView.autoresizingMask = UIViewAutoresizingFlexibleWidth|UIViewAutoresizingFlexibleHeight;
        _tableView.delegate = self;
        _tableView.dataSource = self;
        _tableView.touchDelegate = self;
        
        if (TiDimensionIsDip(_rowHeight)) {
            [_tableView setRowHeight:_rowHeight.value];
        }
        id backgroundColor = [self.proxy valueForKey:@"backgroundColor"];
        BOOL doSetBackground = YES;
        if ([TiUtils isIOS6OrGreater] && (style == UITableViewStyleGrouped)) {
            doSetBackground = (backgroundColor != nil);
        }
        if (doSetBackground) {
            [[self class] setBackgroundColor:[UIColor clearColor] onTable:_tableView];
        }
        UITapGestureRecognizer *tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
        tapGestureRecognizer.delegate = self;
        [_tableView addGestureRecognizer:tapGestureRecognizer];
        [tapGestureRecognizer release];
        if ([TiUtils isIOS7OrGreater]) {
            _defaultSeparatorInsets = [_tableView separatorInset];
        }
    }
    if ([_tableView superview] != self) {
        [self addSubview:_tableView];
    }
    return _tableView;
}

-(void)frameSizeChanged:(CGRect)frame bounds:(CGRect)bounds
{
//        if (searchHidden)
//        {
//            if (searchViewProxy!=nil)
//            {
//                [self hideSearchScreen:nil];
//            }
//        }
//    
    if (!searchViewAnimating && ![[self searchController] isActive]) {
        [searchViewProxy ensureSearchBarHeirarchy];
        if (_searchWrapper != nil) {
            CGFloat rowWidth = [self computeRowWidth:_tableView];
            if (rowWidth > 0) {
                CGFloat right = _tableView.bounds.size.width - rowWidth;
                [_searchWrapper layoutProperties]->right = TiDimensionDip(right);
            }
        }
    }
    [super frameSizeChanged:frame bounds:bounds];
    
    if (_headerViewProxy != nil) {
        [_headerViewProxy parentSizeWillChange];
    }
    if (_footerViewProxy != nil) {
        [_footerViewProxy parentSizeWillChange];
    }
    if (_pullViewWrapper != nil) {
        _pullViewWrapper.frame = CGRectMake(0.0f, 0.0f - bounds.size.height, bounds.size.width, bounds.size.height);
        [_pullViewProxy parentSizeWillChange];
    }
}

- (id)accessibilityElement
{
	return self.tableView;
}

- (TiUIListViewProxy *)listViewProxy
{
	return (TiUIListViewProxy *)self.proxy;
}

- (void)deselectAll:(BOOL)animated
{
	if (_tableView != nil) {
		[_tableView.indexPathsForSelectedRows enumerateObjectsUsingBlock:^(NSIndexPath *indexPath, NSUInteger idx, BOOL *stop) {
			[_tableView deselectRowAtIndexPath:indexPath animated:animated];
		}];
	}
}

- (void) updateIndicesForVisibleRows
{
    if (_tableView == nil || [self isSearchActive]) {
        return;
    }
    
    NSArray* visibleRows = [_tableView indexPathsForVisibleRows];
    [visibleRows enumerateObjectsUsingBlock:^(NSIndexPath *vIndexPath, NSUInteger idx, BOOL *stop) {
        UITableViewCell* theCell = [_tableView cellForRowAtIndexPath:vIndexPath];
        if ([theCell isKindOfClass:[TiUIListItem class]]) {
            ((TiUIListItem*)theCell).proxy.indexPath = vIndexPath;
        }
    }];
}

-(void)proxyDidRelayout:(id)sender
{
//    TiThreadPerformOnMainThread(^{
        if (sender == _headerViewProxy) {
            UIView* headerView = [[self tableView] tableHeaderView];
            [headerView setFrame:[headerView bounds]];
            [[self tableView] setTableHeaderView:headerView];
        } else if (sender == _footerViewProxy) {
            UIView *footerView = [[self tableView] tableFooterView];
            [footerView setFrame:[footerView bounds]];
            [[self tableView] setTableFooterView:footerView];
        } else if (sender == _pullViewProxy) {
            pullThreshhold = -[self tableView].contentInset.top + ([_pullViewProxy view].frame.origin.y - _pullViewWrapper.bounds.size.height);
        }
//    },YES);
}

-(void)setContentInsets_:(id)value withObject:(id)props
{
    UIEdgeInsets insets = [TiUtils contentInsets:value];
    BOOL animated = [TiUtils boolValue:@"animated" properties:props def:NO];
    void (^setInset)(void) = ^{
        [_tableView setContentInset:insets];
    };
    if (animated) {
        double duration = [TiUtils doubleValue:@"duration" properties:props def:300]/1000;
        [UIView animateWithDuration:duration animations:setInset];
    }
    else {
        setInset();
    }
}

- (void)setTemplates_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,NSDictionary);
	NSMutableDictionary *templates = [[NSMutableDictionary alloc] initWithCapacity:[args count]];
	NSMutableDictionary *measureProxies = [[NSMutableDictionary alloc] initWithCapacity:[args count]];
	[(NSDictionary *)args enumerateKeysAndObjectsUsingBlock:^(NSString *key, id obj, BOOL *stop) {
		TiProxyTemplate *template = [TiProxyTemplate templateFromViewTemplate:obj];
		if (template != nil) {
			[templates setObject:template forKey:key];
            
            //create fake proxy for height computation
            id<TiEvaluator> context = self.listViewProxy.executionContext;
            if (context == nil) {
                context = self.listViewProxy.pageContext;
            }
            TiUIListItemProxy *cellProxy = [[TiUIListItemProxy alloc] initWithListViewProxy:self.listViewProxy inContext:context];
            [cellProxy unarchiveFromTemplate:template withEvents:NO];
            [cellProxy bindings];
            [measureProxies setObject:cellProxy forKey:key];
            [cellProxy release];
		}
	}];
    
	[_templates release];
	_templates = [templates copy];
	[templates release];
    
    [_measureProxies release];
	_measureProxies = [measureProxies copy];
	[measureProxies release];
    
	if (_tableView != nil) {
		[_tableView reloadData];
	}
}

-(TiViewProxy*)sectionViewProxy:(NSInteger)section forLocation:(NSString*)location
{
    TiUIListSectionProxy *proxy = [self.listViewProxy sectionForIndex:section];
    return [proxy sectionViewForLocation:location inListView:self];
}

-(UIView*)sectionView:(NSInteger)section forLocation:(NSString*)location section:(TiUIListSectionProxy**)sectionResult
{
    TiViewProxy* viewproxy = [self sectionViewProxy:section forLocation:location];
    if (viewproxy!=nil) {
        return [viewproxy getAndPrepareViewForOpening:self.tableView.bounds];
    }
    return nil;
}

#pragma mark Searchbar-related IBActions


- (IBAction) showSearchScreen: (id) sender
{
	[_tableView setContentOffset:CGPointZero animated:YES];
}

-(void)hideSearchScreen:(id)sender animated:(BOOL)animated
{
    if (!searchHidden || ![(TiViewProxy*)self.proxy viewReady]) {
        return;
    }
    
	// check to make sure we're not in the middle of a layout, in which case we
	// want to try later or we'll get weird drawing animation issues
	if (_tableView.bounds.size.width==0)
	{
		[self performSelector:@selector(hideSearchScreen:) withObject:sender afterDelay:0.1];
		return;
	}
    
    if ([[searchViewProxy view] isFirstResponder]) {
        [[searchViewProxy view] resignFirstResponder];
        [self makeRootViewFirstResponder];
    }
    
    // This logic here is contingent on search controller deactivation
    // (-[TiUITableView searchDisplayControllerDidEndSearch:]) triggering a hide;
    // doing this ensures that:
    //
    // * The hide when the search controller was active is animated
    // * The animation only occurs once
    
    if ([[self searchController] isActive]) {
        [[self searchController] setActive:NO animated:YES];
        searchActive = NO;
        return;
    }
    
    searchActive = NO;
    if (![(TiViewProxy*)self.proxy viewReady]) {
        return;
    }
    NSArray* visibleRows = [_tableView indexPathsForVisibleRows];
    
    // We only want to scroll if the following conditions are met:
    // 1. The top row of the first section (and hence searchbar) are visible (or there are no rows)
    // 2. The current offset is smaller than the new offset (otherwise the search is already hidden)
    
    if (searchHidden) {
        CGPoint offset = CGPointMake(0,MAX(TI_NAVBAR_HEIGHT, searchViewProxy.view.frame.size.height));
        if (([visibleRows count] == 0) ||
            ([_tableView contentOffset].y < offset.y && [visibleRows containsObject:[NSIndexPath indexPathForRow:0 inSection:0]]))
        {
            [_tableView setContentOffset:offset animated:animated];
        }
    }
}
-(void)hideSearchScreen:(id)sender {
    [self hideSearchScreen:sender animated:YES];
}

-(void)scrollToTop:(NSInteger)top animated:(BOOL)animated
{
	[_tableView setContentOffset:CGPointMake(0,top - _tableView.contentInset.top) animated:animated];
}


-(void)scrollToBottom:(NSInteger)bottom animated:(BOOL)animated
{
    if (_tableView.contentSize.height > _tableView.frame.size.height)
    {
        CGPoint offset = CGPointMake(0, _tableView.contentSize.height - _tableView.frame.size.height - bottom);
        [_tableView setContentOffset:offset animated:animated];
    }
}

-(void)closePullView:(NSNumber*)anim
{
    BOOL animated = YES;
	if (anim != nil)
		animated = [anim boolValue];
    
    if (IOS_7) {
        //we have to delay it on ios7 :s
        double delayInSeconds = 0.01;
        dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, (int64_t)(delayInSeconds * NSEC_PER_SEC));
        dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
            [_tableView setContentOffset:CGPointMake(0,-_tableView.contentInset.top) animated:animated];
        });
    }
    else {
        [_tableView setContentOffset:CGPointMake(0,-_tableView.contentInset.top) animated:animated];
    }
    
}

-(void)showPullView:(NSNumber*)anim
{
    if (!_pullViewProxy) {
        _pullViewVisible = YES;
        return;
    }
    BOOL animated = YES;
	if (anim != nil)
		animated = [anim boolValue];
	[_tableView setContentOffset:CGPointMake(0,pullThreshhold) animated:animated];
}

-(BOOL)shouldHighlightCurrentListItem {
    return [_tableView shouldHighlightCurrentListItem];
}

#pragma mark - Helper Methods

-(CGFloat)computeRowWidth:(UITableView*)tableView
{
    if (tableView == nil) {
        return 0;
    }
    CGFloat rowWidth = tableView.bounds.size.width;
    
    // Apple does not provide a good way to get information about the index sidebar size
    // in the event that it exists - it silently resizes row content which is "flexible width"
    // but this is not useful for us. This is a problem when we have Ti.UI.SIZE/FILL behavior
    // on row contents, which rely on the height of the row to be accurately precomputed.
    //
    // The following is unreliable since it uses a private API name, but one which has existed
    // since iOS 3.0. The alternative is to grab a specific subview of the tableview itself,
    // which is more fragile.
    if ((sectionTitles == nil) || (tableView != _tableView) ) {
        return rowWidth;
    }
    NSArray* subviews = [tableView subviews];
    if ([subviews count] > 0) {
        // Obfuscate private class name
        Class indexview = NSClassFromString([@"UITableView" stringByAppendingString:@"Index"]);
        for (UIView* view in subviews) {
            if ([view isKindOfClass:indexview]) {
                rowWidth -= [view frame].size.width;
            }
        }
    }
    
    return floorf(rowWidth);
}

-(id)valueWithKey:(NSString*)key atIndexPath:(NSIndexPath*)indexPath
{
    NSDictionary *item = [[self.listViewProxy sectionForIndex:indexPath.section] itemAtIndex:indexPath.row];
    id propertiesValue = [item objectForKey:@"properties"];
    NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
    NSString* replaceKey = [self replacedKeyForKey:key];
    id theValue = [properties objectForKey:replaceKey];
    if (theValue == nil) {
        id templateId = [item objectForKey:@"template"];
        if (templateId == nil) {
            templateId = _defaultItemTemplate;
        }
        if (![templateId isKindOfClass:[NSNumber class]]) {
            TiProxyTemplate *template = [_templates objectForKey:templateId];
            theValue = [template.properties objectForKey:replaceKey];
        }
        if (theValue == nil) {
            theValue = [self.proxy valueForKey:key];
        }
    }
    
    return theValue;
}


-(void)buildResultsForSearchText
{
    searchActive = ([self.searchString length] > 0);
    RELEASE_TO_NIL(filteredIndices);
    RELEASE_TO_NIL(filteredTitles);
    if (searchActive) {
        BOOL hasResults = NO;
        //Initialize
        if(_searchResults == nil) {
            _searchResults = [[NSMutableArray alloc] init];
        }
        //Clear Out
        [_searchResults removeAllObjects];
        
        //Search Options
        NSStringCompareOptions searchOpts = (caseInsensitiveSearch ? NSCaseInsensitiveSearch : 0);
        
        NSUInteger maxSection = [[self.listViewProxy sectionCount] unsignedIntegerValue];
        NSMutableArray* singleSection = keepSectionsInSearch ? nil : [[NSMutableArray alloc] init];
        for (int i = 0; i < maxSection; i++) {
            NSMutableArray* thisSection = keepSectionsInSearch ? [[NSMutableArray alloc] init] : nil;
            NSUInteger maxItems = [[self.listViewProxy sectionForIndex:i] itemCount];
            for (int j = 0; j < maxItems; j++) {
                NSIndexPath* thePath = [NSIndexPath indexPathForRow:j inSection:i];
                id theValue = [self valueWithKey:@"searchableText" atIndexPath:thePath];
                if (theValue!=nil && [[TiUtils stringValue:theValue] rangeOfString:self.searchString options:searchOpts].location != NSNotFound) {
                    (thisSection != nil) ? [thisSection addObject:thePath] : [singleSection addObject:thePath];
                    hasResults = YES;
                }
            }
            if (thisSection != nil) {
                if ([thisSection count] > 0) {
                    [_searchResults addObject:thisSection];
                    
                    if (sectionTitles != nil && sectionIndices != nil) {
                        NSNumber* theIndex = [NSNumber numberWithInt:i];
                        if ([sectionIndices containsObject:theIndex]) {
                            id theTitle = [sectionTitles objectAtIndex:[sectionIndices indexOfObject:theIndex]];
                            if (filteredTitles == nil) {
                                filteredTitles = [[NSMutableArray alloc] init];
                            }
                            if (filteredIndices == nil) {
                                filteredIndices = [[NSMutableArray alloc] init];
                            }
                            [filteredTitles addObject:theTitle];
                            [filteredIndices addObject:[NSNumber numberWithInt:([_searchResults count] -1)]];
                        }
                    }
                }
                [thisSection release];
            }
        }
        if (singleSection != nil) {
            if ([singleSection count] > 0) {
                [_searchResults addObject:singleSection];
            }
            [singleSection release];
        }
        if (!hasResults) {
            if ([(TiViewProxy*)self.proxy _hasListeners:@"noresults" checkParent:NO]) {
                [self.proxy fireEvent:@"noresults" withObject:nil propagate:NO reportSuccess:NO errorCode:0 message:nil];
            }
        }
        
    } else {
        RELEASE_TO_NIL(_searchResults);
    }
}

-(BOOL) isSearchActive
{
    return searchActive || [[self searchController] isActive];
}

- (void)updateSearchResults:(id)unused
{
    if (searchActive) {
        [self buildResultsForSearchText];
    }
    if ([self isSearchActive]) {
        [[[self searchController] searchResultsTableView] reloadData];
    } else {
        [_tableView reloadData];
    }
}

-(NSIndexPath*)pathForSearchPath:(NSIndexPath*)indexPath
{
    if (_searchResults != nil) {
        NSArray* sectionResults = [_searchResults objectAtIndex:indexPath.section];
        return [sectionResults objectAtIndex:indexPath.row];
    }
    return indexPath;
}

-(NSInteger)sectionForSearchSection:(NSInteger)section
{
    if (_searchResults != nil) {
        NSArray* sectionResults = [_searchResults objectAtIndex:section];
        NSIndexPath* thePath = [sectionResults objectAtIndex:0];
        return thePath.section;
    }
    return section;
}

#pragma mark - Public API

-(void)setSeparatorInsets_:(id)arg
{
    if ([TiUtils isIOS7OrGreater]) {
        [self tableView];
        if ([arg isKindOfClass:[NSDictionary class]]) {
            CGFloat left = [TiUtils floatValue:@"left" properties:arg def:_defaultSeparatorInsets.left];
            CGFloat right = [TiUtils floatValue:@"right" properties:arg def:_defaultSeparatorInsets.right];
            [_tableView setSeparatorInset:UIEdgeInsetsMake(0, left, 0, right)];
        } else {
            [_tableView setSeparatorInset:_defaultSeparatorInsets];
        }
        if (![self isSearchActive]) {
            [_tableView setNeedsDisplay];
        }
    }
}

-(void)setPruneSectionsOnEdit_:(id)args
{
    pruneSections = [TiUtils boolValue:args def:NO];
}

-(void)setScrollingEnabled_:(id)args
{
    UITableView *table = [self tableView];
    [table setScrollEnabled:[TiUtils boolValue:args def:YES]];
}

-(void)setSeparatorStyle_:(id)arg
{
    [[self tableView] setSeparatorStyle:[TiUtils intValue:arg]];
}

-(void)setSeparatorColor_:(id)arg
{
    TiColor *color = [TiUtils colorValue:arg];
    [[self tableView] setSeparatorColor:[color _color]];
}

- (void)setDefaultItemTemplate_:(id)args
{
	if (![args isKindOfClass:[NSString class]] && ![args isKindOfClass:[NSNumber class]]) {
		ENSURE_TYPE_OR_NIL(args,NSString);
	}
	[_defaultItemTemplate release];
	_defaultItemTemplate = [args copy];
	if (_tableView != nil) {
		[_tableView reloadData];
	}	
}

- (void)setRowHeight_:(id)height
{
	_rowHeight = [TiUtils dimensionValue:height];
	if (TiDimensionIsDip(_rowHeight)) {
		[_tableView setRowHeight:_rowHeight.value];
	}
}

- (void)setMinRowHeight_:(id)height
{
	_minRowHeight = [TiUtils dimensionValue:height];
}

- (void)setMaxRowHeight_:(id)height
{
	_maxRowHeight = [TiUtils dimensionValue:height];
}

-(void)onCreateCustomBackground
{
    if (_tableView != nil) {
		[[self class] setBackgroundColor:[UIColor clearColor] onTable:_tableView];
	}
}

- (void)setHeaderTitle_:(id)args
{
    [_headerWrapper removeAllChildren:nil];
    TiViewProxy *theProxy = [[self class] titleViewForText:[TiUtils stringValue:args] inTable:[self tableView] footer:NO];
    [[self getOrCreateSearchWrapper] add:theProxy];
}

- (void)setFooterTitle_:(id)args
{
    if (IS_NULL_OR_NIL(args)) {
        [_footerViewProxy setProxyObserver:nil];
        [_footerViewProxy windowWillClose];
        [self.tableView setTableFooterView:nil];
        [_footerViewProxy windowDidClose];
        RELEASE_TO_NIL(_footerViewProxy);
    } else {
        [self configureFooter];
        [_footerViewProxy removeAllChildren:nil];
        TiViewProxy *theProxy = [[self class] titleViewForText:[TiUtils stringValue:args] inTable:[self tableView] footer:YES];
        [[self getOrCreateFooterHolder] add:theProxy];
    }
}

-(void)setHeaderView_:(id)args
{
    TiViewProxy* viewproxy = (TiViewProxy*)[(TiUIListViewProxy*)self.proxy createChildFromObject:args];
    [_headerWrapper removeAllChildren:nil];
    if (viewproxy!=nil) {
        [[self getOrCreateSearchWrapper] add:viewproxy];
    }
}

-(void)setFooterView_:(id)args
{
    TiViewProxy* viewproxy = (TiViewProxy*)[(TiUIListViewProxy*)self.proxy createChildFromObject:args];
    [_footerViewProxy removeAllChildren:nil];
    if (viewproxy!=nil) {
        [[self getOrCreateFooterHolder] add:viewproxy];
    }
}

-(void)setRefreshControl_:(id)args
{
#ifdef USE_TI_UIREFRESHCONTROL
    ENSURE_SINGLE_ARG_OR_NIL(args,TiUIRefreshControlProxy);
    [[_refreshControlProxy control] removeFromSuperview];
    RELEASE_TO_NIL(_refreshControlProxy);
    [[self proxy] replaceValue:args forKey:@"refreshControl" notification:NO];
    if (args != nil) {
        _refreshControlProxy = [args retain];
        [[self tableView] addSubview:[_refreshControlProxy control]];
    }
#endif
}

-(void)setPullView_:(id)args
{
    TiViewProxy* viewproxy = (TiViewProxy*)[(TiUIListViewProxy*)self.proxy createChildFromObject:args];
    if (viewproxy == nil) {
        [_pullViewProxy setProxyObserver:nil];
        [_pullViewProxy windowWillClose];
        [_pullViewWrapper removeFromSuperview];
        [_pullViewProxy windowDidClose];
        [self.proxy forgetProxy:_pullViewProxy];
        RELEASE_TO_NIL(_pullViewWrapper);
        RELEASE_TO_NIL(_pullViewProxy);
    } else {
        if ([self tableView].bounds.size.width==0)
        {
            [self performSelector:@selector(setPullView_:) withObject:args afterDelay:0.1];
            return;
        }
        if (_pullViewProxy != nil) {
            [_pullViewProxy setProxyObserver:nil];
            [_pullViewProxy windowWillClose];
            [_pullViewProxy windowDidClose];
            RELEASE_TO_NIL(_pullViewProxy);
        }
        if (_pullViewWrapper == nil) {
            _pullViewWrapper = [[UIView alloc] init];
            _pullViewWrapper.backgroundColor = [UIColor clearColor];
            [_tableView addSubview:_pullViewWrapper];
        }
        CGSize refSize = _tableView.bounds.size;
        [_pullViewWrapper setFrame:CGRectMake(0.0, 0.0 - refSize.height, refSize.width, refSize.height)];
        _pullViewProxy = [viewproxy retain];
        LayoutConstraint *viewLayout = [_pullViewProxy layoutProperties];
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
        
        [_pullViewProxy setProxyObserver:self];
        [_pullViewWrapper addSubview:[_pullViewProxy getAndPrepareViewForOpening:_pullViewWrapper.bounds]];
        if (_pullViewVisible) {
            _pullViewVisible = NO;
            [self showPullView:@(NO)];
        }
    }
    
}

-(void)setKeepSectionsInSearch_:(id)args
{
    if (searchViewProxy == nil) {
        keepSectionsInSearch = [TiUtils boolValue:args def:NO];
        if (searchActive) {
            [self buildResultsForSearchText];
            [_tableView reloadData];
        }
    } else {
        keepSectionsInSearch = NO;
    }
}

- (void)setScrollIndicatorStyle_:(id)value
{
	[self.tableView setIndicatorStyle:[TiUtils intValue:value def:UIScrollViewIndicatorStyleDefault]];
}

- (void)setWillScrollOnStatusTap_:(id)value
{
	[self.tableView setScrollsToTop:[TiUtils boolValue:value def:YES]];
}

- (void)setShowVerticalScrollIndicator_:(id)value
{
	[self.tableView setShowsVerticalScrollIndicator:[TiUtils boolValue:value]];
}

-(void)setAllowsSelection_:(id)value
{
    allowsSelection = [TiUtils boolValue:value];
    [tableController setClearsSelectionOnViewWillAppear:!allowsSelection];
}

-(void)setAllowsSelectionDuringEditing_:(id)arg
{
	[[self tableView] setAllowsSelectionDuringEditing:[TiUtils boolValue:arg def:NO]];
}

-(void)setEditing_:(id)args
{
    if ([TiUtils boolValue:args def:NO] != editing) {
        editing = !editing;
        [[self tableView] beginUpdates];
        [_tableView setEditing:editing animated:YES];
        [_tableView endUpdates];
    }
}

-(void)setDelaysContentTouches_:(id)value
{
    [[self tableView] setDelaysContentTouches:[TiUtils boolValue:value def:YES]];
}


-(void)setScrollSuspendsImageLoading_:(id)value
{
    _scrollSuspendImageLoading = [TiUtils boolValue:value def:_scrollSuspendImageLoading];
}

-(void)setDisableBounce_:(id)value
{
	[[self tableView] setBounces:![TiUtils boolValue:value]];
}

-(void)setScrollHidesKeyboard_:(id)value
{
    _scrollHidesKeyboard = [TiUtils boolValue:value def:_scrollHidesKeyboard];
}

-(void)setOnDisplayCell_:(id)callback
{
    hasOnDisplayCell = [callback isKindOfClass:[KrollCallback class]] || [callback isKindOfClass:[KrollWrapper class]];
}

#pragma mark - Search Support

-(TiSearchDisplayController*) searchController
{
    return [searchViewProxy searchController];
}

-(void)setCaseInsensitiveSearch_:(id)args
{
    caseInsensitiveSearch = [TiUtils boolValue:args def:YES];
    if (searchActive) {
        [self buildResultsForSearchText];
        if ([[self searchController] isActive]) {
            [[[self searchController] searchResultsTableView] reloadData];
        } else {
            [_tableView reloadData];
        }
    }
}

-(void)setSearchText_:(id)args
{
    id searchView = [self.proxy valueForKey:@"searchView"];
    if (!IS_NULL_OR_NIL(searchView)) {
        DebugLog(@"Can not use searchText with searchView. Ignoring call.");
        return;
    }
    self.searchString = [TiUtils stringValue:args];
    [self buildResultsForSearchText];
    [_tableView reloadData];
}

-(void)setSearchViewExternal_:(id)args {
    ENSURE_TYPE_OR_NIL(args,TiUISearchBarProxy);
    [self tableView];
    if (searchViewProxy)
    {
		[searchViewProxy setProxyObserver:nil];
        [searchViewProxy detachView];
        searchViewProxy.canHaveSearchDisplayController = NO;
        RELEASE_TO_NIL(searchViewProxy)
    }
    RELEASE_TO_NIL(tableController);
    if (args != nil) {
        searchViewProxy = [args retain];
        [searchViewProxy setDelegate:self];
        searchViewProxy.canHaveSearchDisplayController = YES;
        tableController = [[UITableViewController alloc] init];
        [TiUtils configureController:tableController withObject:nil];
        tableController.tableView = [self tableView];
		[tableController setClearsSelectionOnViewWillAppear:!allowsSelection];
        
        TiSearchDisplayController* searchController = [self searchController];
        searchController.searchResultsDataSource = self;
        searchController.searchResultsDelegate = self;
        searchController.delegate = self;
        searchHidden = NO;
    }
    keepSectionsInSearch = [TiUtils boolValue:[self.proxy valueForKey:@"keepSectionsInSearch"] def:NO];
}

-(void)setSearchView_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,TiUISearchBarProxy);
    [self tableView];
    if (searchViewProxy)
    {
		[searchViewProxy setProxyObserver:nil];
        [searchViewProxy detachView];
        RELEASE_TO_NIL(searchViewProxy)
    }
    RELEASE_TO_NIL(tableController);

    if (args != nil) {
        searchViewProxy = [args retain];
        [searchViewProxy setReadyToCreateView:YES];
        [searchViewProxy setDelegate:self];
        tableController = [[UITableViewController alloc] init];
        [TiUtils configureController:tableController withObject:nil];
        tableController.tableView = [self tableView];
		[tableController setClearsSelectionOnViewWillAppear:!allowsSelection];
        [[self getOrCreateSearchWrapper] add:searchViewProxy];
        
        TiSearchDisplayController* searchController = [self searchController];
        searchController.searchResultsDataSource = self;
        searchController.searchResultsDelegate = self;
        searchController.delegate = self;
        searchHidden = [TiUtils boolValue:[self.proxy valueForKey:@"searchHidden"] def:NO];
        if (searchHidden)
		{
			[self hideSearchScreen:nil animated:NO];
		}
    }
    keepSectionsInSearch = [TiUtils boolValue:[self.proxy valueForKey:@"keepSectionsInSearch"] def:NO];
    
}


-(void)setSearchHidden_:(id)hide
{
	if ([TiUtils boolValue:hide])
	{
		searchHidden = YES;
		if (searchViewProxy)
		{
			[self hideSearchScreen:nil];
		}
	}
	else
	{
		searchHidden = NO;
		if (searchViewProxy)
		{
			[self showSearchScreen:nil];
		}
	}
}

-(void)setHideSearchOnSelection_:(id)yn
{
    hideOnSearch = [TiUtils boolValue:yn def:NO];
}

#pragma mark - SectionIndexTitle Support

-(void)setSectionIndexTitles_:(id)args
{
    ENSURE_TYPE_OR_NIL(args, NSArray);
    
    RELEASE_TO_NIL(sectionTitles);
    RELEASE_TO_NIL(sectionIndices);
    RELEASE_TO_NIL(filteredTitles);
    RELEASE_TO_NIL(filteredIndices);
    
    NSArray* theIndex = args;
	if ([theIndex count] > 0) {
        sectionTitles = [[NSMutableArray alloc] initWithCapacity:[theIndex count]];
        sectionIndices = [[NSMutableArray alloc] initWithCapacity:[theIndex count]];
        
        for (NSDictionary *entry in theIndex) {
            ENSURE_DICT(entry);
            NSString *title = [entry objectForKey:@"title"];
            id index = [entry objectForKey:@"index"];
            [sectionTitles addObject:title];
            [sectionIndices addObject:[NSNumber numberWithInt:[TiUtils intValue:index]]];
        }
    }
    if (searchViewProxy == nil) {
        if (searchActive) {
            [self buildResultsForSearchText];
        }
        [_tableView reloadSectionIndexTitles];
    }
}

#pragma mark - SectionIndexTitle Support Datasource methods.

-(NSArray *)sectionIndexTitlesForTableView:(UITableView *)tableView
{
    if (tableView != _tableView) {
        return nil;
    }
    
    if (editing) {
        return nil;
    }
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            return filteredTitles;
        } else {
            return nil;
        }
    }
    
    return sectionTitles;
}

-(NSInteger)tableView:(UITableView *)tableView sectionForSectionIndexTitle:(NSString *)title atIndex:(NSInteger)theIndex
{
    if (tableView != _tableView) {
        return 0;
    }
    
    if (editing) {
        return 0;
    }
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) && (filteredTitles != nil) && (filteredIndices != nil) ) {
            // get the index for the title
            int index = [filteredTitles indexOfObject:title];
            if (index > 0 && (index < [filteredIndices count]) ) {
                return [[filteredIndices objectAtIndex:index] intValue];
            }
            return 0;
        } else {
            return 0;
        }
    }
    
    if ( (sectionTitles != nil) && (sectionIndices != nil) ) {
        // get the index for the title
        int index = [sectionTitles indexOfObject:title];
        if (index > 0 && (index < [sectionIndices count]) ) {
            return [[sectionIndices objectAtIndex:index] intValue];
        }
        return 0;
    }
    return 0;
}

#pragma mark - Editing Support

-(BOOL)canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    id editValue = [self valueWithKey:@"canEdit" atIndexPath:indexPath];
    //canEdit if undefined is false
    return [TiUtils boolValue:editValue def:NO];
}


-(BOOL)canMoveRowAtIndexPath:(NSIndexPath *)indexPath
{
    id moveValue = [self valueWithKey:@"canMove" atIndexPath:indexPath];
    //canMove if undefined is false
    return [TiUtils boolValue:moveValue def:NO];
}

#pragma mark - Editing Support Datasource methods.

- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (tableView != _tableView) {
        return NO;
    }
    
    if (searchActive) {
        return NO;
    }
    
    if ([self canEditRowAtIndexPath:indexPath]) {
        return YES;
    }
    if (editing) {
        return [self canMoveRowAtIndexPath:indexPath];
    }
    return NO;
}

- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        TiUIListSectionProxy* theSection = [[self.listViewProxy sectionForIndex:indexPath.section] retain];
        NSDictionary *theItem = [[theSection itemAtIndex:indexPath.row] retain];

        
        //Fire the delete Event if required
        NSString *eventName = @"delete";
        if ([self.proxy _hasListeners:eventName]) {
        
            NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                                theSection, @"section",
                                                self.proxy, @"listView",
                                                NUMINT(indexPath.section), @"sectionIndex",
                                                NUMINT(indexPath.row), @"itemIndex",
                                                nil];
            id propertiesValue = [theItem objectForKey:@"properties"];
            NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
            id itemId = [properties objectForKey:@"itemId"];
            if (itemId != nil) {
                [eventObject setObject:itemId forKey:@"itemId"];
            }
            [self.proxy fireEvent:eventName withObject:eventObject propagate:NO checkForListener:NO];
            [eventObject release];
        }
        [theItem release];
        
        BOOL asyncDelete = [TiUtils boolValue:[self.proxy valueForKey:@"asyncDelete"] def:NO];
        if (asyncDelete) return;
        [theSection deleteItemsAt:@[@(indexPath.row), @(1), @{@"animated":@(YES)}]];
    }
}

#pragma mark - Editing Support Delegate Methods.

- (UITableViewCellEditingStyle)tableView:(UITableView *)tableView editingStyleForRowAtIndexPath:(NSIndexPath *)indexPath
{
    //No support for insert style yet
    if ([self canEditRowAtIndexPath:indexPath]) {
        return UITableViewCellEditingStyleDelete;
    } else {
        return UITableViewCellEditingStyleNone;
    }
}

- (BOOL)tableView:(UITableView *)tableView shouldIndentWhileEditingRowAtIndexPath:(NSIndexPath *)indexPath
{
    return [self canEditRowAtIndexPath:indexPath];
}

- (void)tableView:(UITableView *)tableView willBeginEditingRowAtIndexPath:(NSIndexPath *)indexPath
{
    editing = YES;
    [self.proxy replaceValue:NUMBOOL(editing) forKey:@"editing" notification:NO];
}

- (void)tableView:(UITableView *)tableView didEndEditingRowAtIndexPath:(NSIndexPath *)indexPath
{
    editing = [_tableView isEditing];
    [self.proxy replaceValue:NUMBOOL(editing) forKey:@"editing" notification:NO];
    if (!editing) {
        [_tableView reloadData];
    }
}

#pragma mark - UITableViewDataSource

- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (tableView != _tableView) {
        return NO;
    }
    
    if (searchActive) {
        return NO;
    }
    
	return [self canMoveRowAtIndexPath:indexPath];
}

- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath
{
    int fromSectionIndex = [fromIndexPath section];
    int fromRowIndex = [fromIndexPath row];
    int toSectionIndex = [toIndexPath section];
    int toRowIndex = [toIndexPath row];
    
    
    
    if (fromSectionIndex == toSectionIndex) {
        if (fromRowIndex == toRowIndex) {
            return;
        }
        //Moving a row in the same index. Just move and reload section
        TiUIListSectionProxy* theSection = [[self.listViewProxy sectionForIndex:fromSectionIndex] retain];
        NSDictionary *theItem = [[theSection itemAtIndex:fromRowIndex] retain];
        
        //Delete Data
        [theSection deleteItemAtIndex:fromRowIndex];
        
        //Insert the data
        [theSection addItem:theItem atIndex:toRowIndex];
        
        //Fire the move Event if required
        NSString *eventName = @"move";
        if ([self.proxy _hasListeners:eventName]) {
            
            NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                                self.proxy, @"listView",
                                                theSection, @"section",
                                                NUMINT(fromSectionIndex), @"sectionIndex",
                                                NUMINT(fromRowIndex), @"itemIndex",
                                                theSection,@"targetSection",
                                                NUMINT(toSectionIndex), @"targetSectionIndex",
                                                NUMINT(toRowIndex), @"targetItemIndex",
                                                nil];
            id propertiesValue = [theItem objectForKey:@"properties"];
            NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
            id itemId = [properties objectForKey:@"itemId"];
            if (itemId != nil) {
                [eventObject setObject:itemId forKey:@"itemId"];
            }
            [self.proxy fireEvent:eventName withObject:eventObject propagate:NO checkForListener:NO];
            [eventObject release];
        }
        
        [tableView reloadData];
        
        [theSection release];
        [theItem release];
        
        
    } else {
        TiUIListSectionProxy* fromSection = [[self.listViewProxy sectionForIndex:fromSectionIndex] retain];
        NSDictionary *theItem = [[fromSection itemAtIndex:fromRowIndex] retain];
        TiUIListSectionProxy* toSection = [[self.listViewProxy sectionForIndex:toSectionIndex] retain];
        
        //Delete Data
        [fromSection deleteItemAtIndex:fromRowIndex];
        
        //Insert the data
        [toSection addItem:theItem atIndex:toRowIndex];
        
        //Fire the move Event if required
        NSString *eventName = @"move";
        if ([self.proxy _hasListeners:eventName]) {
            
            NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                                fromSection, @"section",
                                                self.proxy, @"listView",
                                                NUMINT(fromSectionIndex), @"sectionIndex",
                                                NUMINT(fromRowIndex), @"itemIndex",
                                                toSection,@"targetSection",
                                                NUMINT(toSectionIndex), @"targetSectionIndex",
                                                NUMINT(toRowIndex), @"targetItemIndex",
                                                nil];
            id propertiesValue = [theItem objectForKey:@"properties"];
            NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
            id itemId = [properties objectForKey:@"itemId"];
            if (itemId != nil) {
                [eventObject setObject:itemId forKey:@"itemId"];
            }
            [self.proxy fireEvent:eventName withObject:eventObject checkForListener:NO];
            [eventObject release];
        }
        
        if ([fromSection itemCount] == 0) {
            if (pruneSections) {
                [self.listViewProxy deleteSectionAtIndex:fromSectionIndex];
            }
        }
        
        [tableView reloadData];
        
        [fromSection release];
        [toSection release];
        [theItem release];
    }
}

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    NSUInteger sectionCount = 0;
    
    //TIMOB-15526
    if (tableView != _tableView && tableView.backgroundColor == [UIColor clearColor]) {
        tableView.backgroundColor = [UIColor whiteColor];
    }

    if (_searchResults != nil) {
        sectionCount = [_searchResults count];
    } else {
        sectionCount = [self.listViewProxy.sectionCount unsignedIntegerValue];
    }
    return MAX(0,sectionCount);
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    if (_searchResults != nil) {
        if ([_searchResults count] <= section) {
            return 0;
        }
        NSArray* theSection = [_searchResults objectAtIndex:section];
        return [theSection count];
        
    } else {
        TiUIListSectionProxy* theSection = [self.listViewProxy sectionForIndex:section];
        if (theSection != nil) {
            return theSection.itemCount;
        }
        return 0;
    }
}

-(UITableViewCell *) forceCellForRowAtIndexPath:(NSIndexPath *)indexPath {
    return [_tableView cellForRowAtIndexPath:indexPath];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSIndexPath* realIndexPath = [self pathForSearchPath:indexPath];
    TiUIListSectionProxy* theSection = [self.listViewProxy sectionForIndex:realIndexPath.section];
    NSInteger maxItem = 0;
    
    if (_searchResults != nil) {
        NSArray* sectionResults = [_searchResults objectAtIndex:indexPath.section];
        maxItem = [sectionResults count];
    } else {
        maxItem = theSection.itemCount;
    }
    
    NSDictionary *item = [theSection itemAtIndex:realIndexPath.row];
    id templateId = [item objectForKey:@"template"];
    if (templateId == nil) {
        templateId = _defaultItemTemplate;
    }
    NSString *cellIdentifier = [templateId isKindOfClass:[NSNumber class]] ? [NSString stringWithFormat:@"TiUIListView__internal%@", templateId]: [templateId description];
    TiUIListItem *cell = [tableView dequeueReusableCellWithIdentifier:cellIdentifier];
    
    TiGroupedListItemPosition position = TiGroupedListItemPositionMiddle;
    BOOL grouped = NO;
    if (tableView.style == UITableViewStyleGrouped) {
        grouped = YES;
        if (indexPath.row == 0) {
            if (maxItem == 1) {
                position = TiGroupedListItemPositionSingleLine;
            } else {
                position = TiGroupedListItemPositionTop;
            }
        } else if (indexPath.row == (maxItem - 1) ) {
            position = TiGroupedListItemPositionBottom;
        } else {
            position = TiGroupedListItemPositionMiddle;
        }
    }
    
    if (cell == nil) {
        id<TiEvaluator> context = self.listViewProxy.executionContext;
        if (context == nil) {
            context = self.listViewProxy.pageContext;
        }
        TiUIListItemProxy *cellProxy = [[TiUIListItemProxy alloc] initWithListViewProxy:self.listViewProxy inContext:context];
        if ([templateId isKindOfClass:[NSNumber class]]) {
            UITableViewCellStyle cellStyle = [templateId unsignedIntegerValue];
            cell = [[TiUIListItem alloc] initWithStyle:cellStyle position:position grouped:grouped reuseIdentifier:cellIdentifier proxy:cellProxy];
        } else {
            [cell configurationStart];
            cell = [[TiUIListItem alloc] initWithProxy:cellProxy position:position grouped:grouped reuseIdentifier:cellIdentifier];
            id template = [_templates objectForKey:templateId];
            if (template != nil) {
                [cellProxy unarchiveFromTemplate:template withEvents:YES];
            }
            [cell configurationSet];
        }
        [cellProxy release];
        [cell autorelease];
    }
    else {
        [cell setPosition:position isGrouped:grouped];
    }
    
    cell.dataItem = item;
    cell.proxy.indexPath = realIndexPath;
    return cell;
}

- (NSString *)tableView:(UITableView *)tableView titleForHeaderInSection:(NSInteger)section
{
    if (tableView != _tableView) {
        return nil;
    }
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            NSInteger realSection = [self sectionForSearchSection:section];
            return [[self.listViewProxy sectionForIndex:realSection] headerTitle];
        } else {
            return nil;
        }
    }
    
    return [[self.listViewProxy sectionForIndex:section] headerTitle];
}

- (NSString *)tableView:(UITableView *)tableView titleForFooterInSection:(NSInteger)section
{
    if (tableView != _tableView) {
        return nil;
    }
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            NSInteger realSection = [self sectionForSearchSection:section];
            return [[self.listViewProxy sectionForIndex:realSection] footerTitle];
        } else {
            return nil;
        }
    }
    return [[self.listViewProxy sectionForIndex:section] footerTitle];
}

#pragma mark - UITableViewDelegate

- (void)tableView:(UITableView *)tableView willDisplayCell:(UITableViewCell *)cell forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (hasOnDisplayCell) {
        TiUIListSectionProxy *section = [self.listViewProxy sectionForIndex:indexPath.section];
        NSDictionary *item = [section itemAtIndex:indexPath.row];
        NSDictionary * propertiesDict = @{
                                          @"view":((TiUIListItem*)cell).proxy,
                                          @"listView": self.proxy,
                                          @"section":section,
                                          @"searchResult":NUMBOOL([self isSearchActive]),
                                          @"sectionIndex":NUMINT(indexPath.section),
                                          @"itemIndex":NUMINT(indexPath.row)
        };
        [self.proxy fireCallback:@"onDisplayCell" withArg:propertiesDict withSource:self.proxy];
    }
    if (searchActive || (tableView != _tableView)) {
        return;
    }
    //Tell the proxy about the cell to be displayed
    [self.listViewProxy willDisplayCell:indexPath];
}

- (UIView *)tableView:(UITableView *)tableView viewForHeaderInSection:(NSInteger)section
{
    if (tableView != _tableView) {
        return nil;
    }
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            NSInteger realSection = [self sectionForSearchSection:section];
            return [self sectionView:realSection forLocation:@"headerView" section:nil];
        } else {
            return nil;
        }
    }
    
    TiUIListSectionProxy *sectionProxy = [self.listViewProxy sectionForIndex:section];
    if(![sectionProxy isHidden] &&  [tableView numberOfRowsInSection:section] == 0)
    {
        return nil;
    }
    
    
    return [self sectionView:section forLocation:@"headerView" section:nil];
}

- (UIView *)tableView:(UITableView *)tableView viewForFooterInSection:(NSInteger)section
{
    if (tableView != _tableView) {
        return nil;
    }
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            NSInteger realSection = [self sectionForSearchSection:section];
            return [self sectionView:realSection forLocation:@"footerView" section:nil];
        } else {
            return nil;
        }
    }
    
    return [self sectionView:section forLocation:@"footerView" section:nil];
}

#define DEFAULT_SECTION_HEADERFOOTER_HEIGHT 20.0

- (CGFloat)tableView:(UITableView *)tableView heightForHeaderInSection:(NSInteger)section
{
    if (tableView != _tableView) {
        return 0.0;
    }
    
    NSInteger realSection = section;
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            realSection = [self sectionForSearchSection:section];
        } else {
            return 0.0;
        }
    }
    
    TiUIListSectionProxy *sectionProxy = [self.listViewProxy sectionForIndex:realSection];
    TiViewProxy* viewProxy = [sectionProxy sectionViewForLocation:@"headerView" inListView:self];
	
    CGFloat size = 0.0;
    if (viewProxy!=nil) {
        [viewProxy getAndPrepareViewForOpening:[self.tableView bounds]]; //to make sure it is setup
        LayoutConstraint *viewLayout = [viewProxy layoutProperties];
        TiDimension constraint =  TiDimensionIsUndefined(viewLayout->height)?[viewProxy defaultAutoHeightBehavior:nil]:viewLayout->height;
        switch (constraint.type)
        {
            case TiDimensionTypeDip:
                size += constraint.value;
                break;
            case TiDimensionTypeAuto:
            case TiDimensionTypeAutoSize:
                size += [viewProxy minimumParentSizeForSize:[self.tableView bounds].size].height;
                break;
            default:
                size+=DEFAULT_SECTION_HEADERFOOTER_HEIGHT;
                break;
        }
    }
    /*
     * This behavior is slightly more complex between iOS 4 and iOS 5 than you might believe, and Apple's
     * documentation is once again misleading. It states that in iOS 4 this value was "ignored if
     * -[delegate tableView:viewForHeaderInSection:] returned nil" but apparently a non-nil value for
     * -[delegate tableView:titleForHeaderInSection:] is considered a valid value for height handling as well,
     * provided it is NOT the empty string.
     *
     * So for parity with iOS 4, iOS 5 must similarly treat the empty string header as a 'nil' value and
     * return a 0.0 height that is overridden by the system.
     */
    else if ([sectionProxy headerTitle]!=nil) {
        if ([[sectionProxy headerTitle] isEqualToString:@""]) {
            return size;
        }
        size+=[tableView sectionHeaderHeight];
        
        if (size < DEFAULT_SECTION_HEADERFOOTER_HEIGHT) {
            size += DEFAULT_SECTION_HEADERFOOTER_HEIGHT;
        }
    }
    return size;
}

- (CGFloat)tableView:(UITableView *)tableView heightForFooterInSection:(NSInteger)section
{
    if (tableView != _tableView) {
        return 0.0;
    }
    
    NSInteger realSection = section;
    
    if (searchActive) {
        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
            realSection = [self sectionForSearchSection:section];
        } else {
            return 0.0;
        }
    }
    
    TiUIListSectionProxy *sectionProxy = [self.listViewProxy sectionForIndex:realSection];
    TiViewProxy* viewProxy = [sectionProxy sectionViewForLocation:@"footerView" inListView:self];
	
    CGFloat size = 0.0;
    if (viewProxy!=nil) {
        [viewProxy getAndPrepareViewForOpening:[self.tableView bounds]]; //to make sure it is setup
        LayoutConstraint *viewLayout = [viewProxy layoutProperties];
        TiDimension constraint =  TiDimensionIsUndefined(viewLayout->height)?[viewProxy defaultAutoHeightBehavior:nil]:viewLayout->height;
        switch (constraint.type)
        {
            case TiDimensionTypeDip:
                size += constraint.value;
                break;
            case TiDimensionTypeAuto:
            case TiDimensionTypeAutoSize:
                size += [viewProxy minimumParentSizeForSize:[self.tableView bounds].size].height;
                break;
            default:
                size+=DEFAULT_SECTION_HEADERFOOTER_HEIGHT;
                break;
        }
    }
    /*
     * This behavior is slightly more complex between iOS 4 and iOS 5 than you might believe, and Apple's
     * documentation is once again misleading. It states that in iOS 4 this value was "ignored if
     * -[delegate tableView:viewForHeaderInSection:] returned nil" but apparently a non-nil value for
     * -[delegate tableView:titleForHeaderInSection:] is considered a valid value for height handling as well,
     * provided it is NOT the empty string.
     *
     * So for parity with iOS 4, iOS 5 must similarly treat the empty string header as a 'nil' value and
     * return a 0.0 height that is overridden by the system.
     */
    else if ([sectionProxy footerTitle]!=nil) {
        if ([[sectionProxy footerTitle] isEqualToString:@""]) {
            return size;
        }
        size+=[tableView sectionFooterHeight];
        
        if (size < DEFAULT_SECTION_HEADERFOOTER_HEIGHT) {
            size += DEFAULT_SECTION_HEADERFOOTER_HEIGHT;
        }
    }
    return size;
}

-(CGFloat)computeRowWidth
{
    CGFloat rowWidth = _tableView.bounds.size.width;
	if ((self.tableView.style == UITableViewStyleGrouped) && (![TiUtils isIOS7OrGreater]) ){
		rowWidth -= GROUPED_MARGIN_WIDTH;
	}
    
    // Apple does not provide a good way to get information about the index sidebar size
    // in the event that it exists - it silently resizes row content which is "flexible width"
    // but this is not useful for us. This is a problem when we have Ti.UI.SIZE/FILL behavior
    // on row contents, which rely on the height of the row to be accurately precomputed.
    //
    // The following is unreliable since it uses a private API name, but one which has existed
    // since iOS 3.0. The alternative is to grab a specific subview of the tableview itself,
    // which is more fragile.
    
    NSArray* subviews = [_tableView subviews];
    if ([subviews count] > 0) {
        // Obfuscate private class name
        Class indexview = NSClassFromString([@"UITableView" stringByAppendingString:@"Index"]);
        for (UIView* view in subviews) {
            if ([view isKindOfClass:indexview]) {
                rowWidth -= [view frame].size.width;
            }
        }
    }
    
    return rowWidth;
}

-(CGFloat)tableView:(UITableView *)tableView rowHeight:(CGFloat)height
{
	if (TiDimensionIsDip(_minRowHeight))
	{
		height = MAX(_minRowHeight.value,height);
	}
	if (TiDimensionIsDip(_maxRowHeight))
	{
		height = MIN(_maxRowHeight.value,height);
	}
	return height < 1 ? tableView.rowHeight : height;
}

-(TiUIListItem*)visibleCellAtIndexPath:(NSIndexPath *)indexPath {
    NSArray* visibleCells = _tableView.visibleCells;
    if (!visibleCells) return nil;
    __block TiUIListItem* result = nil;
    [visibleCells enumerateObjectsUsingBlock:^(TiUIListItem* obj, NSUInteger idx, BOOL *stop) {
        if ([obj isKindOfClass:[TiUIListItem class]]) {
            if ([obj.proxy.indexPath compare:indexPath] == NSOrderedSame) {
                result = obj;
                stop = YES;
            }
        }
        
    }];
    return result;
}


- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSIndexPath* realIndexPath = [self pathForSearchPath:indexPath];
    
    id visibleProp = [self valueWithKey:@"visible" atIndexPath:realIndexPath];
    BOOL visible = realIndexPath?[visibleProp boolValue]:true;
    if (!visible) return 0.0f;
    
    id heightValue = [self valueWithKey:@"rowHeight" atIndexPath:realIndexPath];
    TiDimension height = _rowHeight;
    if (heightValue != nil) {
        height = [TiUtils dimensionValue:heightValue];
    }
    if (TiDimensionIsDip(height)) {
        return [self tableView:tableView rowHeight:height.value];
    }
    
    else if (TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height))
    {
        TiUIListSectionProxy* theSection = [self.listViewProxy sectionForIndex:realIndexPath.section];
        NSDictionary *item = [theSection itemAtIndex:realIndexPath.row];
        id templateId = [item objectForKey:@"template"];
        if (templateId == nil) {
            templateId = _defaultItemTemplate;
        }
        TiUIListItemProxy *cellProxy = nil;
//        TiUIListItem* visibleCell = [self visibleCellAtIndexPath:realIndexPath];
//        if (visibleCell) {
//            cellProxy = [((TiUIListItem*)visibleCell) proxy];
//        }
        if (!cellProxy) {
            cellProxy = [_measureProxies objectForKey:templateId];
        }
        if (cellProxy != nil) {
            CGFloat width = [cellProxy sizeWidthForDecorations:[self computeRowWidth] forceResizing:YES];
            if (width > 0) {
                [cellProxy setDataItem:item];
                return [self tableView:tableView rowHeight:[cellProxy minimumParentSizeForSize:CGSizeMake(width, 0)].height];
            }
        }
    }
    else if (TiDimensionIsPercent(height) || TiDimensionIsAutoFill(height)) {
        return [self tableView:tableView rowHeight:TiDimensionCalculateValue(height, tableView.bounds.size.height)];
    }
    return 44;
}

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (allowsSelection==NO)
	{
		[tableView deselectRowAtIndexPath:indexPath animated:YES];
	}
    [self fireClickForItemAtIndexPath:[self pathForSearchPath:indexPath] tableView:tableView accessoryButtonTapped:NO];
}

- (void)tableView:(UITableView *)tableView accessoryButtonTappedForRowWithIndexPath:(NSIndexPath *)indexPath
{
    if (allowsSelection==NO)
	{
		[tableView deselectRowAtIndexPath:indexPath animated:YES];
	}
    [self fireClickForItemAtIndexPath:[self pathForSearchPath:indexPath] tableView:tableView accessoryButtonTapped:YES];
}

#pragma mark - ScrollView Delegate

- (NSMutableDictionary *) eventObjectForScrollView: (UIScrollView *) scrollView
{
	return [NSMutableDictionary dictionaryWithObjectsAndKeys:
			[TiUtils pointToDictionary:scrollView.contentOffset],@"contentOffset",
			[TiUtils sizeToDictionary:scrollView.contentSize], @"contentSize",
			[TiUtils sizeToDictionary:_tableView.bounds.size], @"size",
			nil];
}

- (void)fireScrollEvent:(UIScrollView *)scrollView {
	if ([self.proxy _hasListeners:@"scroll" checkParent:NO])
	{
        NSArray* visibles = [_tableView indexPathsForVisibleRows];
        NSMutableDictionary* event = [self eventObjectForScrollView:scrollView];
        [event setObject:NUMINT(((NSIndexPath*)[visibles objectAtIndex:0]).row) forKey:@"firstVisibleItem"];
        [event setObject:NUMINT([visibles count]) forKey:@"visibleItemCount"];
		[self.proxy fireEvent:@"scroll" withObject:event checkForListener:NO];
	}
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView
{
    if (scrollView.isDragging || scrollView.isDecelerating)
	{
        [self fireScrollEvent:scrollView];
    }
    if ( (_pullViewProxy != nil) && ([scrollView isTracking]) ) {
        BOOL pullChanged = NO;
        if ( (scrollView.contentOffset.y < pullThreshhold) && (pullActive == NO) ) {
            pullActive = YES;
            pullChanged = YES;
        } else if ( (scrollView.contentOffset.y > pullThreshhold) && (pullActive == YES) ) {
            pullActive = NO;
            pullChanged = YES;
        }
        if (pullChanged && [(TiViewProxy*)self.proxy _hasListeners:@"pullchanged" checkParent:NO]) {
            [self.proxy fireEvent:@"pullchanged" withObject:[NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(pullActive),@"active",nil] propagate:NO checkForListener:NO];
        }
        if (scrollView.contentOffset.y <= 0 && [(TiViewProxy*)self.proxy _hasListeners:@"pull" checkParent:NO]) {
            [self.proxy fireEvent:@"pull" withObject:[NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(pullActive),@"active",nil] propagate:NO checkForListener:NO];
        }
    }
    
}

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
    if (_scrollHidesKeyboard) {
        [scrollView endEditing:YES];
    }
	// suspend image loader while we're scrolling to improve performance
	if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] suspend];
    [self.proxy fireEvent:@"dragstart" propagate:NO];
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
    if (decelerate==NO && _scrollSuspendImageLoading)
	{
		// resume image loader when we're done scrolling
		[[ImageLoader sharedLoader] resume];
	}
	if ([(TiViewProxy*)self.proxy _hasListeners:@"dragend" checkParent:NO])
	{
		[self.proxy fireEvent:@"dragend" withObject:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:decelerate],@"decelerate",nil] propagate:NO checkForListener:NO];
	}
    
    
    if ( (_pullViewProxy != nil) && (pullActive == YES) ) {
        pullActive = NO;
        [self.proxy fireEvent:@"pullend" propagate:NO];
    }
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
	// resume image loader when we're done scrolling
	if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] resume];
	if ([(TiViewProxy*)self.proxy _hasListeners:@"scrollend" checkParent:NO])
	{
		[self.proxy fireEvent:@"scrollend" withObject:[self eventObjectForScrollView:scrollView] propagate:NO checkForListener:NO];
	}
}

- (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView
{
	// suspend image loader while we're scrolling to improve performance
	if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] suspend];
	return YES;
}

- (void)scrollViewDidScrollToTop:(UIScrollView *)scrollView
{
    [self fireScrollEvent:scrollView];
}

#pragma mark Overloaded view handling
- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event
{
	UIView * result = [super hitTest:point withEvent:event];
	if(result == self)
	{	//There is no valid reason why the TiUITableView will get an
		//touch event; it should ALWAYS be a child view.
		return nil;
	}
	return result;
}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
	// iOS idiom seems to indicate that you should never be able to interact with a table
	// while the 'delete' button is showing for a row, but touchesBegan:withEvent: is still triggered.
	// Turn it into a no-op while we're editing
	if (!editing) {
		[super touchesBegan:touches withEvent:event];
	}
}

-(void)recognizedSwipe:(UISwipeGestureRecognizer *)recognizer
{
    BOOL viaSearch = [self isSearchActive];
    UITableView* theTableView = viaSearch ? [[self searchController] searchResultsTableView] : [self tableView];
    CGPoint point = [recognizer locationInView:theTableView];
    NSIndexPath* indexPath = [theTableView indexPathForRowAtPoint:point];
    indexPath = [self pathForSearchPath:indexPath];
    if (indexPath != nil) {
        if ([[self proxy] _hasListeners:@"swipe"]) {
            NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath tableView:theTableView];
            [event setValue:[self swipeStringFromGesture:recognizer] forKey:@"direction"];
            [[self proxy] fireEvent:@"swipe" withObject:event checkForListener:NO];
        }
        
    }
    else {
        [super recognizedSwipe:recognizer];
    }
    
    if (allowsSelection == NO)
    {
        [theTableView deselectRowAtIndexPath:indexPath animated:YES];
    }
}

-(void)recognizedLongPress:(UILongPressGestureRecognizer*)recognizer
{
    if ([recognizer state] == UIGestureRecognizerStateBegan) {
        BOOL viaSearch = [self isSearchActive];
        UITableView* theTableView = viaSearch ? [[self searchController] searchResultsTableView] : [self tableView];
        CGPoint point = [recognizer locationInView:theTableView];
        NSIndexPath* indexPath = [theTableView indexPathForRowAtPoint:point];
        indexPath = [self pathForSearchPath:indexPath];
        
        NSMutableDictionary *event;
        if (indexPath != nil) {
            if ([[self proxy] _hasListeners:@"longpress"]) {
                NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath tableView:theTableView atPoint:point];
                [[self proxy] fireEvent:@"longpress" withObject:event checkForListener:NO];
            }
        }
        else {
            [super recognizedLongPress:recognizer];
        }
        
        if (allowsSelection == NO)
        {
            [theTableView deselectRowAtIndexPath:indexPath animated:YES];
        }
        if (viaSearch) {
            if (hideOnSearch) {
                [self hideSearchScreen:nil];
            }
            else {
                /*
                 TIMOB-7397. Observed that `searchBarTextDidBeginEditing` delegate
                 method was being called on screen transition which was causing a
                 visual glitch. Checking for isFirstResponder at this point always
                 returns false. Calling blur here so that the UISearchBar resigns
                 as first responder on main thread
                 */
                [searchViewProxy performSelector:@selector(blur:) withObject:nil];
            }
            
        }
    }
}


#pragma mark - UISearchBarDelegate Methods
- (BOOL)searchBarShouldBeginEditing:(UISearchBar *)searchBar
{
    if (_searchWrapper != nil) {
        [_searchWrapper layoutProperties]->right = TiDimensionDip(0);
        [_searchWrapper refreshViewIfNeeded];
    }
}

- (void)searchBarTextDidBeginEditing:(UISearchBar *)searchBar
{
    self.searchString = (searchBar.text == nil) ? @"" : searchBar.text;
    [self buildResultsForSearchText];
    [[[self searchController] searchResultsTableView] reloadData];
}

- (void)searchBarTextDidEndEditing:(UISearchBar *)searchBar
{
    if ([searchBar.text length] == 0) {
        self.searchString = @"";
        [self buildResultsForSearchText];
        if ([[self searchController] isActive]) {
            [[self searchController] setActive:NO animated:YES];
        }
    }
}

- (void)searchBar:(UISearchBar *)searchBar textDidChange:(NSString *)searchText
{
    self.searchString = (searchText == nil) ? @"" : searchText;
    [self buildResultsForSearchText];
}

- (void)searchBarSearchButtonClicked:(UISearchBar *)searchBar
{
    [searchBar resignFirstResponder];
    [self makeRootViewFirstResponder];
}

- (void)searchBarCancelButtonClicked:(UISearchBar *) searchBar
{
    self.searchString = @"";
    [searchBar setText:self.searchString];
    [self buildResultsForSearchText];
}

#pragma mark - UISearchDisplayDelegate Methods

- (void)searchDisplayControllerWillBeginSearch:(UISearchDisplayController *)controller
{
    searchViewAnimating = YES;
	[_tableView setContentOffset:CGPointZero animated:NO];
}

- (void) searchDisplayControllerWillEndSearch:(UISearchDisplayController *)controller {
    searchViewAnimating = YES;
}

- (void) searchDisplayControllerDidBeginSearch:(UISearchDisplayController *)controller {
    searchViewAnimating = NO;
}

- (void) searchDisplayControllerDidEndSearch:(UISearchDisplayController *)controller
{
    searchViewAnimating = NO;
    self.searchString = @"";
    [self buildResultsForSearchText];
    
    //IOS7 DP3. TableView seems to be adding the searchView to
    //tableView. Bug on IOS7?
    if (_searchWrapper != nil) {
        CGFloat rowWidth = floorf([self computeRowWidth:_tableView]);
        if (rowWidth > 0) {
            CGFloat right = _tableView.bounds.size.width - rowWidth;
            [_searchWrapper layoutProperties]->right = TiDimensionDip(right);
            [_searchWrapper refreshViewIfNeeded];
        }
    }
    [searchViewProxy ensureSearchBarHeirarchy];
    [_tableView reloadData];
    [self hideSearchScreen:nil];
//    [self performSelector:@selector(hideSearchScreen:) withObject:nil afterDelay:0.2];
}

#pragma mark - TiScrolling

-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop
{
    CGRect minimumContentRect = [_tableView bounds];
    InsetScrollViewForKeyboard(_tableView,keyboardTop,minimumContentRect.size.height + minimumContentRect.origin.y);
}

-(void)scrollToShowView:(TiUIView *)firstResponderView withKeyboardHeight:(CGFloat)keyboardTop
{
    if ([_tableView isScrollEnabled]) {
        CGRect minimumContentRect = [_tableView bounds];
        CGRect responderRect = [self convertRect:[firstResponderView bounds] fromView:firstResponderView];
        CGPoint offsetPoint = [_tableView contentOffset];
        responderRect.origin.x += offsetPoint.x;
        responderRect.origin.y += offsetPoint.y;
        
        OffsetScrollViewForRect(_tableView,keyboardTop,minimumContentRect.size.height + minimumContentRect.origin.y,responderRect);
    }
}


#pragma mark - Internal Methods

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView  atPoint:(CGPoint)point accessoryButtonTapped:(BOOL)accessoryButtonTapped
{
    TiUIListSectionProxy *section = [self.listViewProxy sectionForIndex:indexPath.section];
	NSDictionary *item = [section itemAtIndex:indexPath.row];
    NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
										section, @"section",
										self.proxy, @"listView",
										NUMBOOL([self isSearchActive]), @"searchResult",
										NUMINT(indexPath.section), @"sectionIndex",
										NUMINT(indexPath.row), @"itemIndex",
										NUMBOOL(accessoryButtonTapped), @"accessoryClicked",
										nil];
	id propertiesValue = [item objectForKey:@"properties"];
	NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
	id itemId = [properties objectForKey:@"itemId"];
	if (itemId != nil) {
		[eventObject setObject:itemId forKey:@"itemId"];
	}
	TiUIListItem *cell = (TiUIListItem *)[tableView cellForRowAtIndexPath:indexPath];
	if (cell.templateStyle == TiUIListItemTemplateStyleCustom) {
		UIView *contentView = cell.contentView;
        TiViewProxy *tapViewProxy =[TiUIHelper findViewProxyWithBindIdUnder:contentView containingPoint:[tableView convertPoint:point toView:contentView]];
		if (tapViewProxy != nil) {
			[eventObject setObject:[tapViewProxy valueForKey:@"bindId"] forKey:@"bindId"];
		}
	}
    return [eventObject autorelease];
}

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView
{
    return [self EventObjectForItemAtIndexPath:indexPath tableView:tableView atPoint:[_tableView touchPoint]];
}

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView atPoint:(CGPoint)point
{
    NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath tableView:tableView atPoint:point accessoryButtonTapped:NO];
    [event setObject:NUMFLOAT(point.x) forKey:@"x"];
    [event setObject:NUMFLOAT(point.y) forKey:@"y"];
    return event;
}

- (void)fireClickForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView accessoryButtonTapped:(BOOL)accessoryButtonTapped
{
	NSString *eventName = @"itemclick";
    if (![self.proxy _hasListeners:eventName]) {
		return;
	}
	
	
	[self.proxy fireEvent:eventName withObject:[self EventObjectForItemAtIndexPath:indexPath tableView:tableView] checkForListener:NO];
}


- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath {
    int numOfSections = [self numberOfSectionsInTableView:self.tableView];
    int nextSection = ((indexPath.section + 1) % numOfSections);
    
    if (indexPath.row + 1 == [self tableView:self.tableView numberOfRowsInSection:indexPath.section]) {
        return [NSIndexPath indexPathForRow:0 inSection:nextSection];
    } else {
        return [NSIndexPath indexPathForRow:(indexPath.row + 1) inSection:indexPath.section];
    }
}

#pragma mark - UITapGestureRecognizer

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer
{
//	tapPoint = [gestureRecognizer locationInView:gestureRecognizer.view];
	return NO;
}

- (void)handleTap:(UITapGestureRecognizer *)tapGestureRecognizer
{
	// Never called
}

#pragma mark - Static Methods

+ (void)setBackgroundColor:(UIColor*)bgColor onTable:(UITableView*)table
{
	UIColor* defaultColor = [table style] == UITableViewStylePlain ? [UIColor whiteColor] : [UIColor groupTableViewBackgroundColor];
	
	// WORKAROUND FOR APPLE BUG: 4.2 and lower don't like setting background color for grouped table views on iPad.
	// So, we check the table style and device, and if they match up wrong, we replace the background view with our own.
	if ([table style] == UITableViewStyleGrouped && ([TiUtils isIPad] || [TiUtils isIOS6OrGreater])) {
		UIView* bgView = [[[UIView alloc] initWithFrame:[table frame]] autorelease];
		[table setBackgroundView:bgView];
	}
	
	[table setBackgroundColor:(bgColor != nil ? bgColor : defaultColor)];
	[[table backgroundView] setBackgroundColor:[table backgroundColor]];
	
	[table setOpaque:![[table backgroundColor] isEqual:[UIColor clearColor]]];
}

+ (TiViewProxy*)titleViewForText:(NSString*)text inTable:(UITableView *)tableView footer:(BOOL)footer
{
    TiUILabelProxy* titleProxy = [[TiUILabelProxy alloc] init];
    [titleProxy setValue:[NSDictionary dictionaryWithObjectsAndKeys:@"17",@"fontSize",@"bold",@"fontWeight", nil] forKey:@"font"];
    [titleProxy setValue:text forKey:@"text"];
    [titleProxy setValue:@"black" forKey:@"color"];
    [titleProxy setValue:@"white" forKey:@"shadowColor"];
    [titleProxy setValue:[NSDictionary dictionaryWithObjectsAndKeys:@"0",@"x",@"1",@"y", nil] forKey:@"shadowOffset"];
    
    LayoutConstraint *viewLayout = [titleProxy layoutProperties];
    viewLayout->width = TiDimensionAutoFill;
    viewLayout->height = TiDimensionAutoSize;
    viewLayout->top = TiDimensionDip(10.0);
    viewLayout->bottom = TiDimensionDip(10.0);
    viewLayout->left = ([tableView style] == UITableViewStyleGrouped) ? TiDimensionDip(15.0) : TiDimensionDip(10.0);
    viewLayout->right = ([tableView style] == UITableViewStyleGrouped) ? TiDimensionDip(15.0) : TiDimensionDip(10.0);

    return [titleProxy autorelease];
}

+ (UITableViewRowAnimation)animationStyleForProperties:(NSDictionary*)properties
{
	BOOL found;
	UITableViewRowAnimation animationStyle = [TiUtils intValue:@"animationStyle" properties:properties def:UITableViewRowAnimationNone exists:&found];
	if (found) {
		return animationStyle;
	}
	BOOL animate = [TiUtils boolValue:@"animated" properties:properties def:NO];
	return animate ? UITableViewRowAnimationFade : UITableViewRowAnimationNone;
}

@end



#endif
