#import "TiTransitionZoom.h"
#import "ADZoomTransition.h"

@implementation TiTransitionZoom

- (id)initWithDuration:(CFTimeInterval)duration
{
    if (self = [super init]) {
        _adTransition = [[ADZoomTransition alloc] initWithDuration:duration];
    }
    return self;
}

@end
