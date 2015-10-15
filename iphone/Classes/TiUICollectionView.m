/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW
#import "TiUICollectionView.h"
#import "TiUICollectionSectionProxy.h"
#import "TiUICollectionItem.h"
#import "TiUICollectionItemProxy.h"
#import "TiUILabelProxy.h"
#import "TiUISearchBarProxy.h"
#import "TiUISearchBar.h"
#import "ImageLoader.h"
#ifdef USE_TI_UIREFRESHCONTROL
#import "TiUIRefreshControlProxy.h"
#endif
#import "TiCollectionView.h"
#import "TiUIHelper.h"
#import "TiApp.h"
#import "WrapperViewProxy.h"
#import "TiUICollectionWrapperViewProxy.h"
#import "TiUICollectionWrapperView.h"
#import "TiUICollectionViewFlowLayout.h"


@interface TiUIView(eventHandler);
-(void)handleCollectionenerRemovedWithEvent:(NSString *)event;
-(void)handleCollectionenerAddedWithEvent:(NSString *)event;
@end


@interface TiUICollectionView ()
@property (nonatomic, readonly) TiUICollectionViewProxy *listViewProxy;
@property (nonatomic,copy,readwrite) NSString * searchString;
@property (nonatomic, strong) NSMutableSet *shownIndexes;
@end

@interface TiUICollectionSectionProxy()
-(TiViewProxy*)currentViewForLocation:(NSString*)location inCollectionView:(TiUICollectionView*)listView;
@end


@implementation TiUICollectionView {
    TiCollectionView *_tableView;

    id _defaultItemTemplate;
    BOOL hideOnSearch;
    BOOL searchViewAnimating;

    TiDimension _itemWidth;
    TiDimension _minItemWidth;
    TiDimension _maxItemWidth;
    
    TiDimension _itemHeight;
    TiDimension _minItemHeight;
    TiDimension _maxItemHeight;


    UICollectionViewController *tableController;
    TiSearchDisplayController *searchController;

    NSMutableArray * sectionTitles;
    NSMutableArray * sectionIndices;
    NSMutableArray * filteredTitles;
    NSMutableArray * filteredIndices;

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
    
    BOOL _scrollSuspendImageLoading;
    BOOL _scrollHidesKeyboard;
    BOOL hasOnDisplayCell;
    BOOL _updateInsetWithKeyboard;
    
    NSInteger _currentSection;
    
//    BOOL _canSwipeCells;
    BOOL _stickyHeaders;
//    MGSwipeTableCell * _currentSwipeCell;
    
    id _appearAnimation;
    BOOL _useAppearAnimation;
}

static NSDictionary* replaceKeysForRow;
-(NSDictionary *)replaceKeysForRow
{
	if (replaceKeysForRow == nil)
	{
		replaceKeysForRow = [@{@"columnWidth":@"width", @"rowHeight":@"height"} retain];
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
        _scrollHidesKeyboard = NO;
        hideOnSearch = NO;
        searchViewAnimating = NO;
        _updateInsetWithKeyboard = NO;
        _currentSection = -1;
//        _canSwipeCells = NO;
        _stickyHeaders = NO;
        _appearAnimation = nil;
        _useAppearAnimation = NO;
    }
    return self;
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    _tableView.delegate = nil;
    _tableView.dataSource = nil;
    RELEASE_TO_NIL(_tableView)
    RELEASE_TO_NIL(_defaultItemTemplate)
    RELEASE_TO_NIL(_searchString)
    RELEASE_TO_NIL(_searchResults)
    RELEASE_TO_NIL(tableController)
    RELEASE_TO_NIL(searchController)
    RELEASE_TO_NIL(sectionTitles)
    RELEASE_TO_NIL(sectionIndices)
    RELEASE_TO_NIL(filteredTitles)
    RELEASE_TO_NIL(filteredIndices)

    RELEASE_TO_NIL(_appearAnimation)
    RELEASE_TO_NIL(_shownIndexes)
    [super dealloc];
}

-(WrapperViewProxy*)wrapperProxyWithVerticalLayout:(BOOL)vertical
{
    WrapperViewProxy* theProxy = [[WrapperViewProxy alloc] initWithVerticalLayout:vertical];
    [theProxy setParent:(TiParentingProxy*)self.proxy];
    return [theProxy autorelease];
}

-(WrapperViewProxy*)wrapperProxy
{
    return [self wrapperProxyWithVerticalLayout:NO];
}

-(void)setHeaderFooter:(TiViewProxy*)theProxy isHeader:(BOOL)header
{
//    [theProxy setProxyObserver:self];
//    if (header) {
//        [self.tableView setTableHeaderView:[theProxy getAndPrepareViewForOpening:CGRectZero]];
//    } else {
//        [self.tableView setTableFooterView:[theProxy getAndPrepareViewForOpening:CGRectZero]];
//    }
}

-(TiViewProxy*)getOrCreateHeaderHolder
{
    TiViewProxy* vp = [self holdedProxyForKey:@"headerWrapper"];
    if (!vp) {
        vp = (TiViewProxy*)[[self viewProxy] addObjectToHold:@{
                                                               @"layout":@"vertical",
                                                               @"top":@(0),
                                                               @"touchPassThrough":@(YES),
                                                               @"width":@"FILL",
                                                               @"height":@"SIZE"
                                                               } forKey:@"headerWrapper"];
        vp.canBeResizedByFrame = YES;
        [self setHeaderFooter:vp isHeader:YES];
    }
    return vp;
}

- (TDUICollectionView *)tableView
{
    if (_tableView == nil) {
         TiUICollectionViewFlowLayout* layout = [[TiUICollectionViewFlowLayout alloc] init];

        _tableView = [[TiCollectionView alloc] initWithFrame:self.bounds collectionViewLayout:layout];
        [layout release];
//        _tableView.autoresizingMask = UIViewAutoresizingNone;
        _tableView.delegate = self;
        _tableView.dataSource = self;
        _tableView.touchDelegate = self;
        _tableView.alwaysBounceVertical = YES;
        
        id backgroundColor = [self.proxy valueForKey:@"backgroundColor"];
        BOOL doSetBackground = (backgroundColor != nil);
        if (doSetBackground) {
            [[self class] setBackgroundColor:[UIColor clearColor] onTable:_tableView];
        }
//        if ([TiUtils isIOS7OrGreater]) {
//            _defaultSeparatorInsets = [_tableView separatorInset];
//        }
        
        if ([TiUtils isIOS8OrGreater]) {
            [_tableView setLayoutMargins:UIEdgeInsetsZero];
        }
        
        //prevents crash if no template defined for headers/footers
        [_tableView registerClass:[TiUICollectionWrapperView class] forSupplementaryViewOfKind:UICollectionElementKindSectionHeader withReuseIdentifier:@"header"];
        [_tableView registerClass:[TiUICollectionWrapperView class] forSupplementaryViewOfKind:UICollectionElementKindSectionFooter withReuseIdentifier:@"footer"];
    }
    if ([_tableView superview] != self) {
        [self addSubview:_tableView];
    }
    return _tableView;
}
-(UIScrollView*)scrollview {
    return [self tableView];
}


-(void)reloadTableViewData {
//    _canSwipeCells = NO;
    [_tableView reloadData];
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
    [_tableView setFrame:bounds];
    if (!searchViewAnimating) {
        
        TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
        if (searchViewProxy) {
            [searchViewProxy ensureSearchBarHeirarchy];
            CGFloat rowWidth = [self computeRowWidth:_tableView];
            if (rowWidth > 0) {
                CGFloat right = _tableView.bounds.size.width - rowWidth;
                [searchViewProxy layoutProperties]->right = TiDimensionDip(right);
            }
        }
    } else {
        [_tableView reloadData];
    }
    [super frameSizeChanged:frame bounds:bounds];
    
    TiViewProxy* vp = [self holdedProxyForKey:@"headerWrapper"];
    if (vp) {
        [vp parentSizeWillChange];
    }
    vp = [self holdedProxyForKey:@"footerView"];
    if (vp) {
        [vp parentSizeWillChange];
    }
}

- (id)accessibilityElement
{
	return self.tableView;
}

- (TiUICollectionViewProxy *)listViewProxy
{
	return (TiUICollectionViewProxy *)self.proxy;
}

- (void)selectItem:(NSIndexPath*)indexPath animated:(BOOL)animated
{
    //    [self tableView:_tableView willSelectRowAtIndexPath:indexPath];
    UICollectionViewScrollPosition pos = UICollectionViewScrollPositionCenteredVertically;
    [_tableView scrollToItemAtIndexPath:indexPath atScrollPosition:pos animated:animated];
    [_tableView selectItemAtIndexPath:indexPath animated:animated scrollPosition:pos];
//    [self tableView:_tableView didSelectRowAtIndexPath:indexPath];
}
- (void)deselectItem:(NSIndexPath*)indexPath animated:(BOOL)animated
{
    [_tableView deselectItemAtIndexPath:indexPath animated:animated];
}

