/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */


#ifdef USE_TI_AUDIOSTREAMER

#import "TiProxy.h"
#import "ImageLoader.h"

@interface TiAudioStreamerProxy : TiProxy<ImageLoaderDelegate>

@property (nonatomic,readonly) NSURL *url;
@property (nonatomic,readwrite,assign)  NSNumber *paused;
@property (nonatomic,readonly) NSNumber *playing;
@property (nonatomic,readonly) NSNumber *waiting;
@property (nonatomic,readonly) NSNumber *idle;
@property (nonatomic,readonly) NSNumber *bitRate;
@property (nonatomic,readonly) NSNumber *progress;
@property (nonatomic,readonly) NSNumber *state;
@property (nonatomic,readonly) NSNumber *duration;

@property (nonatomic,copy)	NSNumber *volume;

@property (nonatomic,readonly) NSNumber *STATE_INITIALIZED;
@property (nonatomic,readonly) NSNumber *STATE_ERROR;
@property (nonatomic,readonly) NSNumber *STATE_PLAYING;
@property (nonatomic,readonly) NSNumber *STATE_BUFFERING;
@property (nonatomic,readonly) NSNumber *STATE_STOPPED;
@property (nonatomic,readonly) NSNumber *STATE_PAUSED;

@end

#endif