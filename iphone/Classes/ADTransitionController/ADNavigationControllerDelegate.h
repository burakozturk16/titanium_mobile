//
//  ADNavigationControllerDelegate.h
//  ADTransitionController
//
//  Created by Patrick Nollet on 09/10/13.
//  Copyright (c) 2013 Applidium. All rights reserved.
//

#import <Foundation/Foundation.h>

@class ADTransitioningDelegate;
@interface ADPercentDrivenInteractiveTransition : UIPercentDrivenInteractiveTransition
@property (nonatomic, strong) ADTransitioningDelegate *transitionDelegate;

@end

@interface ADNavigationControllerDelegate : NSObject <UINavigationControllerDelegate>
@property (nonatomic, strong) ADPercentDrivenInteractiveTransition *interactivePopTransition;
@end
