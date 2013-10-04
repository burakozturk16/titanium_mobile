//
//  TiSVGImage.m
//  Titanium
//
//  Created by Martin Guillon on 04/10/13.
//
//

#import "TiSVGImage.h"
#import "TiBase.h"

@interface TiSVGImage()
{
    UIImage * fullImage;
 	UIImage * recentlyResizedImage;
}

@end

@implementation TiSVGImage

- (void) dealloc
{
	RELEASE_TO_NIL(recentlyResizedImage);
	RELEASE_TO_NIL(fullImage);
	[super dealloc];
}

-(UIImage*)imageForSize:(CGSize)size
{
    if (CGSizeEqualToSize(size, CGSizeZero)) return [self fullImage];
    float screenScale = [UIScreen mainScreen].scale;
    
   
    CGSize svgSize = [self hasSize]?[self size]:size;
    
    if ([self hasSize] && CGSizeEqualToSize(size, svgSize))
    {
		return [self fullImage];
    }
    
    CGFloat SVGRatio = svgSize.width/svgSize.height;
    CGSize realSize;
    if (size.width > size.height) {
        realSize = CGSizeMake(size.width, size.width / SVGRatio);
    }
    else {
        realSize = CGSizeMake(size.height * SVGRatio, size.height);
    }
    realSize.width = roundf(realSize.width * screenScale);
    realSize.height = roundf(realSize.height * screenScale);
    
    if (recentlyResizedImage != nil) {
        CGSize oldSize = recentlyResizedImage.size;
        if (CGSizeEqualToSize(realSize, oldSize)) {
            return recentlyResizedImage;
        }
        
    }
    UIGraphicsBeginImageContextWithOptions( realSize, NO, screenScale );
	CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGSize scale = CGSizeMake( realSize.width /  svgSize.width, realSize.height / svgSize.height);
    CGContextScaleCTM( context, scale.width, scale.height );
    [self.CALayerTree renderInContext:context];
	UIImage* result = UIGraphicsGetImageFromCurrentImageContext();
	UIGraphicsEndImageContext();
    recentlyResizedImage = [result retain];
	return result;
}

- (UIImage *)fullImage {
	if(fullImage == nil) {
		fullImage = [[self UIImage] retain];
	}
	return fullImage;
}

@end
