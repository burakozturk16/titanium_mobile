#import "TiShapeAnimation.h"
#import "TiHLSAnimation+Friend.h"

@class ShapeProxy;
@interface TiShapeAnimation (Friend)

@property (nonatomic, retain) ShapeProxy* shapeProxy;
@property (nonatomic, assign) BOOL autoreverse;
@property (nonatomic, assign) BOOL restartFromBeginning;

@end
