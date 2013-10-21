#import "TitransitionBackFade.h"
#import "ADBackFadeTransition.h"

@implementation TitransitionBackFade

- (id)initWithDuration:(CFTimeInterval)duration
{
    if (self = [super init]) {
        _adTransition = [[ADBackFadeTransition alloc] initWithDuration:duration];
    }
    return self;
}

@end
