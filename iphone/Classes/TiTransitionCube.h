#import "TiTransition.h"

@interface TiTransitionCube : TiTransition
@property(nonatomic,assign)	CGFloat faceNb;
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;

@end
