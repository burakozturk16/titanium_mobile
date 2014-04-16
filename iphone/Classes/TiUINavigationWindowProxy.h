/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */


#ifdef USE_TI_UINAVIGATIONWINDOW
#import "TiWindowProxy.h"
#import "ADTransitionController.h"

#define AD_SYSTEM_VERSION_GREATER_THAN_7 ([[[UIDevice currentDevice] systemVersion] compare:@"7" options:NSNumericSearch] == NSOrderedDescending)


@interface TiUINavigationWindowProxy : TiWindowProxy<UINavigationControllerDelegate, ADTransitionControllerDelegate,TiOrientationController,TiTab> {
@private
    id navController;
    TiWindowProxy *rootWindow;
    TiWindowProxy *current;
    BOOL transitionIsAnimating;
    BOOL transitionWithGesture;
}

//Private API
-(void)setFrame:(CGRect)bounds;
@end
#endif
