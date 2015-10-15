/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_AUDIO

#import "AudioModule.h"
#import "TiUtils.h"
#import "TiApp.h"
#import "TiViewProxy.h"
#import "Ti2DMatrix.h"
#import "SCListener.h"
#import "TiAudioSession.h"
#import "TiAudioMusicPlayer.h"
#import "TiAudioItem.h"

#import <AudioToolbox/AudioToolbox.h>
#import <AVFoundation/AVAudioPlayer.h>
#import <AVFoundation/AVAudioSession.h>

#import <UIKit/UIPopoverController.h>
// by default, we want to make the camera fullscreen and 
// these transform values will scale it when we have our own overlay

enum  
{
	AudioModuleErrorUnknown,
	AudioModuleErrorBusy,
	AudioModuleErrorNoMusicPlayer
};

// Have to distinguish between filterable and nonfilterable properties
static NSDictionary* TI_itemProperties;
static NSDictionary* TI_filterableItemProperties;

#pragma mark - Backwards compatibility for pre-iOS 7.0

#if __IPHONE_OS_VERSION_MAX_ALLOWED < __IPHONE_7_0

@protocol AVAudioSessionIOS7Support <NSObject>
@optional
- (void)requestRecordPermission:(PermissionBlock)response;
typedef void (^PermissionBlock)(BOOL granted)
@end

#endif


@implementation AudioModule
@synthesize popoverView;

-(void)dealloc
{
    [self destroyPicker];
    RELEASE_TO_NIL(systemMusicPlayer);
    RELEASE_TO_NIL(appMusicPlayer);
    RELEASE_TO_NIL(popoverView);
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [super dealloc];
}

#pragma mark Static Properties

+(NSDictionary*)filterableItemProperties
{
    if (TI_filterableItemProperties == nil) {
        TI_filterableItemProperties = [[NSDictionary alloc] initWithObjectsAndKeys:MPMediaItemPropertyMediaType, @"mediaType", // Filterable
                                                                                   MPMediaItemPropertyTitle, @"title", // Filterable
                                                                                   MPMediaItemPropertyAlbumTitle, @"albumTitle", // Filterable
                                                                                   MPMediaItemPropertyArtist, @"artist", // Filterable
                                                                                   MPMediaItemPropertyAlbumArtist, @"albumArtist", //Filterable
                                                                                   MPMediaItemPropertyGenre, @"genre", // Filterable
                                                                                   MPMediaItemPropertyComposer, @"composer", // Filterable
                                                                                   MPMediaItemPropertyIsCompilation, @"isCompilation", // Filterable
                                                                                   nil];
    }
    return TI_filterableItemProperties;
}

+(NSDictionary*)itemProperties
{
	if (TI_itemProperties == nil) {
		TI_itemProperties = [[NSDictionary alloc] initWithObjectsAndKeys:MPMediaItemPropertyPlaybackDuration, @"playbackDuration",
                                                                         MPMediaItemPropertyAlbumTrackNumber, @"albumTrackNumber",
                                                                         MPMediaItemPropertyAlbumTrackCount, @"albumTrackCount",
                                                                         MPMediaItemPropertyDiscNumber, @"discNumber",
                                                                         MPMediaItemPropertyDiscCount, @"discCount",
                                                                         MPMediaItemPropertyLyrics, @"lyrics",
                                                                         MPMediaItemPropertyPodcastTitle, @"podcastTitle",
                                                                         MPMediaItemPropertyPlayCount, @"playCount",
                                                                         MPMediaItemPropertySkipCount, @"skipCount",
                                                                         MPMediaItemPropertyRating, @"rating",
                                                                         nil	];		
	}
	return TI_itemProperties;
}

#pragma mark Public Properties
-(NSString*)apiName
{
    return @"Ti.Media";
}

MAKE_SYSTEM_UINT(AUDIO_FORMAT_LINEAR_PCM,kAudioFormatLinearPCM);
MAKE_SYSTEM_UINT(AUDIO_FORMAT_ULAW,kAudioFormatULaw);
MAKE_SYSTEM_UINT(AUDIO_FORMAT_ALAW,kAudioFormatALaw);
MAKE_SYSTEM_UINT(AUDIO_FORMAT_IMA4,kAudioFormatAppleIMA4);
MAKE_SYSTEM_UINT(AUDIO_FORMAT_ILBC,kAudioFormatiLBC);
MAKE_SYSTEM_UINT(AUDIO_FORMAT_APPLE_LOSSLESS,kAudioFormatAppleLossless);
MAKE_SYSTEM_UINT(AUDIO_FORMAT_AAC,kAudioFormatMPEG4AAC);

MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_WAVE,kAudioFileWAVEType);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_AIFF,kAudioFileAIFFType);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_MP3,kAudioFileMP3Type);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_MP4,kAudioFileMPEG4Type);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_MP4A,kAudioFileM4AType);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_CAF,kAudioFileCAFType);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_3GPP,kAudioFile3GPType);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_3GP2,kAudioFile3GP2Type);
MAKE_SYSTEM_UINT(AUDIO_FILEFORMAT_AMR,kAudioFileAMRType);


//Constants for currentRoute
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_LINEIN,AVAudioSessionPortLineIn)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_BUILTINMIC,AVAudioSessionPortBuiltInMic)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_HEADSETMIC,AVAudioSessionPortHeadsetMic)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_LINEOUT,AVAudioSessionPortLineOut)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_HEADPHONES,AVAudioSessionPortHeadphones)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_BLUETOOTHA2DP,AVAudioSessionPortBluetoothA2DP)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_BUILTINRECEIVER,AVAudioSessionPortBuiltInReceiver)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_BUILTINSPEAKER,AVAudioSessionPortBuiltInSpeaker)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_HDMI,AVAudioSessionPortHDMI)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_AIRPLAY,AVAudioSessionPortAirPlay)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_BLUETOOTHHFP,AVAudioSessionPortBluetoothHFP)
MAKE_SYSTEM_STR(AUDIO_SESSION_PORT_USBAUDIO,AVAudioSessionPortUSBAudio)

-(NSString*)AUDIO_SESSION_PORT_BLUETOOTHLE
{
    return AVAudioSessionPortBluetoothLE;
}

-(NSString*)AUDIO_SESSION_PORT_CARAUDIO
{
    return AVAudioSessionPortCarAudio;
}


//Constants for AudioSessions
-(NSNumber*)AUDIO_SESSION_MODE_AMBIENT
{
    DEPRECATED_REPLACED(@"Media.AUDIO_SESSION_MODE_AMBIENT", @"3.4.2", @"Ti.Media.AUDIO_SESSION_CATEGORY_AMBIENT");
    return [NSNumber numberWithUnsignedInt:kAudioSessionCategory_AmbientSound];
}
-(NSNumber*)AUDIO_SESSION_MODE_SOLO_AMBIENT
{
    DEPRECATED_REPLACED(@"Media.AUDIO_SESSION_MODE_SOLO_AMBIENT", @"3.4.2", @"Ti.Media.AUDIO_SESSION_CATEGORY_SOLO_AMBIENT");
    return [NSNumber numberWithUnsignedInt:kAudioSessionCategory_SoloAmbientSound];
}
-(NSNumber*)AUDIO_SESSION_MODE_PLAYBACK
{
    DEPRECATED_REPLACED(@"Media.AUDIO_SESSION_MODE_PLAYBACK", @"3.4.2", @"Ti.Media.AUDIO_SESSION_CATEGORY_PLAYBACK");
    return [NSNumber numberWithUnsignedInt:kAudioSessionCategory_MediaPlayback];
}
-(NSNumber*)AUDIO_SESSION_MODE_RECORD
{
    DEPRECATED_REPLACED(@"Media.AUDIO_SESSION_MODE_RECORD", @"3.4.2", @"Ti.Media.AUDIO_SESSION_CATEGORY_RECORD");
    return [NSNumber numberWithUnsignedInt:kAudioSessionCategory_RecordAudio];
}
-(NSNumber*)AUDIO_SESSION_MODE_PLAY_AND_RECORD
{
    DEPRECATED_REPLACED(@"Media.AUDIO_SESSION_MODE_PLAY_AND_RECORD", @"3.4.2", @"Ti.Media.AUDIO_SESSION_CATEGORY_PLAY_AND_RECORD");
    return [NSNumber numberWithUnsignedInt:kAudioSessionCategory_PlayAndRecord];
}

//Constants for AudioSessions
MAKE_SYSTEM_STR(AUDIO_SESSION_CATEGORY_AMBIENT,AVAudioSessionCategoryAmbient);
MAKE_SYSTEM_STR(AUDIO_SESSION_CATEGORY_SOLO_AMBIENT, AVAudioSessionCategorySoloAmbient);
MAKE_SYSTEM_STR(AUDIO_SESSION_CATEGORY_PLAYBACK, AVAudioSessionCategoryPlayback);
MAKE_SYSTEM_STR(AUDIO_SESSION_CATEGORY_RECORD, AVAudioSessionCategoryRecord);
MAKE_SYSTEM_STR(AUDIO_SESSION_CATEGORY_PLAY_AND_RECORD, AVAudioSessionCategoryPlayAndRecord);


