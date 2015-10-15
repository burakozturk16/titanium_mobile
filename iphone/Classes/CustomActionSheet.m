
#import "CustomActionSheet.h"
#import <objc/message.h>
#import <sys/utsname.h>
#import "TiLabel.h"
#import "DTCoreText/DTCoreText.h"
#import "SWActionSheet.h"

CG_INLINE BOOL isIPhone4()
{
    struct utsname systemInfo;
    uname(&systemInfo);
    
    NSString *modelName = [NSString stringWithCString:systemInfo.machine encoding:NSUTF8StringEncoding];
    return ([modelName rangeOfString:@"iPhone3"].location != NSNotFound);
}

@interface AbstractActionSheetPicker ()

@property(nonatomic, strong) UIBarButtonItem *barButtonItem;
@property(nonatomic, strong) UIBarButtonItem *doneBarButtonItem;
@property(nonatomic, strong) UIBarButtonItem *cancelBarButtonItem;
@property(nonatomic, strong) UIView *containerView;
@property(nonatomic, strong) SWActionSheet *actionSheet;
@property(nonatomic, strong) UIPopoverController *popOverController;
@property(nonatomic, strong) NSObject *selfReference;

- (void)presentPickerForView:(UIView *)aView;

- (void)configureAndPresentPopoverForView:(UIView *)aView;

- (void)configureAndPresentActionSheetForView:(UIView *)aView;

- (void)presentActionSheet:(SWActionSheet *)actionSheet;

- (void)presentPopover:(UIPopoverController *)popover;

- (void)dismissPicker;

- (BOOL)isViewPortrait;

- (BOOL)isValidOrigin:(id)origin;

- (id)storedOrigin;

- (UIToolbar *)createPickerToolbarWithTitle:(NSString *)aTitle;

- (UIBarButtonItem *)createButtonWithType:(UIBarButtonSystemItem)type target:(id)target action:(SEL)buttonAction;

- (IBAction)actionPickerDone:(id)sender;

- (IBAction)actionPickerCancel:(id)sender;
@end


@interface CustomActionSheet()
@property (nonatomic, strong) UIBarButtonItem *titleBarButtonItem;
@property (nonatomic, assign) BOOL animated;

@end

@implementation CustomActionSheet
{
    NSUInteger _nbButtons;
}
@synthesize title = _title;
@synthesize htmlTitle = _htmlTitle;
@synthesize animated = _animated;
@synthesize tintColor = _tinColor;
@synthesize dismissOnAction = _dismissOnAction;
@synthesize delegate = _delegate;
@synthesize tapOutDismiss = _tapOutDismiss;
@synthesize style = _style;

#pragma mark - Abstract Implementation

- (id)initWithTarget:(id)target successAction:(SEL)successAction cancelAction:(SEL)cancelActionOrNil origin:(id)origin  {
    self = [super initWithTarget:target successAction:successAction cancelAction:cancelActionOrNil origin:origin];
    if (self) {
        self.presentFromRect = CGRectZero;
        self.dismissOnAction = YES;
        self.tapOutDismiss = NO;
        self.cancelIndex = -1;
        self.style = UIActionSheetStyleDefault;
    }
    return self;
}

- (id)initWithTarget:(id)target  {
    self = [super initWithTarget:target];
    if (self) {
        self.presentFromRect = CGRectZero;
        self.dismissOnAction = YES;
        self.tapOutDismiss = NO;
        self.style = UIActionSheetStyleDefault;
    }
    return self;
}

- (id)init  {
    self = [super init];
    if (self) {
        self.presentFromRect = CGRectZero;

        //allows us to use this without needing to store a reference in calling class
        self.dismissOnAction = YES;
        self.tapOutDismiss = NO;
        self.style = UIActionSheetStyleDefault;
    }
    return self;
}

-(void)setTitle:(NSString *)title
{
    _title = title;
    if (self.titleBarButtonItem) {
        UILabel* label = (UILabel*)[self.titleBarButtonItem customView];
        [label setText:title];
        [label sizeToFit];
    }
}

-(NSString*)defaultSystemFontFamily
{
    static NSString *defaultSystemFontFamily = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        defaultSystemFontFamily = [UIFont systemFontOfSize:[UIFont systemFontSize]].familyName;
    });
    return defaultSystemFontFamily;
}


