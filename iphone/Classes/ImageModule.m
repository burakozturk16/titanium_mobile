
#import "TiBase.h"

#import "ImageModule.h"
#import "GPUImage/GPUImage.h"
#import "TiViewProxy.h"
#import "TiImageHelper.h"

#define USE_TI_MEDIA

#import "MediaModule.h"


@implementation ImageModule

MAKE_SYSTEM_PROP(FILTER_GAUSSIAN_BLUR,TiImageHelperFilterGaussianBlur);
MAKE_SYSTEM_PROP(FILTER_IOS_BLUR,TiImageHelperFilterIOSBlur);

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
    if (currentFilter == nil || ![currentFilter isKindOfClass:class])
    {
        currentFilter = [[class alloc] init];
    }
}

typedef UIImage* (^ProcessImageBlock) ();

-(id)processImage:(ProcessImageBlock)block withOptions:(NSDictionary*)options
{
    if ([options objectForKey:@"callback"]) {
        id callback = [options objectForKey:@"callback"];
        ENSURE_TYPE(callback, KrollCallback)
        if (callback != nil) {
            TiThreadPerformOnMainThread(^{
                UIImage* result = [[TiImageHelper imageFiltered:block() withOptions:options] retain];
                TiBlob* blob = [[TiBlob alloc] initWithImage:result];
                NSDictionary *event = [NSDictionary dictionaryWithObject:blob forKey:@"image"];
                KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback
                                                                        eventObject:event
                                                                         thisObject:self];
                [[(KrollCallback*)callback context] enqueue:invocationEvent];
                [invocationEvent release];
                [blob release];
                [result release];
            }, NO);
            
        }
        return nil;
    }
    else {
        UIImage* result = [[TiImageHelper imageFiltered:block() withOptions:options] retain];
        return [[[TiBlob alloc] initWithImage:[result autorelease]] autorelease];
    }
}

-(id)getFilteredImage:(id)args
{
    ENSURE_TYPE(args, NSArray)
    NSDictionary *options;
    ENSURE_ARG_OR_NIL_AT_INDEX(options, args, 1, NSDictionary);
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
    return [self processImage:^UIImage *{
        return image;
    } withOptions:options];
}

-(id)getFilteredViewToImage:(id)args
{
    ENSURE_TYPE(args, NSArray)
    TiViewProxy *viewProxy = nil;
    float scale = 1.0f;
    NSDictionary *options = nil;
	ENSURE_ARG_AT_INDEX(viewProxy, args, 0, TiViewProxy);
    ENSURE_ARG_OR_NIL_AT_INDEX(options, args, 1, NSDictionary);
    
    if ([options objectForKey:@"scale"]) {
        scale = [[options objectForKey:@"scale"] floatValue];
    }
    return [self processImage:^UIImage *{
        return [viewProxy toImageWithScale:scale];
    } withOptions:options];
}

-(id)getFilteredScreenshot:(id)args
{
    ENSURE_TYPE(args, NSArray)
    TiViewProxy *viewProxy = nil;
    float scale = 1.0f;
    NSDictionary *options = nil;
	ENSURE_ARG_AT_INDEX(viewProxy, args, 0, TiViewProxy);
    ENSURE_ARG_OR_NIL_AT_INDEX(options, args, 1, NSDictionary);
    
    if ([options objectForKey:@"scale"]) {
        scale = [[options objectForKey:@"scale"] floatValue];
    }
    return [self processImage:^UIImage *{
        return [MediaModule takeScreenshotWithScale:scale];
    } withOptions:options];
}

#pragma mark Public Constants
@end
