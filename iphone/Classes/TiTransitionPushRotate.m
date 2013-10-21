#import "TiTransitionPushRotate.h"
#import "ADPushRotateTransition.h"

@implementation TiTransitionPushRotate

- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADPushRotateTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
}

@end
