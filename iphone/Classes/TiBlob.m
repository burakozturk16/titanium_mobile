/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiBlob.h"
#import "Mimetypes.h"
#import "TiUtils.h"
#import "UIImage+Alpha.h"
#import "UIImage+Resize.h"
#import "UIImage+RoundedCorner.h"
#import "ImageLoader.h"
//NOTE:FilesystemFile is conditionally compiled based on the filesystem module.
#import "TiRect.h"
#import "TiFilesystemFileProxy.h"
#import "NSDictionary+Merge.h"
#import "UIImage+UserInfo.h"
#import "TiImageHelper.h"

static NSString *const MIMETYPE_PNG = @"image/png";
static NSString *const MIMETYPE_JPEG = @"image/jpeg";

@implementation TiBlob
{
    NSDictionary* extraInfo;
}

-(void)dealloc
{
	RELEASE_TO_NIL(mimetype);
	RELEASE_TO_NIL(data);
	RELEASE_TO_NIL(image);
    RELEASE_TO_NIL(path);
    RELEASE_TO_NIL(extraInfo);
	[super dealloc];
}

-(id)description
{
	NSString* text = [self text];
	if (text == nil || [text isEqualToString:@""]) {
		return @"[object TiBlob]";
	}
	return text;
}

-(NSString*)apiName
{
    return @"Ti.Blob";
}

-(BOOL)isImageMimeType
{
	return [mimetype hasPrefix:@"image/"];
}

-(void)ensureImageLoaded
{
	if (image == nil && [self isImageMimeType])
	{
		image = [[self image] retain];
	}
}

-(NSInteger)width
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		return image.size.width * image.scale;
	}
	return 0;
}

-(NSInteger)height
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		return image.size.height * image.scale;
	}
	return 0;
}

-(NSInteger)size
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		return image.size.width * image.scale * image.size.height * image.scale;
	}
	switch (type)
	{
		case TiBlobTypeData:
		{
			return [data length];
		}
		case TiBlobTypeFile:
		{
			NSFileManager *fm = [NSFileManager defaultManager];
			NSError *error = nil; 
			NSDictionary * resultDict = [fm attributesOfItemAtPath:path error:&error];
			id result = [resultDict objectForKey:NSFileSize];
			if (error!=NULL)
			{
				return 0;
			}
			return [result intValue];
		}
		default: {
			break;
		}
	}
	return 0;
}

-(id)initWithImage:(UIImage*)image_
{
	if (self = [super init])
	{
		image = [image_ retain];
        [self setInfo:image.info];
		type = TiBlobTypeImage;
		mimetype = [([UIImageAlpha hasAlpha:image_] ? MIMETYPE_PNG : MIMETYPE_JPEG) copy];
	}
	return self;
}

-(id)initWithData:(NSData*)data_ mimetype:(NSString*)mimetype_
{
	if (self = [super init])
	{
		data = [data_ retain];
		type = TiBlobTypeData;
		mimetype = [mimetype_ copy];
	}
	return self;
}

-(id)initWithFile:(NSString*)path_
{
	if (self = [super init])
	{
		type = TiBlobTypeFile;
		path = [path_ retain];
		mimetype = [[Mimetypes mimeTypeForExtension:path] copy];
	}
	return self;
}

-(TiBlobType)type
{
	return type;
}

-(NSString*)mimeType
{
	return mimetype;
}