static NSDictionary* htmlOptions;
-(NSDictionary *)htmlOptions
{
    if (htmlOptions == nil)
    {
        htmlOptions = @{
                        DTDefaultTextAlignment:@(kCTLeftTextAlignment),
                        DTDefaultFontStyle:@(0),
                        DTIgnoreLinkStyleOption:@(NO),
                        DTDefaultFontFamily:[self defaultSystemFontFamily],
                        NSFontAttributeName:[self defaultSystemFontFamily],
                        NSTextSizeMultiplierDocumentOption:@(16 / 12.0),
                        DTUseiOS6Attributes:@YES,
                        DTDocumentPreserveTrailingSpaces:@(YES),
                        DTDefaultLineBreakMode:@(kCTLineBreakByWordWrapping)};
    }
    return htmlOptions;
}

-(void)setHtmlTitle:(NSString *)htmlTitle {
    
    
    _htmlTitle = htmlTitle;
    if (self.titleBarButtonItem) {
        TiLabel* label = (TiLabel*)[self.titleBarButtonItem customView];
        
        [label setText:[[NSAttributedString alloc] initWithHTMLData:[htmlTitle dataUsingEncoding:NSUTF8StringEncoding] options:[self htmlOptions] documentAttributes:nil]];
        [label sizeToFit];
    }
}

- (UIView *)configuredCustomView {
    NSAssert(NO, @"This is an abstract class, you must use a subclass of CustomActionSheet");
    return nil;
}

#pragma mark - Actions

- (void)showActionSheet {
    self.pickerView = [self configuredCustomView];
    CGRect frame = CGRectMake(0, 0, self.viewSize.width, self.pickerView.frame.size.height);
    
    self.toolbar = [self createToolbar];
    if ([self.toolbar.items count] == 0) {
        [self.toolbar setHidden:YES];
    }
    else {
        frame.size.height += self.toolbar.frame.size.height;
        CGRect pickerFrame = self.pickerView.frame;
        pickerFrame.origin.y = self.toolbar.frame.size.height;
        self.pickerView.frame = pickerFrame;
        if (_htmlTitle) {
            [self setHtmlTitle:_htmlTitle];
        }
        else if (_title) {
            [self setTitle:_title];
        }
    }

    UIView *masterView = [[UIView alloc] initWithFrame:frame];

    // to fix bug, appeared only on iPhone 4 Device: https://github.com/skywinder/ActionSheetPicker-3.0/issues/5
    if (isIPhone4()) {
        masterView.backgroundColor = [UIColor colorWithRed:0.97 green:0.97 blue:0.97 alpha:1.0];
    }
    if (self.toolbar.isHidden == NO) {
        [masterView addSubview:self.toolbar];
    }
    
    //ios7 picker draws a darkened alpha-only region on the first and last 8 pixels horizontally, but blurs the rest of its background.  To make the whole popup appear to be edge-to-edge, we have to add blurring to the remaining left and right edges.
    if ( NSFoundationVersionNumber >= NSFoundationVersionNumber_iOS_7_0 )
    {
        masterView.tintColor = _tinColor?_tinColor:[[UIApplication sharedApplication] keyWindow].tintColor;
        CGFloat top =(self.toolbar.isHidden == YES)?0:self.toolbar.frame.size.height;
        CGRect f = CGRectMake(0,top, masterView.frame.size.width, masterView.frame.size.height - top);
        UIToolbar* insideToolbar = [[UIToolbar alloc] initWithFrame: f];
        insideToolbar.barTintColor = self.toolbar.barTintColor;
        insideToolbar.barStyle = self.toolbar.barStyle;
        [masterView insertSubview: insideToolbar atIndex: 0];
    }

    [masterView addSubview:self.pickerView];
    [self presentPickerForView:masterView];
}

- (void)presentActionSheet:(SWActionSheet *)actionSheet
{
    [super presentActionSheet:actionSheet];
    actionSheet.delegate = self;
}

- (IBAction)actionDone:(id)sender {
    
    if ([_delegate respondsToSelector:@selector(customActionSheet:clickedButtonAtIndex:)]) {
        //Ok button is always last button
        [_delegate customActionSheet:self clickedButtonAtIndex:_nbButtons - 1];
    }
    if (_dismissOnAction) {
        [self dismiss];
    }
}

- (IBAction)actionCancel:(id)sender {
    if ([_delegate respondsToSelector:@selector(customActionSheet:clickedButtonAtIndex:)]) {
        //Ok button is always first button
        [_delegate customActionSheet:self clickedButtonAtIndex:0];
    }

    if (_dismissOnAction) {
        [self dismiss];
    }
}

