/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UILISTVIEW

#import "TiUIListSectionProxy.h"
#import "TiUIListViewProxy.h"
#import "TiUIListView.h"
#import "TiUIListItem.h"
#import "NSDictionary+Merge.h"

@interface TiUIListView()
-(TiViewProxy*)wrapperProxyWithVerticalLayout:(BOOL)vertical;
@end

@implementation TiUIListSectionProxy {
	NSMutableArray *_items;
    BOOL _hidden;
}

@synthesize delegate = _delegate;
@synthesize sectionIndex = _sectionIndex;
@synthesize headerTitle = _headerTitle;
@synthesize footerTitle = _footerTitle;

- (id)init
{
    self = [super init];
    if (self) {
		_items = [[NSMutableArray alloc] initWithCapacity:20];
        _hidden = false;
        _hideWhenEmpty = NO;
        _showHeaderWhenHidden = NO;
    }
    return self;
}

- (void)dealloc
{
	_delegate = nil;
    RELEASE_TO_NIL(_items)
    RELEASE_TO_NIL(_headerTitle)
    RELEASE_TO_NIL(_footerTitle)
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.UI.ListSection";
}

- (id<TiUIListViewDelegate>)dispatcher
{
	return _delegate != nil ? _delegate : self;
}

// These API's are used by the ListView directly. Not for public consumption
- (NSDictionary *)itemAtIndex:(NSUInteger)index
{
	if (index < [_items count]) {
		id item = [_items objectAtIndex:index];
		if ([item isKindOfClass:[NSDictionary class]]) {
			return item;
		}
	}
	return nil;
}

- (void) deleteItemAtIndex:(NSUInteger)index
{
    if ([_items count] <= index) {
        DebugLog(@"[WARN] ListSectionProxy: deleteItemAtIndex index is out of range");
    } else {
        [_items removeObjectAtIndex:index];
    }
}

- (void) addItem:(NSDictionary*)item atIndex:(NSUInteger)index
{
    if (index > [_items count]) {
        DebugLog(@"[WARN] ListSectionProxy: addItem:atIndex: index is out of range");
    } else {
        if (index == [_items count]) {
            [_items addObject:item];
        } else {
            [_items insertObject:item atIndex:index];
        }
    }
}

-(void)setHeaderView:(id)value
{
    NSString* wrapperKey = [NSString stringWithFormat:@"%@Wrapper", @"headerView"];
    [self removeHoldedProxyForKey:wrapperKey];
    [self addObjectToHold:value forKey:@"headerView"];
    [self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
        [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation:UITableViewRowAnimationNone];
    }];
    [self replaceValue:value forKey:@"headerView" notification:NO];
}

-(void)setFooterView:(id)value
{
    NSString* wrapperKey = [NSString stringWithFormat:@"%@Wrapper", @"footerView"];
    [self removeHoldedProxyForKey:wrapperKey];
    [self addObjectToHold:value forKey:@"footerView"];
    [self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
        [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation:UITableViewRowAnimationNone];
    }];
    [self replaceValue:value forKey:@"footerView" notification:NO];
}

-(TiViewProxy*)currentViewForLocation:(NSString*)location inListView:(TiUIListView*)listView
{
    return (TiViewProxy*)[self holdedProxyForKey:location];
}

-(TiViewProxy*)sectionViewForLocation:(NSString*)location inListView:(TiUIListView*)listView
{
    NSString* wrapperKey = [NSString stringWithFormat:@"%@Wrapper", location];
    TiProxy* vp = [self holdedProxyForKey:wrapperKey];
    if (vp) {
        return (TiViewProxy*)vp;
    }
    vp = [self holdedProxyForKey:location];
    if (IS_OF_CLASS(vp, TiViewProxy)) {
        ((TiViewProxy*)vp).canBeResizedByFrame = YES;
        TiViewProxy* wrapperProxy = [listView wrapperProxyWithVerticalLayout:NO];
        [wrapperProxy add:vp];
        [self addProxyToHold:wrapperProxy forKey:wrapperKey];
        return wrapperProxy;
    }
    return nil;
}


