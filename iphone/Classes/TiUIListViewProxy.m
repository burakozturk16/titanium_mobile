/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListViewProxy.h"
#import "TiUIListView.h"
#import "TiUIListItem.h"
#import "TiUtils.h"
#import "TiProxyTemplate.h"
#import "TiTableView.h"

@interface TiUIListViewProxy ()
@property (nonatomic, readwrite) TiUIListView *listView;
@end

@implementation TiUIListViewProxy {
    NSMutableArray *_sections;
    NSMutableArray *_operationQueue;
    NSMutableArray *_markerArray;
    pthread_mutex_t _operationQueueMutex;
    pthread_rwlock_t _markerLock;
    NSDictionary* _propertiesForItems;
    BOOL _needsReload;
//    NSMutableDictionary* _measureProxies;
//    NSDictionary *_templates;
}
@synthesize propertiesForItems = _propertiesForItems;
@synthesize autoResizeOnImageLoad;

#pragma mark Internal

-(NSArray *)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"style", @"templates", @"defaultItemTemplate", @"sections", @"backgroundColor", @"searchHidden", @"keepSectionsInSearch"]] retain];;
    });
    return keySequence;
}

static NSArray* keysToGetFromListView;
-(NSArray *)keysToGetFromListView
{
	if (keysToGetFromListView == nil)
	{
		keysToGetFromListView = [[NSArray arrayWithObjects:@"tintColor",@"accessoryType",@"selectionStyle",@"selectedBackgroundColor",@"selectedBackgroundImage",@"selectedBackgroundGradient", @"unHighlightOnSelect", nil] retain];
	}
	return keysToGetFromListView;
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
        _markerArray = [[NSMutableArray alloc] initWithCapacity:4];
        pthread_mutex_init(&_operationQueueMutex,NULL);
        pthread_rwlock_init(&_markerLock,NULL);
        autoResizeOnImageLoad = NO;
        _needsReload = NO;
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
    return @"Ti.UI.ListView";
}

- (void)windowWillClose
{
    if([self viewInitialized])
    {
        [self makeViewPerformSelector:@selector(cleanup:) withObject:nil createIfNeeded:NO waitUntilDone:YES];
    }
    [super windowWillClose];
}
-(void)windowDidClose
{
    if([self viewInitialized])
    {
        [self makeViewPerformSelector:@selector(windowDidClose:) withObject:nil createIfNeeded:NO waitUntilDone:YES];
    }
    [super windowDidClose];

}


- (void)dealloc
{
	[_operationQueue release];
	pthread_mutex_destroy(&_operationQueueMutex);
	pthread_rwlock_destroy(&_markerLock);
    RELEASE_TO_NIL(_sections);
	RELEASE_TO_NIL(_markerArray);
    RELEASE_TO_NIL(_propertiesForItems);
    RELEASE_TO_NIL(_measureProxies)
    RELEASE_TO_NIL(_templates)
    [super dealloc];
}

- (TiUIListView *)listView
{
	return (TiUIListView *)self.view;
}

- (id<TiUIListViewDelegateView>) delegateView
{
    if (view != nil) {
        return [self listView];
    }
    return nil;
}

-(void)setValue:(id)value forKey:(NSString *)key
{
    if ([[self keysToGetFromListView] containsObject:key])
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

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:YES];
}

