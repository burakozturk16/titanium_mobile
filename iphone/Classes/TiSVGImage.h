//
//  TiSVGImage.h
//  Titanium
//
//  Created by Martin Guillon on 04/10/13.
//
//

#import "SVGKImage.h"

@interface TiSVGImage : SVGKImage
-(UIImage*)imageForSize:(CGSize)size;
- (UIImage *)fullImage;
@end
