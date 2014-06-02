#import "TitransitionBackFade.h"
#import "ADBackFadeTransition.h"

@implementation TitransitionBackFade

-(Class) adTransitionClass {
    return [ADBackFadeTransition class];
}
@end
