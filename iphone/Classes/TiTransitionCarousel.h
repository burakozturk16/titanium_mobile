#import "TiTransitionPerspective.h"

@interface TiTransitionCarousel : TiTransitionPerspective
@property(nonatomic,assign)	CGFloat faceNb;
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;

@end
