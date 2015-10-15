/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import <QuartzCore/QuartzCore.h>
#import "TiErrorController.h"
#import "TiApp.h"
#import "TiBase.h"
#import "TiUtils.h"
#import "TiExceptionHandler.h"
#import "TiViewProxy.h"
#import "TiFileSystemHelper.h"
#ifdef USE_TI_UIBUTTON
#import "TiUIButton.h"
#import "TiUIButtonProxy.h"
#endif

@implementation TiErrorController
{
    TiScriptError *error;
    TiViewProxy *viewProxy;
}

-(id)initWithError:(TiScriptError*)error_ template:(NSDictionary*)template inContext:(KrollBridge*)bridge
{
	if (self = [super init])
	{
        viewProxy = (TiViewProxy*)[[[TiViewProxy class] createFromDictionary:template rootProxy:nil inContext:bridge] retain];
        if (error_) {
            error = [error_ retain];
            NSString* path = [[NSURL fileURLWithPath:[TiFileSystemHelper resourcesDirectory]] absoluteString];
            if ([path hasSuffix:@"/"]) {
                path = [path substringToIndex:path.length - 1];
            }
            NSMutableDictionary* dict = [NSMutableDictionary dictionaryWithObjectsAndKeys:
                                         @{
                                           @"text":[error message]
                                           }, @"message", nil];
            if (error.sourceURL) {
                [dict setObject:@{
                                  @"text":[error scriptLocation]
                                  } forKey:@"location"];
            }
            if (error.backtrace) {
                [dict setObject:@{
                                  @"text":[[error backtrace] stringByReplacingOccurrencesOfString:path withString:@""]
                                  } forKey:@"callstack"];
            }
            if (error.sourceLine) {
                [dict setObject:@{
                                 @"text":error.sourceLine
                                 } forKey:@"source"];
            }
            [viewProxy applyProperties:@[dict, @(YES)]];
        }
        
	}
	return self;
}

-(void)dealloc
{
    RELEASE_TO_NIL(viewProxy);
    RELEASE_TO_NIL(error);
	[super dealloc];
}

-(void)dismiss:(id)sender
{
	[[TiApp app] hideModalController:self animated:YES];
}

#ifndef TI_DEPLOY_TYPE_PRODUCTION
-(void)kill:(id)sender
{
    exit(0);
}
#endif

- (void)loadView
{
    [super loadView];
    self.modalTransitionStyle = UIModalTransitionStyleCoverVertical;
    UIView *view = [self view];
    [view addSubview:[viewProxy getAndPrepareViewForOpening:view.bounds]];
#ifdef USE_TI_UIBUTTON
    TiProxy* dismissButton = [viewProxy bindingForKey:@"dismiss"];
    if (IS_OF_CLASS(dismissButton, TiUIButtonProxy)) {
        [[(TiUIButtonProxy*)dismissButton button] addTarget:self action:@selector(dismiss:) forControlEvents:UIControlEventTouchUpInside];
    }
#ifndef TI_DEPLOY_TYPE_PRODUCTION
    TiProxy* killButton = [viewProxy bindingForKey:@"kill"];
    if (IS_OF_CLASS(killButton, TiUIButtonProxy)) {
        [[(TiUIButtonProxy*)killButton button] addTarget:self action:@selector(kill:) forControlEvents:UIControlEventTouchUpInside];
    }
#endif
#endif
}

-(void)viewDidAppear:(BOOL)animated
{
    [super viewDidAppear:animated];
    [self.view layoutIfNeeded];
}

@end