MAKE_SYSTEM_UINT(AUDIO_SESSION_OVERRIDE_ROUTE_NONE, AVAudioSessionPortOverrideNone);
MAKE_SYSTEM_UINT(AUDIO_SESSION_OVERRIDE_ROUTE_SPEAKER, AVAudioSessionPortOverrideSpeaker);

//Constants for mediaTypes in openMusicLibrary
MAKE_SYSTEM_PROP(MUSIC_MEDIA_TYPE_MUSIC, MPMediaTypeMusic);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_TYPE_PODCAST, MPMediaTypePodcast);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_TYPE_AUDIOBOOK, MPMediaTypeAudioBook);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_TYPE_ANY_AUDIO, MPMediaTypeAnyAudio);
-(NSNumber*)MUSIC_MEDIA_TYPE_ALL
{
    return NUMUINTEGER(MPMediaTypeAny);
}
//Constants for grouping in queryMusicLibrary
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_TITLE, MPMediaGroupingTitle);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_ALBUM, MPMediaGroupingAlbum);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_ARTIST, MPMediaGroupingArtist);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_ALBUM_ARTIST, MPMediaGroupingAlbumArtist);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_COMPOSER, MPMediaGroupingComposer);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_GENRE, MPMediaGroupingGenre);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_PLAYLIST, MPMediaGroupingPlaylist);
MAKE_SYSTEM_PROP(MUSIC_MEDIA_GROUP_PODCAST_TITLE, MPMediaGroupingPodcastTitle);

//Constants for MusicPlayer playback state
MAKE_SYSTEM_PROP(MUSIC_PLAYER_STATE_STOPPED, MPMusicPlaybackStateStopped);
MAKE_SYSTEM_PROP(MUSIC_PLAYER_STATE_PLAYING, MPMusicPlaybackStatePlaying);
MAKE_SYSTEM_PROP(MUSIC_PLAYER_STATE_PAUSED, MPMusicPlaybackStatePaused);
MAKE_SYSTEM_PROP(MUSIC_PLAYER_STATE_INTERRUPTED, MPMusicPlaybackStateInterrupted);
MAKE_SYSTEM_PROP(MUSIC_PLAYER_STATE_SKEEK_FORWARD, MPMusicPlaybackStateSeekingForward);
MAKE_SYSTEM_PROP(MUSIC_PLAYER_STATE_SEEK_BACKWARD, MPMusicPlaybackStateSeekingBackward);

//Constants for MusicPlayer repeatMode
MAKE_SYSTEM_PROP(REPEAT_DEFAULT, MPMusicRepeatModeDefault);
MAKE_SYSTEM_PROP(REPEAT_NONE, MPMusicRepeatModeNone);
MAKE_SYSTEM_PROP(REPEAT_ONE, MPMusicRepeatModeOne);
MAKE_SYSTEM_PROP(REPEAT_ALL, MPMusicRepeatModeAll);

//Constants for MusicPlayer shuffleMode
MAKE_SYSTEM_PROP(SHUFFLE_DEFAULT, MPMusicShuffleModeDefault);
MAKE_SYSTEM_PROP(SHUFFLE_NONE, MPMusicShuffleModeOff);
MAKE_SYSTEM_PROP(SHUFFLE_SONGS, MPMusicShuffleModeSongs);
MAKE_SYSTEM_PROP(SHUFFLE_ALBUMS, MPMusicShuffleModeAlbums);

//Error constants for MediaModule
MAKE_SYSTEM_PROP(UNKNOWN_ERROR,AudioModuleErrorUnknown);
MAKE_SYSTEM_PROP(DEVICE_BUSY,AudioModuleErrorBusy);
MAKE_SYSTEM_PROP(NO_MUSIC_PLAYER,AudioModuleErrorNoMusicPlayer);

-(TiAudioMusicPlayer*)systemMusicPlayer
{
    if (systemMusicPlayer == nil) {
        if (![NSThread isMainThread]) {
            __block id result;
            TiThreadPerformOnMainThread(^{result = [self systemMusicPlayer];}, YES);
            return result;
        }
        if ([TiUtils isIOS8OrGreater]) {
            systemMusicPlayer = [[TiAudioMusicPlayer alloc] _initWithPageContext:[self pageContext] player:[MPMusicPlayerController systemMusicPlayer]];
        } else {
            systemMusicPlayer = [[TiAudioMusicPlayer alloc] _initWithPageContext:[self pageContext] player:[MPMusicPlayerController iPodMusicPlayer]];
        }
    }
    return systemMusicPlayer;
}

-(TiAudioMusicPlayer*)appMusicPlayer
{
    if (appMusicPlayer == nil) {
        if (![NSThread isMainThread]) {
            __block id result;
            TiThreadPerformOnMainThread(^{result = [self appMusicPlayer];}, YES);
            return appMusicPlayer;
        }
        appMusicPlayer = [[TiAudioMusicPlayer alloc] _initWithPageContext:[self pageContext] player:[MPMusicPlayerController applicationMusicPlayer]];
    }
    return appMusicPlayer;
}