-(void)dispatchUpdateAction:(void(^)(UITableView *tableView))block animated:(BOOL)animated
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:YES];
}

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block maintainPosition:(BOOL)maintain
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:maintain];
}
-(void)dispatchUpdateAction:(void(^)(UITableView *tableView))block animated:(BOOL)animated maintainPosition:(BOOL)maintain
{
	if (view == nil) {
		block(nil);
		return;
	}
    
    if ([self.listView isSearchActive]) {
        block(nil);
        TiThreadPerformBlockOnMainThread(^{
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

- (void)dispatchBlock:(void(^)(UITableView *tableView))block
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
	TiTableView *tableView = self.listView.tableView;
	BOOL removeHead = NO;
    CGPoint offset;
	while (YES) {
		void (^block)(UITableView *) = nil;
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
            [CATransaction begin];
            [CATransaction setCompletionBlock:^{
                [self.listView scrollViewDidScroll:tableView];
            }];
            [tableView beginUpdates];
            block(tableView);
            [tableView endUpdates];
            [CATransaction commit];
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

- (TiUIListSectionProxy *)sectionForIndex:(NSUInteger)index
{
	if (index < [_sections count]) {
		return [_sections objectAtIndex:index];
	}
	return nil;
}

- (void) deleteSectionAtIndex:(NSUInteger)index
{
    if ([_sections count] <= index) {
        DebugLog(@"[WARN] ListViewProxy: Delete section index is out of range");
        return;
    }
    TiUIListSectionProxy *section = [_sections objectAtIndex:index];
    [_sections removeObjectAtIndex:index];
    section.delegate = nil;
    [_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
        section.sectionIndex = idx;
    }];
    [self forgetProxy:section];
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

-(BOOL)shouldHighlightCurrentListItem {
    return [self.listView shouldHighlightCurrentListItem];
}


-(BOOL)isEditing {
    return [self.listView editing];
}

- (NSIndexPath *) nextIndexPath:(NSIndexPath *) indexPath {
    return [self.listView nextIndexPath:indexPath];
}

-(TiTableView*)tableView
{
    return self.listView.tableView;
}

//-(NSArray*)visibleChildren
//{
//    return [self.listView visibleCellsProxies];
//}

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
            id<TiEvaluator> context = self.executionContext;
            if (context == nil) {
                context = self.pageContext;
            }
            TiUIListItemProxy *cellProxy = [[TiUIListItemProxy alloc] initWithListViewProxy:self inContext:context];
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
    if (![self isConfigurationSet]) {
        _needsReload = YES;
    }
    [self replaceValue:args forKey:@"templates" notification:YES];
}


- (NSArray *)sections
{
//	return [self dispatchBlockWithResult:^() {
		return [[_sections copy] autorelease];
//	}];
}

- (NSNumber *)sectionCount
{
//	return [self dispatchBlockWithResult:^() {
    return @([_sections count]);
//	}];
}

- (void)setSections:(id)args
{
	ENSURE_TYPE_OR_NIL(args,NSArray);
	NSMutableArray *insertedSections = [args mutableCopy];
    for (int i = 0; i < [insertedSections count]; i++) {
        id section = [insertedSections objectAtIndex:i];
        if ([section isKindOfClass:[NSDictionary class]]) {
            //wer support directly sending a dictionnary
            section = [[[TiUIListSectionProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:section]] autorelease];
            [insertedSections replaceObjectAtIndex:i withObject:section];
        }
        else {
		ENSURE_TYPE(section, TiUIListSectionProxy);
        }
		[self rememberProxy:section];
    }
	[self dispatchBlock:^(UITableView *tableView) {
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.delegate = nil;
			if (![insertedSections containsObject:section]) {
				[self forgetProxy:section];
			}
		}];
		[_sections release];
		_sections = [insertedSections retain];
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.delegate = self;
			section.sectionIndex = idx;
		}];
		[tableView reloadData];
		[self contentsWillChange];
	}];
    if (![self isConfigurationSet]) {
        _needsReload = YES;
    }
	[insertedSections release];
}

