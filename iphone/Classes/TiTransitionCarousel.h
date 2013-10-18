#import "TiTransition.h"

@interface TiTransitionCarousel : TiTransition
@property(nonatomic,assign)	CGFloat faceNb;
- (id)initWithDuration:(CFTimeInterval)duration orientation:(ADTransitionOrientation)orientation sourceRect:(CGRect)sourceRect;

@end
