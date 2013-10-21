#import "TiTransition.h"

@interface TiTransitionGlue : TiTransition
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;

@end
