//
//  TiCellBackroundView.m
//  Titanium
//
//  Created by Martin Guillon on 04/09/13.
//
//

#import "TiCellBackgroundView.h"
#import "TiSelectableBackgroundLayer.h"


@interface TiCellBackgroundView ()
{
    CGFloat cornersRadius;
    UIRectCorner roundedCorners;
}

@end

@implementation TiCellBackgroundView

+ (Class)layerClass
{
    return [TiSelectableBackgroundLayer class];
}

- (id)initWithFrame:(CGRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code
        self.autoresizingMask = UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight;
        self.layer.masksToBounds = YES;
        [self selectableLayer].animateTransition = NO;
    }
    return self;
}


-(void)setFrame:(CGRect)frame
{
    [super setFrame:frame];
    if(self.layer.mask != nil) {
        [self.layer.mask setFrame:self.bounds];
    }
    [self updateMaskOnLayer:self.layer];
}


- (void)updateMaskOnLayer:(CALayer*)layer
{
    if (layer.mask == nil || CGSizeEqualToSize(self.bounds.size, CGSizeZero)) return;
    
    UIBezierPath *maskPath = [UIBezierPath bezierPathWithRoundedRect:layer.bounds
                                                   byRoundingCorners:roundedCorners
                                                         cornerRadii:CGSizeMake(cornersRadius, cornersRadius)];
    ((CAShapeLayer*)layer.mask).path = maskPath.CGPath;
}

- (void)setRoundedRadius:(CGFloat)radius inCorners:(UIRectCorner)corners
{
    if (cornersRadius == radius && roundedCorners == corners) return;
    if (corners == -10) {
        if (self.layer.mask != nil) self.layer.mask = nil;
    }
    else {
        if (self.layer.mask == nil) {
            CAShapeLayer* maskLayer = [[CAShapeLayer alloc] init];
            maskLayer.frame = self.bounds;
            self.layer.mask = maskLayer;
            [maskLayer release];
        }
        cornersRadius = radius;
        roundedCorners = corners;
        [self updateMaskOnLayer:self.layer];
    }
}

-(TiSelectableBackgroundLayer*)selectableLayer
{
    return (TiSelectableBackgroundLayer*)self.layer;
}

@end