#pragma mark - Public API

- (void)setVisible:(id)args
{
    BOOL visible = YES;
    BOOL animated = YES;
    if ([args isKindOfClass:[NSArray class]]) {
        id value = nil;
        NSNumber* anim = nil;
        ENSURE_ARG_AT_INDEX(value , args, 0, NSObject);
        ENSURE_ARG_OR_NIL_AT_INDEX(anim, args, 1, NSNumber);
        if (anim != nil)
            animated = [anim boolValue];
        visible = [TiUtils boolValue:value def:visible];
    }
    else {
        visible = [TiUtils boolValue:args def:visible];
    }
    if (_hidden == !visible) return;
	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
        _hidden = !visible;
		[tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation:animated?UITableViewRowAnimationAutomatic:UITableViewRowAnimationNone];
	} animated:animated];
}

- (id)visible
{
    return NUMBOOL(!_hidden);
}

- (BOOL)isHidden
{
    return (_hidden && !_showHeaderWhenHidden) || ([_items count] == 0 && _hideWhenEmpty);
}



- (void)show:(id)arg
{
	[self setVisible:@(YES)];
}

-(void)hide:(id)arg
{
	[self setVisible:@(NO)];
}

- (NSArray *)items
{
	return [self.dispatcher dispatchBlockWithResult:^() {
		return [[_items copy] autorelease];
	}];
}

-(id)length
{
    return @([_items count]);
}

- (NSUInteger)itemCountInternal
{
    return _hidden?0:[_items count];
}

- (NSUInteger)itemCount
{
//	return [[self.dispatcher dispatchBlockWithResult:^() {
		return _hidden?0:[_items count];
//	}] unsignedIntegerValue];
}


- (id)getItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:0]];
	return [self.dispatcher dispatchBlockWithResult:^() {
		return (itemIndex < [_items count]) ? [_items objectAtIndex:itemIndex] : nil;
	}];
}

- (void)setItems:(id)args
{
	[self setItems:args withObject:[NSDictionary dictionaryWithObject:NUMINT(UITableViewRowAnimationNone) forKey:@"animationStyle"]];
}

- (void)setItems:(id)args withObject:(id)properties
{
    ENSURE_TYPE_OR_NIL(args,NSArray);
    NSArray *items = args;
    UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];
    id<TiUIListViewDelegate> theDispatcher = self.dispatcher;
    
    if (animation == UITableViewRowAnimationNone) {
        [theDispatcher dispatchBlock:^(UITableView *tableView) {
            [_items setArray:items];
            id <TiUIListViewDelegateView> theDelegate = [theDispatcher delegateView];
            if (theDelegate != nil) {
                [theDelegate updateSearchResults:nil];
                if ([theDispatcher isKindOfClass:[TiViewProxy class]]) {
                    [(TiViewProxy*)theDispatcher contentsWillChange];
                }
            }
        }];
    } else {
        [theDispatcher dispatchUpdateAction:^(UITableView *tableView) {
            [_items setArray:items];
            [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation:animation];
        } maintainPosition:maintainPosition];
    }
}