- (void)dismissWithClickedButtonIndex:(NSUInteger)buttonIndex animated:(BOOL)animated {
#if __IPHONE_4_1 <= __IPHONE_OS_VERSION_MAX_ALLOWED
    if (self.actionSheet)
#else
        if (self.actionSheet && [self.actionSheet isVisible])
#endif
            [self.actionSheet dismissWithClickedButtonIndex:buttonIndex animated:animated];
        else if (self.popOverController && self.popOverController.popoverVisible)
            [self.popOverController dismissPopoverAnimated:animated];
    self.actionSheet = nil;
    self.popOverController = nil;
    self.selfReference = nil;
}

- (void)dismissAnimated:(BOOL)animated {
    [self dismissWithClickedButtonIndex:-1 animated:animated];
}

- (void)dismiss {
    [self dismissAnimated:YES];
}


-(BOOL)isVisible
{
    return self.actionSheet != nil;
}


#pragma mark - Custom Buttons

- (void)addCustomButtonWithTitle:(NSString *)title value:(id)value {
    if (!self.customButtons)
        self.customButtons = [[NSMutableArray alloc] init];
    if (!title)
        title = @"";
    if (!value)
        value = [NSNumber numberWithInt:0];
    NSDictionary *buttonDetails = [[NSDictionary alloc] initWithObjectsAndKeys:title, @"buttonTitle", value, @"buttonValue", nil];
    [self.customButtons addObject:buttonDetails];
}

- (IBAction)customButtonPressed:(id)sender {
    UIBarButtonItem *button = (UIBarButtonItem*)sender;
    NSInteger index = button.tag;
    NSAssert((index >= 0 && index < self.customButtons.count), @"Bad custom button tag: %ld, custom button count: %lu", (long)index, (unsigned long)self.customButtons.count);
    NSLog(@"customButtonPressed not overridden");
}

- (UIToolbar *)createToolbar  {
    _nbButtons = 0;
    CGRect frame = CGRectMake(0, 0, self.viewSize.width, 44);
    UIToolbar *pickerToolbar = [[UIToolbar alloc] initWithFrame:frame];
    switch (_style) {
        case UIActionSheetStyleBlackTranslucent:
            pickerToolbar.barStyle = UIBarStyleBlackTranslucent;
            break;
        default:
            pickerToolbar.barStyle = UIBarStyleDefault;
            break;
    }
//    pickerToolbar.barStyle = OSAtLeast(@"7.0") ? UIBarStyleDefault : UIBarStyleBlackTranslucent;
    if (self.cancelBarButtonItem || self.doneBarButtonItem || self.customButtons || _title || _htmlTitle) {
        NSMutableArray *barItems = [[NSMutableArray alloc] init];
        NSInteger index = 0;
        
        if (NO == self.hideCancel && self.cancelBarButtonItem) {
            [barItems addObject:self.cancelBarButtonItem];
            _nbButtons += 1;
        }
        UIBarButtonItem *flexSpace = [self createButtonWithType:UIBarButtonSystemItemFlexibleSpace target:nil action:nil];
        [barItems addObject:flexSpace];
        
        for (NSDictionary *buttonDetails in self.customButtons) {
            NSString *buttonTitle = [buttonDetails objectForKey:@"buttonTitle"];
          //NSInteger buttonValue = [[buttonDetails objectForKey:@"buttonValue"] intValue];
            UIBarButtonItem *button = [[UIBarButtonItem alloc] initWithTitle:buttonTitle style:UIBarButtonItemStyleBordered target:self action:@selector(customButtonPressed:)];
            button.tag = index;
            [barItems addObject:button];
            index++;
            _nbButtons += 1;
        }
        
        if (_htmlTitle || _title){
            [self setTitleBarButtonItem:[self createToolbarLabel]];
            [barItems addObject:self.titleBarButtonItem];
        }
        [barItems addObject:flexSpace];
        if (self.doneBarButtonItem) {
            [barItems addObject:self.doneBarButtonItem];
            _nbButtons += 1;
        }
        [pickerToolbar setItems:barItems animated:YES];
    }
    return pickerToolbar;
}

- (UIBarButtonItem *)createToolbarLabel {
    TiLabel  *toolBarItemlabel = [[TiLabel alloc]initWithFrame:CGRectMake(0, 0, self.viewSize.width,30)];
    [toolBarItemlabel setTextAlignment:NSTextAlignmentCenter];
    [toolBarItemlabel setTextColor: (NSFoundationVersionNumber > NSFoundationVersionNumber_iOS_6_1) ? [UIColor blackColor] : [UIColor whiteColor]];
    [toolBarItemlabel setFont:[UIFont boldSystemFontOfSize:16]];    
    [toolBarItemlabel setBackgroundColor:[UIColor clearColor]];
    [toolBarItemlabel sizeToFit];
    UIBarButtonItem *buttonLabel = [[UIBarButtonItem alloc]initWithCustomView:toolBarItemlabel];
    return buttonLabel;
}