-(void)setDefaultAudioSessionMode:(NSNumber*)mode
{
    DebugLog(@"[WARN] Deprecated; use 'audioSessionMode'");
    [self setAudioSessionMode:mode];
}

-(NSNumber*)defaultAudioSessionMode
{
    DebugLog(@"[WARN] Deprecated; use 'audioSessionMode'");
    return [self audioSessionMode];
}

-(void)setAudioSessionMode:(NSNumber*)mode
{
    DebugLog(@"[WARN] Deprecated; use 'audioSessionCategory'");
    switch ([mode unsignedIntegerValue]) {
        case kAudioSessionCategory_AmbientSound:
            [self setAudioSessionCategory:[self AUDIO_SESSION_CATEGORY_AMBIENT]];
            break;
        case kAudioSessionCategory_SoloAmbientSound:
            [self setAudioSessionCategory:[self AUDIO_SESSION_CATEGORY_SOLO_AMBIENT]];
            break;
        case kAudioSessionCategory_PlayAndRecord:
            [self setAudioSessionCategory:[self AUDIO_SESSION_CATEGORY_PLAY_AND_RECORD]];
            break;
        case kAudioSessionCategory_RecordAudio:
            [self setAudioSessionCategory:[self AUDIO_SESSION_CATEGORY_RECORD]];
            break;
        case kAudioSessionCategory_MediaPlayback:
            [self setAudioSessionCategory:[self AUDIO_SESSION_CATEGORY_PLAYBACK]];
            break;
        default:
            DebugLog(@"Unsupported audioSessionMode specified");
            break;
    }
    
}

-(NSNumber*)audioSessionMode
{
    DebugLog(@"[WARN] Deprecated; use 'audioSessionCategory'");
    NSString* category = [self audioSessionCategory];
    if ([category isEqualToString:[self AUDIO_SESSION_CATEGORY_AMBIENT]]) {
        return [self AUDIO_SESSION_MODE_AMBIENT];
    } else if ([category isEqualToString:[self AUDIO_SESSION_CATEGORY_SOLO_AMBIENT]]) {
        return [self AUDIO_SESSION_MODE_SOLO_AMBIENT];
    } else if ([category isEqualToString:[self AUDIO_SESSION_CATEGORY_PLAYBACK]]) {
        return [self AUDIO_SESSION_MODE_PLAYBACK];
    } else if ([category isEqualToString:[self AUDIO_SESSION_CATEGORY_RECORD]]) {
        return [self AUDIO_SESSION_MODE_RECORD];
    } else if ([category isEqualToString:[self AUDIO_SESSION_CATEGORY_PLAY_AND_RECORD]]) {
        return [self AUDIO_SESSION_MODE_PLAY_AND_RECORD];
    } else {
        return NUMINT(-1);
    }
}

-(void)setAudioSessionCategory:(NSString*)mode
{
    [[TiAudioSession sharedSession] setSessionMode:mode];
}

-(NSString*)audioSessionCategory
{
    return [[TiAudioSession sharedSession] sessionMode];
}

-(NSNumber*)canRecord
{
    return NUMBOOL([[TiAudioSession sharedSession] hasInput]);
}

-(NSNumber*)volume
{
    return NUMFLOAT([[TiAudioSession sharedSession] volume]);
}

-(NSNumber*)audioPlaying
{
    return NUMBOOL([[TiAudioSession sharedSession] isAudioPlaying]);
}

-(NSDictionary*)currentRoute
{
    return [[TiAudioSession sharedSession] currentRoute];
}

#pragma mark Public Methods

-(void)setOverrideAudioRoute:(NSNumber*)mode
{
    [[TiAudioSession sharedSession] setRouteOverride:[mode unsignedIntValue]];
}

/**
 Microphone And Recording Support. These make no sense here and should be moved to Audiorecorder
 **/
-(void)requestAuthorization:(id)args
{
    DEPRECATED_REPLACED(@"Media.requestAuthorization", @"5.1.0", @"Media.requestAudioPermissions");
    [self requestAudioPermissions:args];
}

