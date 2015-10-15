/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#include <execinfo.h>
#import "TiExceptionHandler.h"
#import "TiBase.h"
#import "TiApp.h"

static void TiUncaughtExceptionHandler(NSException *exception);

static NSUncaughtExceptionHandler *prevUncaughtExceptionHandler = NULL;

@implementation TiExceptionHandler

@synthesize delegate = _delegate;

+ (TiExceptionHandler *)defaultExceptionHandler
{
    static TiExceptionHandler *defaultExceptionHandler;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        defaultExceptionHandler = [[self alloc] init];
        prevUncaughtExceptionHandler = NSGetUncaughtExceptionHandler();
        NSSetUncaughtExceptionHandler(&TiUncaughtExceptionHandler);
    });
    return defaultExceptionHandler;
}

- (void)reportException:(NSException *)exception withStackTrace:(NSArray *)stackTrace
{
    NSString *message = [NSString stringWithFormat:
                         @"[ERROR] The application has crashed with an uncaught exception '%@'.\nReason:\n%@\nStack trace:\n\n%@\n",
                         exception.name, exception.reason, [stackTrace componentsJoinedByString:@"\n"]];
    NSLog(@"%@",message);
    id <TiExceptionHandlerDelegate> currentDelegate = _delegate;
    if (currentDelegate == nil) {
        currentDelegate = self;
    }
    [currentDelegate handleUncaughtException:exception withStackTrace:stackTrace];
}

- (void)reportScriptError:(TiScriptError *)scriptError
{
    DebugLog(@"[ERROR] Script Error at %@", [scriptError oneLineDescription]);
    DebugLog(@"[ERROR] backtrace:\n%@", [scriptError stackDescription]);
    
    id <TiExceptionHandlerDelegate> currentDelegate = _delegate;
    if (currentDelegate == nil) {
        currentDelegate = self;
    }
    [currentDelegate handleScriptError:scriptError];
}

- (void)showScriptError:(TiScriptError *)error
{
#ifndef TI_DEPLOY_TYPE_PRODUCTION
    [[TiApp app] showModalError:error];
#endif
	[[NSNotificationCenter defaultCenter] postNotificationName:kTiErrorNotification
	                                                    object:self
	                                                  userInfo:error.dictionaryValue];
}

#pragma mark - TiExceptionHandlerDelegate

- (void)handleUncaughtException:(NSException *)exception withStackTrace:(NSArray *)stackTrace
{
    [self showScriptError:[TiUtils scriptErrorValue:exception]];

}

- (void)handleScriptError:(TiScriptError *)error
{
    [self showScriptError:error];
}

@end

@implementation TiScriptError

@synthesize message = _message;
@synthesize sourceURL = _sourceURL;
@synthesize lineNo = _lineNo;
@synthesize dictionaryValue = _dictionaryValue;
@synthesize backtrace = _backtrace;
@synthesize sourceLine;

- (id)initWithMessage:(NSString *)message sourceURL:(NSString *)sourceURL lineNo:(NSInteger)lineNo
{
    self = [super init];
    if (self) {
        _message = [message copy];
        _sourceURL = [sourceURL copy];
        _lineNo = lineNo;
    }
    return self;
}

- (id)initWithDictionary:(NSDictionary *)dictionary
{
    NSString *message = [[dictionary objectForKey:@"message"] description];
    if ([dictionary objectForKey:@"nativeReason"]) {
        if (message) {
            message = [message stringByAppendingFormat:@" : %@", [[dictionary objectForKey:@"nativeReason"] description]];
            
        } else {
            message = [[dictionary objectForKey:@"nativeReason"] description];
        }
    }
    NSString *sourceURL = [[dictionary objectForKey:@"sourceURL"] description];
    NSInteger lineNo = [[dictionary objectForKey:@"line"] integerValue];
    
    self = [self initWithMessage:message sourceURL:sourceURL lineNo:lineNo];
    if (self) {
        NSString* backtrace = [[dictionary objectForKey:@"backtrace"] description];
        if (backtrace == nil) {
            backtrace = [[dictionary objectForKey:@"stack"] description];
        }
        if ([dictionary objectForKey:@"nativeLocation"]) {
            if (backtrace) {
                backtrace = [NSString stringWithFormat:@"%@\n%@", [[dictionary objectForKey:@"nativeLocation"] description], backtrace];
                
            } else {
                backtrace = [[dictionary objectForKey:@"nativeLocation"] description];
            }
        }
        _backtrace = [backtrace retain];
        _dictionaryValue = [dictionary copy];
    }
    return self;
}

- (void)dealloc
{
    RELEASE_TO_NIL(sourceLine);
    RELEASE_TO_NIL(_message);
    RELEASE_TO_NIL(_sourceURL);
    RELEASE_TO_NIL(_backtrace);
    RELEASE_TO_NIL(_dictionaryValue);
    [super dealloc];
}

- (NSString *)description
{
    if (self.sourceURL != nil) {
        return [NSString stringWithFormat:@"%@ at %@ (line %ld)", self.message,[self.sourceURL lastPathComponent], (long)self.lineNo];
    } else {
        return [NSString stringWithFormat:@"%@", self.message];
    }
}

- (NSString *)detailedDescription
{
    return _dictionaryValue != nil ? [_dictionaryValue description] : [self description];
}


- (NSString *)stackDescription
{
    if (_backtrace) {
        return [_backtrace stringByReplacingOccurrencesOfString:@"%20" withString:@" "];
    }
    return @"";
}

- (NSString *)oneLineDescription
{
    if (self.sourceURL != nil) {
        return [NSString stringWithFormat:@"%@:%ld:\"%@\"", [self.sourceURL stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding], (long)self.lineNo, self.message];
    } else {
        return [NSString stringWithFormat:@"%@", self.message];
    }
}

- (NSString *)scriptLocation
{
        return [NSString stringWithFormat:@"%@:%ld:%@", [self.sourceURL lastPathComponent], (long)self.lineNo, [_dictionaryValue valueForKey:@"column"]];
}

@end

//
// thanks to: http://www.restoroot.com/Blog/2008/10/18/crash-reporter-for-iphone-applications/
//
static void TiUncaughtExceptionHandler(NSException *exception)
{
    static BOOL insideException = NO;
    
    // prevent recursive exceptions
    if (insideException==YES) {
        exit(1);
        return;
    }
    insideException = YES;
    
    NSArray *callStackArray = [exception callStackReturnAddresses];
    int frameCount = (int)[callStackArray count];
    void *backtraceFrames[frameCount];
    
    for (int i = 0; i < frameCount; ++i) {
        backtraceFrames[i] = (void *)[[callStackArray objectAtIndex:i] unsignedIntegerValue];
    }
    char **frameStrings = backtrace_symbols(&backtraceFrames[0], frameCount);
    
    NSMutableArray *stack = [[NSMutableArray alloc] initWithCapacity:frameCount];
    if (frameStrings != NULL) {
        for (int i = 0; (i < frameCount) && (frameStrings[i] != NULL); ++i) {
            [stack addObject:[NSString stringWithCString:frameStrings[i] encoding:NSASCIIStringEncoding]];
        }
        free(frameStrings);
    }
    
    [[TiExceptionHandler defaultExceptionHandler] reportException:exception withStackTrace:[[stack copy] autorelease]];
    [stack release];
    
    insideException=NO;
    if (prevUncaughtExceptionHandler != NULL) {
        prevUncaughtExceptionHandler(exception);
    }
}
