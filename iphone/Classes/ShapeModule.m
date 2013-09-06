//
//  ShapeModule.m
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "ShapeModule.h"
#import "ShapeProxy.h"

@implementation ShapeModule


MAKE_SYSTEM_PROP(BATTERY_STATE_UNKNOWN,UIDeviceBatteryStateUnknown);


MAKE_SYSTEM_PROP(CAP_BUTT,kCGLineCapButt);
MAKE_SYSTEM_PROP(CAP_ROUND,kCGLineCapRound);
MAKE_SYSTEM_PROP(CAP_SQUARE,kCGLineCapSquare);
MAKE_SYSTEM_PROP(JOIN_MITER,kCGLineJoinMiter);
MAKE_SYSTEM_PROP(JOIN_ROUND,kCGLineJoinRound);
MAKE_SYSTEM_PROP(JOIN_BEVEL,kCGLineJoinBevel);

MAKE_SYSTEM_PROP(HORIZONTAL,0);
MAKE_SYSTEM_PROP(VERTICAL,1);

MAKE_SYSTEM_PROP(CW,YES);
MAKE_SYSTEM_PROP(CCW,NO);

MAKE_SYSTEM_PROP(TOP_MIDDLE,ShapeAnchorTopMiddle);
MAKE_SYSTEM_PROP(LEFT_TOP,ShapeAnchorLeftTop);
MAKE_SYSTEM_PROP(LEFT_MIDDLE,ShapeAnchorLeftMiddle);
MAKE_SYSTEM_PROP(LEFT_BOTTOM,ShapeAnchorLeftBottom);
MAKE_SYSTEM_PROP(RIGHT_TOP,ShapeAnchorRightTop);
MAKE_SYSTEM_PROP(RIGHT_MIDDLE,ShapeAnchorRightMiddle);
MAKE_SYSTEM_PROP(RIGHT_BOTTOM,ShapeAnchorRightBottom);
MAKE_SYSTEM_PROP(BOTTOM_MIDDLE,ShapeAnchorBottomMiddle);
MAKE_SYSTEM_PROP(CENTER,ShapeAnchorCenter);

MAKE_SYSTEM_PROP(OP_RECT,ShapeOpRect);
MAKE_SYSTEM_PROP(OP_ROUNDRECT,ShapeOpRoundedRect);
MAKE_SYSTEM_PROP(OP_CIRCLE,ShapeOpCircle);
MAKE_SYSTEM_PROP(OP_ELLIPSE,ShapeOpEllipse);
MAKE_SYSTEM_PROP(OP_POINTS,ShapeOpPoints);
MAKE_SYSTEM_PROP(OP_ARC,ShapeOpArc);

#ifdef USE_TI_SHAPEANIMATION
-(id)createAnimation:(id)args
{
	if (args!=nil && [args isKindOfClass:[NSArray class]])
	{
		id properties = [args objectAtIndex:0];
		id callback = [args count] > 1 ? [args objectAtIndex:1] : nil;
		ENSURE_TYPE_OR_NIL(callback,KrollCallback);
		if ([properties isKindOfClass:[NSDictionary class]])
		{
			TiShapeAnimation *a = [[[TiAnimation alloc] initWithDictionary:properties context:[self pageContext] callback:callback] autorelease];
			return a;
		}
	}
	return [[[TiShapeAnimation alloc] _initWithPageContext:[self executionContext]] autorelease];
}
#endif

-(id)createShape:(id)args
{
	return [[[ShapeProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

@end
