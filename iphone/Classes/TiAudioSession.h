/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2014 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#if defined(USE_TI_AUDIO)

extern NSString * const kTiAudioSessionInterruptionBegin;
extern NSString * const kTiAudioSessionInterruptionEnd;
extern NSString * const kTiAudioSessionRouteChange;
extern NSString * const kTiAudioSessionVolumeChange;
extern NSString * const kTiAudioSessionInputChange;

@interface TiAudioSession : NSObject {
@private
	NSInteger count;
	NSLock *lock;
}

@property (readwrite, assign) NSString* sessionMode;

+(TiAudioSession*)sharedSession;

-(void)startAudioSession;
-(void)stopAudioSession;
-(BOOL)canRecord;
-(BOOL)canPlayback;
-(BOOL)isActive;
-(NSDictionary*)currentRoute;
-(CGFloat)volume;
-(BOOL)isAudioPlaying;
-(BOOL)hasInput;
-(void)setRouteOverride:(UInt32)mode;

@end

#endif