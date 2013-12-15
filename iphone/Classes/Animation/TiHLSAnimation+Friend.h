#import "HLSObjectAnimation.h"
#import "HLSObjectAnimation+Friend.h"
#import "TiHLSAnimation.h"
#import "HLSAnimation.h"
/**
 * Interface meant to be used by friend classes of TiViewAnimation (= classes which must have access to private implementation
 * details)
 */
@interface TiHLSAnimation (Friend)

@property (nonatomic, retain) TiAnimation* animationProxy;
@property (nonatomic, retain) TiAnimatableProxy* animatedProxy;
@property (nonatomic, assign) BOOL isReversed;
- (id)reverseObjectAnimation;
-(NSDictionary*)animationProperties;
@end
