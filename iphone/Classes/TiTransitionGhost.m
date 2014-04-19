#import "TiTransitionGhost.h"
#import "ADGhostTransition.h"

@implementation TiTransitionGhost

-(Class) adTransitionClass {
    return [ADGhostTransition class];
}

@end
