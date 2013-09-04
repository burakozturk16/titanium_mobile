//
//  TiUIHelper.h
//  Titanium
//
//  Created by Martin Guillon on 23/08/13.
//
//

#import <QuartzCore/QuartzCore.h>
typedef struct ShadowDef{
    CGFloat opacity;
    CGColorRef color;
    CGSize offset;
    CGFloat radius;
} ShadowDef;

@interface TiUIHelper : NSObject

+(void)applyShadow:(NSDictionary*)args toLayer:(CALayer *)layer;
+(ShadowDef)getShadow:(NSDictionary*)args;

@end
