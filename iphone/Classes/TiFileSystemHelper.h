//
//  FileSystemHelper.h
//  Titanium
//
//  Created by Martin Guillon on 09/05/2014.
//
//

#import <Foundation/Foundation.h>

@interface TiFileSystemHelper : NSObject

+(id)resolveFile:(id)arg;
+(NSString*)pathFromComponents:(NSArray*)args;
+(NSString*)separator;
+(NSString*)lineEnding;
+(NSString*)resourcesDirectory;
+(NSString*)applicationDirectory;
+(NSString*)applicationSupportDirectory;
+(NSString*)applicationDataDirectory;
+(NSString*)applicationCacheDirectory;
+(NSString*)tempDirectory;
@end
