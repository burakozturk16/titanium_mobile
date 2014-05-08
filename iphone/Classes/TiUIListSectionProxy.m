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

@interface TiUIListSectionProxy ()
@property (nonatomic, readonly) id<TiUIListViewDelegate> dispatcher;
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
    return _hidden;
}

- (void)show:(id)arg
{
	[self setVisible:YES];
}

-(void)hide:(id)arg
{
	[self setVisible:NO];
}

- (NSArray *)items
{
	return [self.dispatcher dispatchBlockWithResult:^() {
		return [[_items copy] autorelease];
	}];
}

- (NSUInteger)itemCount
{
	return [[self.dispatcher dispatchBlockWithResult:^() {
		return _hidden?0:[NSNumber numberWithUnsignedInteger:[_items count]];
	}] unsignedIntegerValue];
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
	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
		[_items setArray:items];
		[tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex] withRowAnimation:animation];
	} animated:(animation != UITableViewRowAnimationNone)];
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
	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
		NSUInteger insertIndex = [_items count];
		[_items addObjectsFromArray:items];
		NSUInteger count = [items count];
		NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:count];
		for (NSUInteger i = 0; i < count; ++i) {
			[indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
		}
		[tableView insertRowsAtIndexPaths:indexPaths withRowAnimation:animation];
		[indexPaths release];
	} animated:(animation != UITableViewRowAnimationNone)];
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

	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
		if ([_items count] < insertIndex) {
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
		[indexPaths release];
	} animated:(animation != UITableViewRowAnimationNone)];
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
	
	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
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
	} animated:(animation != UITableViewRowAnimationNone)];
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
	
	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
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
	} animated:(animation != UITableViewRowAnimationNone)];
}

- (void)updateItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 2);
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *item = [args objectAtIndex:1];
	ENSURE_TYPE(item,NSDictionary);
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUIListView animationStyleForProperties:properties];
	
	[self.dispatcher dispatchUpdateAction:^(UITableView *tableView) {
		if ([_items count] <= itemIndex) {
			DebugLog(@"[WARN] ListView: Update item index is out of range");
			return;
		}
        NSDictionary* currentItem = [[_items objectAtIndex:itemIndex] dictionaryByMergingWith:item force:YES];
		if (currentItem)[_items replaceObjectAtIndex:itemIndex withObject:currentItem];
		NSArray *indexPaths = [[NSArray alloc] initWithObjects:[NSIndexPath indexPathForRow:itemIndex inSection:_sectionIndex], nil];
		BOOL forceReload = (animation != UITableViewRowAnimationNone);
		if (!forceReload) {
			TiUIListItem *cell = (TiUIListItem *)[tableView cellForRowAtIndexPath:[indexPaths objectAtIndex:0]];
			if ((cell != nil) && ([cell canApplyDataItem:item])) {
				cell.dataItem = item;
                [cell setNeedsLayout];
			} else {
				forceReload = YES;
			}
		}
		if (forceReload) {
			[tableView reloadRowsAtIndexPaths:indexPaths withRowAnimation:animation];
		}
		[indexPaths release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

#pragma mark - TiUIListViewDelegate

- (void)dispatchUpdateAction:(void(^)(UITableView *tableView))block
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

- (id)dispatchBlockWithResult:(id (^)(void))block
{
	return block();
}

@end

#endif