-(void)requestAudioPermissions:(id)args
{
    ENSURE_SINGLE_ARG(args, KrollCallback);
    KrollCallback * callback = args;
    if ([[AVAudioSession sharedInstance] respondsToSelector:@selector(requestRecordPermission:)]) {
        TiThreadPerformOnMainThread(^(){
            [[AVAudioSession sharedInstance] requestRecordPermission:^(BOOL granted){
                KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback
                                                                        eventObject:[TiUtils dictionaryWithCode:(granted ? 0 : 1) message:nil]
                                                                         thisObject:self];
                [[callback context] enqueue:invocationEvent];
				RELEASE_TO_NIL(invocationEvent);
            }];
        }, NO);
    } else {
        NSDictionary * propertiesDict = [TiUtils dictionaryWithCode:0 message:nil];
        NSArray * invocationArray = [[NSArray alloc] initWithObjects:&propertiesDict count:1];
        [callback call:invocationArray thisObject:self];
        [invocationArray release];
        return;
    }
}

-(void)startMicrophoneMonitor:(id)args
{
    [[SCListener sharedListener] listen];
}

-(void)stopMicrophoneMonitor:(id)args
{
    [[SCListener sharedListener] stop];
}

-(NSNumber*)peakMicrophonePower
{
    if ([[SCListener sharedListener] isListening])
    {
        return NUMFLOAT([[SCListener sharedListener] peakPower]);
    }
    return NUMFLOAT(-1);
}

-(NSNumber*)averageMicrophonePower
{
    if ([[SCListener sharedListener] isListening])
    {
        return NUMFLOAT([[SCListener sharedListener] averagePower]);
    }
    return NUMFLOAT(-1);
}

/**
 End Microphone and Recording Support
 **/

/**
 Music Library Support
 **/
-(void)openMusicLibrary:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    ENSURE_UI_THREAD(openMusicLibrary,args);
    
    if (musicPicker != nil) {
        [self sendPickerError:AudioModuleErrorBusy];
        return;
    }
    
    animatedPicker = YES;
    
    // Have to perform setup & manual check for simulator; otherwise things
    // fail less than gracefully.
    [self commonPickerSetup:args];
    
    // iPod not available on simulator
#if TARGET_IPHONE_SIMULATOR
    [self sendPickerError:AudioModuleErrorNoMusicPlayer];
    return;
#endif
    
    if (args != nil)
    {
        MPMediaType mediaTypes = 0;
        id mediaList = [args objectForKey:@"mediaTypes"];
        
        if (mediaList!=nil) {
            if ([mediaList isKindOfClass:[NSArray class]]) {
                for (NSNumber* type in mediaList) {
                    switch ([type integerValue]) {
                        case MPMediaTypeMusic:
                        case MPMediaTypeAnyAudio:
                        case MPMediaTypeAudioBook:
                        case MPMediaTypePodcast:
                        case MPMediaTypeAny:
                            mediaTypes |= [type integerValue];
                    }
                }
            }
            else {
                ENSURE_TYPE(mediaList, NSNumber);
                switch ([mediaList integerValue]) {
                    case MPMediaTypeMusic:
                    case MPMediaTypeAnyAudio:
                    case MPMediaTypeAudioBook:
                    case MPMediaTypePodcast:
                    case MPMediaTypeAny:
                        mediaTypes = [mediaList integerValue];
                }
            }
        }
        
        if (mediaTypes == 0) {
            mediaTypes = MPMediaTypeAny;
        }
        
        musicPicker = [[MPMediaPickerController alloc] initWithMediaTypes:mediaTypes];
        musicPicker.allowsPickingMultipleItems = [TiUtils boolValue:[args objectForKey:@"allowMultipleSelections"] def:NO];
    }
    else {
        musicPicker = [[MPMediaPickerController alloc] init];
    }
    [musicPicker setDelegate:self];
    
    [self displayModalPicker:musicPicker settings:args];
}

-(void)hideMusicLibrary:(id)args
{
    ENSURE_UI_THREAD(hideMusicLibrary,args);
    if (musicPicker != nil)
    {
        [[TiApp app] hideModalController:musicPicker animated:animatedPicker];
        [[TiApp controller] repositionSubviews];
        [self destroyPicker];
    }
}

-(NSArray*)queryMusicLibrary:(id)arg
{
    ENSURE_SINGLE_ARG(arg, NSDictionary);
    
    NSMutableSet* predicates = [NSMutableSet set];
    for (NSString* prop in [AudioModule filterableItemProperties]) {
        id value = [arg valueForKey:prop];
        if (value != nil) {
            if ([value isKindOfClass:[NSDictionary class]]) {
                id propVal = [value objectForKey:@"value"];
                bool exact = [TiUtils boolValue:[value objectForKey:@"exact"] def:YES];
                MPMediaPredicateComparison comparison = (exact) ? MPMediaPredicateComparisonEqualTo : MPMediaPredicateComparisonContains;
                [predicates addObject:[MPMediaPropertyPredicate predicateWithValue:propVal
                                                                       forProperty:[[AudioModule filterableItemProperties] valueForKey:prop]
                                                                    comparisonType:comparison]];
            }
            else {
                [predicates addObject:[MPMediaPropertyPredicate predicateWithValue:value
                                                                       forProperty:[[AudioModule filterableItemProperties] valueForKey:prop]]];
            }
        }
    }
    
    MPMediaQuery* query = [[[MPMediaQuery alloc] initWithFilterPredicates:predicates] autorelease];
    NSMutableArray* result = [NSMutableArray arrayWithCapacity:[[query items] count]];
    for (MPMediaItem* item in [query items]) {
        TiAudioItem* newItem = [[[TiAudioItem alloc] _initWithPageContext:[self pageContext] item:item] autorelease];
        [result addObject:newItem];
    }
    return result;
}

