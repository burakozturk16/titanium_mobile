//
//  TiTransitionHelper.m
//  Titanium
//
//  Created by Martin Guillon on 04/10/13.
//
//

#import "TiTransitionHelper.h"
#import "TiUtils.h"
#import "ADModernPushTransition.h"
#import "ADTransition.h"
#import "ADDualTransition.h"
#import "ADTransformTransition.h"
#import "ADCrossTransition.h"
#import "ADSwapTransition.h"
#import "ADGhostTransition.h"
#import "ADBackFadeTransition.h"
#import "ADZoomTransition.h"
#import "ADSwipeTransition.h"
#import "ADScaleTransition.h"
#import "ADGlueTransition.h"
#import "ADPushRotateTransition.h"
#import "ADSlideTransition.h"

#import "TiTransitionSwipe.h"
#import "TiTransitionSwipeFade.h"
#import "TiTransitionCarousel.h"
#import "TiTransitionFlip.h"
#import "TiTransitionFade.h"
#import "TiTransitionCube.h"
#import "TiTransitionFold.h"
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

+(ADTransition<TiTransition>*) tiTransitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view options:(NSDictionary*)options
{
    switch (type) {
        case NWTransitionSwipe:
            return [[TiTransitionSwipe alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSwipeFade:
            return [[TiTransitionSwipeFade alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionCube:
        {
            TiTransitionCube* result = [[TiTransitionCube alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            if ([options objectForKey:@"faces"]) {
                result.faceNb = [TiUtils intValue:@"faces" properties:options];
            }
            return result;
            break;
        }
        case NWTransitionCarousel:
        {
            TiTransitionCarousel* result = [[TiTransitionCarousel alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            if ([options objectForKey:@"faces"]) {
                result.faceNb = [TiUtils intValue:@"faces" properties:options];
            }
            return result;
            break;
        }
        case NWTransitionCross:
            return [[ADCrossTransition alloc] initWithDuration:duration sourceRect:view.frame];
            break;
        case NWTransitionFlip:
            return [[TiTransitionFlip alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
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
            return [[TiTransitionFold alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSlide:
            return [[ADSlideTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionFade:
            return [[TiTransitionFade alloc] initWithDuration:duration];
            break;
        case NWTransitionModernPush:
            return [[ADModernPushTransition alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        default:
            return nil;
            break;
    }

}

+(ADTransition<TiTransition>*) tiTransitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view

{
    return [self tiTransitionForType:type subType:subtype withDuration:duration containerView:view options:nil];
}

+(NWTransition) typeFromObject:(ADTransition*)transition {
    if ([transition isKindOfClass:[ADSlideTransition class]]) return NWTransitionSlide;
    if ([transition isKindOfClass:[ADSwipeTransition class]]) return NWTransitionSwipe;
    if ([transition isKindOfClass:[ADSwipeFadeTransition class]]) return NWTransitionSwipeFade;
    if ([transition isKindOfClass:[ADFadeTransition class]]) return NWTransitionFade;
    if ([transition isKindOfClass:[ADFlipTransition class]]) return NWTransitionFlip;
    if ([transition isKindOfClass:[ADCarrouselTransition class]]) return NWTransitionCarousel;
    if ([transition isKindOfClass:[ADCubeTransition class]]) return NWTransitionCube;
    if ([transition isKindOfClass:[ADModernPushTransition class]]) return NWTransitionModernPush;
    if ([transition isKindOfClass:[ADCrossTransition class]]) return NWTransitionCross;
    if ([transition isKindOfClass:[ADSwapTransition class]]) return NWTransitionSwap;
    if ([transition isKindOfClass:[ADBackFadeTransition class]]) return NWTransitionBackFade;
    if ([transition isKindOfClass:[ADGhostTransition class]]) return NWTransitionGhost;
    if ([transition isKindOfClass:[ADZoomTransition class]]) return NWTransitionZoom;
    if ([transition isKindOfClass:[ADScaleTransition class]]) return NWTransitionScale;
    if ([transition isKindOfClass:[ADGlueTransition class]]) return NWTransitionGlue;
    if ([transition isKindOfClass:[ADPushRotateTransition class]]) return NWTransitionPushRotate;
    if ([transition isKindOfClass:[ADFoldTransition class]]) return NWTransitionFold;
}

+(BOOL)isTransitionPush:(ADTransition*)transition
{
    return transition.orientation == ADTransitionRightToLeft || transition.orientation == ADTransitionBottomToTop;
}

+(BOOL)isTransitionVertical:(ADTransition*)transition
{
    return transition.orientation == ADTransitionTopToBottom || transition.orientation == ADTransitionBottomToTop;
}

+(ADTransition<TiTransition>*)transitionFromArg:(NSDictionary*)arg defaultArg:(NSDictionary*)defaultArg defaultTransition:(ADTransition<TiTransition>*)transition containerView:(UIView*)container
{
    ADTransition<TiTransition>* result = transition;
    if (arg != nil || defaultArg != nil) {
        float duration = [TiUtils floatValue:@"duration" properties:arg def:[TiUtils floatValue:@"duration" properties:defaultArg def:300]]/1000;
        ADTransitionOrientation subtype = [TiUtils intValue:@"substyle" properties:arg def:transition?transition.orientation:[TiUtils intValue:@"substyle" properties:defaultArg def:ADTransitionRightToLeft]];
        NWTransition type = [TiUtils intValue:@"style" properties:arg def:transition?([self typeFromObject:transition]):[TiUtils intValue:@"type" properties:defaultArg def:-1]];
        result = [self tiTransitionForType:type subType:subtype withDuration:duration containerView:container options:arg];
        if ([transition isReversed] && result)
        {
            result = [result reverseTransition];
        }
    }
    return result;
}

+(ADTransition<TiTransition>*)transitionFromArg:(NSDictionary*)arg defaultTransition:(ADTransition<TiTransition>*)transition containerView:(UIView*)container
{
    return [self transitionFromArg:arg defaultArg:nil defaultTransition:transition containerView:container];
}
+(ADTransition<TiTransition>*)transitionFromArg:(NSDictionary*)arg defaultArg:(NSDictionary*)defaultArg containerView:(UIView*)container
{
    return [self transitionFromArg:arg defaultArg:defaultArg defaultTransition:nil containerView:container];
}
+(ADTransition<TiTransition>*)transitionFromArg:(NSDictionary*)arg containerView:(UIView*)container
{
    return [self transitionFromArg:arg defaultArg:nil defaultTransition:nil containerView:container];
}

@end
