/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiUICollectionViewProxy.h"
#import "TiUICollectionView.h"
#import "TiUICollectionItem.h"
#import "TiUtils.h"
#import "TiProxyTemplate.h"
#import "TiTableView.h"
#import "TiCollectionView.h"

@interface TiUICollectionViewProxy ()
@property (nonatomic, readwrite) TiUICollectionView *listView;
@end

@implementation TiUICollectionViewProxy {
	NSMutableArray *_sections;
	NSMutableArray *_operationQueue;
	pthread_mutex_t _operationQueueMutex;
	pthread_rwlock_t _markerLock;
	NSIndexPath *marker;
    NSDictionary* _propertiesForItems;
}
@synthesize propertiesForItems = _propertiesForItems;
@synthesize autoResizeOnImageLoad;

static NSArray* keysToGetFromCollectionView;
-(NSArray *)keysToGetFromCollectionView
{
	if (keysToGetFromCollectionView == nil)
	{
		keysToGetFromCollectionView = [[NSArray arrayWithObjects:@"tintColor",@"accessoryType",@"selectionStyle",@"selectedBackgroundColor",@"selectedBackgroundImage",@"selectedBackgroundGradient", @"unHighlightOnSelect", nil] retain];
	}
	return keysToGetFromCollectionView;
}

static NSDictionary* listViewKeysToReplace;
-(NSDictionary *)listViewKeysToReplace
{
	if (listViewKeysToReplace == nil)
	{
		listViewKeysToReplace = [@{@"selectedBackgroundColor": @"backgroundSelectedColor",
                                   @"selectedBackgroundGradient": @"backgroundSelectedGradient",
                                   @"selectedBackgroundImage": @"backgroundSelectedImage"
                                   } retain];
	}
	return listViewKeysToReplace;
}

- (id)init
{
    self = [super init];
    if (self) {
		_sections = [[NSMutableArray alloc] initWithCapacity:4];
		_operationQueue = [[NSMutableArray alloc] initWithCapacity:10];
		pthread_mutex_init(&_operationQueueMutex,NULL);
		pthread_rwlock_init(&_markerLock,NULL);
        autoResizeOnImageLoad = NO;
    }
    return self;
}

-(void)_initWithProperties:(NSDictionary *)properties
{
    [self initializeProperty:@"canScroll" defaultValue:NUMBOOL(YES)];
    [self initializeProperty:@"caseInsensitiveSearch" defaultValue:NUMBOOL(YES)];
    [super _initWithProperties:properties];
}

-(NSString*)apiName
{
    return @"Ti.UI.CollectionView";
}

- (void)dealloc
{
	[_operationQueue release];
	pthread_mutex_destroy(&_operationQueueMutex);
	pthread_rwlock_destroy(&_markerLock);
    RELEASE_TO_NIL(_sections);
	RELEASE_TO_NIL(marker);
    RELEASE_TO_NIL(_propertiesForItems);
    RELEASE_TO_NIL(_measureProxies)
    RELEASE_TO_NIL(_templates)
    [super dealloc];
}

- (TiUICollectionView *)listView
{
	return (TiUICollectionView *)self.view;
}

-(void)setValue:(id)value forKey:(NSString *)key
{
    if ([[self keysToGetFromCollectionView] containsObject:key])
    {
        if (_propertiesForItems == nil)
        {
            _propertiesForItems = [[NSMutableDictionary alloc] init];
        }
        if ([[self listViewKeysToReplace] valueForKey:key]) {
            [_propertiesForItems setValue:value forKey:[[self listViewKeysToReplace] valueForKey:key]];
        }
        else {
            [_propertiesForItems setValue:value forKey:key];
        }
    }
    [super setValue:value forKey:key];
}

- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:YES];
}

-(void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block animated:(BOOL)animated
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:YES];
}

- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block maintainPosition:(BOOL)maintain
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:maintain];
}

