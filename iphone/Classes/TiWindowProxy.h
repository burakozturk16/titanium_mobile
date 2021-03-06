/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiViewProxy.h"
#import "TiTab.h"
#import "TiControllerProtocols.h"

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
#import "TiUIiOSTransitionAnimationProxy.h"
#endif

@interface TiWindowProxy : TiViewProxy<TiWindowProtocol, HLSAnimationDelegate> {
@protected
    id<TiOrientationController> parentController;
    TiOrientationFlags _supportedOrientations;
    BOOL opening;
    BOOL opened;
    BOOL closing;
    BOOL focussed;
    BOOL isModal;
    TiViewProxy<TiTab> *tab;
    TiAnimation * openAnimation;
    TiAnimation * closeAnimation;
    UIView* animatedOver;
#ifdef USE_TI_UIIOSTRANSITIONANIMATION
    TiUIiOSTransitionAnimationProxy* transitionProxy;
#endif
    
}

@property (nonatomic, readwrite, assign) TiViewProxy<TiTab> *tab;
@property (nonatomic, readonly) TiProxy* tabGroup;
@property (nonatomic, readonly) BOOL focussed;
@property (nonatomic, readwrite) NSInteger internalStatusBarStyle;
@property (nonatomic, readwrite) BOOL hidesStatusBar;
-(void)updateOrientationModes;

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
-(TiUIiOSTransitionAnimationProxy*) transitionAnimation;
#endif
@end
