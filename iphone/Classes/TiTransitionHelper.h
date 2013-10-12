//
//  TiTransitionHelper.h
//  Titanium
//
//  Created by Martin Guillon on 04/10/13.
//
//
#import "ADTransition.h"

typedef enum NWTransition {
    NWTransitionModernPush,
    NWTransitionSwipe,
    NWTransitionSwipeFade,
    NWTransitionCube,
    NWTransitionCarousel,
    NWTransitionCross,
    NWTransitionFlip,
    NWTransitionSwap,
    NWTransitionBackFade,
    NWTransitionGhost,
    NWTransitionZoom,
    NWTransitionScale,
    NWTransitionGlue,
    NWTransitionPushRotate,
    NWTransitionFold,
    NWTransitionSlide,
    NWTransitionFade
} NWTransition;

@interface TiTransitionHelper : NSObject

+(ADTransition*) transitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view;

@end
