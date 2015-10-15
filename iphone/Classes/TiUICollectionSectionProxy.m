/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#ifdef USE_TI_UICOLLECTIONVIEW

#import "TiUICollectionSectionProxy.h"
#import "TiUICollectionViewProxy.h"
#import "TiUICollectionView.h"
#import "TiUICollectionItem.h"
#import "NSDictionary+Merge.h"

@interface TiUICollectionView()
-(TiViewProxy*)wrapperProxyWithVerticalLayout:(BOOL)vertical;
@end

@interface TiUICollectionSectionProxy ()
@property (nonatomic, readonly) id<TiUICollectionViewDelegate> dispatcher;
@end

@implementation TiUICollectionSectionProxy {
	NSMutableArray *_items;
    BOOL _hidden;
    NSMutableDictionary* _storedSectionViews;
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
        _storedSectionViews = [[NSMutableDictionary alloc] init];
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
    if (_storedSectionViews) {
        [_storedSectionViews enumerateKeysAndObjectsUsingBlock:^(id key, TiViewProxy* childProxy, BOOL *stop) {
            [childProxy setParent:nil];
            [childProxy detachView];
            [self forgetProxy:childProxy];
        }];
        RELEASE_TO_NIL(_storedSectionViews)
    }
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.UI.CollectionSection";
}

- (id<TiUICollectionViewDelegate>)dispatcher
{
	return _delegate != nil ? _delegate : self;
}

// These API's are used by the CollectionView directly. Not for public consumption
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
        DebugLog(@"[WARN] CollectionSectionProxy: deleteItemAtIndex index is out of range");
    } else {
        [_items removeObjectAtIndex:index];
    }
}

- (void) addItem:(NSDictionary*)item atIndex:(NSUInteger)index
{
    if (index > [_items count]) {
        DebugLog(@"[WARN] CollectionSectionProxy: addItem:atIndex: index is out of range");
    } else {
        if (index == [_items count]) {
            [_items addObject:item];
        } else {
            [_items insertObject:item atIndex:index];
        }
    }
}

-(TiViewProxy*)currentViewForLocation:(NSString*)location inCollectionView:(TiUICollectionView*)listView
{
    if ([_storedSectionViews objectForKey:location]) {
        return [[[_storedSectionViews objectForKey:location] children] firstObject];
    }
    return nil;
}


