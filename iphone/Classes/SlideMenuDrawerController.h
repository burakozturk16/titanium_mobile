#import "MMDrawerController+Subclass.h"

@class TiProxy;
@interface SlideMenuDrawerController : MMDrawerController
@property(nonatomic,assign) CGFloat fadeDegree;
@property(nonatomic,assign) CGFloat leftDisplacement;
@property(nonatomic,assign) CGFloat rightDisplacement;
@property(nonatomic,retain) TiProxy * proxy;
@property (nonatomic, copy) MMDrawerControllerDrawerVisualStateBlock leftVisualBlock;
@property (nonatomic, copy) MMDrawerControllerDrawerVisualStateBlock rightVisualBlock;
-(CGRect)childControllerContainerViewFrame;

@end