-(void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block animated:(BOOL)animated maintainPosition:(BOOL)maintain
{
    if (view == nil) {
        block(nil);
        return;
    }
    
    if ([self.listView isSearchActive]) {
        block(nil);
        TiThreadPerformOnMainThread(^{
            [self.listView updateSearchResults:nil];
        }, NO);
        return;
    }
    
    BOOL triggerMainThread;
    pthread_mutex_lock(&_operationQueueMutex);
    triggerMainThread = [_operationQueue count] == 0;
    [_operationQueue addObject:Block_copy(block)];
    pthread_mutex_unlock(&_operationQueueMutex);
    if (triggerMainThread) {
        TiThreadPerformBlockOnMainThread(^{
            if (animated)
            {
                [self processUpdateActions:maintain];
            }
            else {
                [UIView setAnimationsEnabled:NO];
                [self processUpdateActions:maintain];
                [UIView setAnimationsEnabled:YES];
            }
        }, NO);
    }
}

- (void)dispatchBlock:(void(^)(UICollectionView *tableView))block
{
	if (view == nil) {
		block(nil);
		return;
	}
	if ([NSThread isMainThread]) {
		return block(self.listView.tableView);
	}
	TiThreadPerformOnMainThread(^{
		block(self.listView.tableView);
	}, YES);
}

- (id)dispatchBlockWithResult:(id(^)(void))block
{
	if ([NSThread isMainThread]) {
		return block();
	}
	
	__block id result = nil;
	TiThreadPerformOnMainThread(^{
		result = [block() retain];
	}, YES);
	return [result autorelease];
}

- (void)processUpdateActions:(BOOL)maintainPosition
{
	UICollectionView *tableView = self.listView.tableView;
	BOOL removeHead = NO;
    CGPoint offset;
	while (YES) {
		void (^block)(UICollectionView *) = nil;
		pthread_mutex_lock(&_operationQueueMutex);
		if (removeHead) {
			[_operationQueue removeObjectAtIndex:0];
		}
		if ([_operationQueue count] > 0) {
			block = [_operationQueue objectAtIndex:0];
			removeHead = YES;
		}
		pthread_mutex_unlock(&_operationQueueMutex);
		if (block != nil) {
            if (maintainPosition) {
                offset = [tableView contentOffset];
            }
//            [tableView beginUpdates];
            block(tableView);
//            [tableView endUpdates];
            if (maintainPosition) {
                [tableView setContentOffset:offset animated:NO];
            }
			Block_release(block);
		} else {
			[self.listView updateIndicesForVisibleRows];
			[self contentsWillChange];
			return;
		}
	}
}

- (TiUICollectionSectionProxy *)sectionForIndex:(NSUInteger)index
{
	if (index < [_sections count]) {
		return [_sections objectAtIndex:index];
	}
	return nil;
}

- (void) deleteSectionAtIndex:(NSUInteger)index
{
    if ([_sections count] <= index) {
        DebugLog(@"[WARN] CollectionViewProxy: Delete section index is out of range");
        return;
    }
    TiUICollectionSectionProxy *section = [_sections objectAtIndex:index];
    [_sections removeObjectAtIndex:index];
    section.delegate = nil;
    [_sections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
        section.sectionIndex = idx;
    }];
    [self forgetProxy:section];
}


-(NSArray *)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"style", @"templates", @"defaultItemTemplate", @"sections"]] retain];;
    });
    return keySequence;
}

- (void)viewDidInitialize
{
	[self.listView tableView];
    [super viewDidInitialize];
}

- (void)willShow
{
	[self.listView deselectAll:YES];
	[super willShow];
}

-(BOOL)shouldHighlightCurrentCollectionItem {
    return [self.listView shouldHighlightCurrentCollectionItem];
}

- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath {
    return [self.listView nextIndexPath:indexPath];
}

-(TDUICollectionView*)tableView
{
    return self.listView.tableView;
}

