#import "TiTransitionScale.h"
#import "ADScaleTransition.h"

@implementation TiTransitionScale

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADScaleTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
}

@end
