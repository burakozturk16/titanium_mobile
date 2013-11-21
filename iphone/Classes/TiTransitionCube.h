#import "TiTransitionPerspective.h"

@interface TiTransitionCube : TiTransitionPerspective
@property(nonatomic,assign)	CGFloat faceNb;
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;

@end
