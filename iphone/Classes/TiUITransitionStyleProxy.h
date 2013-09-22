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
@property(nonatomic,readonly) NSNumber *SWIPEFADE;
@property(nonatomic,readonly) NSNumber *CUBE;
@property(nonatomic,readonly) NSNumber *CAROUSEL;
@property(nonatomic,readonly) NSNumber *CROSS;
@property(nonatomic,readonly) NSNumber *FLIP;
@property(nonatomic,readonly) NSNumber *SWAP;
@property(nonatomic,readonly) NSNumber *BACKFADE;
@property(nonatomic,readonly) NSNumber *GHOST;
@property(nonatomic,readonly) NSNumber *ZOOM;
@property(nonatomic,readonly) NSNumber *SCALE;
@property(nonatomic,readonly) NSNumber *GLUE;
@property(nonatomic,readonly) NSNumber *PUSHROTATE;
@property(nonatomic,readonly) NSNumber *FOLD;
@property(nonatomic,readonly) NSNumber *SLIDE;

@end
#endif