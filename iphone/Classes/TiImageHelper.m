//
//  TiImageHelper.m
//  Titanium
//
//  Created by Martin Guillon on 11/10/13.
//
//

#import "TiImageHelper.h"
#import "TiBase.h"
#import "TiUtils.h"
#import "TiDimension.h"
#import "UIImage+Resize.h"
#import <GPUImage/GPUImage.h>
#import "SLColorArt.h"
#import "UIImage+UserInfo.h"
#import "UIImage+ImageEffects.h"


@implementation TiImageHelper

+(GPUImageOutput<GPUImageInput>*)getFilter:(TiImageHelperFilterType)filterType options:(NSDictionary*)options
{
    switch (filterType) {
        case TiImageHelperFilterIOSBlur:
        {
            GPUImageiOSBlurFilter* filter = [[GPUImageiOSBlurFilter alloc] init];
            filter.blurRadiusInPixels = [TiUtils floatValue:@"radius" properties:options def:12.0f];
            filter.downsampling = [TiUtils floatValue:@"downsampling" properties:options def:4.0f];
            filter.saturation = [TiUtils floatValue:@"saturation" properties:options def:0.8f];
            return [filter autorelease];
        }
        case TiImageHelperFilterGaussianBlur:
        {
            GPUImageGaussianBlurFilter* filter = [[GPUImageGaussianBlurFilter alloc] init];
            filter.blurRadiusInPixels = [TiUtils floatValue:@"radius" properties:options def:2.0f];
            filter.blurPasses = [TiUtils floatValue:@"passes" properties:options def:1.0f];
            filter.texelSpacingMultiplier = [TiUtils floatValue:@"texelSpacingMultiplier" properties:options def:1.0f];
            return [filter autorelease];
        }
        default:
            break;
    }
    return nil;
}

+(UIImage*)getFilteredImage:(UIImage*)inputImage withFilter:(TiImageHelperFilterType)filterType options:(NSDictionary*)options
{
    if (filterType == TiImageHelperFilterIOSBlur) {
        float radius = [TiUtils floatValue:@"radius" properties:options def:12.0f];
        float downsampling = [TiUtils floatValue:@"downsampling" properties:options def:4.0f];
        float saturation = [TiUtils floatValue:@"saturation" properties:options def:0.8f];
        UIColor* tint = [[TiUtils colorValue:@"tint" properties:options] _color];
        return [inputImage applyBlurWithRadius:radius tintColor:tint saturationDeltaFactor:saturation maskImage:nil];
    }
    GPUImageOutput* filter = [self getFilter:filterType options:options];
    return [filter imageByFilteringImage:inputImage];
}

+(UIImage*)tintedImage:(UIImage*)source withColor:(UIColor*)color blendMode:(CGBlendMode)mode
{
    CGRect rect = CGRectMake(0, 0, source.size.width, source.size.height);
    UIGraphicsBeginImageContextWithOptions(source.size, NO, source.scale);
    CGContextRef context = UIGraphicsGetCurrentContext();
    // draw black background to preserve color of transparent pixels
    CGContextSetBlendMode(context, kCGBlendModeNormal);
    [[UIColor blackColor] setFill];
    CGContextFillRect(context, rect);
    
    // draw original image
    CGContextSetBlendMode(context, kCGBlendModeNormal);
    CGContextTranslateCTM(context, 0, source.size.height);
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextDrawImage(context, rect, source.CGImage);
    
    // tint image (loosing alpha) - the luminosity of the original image is preserved
    CGContextSetBlendMode(context, mode);
    [color setFill];
    CGContextFillRect(context, rect);
    
    // mask by alpha values of original image
    CGContextSetBlendMode(context, kCGBlendModeDestinationIn);
    CGContextDrawImage(context, rect, source.CGImage);
    UIImage *tintedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return tintedImage;
}

+(id)imageFiltered:(UIImage*)image withOptions:(NSDictionary*)options
{
    int width = image.size.width;
    int height = image.size.height;
    NSDictionary* info = nil;
    if ([options objectForKey:@"crop"]) {
        NSDictionary* rectObj = [options objectForKey:@"crop"];
        CGRect bounds = CGRectMake(TiDimensionCalculateValueFromStringInBouding([rectObj objectForKey:@"x"], width)
                                 , TiDimensionCalculateValueFromStringInBouding([rectObj objectForKey:@"y"], height), TiDimensionCalculateValueFromStringInBouding([rectObj objectForKey:@"width"], width), TiDimensionCalculateValueFromStringInBouding([rectObj objectForKey:@"height"], height));
        image = [UIImageResize croppedImage:bounds image:image];
    }

    

    if ([options objectForKey:@"scale"]) {
        CGFloat scale = [TiUtils floatValue:@"scale" properties:options def:1.0f];
        if (scale != 1.0f) {
            CGSize size = CGSizeMake(scale * width, scale * height);
            image = [UIImageResize resizedImage:size interpolationQuality:kCGInterpolationMedium image:image hires:NO ];
        }
    }
    
    if ([options objectForKey:@"filters"]) {
        NSArray* filters = [options objectForKey:@"filters"];
        if ([filters count] > 0) {
            GPUImageFilterGroup* group = [[GPUImageFilterGroup alloc] init];
            GPUImageOutput<GPUImageInput>* lastFilter = nil;
            for (NSDictionary* filterOptions in filters) {
                TiImageHelperFilterType type = [[filterOptions valueForKey:@"type"] integerValue];
                GPUImageOutput<GPUImageInput>* filter = [self getFilter:type options:filterOptions];
                if (filter) {
                    if (lastFilter) {
                        [lastFilter addTarget:filter];
                    }
                    [group addFilter:filter];
                    lastFilter = filter;
                }
            }
            group.initialFilters = @[[group filterAtIndex:0]];
            group.terminalFilter = [group filterAtIndex:[group filterCount] - 1];
            image = [group imageByFilteringImage:image];
            [group release];
        }
    }
    id tint = [options objectForKey:@"tint"];
    if (tint && tint != [NSNull null]) {
        UIColor* color = [[TiUtils colorValue:@"tint" properties:options].color retain];
        CGBlendMode mode = [TiUtils intValue:@"blend" properties:options def:kCGBlendModeMultiply];
        image = [self tintedImage:image withColor:color blendMode:mode];
        [color release];
    }
    if ([options objectForKey:@"colorArt"]) {
        NSObject* colorArtOptions = [options objectForKey:@"colorArt"];
        CGSize size = CGSizeMake(120, 120);
        if (IS_OF_CLASS(colorArtOptions, NSDictionary)) {
            size.width = [TiUtils intValue:@"width" properties:(NSDictionary*)colorArtOptions def:120];
            size.height = [TiUtils intValue:@"height" properties:(NSDictionary*)colorArtOptions def:120];
        }
        SLColorArt *color = [[SLColorArt alloc] initWithImage:image scaleSize:size];
        if (!info) {
            info = [NSMutableDictionary dictionary];
        }
        [info setValue:@{
                         @"backgroundColor":[TiUtils colorHexString:color.backgroundColor],
                         @"primaryColor":[TiUtils colorHexString:color.primaryColor],
                         @"secondaryColor":[TiUtils colorHexString:color.secondaryColor],
                         @"detailColor":[TiUtils colorHexString:color.detailColor],
                         } forKey:@"colorArt"];
        [color release];
    }
    
    image.info = info;
    return image;
}
@end
