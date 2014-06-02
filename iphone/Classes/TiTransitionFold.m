#import "TiTransitionFold.h"
#import "ADFoldTransition.h"

@implementation TiTransitionFold


-(Class) adTransitionClass {
    return [ADFoldTransition class];
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
}

-(BOOL)needsReverseDrawOrder
{
    return [self isTransitionPush];
}
@end