#pragma mark - Public API
- (void)setTemplates:(id)args
{
    ENSURE_TYPE_OR_NIL(args,NSDictionary);
    NSMutableDictionary *templates = [[NSMutableDictionary alloc] initWithCapacity:[args count]];
    NSMutableDictionary *measureProxies = [[NSMutableDictionary alloc] initWithCapacity:[args count]];
    [(NSDictionary *)args enumerateKeysAndObjectsUsingBlock:^(NSString *key, id obj, BOOL *stop) {
        TiProxyTemplate *template = [TiProxyTemplate templateFromViewTemplate:obj];
        if (template != nil) {
            [templates setObject:template forKey:key];
            
            //create fake proxy for height computation
            id<TiEvaluator> context = [self getContext];
            TiUICollectionItemProxy *cellProxy = [[TiUICollectionItemProxy alloc] initWithCollectionViewProxy:self inContext:context];
            [cellProxy unarchiveFromTemplate:template withEvents:NO];
//            [cellProxy bindings];
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
    
    [self replaceValue:args forKey:@"templates" notification:YES];
}

- (NSArray *)sections
{
	return [self dispatchBlockWithResult:^() {
		return [[_sections copy] autorelease];
	}];
}

- (NSNumber *)sectionCount
{
	return [self dispatchBlockWithResult:^() {
		return [NSNumber numberWithUnsignedInteger:[_sections count]];
	}];
}

- (void)setSections:(id)args
{
	ENSURE_TYPE_OR_NIL(args,NSArray);
	NSMutableArray *insertedSections = [args mutableCopy];
    for (int i = 0; i < [insertedSections count]; i++) {
        id section = [insertedSections objectAtIndex:i];
        if ([section isKindOfClass:[NSDictionary class]]) {
            //wer support directly sending a dictionnary
            section = [[[TiUICollectionSectionProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:section]] autorelease];
            [insertedSections replaceObjectAtIndex:i withObject:section];
        }
        else {
		ENSURE_TYPE(section, TiUICollectionSectionProxy);
        }
		[self rememberProxy:section];
    }
	[self dispatchBlock:^(UICollectionView *tableView) {
		[_sections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.delegate = nil;
			if (![insertedSections containsObject:section]) {
				[self forgetProxy:section];
			}
		}];
		[_sections release];
		_sections = [insertedSections retain];
		[_sections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.delegate = self;
			section.sectionIndex = idx;
		}];
		[tableView reloadData];
		[self contentsWillChange];
	}];
	[insertedSections release];
}

- (void)appendSection:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	id arg = [args objectAtIndex:0];
	NSArray *appendedSections = [arg isKindOfClass:[NSArray class]] ? arg : [NSArray arrayWithObject:arg];
	if ([appendedSections count] == 0) {
		return;
	}
	NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
    NSMutableArray *insertedSections = [NSMutableArray arrayWithCapacity:[appendedSections count]];
    for (int i = 0; i < [appendedSections count]; i++) {
        id section = [appendedSections objectAtIndex:i];
        if ([section isKindOfClass:[NSDictionary class]]) {
            //wer support directly sending a dictionnary
            section = [[[TiUICollectionSectionProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:section]] autorelease];
        }
        else {
		ENSURE_TYPE(section, TiUICollectionSectionProxy);
        }
		[self rememberProxy:section];
        [insertedSections addObject:section];
    }
	[self dispatchUpdateAction:^(UICollectionView *tableView) {
		NSMutableIndexSet *indexSet = [[NSMutableIndexSet alloc] init];
		[insertedSections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
			if (![_sections containsObject:section]) {
				NSUInteger insertIndex = [_sections count];
				[_sections addObject:section];
				section.delegate = self;
				section.sectionIndex = insertIndex;
				[indexSet addIndex:insertIndex];
			} else {
				DebugLog(@"[WARN] CollectionView: Attempt to append exising section");
			}
		}];
		if ([indexSet count] > 0) {
			[tableView insertSections:indexSet];
		}
		[indexSet release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)deleteSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	NSUInteger deleteIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	[self dispatchUpdateAction:^(UICollectionView *tableView) {
		if ([_sections count] <= deleteIndex) {
			DebugLog(@"[WARN] CollectionView: Delete section index is out of range");
			return;
		}
		TiUICollectionSectionProxy *section = [_sections objectAtIndex:deleteIndex];
		[_sections removeObjectAtIndex:deleteIndex];
		section.delegate = nil;
		[_sections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.sectionIndex = idx;
		}];
		[tableView deleteSections:[NSIndexSet indexSetWithIndex:deleteIndex]];
		[self forgetProxy:section];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)insertSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger insertIndex = [TiUtils intValue:[args objectAtIndex:0]];
	id arg = [args objectAtIndex:1];
	NSArray *insertSections = [arg isKindOfClass:[NSArray class]] ? arg : [NSArray arrayWithObject:arg];
	if ([insertSections count] == 0) {
		return;
	}
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	[insertSections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
		ENSURE_TYPE(section, TiUICollectionSectionProxy);
		[self rememberProxy:section];
	}];
	[self dispatchUpdateAction:^(UICollectionView *tableView) {
		if ([_sections count] < insertIndex) {
			DebugLog(@"[WARN] CollectionView: Insert section index is out of range");
			[insertSections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
				[self forgetProxy:section];
			}];
			return;
		}
		NSMutableIndexSet *indexSet = [[NSMutableIndexSet alloc] init];
		__block NSUInteger index = insertIndex;
		[insertSections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
			if (![_sections containsObject:section]) {
				[_sections insertObject:section atIndex:index];
				section.delegate = self;
				[indexSet addIndex:index];
				++index;
			} else {
				DebugLog(@"[WARN] CollectionView: Attempt to insert exising section");
			}
		}];
		[_sections enumerateObjectsUsingBlock:^(TiUICollectionSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.sectionIndex = idx;
		}];
		[tableView insertSections:indexSet];
		[indexSet release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)replaceSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger replaceIndex = [TiUtils intValue:[args objectAtIndex:0]];
	TiUICollectionSectionProxy *section = [args objectAtIndex:1];
	ENSURE_TYPE(section, TiUICollectionSectionProxy);
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	[self rememberProxy:section];
	[self dispatchUpdateAction:^(UICollectionView *tableView) {
		if ([_sections containsObject:section]) {
			DebugLog(@"[WARN] CollectionView: Attempt to insert exising section");
			return;
		}
		if ([_sections count] <= replaceIndex) {
			DebugLog(@"[WARN] CollectionView: Replace section index is out of range");
			[self forgetProxy:section];
			return;
		}
		TiUICollectionSectionProxy *prevSection = [_sections objectAtIndex:replaceIndex];
		prevSection.delegate = nil;
		[_sections replaceObjectAtIndex:replaceIndex withObject:section];
		section.delegate = self;
		section.sectionIndex = replaceIndex;
		NSIndexSet *indexSet = [NSIndexSet indexSetWithIndex:replaceIndex];
		[tableView deleteSections:indexSet];
		[tableView insertSections:indexSet];
		[self forgetProxy:prevSection];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)scrollToItem:(id)args
{
    if (view != nil) {
        ENSURE_ARG_COUNT(args, 2);
        NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
        UICollectionViewScrollPosition scrollPosition = [TiUtils intValue:@"position" properties:properties def:UICollectionViewScrollPositionNone];
        BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] CollectionView: Scroll to section index is out of range");
                return;
            }
            TiUICollectionSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:MIN(itemIndex, section.itemCount) inSection:sectionIndex];
            [self.listView.tableView scrollToItemAtIndexPath:indexPath atScrollPosition:scrollPosition animated:animated];
        }, NO);
    }
}

