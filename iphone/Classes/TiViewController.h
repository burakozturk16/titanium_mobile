/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import <UIKit/UIKit.h>
#import "TiControllerProtocols.h"
#import "ADTransitioningViewController.h"

@interface TiViewController : ADTransitioningViewController {

    TiViewProxy* _proxy;
    TiOrientationFlags _supportedOrientations;
}
-(id)initWithViewProxy:(TiViewProxy*)window;
-(void)detachProxy;
-(TiViewProxy*) proxy;

@end
