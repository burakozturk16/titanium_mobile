#import "TiTransitionAnimation.h"
#import "TiHLSAnimation+Friend.h"
@class TiTransitionAnimationStep;
@class TiViewProxy;

@interface TiTransitionAnimation (Friend)
@property (nonatomic, retain) TiViewProxy* holderViewProxy;
@property (nonatomic, retain) TiViewProxy* transitionViewProxy;
@property (nonatomic, assign) int transition;
@property (nonatomic, assign) BOOL openTransition;
@property (nonatomic, assign) BOOL closeTransition;
@end
