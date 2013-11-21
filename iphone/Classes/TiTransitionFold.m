#import "TiTransitionFold.h"
#import "ADFoldTransition.h"

@implementation TiTransitionFold


- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect
{
    if (self = [super init]) {
        _adTransition = [[ADFoldTransition alloc] initWithDuration:duration orientation:orientation sourceRect:sourceRect];
    }
    return self;
}

-(void)transformView:(UIView*)view withPosition:(CGFloat)position adjustTranslation:(BOOL)adjust size:(CGSize)size
{
}

-(BOOL)needsReverseDrawOrder
{
    return [self isTransitionPush];
}
@end

