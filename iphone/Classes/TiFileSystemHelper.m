//
//  FileSystemHelper.m
//  Titanium
//
//  Created by Martin Guillon on 09/05/2014.
//
//

#import "TiFileSystemHelper.h"
#import "TiUtils.h"
#import "TiHost.h"
#ifdef USE_TI_FILESYSTEM
#import "TiFilesystemFileProxy.h"
#endif
@implementation TiFileSystemHelper


#define fileURLify(foo)	[[NSURL fileURLWithPath:foo isDirectory:YES] path]
static NSString* _resourcesDirectory = nil;
static NSString* _applicationDirectory = nil;
static NSString* _applicationSupportDirectory = nil;
static NSString* _applicationDataDirectory = nil;
static NSString* _applicationCacheDirectory = nil;
static NSString* _tempDirectory = nil;
static NSString* _separator = @"/";
static NSString* _lineEnding = @"\n";

+(id)resolveFile:(id)arg
{
#ifdef USE_TI_FILESYSTEM
	if ([arg isKindOfClass:[TiFilesystemFileProxy class]])
	{
		return [(TiFilesystemFileProxy*)arg path];
	}
#endif
	return [TiUtils stringValue:arg];
}

+(NSString*)pathFromComponents:(NSArray*)args
{
	NSString * newpath;
	id first = [args objectAtIndex:0];
	if ([first hasPrefix:@"file://"])
	{
		NSURL * fileUrl = [NSURL URLWithString:first];
		//Why not just crop? Because the url may have some things escaped that need to be unescaped.
		newpath =[fileUrl path];
	}
	else if ([first characterAtIndex:0]!='/')
	{
        NSURL* url = [NSURL URLWithString:[[TiFileSystemHelper resourcesDirectory] stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
        newpath = [[url path] stringByAppendingPathComponent:[TiFileSystemHelper resolveFile:first]];
	}
	else
	{
		newpath = [TiFileSystemHelper resolveFile:first];
	}
	
	if ([args count] > 1)
	{
		for (int c=1;c<[args count];c++)
		{
			newpath = [newpath stringByAppendingPathComponent:[TiFileSystemHelper resolveFile:[args objectAtIndex:c]]];
		}
	}
    
//    return [[newpath stringByStandardizingPath] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];;
    return [newpath stringByStandardizingPath];
}

+(NSString*)separator
{
	return _separator;
}

+(NSString*)lineEnding
{
	return _lineEnding;
}

+(NSString*)getPlatformPath:(NSString*)path {
    NSString* result = [NSString stringWithFormat:@"%@/",fileURLify(path)];
    if ([result hasPrefix:@"/private"]) {
        result = [result substringFromIndex:8];
    }
    return result;
}

+(NSString*)resourcesDirectory
{
    if (_resourcesDirectory == nil) {
        _resourcesDirectory = [[TiFileSystemHelper getPlatformPath:[TiHost resourcePath]] retain];
    }
	return _resourcesDirectory;
}

+(NSString*)applicationDirectory
{
    if (_applicationDirectory == nil)
        _applicationDirectory = [[TiFileSystemHelper getPlatformPath:[NSSearchPathForDirectoriesInDomains(NSApplicationDirectory, NSUserDomainMask, YES) objectAtIndex:0]] retain];
	return _applicationDirectory;
}

+(NSString*)applicationSupportDirectory
{
    if (_applicationSupportDirectory == nil)
        _applicationSupportDirectory = [[TiFileSystemHelper getPlatformPath:[NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, YES) objectAtIndex:0]] retain];
	return _applicationSupportDirectory;
}

+(NSString*)applicationDataDirectory
{
    if (_applicationDataDirectory == nil)
        _applicationDataDirectory = [[TiFileSystemHelper getPlatformPath:[NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES) objectAtIndex:0]] retain];
	return _applicationDataDirectory;
}

+(NSString*)applicationCacheDirectory
{
    if (_applicationCacheDirectory == nil)
        _applicationCacheDirectory = [[TiFileSystemHelper getPlatformPath:[NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, YES) objectAtIndex:0]] retain];
	return _applicationCacheDirectory;
}

+(NSString*)tempDirectory
{
    if (_tempDirectory == nil)
        _tempDirectory = [[TiFileSystemHelper getPlatformPath:NSTemporaryDirectory()] retain];
	return _tempDirectory;
}

+(NSString*)directoryForSuite:(NSString*)suitePath
{
    return [NSString stringWithFormat:@"%@/",fileURLify(suitePath)];

}

@end