/**
 End Music Library Support
 **/


#pragma mark Internal Methods

-(void)destroyPickerCallbacks
{
	RELEASE_TO_NIL(pickerSuccessCallback);
	RELEASE_TO_NIL(pickerErrorCallback);
	RELEASE_TO_NIL(pickerCancelCallback);
}

-(void)destroyPicker
{
	RELEASE_TO_NIL(popover);
	RELEASE_TO_NIL(musicPicker);
	RELEASE_TO_NIL(pickerSuccessCallback);
	RELEASE_TO_NIL(pickerErrorCallback);
	RELEASE_TO_NIL(pickerCancelCallback);
}

-(void)dispatchCallback:(NSArray*)args
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	NSString *type = [args objectAtIndex:0];
	id object = [args objectAtIndex:1];
	id listener = [args objectAtIndex:2];
	// we have to give our modal picker view time to 
	// dismiss with animation or if you do anything in a callback that 
	// attempt to also touch a modal controller, you'll get into deep doodoo
	// wait for the picker to dismiss with animation
	[NSThread sleepForTimeInterval:0.5];
	[self _fireEventToListener:type withObject:object listener:listener thisObject:nil];
	[pool release];
}

-(void)sendPickerError:(int)code
{
	id listener = [[pickerErrorCallback retain] autorelease];
	[self destroyPicker];
	if (listener!=nil)
	{
		NSDictionary *event = [TiUtils dictionaryWithCode:code message:nil];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error",event,listener,nil]];
	}
}

-(void)sendPickerCancel
{
	id listener = [[pickerCancelCallback retain] autorelease];
	[self destroyPicker];
	if (listener!=nil)
	{
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:-1 message:@"The user cancelled the picker"];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"cancel",event,listener,nil]];
	}
}

-(void)sendPickerSuccess:(id)event
{
	id listener = [[pickerSuccessCallback retain] autorelease];
	if (autoHidePicker)
	{
		[self destroyPicker];
	}
	if (listener!=nil)
	{
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"success",event,listener,nil]];
	}
}

-(void)commonPickerSetup:(NSDictionary*)args
{
	if (args!=nil) {
		pickerSuccessCallback = [args objectForKey:@"success"];
		ENSURE_TYPE_OR_NIL(pickerSuccessCallback,KrollCallback);
		[pickerSuccessCallback retain];
		
		pickerErrorCallback = [args objectForKey:@"error"];
		ENSURE_TYPE_OR_NIL(pickerErrorCallback,KrollCallback);
		[pickerErrorCallback retain];
		
		pickerCancelCallback = [args objectForKey:@"cancel"];
		ENSURE_TYPE_OR_NIL(pickerCancelCallback,KrollCallback);
		[pickerCancelCallback retain];
		
		// we use this to determine if we should hide the camera after taking 
		// a picture/video -- you can programmatically take multiple pictures
		// and use your own controls so this allows you to control that
		// (similarly for ipod library picking)
		autoHidePicker = [TiUtils boolValue:@"autohide" properties:args def:YES];
		
		animatedPicker = [TiUtils boolValue:@"animated" properties:args def:YES];
	}
}

-(void)displayModalPicker:(UIViewController*)picker_ settings:(NSDictionary*)args
{
    TiApp * tiApp = [TiApp app];
    if ([TiUtils isIPad]==NO) {
        [tiApp showModalController:picker_ animated:animatedPicker];
    }
    else {
        RELEASE_TO_NIL(popover);
        TiViewProxy* popoverViewProxy = [args objectForKey:@"popoverView"];
        
        if (![popoverViewProxy isKindOfClass:[TiViewProxy class]]) {
            popoverViewProxy = nil;
        }
        
        self.popoverView = popoverViewProxy;
        arrowDirection = [TiUtils intValue:@"arrowDirection" properties:args def:UIPopoverArrowDirectionAny];
        
        TiThreadPerformOnMainThread(^{
            if (![TiUtils isIOS8OrGreater]) {
                [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(updatePopover:) name:UIApplicationWillChangeStatusBarOrientationNotification object:nil];
            }
            [self updatePopoverNow:picker_];
        }, YES);
	}
}

