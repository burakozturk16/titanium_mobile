#import "HLSObjectAnimation.h"

@class TiAnimation;
@class TiAnimatableProxy;
@interface TiHLSAnimation : HLSObjectAnimation {
@private
    TiAnimation* m_animationProxy;
    TiAnimatableProxy* m_animatedProxy;
    BOOL m_isReversed;
}
@end
