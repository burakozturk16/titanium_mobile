//
//  UIGestureRecognizer+Ti.m
//  MapMe
//
//  Created by Martin Guillon on 16/07/2015.
//
//

#import "UIGestureRecognizer+Ti.h"
#import "TiUIView.h"
#import <objc/runtime.h>
#import <UIKit/UIGestureRecognizerSubclass.h>
#import "JRSwizzle.h"

static NSString * const kTiGestureStartTouchedView = @"kTiGestureStartTouchedView";
static NSString * const kIsTiGesture = @"kIsTiGesture";

@implementation UIGestureRecognizer (StartTouchedView)

+ (void) swizzle
{
    [UIGestureRecognizer jr_swizzleMethod:@selector(setState:) withMethod:@selector(setStateCustom:) error:nil];
}

- (void)setTiGesture:(BOOL)value
{
    objc_setAssociatedObject(self, kIsTiGesture, @(value), OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (BOOL)tiGesture
{
    id obj = objc_getAssociatedObject(self, kIsTiGesture);
    if (obj) {
        return [obj boolValue];
    }
    return false;
}

- (void)setStartTouchedView:(TiUIView *)view
{
    objc_setAssociatedObject(self, kTiGestureStartTouchedView, view, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
}

- (TiUIView*)startTouchedView
{
    return objc_getAssociatedObject(self, kTiGestureStartTouchedView);
}

-(void)setStateCustom:(UIGestureRecognizerState)state
{
    //WARNING: this is the swizzle trick, will actually call [UIGestureRecognizer setState:]
    [self setStateCustom:state];
    BOOL tiGesture = [self tiGesture];
    if (!tiGesture) {
        return;
    }
    if (state == UIGestureRecognizerStateRecognized) {
        UIView* theView = [self startTouchedView];
        [(TiUIView*)theView processTouchesCancelled:nil withEvent:nil];
    }
}
@end

