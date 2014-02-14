//
//  TiUIHelper.h
//  Titanium
//
//  Created by Martin Guillon on 23/08/13.
//
//

#import <QuartzCore/QuartzCore.h>
@interface TiShadow : NSShadow
@end
@interface TiUIHelper : NSObject

+(void)applyShadow:(NSDictionary*)args toLayer:(CALayer *)layer;
+(TiShadow*)getShadow:(NSDictionary*)args;

@end
