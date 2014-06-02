#import "TiTransitionPerspective.h"

@interface TiTransitionZoom : TiTransitionPerspective
- (id)initWithScale:(CGFloat)scale forDuration:(double)duration orientation:(ADTransitionOrientation)orientation reversed:(BOOL)reversed;

@end
