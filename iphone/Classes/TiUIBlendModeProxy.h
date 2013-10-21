//
//  TiUIBlendModeProxy.h
//  Titanium
//
//  Created by Martin Guillon on 11/10/13.
//
//

#ifdef USE_TI_UIBLENDMODE
#import "TiProxy.h"

@interface TiUIBlendModeProxy : TiProxy
@property(nonatomic,readonly) NSNumber *DARKEN;
@property(nonatomic,readonly) NSNumber *LIGHTEN;
@property(nonatomic,readonly) NSNumber *MULTIPLY;
@property(nonatomic,readonly) NSNumber *ADD;
@property(nonatomic,readonly) NSNumber *SCREEN;
@property(nonatomic,readonly) NSNumber *CLEAR;
@property(nonatomic,readonly) NSNumber *DST;
@property(nonatomic,readonly) NSNumber *SRC;
@property(nonatomic,readonly) NSNumber *DST_ATOP;
@property(nonatomic,readonly) NSNumber *DST_IN;
@property(nonatomic,readonly) NSNumber *DST_OUT;
@property(nonatomic,readonly) NSNumber *DST_OVER;
@property(nonatomic,readonly) NSNumber *SRC_ATOP;
@property(nonatomic,readonly) NSNumber *SRC_IN;
@property(nonatomic,readonly) NSNumber *SRC_OUT;
@property(nonatomic,readonly) NSNumber *SRC_OVER;
@property(nonatomic,readonly) NSNumber *OVERLAY;
@property(nonatomic,readonly) NSNumber *XOR;
@end

#endif