- (id)getItem:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
    if ([_sections count] <= sectionIndex) {
        DebugLog(@"[WARN] CollectionView: getItem section  index is out of range");
        return nil;
    }
    TiUICollectionSectionProxy *section = [_sections objectAtIndex:sectionIndex];
    if ([section itemCount] <= itemIndex) {
        DebugLog(@"[WARN] CollectionView: getItem index is out of range");
        return nil;
    }
    return [section itemAtIndex:itemIndex];
}

- (id)getChildByBindId:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
	NSString *bindId = [TiUtils stringValue:[args objectAtIndex:2]];
    if ([_sections count] <= sectionIndex) {
        DebugLog(@"[WARN] CollectionView:getChildByBindId section index is out of range");
        return nil;
    }
    TiUICollectionSectionProxy *section = [_sections objectAtIndex:sectionIndex];
    if ([section itemCount] <= itemIndex) {
        DebugLog(@"[WARN] CollectionView: getChildByBindId index is out of range");
        return nil;
    }
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:MIN(itemIndex, section.itemCount) inSection:sectionIndex];
    TiUICollectionItem *cell = (TiUICollectionItem *)[self.listView.tableView cellForItemAtIndexPath:indexPath];
    id bindObject = [[cell proxy] valueForUndefinedKey:bindId];
    return bindObject;
}