- (void)deselectAll:(BOOL)animated
{
	if (_tableView != nil) {
		[_tableView.indexPathsForSelectedItems enumerateObjectsUsingBlock:^(NSIndexPath *indexPath, NSUInteger idx, BOOL *stop) {
			[_tableView deselectItemAtIndexPath:indexPath animated:animated];
		}];
	}
}

- (void) updateIndicesForVisibleRows
{
    if (_tableView == nil || [self isSearchActive]) {
        return;
    }
    
    NSArray* visibleRows = [_tableView indexPathsForVisibleItems];
    [visibleRows enumerateObjectsUsingBlock:^(NSIndexPath *vIndexPath, NSUInteger idx, BOOL *stop) {
        UICollectionViewCell* theCell = [_tableView cellForItemAtIndexPath:vIndexPath];
        if ([theCell isKindOfClass:[TiUICollectionItem class]]) {
            ((TiUICollectionItem*)theCell).proxy.indexPath = vIndexPath;
//            [((TiUICollectionItem*)theCell) ensureVisibleSelectorWithTableView:_tableView];
        }
    }];
}

//-(void)proxyDidRelayout:(id)sender
//{
//    NSArray* keys = [[self viewProxy] allKeysForHoldedProxy:sender];
//    if ([keys count] > 0) {
//        NSString* key = [keys objectAtIndex:0];
//
////        if ([key isEqualToString:@"headerWrapper"]) {
////            UIView* headerView = [[self tableView] tableHeaderView];
////            [headerView setFrame:[headerView bounds]];
////            [[self tableView] setTableHeaderView:headerView];
////            [((TiUICollectionViewProxy*)[self proxy]) contentsWillChange];
////        } else if ([key isEqualToString:@"footerView"]) {
////            UIView* footerView = [[self tableView] tableFooterView];
////            [footerView setFrame:[footerView bounds]];
////            [[self tableView] setTableFooterView:footerView];
////            [((TiUICollectionViewProxy*)[self proxy]) contentsWillChange];
//        }
//    }
//}

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
	[self.listViewProxy.templates enumerateKeysAndObjectsUsingBlock:^(NSString *key, id obj, BOOL *stop) {

            NSString *cellIdentifier = [key isKindOfClass:[NSNumber class]] ? [NSString stringWithFormat:@"TiUIListView__internal%@", key]: [key description];
            
            
            if ([cellIdentifier rangeOfString:@"header" options:NSCaseInsensitiveSearch].location != NSNotFound) {
                [[self tableView] registerClass:[TiUICollectionWrapperView class] forSupplementaryViewOfKind:UICollectionElementKindSectionHeader withReuseIdentifier:cellIdentifier];
            } else if ([cellIdentifier rangeOfString:@"footer" options:NSCaseInsensitiveSearch].location != NSNotFound) {
                [[self tableView] registerClass:[TiUICollectionWrapperView class] forSupplementaryViewOfKind:UICollectionElementKindSectionFooter withReuseIdentifier:cellIdentifier];
            } else {
                [[self tableView] registerClass:[TiUICollectionItem class] forCellWithReuseIdentifier:cellIdentifier];
            }
	}];
    if (configurationSet) {
        [self reloadTableViewData];
    }
}

-(TiViewProxy*)sectionViewProxy:(NSInteger)section forLocation:(NSString*)location
{
    TiUICollectionSectionProxy *sectionProxy = [self.listViewProxy sectionForIndex:section];
    return [sectionProxy sectionViewForLocation:location inCollectionView:self];
}

-(TiViewProxy*)currentSectionViewProxy:(NSInteger)section forLocation:(NSString*)location
{
    TiUICollectionSectionProxy *sectionProxy = [self.listViewProxy sectionForIndex:section];
    return [sectionProxy currentViewForLocation:location inCollectionView:self];
}

-(UIView*)sectionView:(NSInteger)section forLocation:(NSString*)location section:(TiUICollectionSectionProxy**)sectionResult
{
    TiViewProxy* viewproxy = [self sectionViewProxy:section forLocation:location];
    if (viewproxy!=nil) {
        return [viewproxy getAndPrepareViewForOpening:self.tableView.bounds];
    }
    return nil;
}

//- (CGFloat)heightForSection:(NSInteger)section
//{
//    return [self heightForSection:section estimated:NO];
//}
//
//- (CGFloat)heightForSection:(NSInteger)section estimated:(BOOL)estimated
//{
//    CGFloat height = 0;
//    
//
//    NSInteger rowsInSection = [_tableView numberOfItemsInSection:section];
//    
//    for ( NSInteger i = 0; i < rowsInSection; i++ ) {
//        height += [self heightForRowAtIndexPath:[NSIndexPath indexPathForItem:i inSection:section] estimated:estimated];
//    }
//    
//    if ( rowsInSection > 1 ) {
//        height += [self rowSpacingForSection:section] * ( rowsInSection - 1 );
//    }
//    
//    // header and footer - will be zero if not set/implemented in delegate
//    height += [self headerHeightForSection:section];
//    height += [self footerHeightForSection:section];
//    
//    // Insets
//    UIEdgeInsets insets = [self insetsForSection:section];
//    height += insets.top + insets.bottom;
//    
//    if ( !estimated ) {
//        [self.sectionHeightCache setObject:@(height) forKey:@(section)];
//    }
//    
//    return height;
//}


-(CGSize)contentSizeForSize:(CGSize)size
{
    if (_tableView == nil) {
        return CGSizeZero;
    }
    
//    CGSize refSize = CGSizeMake(size.width, 1000);
//    
//    CGFloat resultHeight = 0;
//    
//    //Last Section rect
//    NSInteger lastSectionIndex = [self numberOfSectionsInCollectionView:_tableView] - 1;
//    if (lastSectionIndex >= 0) {
//        CGRect refRect = [_tableView rectForSection:lastSectionIndex];
//        resultHeight += refRect.size.height + refRect.origin.y;
//    } else {
//        //Header auto height when no sections
//        if (_headerViewProxy != nil) {
//            resultHeight += [_headerViewProxy contentSizeForSize:refSize].height;
//        }
//    }
//    
//    //Footer auto height
//    if (_footerViewProxy) {
//        resultHeight += [_footerViewProxy contentSizeForSize:refSize].height;
//    }
//    refSize.height = resultHeight;
//    
//    return refSize;
    return  size;
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
    TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
    if ([[searchViewProxy view] isFirstResponder]) {
        [searchViewProxy blur:nil];
    }
    
    // This logic here is contingent on search controller deactivation
    // (-[TiUITableView searchDisplayControllerDidEndSearch:]) triggering a hide;
    // doing this ensures that:
    //
    // * The hide when the search controller was active is animated
    // * The animation only occurs once
    
    searchActive = NO;
    if (![(TiViewProxy*)self.proxy viewReady]) {
        return;
    }
    NSArray* visibleRows = [_tableView indexPathsForVisibleItems];
    
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


-(BOOL)shouldHighlightCurrentCollectionItem {
    return [_tableView shouldHighlightCurrentCollectionItem];
}

#pragma mark - Helper Methods

-(CGFloat)computeRowWidth:(UICollectionView*)tableView
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

-(id)valueWithKey:(NSString*)key forSection:(NSInteger)sectionIndex forLocation:(NSString*)location
{
    NSDictionary *item = [[self.listViewProxy sectionForIndex:sectionIndex] valueForKey:location];
    id propertiesValue = [item objectForKey:@"properties"];
    NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
    id theValue = [properties objectForKey:key];
    if (theValue == nil) {
        id templateId = [item objectForKey:@"template"];
        if (templateId == nil) {
            templateId = _defaultItemTemplate;
        }
        if (![templateId isKindOfClass:[NSNumber class]]) {
            TiProxyTemplate *template = [self.listViewProxy.templates objectForKey:templateId];
            theValue = [template.properties objectForKey:key];
        }
        if (theValue == nil) {
            theValue = [self.proxy valueForKey:key];
        }
    }
    
    return theValue;
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
            TiProxyTemplate *template = [self.listViewProxy.templates objectForKey:templateId];
            theValue = [template.properties objectForKey:replaceKey];
        }
        if (theValue == nil) {
            theValue = [self.proxy valueForKey:key];
        }
    }
    
    return theValue;
}

-(id)firstItemValueForKeys:(NSArray*)keys atIndexPath:(NSIndexPath*)indexPath
{
    NSDictionary *item = [[self.listViewProxy sectionForIndex:indexPath.section] itemAtIndex:indexPath.row];
    NSDictionary* properties = [item objectForKey:@"properties"]?[item objectForKey:@"properties"]:item;
    __block id theResult = nil;
    [keys enumerateObjectsUsingBlock:^(NSString* key, NSUInteger idx, BOOL *stop) {
        theResult = [properties objectForKey:key];
        if (theResult) {
            *stop = YES;
        }
    }];
    return theResult;
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
                id theValue = [self firstItemValueForKeys:@[@"searchableText", @"title"] atIndexPath:thePath];
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
                            [filteredIndices addObject:NUMINTEGER([_searchResults count] -1)];
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
    return searchActive;
}

