#import "TiHLSAnimationStep.h"

@class TiTransitionAnimation, TiViewProxy;
@interface TiTransitionAnimationStep : TiHLSAnimationStep {
}


- (void)addTransitionAnimation:(TiTransitionAnimation *)animation insideHolder:(UIView *)holder;
@end
