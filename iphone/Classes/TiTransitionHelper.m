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
#import "TiTransitionSwipeDualFade.h"
#import "TiTransitionCarousel.h"
#import "TiTransitionFlip.h"
#import "TiTransitionFade.h"
#import "TiTransitionCube.h"
#import "TiTransitionFold.h"

#import "TiTransition.h"

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
#import "ADSwipeDualFadeTransition.h"
#import "ADScaleTransition.h"
#import "ADGlueTransition.h"
#import "ADPushRotateTransition.h"
#import "ADFoldTransition.h"
#import "ADSlideTransition.h"
#import "ADModernPushTransition.h"

@interface UIView (FindUIViewController)
- (UIViewController *) firstAvailableUIViewController;
- (id) traverseResponderChainForUIViewController;
@end

@implementation UIView (FindUIViewController)
- (UIViewController *) firstAvailableUIViewController {
    // convenience function for casting and to "mask" the recursive function
    return (UIViewController *)[self traverseResponderChainForUIViewController];
}

- (id) traverseResponderChainForUIViewController {
    id nextResponder = [self nextResponder];
    if ([nextResponder isKindOfClass:[UIViewController class]]) {
        return nextResponder;
    } else if ([nextResponder isKindOfClass:[UIView class]]) {
        return [nextResponder traverseResponderChainForUIViewController];
    } else {
        return nil;
    }
}
@end

@interface TransitionView : UIView
@end
@implementation TransitionView
+ (Class)layerClass {
    return [CATransformLayer class];
}
@end

@implementation TiTransitionHelper


static NSDictionary* typeMap = nil;
+(NSDictionary*)typeMap
{
    if (typeMap == nil) {
        typeMap = [@{NSStringFromClass([ADCrossTransition class]): NUMINT(NWTransitionCross),
                    NSStringFromClass([ADCarrouselTransition class]): NUMINT(NWTransitionCarousel),
                    NSStringFromClass([ADCubeTransition class]): NUMINT(NWTransitionCube),
                    NSStringFromClass([ADFadeTransition class]): NUMINT(NWTransitionFade),
                    NSStringFromClass([ADFlipTransition class]): NUMINT(NWTransitionFlip),
                    NSStringFromClass([ADSwapTransition class]): NUMINT(NWTransitionSwap),
                    NSStringFromClass([ADGhostTransition class]): NUMINT(NWTransitionGhost),
                    NSStringFromClass([ADBackFadeTransition class]): NUMINT(NWTransitionBackFade),
                    NSStringFromClass([ADZoomTransition class]): NUMINT(NWTransitionZoom),
                    NSStringFromClass([ADSwipeTransition class]): NUMINT(NWTransitionSwipe),
                    NSStringFromClass([ADSwipeFadeTransition class]): NUMINT(NWTransitionSwipeFade),
                    NSStringFromClass([ADSwipeDualFadeTransition class]): NUMINT(NWTransitionSwipeDualFade),
                    NSStringFromClass([ADScaleTransition class]): NUMINT(NWTransitionScale),
                    NSStringFromClass([ADGlueTransition class]): NUMINT(NWTransitionGlue),
                    NSStringFromClass([ADPushRotateTransition class]): NUMINT(NWTransitionPushRotate),
                    NSStringFromClass([ADFoldTransition class]): NUMINT(NWTransitionFold),
                    NSStringFromClass([ADSlideTransition class]): NUMINT(NWTransitionSlide),
                    NSStringFromClass([ADModernPushTransition class]): NUMINT(NWTransitionModernPush)} retain];
    }
    return typeMap;
}

+(NSNumber*)tiTransitionTypeForADTransition:(ADTransition*)transition
{
    return [[self typeMap] objectForKey:NSStringFromClass([transition class])];
}

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
        case NWTransitionSwipeDualFade:
            result = [[TiTransitionSwipeDualFade alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
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
            result = [[TiTransitionGhost alloc] initWithDuration:duration];
            break;
        case NWTransitionZoom:
        {
            CGFloat scale = 2.0f;
            if ([options objectForKey:@"scale"]) {
                scale = [TiUtils floatValue:@"scale" properties:options];
            }
            result = [[TiTransitionZoom alloc] initWithScale:scale forDuration:duration];
            break;
        }
        case NWTransitionScale:
            result = [[TiTransitionScale alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionGlue:
            result = [[TiTransitionGlue alloc] initWithDuration:duration orientation:subtype sourceRect:view.frame];
            break;
        case NWTransitionPushRotate:
            result = [[TiTransitionPushRotate alloc] initWithDuration:duration orientation:ADTransitionRightToLeft sourceRect:view.frame];
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
        result.duration = duration;
    }
    return [result autorelease];
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
    if ((arg != nil && [arg isKindOfClass:[NSDictionary class]]) || defaultArg != nil) {
        float duration = [TiUtils floatValue:@"duration" properties:arg def:[TiUtils floatValue:@"duration" properties:defaultArg def:300]]/1000;
        BOOL reversed =  [TiUtils boolValue:@"reverse" properties:arg def:(transition && [transition.adTransition isReversed])];
        
        ADTransitionOrientation subtype = [TiUtils intValue:@"substyle" properties:arg def:transition?transition.orientation:[TiUtils intValue:@"substyle" properties:defaultArg def:ADTransitionRightToLeft]];
        NWTransition type = [TiUtils intValue:@"style" properties:arg def:transition?([self typeFromObject:transition]):[TiUtils intValue:@"style" properties:defaultArg def:-1]];
        result = [self tiTransitionForType:type subType:subtype withDuration:duration containerView:container options:arg];
        if (result && reversed)
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

+ (void)transitionfromView:(UIView *)viewOut toView:(UIView *)viewIn insideView:(UIView*)holder withTransition:(TiTransition *)transition completionBlock:(void (^)(void))block
{
    ADTransition* adTransition = transition.adTransition ;
    
    BOOL needsTransformFix = [adTransition isKindOfClass:[ADTransformTransition class]] && ![holder.layer isKindOfClass:[CATransformLayer class]];
    UIView* workingView = holder;
    
    if (needsTransformFix) {
        workingView = [[TransitionView alloc] initWithFrame: holder.bounds];
        [holder addSubview:workingView];
        if (viewOut) {
            [workingView addSubview:viewOut];
        }
        [workingView release];
    }
    if (viewIn) {
        [workingView addSubview:viewIn];
    }
    
    adTransition.type = ADTransitionTypePush;
    [transition prepareViewHolder:holder];
    [adTransition prepareTransitionFromView:viewOut toView:viewIn inside:workingView];
    
    void (^completionBlock)(void) = ^void(void) {
        [viewOut removeFromSuperview];
        if (needsTransformFix) {
            if (viewIn) {
                [holder addSubview:viewIn];
            }
            [workingView removeFromSuperview];
        }
        if (block != nil) {
            block();
        }
    };
    
    UIViewController * holderController = [holder firstAvailableUIViewController];
    if ([holderController.view isHidden]) {
        completionBlock();
        return;
    }
    [CATransaction setCompletionBlock:^{
        [adTransition finishedTransitionFromView:viewOut toView:viewIn inside:workingView];
        [transition finishedTransitionFromView:viewOut toView:viewIn inside:workingView];
        completionBlock();
    }];
    
    [adTransition startTransitionFromView:viewOut toView:viewIn inside:workingView];
}

@end