#pragma mark - Popovers and ActionSheets

- (void)presentForView:(UIView *)aView {
//    self.presentFromRect = aView.frame;
    
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad)
        [self configureAndPresentPopoverForView:aView];
    else
        [self configureAndPresentActionSheetForView:aView];
    self.actionSheet.delegate = self;
}

//- (void)configureAndPresentActionSheetForView:(UIView *)aView {
//    NSString *paddedSheetTitle = nil;
//    CGFloat sheetHeight = aView.frame.size.height;
//    
//    BOOL needsCancelButton = NO;
//    if (!OSAtLeast(@"7.0"))  {
//        sheetHeight -= 11;
//    }
//    else if (_tapOutDismiss){
//        //adding a cancel button makes the alertsheet higher
//        //though we need the cancel
//        needsCancelButton = YES;
//        sheetHeight -= 26;
//    }
//    _actionSheet = [[UIActionSheet alloc] initWithTitle:paddedSheetTitle delegate:self cancelButtonTitle:needsCancelButton?@"":nil destructiveButtonTitle:nil otherButtonTitles:nil];
////    if (needsCancelButton) {
////        for (UIView* view in _actionSheet.subviews) {
////            if ([view isKindOfClass:[UIButton class]])
////                [view setHidden:YES];
////            NSLog(@"test %@", view);
////        }
////    }
//    [_actionSheet setActionSheetStyle:self.style];
//    [_actionSheet addSubview:aView];
//    [self presentActionSheet:_actionSheet];
//    
//    // Use beginAnimations for a smoother popup animation, otherwise the UIActionSheet pops into view
//    [UIView beginAnimations:nil context:nil];
//    _actionSheet.bounds = CGRectMake(0, 0, self.viewSize.width, 2*sheetHeight);
//    [UIView commitAnimations];    
//}

// For detecting taps outside of the alert view
-(void)tapOut:(UIGestureRecognizer *)gestureRecognizer {
    CGPoint p = [gestureRecognizer locationInView:self.pickerView.superview];
    if (p.y < 0) { // They tapped outside
        [self dismissWithClickedButtonIndex:self.cancelIndex animated:YES];
    }
}


- (void)showFromToolbar:(UIToolbar *)view {
    self.animated = YES;
    self.containerView = view;
    [self showActionSheet];
}
- (void)showFromTabBar:(UITabBar *)view {
    self.animated = YES;
    self.containerView = view;
    [self showActionSheet];
}

- (void)showFromBarButtonItem:(UIBarButtonItem *)item animated:(BOOL)animated {
    self.animated = animated;
    self.barButtonItem = item;
    [self showActionSheet];
}

- (void)showFromRect:(CGRect)rect inView:(UIView *)view animated:(BOOL)animated {
    self.animated = animated;
    self.containerView = view;
    self.presentFromRect = rect;
    [self showActionSheet];
}
- (void)showInView:(UIView *)view {
    self.animated = YES;
    self.containerView = view;
    [self showActionSheet];
}

#pragma mark - SWActionSheetDelegate
- (void)actionSheet:(SWActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex
{
    if (buttonIndex >= 0) {
        if ([_delegate respondsToSelector:@selector(customActionSheet:clickedButtonAtIndex:)]) {
            [_delegate customActionSheet:self clickedButtonAtIndex:buttonIndex];
        }
    }
    if ([_delegate respondsToSelector:@selector(customActionSheet:didDismissWithButtonIndex:)]) {
        [_delegate customActionSheet:self didDismissWithButtonIndex:buttonIndex];
    }
}

- (void)actionSheetCancel:(SWActionSheet *)actionSheet {
    if ([_delegate respondsToSelector:@selector(customActionSheet:actionSheetCancel:)]) {
        [_delegate customActionSheetCancel:self];
    }
}

- (void)notifyTarget:(id)target didSucceedWithAction:(SEL)successAction origin:(id)origin
{
    if ( target && successAction && [target respondsToSelector:successAction] )
    {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
        [target performSelector:successAction withObject:origin];
#pragma clang diagnostic pop
    }
}

- (void)didPresentActionSheet:(SWActionSheet *)actionSheet {
    if (_tapOutDismiss) {
        // Capture taps outside the bounds of this alert view
        UITapGestureRecognizer *tap = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(tapOut:)];
        tap.cancelsTouchesInView = NO; // So that legit taps on the table bubble up to the tableview
        [actionSheet.superview addGestureRecognizer:tap];
    }
}


@end

