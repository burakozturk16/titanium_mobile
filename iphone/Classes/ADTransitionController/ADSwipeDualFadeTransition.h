#import "ADDualTransition.h"

#define kSwipeDualFadeTranslate 1.0f
@interface ADSwipeDualFadeTransition : ADDualTransition
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;
@end
