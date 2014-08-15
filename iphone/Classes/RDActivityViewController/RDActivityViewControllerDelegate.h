//
//  RDActivityViewControllerDelegate.h
//  APOD
//
//  Created by Robert Dougan on 11/5/12.
//  Copyright (c) 2012 Robert Dougan. All rights reserved.
//

#import <Foundation/Foundation.h>

@class RDActivityViewController;

@protocol RDActivityViewControllerDelegate <UIActivityItemSource>

- (NSArray *)activityViewController:(UIActivityViewController *)activityViewController itemsForActivityType:(NSString *)activityType;
@end