- (void)selectItem:(id)args
{
    if (view != nil) {
        ENSURE_ARG_COUNT(args, 2);
        NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        NSDictionary *options = [args count] > 2 ? [args objectAtIndex:2] : nil;
        BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] CollectionView: Select section index is out of range");
                return;
            }
            TiUICollectionSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            if (section.itemCount <= itemIndex) {
                DebugLog(@"[WARN] CollectionView: Select item index is out of range");
                return;
            }
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:itemIndex inSection:sectionIndex];
            [self.listView selectItem:indexPath animated:animated];
        }, NO);
    }
}

- (void)deselectItem:(id)args
{
    if (view != nil) {
        ENSURE_ARG_COUNT(args, 2);
        NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        NSDictionary *options = [args count] > 2 ? [args objectAtIndex:2] : nil;
        BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] CollectionView: Select section index is out of range");
                return;
            }
            TiUICollectionSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            if (section.itemCount <= itemIndex) {
                DebugLog(@"[WARN] CollectionView: Select item index is out of range");
                return;
            }
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:itemIndex inSection:sectionIndex];
            [self.listView deselectItem:indexPath animated:animated];
        }, NO);
    }
}

- (void)deselectAll:(id)args
{
    if (view != nil) {
        NSDictionary *options = [args count] > 0 ? [args objectAtIndex:0] : nil;
        BOOL animated = [TiUtils boolValue:@"animated" properties:options def:YES];
        TiThreadPerformBlockOnMainThread(^{
            [self.listView deselectAll:animated];
        }, NO);
    }
}

-(void)setContentInsets:(id)args
{
    id arg1;
    id arg2;
    if ([args isKindOfClass:[NSDictionary class]]) {
        arg1 = args;
        arg2 = nil;
    }
    else {
        arg1 = [args objectAtIndex:0];
        arg2 = [args count] > 1 ? [args objectAtIndex:1] : nil;
    }
    TiThreadPerformOnMainThread(^{
        [self.listView setContentInsets_:arg1 withObject:arg2];
    }, NO);
}

- (id)getSectionItemsCount:(id)args
{
    NSNumber *sectionIndex = nil;
    ENSURE_ARG_AT_INDEX(sectionIndex, args, 0, NSNumber);
    TiUICollectionSectionProxy* section = [_sections objectAtIndex:[sectionIndex integerValue]];
    if (section) {
        return [section length];
    }
    return 0;
}

- (TiUICollectionSectionProxy *)getSectionAt:(id)args
{
    NSNumber *sectionIndex = nil;
	ENSURE_ARG_AT_INDEX(sectionIndex, args, 0, NSNumber);
	return [_sections objectAtIndex:[sectionIndex integerValue]];
}

