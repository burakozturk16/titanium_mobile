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
    ShadowDef data = [TiUIHelper getShadow:args];
    layer.shadowOffset = data.offset;
    layer.shadowOpacity = data.opacity;
    layer.shadowColor = data.color;
    layer.shadowRadius = data.radius;
}

+(ShadowDef)getShadow:(NSDictionary*)args;
{
    ShadowDef result;
    if ([args objectForKey:@"offset"]) {
        NSDictionary* offsetDict  = [args objectForKey:@"offset"];
        CGSize offset = CGSizeZero;
        if ([offsetDict objectForKey:@"x"]) {
            offset.width = [TiUtils floatValue:[offsetDict objectForKey:@"x"]];
        }
        if ([offsetDict objectForKey:@"y"]) {
            offset.height = [TiUtils floatValue:[offsetDict objectForKey:@"y"]];
        }
        result.offset = offset;
    }
    else {
        result.offset = CGSizeZero;
    }
    
    if ([args objectForKey:@"opacity"]) {
        result.opacity = [TiUtils floatValue:[args objectForKey:@"opacity"]];
    }
    else {
        result.opacity = 1.0f;
    }
    
    if ([args objectForKey:@"color"]) {
        result.color = [[TiUtils colorValue:[args objectForKey:@"color"]] _color].CGColor;
    }
    else {
        result.color = [UIColor blackColor].CGColor;
    }
    if ([args objectForKey:@"radius"]) {
        result.radius = [TiUtils floatValue:[args objectForKey:@"radius"]];
    }
    else {
        result.radius = 3.0f; //same as Android
    }
    return result;
}
@end