- (void)updateSearchResults:(id)unused
{
    if (searchActive) {
        [self buildResultsForSearchText];
    }
    [self reloadTableViewData];
}

-(NSIndexPath*)pathForSearchPath:(NSIndexPath*)indexPath
{
    if (_searchResults != nil && [_searchResults count] > indexPath.section) {
        NSArray* sectionResults = [_searchResults objectAtIndex:indexPath.section];
        if([sectionResults count] > indexPath.row) {
            return [sectionResults objectAtIndex:indexPath.row];
        }
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

-(void)setPruneSectionsOnEdit_:(id)args
{
    pruneSections = [TiUtils boolValue:args def:NO];
}

-(void)setScrollDirection_:(id)args
{
    UICollectionViewScrollDirection direction = ([args isKindOfClass:[NSString class]] && [args caseInsensitiveCompare:@"horizontal"]== NSOrderedSame)?UICollectionViewScrollDirectionHorizontal:UICollectionViewScrollDirectionVertical;
    TiUICollectionViewFlowLayout* layout = (TiUICollectionViewFlowLayout*)[self tableView].collectionViewLayout;
    layout.scrollDirection = direction;

    _tableView.alwaysBounceVertical = direction == UICollectionViewScrollDirectionVertical;
    _tableView.alwaysBounceHorizontal = direction == UICollectionViewScrollDirectionHorizontal;
    [layout invalidateLayout];
}


-(void)setStickyHeaders_:(id)args
{
    TiUICollectionViewFlowLayout* layout = (TiUICollectionViewFlowLayout*)[self tableView].collectionViewLayout;
    _stickyHeaders = [TiUtils boolValue:args def:YES];
    [layout invalidateLayout];
}


//-(void)setSeparatorStyle_:(id)arg
//{
//    [[self tableView] setSeparatorStyle:[TiUtils intValue:arg]];
//}

//-(void)setSeparatorColor_:(id)arg
//{
//    TiColor *color = [TiUtils colorValue:arg];
//    [[self tableView] setSeparatorColor:[color _color]];
//}

- (void)setDefaultItemTemplate_:(id)args
{
	if (![args isKindOfClass:[NSString class]] && ![args isKindOfClass:[NSNumber class]]) {
		ENSURE_TYPE_OR_NIL(args,NSString);
	}
	[_defaultItemTemplate release];
	_defaultItemTemplate = [args copy];
    if (configurationSet) {
        [self reloadTableViewData];
    }
}

- (void)setColumnWidth_:(id)height
{
	_itemWidth = [TiUtils dimensionValue:height];
}

- (void)setMinColumnWidth_:(id)height
{
	_minItemWidth = [TiUtils dimensionValue:height];
}

- (void)setMaxColumnWidth_:(id)height
{
	_maxItemWidth = [TiUtils dimensionValue:height];
}

- (void)setRowHeight_:(id)height
{
    _itemHeight = [TiUtils dimensionValue:height];
}

- (void)setMinRowHeight_:(id)height
{
    _minItemHeight = [TiUtils dimensionValue:height];
}

- (void)setMaxRowmHeight_:(id)height
{
    _maxItemHeight = [TiUtils dimensionValue:height];
}


-(void)onCreateCustomBackground
{
    if (_tableView != nil) {
		[[self class] setBackgroundColor:[UIColor clearColor] onTable:_tableView];
	}
}


- (void)setHeaderTitle_:(id)args
{
    NSString* text = [TiUtils stringValue:args];
    if (text) {
        TiViewProxy* vp = (TiViewProxy*)[[self viewProxy] addObjectToHold:@{
                                                                            @"type":@"Ti.UI.Label",
                                                                            @"font":@{@"size":@(15)},
                                                                            @"padding":@{@"left":@(15), @"right":@(15)},
                                                                            @"width":@"FILL",
                                                                            @"height":@(50),
                                                                            @"autocapitalization":@(YES),
                                                                            @"text":[TiUtils stringValue:args],
                                                                            @"color":@"#333333",
                                                                            } forKey:@"headerView"];
        [vp setProxyObserver:self];
        [[self getOrCreateHeaderHolder] addProxy:vp atIndex:1 shouldRelayout:YES];
    } else {
        [[self viewProxy] removeHoldedProxyForKey:@"headerView"];
    }
}

- (void)setFooterTitle_:(id)args
{
    NSString* text = [TiUtils stringValue:args];
    if (text) {
        TiViewProxy* vp = (TiViewProxy*)[[self viewProxy] addObjectToHold:@{
                                                                            @"type":@"Ti.UI.Label",
                                                                            @"font":@{@"size":@(14)},
                                                                            @"padding":@{@"left":@(15), @"right":@(15), @"top":@(10), @"bottom":@(10)},
                                                                            @"width":@"FILL",
                                                                            @"height":@"SIZE",
                                                                            @"text":[TiUtils stringValue:args],
                                                                            @"color":@"#6E6E6E",
                                                                            } forKey:@"footerView"];
        [vp setProxyObserver:self];
        vp.canBeResizedByFrame = YES;
        [self setHeaderFooter:vp isHeader:NO];
    } else {
        [self setHeaderFooter:nil isHeader:NO];
        
    }
}

-(TiViewProxy*)holdedProxyForKey:(NSString*)key
{
    return (TiViewProxy*)[[self viewProxy] holdedProxyForKey:key];
}

-(void)setHeaderView_:(id)args
{
    id vp = [[self viewProxy] addObjectToHold:args forKey:@"headerView"];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        [(TiViewProxy*)vp setProxyObserver:self];
        [[self getOrCreateHeaderHolder] addProxy:vp atIndex:1 shouldRelayout:YES];
    }
}

-(void)setFooterView_:(id)args
{
    id vp = [[self viewProxy] addObjectToHold:args forKey:@"footerView"];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        [(TiViewProxy*)vp setProxyObserver:self];
        ((TiViewProxy*)vp).canBeResizedByFrame = YES;
        [self setHeaderFooter:(TiViewProxy*)vp isHeader:NO];
    }
    else {
        [self setHeaderFooter:nil isHeader:NO];
    }
}

-(void)setRefreshControl_:(id)args
{
#ifdef USE_TI_UIREFRESHCONTROL
    id vp = [[self viewProxy] addObjectToHold:args forKey:@"refreshControl"];
    if (IS_OF_CLASS(vp, TiUIRefreshControlProxy)) {
        [[self tableView] addSubview:[(TiUIRefreshControlProxy*)vp control]];
    }
#endif
}


-(void)setKeepSectionsInSearch_:(id)args
{
    TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
    if (searchViewProxy) {
        keepSectionsInSearch = [TiUtils boolValue:args def:NO];
        if (searchActive) {
            [self buildResultsForSearchText];
            [self reloadTableViewData];
        }
    } else {
        keepSectionsInSearch = NO;
    }
}

-(void)setAllowsSelection_:(id)value
{
    allowsSelection = [TiUtils boolValue:value];
    [tableController setClearsSelectionOnViewWillAppear:!allowsSelection];
}

//-(void)setAllowsSelectionDuringEditing_:(id)arg
//{
//	[[self tableView] setAllowsSelectionDuringEditing:[TiUtils boolValue:arg def:NO]];
//}

//-(void)setEditing_:(id)args
//{
//    if ([TiUtils boolValue:args def:NO] != editing) {
//        editing = !editing;
//        [[self tableView] beginUpdates];
//        [_tableView setEditing:editing animated:YES];
//        [_tableView endUpdates];
//    }
//}

-(void)setScrollSuspendsImageLoading_:(id)value
{
    _scrollSuspendImageLoading = [TiUtils boolValue:value def:_scrollSuspendImageLoading];
}


-(void)setScrollHidesKeyboard_:(id)value
{
    _scrollHidesKeyboard = [TiUtils boolValue:value def:_scrollHidesKeyboard];
}

-(void)setOnDisplayCell_:(id)callback
{
    hasOnDisplayCell = [callback isKindOfClass:[KrollCallback class]] || [callback isKindOfClass:[KrollWrapper class]];
}

-(void)setAppearAnimation_:(id)value
{
    RELEASE_TO_NIL(_appearAnimation)
    if (IS_OF_CLASS(TiAnimation, value)) {
        _appearAnimation = [value retain];
    } else {
        ENSURE_SINGLE_ARG(value, NSDictionary);
        _appearAnimation = [value retain];
    }
    _useAppearAnimation = _useAppearAnimation || _appearAnimation != nil;
}

-(void)setUseAppearAnimation_:(id)value
{
    _useAppearAnimation = [TiUtils boolValue:value def:NO];
}

#pragma mark - Search Support

//-(TiSearchDisplayController*) searchController
//{
//    return [(TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"] searchController];
//}

-(void)setCaseInsensitiveSearch_:(id)args
{
    caseInsensitiveSearch = [TiUtils boolValue:args def:YES];
    if (searchActive) {
        [self buildResultsForSearchText];
        [self reloadTableViewData];
    }
}

