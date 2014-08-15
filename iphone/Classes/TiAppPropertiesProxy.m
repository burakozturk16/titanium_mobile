/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_APP

#import "TiAppPropertiesProxy.h"
#import "TiUtils.h"
#import "TiApp.h"
#import "TiProperties.h"

@implementation TiAppPropertiesProxy

-(void)dealloc
{
	TiThreadPerformOnMainThread(^{
		[[NSNotificationCenter defaultCenter] removeObserver:self];
	}, YES);
    
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.App.Properties";
}

-(void)_listenerAdded:(NSString*)type count:(int)count
{
	if (count == 1 && [type isEqual:@"change"])
	{
		TiThreadPerformOnMainThread(^{
			[[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(NSUserDefaultsDidChange) name:NSUserDefaultsDidChangeNotification object:nil];
		}, YES);
	}
}

-(void)_listenerRemoved:(NSString*)type count:(int)count
{
	if (count == 0 && [type isEqual:@"change"])
	{
		TiThreadPerformOnMainThread(^{
			[[NSNotificationCenter defaultCenter] removeObserver:self name:NSUserDefaultsDidChangeNotification object:nil];
		}, YES);
	}
}

#define GETPROP \
ENSURE_TYPE(args,NSArray);\
NSString *key = [args objectAtIndex:0];\
id appProp = [[TiApp tiAppProperties] objectForKey:key]; \
if(appProp) { \
return appProp; \
} \
id defaultValue = [args count] > 1 ? [args objectAtIndex:1] : [NSNull null];\

-(id)getBool:(id)args
{
	GETPROP
	return [TiProperties getBool:key defaultValue:defaultValue];
}

-(id)getDouble:(id)args
{
	GETPROP
	return [TiProperties getDouble:key defaultValue:defaultValue];
}

-(id)getInt:(id)args
{
	GETPROP
	return [TiProperties getInt:key defaultValue:defaultValue];
}

-(id)getString:(id)args
{
	GETPROP
	return [TiProperties getString:key defaultValue:defaultValue];
}

-(id)getList:(id)args
{
	GETPROP
	return [TiProperties getList:key defaultValue:defaultValue];
}

-(id)getObject:(id)args
{
    GETPROP
	return [TiProperties getObject:key defaultValue:defaultValue];
}

#define SETPROP \
ENSURE_TYPE(args,NSArray);\
NSString *key = [args objectAtIndex:0];\
id value = [args count] > 1 ? [args objectAtIndex:1] : nil;\

-(void)setBool:(id)args
{
	SETPROP
	[TiProperties setBool:value forKey:key];
}

-(void)setDouble:(id)args
{
	SETPROP
	[TiProperties setDouble:value forKey:key];
}

-(void)setInt:(id)args
{
	SETPROP
	[TiProperties setInt:value forKey:key];

}

-(void)setString:(id)args
{
	SETPROP
	[TiProperties setString:value forKey:key];
}

-(void)setList:(id)args
{
	SETPROP
	[TiProperties setList:value forKey:key];
}

-(void)setObject:(id)args
{
	SETPROP
	[TiProperties setObject:value forKey:key];
}

-(void)removeProperty:(id)args
{
	ENSURE_SINGLE_ARG(args,NSString);
	[TiProperties removeProperty:args];
}

-(void)removeAllProperties {
	[TiProperties removeAllProperties];
}

-(id)hasProperty:(id)args
{
    ENSURE_SINGLE_ARG(args,NSString);
	return NUMBOOL([TiProperties hasProperty:args]);
}

-(id)listProperties:(id)args
{
    return [TiProperties listProperties];
}

-(void) NSUserDefaultsDidChange
{
    NSDictionary* event = nil;
    if ([[TiProperties sharedInstance] changedProperty] != nil) {
        event = [NSDictionary dictionaryWithObject:[[TiProperties sharedInstance] changedProperty] forKey:@"property"];
        [TiProperties sharedInstance].changedProperty = nil;
    }
	[self fireEvent:@"change" withObject:event];
}


@end

#endif