-(void)configurationSet:(BOOL)recursive
{
    [super configurationSet:recursive];
    if (_needsReload) {
        _needsReload = NO;
        [self dispatchBlock:^(UITableView *tableView) {
            [tableView reloadData];
        }];
    }
    
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
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    NSMutableArray *insertedSections = [NSMutableArray arrayWithCapacity:[appendedSections count]];
    for (int i = 0; i < [appendedSections count]; i++) {
        id section = [appendedSections objectAtIndex:i];
        if ([section isKindOfClass:[NSDictionary class]]) {
            //wer support directly sending a dictionnary
            section = [[[TiUIListSectionProxy alloc] _initWithPageContext:[self executionContext] args:[NSArray arrayWithObject:section]] autorelease];
        }
        else {
		ENSURE_TYPE(section, TiUIListSectionProxy);
        }
		[self rememberProxy:section];
        [insertedSections addObject:section];
    }
	[self dispatchUpdateAction:^(UITableView *tableView) {
		NSMutableIndexSet *indexSet = [[NSMutableIndexSet alloc] init];
		[insertedSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			if (![_sections containsObject:section]) {
				NSUInteger insertIndex = [_sections count];
				[_sections addObject:section];
				section.delegate = self;
				section.sectionIndex = insertIndex;
				[indexSet addIndex:insertIndex];
			} else {
				DebugLog(@"[WARN] ListView: Attempt to append exising section");
			}
		}];
		if ([indexSet count] > 0) {
			[tableView insertSections:indexSet withRowAnimation:animation];
		}
		[indexSet release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)deleteSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	NSUInteger deleteIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	[self dispatchUpdateAction:^(UITableView *tableView) {
		if ([_sections count] <= deleteIndex) {
			DebugLog(@"[WARN] ListView: Delete section index is out of range");
			return;
		}
		TiUIListSectionProxy *section = [_sections objectAtIndex:deleteIndex];
		[_sections removeObjectAtIndex:deleteIndex];
		section.delegate = nil;
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.sectionIndex = idx;
		}];
		[tableView deleteSections:[NSIndexSet indexSetWithIndex:deleteIndex] withRowAnimation:animation];
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
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	[insertSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
		ENSURE_TYPE(section, TiUIListSectionProxy);
		[self rememberProxy:section];
	}];
	[self dispatchUpdateAction:^(UITableView *tableView) {
		if ([_sections count] < insertIndex) {
			DebugLog(@"[WARN] ListView: Insert section index is out of range");
			[insertSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
				[self forgetProxy:section];
			}];
			return;
		}
		NSMutableIndexSet *indexSet = [[NSMutableIndexSet alloc] init];
		__block NSUInteger index = insertIndex;
		[insertSections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			if (![_sections containsObject:section]) {
				[_sections insertObject:section atIndex:index];
				section.delegate = self;
				[indexSet addIndex:index];
				++index;
			} else {
				DebugLog(@"[WARN] ListView: Attempt to insert exising section");
			}
		}];
		[_sections enumerateObjectsUsingBlock:^(TiUIListSectionProxy *section, NSUInteger idx, BOOL *stop) {
			section.sectionIndex = idx;
		}];
		[tableView insertSections:indexSet withRowAnimation:animation];
		[indexSet release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)replaceSectionAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger replaceIndex = [TiUtils intValue:[args objectAtIndex:0]];
	TiUIListSectionProxy *section = [args objectAtIndex:1];
	ENSURE_TYPE(section, TiUIListSectionProxy);
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	[self rememberProxy:section];
	[self dispatchUpdateAction:^(UITableView *tableView) {
		if ([_sections containsObject:section]) {
			DebugLog(@"[WARN] ListView: Attempt to insert exising section");
			return;
		}
		if ([_sections count] <= replaceIndex) {
			DebugLog(@"[WARN] ListView: Replace section index is out of range");
			[self forgetProxy:section];
			return;
		}
		TiUIListSectionProxy *prevSection = [_sections objectAtIndex:replaceIndex];
		prevSection.delegate = nil;
		if (section != nil) {
			[_sections replaceObjectAtIndex:replaceIndex withObject:section];
			section.delegate = self;
			section.sectionIndex = replaceIndex;
		}
		NSIndexSet *indexSet = [NSIndexSet indexSetWithIndex:replaceIndex];
		[tableView deleteSections:indexSet withRowAnimation:animation];
		[tableView insertSections:indexSet withRowAnimation:animation];
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
        UITableViewScrollPosition scrollPosition = [TiUtils intValue:@"position" properties:properties def:UITableViewScrollPositionNone];
        BOOL animated = [TiUtils boolValue:@"animated" properties:properties def:YES];
        TiThreadPerformOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] ListView: Scroll to section index is out of range");
                return;
            }
            TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            NSIndexPath *indexPath = [NSIndexPath indexPathForRow:MIN(itemIndex, section.itemCount) inSection:sectionIndex];
            [self.listView.tableView scrollToRowAtIndexPath:indexPath atScrollPosition:scrollPosition animated:animated];
        }, NO);
    }
}

