//
//  TiUIHelper.m
//  Titanium
//
//  Created by Martin Guillon on 23/08/13.
//
//

#import "TiUIHelper.h"
#import "TiBase.h"
#import "TiUtils.h"
@implementation TiShadow
@end


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
    TiShadow* data = [TiUIHelper getShadow:args];
    layer.shadowOffset = data.shadowOffset;
    layer.shadowOpacity = 1.0f;
    layer.shadowColor = ((UIColor*)data.shadowColor).CGColor;
    layer.shadowRadius = data.shadowBlurRadius;
}

+(TiShadow*)getShadow:(NSDictionary*)args;
{
    TiShadow* result = [[TiShadow alloc] init];
    if ([args objectForKey:@"offset"]) {
        NSDictionary* offsetDict  = [args objectForKey:@"offset"];
        CGSize offset = CGSizeZero;
        if ([offsetDict objectForKey:@"x"]) {
            offset.width = [TiUtils floatValue:[offsetDict objectForKey:@"x"]];
        }
        if ([offsetDict objectForKey:@"y"]) {
            offset.height = [TiUtils floatValue:[offsetDict objectForKey:@"y"]];
        }
        result.shadowOffset = offset;
    }
    else {
        result.shadowOffset = CGSizeZero;
    }
    
//    if ([args objectForKey:@"opacity"]) {
//        result.opacity = [TiUtils floatValue:[args objectForKey:@"opacity"]];
//    }
//    else {
//        result.opacity = 1.0f;
//    }
    
    if ([args objectForKey:@"color"]) {
        result.shadowColor = [[TiUtils colorValue:[args objectForKey:@"color"]] _color];
    }
    else {
        result.shadowColor = [UIColor blackColor];
    }
    if ([args objectForKey:@"radius"]) {
        result.shadowBlurRadius = [TiUtils floatValue:[args objectForKey:@"radius"]];
    }
    else {
        result.shadowBlurRadius = 3.0f; //same as Android
    }
    return [result autorelease];
}

@end
