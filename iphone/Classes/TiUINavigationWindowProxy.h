/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */


#ifdef USE_TI_UINAVIGATIONWINDOW
#import "TiWindowProxy.h"
#import "ADTransitionController.h"


@interface TiUINavigationWindowProxy : TiWindowProxy<ADTransitionControllerDelegate,TiOrientationController,TiTab> {
@private
    ADTransitionController *navController;
    TiWindowProxy *rootWindow;
    TiWindowProxy *current;
    BOOL transitionIsAnimating;
}

//Private API
-(void)setFrame:(CGRect)bounds;
@end
#endif
