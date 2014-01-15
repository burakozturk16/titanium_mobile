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
    NWTransitionSwipeDualFade,
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

@class TiTransition;
@interface TiTransitionHelper : NSObject

+(TiTransition*) tiTransitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view;
+(TiTransition*) tiTransitionForType:(NWTransition)type subType:(ADTransitionOrientation)subtype withDuration:(float)duration containerView:(UIView*)view options:(NSDictionary*)options;

+(BOOL)isTransitionPush:(ADTransition*)transition;
+(BOOL)isTransitionVertical:(ADTransition*)transition;
+(TiTransition*)transitionFromArg:(NSDictionary*)arg defaultArg:(NSDictionary*)defaultArg defaultTransition:(TiTransition*)transition containerView:(UIView*)container;
+(TiTransition*)transitionFromArg:(NSDictionary*)arg defaultTransition:(TiTransition*)transition containerView:(UIView*)container;
+(TiTransition*)transitionFromArg:(NSDictionary*)arg defaultArg:(NSDictionary*)defaultArg containerView:(UIView*)container;
+(TiTransition*)transitionFromArg:(NSDictionary*)arg containerView:(UIView*)container;
+(NSNumber*)tiTransitionTypeForADTransition:(ADTransition*)transition;
+ (void)transitionfromView:(UIView *)viewOut toView:(UIView *)viewIn insideView:(UIView*)holder withTransition:(TiTransition *)transition completionBlock:(void (^)(void))block;
+ (void)transitionfromView:(UIView *)viewOut toView:(UIView *)viewIn insideView:(UIView*)holder withTransition:(TiTransition *)transition prepareBlock:(void (^)(void))prepareBlock completionBlock:(void (^)(void))block;

@end
