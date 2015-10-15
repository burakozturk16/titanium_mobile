/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_UIIMAGEVIEW

#import "TiUIImageViewProxy.h"
#import "TiUIImageView.h"
#import "OperationQueue.h"
#import "TiApp.h"
#import "TiFile.h"
#import "TiBlob.h"
#import "TiSVGImage.h"
#import "UIImage+UserInfo.h"
#import "NSDictionary+Merge.h"

#define DEBUG_IMAGEVIEW
#define DEFAULT_IMAGEVIEW_INTERVAL 200

@implementation TiUIImageViewProxy
@synthesize imageURL;


#pragma mark Internal

-(NSArray *)keySequence
{
    static NSArray *keySequence = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        keySequence = [[[super keySequence] arrayByAddingObjectsFromArray:@[@"width",@"height",@"scaleType",@"localLoadSync",  @"duration", @"repeatCount", @"reverse",@"image",@"animatedImages"]] retain];;
    });
    return keySequence;
}


-(NSString*)apiName
{
    return @"Ti.UI.ImageView";
}

-(void)propagateLoadEvent:(NSString *)stateString
{
#ifndef TI_USE_AUTOLAYOUT
    //Send out a content change message if we are auto sizing
    if (TiDimensionIsAuto(layoutProperties.width) || TiDimensionIsAutoSize(layoutProperties.width) || TiDimensionIsUndefined(layoutProperties.width) ||
        TiDimensionIsAuto(layoutProperties.height) || TiDimensionIsAutoSize(layoutProperties.height) || TiDimensionIsUndefined(layoutProperties.height)) {
        [self refreshSize];
        [self willChangeSize];
    }
    #endif
    if ([self _hasListeners:@"load" checkParent:NO]) {
        TiUIImageView *iv = (TiUIImageView*)[self view];
        UIImage* image = [iv getImage];
        TiBlob* blob = [[TiBlob alloc] initWithImage:image];
        
        NSDictionary *event = [NSDictionary dictionaryWithObjectsAndKeys:stateString,@"state", [blob autorelease], @"image", nil];
        if (image.info) {
            event = [event dictionaryByMergingWith:image.info];
        }
        [self fireEvent:@"load" withObject:event propagate:NO checkForListener:NO];
    }
//    else if(eventOverrideDelegate) {
//        [eventOverrideDelegate overrideEventObject:nil forEvent:@"load" fromViewProxy:self];
//    }
}

-(void)_configure
{
    [self replaceValue:NUMINT(UIViewContentModeScaleAspectFit) forKey:@"scaleType" notification:NO];
    [self replaceValue:NUMBOOL(NO) forKey:@"animating" notification:NO];
    [self replaceValue:NUMBOOL(NO) forKey:@"paused" notification:NO];
    [self replaceValue:NUMBOOL(NO) forKey:@"reverse" notification:NO];
    [self replaceValue:NUMBOOL(YES) forKey:@"autorotate" notification:NO];
    [self replaceValue:NUMFLOAT(DEFAULT_IMAGEVIEW_INTERVAL) forKey:@"duration" notification:NO];
}

-(void)start:(id)args
{
    if ([self viewAttached])
	{
		TiThreadPerformOnMainThread(^{
            TiUIImageView *iv = (TiUIImageView*)[self view];
            [iv start];
        }, NO);
	}
    else {
        [self replaceValue:NUMBOOL(YES) forKey:@"animating" notification:NO];
    }
}

-(void)stop:(id)args
{
    TiThreadPerformOnMainThread(^{
        TiUIImageView *iv= (TiUIImageView*)[self view];
        [iv stop];
    }, NO);
}

-(void)pause:(id)args
{
    TiThreadPerformOnMainThread(^{
        TiUIImageView *iv = (TiUIImageView*)[self view];
        [iv pause];
    }, NO);
}

-(void)resume:(id)args
{
    TiThreadPerformOnMainThread(^{
        TiUIImageView *iv = (TiUIImageView*)[self view];
        [iv resume];
    }, NO);
}

-(void)pauseOrResume:(id)args
{
    TiThreadPerformOnMainThread(^{
        TiUIImageView *iv = (TiUIImageView*)[self view];
        [iv playpause];
    }, NO);
}

