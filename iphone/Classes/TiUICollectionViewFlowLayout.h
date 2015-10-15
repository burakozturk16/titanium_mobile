/**
 * Akylas
 * Copyright (c) 2009-2010 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import <UIKit/UIKit.h>

@protocol TiUICollectionViewFlowLayoutDelegate<UICollectionViewDelegateFlowLayout>
@optional
- (BOOL)shouldStickHeaderToTopInSection:(NSUInteger)section;
@end

@interface TiUICollectionViewFlowLayout : UICollectionViewFlowLayout
@end
