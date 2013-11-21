#import "TiTransitionZoom.h"
#import "ADZoomTransition.h"

@implementation TiTransitionZoom

- (id)initWithSourceRect:(CGRect)sourceRect andTargetRect:(CGRect)targetRect forDuration:(double)duration
{
    if (self = [super init]) {
        _adTransition = [[ADZoomTransition alloc] initWithSourceRect:sourceRect andTargetRect:targetRect forDuration:duration];
    }
    return self;
}

- (id)initWithScale:(CGFloat)scale forDuration:(double)duration {
    if (self = [super init]) {
        _adTransition = [[ADZoomTransition alloc] initWithScale:scale forDuration:duration];
    }
    return self;
}
@end
