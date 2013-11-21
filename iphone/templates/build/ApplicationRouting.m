/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-<%- (new Date).getFullYear() %> by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 *
 * WARNING: This is generated code. Do not modify. Your changes *will* be lost.
 */

#import <Foundation/Foundation.h>
#import "ApplicationRouting.h"

extern NSData* filterDataInRange(NSData* thedata, NSRange range);

@implementation ApplicationRouting

<%- bytes %>

+ (NSData*) resolveAppAsset:(NSString*)path
{
	NSDictionary* map = [ApplicationRouting map];
	NSNumber *index = [map objectForKey:path];
	if (index == nil) { return nil; }
	return filterDataInRange([NSData dataWithBytesNoCopy:data length:sizeof(data) freeWhenDone:NO], ranges[index.integerValue]);
}

+ (NSArray*) getDirectoryListing:(NSString*)path
{
	NSDictionary* map = [ApplicationRouting map];
	if (map == nil) return nil;
	NSMutableArray* result = [[NSMutableArray alloc] init];
	NSString* pathToCompare = path;
	if (![pathToCompare hasSuffix:@"/"])
		pathToCompare = [pathToCompare stringByAppendingString:@"/"];
	id key;
	NSArray *keys = [map allKeys];
	int count = [keys count];
	for (int i = 0; i < count; i++)
	{
		key = [keys objectAtIndex: i];
		if ([key hasPrefix:path]) {
			NSString* value = [[key substringFromIndex:[pathToCompare length]] stringByReplacingOccurrencesOfString:@"_" withString:@"."];
			[result addObject: value];
		}
	}
	NSArray* array = [NSArray arrayWithArray:result];
	[result release];
	return array;
 }

@end