-(NSString*)text
{
	switch (type)
	{
		case TiBlobTypeFile:
		{
			NSData *fdata = [self data];
			return [[[NSString alloc] initWithData:fdata encoding:NSUTF8StringEncoding] autorelease];
		}
		case TiBlobTypeData:
		{
			return [[[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] autorelease];
		}
		default: {
			break;
		}
	}
	// anything else we refuse to write out
	return nil;
}

- (NSString *)hexString
{
    NSData* theData = [self data];
    /* Returns hexadecimal string of NSData. Empty string if data is empty.   */
    
    const unsigned char *dataBuffer = (const unsigned char *)[theData bytes];
    
    if (!dataBuffer)
    {
        return [NSString string];
    }
    
    NSUInteger          dataLength  = [theData length];
    NSMutableString     *hexString  = [NSMutableString stringWithCapacity:(dataLength * 2)];
    
    for (int i = 0; i < dataLength; ++i)
    {
        [hexString appendFormat:@"%02x", (unsigned int)dataBuffer[i]];
    }
    
    return [NSString stringWithString:hexString];
}

-(NSArray*)byteArray
{    
    const unsigned char *dataBuffer = (const unsigned char *)[data bytes];
    
    if (!dataBuffer)
    {
        return [NSMutableArray array];
    }
    
    NSUInteger          dataLength  = [data length];
    NSMutableArray     *hexArray  = [NSMutableArray arrayWithCapacity:dataLength];
    
    for (int i = 0; i < dataLength; ++i)
    {
        [hexArray addObject:NUMINTEGER((unsigned int)dataBuffer[i])];
    }
    
    return [NSArray arrayWithArray:hexArray];
}

-(NSData*)data
{
	switch(type)
	{
		case TiBlobTypeFile:
		{
			NSError *error = nil;
			return [NSData dataWithContentsOfFile:path options:0 error:&error];
		}
		case TiBlobTypeImage:
		{
            if ([mimetype isEqualToString:MIMETYPE_PNG]) {
                return UIImagePNGRepresentation(image);
            }
            return UIImageJPEGRepresentation(image, image.compressionLevel);
		}
		default: {
			break;
		}
	}
	return data;
}

-(UIImage*)image
{
	switch(type)
	{
		case TiBlobTypeFile:
		{
            if (image == nil) {
//                NSURL * result = [TiUtils toURL:path proxy:self];
                image = [[UIImage alloc] initWithContentsOfFile:path];
            }
            break;
		}
		case TiBlobTypeData:
		{
            if (image == nil) {
                image = [[UIImage imageWithData:data] retain];
            }
            break;
		}
		default: {
			break;
		}
	}
	return image;
}

-(id)representedObject
{
    if ([mimetype isEqualToString:MIMETYPE_PNG] ||
        [mimetype isEqualToString:MIMETYPE_JPEG]) {
        return [self image];
    }
    
    else {
        return [self text];
    }
}

-(void)setData:(NSData*)data_
{
	RELEASE_TO_NIL(data);
	RELEASE_TO_NIL(image);
	type = TiBlobTypeData;
	data = [data_ retain];
}

-(void)setImage:(UIImage *)image_
{
	RELEASE_TO_NIL(image);
	image = [image_ retain];
    [self setMimeType:([UIImageAlpha hasAlpha:image_] ? MIMETYPE_PNG : MIMETYPE_JPEG) type:TiBlobTypeImage];
}

-(NSString*)path
{
	return path;
}

// For Android compatibility
-(TiFile *)file
{	/**
	 *	Having such a conditional compile deep in TiBlob may have implications
	 *	later on if we restructure platform. This may also mean we should
	 *	require filesystem to always be compiled in, but that seems overkill
	 *	for now. There may be some issues with parity with Android's
	 *	implementation in behavior when filesystem module is missing or when the
	 *	path does not point to a valid file.
	 *	TODO: What is expected behavior when path is valid but there's no file?
	 *	TODO: Should file property require explicit use of filesystem module?
	 */
#ifdef USE_TI_FILESYSTEM
	if (path != nil) {
		return [[[TiFilesystemFileProxy alloc] initWithFile:path] autorelease];	
	}
#else
	NSLog(@"[FATAL] Blob.file property requested but the Filesystem module was never requested.")
#endif
	return nil;
}

-(id)nativePath
{
    if (path != nil) {
	    return [[NSURL fileURLWithPath:path] absoluteString];
    }
    return [NSNull null];
}

-(NSNumber*)length
{
    return NUMLONGLONG([[self data] length]);
}

-(void)setMimeType:(NSString*)mime type:(TiBlobType)type_
{
	RELEASE_TO_NIL(mimetype);
	mimetype = [mime copy];
	type = type_;
}

-(BOOL)writeTo:(NSString*)destination error:(NSError**)error
{
	NSData *writeData = nil;
	switch(type)
	{
		case TiBlobTypeFile:
		{
			NSFileManager *fm = [NSFileManager defaultManager];
			return [fm copyItemAtPath:path toPath:destination error:error];
		}
		case TiBlobTypeImage:
		{
			writeData = [self data];
            CGFloat scale =[[self image] scale];
            if (scale > 1 && [destination rangeOfString:@"@"].location == NSNotFound) {
                NSString* mainPath = [destination stringByDeletingPathExtension];
                NSString* ext = [destination pathExtension];
                destination = [NSString stringWithFormat:@"%@@%.0fx.%@", mainPath, scale, ext];
            }
			break;
		}
		case TiBlobTypeData:
		{
			writeData = data;
            
			break;
		}
	}
	if (writeData!=nil)
	{
		return [writeData writeToFile:destination atomically:YES];
	}
	return NO;
}

#pragma mark Image Manipulations

- (id)imageWithAlpha:(id)args
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		TiBlob *blob = [[TiBlob alloc] initWithImage:[UIImageAlpha imageWithAlpha:image]];
		return [blob autorelease];
	}
	return nil;
}

- (id)imageWithTransparentBorder:(id)args
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		ENSURE_SINGLE_ARG(args,NSObject);
		NSUInteger size = [TiUtils intValue:args];
		TiBlob *blob = [[TiBlob alloc] initWithImage:[UIImageAlpha transparentBorderImage:size image:image]];
		return [blob autorelease];
	}
	return nil;
}

