//
//  ShapeModule.h
//  Titanium
//
//  Created by Martin Guillon on 10/08/13.
//
//

#import "TiModule.h"
#ifdef USE_TI_SHAPEANIMATION
#define USE_TI_UIANIMATION
#endif


@interface ShapeModule : TiModule


@property(readonly,nonatomic) NSNumber *CAP_BUTT;
@property(readonly,nonatomic) NSNumber *CAP_ROUND;
@property(readonly,nonatomic) NSNumber *CAP_SQUARE;
@property(readonly,nonatomic) NSNumber *JOIN_MITER;
@property(readonly,nonatomic) NSNumber *JOIN_ROUND;
@property(readonly,nonatomic) NSNumber *JOIN_BEVEL;

@property(readonly,nonatomic) NSNumber *HORIZONTAL;
@property(readonly,nonatomic) NSNumber *VERTICAL;

@property(readonly,nonatomic) NSNumber *CW;
@property(readonly,nonatomic) NSNumber *CCW;

@property(readonly,nonatomic) NSNumber *TOP_MIDDLE;
@property(readonly,nonatomic) NSNumber *LEFT_TOP;
@property(readonly,nonatomic) NSNumber *LEFT_MIDDLE;
@property(readonly,nonatomic) NSNumber *LEFT_BOTTOM;
@property(readonly,nonatomic) NSNumber *RIGHT_TOP;
@property(readonly,nonatomic) NSNumber *RIGHT_MIDDLE;
@property(readonly,nonatomic) NSNumber *RIGHT_BOTTOM;
@property(readonly,nonatomic) NSNumber *BOTTOM_MIDDLE;
@property(readonly,nonatomic) NSNumber *CENTER;


@property(readonly,nonatomic) NSNumber *OP_RECT;
@property(readonly,nonatomic) NSNumber *OP_ROUNDRECT;
@property(readonly,nonatomic) NSNumber *OP_CIRCLE;
@property(readonly,nonatomic) NSNumber *OP_ELLIPSE;
@property(readonly,nonatomic) NSNumber *OP_POINTS;
@property(readonly,nonatomic) NSNumber *OP_ARC;
@end
