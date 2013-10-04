//
//  TiUITransitionStyleProxy.h
//  Titanium
//
//  Created by Martin Guillon on 22/09/13.
//
//

#ifdef USE_TI_UITRANSITIONSTYLE

#import "TiProxy.h"

@interface TiUITransitionStyleProxy : TiProxy

@property(nonatomic,readonly) NSNumber *SWIPE;
@property(nonatomic,readonly) NSNumber *SWIPE_FADE;
@property(nonatomic,readonly) NSNumber *CUBE;
@property(nonatomic,readonly) NSNumber *CAROUSEL;
@property(nonatomic,readonly) NSNumber *CROSS;
@property(nonatomic,readonly) NSNumber *FLIP;
@property(nonatomic,readonly) NSNumber *SWAP;
@property(nonatomic,readonly) NSNumber *BACK_FADE;
@property(nonatomic,readonly) NSNumber *GHOST;
@property(nonatomic,readonly) NSNumber *ZOOM;
@property(nonatomic,readonly) NSNumber *SCALE;
@property(nonatomic,readonly) NSNumber *GLUE;
@property(nonatomic,readonly) NSNumber *PUSH_ROTATE;
@property(nonatomic,readonly) NSNumber *FOLD;
@property(nonatomic,readonly) NSNumber *SLIDE;

@property(nonatomic,readonly) NSNumber *TOP_TO_BOTTOM;
@property(nonatomic,readonly) NSNumber *BOTTOM_TO_TOP;
@property(nonatomic,readonly) NSNumber *RIGHT_TO_LEFT;
@property(nonatomic,readonly) NSNumber *LEFT_TO_RIGHT;

@end
#endif