- (void)appendItems:(id)args
{
    ENSURE_ARG_COUNT(args, 1);
    NSArray *items = [args objectAtIndex:0];
    if ([items count] == 0) {
        return;
    }
    ENSURE_TYPE_OR_NIL(items,NSArray);
    NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
    UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];
    id<TiUIListViewDelegate> theDispatcher = self.dispatcher;
    
    if (animation == UITableViewRowAnimationNone) {
        [theDispatcher dispatchBlock:^(UITableView* tableView) {
            [_items addObjectsFromArray:items];
            id <TiUIListViewDelegateView> theDelegate = [theDispatcher delegateView];
            if (theDelegate != nil) {
                [theDelegate updateSearchResults:nil];
                if ([theDispatcher isKindOfClass:[TiViewProxy class]]) {
                    [(TiViewProxy*)theDispatcher contentsWillChange];
                }
            }
        }];
    } else {
        [theDispatcher dispatchUpdateAction:^(UITableView* tableView) {
            NSUInteger insertIndex = [_items count];
            [_items addObjectsFromArray:items];
            NSUInteger count = [items count];
            NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:count];
            for (NSUInteger i = 0; i < count; ++i) {
                [indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
            }
            [tableView insertRowsAtIndexPaths:indexPaths withRowAnimation:animation];
            if (insertIndex == 0) {
                [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation: animation];
            }

            [indexPaths release];
        } maintainPosition:maintainPosition];
    }
}

- (void)insertItemsAt:(id)args
{
    ENSURE_ARG_COUNT(args, 2);
    NSUInteger insertIndex = [TiUtils intValue:[args objectAtIndex:0]];
    NSArray *items = [args objectAtIndex:1];
    if ([items count] == 0) {
        return;
    }
    ENSURE_TYPE_OR_NIL(items,NSArray);
    NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
    UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];
    id<TiUIListViewDelegate> theDispatcher = self.dispatcher;

    if (animation == UITableViewRowAnimationNone) {
        [theDispatcher dispatchBlock:^(UITableView* tableView) {
            if ([_items count] < insertIndex) {
                DebugLog(@"[WARN] ListView: Insert item index is out of range");
                return;
            }
            [_items replaceObjectsInRange:NSMakeRange(insertIndex, 0) withObjectsFromArray:items];
            id <TiUIListViewDelegateView> theDelegate = [theDispatcher delegateView];
            if (theDelegate != nil) {
                [theDelegate updateSearchResults:nil];
                if ([theDispatcher isKindOfClass:[TiViewProxy class]]) {
                    [(TiViewProxy*)theDispatcher contentsWillChange];
                }
            }
        }];
    } else {
        [theDispatcher dispatchUpdateAction :^(UITableView* tableView) {
            NSInteger currentCount = [_items count];
            if (currentCount < insertIndex) {
                DebugLog(@"[WARN] ListView: Insert item index is out of range");
                return;
            }
            [_items replaceObjectsInRange:NSMakeRange(insertIndex, 0) withObjectsFromArray:items];
            NSUInteger count = [items count];
            NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:count];
            for (NSUInteger i = 0; i < count; ++i) {
                [indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
            }
            [tableView insertRowsAtIndexPaths:indexPaths withRowAnimation:animation];
            if (currentCount == 0) {
                [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation: animation];
                
            }
            [indexPaths release];
        } maintainPosition:maintainPosition];
    }
}

