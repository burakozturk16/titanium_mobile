#import "UIGestureRecognizer+ADNavTransition.h"
#import <objc/runtime.h>

@implementation UIGestureRecognizer (ADNavTransition)

- (UIViewController *)AD_viewController
{
    return objc_getAssociatedObject(self, @selector(AD_viewController));
}

- (void)setAD_viewController:(UIViewController *)AD_viewController
{
    objc_setAssociatedObject(self, @selector(AD_viewController), AD_viewController, OBJC_ASSOCIATION_ASSIGN);
}
@end
