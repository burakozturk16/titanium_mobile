//
//  TiUIBlendModeProxy.m
//  Titanium
//
//  Created by Martin Guillon on 11/10/13.
//
//

#import "TiUIBlendModeProxy.h"

@implementation TiUIBlendModeProxy
MAKE_SYSTEM_PROP(DARKEN,kCGBlendModeDarken);
MAKE_SYSTEM_PROP(LIGHTEN,kCGBlendModeLighten);
MAKE_SYSTEM_PROP(MULTIPLY,kCGBlendModeMultiply);
MAKE_SYSTEM_PROP(ADD,kCGBlendModeNormal);
MAKE_SYSTEM_PROP(SCREEN,kCGBlendModeScreen);
MAKE_SYSTEM_PROP(CLEAR,kCGBlendModeDarken);
MAKE_SYSTEM_PROP(DST,kCGBlendModeCopy);
MAKE_SYSTEM_PROP(SRC,kCGBlendModeCopy);
MAKE_SYSTEM_PROP(DST_ATOP,kCGBlendModeDestinationAtop);
MAKE_SYSTEM_PROP(DST_IN,kCGBlendModeDestinationIn);
MAKE_SYSTEM_PROP(DST_OUT,kCGBlendModeDestinationOut);
MAKE_SYSTEM_PROP(DST_OVER,kCGBlendModeDestinationOver);
MAKE_SYSTEM_PROP(SRC_ATOP,kCGBlendModeSourceAtop);
MAKE_SYSTEM_PROP(SRC_IN,kCGBlendModeSourceIn);
MAKE_SYSTEM_PROP(SRC_OUT,kCGBlendModeSourceOut);
MAKE_SYSTEM_PROP(SRC_OVER,kCGBlendModeDestinationOver);
MAKE_SYSTEM_PROP(OVERLAY,kCGBlendModeDarken);
MAKE_SYSTEM_PROP(XOR,kCGBlendModeXOR);
@end
