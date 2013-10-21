#import "TiTransitionGhost.h"
#import "ADGhostTransition.h"

@implementation TiTransitionGhost

- (id)initWithDuration:(CFTimeInterval)duration
{
    if (self = [super init]) {
        _adTransition = [[ADGhostTransition alloc] initWithDuration:duration];
    }
    return self;
}

@end
