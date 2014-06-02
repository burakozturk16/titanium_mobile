#import "TiTransitionZoom.h"
#import "ADZoomTransition.h"

@implementation TiTransitionZoom

-(Class) adTransitionClass {
    return [ADZoomTransition class];
}

- (id)initWithScale:(CGFloat)scale forDuration:(double)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed {
    if (self = [super init]) {
        _adTransition = [[ADZoomTransition alloc] initWithScale:scale forDuration:duration orientation:orientation reversed:reversed];
    }
    return self;
}
@end
