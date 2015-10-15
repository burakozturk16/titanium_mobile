/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 * WARNING: This is generated code. Modify at your own risk and without support.
 */

#import "TiUICollectionViewFlowLayout.h"


@implementation TiUICollectionViewFlowLayout

//- (BOOL) shouldInvalidateLayoutForBoundsChange:(CGRect)newBounds {
//    return YES;
//}
//
//- (NSArray *) layoutAttributesForElementsInRect:(CGRect)rect {
//    NSMutableArray *attributes = [[super layoutAttributesForElementsInRect:rect] mutableCopy];
//    NSMutableArray *visibleSectionsWithoutHeader = [NSMutableArray array];
//    for (UICollectionViewLayoutAttributes *itemAttributes in attributes) {
//        if (![visibleSectionsWithoutHeader containsObject:[NSNumber numberWithInteger:itemAttributes.indexPath.section]]) {
//            [visibleSectionsWithoutHeader addObject:[NSNumber numberWithInteger:itemAttributes.indexPath.section]];
//        }
//        if (itemAttributes.representedElementKind==UICollectionElementKindSectionHeader) {
//            NSUInteger indexOfSectionObject=[visibleSectionsWithoutHeader indexOfObject:[NSNumber numberWithInteger:itemAttributes.indexPath.section]];
//            if (indexOfSectionObject!=NSNotFound) {
//                [visibleSectionsWithoutHeader removeObjectAtIndex:indexOfSectionObject];
//            }
//        }
//    }
//    for (NSNumber *sectionNumber in visibleSectionsWithoutHeader) {
//        if ([self shouldStickHeaderToTopInSection:[sectionNumber integerValue]]) {
//            UICollectionViewLayoutAttributes *headerAttributes=[self layoutAttributesForSupplementaryViewOfKind:UICollectionElementKindSectionHeader atIndexPath:[NSIndexPath indexPathForItem:0 inSection:[sectionNumber integerValue]]];
//            if (headerAttributes.frame.size.width>0 && headerAttributes.frame.size.height>0) {
//                [attributes addObject:headerAttributes];
//            }
//        }
//    }
//    for (UICollectionViewLayoutAttributes *itemAttributes in attributes) {
//        if (itemAttributes.representedElementKind==UICollectionElementKindSectionHeader) {
//            UICollectionViewLayoutAttributes *headerAttributes = itemAttributes;
//            if ([self shouldStickHeaderToTopInSection:headerAttributes.indexPath.section]) {
//                CGPoint contentOffset = self.collectionView.contentOffset;
//                CGPoint originInCollectionView=CGPointMake(headerAttributes.frame.origin.x-contentOffset.x, headerAttributes.frame.origin.y-contentOffset.y);
//                originInCollectionView.y-=self.collectionView.contentInset.top;
//                CGRect frame = headerAttributes.frame;
//                if (originInCollectionView.y<0) {
//                    frame.origin.y+=(originInCollectionView.y*-1);
//                }
//                NSInteger numberOfSections = 1;
//                if ([self.collectionView.dataSource respondsToSelector:@selector(numberOfSectionsInCollectionView:)]) {
//                    numberOfSections = [self.collectionView.dataSource numberOfSectionsInCollectionView:self.collectionView];
//                }
//                if (numberOfSections>headerAttributes.indexPath.section+1) {
//                    UICollectionViewLayoutAttributes *nextHeaderAttributes=[self layoutAttributesForSupplementaryViewOfKind:UICollectionElementKindSectionHeader atIndexPath:[NSIndexPath indexPathForItem:0 inSection:headerAttributes.indexPath.section+1]];
//                    CGFloat maxY=nextHeaderAttributes.frame.origin.y;
//                    if (CGRectGetMaxY(frame)>=maxY) {
//                        frame.origin.y=maxY-frame.size.height;
//                    }
//                }
//                headerAttributes.frame = frame;
//            }
//            headerAttributes.zIndex = 1024;
//        }
//    }
//    return [attributes autorelease];
//}
- (BOOL)shouldStickHeaderToTopInSection:(NSUInteger)section{
    BOOL shouldStickToTop=YES;
    if ([self.collectionView.delegate conformsToProtocol:@protocol(TiUICollectionViewFlowLayoutDelegate)]) {
        id<TiUICollectionViewFlowLayoutDelegate> stickyHeadersDelegate=(id<TiUICollectionViewFlowLayoutDelegate>)self.collectionView.delegate;
        if ([stickyHeadersDelegate respondsToSelector:@selector(shouldStickHeaderToTopInSection:)]) {
            shouldStickToTop=[stickyHeadersDelegate shouldStickHeaderToTopInSection:section];
        }
    }
    return shouldStickToTop;
}

