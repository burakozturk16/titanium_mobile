/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_FILESYSTEM

#import "FilesystemModule.h"
#import "TiFilesystemFileProxy.h"
#import "TiFilesystemBlobProxy.h"
#import "TiFilesystemFileStreamProxy.h"
#import "TiFileSystemHelper.h"

#if TARGET_IPHONE_SIMULATOR 
extern NSString * TI_APPLICATION_RESOURCE_DIR;
#endif

@implementation FilesystemModule

-(void)dealloc
{
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.Filesystem";
}

-(id)createTempFile:(id)args
{
	return [TiFilesystemFileProxy makeTemp:NO];
}

-(id)createTempDirectory:(id)args
{
	return [TiFilesystemFileProxy makeTemp:YES];
}

-(id)openStream:(id) args {
	NSNumber *fileMode = nil;
	
	ENSURE_ARG_AT_INDEX(fileMode, args, 0, NSNumber);
	ENSURE_VALUE_RANGE([fileMode intValue], TI_READ, TI_APPEND);

	if([args count] < 2) {
		[self throwException:TiExceptionNotEnoughArguments
				   subreason:nil
					location:CODELOCATION];
	}
	
	//allow variadic file components to be passed
	NSArray *pathComponents = [args subarrayWithRange:NSMakeRange(1, [args count] - 1 )];
	NSString *path = [TiFileSystemHelper pathFromComponents:pathComponents];
    
	NSArray *payload = [NSArray arrayWithObjects:path, fileMode, nil];

	return [[[TiFilesystemFileStreamProxy alloc] _initWithPageContext:[self executionContext] args:payload] autorelease];
}

-(id)MODE_APPEND
{
	return NUMINT(TI_APPEND);
}

-(id)MODE_READ
{
	return NUMINT(TI_READ);
}

-(id)MODE_WRITE
{
	return NUMINT(TI_WRITE);
}

-(id)isExternalStoragePresent:(id)unused
{
	//IOS treats the camera connection kit as just that, and does not allow
	//R/W access to it, which is just as well as it'd mess up cameras.
	return NUMBOOL(NO);
}

-(NSString*)resourcesDirectory
{
	return [TiFileSystemHelper resourcesDirectory];
}

-(NSString*)applicationDirectory
{
	return [TiFileSystemHelper applicationDirectory];
}

-(NSString*)applicationSupportDirectory
{
	return [TiFileSystemHelper applicationSupportDirectory];
}

-(NSString*)applicationDataDirectory
{
	return [TiFileSystemHelper applicationDataDirectory];
}

-(NSString*)applicationCacheDirectory
{
	return [TiFileSystemHelper applicationCacheDirectory];
}

-(NSString*)tempDirectory
{
	return [TiFileSystemHelper tempDirectory];
}

-(NSString*)separator
{
	return [TiFileSystemHelper separator];
}

-(NSString*)lineEnding
{
	return [TiFileSystemHelper lineEnding];
}

-(id)getFile:(id)args
{
	NSString* newpath = [TiFileSystemHelper pathFromComponents:args];
    
	if ([newpath hasSuffix:@".html"]||
		 [newpath hasSuffix:@".js"]||
		 [newpath hasSuffix:@".css"])
	{
		NSURL *url = [NSURL fileURLWithPath:newpath];
		NSData *data = [TiUtils loadAppResource:url];
		if (data!=nil)
		{
			return [[[TiFilesystemBlobProxy alloc] initWithURL:url data:data] autorelease];
		}
	}
	
	return [[[TiFilesystemFileProxy alloc] initWithFile:newpath] autorelease];
}

@end

#endif