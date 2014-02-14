
#import "TiBase.h"

#import "ImageModule.h"
#import "GPUImage.h"
#import "TiViewProxy.h"

#define USE_TI_MEDIA

#import "MediaModule.h"


@implementation ImageModule

MAKE_SYSTEM_PROP(FILTER_GAUSSIAN_BLUR,0);

-(void)startup
{
	[super startup];
}

-(void)dealloc
{
    RELEASE_TO_NIL(currentFilter);
	[super dealloc];
}

-(NSString*)getPathToApplicationAsset:(NSString*) fileName
{
	// The application assets can be accessed by building a path from the mainBundle of the application.
    
	NSString *result = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:fileName];
    
	return result;
}

-(void)setCurrentFilter:(Class)class {
    if (currentFilter != nil)
    {
        RELEASE_TO_NIL(currentFilter);
    }
//    if (currentFilter == nil || ![currentFilter isKindOfClass:class])
//    {
        currentFilter = [[class alloc] init];
//    }
}

-(UIImage*)getFilteredImage:(UIImage*)inputImage withFilter:(NSNumber*)filterType options:(NSDictionary*)options
{
    int type = [filterType intValue];
    switch (type) {
        case 0:
        {
            [self setCurrentFilter:[GPUImageFastBlurFilter class]];
            CGFloat blurSize = [TiUtils floatValue:@"blurSize" properties:options def:1.0f] *1.2f; //multiplicator to get closer result to android version
            ((GPUImageFastBlurFilter*)currentFilter).blurSize = blurSize;
            return [currentFilter imageByFilteringImage:inputImage];
            break;
        }
        default:
            break;
    }
    return nil;
}

-(id)getFilteredImage:(id)args
{
    ENSURE_TYPE(args, NSArray)
    NSNumber *filterType;
    NSDictionary *options;
	ENSURE_ARG_AT_INDEX(filterType, args, 1, NSNumber);
    ENSURE_ARG_OR_NIL_AT_INDEX(options, args, 2, NSDictionary);
    id imageArg = [args objectAtIndex:0];
    UIImage* image = nil;
    if ([imageArg isKindOfClass:[NSString class]]) {
        NSString *imagePath = [self getPathToApplicationAsset:imageArg];
        image = [UIImage imageWithContentsOfFile:imagePath];
    }
    else if([imageArg isKindOfClass:[UIImage class]]) {
        image = imageArg;
    }
    else if([imageArg isKindOfClass:[TiBlob class]]) {
        image = ((TiBlob*)imageArg).image;
    }
    
    if (image == nil) {
        NSLog(@"[ERROR] getFilteredImage: could not load image from object of type: %@",[imageArg class]);
		return;
    }
    
    if (options != nil) {
        if ([options objectForKey:@"scale"]) {
            float scale = [[options objectForKey:@"scale"] floatValue];
            CGSize imageSize = image.size;
            imageSize.width *=scale;
            imageSize.height *=scale;
            image = [TiUtils scaleImage:image toSize:imageSize];
        }
        if ([options objectForKey:@"callback"]) {
            KrollCallback *callback = [options objectForKey:@"callback"];
            if (callback != nil) {
                TiThreadPerformOnMainThread(^{                   
                    TiBlob* blob = [[TiBlob alloc] initWithImage:[self getFilteredImage:image withFilter:filterType options:options]];
                    NSDictionary *event = [NSDictionary dictionaryWithObject:blob forKey:@"image"];
                    KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback
                                                                            eventObject:event
                                                                             thisObject:self];
                    [[callback context] enqueue:invocationEvent];
                    [invocationEvent release];
                    [blob release];
                }, NO);
                return nil;
            }
        }
    }
    
    UIImage* result = [self getFilteredImage:image withFilter:filterType options:options];
    return [[[TiBlob alloc] initWithImage:result] autorelease];
}

-(id)getFilteredViewToImage:(id)args
{
    ENSURE_TYPE(args, NSArray)
    TiViewProxy *viewProxy = nil;
    NSNumber *filterType = nil;
    float scale = 1.0f;
    NSDictionary *options = nil;
	ENSURE_ARG_AT_INDEX(viewProxy, args, 0, TiViewProxy);
	ENSURE_ARG_AT_INDEX(filterType, args, 1, NSNumber);
    ENSURE_ARG_OR_NIL_AT_INDEX(options, args, 2, NSDictionary);
    if (options != nil) {
        if ([options objectForKey:@"scale"]) {
            scale = [[options objectForKey:@"scale"] floatValue];
        }
        if ([options objectForKey:@"callback"]) {
            KrollCallback *callback = [options objectForKey:@"callback"];
            if (callback != nil) {
                TiThreadPerformOnMainThread(^{
                    UIImage *inputImage = [viewProxy toImageWithScale:scale];
                    TiBlob* blob = [[TiBlob alloc] initWithImage:[self getFilteredImage:inputImage withFilter:filterType options:options]];
                    NSDictionary *event = [NSDictionary dictionaryWithObject:blob forKey:@"image"];
                    KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback
                                                                            eventObject:event
                                                                             thisObject:self];
                    [[callback context] enqueue:invocationEvent];
                    [invocationEvent release];
                    [blob release];
                }, NO);
                return nil;
            }
        }
    }
    
    __block TiBlob* result =[[TiBlob alloc] init];
    TiThreadPerformOnMainThread(^{
        UIImage *inputImage = [viewProxy toImageWithScale:scale];
        result = [[TiBlob alloc] initWithImage:[self getFilteredImage:inputImage withFilter:filterType options:options]];
	}, YES);
    return [result autorelease];
    
}

-(id)getFilteredScreenshot:(id)args
{
    ENSURE_TYPE(args, NSArray)
    NSNumber *filterType;
    NSNumber *scale = [NSNumber numberWithFloat:1.0f];
    NSDictionary *options;
	ENSURE_ARG_AT_INDEX(scale, args, 0, NSNumber);
	ENSURE_ARG_AT_INDEX(filterType, args, 0, NSNumber);
    ENSURE_ARG_OR_NIL_AT_INDEX(options, args, 1, NSDictionary);
    if (options != nil) {
        if ([options objectForKey:@"scale"]) {
            scale = [options objectForKey:@"scale"];
        }
    }
    
    __block TiBlob* result =[[TiBlob alloc] init];
    TiThreadPerformOnMainThread(^{
		UIImage *inputImage = [MediaModule takeScreenshotWithScale:[scale floatValue]];
        result.image = [self getFilteredImage:inputImage withFilter:filterType options:options];
        [result setMimeType:@"image/png" type:TiBlobTypeImage];

	}, YES);
    return [result autorelease];
}

#pragma mark Public Constants
@end
