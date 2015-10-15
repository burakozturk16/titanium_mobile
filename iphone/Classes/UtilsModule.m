/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UTILS

#import "UtilsModule.h"
#import "TiUtils.h"
#import "TiBlob.h"
#import "TiFile.h"
#import <CommonCrypto/CommonDigest.h>
#import <CommonCrypto/CommonHMAC.h>
#import "DDMathEvaluator.h"

@implementation UtilsModule

-(NSString*)convertToString:(id)arg
{
	if ([arg isKindOfClass:[NSString class]])
	{
		return arg;
	}
	else if ([arg isKindOfClass:[TiBlob class]])
	{
		return [(TiBlob*)arg text];
	}
	THROW_INVALID_ARG(@"invalid type");
}

-(NSData*)convertToData:(id)arg
{
	if ([arg isKindOfClass:[NSString class]])
	{
		return [arg dataUsingEncoding:NSUTF8StringEncoding];
	}
	else if ([arg isKindOfClass:[TiBlob class]])
	{
		return [(TiBlob*)arg data];
	}
    else if ([arg isKindOfClass:[NSData class]])
	{
		return arg;
	}
	THROW_INVALID_ARG(@"invalid type");
}

-(NSString*)apiName
{
    return @"Ti.Utils";
}


#pragma mark Public API

-(TiBlob*)base64encode:(id)args
{
	ENSURE_SINGLE_ARG(args,NSObject);
	NSData *data = [self convertToData:args];
    NSString* result = [TiUtils base64encode:data];
    return [[[TiBlob alloc] initWithData:[result dataUsingEncoding:NSUTF8StringEncoding] mimetype:@"application/octet-stream"] autorelease];
}

-(TiBlob*)base64decode:(id)args
{
	ENSURE_SINGLE_ARG(args,NSObject);
	NSString* encoded = [self convertToString:args];
    NSData* result = [TiUtils base64decode:encoded];
    return [[[TiBlob alloc] initWithData:result mimetype:@"application/octet-stream"] autorelease];
}

-(NSString*)md5HexDigest:(id)args
{
	ENSURE_SINGLE_ARG(args,NSObject);
	
	NSData* data = nil;
	NSString *nstr = [self convertToString:args];
	if (nstr) {
		const char* s = [nstr UTF8String];
		data = [NSData dataWithBytes:s length:strlen(s)];
	} else if ([args respondsToSelector:@selector(data)]) {
		data = [args data];
	}
	return [TiUtils md5:data];
}

-(id)sha1:(id)args
{
	ENSURE_SINGLE_ARG(args,NSObject);
	NSString *nstr = [self convertToString:args];
	const char *cStr = [nstr UTF8String];
	unsigned char result[CC_SHA1_DIGEST_LENGTH];
	CC_SHA1(cStr, (CC_LONG)[nstr lengthOfBytesUsingEncoding:NSUTF8StringEncoding], result);
	return [TiUtils convertToHex:(unsigned char*)&result length:CC_SHA1_DIGEST_LENGTH];
}

-(id)sha256:(id)args
{
	ENSURE_SINGLE_ARG(args,NSObject);
	NSString *nstr = [self convertToString:args];
	const char *cStr = [nstr UTF8String];
	unsigned char result[CC_SHA256_DIGEST_LENGTH];
	CC_SHA256(cStr, (CC_LONG)[nstr lengthOfBytesUsingEncoding:NSUTF8StringEncoding], result);
	return [TiUtils convertToHex:(unsigned char*)&result length:CC_SHA256_DIGEST_LENGTH];
}

-(id)blob:(id)args
{
   	ENSURE_SINGLE_ARG(args,NSString);
    TiBlob* result = [[TiBlob alloc] initWithFile:args];
    result.executionContext = [self executionContext];
    return [result autorelease];
}

- (NSString *)replacingStringsIn:(NSString*)string fromDictionary:(NSDictionary *)dict
{
    NSMutableString *result = [string mutableCopy];
    for (NSString *target in dict) {
        [result replaceOccurrencesOfString:target withString:[dict objectForKey:target]
                                   options:0 range:NSMakeRange(0, [string length])];
    }
    return [result autorelease];
}

@end

#endif