- (NSArray *) layoutAttributesForElementsInRect:(CGRect)rect {
    
    NSArray *attributesArray = [super layoutAttributesForElementsInRect:rect];
    NSMutableArray *modifiedAttributesArray = [attributesArray mutableCopy];
    NSMutableIndexSet *attributesToRemoveIdxs = [NSMutableIndexSet indexSet];
    
    NSMutableIndexSet *missingSections = [NSMutableIndexSet indexSet];
    for (NSUInteger idx=0; idx<[attributesArray count]; idx++) {
        UICollectionViewLayoutAttributes *attributes = attributesArray[idx];
        if (!attributes) {
            continue;
        }
        if (attributes.representedElementCategory == UICollectionElementCategoryCell) {
            // remember that we need to layout header for this section
            [missingSections addIndex:attributes.indexPath.section];
        }
        if ([attributes.representedElementKind isEqualToString:UICollectionElementKindSectionHeader]) {
            // remember indexes of header layout attributes, so that we can remove them and add them later
            [attributesToRemoveIdxs addIndex:idx];
        }
    }
    
    // remove headers layout attributes
    [modifiedAttributesArray removeObjectsAtIndexes:attributesToRemoveIdxs];
    
    // layout all headers needed for the rect using self code
    [missingSections enumerateIndexesUsingBlock:^(NSUInteger idx, BOOL *stop) {
        NSIndexPath *indexPath = [NSIndexPath indexPathForItem:0 inSection:idx];
        UICollectionViewLayoutAttributes *layoutAttributes = [self layoutAttributesForSupplementaryViewOfKind:UICollectionElementKindSectionHeader atIndexPath:indexPath];
        if (layoutAttributes) {
            [modifiedAttributesArray addObject:layoutAttributes];
        }
    }];
    
    return [modifiedAttributesArray autorelease];
}

- (UICollectionViewLayoutAttributes *)layoutAttributesForSupplementaryViewOfKind:(NSString *)kind atIndexPath:(NSIndexPath *)indexPath {
    UICollectionViewLayoutAttributes *attributes = [super layoutAttributesForSupplementaryViewOfKind:kind atIndexPath:indexPath];
    if ([kind isEqualToString:UICollectionElementKindSectionHeader]&&
        [self shouldStickHeaderToTopInSection:attributes.indexPath.section]) {
        UICollectionView * const cv = self.collectionView;
        UIEdgeInsets const contentEdgeInsets = cv.contentInset;
        CGPoint const contentOffset = CGPointMake(cv.contentOffset.x, cv.contentOffset.y + contentEdgeInsets.top);
        //        CGPoint const contentOffset = cv.contentOffset;
        CGPoint nextHeaderOrigin = CGPointMake(INFINITY, INFINITY);
        
        if (indexPath.section+1 < [cv numberOfSections]) {
            UICollectionViewLayoutAttributes *nextHeaderAttributes = [super layoutAttributesForSupplementaryViewOfKind:kind atIndexPath:[NSIndexPath indexPathForItem:0 inSection:indexPath.section+1]];
            nextHeaderOrigin = nextHeaderAttributes.frame.origin;
        }
        
        CGRect frame = attributes.frame;
        if (self.scrollDirection == UICollectionViewScrollDirectionVertical) {
            frame.origin.y = MIN(MAX(contentOffset.y, frame.origin.y), nextHeaderOrigin.y - CGRectGetHeight(frame));
        }
        else { // UICollectionViewScrollDirectionHorizontal
            frame.origin.x = MIN(MAX(contentOffset.x, frame.origin.x), nextHeaderOrigin.x - CGRectGetWidth(frame));
        }
        attributes.zIndex = 1024;
        attributes.frame = frame;
    }
    return attributes;
}

- (UICollectionViewLayoutAttributes *)initialLayoutAttributesForAppearingSupplementaryElementOfKind:(NSString *)kind atIndexPath:(NSIndexPath *)indexPath {
    UICollectionViewLayoutAttributes *attributes = [self layoutAttributesForSupplementaryViewOfKind:kind atIndexPath:indexPath];
    return attributes;
}
- (UICollectionViewLayoutAttributes *)finalLayoutAttributesForDisappearingSupplementaryElementOfKind:(NSString *)kind atIndexPath:(NSIndexPath *)indexPath {
    UICollectionViewLayoutAttributes *attributes = [self layoutAttributesForSupplementaryViewOfKind:kind atIndexPath:indexPath];
    return attributes;
}

- (BOOL) shouldInvalidateLayoutForBoundsChange:(CGRect)newBound {
    return YES;
}
@end