-(void)setSearchText_:(id)args
{
    id searchView = [self holdedProxyForKey:@"searchView"];
    if (searchView) {
        DebugLog(@"Can not use searchText with searchView. Ignoring call.");
        return;
    }
    self.searchString = [TiUtils stringValue:args];
    [self buildResultsForSearchText];
    [self reloadTableViewData];
}

-(void)setSearchViewExternal_:(id)args {
    RELEASE_TO_NIL(tableController);
    ENSURE_SINGLE_ARG_OR_NIL(args, TiUISearchBarProxy)

    id vp = [[self viewProxy] addProxyToHold:args setParent:NO forKey:@"searchView" shouldRelayout:NO];
    if (IS_OF_CLASS(vp, TiUISearchBarProxy)) {
        [(TiUISearchBarProxy*)vp setReadyToCreateView:YES];
        [(TiUISearchBarProxy*)vp setDelegate:self];
        ((TiUISearchBarProxy*)vp).canHaveSearchDisplayController = NO;
        if (searchHidden)
        {
            [self hideSearchScreen:nil animated:NO];
        }
    }
}

-(void)setSearchView_:(id)args
{
    RELEASE_TO_NIL(tableController);
    id vp = [[self viewProxy] addObjectToHold:args forKey:@"searchView"];
    if (IS_OF_CLASS(vp, TiUISearchBarProxy)) {
        [(TiUISearchBarProxy*)vp setReadyToCreateView:YES];
        [(TiUISearchBarProxy*)vp setDelegate:self];
        [[self getOrCreateHeaderHolder] addProxy:vp atIndex:0 shouldRelayout:YES];
        if (searchHidden)
        {
            [self hideSearchScreen:nil animated:NO];
        }
    }
}


-(void)setSearchHidden_:(id)hide
{
    searchHidden = [TiUtils boolValue:hide def:NO];
    TiUISearchBarProxy* vp = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
    if (vp) {
        if (searchHidden)
        {
            [self hideSearchScreen:nil];
        }
        else
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

//-(void)setSectionIndexTitles_:(id)args
//{
//    ENSURE_TYPE_OR_NIL(args, NSArray);
//    
//    RELEASE_TO_NIL(sectionTitles);
//    RELEASE_TO_NIL(sectionIndices);
//    RELEASE_TO_NIL(filteredTitles);
//    RELEASE_TO_NIL(filteredIndices);
//    
//    NSArray* theIndex = args;
//	if ([theIndex count] > 0) {
//        sectionTitles = [[NSMutableArray alloc] initWithCapacity:[theIndex count]];
//        sectionIndices = [[NSMutableArray alloc] initWithCapacity:[theIndex count]];
//        
//        for (NSDictionary *entry in theIndex) {
//            ENSURE_DICT(entry);
//            NSString *title = [entry objectForKey:@"title"];
//            id index = [entry objectForKey:@"index"];
//            [sectionTitles addObject:title];
//            [sectionIndices addObject:[NSNumber numberWithInt:[TiUtils intValue:index]]];
//        }
//    }
//    if (searchViewProxy == nil) {
//        if (searchActive) {
//            [self buildResultsForSearchText];
//        }
//        [_tableView reloadSectionIndexTitles];
//    }
//}

#pragma mark - SectionIndexTitle Support Datasource methods.

//-(NSArray *)sectionIndexTitlesForTableView:(UITableView *)tableView
//{
//    if (tableView != _tableView) {
//        return nil;
//    }
//    
//    if (editing) {
//        return nil;
//    }
//    
//    if (searchActive) {
//        if (keepSectionsInSearch && ([_searchResults count] > 0) ) {
//            return filteredTitles;
//        } else {
//            return nil;
//        }
//    }
//    
//    return sectionTitles;
//}

//-(NSInteger)tableView:(UITableView *)tableView sectionForSectionIndexTitle:(NSString *)title atIndex:(NSInteger)theIndex
//{
//    if (tableView != _tableView) {
//        return 0;
//    }
//    
//    if (editing) {
//        return 0;
//    }
//    
//    if (searchActive) {
//        if (keepSectionsInSearch && ([_searchResults count] > 0) && (filteredTitles != nil) && (filteredIndices != nil) ) {
//            // get the index for the title
//            int index = [filteredTitles indexOfObject:title];
//            if (index > 0 && (index < [filteredIndices count]) ) {
//                return [[filteredIndices objectAtIndex:index] intValue];
//            }
//            return 0;
//        } else {
//            return 0;
//        }
//    }
//    
//    if ( (sectionTitles != nil) && (sectionIndices != nil) ) {
//        // get the index for the title
//        int index = [sectionTitles indexOfObject:title];
//        if (index > 0 && (index < [sectionIndices count]) ) {
//            return [[sectionIndices objectAtIndex:index] intValue];
//        }
//        return 0;
//    }
//    return 0;
//}

//#pragma mark - Editing Support
//
//-(BOOL)canEditRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    id editValue = [self valueWithKey:@"canEdit" atIndexPath:indexPath];
//    //canEdit if undefined is false
//    return [TiUtils boolValue:editValue def:NO];
//}
//
//
//-(BOOL)canMoveRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    id moveValue = [self valueWithKey:@"canMove" atIndexPath:indexPath];
//    //canMove if undefined is false
//    return [TiUtils boolValue:moveValue def:NO];
//}

//#pragma mark - Editing Support Datasource methods.
//
//- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    if (tableView != _tableView) {
//        return NO;
//    }
//    
//    if (searchActive) {
//        return NO;
//    }
//    
//    if ([self canEditRowAtIndexPath:indexPath]) {
//        return YES;
//    }
//    if (editing) {
//        return [self canMoveRowAtIndexPath:indexPath];
//    }
//    return NO;
//}
//
//- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    if (editingStyle == UITableViewCellEditingStyleDelete) {
//        TiUICollectionSectionProxy* theSection = [[self.listViewProxy sectionForIndex:indexPath.section] retain];
//        NSDictionary *theItem = [[theSection itemAtIndex:indexPath.row] retain];
//
//        
//        //Fire the delete Event if required
//        NSString *eventName = @"delete";
//        if ([self.proxy _hasListeners:eventName]) {
//        
//            NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
//                                                theSection, @"section",
//                                                self.proxy, @"listView",
//                                                NUMINT(indexPath.section), @"sectionIndex",
//                                                NUMINT(indexPath.row), @"itemIndex",
//                                                nil];
//            id propertiesValue = [theItem objectForKey:@"properties"];
//            NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
//            id itemId = [properties objectForKey:@"itemId"];
//            if (itemId != nil) {
//                [eventObject setObject:itemId forKey:@"itemId"];
//            }
//            [self.proxy fireEvent:eventName withObject:eventObject propagate:NO checkForListener:NO];
//            [eventObject release];
//        }
//        [theItem release];
//        
//        BOOL asyncDelete = [TiUtils boolValue:[self.proxy valueForKey:@"asyncDelete"] def:NO];
//        if (asyncDelete) return;
//        [tableView beginUpdates];
////        [theSection willRemoveItemAt:indexPath];
////        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
////        [tableView endUpdates];
//        [theSection deleteItemsAt:@[@(indexPath.row), @(1), @{@"animated":@(YES)}]];
//    }
//}

//#pragma mark - Editing Support Delegate Methods.
//
//- (UITableViewCellEditingStyle)tableView:(UITableView *)tableView editingStyleForRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    //No support for insert style yet
//    if ([self canEditRowAtIndexPath:indexPath]) {
//        return UITableViewCellEditingStyleDelete;
//    } else {
//        return UITableViewCellEditingStyleNone;
//    }
//}
//
//- (BOOL)tableView:(UITableView *)tableView shouldIndentWhileEditingRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    return [self canEditRowAtIndexPath:indexPath];
//}
//
//- (void)tableView:(UITableView *)tableView willBeginEditingRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    editing = YES;
//    [self.proxy replaceValue:NUMBOOL(editing) forKey:@"editing" notification:NO];
//}
//
//- (void)tableView:(UITableView *)tableView didEndEditingRowAtIndexPath:(NSIndexPath *)indexPath
//{
//    editing = [_tableView isEditing];
//    [self.proxy replaceValue:NUMBOOL(editing) forKey:@"editing" notification:NO];
//    if (!editing) {
//        [self performSelector:@selector(reloadTableViewData) withObject:nil afterDelay:0.1];
//    }
//}

#pragma mark - UICollectionViewDataSource

- (NSInteger)numberOfSectionsInCollectionView:(UICollectionView *)collectionView
{
    NSUInteger sectionCount = 0;
    
    //TIMOB-15526
//    if (collectionView != _tableView && collectionView.backgroundColor == [UIColor clearColor]) {
//        collectionView.backgroundColor = [UIColor whiteColor];
//    }

    if (_searchResults != nil) {
        sectionCount = [_searchResults count];
    } else {
        sectionCount = [self.listViewProxy.sectionCount unsignedIntegerValue];
    }
    return MAX(0,sectionCount);
}

- (NSInteger)collectionView:(UICollectionView *)collectionView numberOfItemsInSection:(NSInteger)section
{

    if (_searchResults != nil) {
        if ([_searchResults count] <= section) {
            return 0;
        }
        NSArray* theSection = [_searchResults objectAtIndex:section];
        return [theSection count];
        
    } else {
        TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:section];
        if (theSection != nil) {
            return theSection.itemCount;
        }
        return 0;
    }
}

-(UICollectionViewCell *) forceCellForRowAtIndexPath:(NSIndexPath *)indexPath {
    return [_tableView cellForItemAtIndexPath:indexPath];
}

- (UICollectionViewCell *)collectionView:(UICollectionView *)collectionView cellForItemAtIndexPath:(NSIndexPath *)indexPath
{
    NSIndexPath* realIndexPath = [self pathForSearchPath:indexPath];
    TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:realIndexPath.section];

    NSDictionary *item = [theSection itemAtIndex:realIndexPath.row];
    id templateId = [item objectForKey:@"template"];
    if (templateId == nil) {
        templateId = _defaultItemTemplate;
    }
    NSString *cellIdentifier = [templateId isKindOfClass:[NSNumber class]] ? [NSString stringWithFormat:@"TiUICollectionView__internal%@", templateId]: [templateId description];
    TiUICollectionItem *cell = [collectionView dequeueReusableCellWithReuseIdentifier:cellIdentifier forIndexPath:indexPath];
    
    if (cell.proxy == nil) {
        id<TiEvaluator> context = self.listViewProxy.executionContext;
        if (context == nil) {
            context = self.listViewProxy.pageContext;
        }
        TiUICollectionItemProxy *cellProxy = [[TiUICollectionItemProxy alloc] initWithCollectionViewProxy:self.listViewProxy inContext:context];
        [cell initWithProxy:cellProxy];
        [cell configurationStart];
        id template = [self.listViewProxy.templates objectForKey:templateId];
        if (template != nil) {
            [cellProxy unarchiveFromTemplate:template withEvents:YES];
            [cellProxy windowWillOpen];
            [cellProxy windowDidOpen];
        }
        [cell configurationSet];
        
        if ([TiUtils isIOS8OrGreater] && (collectionView == _tableView)) {
            [cell setLayoutMargins:UIEdgeInsetsZero];
        }
        [cellProxy release];
    }
    
    cell.dataItem = item;
    cell.proxy.indexPath = realIndexPath;
    TiViewAnimationStep* anim = [cell.proxy runningAnimation];
    if (anim)
    {
        [cell.proxy setRunningAnimationRecursive:anim];
        [cell.proxy refreshViewIfNeeded:YES];
        [cell.proxy setRunningAnimationRecursive:nil];
    }
    else {
        [cell.proxy refreshViewIfNeeded:YES];
    }
    return cell;
}

