#import "TiTransitionPushRotate.h"
#import "ADPushRotateTransition.h"

@implementation TiTransitionPushRotate

-(Class) adTransitionClass {
    return [ADPushRotateTransition class];
}

@end
