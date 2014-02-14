//
//  UIControl+TiUIView.m
//  Titanium
//
//  Created by Martin Guillon on 20/09/13.
//
//

#import "UIControl+TiUIView.h"
#import "JRSwizzle.h"
#import "TiUIView.h"
#import <objc/runtime.h>

NSString * const kTiUIViewKey = @"kTiView";
@implementation UIControl (TiUIView)

+ (void) swizzle
{
	[UIControl jr_swizzleMethod:@selector(setSelected:) withMethod:@selector(setSelectedCustom:) error:nil];
	[UIControl jr_swizzleMethod:@selector(setHighlighted:) withMethod:@selector(setHighlightedCustom:) error:nil];
	[UIControl jr_swizzleMethod:@selector(setEnabled:) withMethod:@selector(setEnabledCustom:) error:nil];
}

- (void)setTiUIView:(TiUIView *)view
{
	objc_setAssociatedObject(self, kTiUIViewKey, view, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
    if (view != nil) {
        view.animateBgdTransition = YES;
    }
}

- (TiUIView*)tiUIView
{
	return objc_getAssociatedObject(self, kTiUIViewKey);
}

-(void)setSelected:(BOOL)highlighted animated:(BOOL)animated
{
    [self setSelected:highlighted];
}

-(void)setSelectedCustom:(BOOL)selected
{
    //WARNING: this is the swizzle trick, will actually call [UIControl setSelected:]
    [self setSelectedCustom:selected];
    TiUIView* tiView = [self tiUIView];
    if (tiView)
        [tiView setSelected:selected];
}

-(void)setHighlighted:(BOOL)highlighted animated:(BOOL)animated
{
    [self setHighlighted:highlighted];
}

-(void)setHighlightedCustom:(BOOL)highlighted
{
    
    //WARNING: this is the swizzle trick, will actually call [UIControl setHighlighted:]
    [self setHighlightedCustom:highlighted];
    TiUIView* tiView = [self tiUIView];
    if (tiView)
        [tiView setHighlighted:highlighted];
}

-(void)setEnabledCustom:(BOOL)highlighted
{
    
    //WARNING: this is the swizzle trick, will actually call [UIControl setHighlighted:]
    [self setEnabledCustom:highlighted];
    TiUIView* tiView = [self tiUIView];
    if (tiView)
        [tiView setBgState:UIControlStateNormal];
}
@end