- (void)replaceItemsAt:(id)args
{
    ENSURE_ARG_COUNT(args, 3);
    NSUInteger insertIndex = [TiUtils intValue:[args objectAtIndex:0]];
    NSUInteger replaceCount = [TiUtils intValue:[args objectAtIndex:1]];
    NSArray *items = [args objectAtIndex:2];
    ENSURE_TYPE_OR_NIL(items,NSArray);
    NSDictionary *properties = [args count] > 3 ? [args objectAtIndex:3] : nil;
    UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];
    id<TiUIListViewDelegate> theDispatcher = self.dispatcher;
	
    if (animation == UITableViewRowAnimationNone) {
        [theDispatcher dispatchBlock:^(UITableView* tableView) {
            if ([_items count] < insertIndex) {
                DebugLog(@"[WARN] ListView: Replace item index is out of range");
                return;
            }
            NSUInteger actualReplaceCount = MIN(replaceCount, [_items count]-insertIndex);
            [_items replaceObjectsInRange:NSMakeRange(insertIndex, actualReplaceCount) withObjectsFromArray:items];
            id <TiUIListViewDelegateView> theDelegate = [theDispatcher delegateView];
            if (theDelegate != nil) {
                [theDelegate updateSearchResults:nil];
                if ([theDispatcher isKindOfClass:[TiViewProxy class]]) {
                    [(TiViewProxy*)theDispatcher contentsWillChange];
                }
            }
        }];
    } else {
        [theDispatcher dispatchUpdateAction:^(UITableView* tableView) {
            if ([_items count] < insertIndex) {
                DebugLog(@"[WARN] ListView: Replace item index is out of range");
                return;
            }
            NSUInteger actualReplaceCount = MIN(replaceCount, [_items count]-insertIndex);
            [_items replaceObjectsInRange:NSMakeRange(insertIndex, actualReplaceCount) withObjectsFromArray:items];
            NSUInteger count = [items count];
            NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:MAX(count, actualReplaceCount)];
            for (NSUInteger i = 0; i < actualReplaceCount; ++i) {
                [indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
            }
            if (actualReplaceCount > 0) {
                [tableView deleteRowsAtIndexPaths:indexPaths withRowAnimation:animation];
            }
            [indexPaths removeAllObjects];
            for (NSUInteger i = 0; i < count; ++i) {
                [indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
            }
            if (count > 0) {
                [tableView insertRowsAtIndexPaths:indexPaths withRowAnimation:animation];
            }
            [indexPaths release];
        } maintainPosition:maintainPosition];
    }
}

-(void)willRemoveItemAt:(NSIndexPath*)indexPath
{
    [_items removeObjectsInRange:NSMakeRange(indexPath.row, 1)];
}

- (void)deleteItemsAt:(id)args
{
    ENSURE_ARG_COUNT(args, 2);
    NSUInteger deleteIndex = [TiUtils intValue:[args objectAtIndex:0]];
    NSUInteger deleteCount = [TiUtils intValue:[args objectAtIndex:1]];
    if (deleteCount == 0) {
        return;
    }
    NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
    UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];
    id<TiUIListViewDelegate> theDispatcher = self.dispatcher;
	
    if (animation == UITableViewRowAnimationNone) {
        [theDispatcher dispatchBlock:^(UITableView* tableView) {
            if ([_items count] <= deleteIndex) {
                DebugLog(@"[WARN] ListView: Delete item index is out of range");
                return;
            }
            NSUInteger actualDeleteCount = MIN(deleteCount, [_items count]-deleteIndex);
            if (actualDeleteCount == 0) {
                return;
            }
            [_items removeObjectsInRange:NSMakeRange(deleteIndex, actualDeleteCount)];
            id <TiUIListViewDelegateView> theDelegate = [theDispatcher delegateView];
            if (theDelegate != nil) {
                [theDelegate updateSearchResults:nil];
                if ([theDispatcher isKindOfClass:[TiViewProxy class]]) {
                    [(TiViewProxy*)theDispatcher contentsWillChange];
                }
            }
        }];
    } else {
        [theDispatcher dispatchUpdateAction:^(UITableView* tableView) {
            if ([_items count] <= deleteIndex) {
                DebugLog(@"[WARN] ListView: Delete item index is out of range");
                return;
            }
            NSUInteger actualDeleteCount = MIN(deleteCount, [_items count]-deleteIndex);
            if (actualDeleteCount == 0) {
                return;
            }
            [_items removeObjectsInRange:NSMakeRange(deleteIndex, actualDeleteCount)];
            NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:actualDeleteCount];
            for (NSUInteger i = 0; i < actualDeleteCount; ++i) {
                [indexPaths addObject:[NSIndexPath indexPathForRow:deleteIndex+i inSection:_sectionIndex]];
            }
            [tableView deleteRowsAtIndexPaths:indexPaths withRowAnimation:animation];
            [indexPaths release];
        } maintainPosition:maintainPosition];
    }
}

