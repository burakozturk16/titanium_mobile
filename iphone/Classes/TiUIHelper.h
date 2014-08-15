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
@class TiViewProxy;
@interface TiUIHelper : NSObject

+(void)applyShadow:(NSDictionary*)args toLayer:(CALayer *)layer;
+(TiShadow*)getShadow:(NSDictionary*)args;
+(TiViewProxy*)findViewProxyWithBindIdUnder:(UIView *)view containingPoint:(CGPoint)point;


@end
