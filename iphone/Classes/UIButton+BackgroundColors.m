/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "UIButton+BackgroundColors.h"
#import <objc/runtime.h>
#import "JRSwizzle.h"

//NSString * const kDefaultColorKey = @"kDefaultBackgroundColorKey";
//NSString * const kHighlightedColorKey = @"kBackgroundHighlightedColorKey";
//NSString * const kSelectedColorKey = @"kBackgroundSelectedColorKey";
//NSString * const kDisabledColorKey = @"kBackgroundDisabledColorKey";
//
//@implementation UIButton (BackgroundColors)
//
//+ (void) swizzle
//{
//	[UIButton jr_swizzleMethod:@selector(setSelected:) withMethod:@selector(setSelectedCustom:) error:nil];
//	[UIButton jr_swizzleMethod:@selector(setHighlighted:) withMethod:@selector(setHighlightedCustom:) error:nil];
//	[UIButton jr_swizzleMethod:@selector(setEnabled:) withMethod:@selector(setEnabledCustom:) error:nil];
//}
//
//- (void)setBackgroundDefaultColor:(UIColor *)color
//{
//	objc_setAssociatedObject(self, kDefaultColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
//    [super setBackgroundColor:color];
//}
//
//- (void)setBackgroundHighlightedColor:(UIColor *)color
//{
//	objc_setAssociatedObject(self, kHighlightedColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
//}
//
//- (void)setBackgroundSelectedColor:(UIColor *)color
//{
//	objc_setAssociatedObject(self, kSelectedColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
//}
//
//- (void)setBackgroundDisabledColor:(UIColor *)color
//{
//	objc_setAssociatedObject(self, kDisabledColorKey, color, OBJC_ASSOCIATION_RETAIN_NONATOMIC);
//}
//
//- (UIColor*)backgroundHighlightedColor
//{
//	return objc_getAssociatedObject(self, kHighlightedColorKey);
//}
//
//- (UIColor*)backgroundSelectedColor
//{
//	return objc_getAssociatedObject(self, kSelectedColorKey);
//}
//
//- (UIColor*)backgroundDisabledColor
//{
//	return objc_getAssociatedObject(self, kDisabledColorKey);
//}
//
//- (UIColor*)backgroundDefaultColor
//{
//	return objc_getAssociatedObject(self, kDefaultColorKey);
//}
//
//-(void)updateBackgroundColor
//{
//    UIColor* newColor = self.backgroundColor;
//    if (self.enabled)
//    {
//        if(self.highlighted)
//        {
//            if([self backgroundHighlightedColor])
//                newColor = [self backgroundHighlightedColor];
//            else if([self backgroundSelectedColor])
//                newColor = [self backgroundSelectedColor];
//        }
//        else if(self.selected && [self backgroundSelectedColor])
//            newColor = [self backgroundSelectedColor];
//        else if([self backgroundDefaultColor])
//            newColor = [self backgroundDefaultColor];
//        else
//            newColor = [UIColor clearColor];
//    }
//    else
//    {
//        if ([self backgroundDisabledColor])
//            newColor = [self backgroundDisabledColor];
//        else if([self backgroundDefaultColor])
//            newColor = [self backgroundDefaultColor];
//        else
//            newColor = [UIColor clearColor];
//    }
//    if (![newColor isEqual:self.backgroundColor])
//        self.backgroundColor = newColor;
//    
//}
//
//-(void)setSelectedCustom:(BOOL)selected
//{
//    //WARNING: this is the swizzle trick, will actually call [UIButton setSelected:]
//    [self setSelectedCustom:selected];
//    [self updateBackgroundColor];
//}
//
//-(void)setHighlightedCustom:(BOOL)highlighted
//{
//    
//    //WARNING: this is the swizzle trick, will actually call [UIButton setHighlighted:]
//    [self setHighlightedCustom:highlighted];
//    [self updateBackgroundColor];
//}
//
//-(void)setEnabledCustom:(BOOL)enabled
//{
//    //WARNING: this is the swizzle trick, will actually call [UIButton setEnabled:]
//    [self setEnabledCustom:enabled];
//    [self updateBackgroundColor];
//}
//
//@end