-(TiViewProxy*)sectionViewForLocation:(NSString*)location inCollectionView:(TiUICollectionView*)listView
{
    if ([_storedSectionViews objectForKey:location]) {
        return [_storedSectionViews objectForKey:location];
    }
    id value = [self valueForKey:location];
    TiViewProxy* viewproxy = (TiViewProxy*)[self createChildFromObject:value];
    if (viewproxy) {
        LayoutConstraint *viewLayout = [viewproxy layoutProperties];
        //If height is not dip, explicitly set it to SIZE
        if (viewLayout->height.type != TiDimensionTypeDip) {
            viewLayout->height = TiDimensionAutoSize;
        }
        if (viewLayout->width.type == TiDimensionTypeUndefined) {
            viewLayout->width = TiDimensionAutoFill;
        }
//        TiViewProxy* wrapperProxy = [listView initWrapperProxyWithVerticalLayout:YES];
//        [wrapperProxy add:viewproxy];
        [_storedSectionViews setObject:viewproxy forKey:location];
        return viewproxy;
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
	[self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
        _hidden = !visible;
		[tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex]];
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
		return [self itemCountInternal];
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
    UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
    NSUInteger oldCount = [_items count];
    NSUInteger newCount = [items count];
    if ((animation == UITableViewRowAnimationNone) && (oldCount != newCount)) {
        NSUInteger minCount = MIN(oldCount, newCount);
        NSUInteger maxCount = MAX(oldCount, newCount);
        NSUInteger diffCount = maxCount - minCount;
        
        //Dispath block for difference
        [self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
            [_items setArray:items];
            NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:diffCount];
            for (NSUInteger i = 0; i < diffCount; ++i) {
                [indexPaths addObject:[NSIndexPath indexPathForRow:(minCount + i) inSection:_sectionIndex]];
            }
            if (newCount > oldCount) {
                [tableView insertItemsAtIndexPaths:indexPaths];
            } else {
                [tableView deleteItemsAtIndexPaths:indexPaths];
            }
            [indexPaths release];
        } animated:(animation != UITableViewRowAnimationNone)];
        
        //Dispatch block for common items
        if (minCount > 0) {
            [self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
                NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:minCount];
                for (NSUInteger i = 0; i < minCount; ++i) {
                    [indexPaths addObject:[NSIndexPath indexPathForRow:i inSection:_sectionIndex]];
                }
                [tableView reloadItemsAtIndexPaths:indexPaths];
                [indexPaths release];
            } animated:(animation != UITableViewRowAnimationNone)];
        }
        
    } else {
        [self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
            [_items setArray:items];
            [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex]];
        } animated:(animation != UITableViewRowAnimationNone)];
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
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	[self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
		NSUInteger insertIndex = [_items count];
		[_items addObjectsFromArray:items];
		NSUInteger count = [items count];
		NSMutableArray *indexPaths = [[NSMutableArray alloc] initWithCapacity:count];
		for (NSUInteger i = 0; i < count; ++i) {
			[indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
		}
		[tableView insertItemsAtIndexPaths:indexPaths];
        if (insertIndex == 0) {
            [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex]];
        }
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
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];

    [self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
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
		[tableView insertItemsAtIndexPaths:indexPaths];
        if (insertIndex == 0) {
            [tableView reloadSections:[NSIndexSet indexSetWithIndex:_sectionIndex]];
        }
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
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	
	[self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
		if ([_items count] < insertIndex) {
			DebugLog(@"[WARN] CollectionView: Replace item index is out of range");
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
			[tableView deleteItemsAtIndexPaths:indexPaths];
		}
		[indexPaths removeAllObjects];
		for (NSUInteger i = 0; i < count; ++i) {
			[indexPaths addObject:[NSIndexPath indexPathForRow:insertIndex+i inSection:_sectionIndex]];
		}
		if (count > 0) {
			[tableView insertItemsAtIndexPaths:indexPaths];
		}
		[indexPaths release];
	} animated:(animation != UITableViewRowAnimationNone)];
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
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	BOOL animated = (animation != UITableViewRowAnimationNone);
	[self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
		if ([_items count] <= deleteIndex) {
			DebugLog(@"[WARN] CollectionView: Delete item index is out of range");
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
		[tableView deleteItemsAtIndexPaths:indexPaths];
        
        {
            //prevent a weird effect where the first cell after the removed one would end up selected
            double delayInSeconds = 0.001;
            dispatch_time_t popTime = dispatch_time(DISPATCH_TIME_NOW, delayInSeconds * NSEC_PER_SEC);
            NSIndexPath* toDeselect = indexPaths.firstObject;
            dispatch_after(popTime, dispatch_get_main_queue(), ^(void){
                //code to be executed on the main queue after delay
                [tableView deselectItemAtIndexPath:toDeselect animated:NO];
            });
        }
		[indexPaths release];
	} animated:animated maintainPosition:NO];
}

- (void)updateItemAt:(id)args
{
	ENSURE_ARG_COUNT(args, 1);
	NSUInteger itemIndex = [TiUtils intValue:[args objectAtIndex:0]];
	NSDictionary *item = [args count] > 1 ? [args objectAtIndex:1] : nil;
	ENSURE_TYPE_OR_NIL(item,NSDictionary);
	NSDictionary *properties = [args count] > 2 ? [args objectAtIndex:2] : nil;
	UITableViewRowAnimation animation = [TiUICollectionView animationStyleForProperties:properties];
	
	[self.dispatcher dispatchUpdateAction:^(UICollectionView *tableView) {
		if ([_items count] <= itemIndex) {
			DebugLog(@"[WARN] CollectionView: Update item index is out of range");
			return;
		}
        NSArray *indexPaths = [[NSArray alloc] initWithObjects:[NSIndexPath indexPathForRow:itemIndex inSection:_sectionIndex], nil];
        TiUICollectionItem *cell = (TiUICollectionItem *)[tableView cellForItemAtIndexPath:[indexPaths objectAtIndex:0]];
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
			[tableView reloadItemsAtIndexPaths:indexPaths];
		}
		[indexPaths release];
	} animated:(animation != UITableViewRowAnimationNone)];
}

#pragma mark - TiUICollectionViewDelegate

- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:NO];
}

- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block maintainPosition:(BOOL)maintain
{
    [self dispatchUpdateAction:block animated:YES maintainPosition:NO];
}

- (void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block animated:(BOOL)animated
{
    [self dispatchUpdateAction:block animated:animated maintainPosition:NO];
}

-(void)dispatchUpdateAction:(void(^)(UICollectionView *tableView))block animated:(BOOL)animated maintainPosition:(BOOL)maintain
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