- (UICollectionReusableView *)collectionView:(UICollectionView *)collectionView viewForSupplementaryElementOfKind:(NSString *)kind atIndexPath:(NSIndexPath *)indexPath
{
    
    NSIndexPath* realIndexPath = [self pathForSearchPath:indexPath];
    TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:realIndexPath.section];
    
    NSDictionary *item = [theSection valueForKey:(kind == UICollectionElementKindSectionHeader)?@"headerView":@"footerView"];
    id templateId = [item objectForKey:@"template"];
    if (templateId == nil) {
        templateId = (kind == UICollectionElementKindSectionHeader)?@"header":@"footer";
    }
   
    
    id template = [self.listViewProxy.templates objectForKey:templateId];
    if (template != nil) {
        TiUICollectionWrapperView *view = [collectionView dequeueReusableSupplementaryViewOfKind:kind withReuseIdentifier:templateId forIndexPath:realIndexPath];
        if (view.proxy == nil) {
            id<TiEvaluator> context = self.listViewProxy.executionContext;
            if (context == nil) {
                context = self.listViewProxy.pageContext;
            }
            TiUICollectionWrapperViewProxy *viewProxy = [[TiUICollectionWrapperViewProxy alloc] initWithCollectionViewProxy:self.listViewProxy inContext:context];
            [view initWithProxy:viewProxy];
            [view configurationStart];
            if (template != nil) {
                [viewProxy unarchiveFromTemplate:template withEvents:YES];
                [viewProxy windowWillOpen];
                [viewProxy windowDidOpen];
            }
            [view configurationSet];
            
            if ([TiUtils isIOS8OrGreater] && (collectionView == _tableView)) {
                [view setLayoutMargins:UIEdgeInsetsZero];
            }
            
            [viewProxy release];
        }
        view.dataItem = item;
        view.proxy.indexPath = realIndexPath;
        return view;
    } else {
        TiViewProxy* child = [theSection sectionViewForLocation:@"headerView" inCollectionView:self];
        if (child && !child.parent) {
            //view is retained by the collectionView
            TiUICollectionWrapperView *view = [collectionView dequeueReusableSupplementaryViewOfKind:kind withReuseIdentifier:templateId forIndexPath:realIndexPath];
            id<TiEvaluator> context = self.listViewProxy.executionContext;
            if (context == nil) {
                context = self.listViewProxy.pageContext;
            }
            TiUICollectionWrapperViewProxy *viewProxy = [[TiUICollectionWrapperViewProxy alloc] initWithCollectionViewProxy:self.listViewProxy inContext:context];
            [view initWithProxy:viewProxy];
            [view configurationStart];

            [viewProxy addProxy:child atIndex:0 shouldRelayout:YES];
            [view configurationSet];
            
            if ([TiUtils isIOS8OrGreater] && (collectionView == _tableView)) {
                [view setLayoutMargins:UIEdgeInsetsZero];
            }
            [viewProxy release];
            view.proxy.indexPath = realIndexPath;
            return view;
        } else if (child) {
            //view is retained by the collectionView
            TiUICollectionWrapperView *view = [collectionView dequeueReusableSupplementaryViewOfKind:kind withReuseIdentifier:templateId forIndexPath:realIndexPath];
            [view updateProxy:(TiUICollectionWrapperViewProxy*)child.parent forIndexPath:realIndexPath];
            return view;
        }
    }
    return nil;
}


#pragma mark - UICollectionViewDelegate

- (void)collectionView:(UICollectionView *)collectionView willDisplayCell:(UICollectionViewCell *)cell forItemAtIndexPath:(NSIndexPath *)indexPath
{
    if (searchActive || (collectionView != _tableView)) {
        return;
    }
    TiUICollectionItem* item = (TiUICollectionItem*)cell;
    NSDictionary *data = item.dataItem;
    
    if (hasOnDisplayCell) {
        TiUICollectionSectionProxy *section = [self.listViewProxy sectionForIndex:indexPath.section];
        NSDictionary * propertiesDict = @{
                                          @"view":((TiUICollectionItem*)cell).proxy,
                                          @"item": data,
                                          @"listView": self.proxy,
                                          @"section":section,
                                          @"searchResult":NUMBOOL([self isSearchActive]),
                                          @"sectionIndex":NUMINTEGER(indexPath.section),
                                          @"itemIndex":NUMINTEGER(indexPath.row)
        };
        [self.proxy fireCallback:@"onDisplayCell" withArg:propertiesDict withSource:self.proxy];
    }
    
    if (_useAppearAnimation) {
        if (!_shownIndexes) {
            _shownIndexes = [[NSMutableSet set] retain];
        }
        if (![_shownIndexes containsObject:indexPath]) {
            [_shownIndexes addObject:indexPath];
            id appearAnimation = [data objectForKey:@"appearAnimation"];
            if (!appearAnimation) {
                appearAnimation = _appearAnimation;
            }
            TiViewProxy* itemProxy = item.proxy;
            TiAnimation * newAnimation = [TiAnimation animationFromArg:appearAnimation context:[itemProxy executionContext] create:NO];
            //            newAnimation.dontApplyOnFinish = YES;
            [itemProxy handleAnimation:newAnimation];
        }
    }
    //Tell the proxy about the cell to be displayed
    [self.listViewProxy willDisplayCell:indexPath];
}

-(CGFloat)collectionView:(UICollectionView *)collectionView itemWidth:(CGFloat)width
{
	if (TiDimensionIsDip(_minItemWidth))
	{
		width = MAX(_minItemWidth.value,width);
	}
	if (TiDimensionIsDip(_maxItemWidth))
	{
		width = MIN(_maxItemWidth.value,width);
	}
	return width;
}

-(CGFloat)collectionView:(UICollectionView *)collectionView itemHeight:(CGFloat)height
{
    if (TiDimensionIsDip(_minItemHeight))
    {
        height = MAX(_minItemHeight.value,height);
    }
    if (TiDimensionIsDip(_maxItemHeight))
    {
        height = MIN(_maxItemHeight.value,height);
    }
    return height;
}

