#import "HLSAnimationStep.h"

/**
 * A view animation step (TiViewAnimationStep) is the combination of several view animations (TiViewAnimation) applied
 * to a set of views, and represent the collective set of changes applied to them during some time interval. An animation
 * (HLSAnimation) is then simply a collection of animation steps, either view-based (TiViewAnimationStep) or layer-based 
 * (HLSLayerAnimationStep).
 *
 * To create a view animation step, simply instantiate it using the +animationStep class method, then add view animations
 * to it, and set its duration and curve
 *
 * Designated initializer: -init (create an animation step with default settings)
 */
@interface TiHLSAnimationStep : HLSAnimationStep {
@private
    UIViewAnimationCurve m_curve;
}

/**
 * The animation curve to use
 *
 * Default value is UIViewAnimationCurveEaseInOut
 */
@property (nonatomic, assign) UIViewAnimationCurve curve;

@end
