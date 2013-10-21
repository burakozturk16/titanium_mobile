#import "TiTransitionCross.h"
#import "ADCrossTransition.h"

@implementation TiTransitionCross

- (id)initWithDuration:(CFTimeInterval)duration sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADCrossTransition alloc] initWithDuration:duration sourceRect:sourceRect];
    }
    return self;
}

@end