- (void)updateItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *item = [args count] > 1 ? [args objectAtIndex:1] : nil;
	ENSURE_TYPE_OR_NIL(item,NSDictionary);
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];

	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
		if ([_items count] <= itemIndex) {
			DebugLog(@"[WARN] ListView: Update item index is out of range");
			return;
		}
        NSArray *indexPaths = [[NSArray alloc] initWithObjects:[NSIndexPath indexPathForRow:itemIndex inSection:_sectionIndex], nil];
        TiUIListItem *cell = (TiUIListItem *)[tableView cellForRowAtIndexPath:[indexPaths objectAtIndex:0]];
		BOOL forceReload = (animation != UITableViewRowAnimationNone);
        if (item) {
            NSDictionary* currentItem = [[_items objectAtIndex:itemIndex] dictionaryByMergingWith:item force:YES];
            if (currentItem)[_items replaceObjectAtIndex:itemIndex withObject:currentItem];
            if (!forceReload) {
                if ((cell != nil) && ([cell canApplyDataItem:currentItem])) {
                    cell.dataItem = currentItem;
                    [cell setNeedsLayout];
                } else {
                    forceReload = YES;
                }
            }
        } else {
            [cell.proxy dirtyItAll];
            [cell setNeedsLayout];
        }
		if (forceReload) {
			[tableView reloadRowsAtIndexPaths:indexPaths withRowAnimation:animation];
		}
		[indexPaths release];
	} animated:(animation != UITableViewRowAnimationNone) maintainPosition:maintainPosition];
}

- (void)updateItems:(id)args
{
    ENSURE_ARG_COUNT(args, 1);
    NSArray *items = [args objectAtIndex:0];
    if ([items count] == 0) {
        return;
    }
    ENSURE_TYPE_OR_NIL(items,NSArray);
    
    NSDictionary *properties = [args count] > 1 ? [args objectAtIndex:1] : nil;
    UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
    BOOL maintainPosition = [TiUtils boolValue:@"maintainPosition" properties:properties def:NO];
    [self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
        if ([items count] != [_items count]) {
            [self throwException:@"Can't update items, count does not match"
                       subreason:nil
                        location:CODELOCATION];
            return;
        }
        NSUInteger count = [items count];
        BOOL forceReload = (animation != UITableViewRowAnimationNone);
        [items enumerateObjectsUsingBlock:^(id item, NSUInteger itemIndex, BOOL *stop) {
            TiUIListItem *cell = (TiUIListItem *)[tableView cellForRowAtIndexPath:[NSIndexPath indexPathForRow:itemIndex inSection:_sectionIndex]];
                NSDictionary* currentItem = [[_items objectAtIndex:itemIndex] dictionaryByMergingWith:item force:YES];
                if (currentItem)[_items replaceObjectAtIndex:itemIndex withObject:currentItem];
                if (!forceReload) {
                    if ((cell != nil) && ([cell canApplyDataItem:currentItem])) {
                        cell.dataItem = currentItem;
                        [cell setNeedsLayout];
                    }
                }
        }];
        if (forceReload) {
            [tableView reloadRowsAtIndexPaths:[tableView indexPathsForVisibleRows] withRowAnimation:animation];
        }
    } animated:(animation != UITableViewRowAnimationNone) maintainPosition:maintainPosition];
}

#pragma mark - TiUIListViewDelegate

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block
{
    [self dispatchUpdateAction:block animated:YES];
}

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block maintainPosition:(BOOL)maintain
{
    [self dispatchUpdateAction:block animated:YES];
}
-(void)dispatchUpdateAction:(void(^)(UITableView *tableView))block animated:(BOOL)animated
{
    if (animated)
    {
        block(nil);
    }
    else {
        [UIView setAnimationsEnabled:NO];
        block(nil);
        [UIView setAnimationsEnabled:YES];
    }
}

- (void)dispatchBlock:(void(^)(UITableView *tableView))block
{
    block(nil);
}

- (id)dispatchBlockWithResult:(id (^)(void))block
{
	return block();
}

- (id<TiUIListViewDelegateView>) delegateView
{
    return nil;
}
@end

#endif
