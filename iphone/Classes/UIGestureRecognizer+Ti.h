//
//  UIGestureRecognizer+Ti.h
//  MapMe
//
//  Created by Martin Guillon on 16/07/2015.
//
//

#import <UIKit/UIKit.h>

@class TiUIView;
@interface UIGestureRecognizer (StartTouchedView)
- (void)setStartTouchedView:(TiUIView *)view;
- (TiUIView*)startTouchedView;
- (void)setTiGesture:(BOOL)value;
- (BOOL)tiGesture;
@end