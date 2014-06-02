#import "TiTransitionModernPush.h"
#import "ADModernPushTransition.h"

@implementation TiTransitionModernPush

-(Class) adTransitionClass {
    return [ADModernPushTransition class];
}


@end