- (id)imageWithRoundedCorner:(id)args
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		NSUInteger cornerSize = [TiUtils intValue:[args objectAtIndex:0]];
		NSUInteger borderSize = [args count] > 1 ? [TiUtils intValue:[args objectAtIndex:1]] : 1;
		TiBlob *blob =  [[TiBlob alloc] initWithImage:[UIImageRoundedCorner roundedCornerImage:cornerSize borderSize:borderSize image:image]];
		return [blob autorelease];
	}
	return nil;
}

- (id)imageAsThumbnail:(id)args
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
		NSUInteger size = [TiUtils intValue:[args objectAtIndex:0]];
		NSUInteger borderSize = [args count] > 1 ? [TiUtils intValue:[args objectAtIndex:1]] : 1;
		NSUInteger cornerRadius = [args count] > 2 ? [TiUtils intValue:[args objectAtIndex:2]] : 0;
		TiBlob *blob = [[TiBlob alloc] initWithImage:[UIImageResize thumbnailImage:size
												  transparentBorder:borderSize
													   cornerRadius:cornerRadius
											   interpolationQuality:kCGInterpolationHigh
															  image:image]];
		return [blob autorelease];		
	}
	return nil;
}

- (id)imageAsFiltered:(id)args
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
        ENSURE_SINGLE_ARG_OR_NIL(args, NSDictionary)
		return [[[TiBlob alloc] initWithImage:[TiImageHelper imageFiltered:image withOptions:args]] autorelease];
	}
	return nil;
}

- (id)imageAsResized:(id)args
{
    [self ensureImageLoaded];
    if (image!=nil)
    {
        ENSURE_ARG_COUNT(args,2);
        NSUInteger width = [TiUtils intValue:[args objectAtIndex:0]];
        NSUInteger height = [TiUtils intValue:[args objectAtIndex:1]];
        TiBlob *blob =  [[TiBlob alloc] initWithImage:[UIImageResize resizedImage:CGSizeMake(width, height) interpolationQuality:kCGInterpolationHigh image:image hires:NO]];
        return [blob autorelease];
    }
    return nil;
}

-(UIImage*)shrinkImage:(UIImage*)theImage withMaxByteSize:(NSUInteger)byteSize {
    
    double compressionRatio = 1;
    int resizeAttempts = 10;
    NSData * lastImgData = nil;
    NSData * imgData = UIImageJPEGRepresentation(theImage,compressionRatio);
    
    NSUInteger currentSize = [imgData length];
    while (currentSize > byteSize && resizeAttempts > 0) {
        resizeAttempts -= 1;
        lastImgData = imgData;
        compressionRatio = 0.7;
        imgData = UIImageJPEGRepresentation([UIImage imageWithData:lastImgData],compressionRatio);
        currentSize = [imgData length];
        if (currentSize <= byteSize) {
//            imgData = [UIImage imageWithData:imgData];
            break;
        } else {
            compressionRatio = 1.0;
        }
        // too much of a compression let's resize the image
        UIImage* newImage = [UIImage imageWithData:lastImgData];
        
        newImage = [UIImageResize resizedImage:CGSizeMake(newImage.size.width*0.5, newImage.size.height*0.5) interpolationQuality:kCGInterpolationDefault image:newImage hires:(newImage.scale >= 2)];
        imgData = UIImageJPEGRepresentation(newImage, 1);
        currentSize = [imgData length];
        //Test size after compression
    }
    UIImage* result = [UIImage imageWithData:imgData];
    result.compressionLevel = compressionRatio;
    return result;
}

- (id)imageAsCompressed:(id)args
{
	[self ensureImageLoaded];
	if (image!=nil)
	{
        ENSURE_SINGLE_ARG(args, NSNumber);
        NSUInteger maxSize = [args intValue];
        TiBlob *blob = [[TiBlob alloc] initWithImage:[self shrinkImage:image withMaxByteSize:maxSize]];
		return [blob autorelease];
	}
	return nil;
}

-(id)toImage:(id)args
{
    [self ensureImageLoaded];
    return self;
}

-(id)toString:(id)args
{
	id represented = [self representedObject];
    if ([represented isKindOfClass:[UIImage class]]) {
        return [TiUtils jsonStringify:@{@"type": @"image",
                                        @"width":@(((UIImage*)represented).size.width),
                                        @"height":@(((UIImage*)represented).size.height)
                                        }];
    } else if ([represented isKindOfClass:[NSString class]]) {
        return represented;
    }
	return [super toString:args];
}

-(void)setInfo:(NSDictionary*)info {
    RELEASE_TO_NIL(extraInfo)
    extraInfo = [info retain];
}

-(void)addInfo:(NSDictionary*)info {
    if (extraInfo) {
        NSDictionary* oldInfo = extraInfo;
        extraInfo = [oldInfo dictionaryByMergingWith:info];
        RELEASE_TO_NIL(oldInfo)
    }
    else {
        [self setInfo:info];
    }
}

-(NSDictionary*)info {
    return extraInfo;
}

@end