-(void)viewWillDetach
{
    [self cancelPendingImageLoads];
	[super viewWillDetach];
}

-(void)windowWillClose
{
    [self cancelPendingImageLoads];
	[super windowWillClose];
}

-(void)_destroy
{
	[super _destroy];
}

- (void) dealloc
{
	RELEASE_TO_NIL(urlRequest);
//    [self replaceValue:nil forKey:@"image" notification:NO];
    
    RELEASE_TO_NIL(imageURL);
	[super dealloc];
}

-(id)toBlob:(id)args
{
	id imageValue = [self valueForKey:@"image"];

	if ([imageValue isKindOfClass:[TiBlob class]])
	{
		//We already have it right here already!
		return imageValue;
	}

	if ([imageValue isKindOfClass:[TiFile class]])
	{
		return [(TiFile *)imageValue toBlob:nil];
	}

	if (imageValue!=nil)
	{
        TiUIImageView* imageView = (TiUIImageView*)[self view];
        UIImage* imageToUse = [imageView getImage];
        if (!imageToUse) {
            NSURL *url_ = [TiUtils toURL:[TiUtils stringValue:imageValue] proxy:self];
            id theimage = [[ImageLoader sharedLoader] loadImmediateImage:url_];
            
            if (theimage == nil)
            {
                theimage = [[ImageLoader sharedLoader] loadRemote:url_ withOptions:[self valueForUndefinedKey:@"httpOptions"]];
            }
            
            // we're on the non-UI thread, we need to block to load
            UIImage *imageToUse = [imageView prepareImage:[imageView convertToUIImage:theimage]];
        }
		
		return [[[TiBlob alloc] initWithImage:imageToUse] autorelease];
	}
	return nil;
}

-(void)addLoadDelegate:(id <ImageLoaderDelegate>)delegate
{
	
}

-(CGSize) imageSize {
    CGFloat scale = [TiUtils screenScale];
    return CGSizeMake(TiDimensionCalculateValue(layoutProperties.width, 0.0) * scale,
                      TiDimensionCalculateValue(layoutProperties.height,0.0) * scale);
}

-(UIImage*)rotatedImage:(UIImage*)originalImage
{
    //If autorotate is set to false and the image orientation is not UIImageOrientationUp create new image
    if (![TiUtils boolValue:[self valueForUndefinedKey:@"autorotate"] def:YES] && (originalImage.imageOrientation != UIImageOrientationUp)) {
        UIImage* theImage = [UIImage imageWithCGImage:[originalImage CGImage] scale:[originalImage scale] orientation:UIImageOrientationUp];
        return theImage;
    }
    else {
        return originalImage;
    }
}

-(CGSize)contentSizeForSize:(CGSize)size andImageSize:(CGSize)imageSize
{
    CGSize result = size;
    if (CGSizeEqualToSize(size, CGSizeZero)) {
        result = CGSizeMake(imageSize.width, imageSize.height);
    }
    else if(size.width == 0 && imageSize.height>0) {
        result.width = (size.height*imageSize.width/imageSize.height);
    }
    else if(size.height == 0 && imageSize.width > 0) {
        result.height = (size.width*imageSize.height/imageSize.width);
    }
    else if(imageSize.height == 0 || imageSize.width == 0) {
    }
    else {
        BOOL autoSizeWidth = [(TiViewProxy*)self widthIsAutoSize];
        BOOL autoSizeHeight = [(TiViewProxy*)self heightIsAutoSize];
        if (!autoSizeWidth && ! autoSizeHeight) {
            result = size;
        }
        else if(autoSizeWidth && autoSizeHeight) {
            float ratio = imageSize.width/imageSize.height;
            float viewratio = size.width/size.height;
            if(viewratio > ratio) {
                result.height = MIN(size.height, imageSize.height);
                result.width = (result.height*imageSize.width/imageSize.height);
            }
            else {
                result.width = MIN(size.width, imageSize.width);
                result.height = (result.width*imageSize.height/imageSize.width);
            }        }
        else if(autoSizeHeight) {
            result.width = size.width;
            result.height = result.width*imageSize.height/imageSize.width;
        }
        else {
            result.height = size.height;
            result.width = (result.height*imageSize.width/imageSize.height);
        }
    }
    result.width = ceilf(result.width);
    result.height = ceilf(result.height);
    return result;
}

