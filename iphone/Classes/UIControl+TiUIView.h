//
//  UIControl+TiUIView.h
//  Titanium
//
//  Created by Martin Guillon on 20/09/13.
//
//

#import <UIKit/UIKit.h>

@class TiUIView;
@interface UIControl (TiUIView)
+ (void) swizzle;
- (void)setTiUIView:(TiUIView *)view;
@end
