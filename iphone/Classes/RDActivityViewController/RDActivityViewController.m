//
//  RDActivityViewController.m
//  APOD
//
//  Created by Robert Dougan on 11/5/12.
//  Copyright (c) 2012 Robert Dougan. All rights reserved.
//

#import "RDActivityViewController.h"

@interface RDActivityViewController () {
    NSMutableDictionary *_itemsMapping;
    int _maximumNumberOfItems;
}

@end

@implementation RDActivityViewController

@synthesize delegate = _delegate;
@synthesize placeholderItem = _placeholderItem;

- (id)initWithDelegate:(id<RDActivityViewControllerDelegate>)delegate {
    return [self initWithDelegate:delegate maximumNumberOfItems:10 applicationActivities:nil placeholderItem:nil];
}

- (id)initWithDelegate:(id)delegate maximumNumberOfItems:(int)maximumNumberOfItems {
    return [self initWithDelegate:delegate maximumNumberOfItems:maximumNumberOfItems applicationActivities:nil placeholderItem:nil];
}

- (id)initWithDelegate:(id)delegate maximumNumberOfItems:(int)maximumNumberOfItems applicationActivities:(NSArray *)applicationActivities {
    return [self initWithDelegate:delegate maximumNumberOfItems:maximumNumberOfItems applicationActivities:applicationActivities placeholderItem:nil];
}

- (id)initWithDelegate:(id)delegate maximumNumberOfItems:(int)maximumNumberOfItems applicationActivities:(NSArray *)applicationActivities placeholderItem:(id)placeholderItem {
    _delegate = delegate;
    _maximumNumberOfItems = maximumNumberOfItems;
    _placeholderItem = placeholderItem;
    NSMutableArray *items = [[NSMutableArray alloc] init];
    int i;
    
    for (i = 0; i < maximumNumberOfItems; i++) {
        [items addObject:self];
    }
    
    self = [self initWithActivityItems:items applicationActivities:applicationActivities];
    if (self) {
        _itemsMapping = [[NSMutableDictionary alloc] init];
    }
    
    return self;
}

- (id)activityViewController:(UIActivityViewController *)activityViewController itemForActivityType:(NSString *)activityType {
    // Get the items if not already received
    NSMutableDictionary *activity = [_itemsMapping objectForKey:activityType];
    NSArray *items = nil;
    
    if (!activity) {
        if ([_delegate respondsToSelector:@selector(activityViewController:itemsForActivityType:)]) {
            items = [_delegate activityViewController:activityViewController itemsForActivityType:activityType];
            activity = [[NSMutableDictionary alloc] initWithObjectsAndKeys:items, @"items", [NSNumber numberWithInt:0], @"index", nil];
        }
        else {
            activity = [[NSMutableDictionary alloc] initWithObjectsAndKeys:[NSNumber numberWithInt:0], @"index", nil];
        }
        [_itemsMapping setObject:activity forKey:activityType];
    } else {
        items = [activity objectForKey:@"items"];
    }
    
    // Get the item
    int index = [[activity objectForKey:@"index"] integerValue];
    id item = nil;
    
    if (index < [items count]) {
        item = [items objectAtIndex:index];
    }
    
    // Increase the index, and reset
    index = (index + 1) % _maximumNumberOfItems;
    [activity setObject:[NSNumber numberWithInt:index] forKey:@"index"];
    
    return item;
}

- (NSString *)activityViewController:(UIActivityViewController *)activityViewController subjectForActivityType:(NSString *)activityType
{
    if ([_delegate respondsToSelector:@selector(activityViewController:subjectForActivityType:)]) {
        return [_delegate activityViewController:activityViewController subjectForActivityType:activityType];
    }
}
- (NSString *)activityViewController:(UIActivityViewController *)activityViewController dataTypeIdentifierForActivityType:(NSString *)activityType
{
    if ([_delegate respondsToSelector:@selector(activityViewController:dataTypeIdentifierForActivityType:)]) {
        return [_delegate activityViewController:activityViewController dataTypeIdentifierForActivityType:activityType];
    }
}

- (UIImage *)activityViewController:(UIActivityViewController *)activityViewController thumbnailImageForActivityType:(NSString *)activityType suggestedSize:(CGSize)size
{
    if ([_delegate respondsToSelector:@selector(activityViewController:thumbnailImageForActivityType:suggestedSize:)]) {
        return [_delegate activityViewController:activityViewController thumbnailImageForActivityType:activityType suggestedSize:size];
    }
}

- (id)activityViewControllerPlaceholderItem:(UIActivityViewController *)activityViewController {
    if(_placeholderItem == nil) { return @""; }
    return _placeholderItem;
}

@end
