#import "TiViewAnimation.h"
#import "TiHLSAnimation+Friend.h"
@class TiPoint;
@class TiColor;
@class TiProxy;
@class TiViewAnimationStep;
@class TiViewProxy;
/**
 * Interface meant to be used by friend classes of TiViewAnimation (= classes which must have access to private implementation
 * details)
 */
@interface TiViewAnimation (Friend)

@property(nonatomic,retain,readwrite) NSNumber	*zIndex;
@property(nonatomic,retain,readwrite) TiPoint	*center;
@property(nonatomic,retain,readwrite) TiColor	*color;
@property(nonatomic,retain,readwrite) TiColor	*backgroundColor;
@property(nonatomic,retain,readwrite) NSNumber	*opacity;
@property(nonatomic,retain,readwrite) NSNumber	*opaque;
@property(nonatomic,retain,readwrite) NSNumber	*visible;
@property(nonatomic,retain,readwrite) TiProxy	*transform;
@property (nonatomic, retain) TiViewProxy* tiViewProxy;

-(void)checkParameters;
-(void)applyOnView:(UIView*)_view forStep:(TiViewAnimationStep*) step;

@end
