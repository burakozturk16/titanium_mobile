//
//  ADNavigationControllerDelegate.h
//  ADTransitionController
//
//  Created by Patrick Nollet on 09/10/13.
//  Copyright (c) 2013 Applidium. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface ADNavigationControllerDelegate : NSObject
@property (nonatomic, assign) BOOL isInteractive;
@property (nonatomic, assign) BOOL isInteracting;
@property (nonatomic, retain) id<UINavigationControllerDelegate> delegate;

- (void)manageNavigationController:(UINavigationController *)navigationController;

- (UIScreenEdgePanGestureRecognizer *)panGestureRecognizerForLeftEdgeOfViewController:(UIViewController *)viewController;
@end
