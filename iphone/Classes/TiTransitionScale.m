#import "TiTransitionScale.h"
#import "ADScaleTransition.h"

@implementation TiTransitionScale

-(Class) adTransitionClass {
    return [ADScaleTransition class];
}
@end
