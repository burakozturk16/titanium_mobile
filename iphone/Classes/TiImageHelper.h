//
//  TiImageHelper.h
//  Titanium
//
//  Created by Martin Guillon on 11/10/13.
//
//

typedef NS_ENUM(NSInteger, TiImageHelperFilterType) {
    TiImageHelperFilterBoxBlur,
    TiImageHelperFilterGaussianBlur,
    TiImageHelperFilterIOSBlur
};

@interface TiImageHelper : NSObject
+(UIImage*)getFilteredImage:(UIImage*)inputImage withFilter:(TiImageHelperFilterType)filterType options:(NSDictionary*)options;
+(id)imageFiltered:(UIImage*)image withOptions:(NSDictionary*)options;
+(UIImage*)tintedImage:(UIImage*)source withColor:(UIColor*)color blendMode:(CGBlendMode)mode;
@end
