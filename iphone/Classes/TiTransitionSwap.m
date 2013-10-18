#import "TiTransitionSwap.h"
#import "ADSwapTransition.h"

@implementation TiTransitionSwap

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADSwapTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
}

@end
