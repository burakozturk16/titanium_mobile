//
//  UIBezierPath+Additions.m
//  Titanium
//
//  Created by Martin Guillon on 07/09/13.
//
//

#import "UIBezierPath+Additions.h"


@implementation BezierPoint
@synthesize curvePoint1, curvePoint2, curvePoint1Assigned, curvePoint2Assigned;


-(void)setCurvePoint1:(CGPoint)value
{
    curvePoint1 = value;
    curvePoint1Assigned = YES;
}

-(void)setCurvePoint2:(CGPoint)value
{
    curvePoint2 = value;
    curvePoint2Assigned = YES;
}

- (id)copyWithZone:(NSZone *)zone {
    BezierPoint* copy = [[BezierPoint alloc] init];
    copy.point = self.point;
    if(self.curvePoint1Assigned) {
        copy.curvePoint1 = self.curvePoint1;
    }
    if(self.curvePoint2Assigned) {
        copy.curvePoint2 = self.curvePoint2;
    }
    return copy;
}

@end

@implementation UIBezierPath (Additions)

- (void)addRoundedRect:(CGRect)rect byRoundingCorners:(UIRectCorner)corners cornerRadii:(CGSize)cornerRadii
{
    const CGPoint topLeft = rect.origin;
    const CGPoint topRight = CGPointMake(CGRectGetMaxX(rect), CGRectGetMinY(rect));
    const CGPoint bottomRight = CGPointMake(CGRectGetMaxX(rect), CGRectGetMaxY(rect));
    const CGPoint bottomLeft = CGPointMake(CGRectGetMinX(rect), CGRectGetMaxY(rect));
    
    if (CGSizeEqualToSize(cornerRadii, CGSizeZero)) {
        [self moveToPoint:topLeft];
        [self addLineToPoint:topRight];
        [self addLineToPoint:bottomRight];
        [self addLineToPoint:bottomLeft];
        [self addLineToPoint:topLeft];
        [self closePath];
        return;
   }
    
    if (corners & UIRectCornerTopLeft) {
        [self moveToPoint:CGPointMake(topLeft.x+cornerRadii.width, topLeft.y)];
    } else {
        [self moveToPoint:topLeft];
    }
    
    if (corners & UIRectCornerTopRight) {
        [self addLineToPoint:CGPointMake(topRight.x-cornerRadii.width, topRight.y)];
        [self addCurveToPoint:CGPointMake(topRight.x, topRight.y+cornerRadii.height) controlPoint1:topRight controlPoint2:CGPointMake(topRight.x, topRight.y+cornerRadii.height)];
    } else {
        [self addLineToPoint:topRight];
    }
    
    if (corners & UIRectCornerBottomRight) {
        [self addLineToPoint:CGPointMake(bottomRight.x, bottomRight.y-cornerRadii.height)];
        [self addCurveToPoint:CGPointMake(bottomRight.x-cornerRadii.width, bottomRight.y) controlPoint1:bottomRight controlPoint2:CGPointMake(bottomRight.x-cornerRadii.width, bottomRight.y)];
    } else {
        [self addLineToPoint:bottomRight];
    }
    
    if (corners & UIRectCornerBottomLeft) {
        [self addLineToPoint:CGPointMake(bottomLeft.x+cornerRadii.width, bottomLeft.y)];
        [self addCurveToPoint:CGPointMake(bottomLeft.x, bottomLeft.y-cornerRadii.height) controlPoint1:bottomLeft controlPoint2:CGPointMake(bottomLeft.x, bottomLeft.y-cornerRadii.height)];
    } else {
        [self addLineToPoint:bottomLeft];
    }
    
    if (corners & UIRectCornerTopLeft) {
        [self addLineToPoint:CGPointMake(topLeft.x, topLeft.y+cornerRadii.height)];
        [self addCurveToPoint:CGPointMake(topLeft.x+cornerRadii.width, topLeft.y) controlPoint1:topLeft controlPoint2:CGPointMake(topLeft.x+cornerRadii.width, topLeft.y)];
    } else {
        [self addLineToPoint:topLeft];
    }
    [self closePath];
}

+ (UIBezierPath *)bezierPathWithCustomRoundedRect:(CGRect)rect byRoundingCorners:(UIRectCorner)corners cornerRadii:(CGSize)cornerRadii
{
    UIBezierPath* path = [[UIBezierPath alloc] init];
    [path addRoundedRect:rect byRoundingCorners:corners cornerRadii:cornerRadii];
    
    return [path autorelease];
}

- (void)addBezierPoints:(NSArray*)points {
    BOOL firstOne = YES;
    for (BezierPoint* bezierPoint in points) {
        if (firstOne) {
            firstOne = NO;
            CGPoint point = bezierPoint.point;
            [self moveToPoint:point];
        }
        else {
            if (bezierPoint.curvePoint2Assigned && bezierPoint.curvePoint1Assigned) {
                [self addCurveToPoint:bezierPoint.point controlPoint1:bezierPoint.curvePoint1 controlPoint2:bezierPoint.curvePoint2];
            }
            else if (bezierPoint.curvePoint1Assigned) {
                [self addQuadCurveToPoint:bezierPoint.point controlPoint:bezierPoint.curvePoint1];
            }
            else {
                [self addLineToPoint:bezierPoint.point];
            }
        }
    }
}

+ (UIBezierPath *)bezierPathWithPoints:(NSArray*)points {
    UIBezierPath* path = [[UIBezierPath alloc] init];
    [path addBezierPoints:points];
    return [path autorelease];
}

- (void)addPieSliceCenter:(CGPoint)center radius:(CGFloat)radius innerRadius:(CGFloat)innerRadius startAngle:(CGFloat)startAngle endAngle:(CGFloat)endAngle
{
    BOOL clockwise = endAngle > startAngle;
    
    if (innerRadius == 0.0f) {
        [self moveToPoint:center];
        [self addArcWithCenter:center radius:radius startAngle:startAngle endAngle:endAngle clockwise:clockwise];
    }
    else {
        [self addArcWithCenter:center radius:innerRadius startAngle:startAngle endAngle:endAngle clockwise:clockwise];
        [self addArcWithCenter:center radius:radius startAngle:endAngle endAngle:startAngle clockwise:!clockwise];
    }
    [self closePath];
}

+ (UIBezierPath *)bezierPathWithPieSliceCenter:(CGPoint)center radius:(CGFloat)radius innerRadius:(CGFloat)innerRadius startAngle:(CGFloat)startAngle endAngle:(CGFloat)endAngle
{
    UIBezierPath* path = [[UIBezierPath alloc] init];
    [path addPieSliceCenter:center radius:radius innerRadius:innerRadius startAngle:startAngle endAngle:endAngle];
    return [path autorelease];
}
@end
