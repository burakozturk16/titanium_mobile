//
//  TiTransitionHelper.m
//  Titanium
//
//  Created by Martin Guillon on 04/10/13.
//
//

#import "TiTransitionHelper.h"
#import "TiUtils.h"
#import "TiTransitionModernPush.h"
#import "TiTransitionCross.h"
#import "TiTransitionSwap.h"
#import "TiTransitionGhost.h"
#import "TitransitionBackFade.h"
#import "TiTransitionZoom.h"
#import "TiTransitionScale.h"
#import "TiTransitionGlue.h"
#import "TiTransitionPushRotate.h"
#import "TiTransitionSlide.h"
#import "TiTransitionSwipe.h"
#import "TiTransitionSwipeFade.h"
#import "TiTransitionCarousel.h"
#import "TiTransitionFlip.h"
#import "TiTransitionFade.h"
#import "TiTransitionCube.h"
#import "TiTransitionFold.h"

#import "TiTransition.h"

@implementation TiTransitionHelper

+(TiTransition*) tiTransitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view options:(NSDictionary*)options
{
    TiTransition* result;
    switch (type) {
        case NWTransitionSwipe:
            result = [[TiTransitionSwipe alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSwipeFade:
            result = [[TiTransitionSwipeFade alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionCube:
        {
            result = [[TiTransitionCube alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            if ([options objectForKey:@"faces"]) {
                ((TiTransitionCube*)result).faceNb = [TiUtils intValue:@"faces" properties:options];
            }
            break;
        }
        case NWTransitionCarousel:
        {
            result = [[TiTransitionCarousel alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            if ([options objectForKey:@"faces"]) {
                ((TiTransitionCarousel*)result).faceNb = [TiUtils intValue:@"faces" properties:options];
            }
            return result;
            break;
        }
        case NWTransitionCross:
            result = [[TiTransitionCross alloc] initWithDuration:duration sourceRect:view.frame];
            break;
        case NWTransitionFlip:
            result = [[TiTransitionFlip alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSwap:
            result = [[TiTransitionSwap alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionBackFade:
            result = [[TitransitionBackFade alloc] initWithDuration:duration];
            break;
        case NWTransitionGhost:
            return [[TiTransitionGhost alloc] initWithDuration:duration];
            break;
        case NWTransitionZoom:
            result = [[TiTransitionZoom alloc] initWithDuration:duration];
            break;
        case NWTransitionScale:
            result = [[TiTransitionScale alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionGlue:
            result = [[TiTransitionGlue alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionPushRotate:
            return [[TiTransitionPushRotate alloc] initWithDuration:duration orientation:ADTransitionRightToLeft sourceRect:view.frame];
            break;
        case NWTransitionFold:
            result = [[TiTransitionFold alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionSlide:
            result = [[TiTransitionSlide alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionFade:
            result = [[TiTransitionFade alloc] initWithDuration:duration];
            break;
        case NWTransitionModernPush:
            result = [[TiTransitionModernPush alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        default:
            return nil;
            break;
    }
    if (result != nil) {
        result.type = type;
    }
    return result;
}

+(TiTransition*) tiTransitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view

{
    return [self tiTransitionForType:type subType:subtype withDuration:duration containerView:view options:nil];
}

+(NWTransition) typeFromObject:(TiTransition*)transition {
    return transition.type;
}

+(BOOL)isTransitionPush:(ADTransition*)transition
{
    return transition.orientation == ADTransitionRightToLeft || transition.orientation == ADTransitionBottomToTop;
}

+(BOOL)isTransitionVertical:(ADTransition*)transition
{
    return transition.orientation == ADTransitionTopToBottom || transition.orientation == ADTransitionBottomToTop;
}

+(TiTransition*)transitionFromArg:(NSDictionary*)arg defaultArg:(NSDictionary*)defaultArg defaultTransition:(TiTransition*)transition containerView:(UIView*)container
{
    TiTransition* result = transition;
    if (arg != nil || defaultArg != nil) {
        float duration = [TiUtils floatValue:@"duration" properties:arg def:[TiUtils floatValue:@"duration" properties:defaultArg def:300]]/1000;
        ADTransitionOrientation subtype = [TiUtils intValue:@"substyle" properties:arg def:transition?transition.orientation:[TiUtils intValue:@"substyle" properties:defaultArg def:ADTransitionRightToLeft]];
        NWTransition type = [TiUtils intValue:@"style" properties:arg def:transition?([self typeFromObject:transition]):[TiUtils intValue:@"type" properties:defaultArg def:-1]];
        result = [self tiTransitionForType:type subType:subtype withDuration:duration containerView:container options:arg];
        if (result && transition && [transition.adTransition isReversed])
        {
            [result reverseADTransition];
        }
    }
    return result;
}

+(TiTransition*)transitionFromArg:(NSDictionary*)arg defaultTransition:(TiTransition*)transition containerView:(UIView*)container
{
    return [self transitionFromArg:arg defaultArg:nil defaultTransition:transition containerView:container];
}
+(TiTransition*)transitionFromArg:(NSDictionary*)arg defaultArg:(NSDictionary*)defaultArg containerView:(UIView*)container
{
    return [self transitionFromArg:arg defaultArg:defaultArg defaultTransition:nil containerView:container];
}
+(TiTransition*)transitionFromArg:(NSDictionary*)arg containerView:(UIView*)container
{
    return [self transitionFromArg:arg defaultArg:nil defaultTransition:nil containerView:container];
}

@end
