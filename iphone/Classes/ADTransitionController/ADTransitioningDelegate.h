//
//  ADTransitioningDelegate.h
//  ADTransitionController
//
//  Created by Patrick Nollet on 09/10/13.
//  Copyright (c) 2013 Applidium. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "ADTransition.h"
#import "ADPercentDrivenInteractiveTransition.h"

@interface ADTransitioningDelegate : ADPercentDrivenInteractiveTransition <ADTransitionDelegate>
@property (nonatomic, retain) ADTransition * transition;
- (id)initWithTransition:(ADTransition *)transition;
@end
