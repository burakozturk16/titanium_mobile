//
//  UIBezierPath+Additions.h
//  Titanium
//
//  Created by Martin Guillon on 07/09/13.
//
//

#import <UIKit/UIKit.h>

@interface BezierPoint : NSObject<NSCopying>
@property (nonatomic, readwrite, assign) CGPoint point;
@property (nonatomic, readwrite, assign) CGPoint curvePoint1;
@property (nonatomic, readwrite, assign) CGPoint curvePoint2;
@property (nonatomic, readonly) BOOL curvePoint2Assigned;
@property (nonatomic, readonly) BOOL curvePoint1Assigned;

@end

@interface UIBezierPath (Additions)
- (void)addRoundedRect:(CGRect)rect byRoundingCorners:(UIRectCorner)corners cornerRadii:(CGSize)cornerRadii;
- (void)addPieSliceCenter:(CGPoint)center radius:(CGFloat)radius innerRadius:(CGFloat)innerRadius startAngle:(CGFloat)startAngle endAngle:(CGFloat)endAngle;
- (void)addBezierPoints:(NSArray*)points;


+ (UIBezierPath *)bezierPathWithCustomRoundedRect:(CGRect)rect byRoundingCorners:(UIRectCorner)corners cornerRadii:(CGSize)cornerRadii;
+ (UIBezierPath *)bezierPathWithPoints:(NSArray*)points;
+ (UIBezierPath *)bezierPathWithPieSliceCenter:(CGPoint)center radius:(CGFloat)radius innerRadius:(CGFloat)innerRadius startAngle:(CGFloat)startAngle endAngle:(CGFloat)endAngle;
@end
