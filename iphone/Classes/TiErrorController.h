/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

@class TiScriptError;
@class KrollBridge;
@interface TiErrorController : UIViewController

-(id)initWithError:(TiScriptError*)error_ template:(NSDictionary*)template inContext:(KrollBridge*)bridge;

@end
