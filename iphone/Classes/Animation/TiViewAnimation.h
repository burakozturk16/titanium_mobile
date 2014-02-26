

/**
 * A view animation (HLSViewAnimation) describes the changes applied to a view within an animation step 
 * (HLSViewAnimationStep). An animation step is the combination of several view animations applied
 * to a set of views, and represent the collective set of changes applied to them during some time interval. 
 * An animation (HLSAnimation) is then simply a collection of animation steps, either view-based
 * (HLSViewAnimationStep) or layer-based (HLSLayerAnimationStep).
 *
 * Note that a view animation:
 *   - animates view frames. The animated views have to correctly respond to such changes, either by setting
 *     their autoresizing mask properly, or by implementing -layoutSubviews if they need a finer control
 *     over the resizing process
 *   - applies only affine operations
 *
 * In general, and if you do not need to animate view frames to resize subviews during animations, you should 
 * use layer animations instead of view animations since they have far more capabilities.
 *
 * Designated initializer: -init (create a view animation step with default settings)
 */
#import "TiHLSAnimation.h"
@class TiViewProxy;
@class TiPoint;
@class TiColor;
@class TiProxy;
@interface TiViewAnimation : TiHLSAnimation {
@private
    TiViewProxy* m_tiViewProxy;
    
    NSNumber	*zIndex;
    id  left;
    id  right;
    id  top;
    id  bottom;
    id  width;
    id  height;
    BOOL  _fullscreen;
    
    id minWidth;
    id minHeight;
    id maxWidth;
    id maxHeight;
 
    BOOL  leftDefined;
    BOOL  rightDefined;
    BOOL  topDefined;
    BOOL  bottomDefined;
    BOOL  widthDefined;
    BOOL  heightDefined;
    BOOL  transformDefined;
    BOOL  fullscreenDefined;
    
    BOOL minWidthDefined;
    BOOL minHeightDefined;
    BOOL maxWidthDefined;
    BOOL maxHeightDefined;
   
    
    TiPoint		*center;
    TiColor		*backgroundColor;
    TiColor		*color;
    NSNumber	*opacity;
    NSNumber	*opaque;
    NSNumber	*visible;
    TiProxy		*transform;
    NSNumber	*transition;
}
@end