- (CGSize)collectionView:(UICollectionView *)collectionView sizeForItemAtIndexPath:(NSIndexPath *)indexPath
{
    NSIndexPath* realIndexPath = [self pathForSearchPath:indexPath];
    
    id visibleProp = [self valueWithKey:@"visible" atIndexPath:realIndexPath];
    BOOL visible = realIndexPath?[visibleProp boolValue]:true;
    CGSize result = CGSizeZero;
    if (!visible) return result;
    result = self.bounds.size;
    
    TiDimension width = _itemWidth;
    id widthValue = [self valueWithKey:@"columnWidth" atIndexPath:realIndexPath];
    if (widthValue != nil) {
        width = [TiUtils dimensionValue:widthValue];
    }
    if (TiDimensionIsDip(width)) {
        result.width = [self collectionView:collectionView itemWidth:width.value];
    }
    else if (TiDimensionIsPercent(width) || TiDimensionIsAutoFill(width)) {
        result.width = [self collectionView:collectionView itemWidth:TiDimensionCalculateValue(width, result.width)];
    }
    
    id heightValue = [self valueWithKey:@"rowHeight" atIndexPath:realIndexPath];
    TiDimension height = _itemHeight;
    if (heightValue != nil) {
        height = [TiUtils dimensionValue:heightValue];
        
    }
    if (TiDimensionIsDip(height)) {
        result.height = [self collectionView:collectionView itemHeight:height.value];
    }
    else if (TiDimensionIsPercent(height) || TiDimensionIsAutoFill(height)) {
        result.height = [self collectionView:collectionView itemHeight:TiDimensionCalculateValue(height, result.height)];
    }
    
    
    if (TiDimensionIsAuto(width) || TiDimensionIsAutoSize(width) ||
        TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height))
    {
        TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:realIndexPath.section];
        NSDictionary *item = [theSection itemAtIndex:realIndexPath.row];
        id templateId = [item objectForKey:@"template"];
        if (templateId == nil) {
            templateId = _defaultItemTemplate;
        }
        TiUICollectionItemProxy *cellProxy = nil;
        if (!cellProxy) {
            cellProxy = [self.listViewProxy.measureProxies objectForKey:templateId];
        }
        if (cellProxy != nil) {
            [cellProxy setDataItem:item];
            CGSize autoSize = [cellProxy minimumParentSizeForSize:result];
            result.width = [self collectionView:collectionView itemWidth:autoSize.width];
            result.height = [self collectionView:collectionView itemHeight:autoSize.height];
        }
    }
    
    UIEdgeInsets sectionInset = [(UICollectionViewFlowLayout *)collectionView.collectionViewLayout sectionInset];
    result.width -= (sectionInset.left + sectionInset.right);
    return result;
}

#pragma mark UICollectionViewDelegateFlowLayout

- (BOOL)shouldStickHeaderToTopInSection:(NSUInteger)section
{
    return _stickyHeaders;
}

- (CGSize)collectionView:(UICollectionView *)collectionView layout:(UICollectionViewLayout*)collectionViewLayout sizeForItemAtIndexPath:(NSIndexPath *)indexPath;
{
    NSIndexPath* realPath = [self pathForSearchPath:indexPath];
    
    return [self collectionView:collectionView sizeForItemAtIndexPath:realPath];
}


- (CGFloat)collectionView:(UICollectionView *)collectionView layout:(UICollectionViewLayout*)collectionViewLayout minimumInteritemSpacingForSectionAtIndex:(NSInteger)section {
    TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:section];
    return [TiUtils floatValue:[theSection valueForKey:@"itemSpacing"] def:0];
}

- (CGFloat)collectionView:(UICollectionView *)collectionView layout:(UICollectionViewLayout*)collectionViewLayout minimumLineSpacingForSectionAtIndex:(NSInteger)section {
    TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:section];
    return [TiUtils floatValue:[theSection valueForKey:@"lineSpacing"] def:0];
}

- (void)collectionView:(UICollectionView *)collectionView didSelectItemAtIndexPath:(NSIndexPath *)indexPath
{
    [self fireClickForItemAtIndexPath:[self pathForSearchPath:indexPath] collectionView:collectionView accessoryButtonTapped:NO];
    if (allowsSelection == NO)
    {
        [collectionView deselectItemAtIndexPath:indexPath animated:YES];
    }
}

- (UIEdgeInsets)collectionView:(UICollectionView *)collectionView
                        layout:(UICollectionViewLayout *)collectionViewLayout
        insetForSectionAtIndex:(NSInteger)section
{
    TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:section];
    if ([theSection valueForKey:@"inset"]) {
        return [TiUtils insetValue:[theSection valueForKey:@"inset"]];
    }
    
    CGFloat width = self.frame.size.width;
    CGFloat itemWidth = 0;
    if (TiDimensionIsDip(_itemWidth)) {
        itemWidth = [self collectionView:collectionView itemWidth:_itemWidth.value];
    }
    else if (TiDimensionIsPercent(_itemWidth) || TiDimensionIsAutoFill(_itemWidth)) {
        itemWidth = [self collectionView:collectionView itemWidth:TiDimensionCalculateValue(_itemWidth, width)];
    }
    if (itemWidth > 0) {
        NSInteger numberOfCells = width / itemWidth;
        NSInteger edgeInsets = (width - (numberOfCells * itemWidth)) / (numberOfCells + 1);
        return UIEdgeInsetsMake(0, edgeInsets, 0, edgeInsets);
    }
    return UIEdgeInsetsZero;
}

- (CGSize)collectionView:(UICollectionView *)collectionView
                  layout:(UICollectionViewLayout *)collectionViewLayout
referenceSizeForHeaderInSection:(NSInteger)section
{
    if (collectionView != _tableView) {
        return CGSizeZero;
    }
    NSString* location = @"headerView";
    TiUICollectionSectionProxy* theSection = [self.listViewProxy sectionForIndex:section];
    NSDictionary *item = [theSection valueForKey:location];
    if (!item) {
        return CGSizeZero;
    }
    id templateId = [item objectForKey:@"template"];
    if (templateId == nil) {
        templateId = @"header";
    }
    CGSize result = CGSizeZero;
    id template = [self.listViewProxy.templates objectForKey:templateId];
    if (template != nil) {
        id visibleProp = [self valueWithKey:@"visible" forSectionItem:item template:templateId];
        BOOL visible = visibleProp?[visibleProp boolValue]:YES;
        if (!visible) return result;
        result = collectionView.bounds.size;
        
        TiDimension width = [TiUtils dimensionValue:[self valueWithKey:@"width" forSectionItem:item template:templateId]];
        if (TiDimensionIsDip(width)) {
            result.width = [self collectionView:collectionView itemWidth:width.value];
        }
        else if (TiDimensionIsPercent(width) || TiDimensionIsAutoFill(width)) {
            result.width = [self collectionView:collectionView itemWidth:TiDimensionCalculateValue(width, collectionView.bounds.size.width)];
        }
         TiDimension height = [TiUtils dimensionValue:[self valueWithKey:@"height" forSectionItem:item template:templateId]];
        if (TiDimensionIsDip(height)) {
            result.height = [self collectionView:collectionView itemHeight:height.value];
        }
        else if (TiDimensionIsPercent(height) || TiDimensionIsAutoFill(height)) {
            result.height = [self collectionView:collectionView itemHeight:TiDimensionCalculateValue(height, collectionView.bounds.size.height)];
        }
        
        
        if (TiDimensionIsAuto(width) || TiDimensionIsAutoSize(width) ||
            TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height))
        {
            TiUICollectionItemProxy *cellProxy = nil;
            if (!cellProxy) {
                cellProxy = [self.listViewProxy.measureProxies objectForKey:templateId];
            }
            if (cellProxy != nil) {
                [cellProxy setDataItem:item];
                CGSize autoSize = [cellProxy minimumParentSizeForSize:result];
                result.width = [self collectionView:collectionView itemWidth:result.width];
                result.height = [self collectionView:collectionView itemHeight:result.height];
            }
        }
    } else {
        TiViewProxy* child = [theSection sectionViewForLocation:@"headerView" inCollectionView:self];
        if (child) {
            id visibleProp = [child valueForKey:@"visible"];
            BOOL visible = visibleProp?[visibleProp boolValue]:YES;
            if (!visible) return result;
            result = collectionView.bounds.size;
            
            id widthValue = [child valueForKey:@"width"];
            TiDimension width = _itemWidth;
            if (widthValue != nil) {
                width = [TiUtils dimensionValue:widthValue];
            }
            if (TiDimensionIsDip(width)) {
                result.width = [self collectionView:collectionView itemWidth:width.value];
            }
            else if (TiDimensionIsPercent(width) || TiDimensionIsAutoFill(width)) {
                result.width = [self collectionView:collectionView itemWidth:TiDimensionCalculateValue(width, collectionView.bounds.size.width)];
            }
            
            id heightValue = [child valueForKey:@"height"];
            TiDimension height = _itemHeight;
            if (heightValue != nil) {
                height = [TiUtils dimensionValue:heightValue];
            }
            if (TiDimensionIsDip(height)) {
                result.height = [self collectionView:collectionView itemHeight:height.value];
            }
            else if (TiDimensionIsPercent(height) || TiDimensionIsAutoFill(height)) {
                result.height = [self collectionView:collectionView itemHeight:TiDimensionCalculateValue(height, collectionView.bounds.size.height)];
            }
            
            
            if (TiDimensionIsAuto(width) || TiDimensionIsAutoSize(width) ||
                TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height))
            {
                CGSize autoSize = [child minimumParentSizeForSize:result];
                result.width = [self collectionView:collectionView itemWidth:result.width];
                result.height = [self collectionView:collectionView itemHeight:result.height];
            }
        }
    }

    
    UIEdgeInsets sectionInset = [(UICollectionViewFlowLayout *)collectionView.collectionViewLayout sectionInset];
    result.width -= (sectionInset.left + sectionInset.right);
    return result;
}