- (id)getItem:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger sectionIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:1]];
    if ([_sections count] <= sectionIndex) {
        DebugLog(@"[WARN] ListView: getItem section  index is out of range");
        return nil;
    }
    TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
    if ([section itemCount] <= itemIndex) {
        DebugLog(@"[WARN] ListView: getItem index is out of range");
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
        DebugLog(@"[WARN] ListView:getChildByBindId section index is out of range");
        return nil;
    }
    TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
    if ([section itemCount] <= itemIndex) {
        DebugLog(@"[WARN] ListView: getChildByBindId index is out of range");
        return nil;
    }
    NSIndexPath *indexPath = [NSIndexPath indexPathForRow:MIN(itemIndex, section.itemCount) inSection:sectionIndex];
    TiUIListItem *cell = (TiUIListItem *)[self.listView.tableView cellForRowAtIndexPath:indexPath];
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
                DebugLog(@"[WARN] ListView: Select section index is out of range");
                return;
            }
            TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            if (section.itemCount <= itemIndex) {
                DebugLog(@"[WARN] ListView: Select item index is out of range");
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
        TiThreadPerformBlockOnMainThread(^{
            if ([_sections count] <= sectionIndex) {
                DebugLog(@"[WARN] ListView: Select section index is out of range");
                return;
            }
            TiUIListSectionProxy *section = [_sections objectAtIndex:sectionIndex];
            if (section.itemCount <= itemIndex) {
                DebugLog(@"[WARN] ListView: Select item index is out of range");
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

-(void)setContentOffset:(id)args
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
        [self.listView setContentOffset_:arg1 withObject:arg2];
    }, NO);

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

- (TiUIListSectionProxy *)getSectionAt:(id)args
{
    NSNumber *sectionIndex = nil;
	ENSURE_ARG_AT_INDEX(sectionIndex, args, 0, NSNumber);
	return [_sections objectAtIndex:[sectionIndex integerValue]];
}

- (id)getSectionItemsCount:(id)args
{
    NSNumber *sectionIndex = nil;
    ENSURE_ARG_AT_INDEX(sectionIndex, args, 0, NSNumber);
    TiUIListSectionProxy* section = [_sections objectAtIndex:[sectionIndex integerValue]];
    if (section) {
        return [section length];
    }
    return 0;
}

- (TiUIListSectionProxy *)getItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
    TiUIListSectionProxy* section = [self getSectionAt:args];
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
	TiUIListSectionProxy* section = [self getSectionAt:args];
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
	TiUIListSectionProxy* section = [self getSectionAt:args];
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
	TiUIListSectionProxy* section = [self getSectionAt:args];
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
	TiUIListSectionProxy* section = [self getSectionAt:args];
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
	TiUIListSectionProxy* section = [self getSectionAt:args];
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

-(void)hideDeleteButton:(id)args {
    [self dispatchUpdateAction:^(UITableView *tableView) {
		[tableView setEditing:NO animated:YES];
	} animated:YES];
}

-(void)closeSwipeMenu:(id)args {
    ENSURE_SINGLE_ARG_OR_NIL(args,NSNumber);
    [self makeViewPerformSelector:@selector(closeSwipeMenu:) withObject:args createIfNeeded:NO waitUntilDone:NO];
}

#pragma mark - Marker Support

-(NSIndexPath*)indexPathFromDictionary:(NSDictionary*) args
{
    BOOL valid = NO;
    NSInteger section = [TiUtils intValue:[args objectForKey:@"sectionIndex"] def:0 valid:&valid];
    if (!valid) {
        section = NSIntegerMax;
    }
    NSInteger row = [TiUtils intValue:[args objectForKey:@"itemIndex"] def:0 valid:&valid];
    if (!valid) {
        row = NSIntegerMax;
    }
    return [NSIndexPath indexPathForRow:row inSection:section];
}

-(BOOL)canAddMarker:(NSIndexPath*)marker
{
    //Checks if the marker is part of currently visible rows.
    __block BOOL canAddMarker = YES;
    TiThreadPerformOnMainThread(^{
        if ([self viewInitialized] && !self.listView.isSearchActive) {
            NSArray* visibleRows = [self.listView.tableView indexPathsForVisibleRows];
            canAddMarker = ![visibleRows containsObject:marker];
        }
    }, YES);
    
    return canAddMarker;
}

- (void)setMarker:(id)args;
{
    ENSURE_SINGLE_ARG(args, NSDictionary);
    NSIndexPath* marker = [self indexPathFromDictionary:args];
    if ([self canAddMarker:marker]) {
        pthread_rwlock_wrlock(&_markerLock);
        [_markerArray removeAllObjects];
        [_markerArray addObject:marker];
        pthread_rwlock_unlock(&_markerLock);
    } else if ([self _hasListeners:@"marker" checkParent:NO]){
        //Index path is currently visible. Fire
        NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                            NUMINTEGER(marker.section), @"sectionIndex",
                                            NUMINTEGER(marker.row), @"itemIndex",
                                            nil];
        [self fireEvent:@"marker" withObject:eventObject withSource:self propagate:NO reportSuccess:NO errorCode:0 message:nil];
        [eventObject release];
    }
}

- (void)addMarker:(id)args
{
    ENSURE_SINGLE_ARG(args, NSDictionary);
    NSIndexPath* marker = [self indexPathFromDictionary:args];
    if ([self canAddMarker:marker]) {
        pthread_rwlock_wrlock(&_markerLock);
        if (![_markerArray containsObject:marker]) {
            [_markerArray addObject:marker];
        }
        pthread_rwlock_unlock(&_markerLock);
    } else if ([self _hasListeners:@"marker" checkParent:NO]){
        //Index path is currently visible. Fire
        NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                            NUMINTEGER(marker.section), @"sectionIndex",
                                            NUMINTEGER(marker.row), @"itemIndex",
                                            nil];
        [self fireEvent:@"marker" withObject:eventObject withSource:self propagate:NO reportSuccess:NO errorCode:0 message:nil];
        [eventObject release];
    }
}

