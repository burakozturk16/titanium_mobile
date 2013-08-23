//
//  TiUIHelper.m
//  Titanium
//
//  Created by Martin Guillon on 23/08/13.
//
//

#import "TiUIHelper.h"
#import "TiUtils.h"

@implementation TiUIHelper

+(void)applyShadow:(NSDictionary*)args toLayer:(CALayer *)layer
{
    if (args == nil) {
        layer.masksToBounds = YES;
        layer.shadowOpacity = 0.0f;
        layer.shouldRasterize = NO;
        return;
    }
    layer.masksToBounds = NO;
    layer.shouldRasterize = YES;
    layer.rasterizationScale = [[UIScreen mainScreen] scale];
    if ([args objectForKey:@"offset"]) {
        NSDictionary* offsetDict  = [args objectForKey:@"offset"];
        CGSize offset = CGSizeZero;
        if ([offsetDict objectForKey:@"x"]) {
            offset.width = [TiUtils floatValue:[offsetDict objectForKey:@"x"]];
        }
        if ([offsetDict objectForKey:@"y"]) {
            offset.height = [TiUtils floatValue:[offsetDict objectForKey:@"y"]];
        }
        layer.shadowOffset = offset;
    }
    
    if ([args objectForKey:@"opacity"]) {
        layer.shadowOpacity = [TiUtils floatValue:[args objectForKey:@"opacity"]];
    }
    else {
        layer.shadowOpacity = 1.0f;
    }
    
    if ([args objectForKey:@"color"]) {
        layer.shadowColor = [[TiUtils colorValue:[args objectForKey:@"color"]] _color].CGColor;
    }
    else {
        layer.shadowColor = [UIColor blackColor].CGColor;
    }
    if ([args objectForKey:@"radius"]) {
        layer.shadowRadius = [TiUtils floatValue:[args objectForKey:@"radius"]];
    }
}

@end