- (TiUICollectionSectionProxy *)getItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
    TiUICollectionSectionProxy* section = [self getSectionAt:args];
    if (section){
//        NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
        return [section getItemAt:sliceArray(args, 1)];
    }
    else {
        DebugLog(@"[WARN] getItemAt item index is out of range");
    }
}

- (void)appendItems:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	TiUICollectionSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section appendItems:sliceArray(args, 1)];
    }
    else {
        DebugLog(@"[WARN] appendItems:section item index is out of range");
    }
}

- (void)insertItemsAt:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	TiUICollectionSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section insertItemsAt:sliceArray(args, 1)];
    }
    else {
        DebugLog(@"[WARN] insertItemsAt item index is out of range");
    }
}

- (void)replaceItemsAt:(id)args
{
	ENSURE_ARG_COUNT(args, 4);
	TiUICollectionSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section replaceItemsAt:sliceArray(args, 1)];
    }
    else {
        DebugLog(@"[WARN] replaceItemsAt item index is out of range");
    }
}

- (void)deleteItemsAt:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	TiUICollectionSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section deleteItemsAt:sliceArray(args, 1)];
    }
    else {
        DebugLog(@"[WARN] deleteItemsAt item index is out of range");
    }
}

- (void)updateItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 3);
	TiUICollectionSectionProxy* section = [self getSectionAt:args];
    if (section){
        [section updateItemAt:sliceArray(args, 1)];
    }
    else {
        DebugLog(@"[WARN] updateItemAt item index is out of range");
    }
}

-(void)showPullView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
	[self makeViewPerformSelector:@selector(showPullView:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

-(void)closePullView:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
	[self makeViewPerformSelector:@selector(closePullView:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

//-(void)hideDeleteButton:(id)args {
//    [self dispatchUpdateAction:^(UICollectionView *tableView) {
//		[tableView setEditing:NO animated:YES];
//	} animated:YES];
//}

-(void)closeSwipeMenu:(id)args {
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
    [self makeViewPerformSelector:@selector(closeSwipeMenu:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

#pragma mark - Marker Support
- (void)setMarker:(id)args;
{
    ENSURE_SINGLE_ARG(args, NSDictionary);
    pthread_rwlock_wrlock(&_markerLock);
    NSInteger section = [TiUtils intValue:[args objectForKey:@"sectionIndex"] def:NSIntegerMax];
    NSInteger row = [TiUtils intValue:[args objectForKey:@"itemIndex"] def:NSIntegerMax];
    RELEASE_TO_NIL(marker);
    marker = [[NSIndexPath indexPathForRow:row inSection:section] retain];
    pthread_rwlock_unlock(&_markerLock);
}

-(void)willDisplayCell:(NSIndexPath*)indexPath
{
    if ((marker != nil) && [self _hasListeners:@"marker"]) {
        //Never block the UI thread
        int result = pthread_rwlock_tryrdlock(&_markerLock);
        if (result != 0) {
            return;
        }
        if ( (indexPath.section > marker.section) || ( (marker.section == indexPath.section) && (indexPath.row >= marker.row) ) ){
            [self fireEvent:@"marker" withObject:nil propagate:NO checkForListener:NO];
            RELEASE_TO_NIL(marker);
        }
        pthread_rwlock_unlock(&_markerLock);
    }
}

-(void)didOverrideEvent:(NSString*)type forItem:(TiUICollectionItemProxy*)item
{
    if ([type isEqualToString:@"load"] && [self autoResizeOnImageLoad]) {
        [self dispatchUpdateAction:^(UICollectionView *tableView) {
            [item dirtyItAll];
            [item.listItem setNeedsLayout];
        } animated:NO];
    }
}

-(void)willRotateToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation duration:(NSTimeInterval)duration
{
    [super willRotateToInterfaceOrientation:toInterfaceOrientation duration:duration];
    [self.listView.tableView.collectionViewLayout invalidateLayout];
}

DEFINE_DEF_BOOL_PROP(willScrollOnStatusTap,YES);
USE_VIEW_FOR_CONTENT_SIZE

@end

#endif
