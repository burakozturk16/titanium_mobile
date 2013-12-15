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

#define GROUPED_MARGIN_WIDTH 18.0

@interface TiUIView(eventHandler);
-(void)handleListenerRemovedWithEvent:(NSString *)event;
-(void)handleListenerAddedWithEvent:(NSString *)event;
@end

@interface TiUIListView ()
@property (nonatomic, readonly) TiUIListViewProxy *listViewProxy;
@end

static TiViewProxy * FindViewProxyWithBindIdContainingPoint(UIView *view, CGPoint point);

@implementation TiUIListView {
    TDUITableView *_tableView;
    NSDictionary *_templates;
    id _defaultItemTemplate;

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
    UISearchDisplayController *searchController;

    NSMutableArray * sectionTitles;
    NSMutableArray * sectionIndices;
    NSMutableArray * filteredTitles;
    NSMutableArray * filteredIndices;

    UIView *_pullViewWrapper;
    CGFloat pullThreshhold;

    BOOL pullActive;
    CGPoint tapPoint;
    BOOL editing;
    BOOL pruneSections;

    BOOL caseInsensitiveSearch;
    NSString* searchString;
    BOOL searchActive;
    BOOL keepSectionsInSearch;
    NSMutableArray* _searchResults;
    UIEdgeInsets _defaultSeparatorInsets;
    
    NSMutableDictionary* _measureProxies;
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
    }
    return self;
}

- (void)dealloc
{
    _tableView.delegate = nil;
    _tableView.dataSource = nil;
    RELEASE_TO_NIL(_tableView);
    RELEASE_TO_NIL(_templates);
    RELEASE_TO_NIL(_defaultItemTemplate);
    RELEASE_TO_NIL(_searchResults);
    RELEASE_TO_NIL(_pullViewWrapper);
    RELEASE_TO_NIL(_pullViewProxy);
    RELEASE_TO_NIL(_headerViewProxy);
    RELEASE_TO_NIL(_searchWrapper);
    RELEASE_TO_NIL(_headerWrapper)
    RELEASE_TO_NIL(_footerViewProxy);
    RELEASE_TO_NIL(searchViewProxy);
    RELEASE_TO_NIL(tableController);
    RELEASE_TO_NIL(searchController);
    RELEASE_TO_NIL(sectionTitles);
    RELEASE_TO_NIL(sectionIndices);
    RELEASE_TO_NIL(filteredTitles);
    RELEASE_TO_NIL(filteredIndices);
    RELEASE_TO_NIL(_measureProxies);
#ifdef USE_TI_UIREFRESHCONTROL
    RELEASE_TO_NIL(_refreshControlProxy);
#endif
    [super dealloc];
}

-(TiViewProxy*)initWrapperProxy
{
    TiViewProxy* theProxy = [[TiViewProxy alloc] init];
    [theProxy setDefaultReadyToCreateView:YES];
    LayoutConstraint* viewLayout = [theProxy layoutProperties];
    viewLayout->width = TiDimensionAutoFill;
    viewLayout->height = TiDimensionAutoSize;
    return theProxy;
}

-(void)setHeaderFooter:(TiViewProxy*)theProxy isHeader:(BOOL)header
{
    [theProxy setProxyObserver:self];
    if (header) {
        [self.tableView setTableHeaderView:[theProxy getOrCreateView]];
    } else {
        [self.tableView setTableFooterView:[theProxy getOrCreateView]];
    }
    [theProxy windowWillOpen];
    [theProxy setParentVisible:YES];
    [theProxy windowDidOpen];
}

-(void)configureFooter
{
    if (_footerViewProxy == nil) {
        _footerViewProxy = [self initWrapperProxy];
        [self setHeaderFooter:_footerViewProxy isHeader:NO];
    }
    
}

-(void)configureHeaders
{
    _headerViewProxy = [self initWrapperProxy];
    LayoutConstraint* viewLayout = [_headerViewProxy layoutProperties];
    viewLayout->layoutStyle = TiLayoutRuleVertical;
    
    _searchWrapper = [self initWrapperProxy];
    _headerWrapper = [self initWrapperProxy];

    [_headerViewProxy add:_searchWrapper];
    [_headerViewProxy add:_headerWrapper];
    
    [self setHeaderFooter:_headerViewProxy isHeader:YES];
}

