//
//  TiUITransitionStyleProxy.m
//  Titanium
//
//  Created by Martin Guillon on 22/09/13.
//
//

#ifdef USE_TI_UITRANSITIONSTYLE
#import "TiUITransitionStyleProxy.h"
#import "TiUINavigationWindowProxy.h"

@implementation TiUITransitionStyleProxy

MAKE_SYSTEM_PROP(SWIPE, NWTransitionSwipe);
MAKE_SYSTEM_PROP(SWIPEFADE, NWTransitionSwipeFade);
MAKE_SYSTEM_PROP(CUBE, NWTransitionCube);
MAKE_SYSTEM_PROP(CAROUSEL, NWTransitionCarousel);
MAKE_SYSTEM_PROP(CROSS, NWTransitionCross);
MAKE_SYSTEM_PROP(FLIP, NWTransitionFlip);
MAKE_SYSTEM_PROP(SWAP, NWTransitionSwap);
MAKE_SYSTEM_PROP(BACKFADE, NWTransitionBackFade);
MAKE_SYSTEM_PROP(GHOST, NWTransitionGhost);
MAKE_SYSTEM_PROP(ZOOM, NWTransitionZoom);
MAKE_SYSTEM_PROP(SCALE, NWTransitionScale);
MAKE_SYSTEM_PROP(GLUE, NWTransitionGlue);
MAKE_SYSTEM_PROP(PUSHROTATE, NWTransitionPushRotate);
MAKE_SYSTEM_PROP(FOLD, NWTransitionFold);
MAKE_SYSTEM_PROP(SLIDE, NWTransitionSlide);

@end
#endif