-(CGSize)contentSizeForSize:(CGSize)size
{
    if (view != nil)
        return [(TiUIView*)view contentSizeForSize:size];
    else
    {
        id value = [self valueForKey:@"image"];
        if (value == nil && ![TiUtils boolValue:@"preventDefaultImage" def:NO]) {
            value = [self valueForKey:@"defaultImage"];
        }
        if (value != nil)
        {
            NSURL* url = [self sanitizeURL:value];
            UIImage *image = [[ImageLoader sharedLoader] loadImmediateImage:url withSize:[self imageSize]];
            if (image) {
                CGSize result = CGSizeZero;
                UIImage* imageToUse = nil;
                if ([image isKindOfClass:[UIImage class]]) {
                    imageToUse = [self rotatedImage:image];
                }
                else if([image isKindOfClass:[TiSVGImage class]]) {
                    result.height = image.size.height;
                    result.width = image.size.width;
                    result =  [(TiSVGImage*)image imageForSize:[self imageSize]].size;
                }
                else {
                    return result;
                }
                float factor = 1.0f;
                float screenScale = [UIScreen mainScreen].scale;
                if ([TiUtils boolValue:[self  valueForKey:@"hires"] def:[TiUtils isRetinaDisplay]])
                {
                    factor /= screenScale;
                }
                result.width = imageToUse.size.width * factor;
                result.height = imageToUse.size.height * factor;
                return [self contentSizeForSize:size andImageSize:result];
            }
            
        }
    }
    return CGSizeZero;
}

#pragma mark Handling ImageLoader

-(void)setImage:(id)newImage
{
    id image = newImage;
    if ([image isEqual:@""])
    {
        image = nil;
    }
	[self cancelPendingImageLoads];
    [self replaceValue:image forKey:@"image" notification:YES];
}

-(void)startImageLoad:(NSURL *)url;
{
	[self cancelPendingImageLoads]; //Just in case we have a crusty old urlRequest.
	NSDictionary* info = nil;
	NSNumber* hires = [self valueForKey:@"hires"];
	if (hires) {
		info = [NSDictionary dictionaryWithObject:hires forKey:@"hires"];
	}
	urlRequest = [[[ImageLoader sharedLoader] loadImage:url delegate:self options:[self valueForUndefinedKey:@"httpOptions"] userInfo:info] retain];
}

-(void)cancelPendingImageLoads
{
	// cancel a pending request if we have one pending
	if (urlRequest!=nil)
	{
		[urlRequest cancel];
		RELEASE_TO_NIL(urlRequest);
	}
}

-(void)imageLoadSuccess:(ImageLoaderRequest*)request image:(id)image
{
	if (request != urlRequest)
	{
		return;
	}
	
	if (view != nil)
	{
		[(TiUIImageView *)[self view] imageLoadSuccess:request image:image];
	}
    [self setImageURL:[urlRequest url]];
	RELEASE_TO_NIL(urlRequest);
}

-(void)imageLoadFailed:(ImageLoaderRequest*)request error:(NSError*)error
{
	if (request == urlRequest)
	{
        if ([self _hasListeners:@"error" checkParent:NO])
		{
            [self fireEvent:@"error" withObject:[NSDictionary dictionaryWithObject:[request url] forKey:@"image"] propagate:NO reportSuccess:YES errorCode:[error code] message:[TiUtils messageFromError:error] checkForListener:NO];
		}
		RELEASE_TO_NIL(urlRequest);
	}
}

-(void)imageLoadCancelled:(ImageLoaderRequest *)request
{
}

-(TiDimension)defaultAutoWidthBehavior:(id)unused
{
    return TiDimensionAutoSize;
}
-(TiDimension)defaultAutoHeightBehavior:(id)unused
{
    return TiDimensionAutoSize;
}

- (void)prepareForReuse
{
    [(TiUIImageView *)view setReusing:YES];
    [super prepareForReuse];
}


- (void)configurationSet:(BOOL)recursive
{
    [super configurationSet:recursive];
    [(TiUIImageView *)view setReusing:NO];
}

@end

#endif