-(void)updatePopover:(NSNotification *)notification
{
    if (popover) {
        [self performSelector:@selector(updatePopoverNow:) withObject:nil afterDelay:[[UIApplication sharedApplication] statusBarOrientationAnimationDuration] inModes:[NSArray arrayWithObject:NSRunLoopCommonModes]];
    }
}

-(void)updatePopoverNow:(UIViewController*)picker_
{
    if ([TiUtils isIOS8OrGreater]) {
        UIViewController* theController = picker_;
        [theController setModalPresentationStyle:UIModalPresentationPopover];
        UIPopoverPresentationController* thePresenter = [theController popoverPresentationController];
        [thePresenter setPermittedArrowDirections:arrowDirection];
        [thePresenter setDelegate:self];
        [[TiApp app] showModalController:theController animated:animatedPicker];
        return;
    }
    
    if (popover == nil) {
        popover = [[UIPopoverController alloc] initWithContentViewController:picker_];
        [(UIPopoverController*)popover setDelegate:self];
    }
    
    if ( (self.popoverView != nil) && ([self.popoverView isUsingBarButtonItem]) ) {
        UIBarButtonItem * ourButtonItem = [popoverView barButtonItem];
        @try {
            /*
             *	Because buttonItems may or many not have a view, there is no way for us
             *	to know beforehand if the request is an invalid one.
             */
            [popover presentPopoverFromBarButtonItem: ourButtonItem permittedArrowDirections:arrowDirection animated:animatedPicker];
        }
        @catch (NSException *exception) {
            DebugLog(@"[WARN] Popover requested on view not attached to current window.");
        }
        return;
    }
    
    UIView* theView = nil;
    CGRect popoverRect = CGRectZero;
    if (self.popoverView != nil) {
        theView = [self.popoverView view];
        popoverRect = [theView bounds];
    } else {
        theView = [[[[TiApp app] controller] topPresentedController] view];
        popoverRect = [theView bounds];
        if (popoverRect.size.height > 50) {
            popoverRect.size.height = 50;
        }
    }
    
    if ([theView window] == nil) {
        DebugLog(@"[WARN] Unable to display picker; view is not attached to the current window");
    }
    [popover presentPopoverFromRect:popoverRect inView:theView permittedArrowDirections:arrowDirection animated:animatedPicker];
}

-(void)closeModalPicker:(UIViewController*)picker_
{

	if (popover)
	{
		[(UIPopoverController*)popover dismissPopoverAnimated:animatedPicker];
		RELEASE_TO_NIL(popover);
	}
	else
	{
		[[TiApp app] hideModalController:picker_ animated:animatedPicker];
		[[TiApp controller] repositionSubviews];
	}
}

#pragma mark UIPopoverControllerDelegate
- (void)popoverControllerDidDismissPopover:(UIPopoverController *)popoverController
{
    if([popoverController contentViewController] == musicPicker) {
        RELEASE_TO_NIL(musicPicker);
    }
    
    RELEASE_TO_NIL(popover);
    [self sendPickerCancel];
    //Unregister for interface change notification
    [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillChangeStatusBarOrientationNotification object:nil];
}

#pragma mark UIPopoverPresentationControllerDelegate
- (void)prepareForPopoverPresentation:(UIPopoverPresentationController *)popoverPresentationController
{
    if (self.popoverView != nil) {
        if ([self.popoverView supportsNavBarPositioning] && [self.popoverView isUsingBarButtonItem]) {
            UIBarButtonItem* theItem = [self.popoverView barButtonItem];
            if (theItem != nil) {
                popoverPresentationController.barButtonItem = [self.popoverView barButtonItem];
                return;
            }
        }
        
        UIView* view = [self.popoverView view];
        if (view != nil && (view.window != nil)) {
            popoverPresentationController.sourceView = view;
            popoverPresentationController.sourceRect = [view bounds];
            return;
        }
    }
    
    //Fell through.
    UIViewController* presentingController = [popoverPresentationController presentingViewController];
    popoverPresentationController.sourceView = [presentingController view];
    CGRect viewrect = [[presentingController view] bounds];
    if (viewrect.size.height > 50) {
        viewrect.size.height = 50;
    }
    popoverPresentationController.sourceRect = viewrect;
}

