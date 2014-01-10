//
//  UIViewController+ADTransitionController.m
//  Transition
//
//  Created by Romain Goyet on 22/02/11.
//  Copyright 2011 Applidium. All rights reserved.
//

#import "UIViewController+ADTransitionController.h"
#import <objc/runtime.h>

extern NSString * ADTransitionControllerAssociationKey;

@implementation UIViewController (ADTransitionController)

- (ADTransitionController *)transitionController {
    return (ADTransitionController *)objc_getAssociatedObject(self, ADTransitionControllerAssociationKey);
}

//- (void)setTransitioningDelegate:(id <UIViewControllerTransitioningDelegate>)delegate {
//    NSAssert(FALSE, @"This setter shouldn't be used! You should set the transition property instead.");
//}
//
//- (void)setTransition:(ADTransition *)transition {
//    [super setTransitioningDelegate:[[[ADTransitioningDelegate alloc] initWithTransition:transition] autorelease]]; // don't call the setter of the current class
//}

@end