-(id)valueWithKey:(NSString*)key forSectionItem:(NSDictionary*)item template:(NSString*)templateId
{
    id propertiesValue = [item objectForKey:@"properties"];
    NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
    id theValue = [properties objectForKey:key];
    if (theValue == nil) {
        if (![templateId isKindOfClass:[NSNumber class]]) {
            TiProxyTemplate *template = [self.listViewProxy.templates objectForKey:templateId];
            theValue = [template.properties objectForKey:key];
        }
    }
    
    return theValue;
}

- (CGSize)collectionView:(UICollectionView *)collectionView
                  layout:(UICollectionViewLayout *)collectionViewLayout
referenceSizeForFooterInSection:(NSInteger)section
{
    if (collectionView != _tableView) {
        return CGSizeZero;
    }
    NSString* location = @"footerView";
    
    NSDictionary *item = [[self.listViewProxy sectionForIndex:section] valueForKey:location];
    if (!item) {
        return CGSizeZero;
    }
    id templateId = [item objectForKey:@"template"];
    if (templateId == nil) {
        templateId = @"footer";
    }
    
    id visibleProp = [self valueWithKey:@"visible" forSectionItem:item template:templateId];
    BOOL visible = visibleProp?[visibleProp boolValue]:YES;
    CGSize result = CGSizeZero;
    if (!visible) return result;
    result = collectionView.bounds.size;
    
    id widthValue = [self valueWithKey:@"width" forSectionItem:item template:templateId];
    TiDimension width = _itemWidth;
    if (widthValue != nil) {
        width = [TiUtils dimensionValue:widthValue];
    }
    if (TiDimensionIsDip(width)) {
        result.width = [self collectionView:collectionView itemWidth:width.value];
    }
    else if (TiDimensionIsPercent(width) || TiDimensionIsAutoFill(width)) {
        result.width = [self collectionView:collectionView itemWidth:TiDimensionCalculateValue(width, collectionView.bounds.size.width)];
    }
    
    id heightValue = [self valueWithKey:@"height" forSectionItem:item template:templateId];
    TiDimension height = _itemHeight;
    if (heightValue != nil) {
        height = [TiUtils dimensionValue:heightValue];
    }
    if (TiDimensionIsDip(height)) {
        result.height = [self collectionView:collectionView itemHeight:height.value];
    }
    else if (TiDimensionIsPercent(height) || TiDimensionIsAutoFill(height)) {
        result.height = [self collectionView:collectionView itemHeight:TiDimensionCalculateValue(height, collectionView.bounds.size.height)];
    }
    
    
    if (TiDimensionIsAuto(width) || TiDimensionIsAutoSize(width) ||
        TiDimensionIsAuto(height) || TiDimensionIsAutoSize(height))
    {
        TiUICollectionItemProxy *cellProxy = nil;
        if (!cellProxy) {
            cellProxy = [self.listViewProxy.measureProxies objectForKey:templateId];
        }
        if (cellProxy != nil) {
            [cellProxy setDataItem:item];
            CGSize autoSize = [cellProxy minimumParentSizeForSize:result];
            result.width = [self collectionView:collectionView itemWidth:result.width];
            result.height = [self collectionView:collectionView itemHeight:result.height];
        }
    }
    
    UIEdgeInsets sectionInset = [(UICollectionViewFlowLayout *)collectionView.collectionViewLayout sectionInset];
    result.width -= (sectionInset.left + sectionInset.right);
    return result;
}

#pragma mark - ScrollView Delegate

-(void)detectSectionChange {
    NSArray* visibles = [_tableView indexPathsForVisibleItems];
    NSIndexPath* indexPath = [visibles firstObject];
    NSInteger section = [indexPath section];
    if (_currentSection != section) {
        _currentSection = section;
        if ([[self viewProxy] _hasListeners:@"headerchange" checkParent:NO])
        {
            NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath collectionView:_tableView];
            [event setObject:NUMINTEGER(indexPath.row) forKey:@"firstVisibleItem"];
            [event setObject:NUMINTEGER([visibles count]) forKey:@"visibleItemCount"];
            TiViewProxy* headerView = [self currentSectionViewProxy:_currentSection forLocation:@"headerView"];
            if (headerView) {
                [event setObject:headerView forKey:@"headerView"];
            }
            [self.proxy fireEvent:@"headerchange" withObject:event checkForListener:NO];
        }
    }
}

- (NSMutableDictionary *) eventObjectForScrollView: (UIScrollView *) scrollView
{
    NSMutableDictionary* eventArgs = [super eventObjectForScrollView:scrollView];
    NSArray* indexPaths = [_tableView indexPathsForVisibleItems];
    if ([indexPaths count] > 0) {
        NSIndexPath *indexPath = [self pathForSearchPath:[indexPaths objectAtIndex:0]];
        
        NSUInteger visibleItemCount = [indexPaths count];
        TiUICollectionSectionProxy* section = [[self listViewProxy] sectionForIndex: [indexPath section]];
        [eventArgs setValue:NUMINTEGER([indexPath row]) forKey:@"firstVisibleItemIndex"];
        [eventArgs setValue:NUMUINTEGER(visibleItemCount) forKey:@"visibleItemCount"];
        [eventArgs setValue:NUMINTEGER([indexPath section]) forKey:@"firstVisibleSectionIndex"];
        [eventArgs setValue:section forKey:@"firstVisibleSection"];
        [eventArgs setValue:[section itemAtIndex:[indexPath row]] forKey:@"firstVisibleItem"];
    }
    return eventArgs;
}

- (void)scrollViewDidScroll:(UIScrollView *)scrollView
{
    [super scrollViewDidScroll:scrollView];
    
    [self detectSectionChange];
}

- (void)scrollViewWillBeginDragging:(UIScrollView *)scrollView
{
    // suspend image loader while we're scrolling to improve performance
    if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] suspend];
    [super scrollViewWillBeginDragging:scrollView];
}

- (void)scrollViewDidEndDragging:(UIScrollView *)scrollView willDecelerate:(BOOL)decelerate
{
    if(!decelerate) {
        [self detectSectionChange];
        if (_scrollSuspendImageLoading) {
            // resume image loader when we're done scrolling
            [[ImageLoader sharedLoader] resume];
        }
    }
    [self detectSectionChange];
    [super scrollViewDidEndDragging:scrollView willDecelerate:decelerate];
}

- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
    // resume image loader when we're done scrolling
    if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] resume];
    [self detectSectionChange];
    [super scrollViewDidEndDecelerating:scrollView];
}

- (BOOL)scrollViewShouldScrollToTop:(UIScrollView *)scrollView
{
    // suspend image loader while we're scrolling to improve performance
    if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] suspend];
    return [super scrollViewShouldScrollToTop:scrollView];
}

- (void)scrollViewDidScrollToTop:(UIScrollView *)scrollView
{
    if (_scrollSuspendImageLoading) [[ImageLoader sharedLoader] resume];
    return [super scrollViewDidScrollToTop:scrollView];
    //Events none (maybe scroll later)
}

#pragma mark Overloaded view handling
-(UIView*)viewForHitTest
{
    return _tableView;
}
//- (UIView *)hitTest:(CGPoint)point withEvent:(UIEvent *)event
//{
//	UIView * result = [super hitTest:point withEvent:event];
//	if(result == self)
//	{	//There is no valid reason why the TiUITableView will get an
//		//touch event; it should ALWAYS be a child view.
//		return nil;
//	}
//	return result;
//}

- (void)touchesBegan:(NSSet *)touches withEvent:(UIEvent *)event
{
	// iOS idiom seems to indicate that you should never be able to interact with a table
	// while the 'delete' button is showing for a row, but touchesBegan:withEvent: is still triggered.
	// Turn it into a no-op while we're editing
	if (!editing) {
		[super touchesBegan:touches withEvent:event];
	}
}

//-(UIView*)viewForGestures
//{
//    return [self tableView];
//}

-(void)recognizedSwipe:(UISwipeGestureRecognizer *)recognizer
{
    BOOL viaSearch = [self isSearchActive];
    CGPoint point = [recognizer locationInView:_tableView];
    NSIndexPath* indexPath = [_tableView indexPathForItemAtPoint:point];
    [super recognizedSwipe:recognizer];
    
    if (allowsSelection == NO)
    {
        [_tableView deselectItemAtIndexPath:indexPath animated:YES];
    }
}

