#import "TiTransitionPerspective.h"

@interface TiTransitionZoom : TiTransitionPerspective
- (id)initWithSourceRect:(CGRect)sourceRect andTargetRect:(CGRect)targetRect forDuration:(double)duration;
- (id)initWithScale:(CGFloat)scale forDuration:(double)duration;

@end
