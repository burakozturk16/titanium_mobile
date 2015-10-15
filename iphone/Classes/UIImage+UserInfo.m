
#import "UIImage+UserInfo.h"
#import <objc/runtime.h>

NSString * const kInfoKey = @"kInfo";
NSString * const kCompressionLevel = @"kCompressionLevel";
@implementation UIImage (UserInfo)

- (void)setInfo:(NSDictionary *)info
{
    objc_setAssociatedObject(self, kInfoKey, info, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (NSDictionary*)info
{
    return objc_getAssociatedObject(self, kInfoKey);
}


- (void)setCompressionLevel:(CGFloat)compressionLevel
{
    objc_setAssociatedObject(self, kCompressionLevel, @(compressionLevel), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (CGFloat)compressionLevel
{
    id obj = objc_getAssociatedObject(self, kCompressionLevel);
    if (obj) {
        return [obj floatValue];
    }
    return 1.0f;
}

@end
