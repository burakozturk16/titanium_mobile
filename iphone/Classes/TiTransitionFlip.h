#import "TiTransitionPerspective.h"

@interface TiTransitionFlip : TiTransitionPerspective
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;
@end
