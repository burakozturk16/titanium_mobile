#import "AbstractActionSheetPicker.h"

@class CustomActionSheet;
@protocol CustomActionSheetDelegate <NSObject>

@optional
- (void)customActionSheet:(CustomActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex;
- (void)customActionSheet:(CustomActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)customActionSheetCancel:(CustomActionSheet *)actionSheet;

@end


@interface CustomActionSheet : AbstractActionSheetPicker
@property (nonatomic, copy) NSString *title;
@property (nonatomic, copy) NSString *htmlTitle;
@property (nonatomic, strong) UIColor *tintColor;
@property (nonatomic, assign) int cancelIndex;
@property (nonatomic, assign) BOOL dismissOnAction;
@property (nonatomic, assign) BOOL tapOutDismiss;
@property (nonatomic, assign) UIActionSheetStyle style;
@property (nonatomic, strong) id<CustomActionSheetDelegate> delegate;

- (void)showActionSheet;

    // For subclasses.  This returns a configured view.  Subclasses should autorelease.
- (UIView *)configuredCustomView;

-(BOOL)isVisible;
- (void)dismissAnimated:(BOOL)animated;
- (void)dismiss;
- (void)showFromToolbar:(UIToolbar *)view;
- (void)showFromTabBar:(UITabBar *)view;
- (void)showFromBarButtonItem:(UIBarButtonItem *)item animated:(BOOL)animated;
- (void)showFromRect:(CGRect)rect inView:(UIView *)view animated:(BOOL)animated;
- (void)showInView:(UIView *)view;
@end