- (TDUITableView *)tableView
{
    if (_tableView == nil) {
        UITableViewStyle style = UITableViewStylePlain;
        if (![TiUtils isIOS7OrGreater]) {
            style = [TiUtils intValue:[self.proxy valueForKey:@"style"] def:style];
        }

        _tableView = [[TDUITableView alloc] initWithFrame:self.bounds style:style];
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
            [[self class] setBackgroundColor:[TiUtils colorValue:backgroundColor] onTable:_tableView];
        }
        UITapGestureRecognizer *tapGestureRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
        tapGestureRecognizer.delegate = self;
        [_tableView addGestureRecognizer:tapGestureRecognizer];
        [tapGestureRecognizer release];

        [self configureHeaders];
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
    if (![searchController isActive]) {
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

-(void)proxyDidRelayout:(id)sender
{
    TiThreadPerformOnMainThread(^{
        if (sender == _headerViewProxy) {
            UIView* headerView = [[self tableView] tableHeaderView];
            [headerView setFrame:[headerView bounds]];
            [[self tableView] setTableHeaderView:headerView];
        } else if (sender == _footerViewProxy) {
            UIView *footerView = [[self tableView] tableFooterView];
            [footerView setFrame:[footerView bounds]];
            [[self tableView] setTableFooterView:footerView];
        } else if (sender == _pullViewProxy) {
            pullThreshhold = ([_pullViewProxy view].frame.origin.y - _pullViewWrapper.bounds.size.height);
        }
    },NO);
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
		TiViewTemplate *template = [TiViewTemplate templateFromViewTemplate:obj];
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

-(TiUIView*)sectionView:(NSInteger)section forLocation:(NSString*)location section:(TiUIListSectionProxy**)sectionResult
{
    TiUIListSectionProxy *proxy = [self.listViewProxy sectionForIndex:section];
    //In the event that proxy is nil, this all flows out to returning nil safely anyways.
    if (sectionResult!=nil) {
        *sectionResult = proxy;
    }
    id value = [proxy valueForKey:location];
    TiViewProxy* viewproxy = nil;
    if ([value isKindOfClass:[TiViewProxy class]]) {
        viewproxy = value;
    }
    else if ([value isKindOfClass:[NSDictionary class]]) {
        id<TiEvaluator> context = proxy.executionContext;
        if (context == nil) {
            context = proxy.pageContext;
        }
        viewproxy = [[TiViewProxy class] unarchiveFromDictionary:value rootProxy:proxy inContext:context];
        [context.krollContext invokeBlockOnThread:^{
            [proxy rememberProxy:viewproxy];
            [viewproxy forgetSelf];
        }];
    }
    
    if (viewproxy!=nil) {
        LayoutConstraint *viewLayout = [viewproxy layoutProperties];
        //If height is not dip, explicitly set it to SIZE
        if (viewLayout->height.type != TiDimensionTypeDip) {
            viewLayout->height = TiDimensionAutoSize;
        }
        
        TiUIView* theView = [viewproxy getOrCreateView];
        if (![viewproxy viewAttached]) {
            [viewproxy windowWillOpen];
            [viewproxy willShow];
            [viewproxy windowDidOpen];
        }
        return theView;
    }
    return nil;
}

-(void)scrollToTop:(NSInteger)top animated:(BOOL)animated
{
	[_tableView setContentOffset:CGPointMake(0,top) animated:animated];
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
	[_tableView setContentOffset:CGPointMake(0,0) animated:animated];
}

-(void)showPullView:(NSNumber*)anim
{
    BOOL animated = YES;
	if (anim != nil)
		animated = [anim boolValue];
	[_tableView setContentOffset:CGPointMake(0,pullThreshhold) animated:animated];
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
            TiViewTemplate *template = [_templates objectForKey:templateId];
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
    searchActive = ([searchString length] > 0);
    RELEASE_TO_NIL(filteredIndices);
    RELEASE_TO_NIL(filteredTitles);
    if (searchActive) {
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
                if (theValue!=nil && [[TiUtils stringValue:theValue] rangeOfString:searchString options:searchOpts].location != NSNotFound) {
                    (thisSection != nil) ? [thisSection addObject:thePath] : [singleSection addObject:thePath];
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
        
    } else {
        RELEASE_TO_NIL(_searchResults);
    }
}

-(BOOL) isSearchActive
{
    return searchActive || [searchController isActive];
}

- (void)updateSearchResults:(id)unused
{
    if (searchActive) {
        [self buildResultsForSearchText];
    }
    if ([searchController isActive]) {
        [[searchController searchResultsTableView] reloadData];
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
        if (![searchController isActive]) {
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

- (void)setBackgroundColor_:(id)arg
{
	if (_tableView != nil) {
		[[self class] setBackgroundColor:[TiUtils colorValue:arg] onTable:_tableView];
	}
}

- (void)setHeaderTitle_:(id)args
{
    [_headerWrapper removeAllChildren:nil];
    TiViewProxy *theProxy = [[self class] titleViewForText:[TiUtils stringValue:args] inTable:[self tableView] footer:NO];
    [_headerWrapper add:theProxy];
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
        [_footerViewProxy add:theProxy];
    }
}

-(void)setHeaderView_:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,TiViewProxy);
    [self tableView];
    [_headerWrapper removeAllChildren:nil];
    if (args!=nil) {
        [_headerWrapper add:(TiViewProxy*) args];
    }
}

-(void)setFooterView_:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,TiViewProxy);
    if (IS_NULL_OR_NIL(args)) {
        [_footerViewProxy setProxyObserver:nil];
        [_footerViewProxy windowWillClose];
        [self.tableView setTableFooterView:nil];
        [_footerViewProxy windowDidClose];
        RELEASE_TO_NIL(_footerViewProxy);
    } else {
        [self configureFooter];
        [_footerViewProxy removeAllChildren:nil];
        [_footerViewProxy add:(TiViewProxy*) args];
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
    if ([args isKindOfClass:[NSDictionary class]]) {
        id<TiEvaluator> context = self.proxy.executionContext;
        if (context == nil) {
            context = self.proxy.pageContext;
        }
        args = [[TiViewProxy class] unarchiveFromDictionary:args rootProxy:self.proxy inContext:context];
        [context.krollContext invokeBlockOnThread:^{
            [self.proxy rememberProxy:args];
            [args forgetSelf];
        }];
    }
    ENSURE_SINGLE_ARG_OR_NIL(args,TiViewProxy);
    if (args == nil) {
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
        _pullViewProxy = [args retain];
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
        
        [_pullViewProxy getOrCreateView];
        [_pullViewProxy setProxyObserver:self];
        [_pullViewProxy windowWillOpen];
        [_pullViewWrapper addSubview:[_pullViewProxy view]];
        _pullViewProxy.parentVisible = YES;
        [_pullViewProxy refreshSize];
        [_pullViewProxy willChangeSize];
        [_pullViewProxy windowDidOpen];
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

#pragma mark - Search Support
-(void)setCaseInsensitiveSearch_:(id)args
{
    caseInsensitiveSearch = [TiUtils boolValue:args def:YES];
    if (searchActive) {
        [self buildResultsForSearchText];
        if ([searchController isActive]) {
            [[searchController searchResultsTableView] reloadData];
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
    searchString = [TiUtils stringValue:args];
    [self buildResultsForSearchText];
    [_tableView reloadData];
}

-(void)setSearchView_:(id)args
{
    ENSURE_TYPE_OR_NIL(args,TiUISearchBarProxy);
    [self tableView];
    [searchViewProxy setDelegate:nil];
    RELEASE_TO_NIL(searchViewProxy);
    RELEASE_TO_NIL(tableController);
    RELEASE_TO_NIL(searchController);
    [_searchWrapper removeAllChildren:nil];

    if (args != nil) {
        searchViewProxy = [args retain];
        [searchViewProxy setDelegate:self];
        tableController = [[UITableViewController alloc] init];
        [TiUtils configureController:tableController withObject:nil];
        tableController.tableView = [self tableView];
		[tableController setClearsSelectionOnViewWillAppear:!allowsSelection];
        searchController = [[UISearchDisplayController alloc] initWithSearchBar:[searchViewProxy searchBar] contentsController:tableController];
        searchController.searchResultsDataSource = self;
        searchController.searchResultsDelegate = self;
        searchController.delegate = self;
        [_searchWrapper add:searchViewProxy];
        keepSectionsInSearch = NO;
    } else {
        keepSectionsInSearch = [TiUtils boolValue:[self.proxy valueForKey:@"keepSectionsInSearch"] def:NO];
    }
    
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
        
        //Delete Data
        [theSection deleteItemAtIndex:indexPath.row];
        
        //Fire the delete Event if required
        NSString *eventName = @"delete";
        if ([self.proxy _hasListeners:eventName]) {
        
            NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                                theSection, @"section",
                                                NUMINT(indexPath.section), @"sectionIndex",
                                                NUMINT(indexPath.row), @"itemIndex",
                                                nil];
            id propertiesValue = [theItem objectForKey:@"properties"];
            NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
            id itemId = [properties objectForKey:@"itemId"];
            if (itemId != nil) {
                [eventObject setObject:itemId forKey:@"itemId"];
            }
            [self.proxy fireEvent:eventName withObject:eventObject withSource:self.proxy propagate:NO reportSuccess:NO errorCode:0 message:nil];
            [eventObject release];
        }
        [theItem release];
        
        BOOL emptySection = NO;
        
        if ([theSection itemCount] == 0) {
            emptySection = YES;
            if (pruneSections) {
                [self.listViewProxy deleteSectionAtIndex:indexPath.section];
            }
        }
        
        BOOL emptyTable = NO;
        NSUInteger sectionCount = [[self.listViewProxy sectionCount] unsignedIntValue];
        if ( sectionCount == 0) {
            emptyTable = YES;
        }
        
        //Reload the data now.
        [tableView beginUpdates];
        if (emptyTable) {
            //Table is empty. Just reload fake section with FADE animation to clear out header and footers
            NSIndexSet *theSet = [NSIndexSet indexSetWithIndex:0];
            [tableView reloadSections:theSet withRowAnimation:UITableViewRowAnimationFade];
        } else if (emptySection) {
            //Section is empty.
            if (pruneSections) {
                //Delete the section
                
                BOOL needsReload = (indexPath.section < sectionCount);
                //If this is not the last section we need to set indices for all the sections coming in after this that are visible.
                //Otherwise the events will not work properly since the indexPath stored in the cell will be incorrect.
                
                if (needsReload) {
                    NSArray* visibleRows = [tableView indexPathsForVisibleRows];
                    [visibleRows enumerateObjectsUsingBlock:^(NSIndexPath *vIndexPath, NSUInteger idx, BOOL *stop) {
                        if (vIndexPath.section > indexPath.section) {
                            //This belongs to the next section. So set the right indexPath otherwise events wont work properly.
                            NSIndexPath *newIndex = [NSIndexPath indexPathForRow:vIndexPath.row inSection:(vIndexPath.section -1)];
                            UITableViewCell* theCell = [tableView cellForRowAtIndexPath:vIndexPath];
                            if ([theCell isKindOfClass:[TiUIListItem class]]) {
                                ((TiUIListItem*)theCell).proxy.indexPath = newIndex;
                            }
                        }
                    }];
                }
                NSIndexSet *deleteSet = [NSIndexSet indexSetWithIndex:indexPath.section];
                [tableView deleteSections:deleteSet withRowAnimation:UITableViewRowAnimationFade];
            } else {
                //Just delete the row. Section stays
                [tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:UITableViewRowAnimationFade];
            }
        } else {
            //Just delete the row.
            BOOL needsReload = (indexPath.row < [theSection itemCount]);
            //If this is not the last row need to set indices for all rows in the section following this row.
            //Otherwise the events will not work properly since the indexPath stored in the cell will be incorrect.
            
            if (needsReload) {
                NSArray* visibleRows = [tableView indexPathsForVisibleRows];
                [visibleRows enumerateObjectsUsingBlock:^(NSIndexPath *vIndexPath, NSUInteger idx, BOOL *stop) {
                    if ( (vIndexPath.section == indexPath.section) && (vIndexPath.row > indexPath.row) ) {
                        //This belongs to the same section. So set the right indexPath otherwise events wont work properly.
                        NSIndexPath *newIndex = [NSIndexPath indexPathForRow:(vIndexPath.row - 1) inSection:(vIndexPath.section)];
                        UITableViewCell* theCell = [tableView cellForRowAtIndexPath:vIndexPath];
                        if ([theCell isKindOfClass:[TiUIListItem class]]) {
                            ((TiUIListItem*)theCell).proxy.indexPath = newIndex;
                        }
                    }
                }];
            }
            [tableView deleteRowsAtIndexPaths:[NSArray arrayWithObject:indexPath] withRowAnimation:UITableViewRowAnimationFade];
        
        }
        [tableView endUpdates];
        [theSection release];
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
            [self.proxy fireEvent:eventName withObject:eventObject withSource:self.proxy propagate:NO reportSuccess:NO errorCode:0 message:nil];
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
            [self.proxy fireEvent:eventName withObject:eventObject];
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
        cellProxy.parentForBubbling = (TiViewProxy*)self.proxy;
        if ([templateId isKindOfClass:[NSNumber class]]) {
            UITableViewCellStyle cellStyle = [templateId unsignedIntegerValue];
            cell = [[TiUIListItem alloc] initWithStyle:cellStyle position:position grouped:grouped reuseIdentifier:cellIdentifier proxy:cellProxy];
        } else {
            cell = [[TiUIListItem alloc] initWithProxy:cellProxy position:position grouped:grouped reuseIdentifier:cellIdentifier];
            id template = [_templates objectForKey:templateId];
            if (template != nil) {
                [cellProxy unarchiveFromTemplate:template withEvents:YES];
            }
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
    TiUIView *view = [self sectionView:realSection forLocation:@"headerView" section:nil];
	
    CGFloat size = 0.0;
    if (view!=nil) {
        TiViewProxy* viewProxy = (TiViewProxy*) [view proxy];
        LayoutConstraint *viewLayout = [viewProxy layoutProperties];
        switch (viewLayout->height.type)
        {
            case TiDimensionTypeDip:
                size += viewLayout->height.value;
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
    TiUIView *view = [self sectionView:realSection forLocation:@"footerView" section:nil];
	
    CGFloat size = 0.0;
    if (view!=nil) {
        TiViewProxy* viewProxy = (TiViewProxy*) [view proxy];
        LayoutConstraint *viewLayout = [viewProxy layoutProperties];
        switch (viewLayout->height.type)
        {
            case TiDimensionTypeDip:
                size += viewLayout->height.value;
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

- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    NSIndexPath* realIndexPath = [self pathForSearchPath:indexPath];
    
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
        TiUIListItemProxy *cellProxy = [_measureProxies objectForKey:templateId];
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
	if ([self.proxy _hasListeners:@"scroll"])
	{
        NSArray* visibles = [_tableView indexPathsForVisibleRows];
        NSMutableDictionary* event = [self eventObjectForScrollView:scrollView];
        [event setObject:NUMINT(((NSIndexPath*)[visibles objectAtIndex:0]).row) forKey:@"firstVisibleItem"];
        [event setObject:NUMINT([visibles count]) forKey:@"visibleItemCount"];
		[self.proxy fireEvent:@"scroll" withObject:event];
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
        if (pullChanged && [self.proxy _hasListeners:@"pullchanged"]) {
            [self.proxy fireEvent:@"pullchanged" withObject:[NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(pullActive),@"active",nil] withSource:self.proxy propagate:NO reportSuccess:NO errorCode:0 message:nil];
        }
        if (scrollView.contentOffset.y <= 0 && [self.proxy _hasListeners:@"pull"]) {
            [self.proxy fireEvent:@"pull" withObject:[NSDictionary dictionaryWithObjectsAndKeys:NUMBOOL(pullActive),@"active",nil] withSource:self.proxy propagate:NO reportSuccess:NO errorCode:0 message:nil];
        }
    }
    
}

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
	// suspend image loader while we're scrolling to improve performance
	[[ImageLoader sharedLoader] suspend];
    if([self.proxy _hasListeners:@"dragstart"])
	{
        [self.proxy fireEvent:@"dragstart" withObject:nil];
    }
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
    if (decelerate==NO)
	{
		// resume image loader when we're done scrolling
		[[ImageLoader sharedLoader] resume];
	}
	if ([self.proxy _hasListeners:@"dragend"])
	{
		[self.proxy fireEvent:@"dragend" withObject:[NSDictionary dictionaryWithObjectsAndKeys:[NSNumber numberWithBool:decelerate],@"decelerate",nil]]	;
	}
    
    
    if ( (_pullViewProxy != nil) && (pullActive == YES) ) {
        pullActive = NO;
        if ([self.proxy _hasListeners:@"pullend"]) {
            [self.proxy fireEvent:@"pullend" withObject:nil withSource:self.proxy propagate:NO reportSuccess:NO errorCode:0 message:nil];
        }
    }
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
	// resume image loader when we're done scrolling
	[[ImageLoader sharedLoader] resume];
	if ([self.proxy _hasListeners:@"scrollend"])
	{
		[self.proxy fireEvent:@"scrollend" withObject:[self eventObjectForScrollView:scrollView]];
	}
}

- (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView
{
	// suspend image loader while we're scrolling to improve performance
	[[ImageLoader sharedLoader] suspend];
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
    UITableView* theTableView = viaSearch ? [searchController searchResultsTableView] : [self tableView];
    CGPoint point = [recognizer locationInView:theTableView];
    NSIndexPath* indexPath = [theTableView indexPathForRowAtPoint:point];
    indexPath = [self pathForSearchPath:indexPath];
    if (indexPath != nil) {
        NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath tableView:theTableView];
        [event setValue:[self swipeStringFromGesture:recognizer] forKey:@"direction"];
        [[self proxy] fireEvent:@"swipe" withObject:event];
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
        UITableView* theTableView = viaSearch ? [searchController searchResultsTableView] : [self tableView];
        CGPoint point = [recognizer locationInView:theTableView];
        NSIndexPath* indexPath = [theTableView indexPathForRowAtPoint:point];
        indexPath = [self pathForSearchPath:indexPath];
        
        NSMutableDictionary *event;
        if (indexPath != nil) {
            NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath tableView:theTableView atPoint:point];
            [[self proxy] fireEvent:@"longpress" withObject:event];
        }
        else {
            [super recognizedLongPress:recognizer];
        }
        
        if (allowsSelection == NO)
        {
            [theTableView deselectRowAtIndexPath:indexPath animated:YES];
        }
    }
}


#pragma mark - UISearchBarDelegate Methods
- (BOOL)searchBarShouldBeginEditing:(UISearchBar *)searchBar
{
    if (_searchWrapper != nil) {
        [_searchWrapper layoutProperties]->right = TiDimensionDip(0);
        [_searchWrapper refreshView:nil];
    }
}

- (void)searchBarTextDidBeginEditing:(UISearchBar *)searchBar
{
    searchString = (searchBar.text == nil) ? @"" : searchBar.text;
    [self buildResultsForSearchText];
    [[searchController searchResultsTableView] reloadData];
}

- (void)searchBarTextDidEndEditing:(UISearchBar *)searchBar
{
    if ([searchBar.text length] == 0) {
        searchString = @"";
        [self buildResultsForSearchText];
        if ([searchController isActive]) {
            [searchController setActive:NO animated:YES];
        }
    }
}

- (void)searchBar:(UISearchBar *)searchBar textDidChange:(NSString *)searchText
{
    searchString = (searchText == nil) ? @"" : searchText;
    [self buildResultsForSearchText];
}

- (void)searchBarSearchButtonClicked:(UISearchBar *)searchBar
{
    [searchBar resignFirstResponder];
    [self makeRootViewFirstResponder];
}

- (void)searchBarCancelButtonClicked:(UISearchBar *) searchBar
{
    searchString = @"";
    [searchBar setText:searchString];
    [self buildResultsForSearchText];
}

#pragma mark - UISearchDisplayDelegate Methods

- (void) searchDisplayControllerDidEndSearch:(UISearchDisplayController *)controller
{
    searchString = @"";
    [self buildResultsForSearchText];
    if ([searchController isActive]) {
        [searchController setActive:NO animated:YES];
    }
    //IOS7 DP3. TableView seems to be adding the searchView to
    //tableView. Bug on IOS7?
    if (_searchWrapper != nil) {
        CGFloat rowWidth = floorf([self computeRowWidth:_tableView]);
        if (rowWidth > 0) {
            CGFloat right = _tableView.bounds.size.width - rowWidth;
            [_searchWrapper layoutProperties]->right = TiDimensionDip(right);
            [_searchWrapper refreshView:nil];
        }
    }
    [searchViewProxy ensureSearchBarHeirarchy];
    [_tableView reloadData];
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

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView  accessoryButtonTapped:(BOOL)accessoryButtonTapped
{
    TiUIListSectionProxy *section = [self.listViewProxy sectionForIndex:indexPath.section];
	NSDictionary *item = [section itemAtIndex:indexPath.row];
    NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
										section, @"section",
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
		TiViewProxy *tapViewProxy = FindViewProxyWithBindIdContainingPoint(contentView, [tableView convertPoint:tapPoint toView:contentView]);
		if (tapViewProxy != nil) {
			[eventObject setObject:[tapViewProxy valueForKey:@"bindId"] forKey:@"bindId"];
		}
	}
    return [eventObject autorelease];
}

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView
{
    return [self EventObjectForItemAtIndexPath:indexPath tableView:tableView accessoryButtonTapped:NO];
}

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath tableView:(UITableView *)tableView atPoint:(CGPoint)point
{
    NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath tableView:tableView];
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
	
	
	[self.proxy fireEvent:eventName withObject:[self EventObjectForItemAtIndexPath:indexPath tableView:tableView]];
}

#pragma mark - UITapGestureRecognizer

- (BOOL)gestureRecognizerShouldBegin:(UIGestureRecognizer *)gestureRecognizer
{
	tapPoint = [gestureRecognizer locationInView:gestureRecognizer.view];
	return NO;
}

- (void)handleTap:(UITapGestureRecognizer *)tapGestureRecognizer
{
	// Never called
}

#pragma mark - Static Methods

+ (void)setBackgroundColor:(TiColor*)color onTable:(UITableView*)table
{
	UIColor* defaultColor = [table style] == UITableViewStylePlain ? [UIColor whiteColor] : [UIColor groupTableViewBackgroundColor];
	UIColor* bgColor = [color _color];
	
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

static TiViewProxy * FindViewProxyWithBindIdContainingPoint(UIView *view, CGPoint point)
{
	if (!CGRectContainsPoint([view bounds], point)) {
		return nil;
	}
	for (UIView *subview in [view subviews]) {
		TiViewProxy *viewProxy = FindViewProxyWithBindIdContainingPoint(subview, [view convertPoint:point toView:subview]);
		if (viewProxy != nil) {
			id bindId = [viewProxy valueForKey:@"bindId"];
			if (bindId != nil) {
				return viewProxy;
			}
		}
	}
	if ([view isKindOfClass:[TiUIView class]]) {
		TiViewProxy *viewProxy = (TiViewProxy *)[(TiUIView *)view proxy];
		id bindId = [viewProxy valueForKey:@"bindId"];
		if (bindId != nil) {
			return viewProxy;
		}
	}
	return nil;
}

#endif
