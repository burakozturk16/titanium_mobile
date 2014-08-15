#import <Foundation/Foundation.h>

@class CustomActionSheet;
@protocol CustomActionSheetDelegate <NSObject>

@optional
- (void)customActionSheet:(CustomActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex;
- (void)customActionSheet:(CustomActionSheet *)actionSheet clickedButtonAtIndex:(NSInteger)buttonIndex;
- (void)customActionSheetCancel:(CustomActionSheet *)actionSheet;

@end


@interface CustomActionSheet : NSObject<UIActionSheetDelegate, UIPopoverControllerDelegate>
@property (nonatomic, strong) UIToolbar* toolbar;
@property (nonatomic, copy) NSString *title;
@property (nonatomic, copy) NSString *htmlTitle;
@property (nonatomic, strong) UIView *pickerView;
@property (nonatomic, readonly) CGSize viewSize;
@property (nonatomic, strong) NSMutableArray *customButtons;
@property (nonatomic, strong) UIColor *tintColor;
@property (nonatomic, assign) BOOL hideCancel;
@property (nonatomic, assign) BOOL dismissOnAction;
@property (nonatomic, assign) BOOL tapOutDismiss;
@property (nonatomic, assign) UIActionSheetStyle style;
@property (nonatomic, assign) CGRect presentFromRect;
@property (nonatomic, strong) id<CustomActionSheetDelegate> delegate;

    // For subclasses.
- (id)initWithTarget:(id)target successAction:(SEL)successAction cancelAction:(SEL)cancelActionOrNil origin:(id)origin;

- (void)showActionSheet;

    // For subclasses.  This returns a configured view.  Subclasses should autorelease.
- (UIView *)configuredCustomView;

    // Adds custom buttons to the left of the UIToolbar that select specified values
- (void)addCustomButtonWithTitle:(NSString *)title value:(id)value;

    //For subclasses. This responds to a custom button being pressed.
- (IBAction)customButtonPressed:(id)sender;

    // Allow the user to specify a custom cancel button
- (void) setCancelButton: (UIBarButtonItem *)button;

    // Allow the user to specify a custom done button
- (void) setDoneButton: (UIBarButtonItem *)button;

-(BOOL)isVisible;
- (void)dismissAnimated:(BOOL)animated;
- (void)dismiss;
- (UIBarButtonItem *)createButtonWithType:(UIBarButtonSystemItem)type target:(id)target action:(SEL)buttonAction;
- (void)showFromToolbar:(UIToolbar *)view;
- (void)showFromTabBar:(UITabBar *)view;
- (void)showFromBarButtonItem:(UIBarButtonItem *)item animated:(BOOL)animated;
- (void)showFromRect:(CGRect)rect inView:(UIView *)view animated:(BOOL)animated;
- (void)showInView:(UIView *)view;
@end
