
#import "TiModule.h"

@class GPUImageFilter;
@interface ImageModule : TiModule {
    GPUImageFilter* currentFilter;
}
@property(nonatomic,readonly) NSNumber* FILTER_GAUSSIAN_BLUR;
@property(nonatomic,readonly) NSNumber* FILTER_IOS_BLUR;

@end