-(void)willDisplayCell:(NSIndexPath*)indexPath
{
    if (([_markerArray count] > 0) && [self _hasListeners:@"marker" checkParent:NO]) {
        //Never block the UI thread
        int result = pthread_rwlock_trywrlock(&_markerLock);
        if (result != 0) {
            return;
        }
        if ([_markerArray containsObject:indexPath]){
            
            NSMutableDictionary *eventObject = [[NSMutableDictionary alloc] initWithObjectsAndKeys:
                                                NUMINTEGER(indexPath.section), @"sectionIndex",
                                                NUMINTEGER(indexPath.row), @"itemIndex",
                                                nil];
            [self fireEvent:@"marker" withObject:eventObject propagate:NO];
            [_markerArray removeObject:indexPath];
            [eventObject release];
        }
        pthread_rwlock_unlock(&_markerLock);
    }
}

-(void)didOverrideEvent:(NSString*)type forItem:(TiUIListItemProxy*)item
{
    if ([type isEqualToString:@"load"] && [self autoResizeOnImageLoad]) {
        [self dispatchUpdateAction:^(UITableView *tableView) {
            [item dirtyItAll];
            [item.listItem setNeedsLayout];
        } animated:NO];
    }
}

DEFINE_DEF_BOOL_PROP(willScrollOnStatusTap,YES);
USE_VIEW_FOR_CONTENT_SIZE

@end

#endif
