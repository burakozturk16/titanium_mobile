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
@implementation TiImageHelper

+(UIImage*)getFilteredImage:(UIImage*)inputImage withFilter:(TiImageHelperFilterType)filterType options:(NSDictionary*)options
{
    switch (filterType) {
        case TiImageHelperFilterIOSBlur:
        {
            GPUImageiOSBlurFilter* filter = [[GPUImageiOSBlurFilter alloc] init];
            filter.blurRadiusInPixels = [TiUtils floatValue:@"radius" properties:options def:12.0f];
            filter.downsampling = [TiUtils floatValue:@"downsampling" properties:options def:4.0f];
            filter.saturation = [TiUtils floatValue:@"saturation" properties:options def:0.8f];
            UIImage* result = [filter imageByFilteringImage:inputImage];
            [filter release];
            return result;
        }
        case TiImageHelperFilterGaussianBlur:
        {
            GPUImageGaussianBlurFilter* filter = [[GPUImageGaussianBlurFilter alloc] init];
            filter.blurRadiusInPixels = [TiUtils floatValue:@"radius" properties:options def:2.0f];
            filter.blurPasses = [TiUtils floatValue:@"passes" properties:options def:1.0f];
            filter.texelSpacingMultiplier = [TiUtils floatValue:@"texelSpacingMultiplier" properties:options def:1.0f];
            UIImage* result = [filter imageByFilteringImage:inputImage];
            [filter release];
            return result;
        }
        default:
            break;
    }
    return nil;
}

+(UIImage*)tintedImage:(UIImage*)source withColor:(UIColor*)color blendMode:(CGBlendMode)mode
{
    CGRect rect = CGRectMake(0, 0, source.size.width, source.size.height);
    UIGraphicsBeginImageContext(source.size);
    CGContextRef context = UIGraphicsGetCurrentContext();
    CGContextTranslateCTM(context, 0, source.size.height);
    CGContextScaleCTM(context, 1.0, -1.0);
    CGContextSetBlendMode(context, kCGBlendModeNormal);
    CGContextDrawImage(context, rect, source.CGImage);
    
    CGContextSetBlendMode(context, mode);
    [color setFill];
    CGContextFillRect(context, rect);
    UIImage *tintedImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return tintedImage;
}

+(id)imageFiltered:(UIImage*)image withOptions:(NSDictionary*)options
{
    int width = image.size.width;
    int height = image.size.height;
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
        for (NSNumber* filterId in filters) {
            TiImageHelperFilterType type = [filterId integerValue];
            image = [self getFilteredImage:image withFilter:type options:options];
        }
        width = image.size.width;
        height = image.size.height;
    }
    
    if ([options objectForKey:@"tint"]) {
        UIColor* color = [[TiUtils colorValue:@"tint" properties:options].color retain];
        CGBlendMode mode = [TiUtils intValue:@"blend" properties:options def:kCGBlendModeLighten];
        image = [self tintedImage:image withColor:color blendMode:mode];
        [color release];
    }
    
    return image;
}
@end
