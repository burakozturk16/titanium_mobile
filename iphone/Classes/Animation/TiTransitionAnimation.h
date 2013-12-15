#import "TiHLSAnimation.h"
@class TiViewProxy;
@interface TiTransitionAnimation : TiHLSAnimation {
@private
    TiViewProxy* m_holderViewProxy;
    TiViewProxy* m_transitionViewProxy;
}
@end
