//
//  TiTransitionHelper.m
//  Titanium
//
//  Created by Martin Guillon on 04/10/13.
//
//

#import "TiTransitionHelper.h"
#import "ADModernPushTransition.h"
#import "ADTransition.h"
#import "ADDualTransition.h"
#import "ADTransformTransition.h"
#import "ADCarrouselTransition.h"
#import "ADCubeTransition.h"
#import "ADCrossTransition.h"
#import "ADFadeTransition.h"
#import "ADFlipTransition.h"
#import "ADSwapTransition.h"
#import "ADGhostTransition.h"
#import "ADBackFadeTransition.h"
#import "ADZoomTransition.h"
#import "ADSwipeTransition.h"
#import "ADSwipeFadeTransition.h"
#import "ADScaleTransition.h"
#import "ADGlueTransition.h"
#import "ADPushRotateTransition.h"
#import "ADFoldTransition.h"
#import "ADSlideTransition.h"
@implementation TiTransitionHelper


+(ADTransition*) transitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view
{
    switch (type) {
        case NWTransitionSwipe:
            return [[ADSwipeTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSwipeFade:
            return [[ADSwipeFadeTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionCube:
            return [[ADCubeTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionCarousel:
            return [[ADCarrouselTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionCross:
            return [[ADCrossTransition alloc] initWithDuration:duration sourceRect:view.frame];
            break;
        case NWTransitionFlip:
            return [[ADFlipTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSwap:
            return [[ADSwapTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionBackFade:
            return [[ADBackFadeTransition alloc] initWithDuration:duration];
            break;
        case NWTransitionGhost:
            return [[ADGhostTransition alloc] initWithDuration:duration];
            break;
        case NWTransitionZoom:
            return [[ADZoomTransition alloc] initWithDuration:duration];
            break;
        case NWTransitionScale:
            return [[ADScaleTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionGlue:
            return [[ADGlueTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionPushRotate:
            return [[ADPushRotateTransition alloc] initWithDuration:duration orientation:ADTransitionRightToLeft sourceRect:view.frame];
            break;
        case NWTransitionFold:
            return [[ADFoldTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSlide:
            return [[ADSlideTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionFade:
            return [[ADFadeTransition alloc] initWithDuration:duration];
            break;
        case NWTransitionModernPush:
            return [[ADModernPushTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        default:
            return nil;
            break;
    }
}

@end