- (void)popoverPresentationController:(UIPopoverPresentationController *)popoverPresentationController willRepositionPopoverToRect:(inout CGRect *)rect inView:(inout UIView **)view
{
    //This will never be called when using bar button item
    UIView* theSourceView = *view;
    BOOL canUseSourceRect = (theSourceView == self.popoverView);
    rect->origin = CGPointMake(theSourceView.bounds.origin.x, theSourceView.bounds.origin.y);
    
    if (!canUseSourceRect && theSourceView.bounds.size.height > 50) {
        rect->size = CGSizeMake(theSourceView.bounds.size.width, 50);
    } else {
        rect->size = CGSizeMake(theSourceView.bounds.size.width, theSourceView.bounds.size.height);
    }
    
    popoverPresentationController.sourceRect = *rect;
}

- (void)popoverPresentationControllerDidDismissPopover:(UIPopoverPresentationController *)popoverPresentationController
{
    if([popoverPresentationController presentedViewController] == musicPicker) {
        RELEASE_TO_NIL(musicPicker);
    }
    
    [self sendPickerCancel];
}

#pragma mark MPMediaPickerControllerDelegate
- (void)mediaPicker:(MPMediaPickerController*)mediaPicker_ didPickMediaItems:(MPMediaItemCollection*)collection
{
	if (autoHidePicker) {
		[self closeModalPicker:musicPicker];
	}
	
	TiAudioItem* representative = [[[TiAudioItem alloc] _initWithPageContext:[self pageContext] item:[collection representativeItem]] autorelease];
	NSNumber* mediaTypes = [NSNumber numberWithUnsignedInteger:[collection mediaTypes]];
	NSMutableArray* items = [NSMutableArray array];
	
	for (MPMediaItem* item in [collection items]) {
		TiAudioItem* newItem = [[[TiAudioItem alloc] _initWithPageContext:[self pageContext] item:item] autorelease];
		[items addObject:newItem];
	}
	
	NSMutableDictionary* picked = [TiUtils dictionaryWithCode:0 message:nil];
	[picked setObject:representative forKey:@"representative"];
	[picked setObject:mediaTypes forKey:@"types"];
	[picked setObject:items forKey:@"items"];
	
	[self sendPickerSuccess:picked];
}

- (void)mediaPickerDidCancel:(MPMediaPickerController *)mediaPicker_
{
	[self closeModalPicker:musicPicker];
	[self sendPickerCancel];
}

#pragma mark Event Listener Management

-(void)audioRouteChanged:(NSNotification*)note
{
    NSDictionary *event = [note userInfo];
    [self fireEvent:@"routechange" withObject:event];
}

-(void)audioVolumeChanged:(NSNotification*)note
{
    NSDictionary* userInfo = [note userInfo];
    if (userInfo != nil) {
        [self fireEvent:@"volume" withObject:userInfo];
    } else {
        NSMutableDictionary *event = [NSMutableDictionary dictionary];
        [event setObject:[self volume] forKey:@"volume"];
        [self fireEvent:@"volume" withObject:event];
    }
}

-(void)_listenerAdded:(NSString *)type count:(NSInteger)count
{
    if (count == 1 && [type isEqualToString:@"routechange"])
    {
        WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe
        [[TiAudioSession sharedSession] startAudioSession]; // Have to start a session to get a listener
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(audioRouteChanged:) name:kTiAudioSessionRouteChange object:[TiAudioSession sharedSession]];
    }
    else if (count == 1 && [type isEqualToString:@"volume"])
    {
        WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
        [[TiAudioSession sharedSession] startAudioSession]; // Have to start a session to get a listener
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(audioVolumeChanged:) name:kTiAudioSessionVolumeChange object:[TiAudioSession sharedSession]];
    }
    else if (count == 1 && [type isEqualToString:@"recordinginput"])
    {
        DebugLog(@"[WARN] This event is no longer supported by the MediaModule. Check the inputs property fo the currentRoute property to check if an input line is available");
    }
    else if (count == 1 && [type isEqualToString:@"linechange"])
    {
        DebugLog(@"[WARN] This event is no longer supported by the MediaModule. Listen for the routechange event instead");
    }
}

-(void)_listenerRemoved:(NSString *)type count:(NSInteger)count
{
    if (count == 0 && [type isEqualToString:@"routechange"])
    {
        WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
        [[TiAudioSession sharedSession] stopAudioSession];
        [[NSNotificationCenter defaultCenter] removeObserver:self name:kTiAudioSessionRouteChange object:[TiAudioSession sharedSession]];
    }
    else if (count == 0 && [type isEqualToString:@"volume"])
    {
        WARN_IF_BACKGROUND_THREAD_OBJ;	//NSNotificationCenter is not threadsafe!
        [[TiAudioSession sharedSession] stopAudioSession];
        [[NSNotificationCenter defaultCenter] removeObserver:self name:kTiAudioSessionVolumeChange object:[TiAudioSession sharedSession]];
    }
}


@end

#endif