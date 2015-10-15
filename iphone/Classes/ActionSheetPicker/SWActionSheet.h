//
// Created by Petr Korolev on 11/08/14.
//


#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

@class SWActionSheet;
@protocol SWActionSheetDelegate <NSObject>

- (void)actionSheet:(SWActionSheet *)actionSheet didDismissWithButtonIndex:(NSInteger)buttonIndex;
- (void)actionSheetCancel:(SWActionSheet *)actionSheet;
- (void)didPresentActionSheet:(SWActionSheet *)actionSheet;

@end

@interface SWActionSheet : UIView
@property(nonatomic, strong) UIView *bgView;
@property (nonatomic, strong) id<SWActionSheetDelegate> delegate;

- (void)dismissWithClickedButtonIndex:(NSInteger)i animated:(BOOL)animated;

- (void)showFromBarButtonItem:(UIBarButtonItem *)item animated:(BOOL)animated;

- (id)initWithView:(UIView *)view;

- (void)showInContainerView;
@end