-(NSMutableDictionary*)dictionaryFromGesture:(UIGestureRecognizer*)recognizer
{
    NSMutableDictionary* event = [super dictionaryFromGesture:recognizer];
    
    BOOL viaSearch = [self isSearchActive];
    CGPoint point = [recognizer locationInView:_tableView];
    NSIndexPath* indexPath = [_tableView indexPathForItemAtPoint:point];
    indexPath = [self pathForSearchPath:indexPath];
    if (indexPath != nil) {
        [event setValuesForKeysWithDictionary:[self EventObjectForItemAtIndexPath:indexPath collectionView:_tableView atPoint:point]];
    };
    return event;
}

-(void)recognizedLongPress:(UILongPressGestureRecognizer*)recognizer
{
    [super recognizedLongPress:recognizer];
    if ([recognizer state] == UIGestureRecognizerStateBegan) {
        BOOL viaSearch = [self isSearchActive];
        CGPoint point = [recognizer locationInView:_tableView];
        NSIndexPath* indexPath = [_tableView indexPathForItemAtPoint:point];
        if (allowsSelection == NO)
        {
            [_tableView deselectItemAtIndexPath:indexPath animated:YES];
        }
        TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
        if (viaSearch && searchViewProxy) {
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
    TiViewProxy* vp = [self holdedProxyForKey:@"searchView"];
    if (vp) {
        [vp layoutProperties]->right = TiDimensionDip(0);
        [vp refreshViewIfNeeded];
    }
}

- (void)searchBarTextDidBeginEditing:(UISearchBar *)searchBar
{
    self.searchString = (searchBar.text == nil) ? @"" : searchBar.text;
    [self buildResultsForSearchText];
    [self reloadTableViewData];
}

- (void)searchBarTextDidEndEditing:(UISearchBar *)searchBar
{
    if ([searchBar.text length] == 0) {
        self.searchString = @"";
        [self buildResultsForSearchText];
        [self reloadTableViewData];
    }
}

- (void)searchBar:(UISearchBar *)searchBar textDidChange:(NSString *)searchText
{
    self.searchString = (searchText == nil) ? @"" : searchText;
    [self buildResultsForSearchText];
    [self reloadTableViewData];
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
    TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
    if (searchViewProxy) {
        CGFloat rowWidth = floorf([self computeRowWidth:_tableView]);
        if (rowWidth > 0) {
            CGFloat right = _tableView.bounds.size.width - rowWidth;
            [searchViewProxy layoutProperties]->right = TiDimensionDip(right);
            [searchViewProxy refreshViewIfNeeded];
        }
    }
    [searchViewProxy ensureSearchBarHeirarchy];
    [self reloadTableViewData];
    [self hideSearchScreen:nil];
//    [self performSelector:@selector(hideSearchScreen:) withObject:nil afterDelay:0.2];
}

#pragma mark - TiScrolling

-(void)keyboardDidShowAtHeight:(CGFloat)keyboardTop
{
    CGRect minimumContentRect = [_tableView bounds];
    InsetScrollViewForKeyboard(_tableView,keyboardTop,minimumContentRect.size.height + minimumContentRect.origin.y);
}

-(void)scrollToShowView:(UIView *)firstResponderView withKeyboardHeight:(CGFloat)keyboardTop
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

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath collectionView:(UICollectionView *)collectionView  atPoint:(CGPoint)point accessoryButtonTapped:(BOOL)accessoryButtonTapped
{
    TiUICollectionSectionProxy *section = [self.listViewProxy sectionForIndex:indexPath.section];
	NSDictionary *item = [section itemAtIndex:indexPath.row];
    NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                        item, @"item",
                                        section, @"section",
										self.proxy, @"listView",
										NUMBOOL([self isSearchActive]), @"searchResult",
										NUMINTEGER(indexPath.section), @"sectionIndex",
										NUMINTEGER(indexPath.row), @"itemIndex",
										NUMBOOL(accessoryButtonTapped), @"accessoryClicked",
										nil];
	id propertiesValue = [item objectForKey:@"properties"];
	NSDictionary *properties = ([propertiesValue isKindOfClass:[NSDictionary class]]) ? propertiesValue : nil;
	id itemId = [properties objectForKey:@"itemId"];
	if (itemId != nil) {
		[eventObject setObject:itemId forKey:@"itemId"];
	}
	TiUICollectionItem *cell = (TiUICollectionItem *)[collectionView cellForItemAtIndexPath:indexPath];
	if (cell.templateStyle == TiUICollectionItemTemplateStyleCustom) {
		UIView *contentView = cell.contentView;
        TiViewProxy *tapViewProxy =[TiUIHelper findViewProxyWithBindIdUnder:contentView containingPoint:[collectionView convertPoint:point toView:contentView]];
		if (tapViewProxy != nil) {
			[eventObject setObject:[tapViewProxy valueForKey:@"bindId"] forKey:@"bindId"];
		}
	}
    return [eventObject autorelease];
}

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath collectionView:(UICollectionView *)collectionView
{
    return [self EventObjectForItemAtIndexPath:indexPath collectionView:collectionView atPoint:[_tableView touchPoint]];
}

- (NSMutableDictionary*)EventObjectForItemAtIndexPath:(NSIndexPath *)indexPath collectionView:(UICollectionView *)collectionView atPoint:(CGPoint)point
{
    NSMutableDictionary *event = [self EventObjectForItemAtIndexPath:indexPath collectionView:collectionView atPoint:point accessoryButtonTapped:NO];
    [event setObject:NUMFLOAT(point.x) forKey:@"x"];
    [event setObject:NUMFLOAT(point.y) forKey:@"y"];
    return event;
}

- (void)fireClickForItemAtIndexPath:(NSIndexPath *)indexPath collectionView:(UICollectionView *)collectionView accessoryButtonTapped:(BOOL)accessoryButtonTapped
{
	NSString *eventName = @"itemclick";
    if (![self.proxy _hasListeners:eventName]) {
		return;
	}
	
	
	[self.proxy fireEvent:eventName withObject:[self EventObjectForItemAtIndexPath:indexPath collectionView:collectionView] checkForListener:NO];
}


- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath {
    NSInteger numOfSections = [self numberOfSectionsInCollectionView:self.tableView];
    if (numOfSections == 0) return nil;
    NSInteger nextSection = ((indexPath.section + 1) % numOfSections);
    
    if (indexPath.row + 1 == [self collectionView:self.tableView numberOfItemsInSection:indexPath.section]) {
        return [NSIndexPath indexPathForRow:0 inSection:nextSection];
    } else {
        return [NSIndexPath indexPathForRow:(indexPath.row + 1) inSection:indexPath.section];
    }
}

-(void)clearSearchController:(id)sender
{
    if (sender == self) {
        TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
        if (!searchViewProxy.canHaveSearchDisplayController) {
            RELEASE_TO_NIL(tableController);
            RELEASE_TO_NIL(searchController);
            [searchViewProxy ensureSearchBarHeirarchy];
        }
        
    }
}

-(void)initSearchController:(id)sender
{
    if (sender == self && tableController == nil) {
        TiUISearchBarProxy* searchViewProxy = (TiUISearchBarProxy*) [self holdedProxyForKey:@"searchView"];
        if (!searchViewProxy.canHaveSearchDisplayController) {
            tableController = [[UICollectionViewController alloc] init];
            [TiUtils configureController:tableController withObject:nil];
            tableController.collectionView = [self tableView];
            searchController = [[TiSearchDisplayController alloc] initWithSearchBar:[searchViewProxy searchBar] contentsController:tableController];
//            searchController.searchResultsDataSource = self;
//            searchController.searchResultsDelegate = self;
            searchController.delegate = self;
        }
    }
}


#pragma mark - Static Methods

+ (void)setBackgroundColor:(UIColor*)bgColor onTable:(UICollectionView*)table
{
    UIColor* defaultColor = [UIColor whiteColor];
	
	[table setBackgroundColor:(bgColor != nil ? bgColor : defaultColor)];
	[[table backgroundView] setBackgroundColor:[table backgroundColor]];
	
	[table setOpaque:![[table backgroundColor] isEqual:[UIColor clearColor]]];
}

+ (TiViewProxy*)titleViewForText:(NSString*)text inTable:(UITableView *)tableView footer:(BOOL)footer
{
    TiUILabelProxy* titleProxy = [[TiUILabelProxy alloc] init];
    [titleProxy setValue:[NSDictionary dictionaryWithObjectsAndKeys:@"17",@"size",@"bold",@"weight", nil] forKey:@"font"];
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

- (void)updateKeyboardInset  {
    if (((UICollectionViewFlowLayout*)_tableView.collectionViewLayout).scrollDirection == UICollectionViewScrollDirectionVertical) {
        [super updateKeyboardInset];
    }
}
@end



#endif
