/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#ifdef USE_TI_MEDIA

#import "MediaModule.h"
#import "TiUtils.h"
#import "TiBlob.h"
#import "TiFile.h"
#import "TiApp.h"
#import "Mimetypes.h"
#import "TiViewProxy.h"
#import "Ti2DMatrix.h"
#import "TouchCapturingWindow.h"

#import <AVFoundation/AVMediaFormat.h>
#import <AVFoundation/AVAsset.h>
#import <AVFoundation/AVAssetExportSession.h>
#import <MediaPlayer/MediaPlayer.h>
#import <MobileCoreServices/UTCoreTypes.h>
#import <QuartzCore/QuartzCore.h>
#import <AVFoundation/AVFoundation.h>

#import <UIKit/UIPopoverController.h>
// by default, we want to make the camera fullscreen and 
// these transform values will scale it when we have our own overlay

enum
{
	MediaModuleErrorUnknown,
	MediaModuleErrorBusy,
	MediaModuleErrorNoCamera,
	MediaModuleErrorNoVideo
};

@interface TiImagePickerController:UIImagePickerController {
@private
    BOOL autoRotate;
}
@end

@implementation TiImagePickerController

-(id)initWithProperties:(NSDictionary*)dict_
{
    if (self = [self init])
    {
        autoRotate = [TiUtils boolValue:@"autorotate" properties:dict_ def:YES];
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    self.edgesForExtendedLayout = UIRectEdgeNone;
    [self prefersStatusBarHidden];
    [self setNeedsStatusBarAppearanceUpdate];
}

- (BOOL)shouldAutorotate
{
    return autoRotate;
}

-(BOOL)prefersStatusBarHidden
{
    return YES;
}

-(UIViewController *)childViewControllerForStatusBarHidden
{
    return nil;
}

- (UIViewController *)childViewControllerForStatusBarStyle
{
    return nil;
}

@end

@implementation MediaModule
@synthesize popoverView;

-(void)dealloc
{
    [self destroyPicker];
//    RELEASE_TO_NIL(systemMusicPlayer);
//    RELEASE_TO_NIL(appMusicPlayer);
    RELEASE_TO_NIL(popoverView);
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    [super dealloc];
}

#pragma mark Public Properties
-(NSString*)apiName
{
    return @"Ti.Media";
}

MAKE_SYSTEM_UINT(CAMERA_AUTHORIZATION_AUTHORIZED, AVAuthorizationStatusAuthorized);
MAKE_SYSTEM_UINT(CAMERA_AUTHORIZATION_DENIED, AVAuthorizationStatusDenied);
MAKE_SYSTEM_UINT(CAMERA_AUTHORIZATION_RESTRICTED, AVAuthorizationStatusRestricted);
MAKE_SYSTEM_UINT(CAMERA_AUTHORIZATION_NOT_DETERMINED, AVAuthorizationStatusNotDetermined);
//Constants for Camera
MAKE_SYSTEM_PROP(CAMERA_FRONT,UIImagePickerControllerCameraDeviceFront);
MAKE_SYSTEM_PROP(CAMERA_REAR,UIImagePickerControllerCameraDeviceRear);

MAKE_SYSTEM_PROP(CAMERA_FLASH_OFF,UIImagePickerControllerCameraFlashModeOff);
MAKE_SYSTEM_PROP(CAMERA_FLASH_AUTO,UIImagePickerControllerCameraFlashModeAuto);
MAKE_SYSTEM_PROP(CAMERA_FLASH_ON,UIImagePickerControllerCameraFlashModeOn);

//Error constants for MediaModule
MAKE_SYSTEM_PROP(UNKNOWN_ERROR,MediaModuleErrorUnknown);
MAKE_SYSTEM_PROP(DEVICE_BUSY,MediaModuleErrorBusy);
MAKE_SYSTEM_PROP(NO_CAMERA,MediaModuleErrorNoCamera);
MAKE_SYSTEM_PROP(NO_VIDEO,MediaModuleErrorNoVideo);

//Constants for mediaTypes in showCamera
MAKE_SYSTEM_STR(MEDIA_TYPE_VIDEO,kUTTypeMovie);
MAKE_SYSTEM_STR(MEDIA_TYPE_PHOTO,kUTTypeImage);

//Constants for videoQuality for Video Editing
MAKE_SYSTEM_PROP(QUALITY_HIGH,UIImagePickerControllerQualityTypeHigh);
MAKE_SYSTEM_PROP(QUALITY_MEDIUM,UIImagePickerControllerQualityTypeMedium);
MAKE_SYSTEM_PROP(QUALITY_LOW,UIImagePickerControllerQualityTypeLow);
MAKE_SYSTEM_PROP(QUALITY_640x480,UIImagePickerControllerQualityType640x480);

//Constants for MediaTypes in VideoPlayer
MAKE_SYSTEM_PROP(VIDEO_MEDIA_TYPE_NONE,MPMovieMediaTypeMaskNone);
MAKE_SYSTEM_PROP(VIDEO_MEDIA_TYPE_VIDEO,MPMovieMediaTypeMaskVideo);
MAKE_SYSTEM_PROP(VIDEO_MEDIA_TYPE_AUDIO,MPMovieMediaTypeMaskAudio);

//Constants for VideoPlayer complete event
MAKE_SYSTEM_PROP(VIDEO_FINISH_REASON_PLAYBACK_ENDED,MPMovieFinishReasonPlaybackEnded);
MAKE_SYSTEM_PROP(VIDEO_FINISH_REASON_PLAYBACK_ERROR,MPMovieFinishReasonPlaybackError);
MAKE_SYSTEM_PROP(VIDEO_FINISH_REASON_USER_EXITED,MPMovieFinishReasonUserExited);

//Constants for VideoPlayer mediaControlStyle
MAKE_SYSTEM_PROP(VIDEO_CONTROL_DEFAULT, MPMovieControlStyleDefault);
MAKE_SYSTEM_PROP(VIDEO_CONTROL_NONE,MPMovieControlStyleNone);
MAKE_SYSTEM_PROP(VIDEO_CONTROL_EMBEDDED,MPMovieControlStyleEmbedded);
MAKE_SYSTEM_PROP(VIDEO_CONTROL_FULLSCREEN,MPMovieControlStyleFullscreen);

// Deprecated old-school video control modes, mapped to the new values
-(NSNumber*)VIDEO_CONTROL_VOLUME_ONLY
{
    DEPRECATED_REPLACED(@"Media.VIDEO_CONTROL_VOLUME_ONLY", @"1.8.0", @"Ti.Media.VIDEO_CONTROL_EMBEDDED");
    return [self VIDEO_CONTROL_EMBEDDED];
}
-(NSNumber*)VIDEO_CONTROL_HIDDEN
{
    return [self VIDEO_CONTROL_NONE];
}

//Constants for VideoPlayer scalingMode
MAKE_SYSTEM_PROP(VIDEO_SCALING_NONE,MPMovieScalingModeNone);
MAKE_SYSTEM_PROP(VIDEO_SCALING_ASPECT_FIT,MPMovieScalingModeAspectFit);
MAKE_SYSTEM_PROP(VIDEO_SCALING_ASPECT_FILL,MPMovieScalingModeAspectFill);
MAKE_SYSTEM_PROP(VIDEO_SCALING_MODE_FILL,MPMovieScalingModeFill);

//Constants for VideoPlayer sourceType
MAKE_SYSTEM_PROP(VIDEO_SOURCE_TYPE_UNKNOWN,MPMovieSourceTypeUnknown);
MAKE_SYSTEM_PROP(VIDEO_SOURCE_TYPE_FILE,MPMovieSourceTypeFile);
MAKE_SYSTEM_PROP(VIDEO_SOURCE_TYPE_STREAMING,MPMovieSourceTypeStreaming);

//Constants for VideoPlayer playbackState
MAKE_SYSTEM_PROP(VIDEO_PLAYBACK_STATE_STOPPED,MPMoviePlaybackStateStopped);
MAKE_SYSTEM_PROP(VIDEO_PLAYBACK_STATE_PLAYING,MPMoviePlaybackStatePlaying);
MAKE_SYSTEM_PROP(VIDEO_PLAYBACK_STATE_PAUSED,MPMoviePlaybackStatePaused);
MAKE_SYSTEM_PROP(VIDEO_PLAYBACK_STATE_INTERRUPTED,MPMoviePlaybackStateInterrupted);
MAKE_SYSTEM_PROP(VIDEO_PLAYBACK_STATE_SEEKING_FORWARD,MPMoviePlaybackStateSeekingForward);
MAKE_SYSTEM_PROP(VIDEO_PLAYBACK_STATE_SEEKING_BACKWARD,MPMoviePlaybackStateSeekingBackward);

//Constants for VideoPlayer loadState
MAKE_SYSTEM_PROP(VIDEO_LOAD_STATE_UNKNOWN,MPMovieLoadStateUnknown);
MAKE_SYSTEM_PROP(VIDEO_LOAD_STATE_PLAYABLE,MPMovieLoadStatePlayable);
MAKE_SYSTEM_PROP(VIDEO_LOAD_STATE_PLAYTHROUGH_OK,MPMovieLoadStatePlaythroughOK);
MAKE_SYSTEM_PROP(VIDEO_LOAD_STATE_STALLED,MPMovieLoadStateStalled);

//Constants for VideoPlayer repeateMode
MAKE_SYSTEM_PROP(VIDEO_REPEAT_MODE_NONE,MPMovieRepeatModeNone);
MAKE_SYSTEM_PROP(VIDEO_REPEAT_MODE_ONE,MPMovieRepeatModeOne);

//Other Constants
MAKE_SYSTEM_PROP(VIDEO_TIME_OPTION_NEAREST_KEYFRAME,MPMovieTimeOptionNearestKeyFrame);
MAKE_SYSTEM_PROP(VIDEO_TIME_OPTION_EXACT,MPMovieTimeOptionExact);


-(NSArray*)availableCameraMediaTypes
{
    NSArray* mediaSourceTypes = [UIImagePickerController availableMediaTypesForSourceType: UIImagePickerControllerSourceTypeCamera];
    return mediaSourceTypes==nil ? [NSArray arrayWithObject:(NSString*)kUTTypeImage] : mediaSourceTypes;
}

-(NSArray*)availablePhotoMediaTypes
{
    NSArray* photoSourceTypes = [UIImagePickerController availableMediaTypesForSourceType: UIImagePickerControllerSourceTypePhotoLibrary];
    return photoSourceTypes==nil ? [NSArray arrayWithObject:(NSString*)kUTTypeImage] : photoSourceTypes;
}

-(NSArray*)availablePhotoGalleryMediaTypes
{
    NSArray* albumSourceTypes = [UIImagePickerController availableMediaTypesForSourceType: UIImagePickerControllerSourceTypeSavedPhotosAlbum];
    return albumSourceTypes==nil ? [NSArray arrayWithObject:(NSString*)kUTTypeImage] : albumSourceTypes;
}

-(NSArray*)availableCameras
{
    NSMutableArray* types = [NSMutableArray arrayWithCapacity:2];
    if ([UIImagePickerController isCameraDeviceAvailable:UIImagePickerControllerCameraDeviceFront])
    {
        [types addObject:NUMINT(UIImagePickerControllerCameraDeviceFront)];
    }
    if ([UIImagePickerController isCameraDeviceAvailable:UIImagePickerControllerCameraDeviceRear])
    {
        [types addObject:NUMINT(UIImagePickerControllerCameraDeviceRear)];
    }
    return types;
}

-(id)cameraFlashMode
{
    if (picker!=nil)
    {
        return NUMINT([picker cameraFlashMode]);
    }
    return NUMINT(UIImagePickerControllerCameraFlashModeAuto);
}

-(void)setCameraFlashMode:(id)args
{
    // Return nothing
    ENSURE_SINGLE_ARG(args,NSNumber);
    ENSURE_UI_THREAD(setCameraFlashMode,args);
    
    if (picker!=nil)
    {
        [picker setCameraFlashMode:[TiUtils intValue:args]];
    }
}

-(NSNumber*)isCameraSupported
{
    return NUMBOOL([UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]);
}

/**
 Check if camera is authorized, only available for >= iOS 7
 **/
-(NSNumber*)cameraAuthorizationStatus
{
    if (![TiUtils isIOS7OrGreater]) {
        return nil;
    }
    AVAuthorizationStatus authStatus = [AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo];
    return NUMUINT(authStatus);
}

#pragma mark Public Methods

-(NSNumber*)isMediaTypeSupported:(id)args
{
    ENSURE_ARG_COUNT(args,2);
    
    NSString *media = [[TiUtils stringValue:[args objectAtIndex:0]] lowercaseString];
    NSString *type = [[TiUtils stringValue:[args objectAtIndex:1]] lowercaseString];
    
    NSArray *array = nil;
    
    if ([media isEqualToString:@"camera"])
    {
        array = [self availableCameraMediaTypes];
    }
    else if ([media isEqualToString:@"photo"])
    {
        array = [self availablePhotoMediaTypes];
    }
    else if ([media isEqualToString:@"photogallery"])
    {
        array = [self availablePhotoGalleryMediaTypes];
    }
    if (array!=nil)
    {
        for (NSString* atype in array)
        {
            if ([[atype lowercaseString] isEqualToString:type])
            {
                return NUMBOOL(YES);
            }
        }
    }
    return NUMBOOL(NO);
}

+(UIImage*) takeScreenshotWithScale:(CGFloat)scale
{
    // Create a graphics context with the target size
    
    CGSize imageSize = [[UIScreen mainScreen] bounds].size;
    if ([TiUtils isRetinaDisplay])
    {
        scale*=2;
        
    }
    
    UIInterfaceOrientation orientation = [UIApplication sharedApplication].statusBarOrientation;
    if (UIInterfaceOrientationIsPortrait(orientation)) {
        imageSize = [UIScreen mainScreen].bounds.size;
    } else {
        imageSize = CGSizeMake([UIScreen mainScreen].bounds.size.height, [UIScreen mainScreen].bounds.size.width);
    }
    UIGraphicsBeginImageContextWithOptions(imageSize, NO, scale);
    
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    // Iterate over every window from back to front
    for (UIWindow *window in [[UIApplication sharedApplication] windows])
    {
        if (IS_OF_CLASS(window, TouchCapturingWindow) && (![window respondsToSelector:@selector(screen)] || [window screen] == [UIScreen mainScreen]))
        {
            CGSize size = [window bounds].size;
            CGPoint center = [window center];
            CGPoint anchorPoint = [[window layer] anchorPoint];
            // -renderInContext: renders in the coordinate space of the layer,
            // so we must first apply the layer's geometry to the graphics context
            CGContextSaveGState(context);
            // Center the context around the window's anchor point
            CGContextTranslateCTM(context, center.x, center.y);
            // Apply the window's transform about the anchor point
            CGContextConcatCTM(context, [window transform]);
            // Offset by the portion of the bounds left of and above the anchor point
            CGContextTranslateCTM(context,
                                  -size.width * anchorPoint.x,
                                  -size.height * anchorPoint.y);
            if (orientation == UIInterfaceOrientationLandscapeLeft) {
                CGContextRotateCTM(context, M_PI_2);
                CGContextTranslateCTM(context, 0, -imageSize.width);
            } else if (orientation == UIInterfaceOrientationLandscapeRight) {
                CGContextRotateCTM(context, -M_PI_2);
                CGContextTranslateCTM(context, -imageSize.height, 0);
            } else if (orientation == UIInterfaceOrientationPortraitUpsideDown) {
                CGContextRotateCTM(context, M_PI);
                CGContextTranslateCTM(context, -imageSize.width, -imageSize.height);
            }
            // Render the layer hierarchy to the current context
            if ([window respondsToSelector:@selector(drawViewHierarchyInRect:afterScreenUpdates:)]) {
                [window drawViewHierarchyInRect:[window bounds] afterScreenUpdates:NO];
            } else {
                [[window layer] renderInContext:context];
            }
            
            // Restore the context
            CGContextRestoreGState(context);
        }
    }
    
    // Retrieve the screenshot image
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    
    UIGraphicsEndImageContext();
    
//    UIInterfaceOrientation windowOrientation = [[UIApplication sharedApplication] statusBarOrientation];
//    switch (windowOrientation) {
//        case UIInterfaceOrientationPortraitUpsideDown:
//            image = [UIImage imageWithCGImage:[image CGImage] scale:[image scale] orientation:UIImageOrientationDown];
//            break;
//        case UIInterfaceOrientationLandscapeLeft:
//            image = [UIImage imageWithCGImage:[image CGImage] scale:[image scale] orientation:UIImageOrientationRight];
//            break;
//        case UIInterfaceOrientationLandscapeRight:
//            image = [UIImage imageWithCGImage:[image CGImage] scale:[image scale] orientation:UIImageOrientationLeft];
//            break;
//        default:
//            break;
//    }
    return image;
}

-(TiBlob*)takeScreenshot:(id)args
{
    KrollCallback *callback = nil;
    float scale = 0.0f;
    
    id obj = nil;
    if( [args count] > 0) {
        obj = [args objectAtIndex:0];
        
        if (obj == [NSNull null]) {
            obj = nil;
        }
        
        if( [args count] > 1) {
            scale = [TiUtils floatValue:[args objectAtIndex:1] def:0.0f];
        }
    }
    ENSURE_SINGLE_ARG_OR_NIL(obj,KrollCallback);
    callback = (KrollCallback*)obj;
    TiBlob *blob = [[[TiBlob alloc] init] autorelease];
    
    TiThreadPerformBlockOnMainThread(^{
        // Retrieve the screenshot image
        UIImage *image = [MediaModule takeScreenshotWithScale:scale];
        [blob setImage:image];
        [blob setMimeType:@"image/png" type:TiBlobTypeImage];
        NSDictionary *event = [NSDictionary dictionaryWithObject:blob forKey:@"image"];
        [self _fireEventToListener:@"screenshot" withObject:event listener:callback thisObject:nil];
    }, (callback==nil));
    return blob;
}

-(void)saveToPhotoGallery:(id)arg
{
    ENSURE_UI_THREAD(saveToPhotoGallery,arg);
    NSObject* image = [arg objectAtIndex:0];
    ENSURE_TYPE(image, NSObject)
    
    NSDictionary* saveCallbacks=nil;
    if ([arg count] > 1) {
        saveCallbacks = [arg objectAtIndex:1];
        ENSURE_TYPE(saveCallbacks, NSDictionary);
        KrollCallback* successCallback = [saveCallbacks valueForKey:@"success"];
        ENSURE_TYPE_OR_NIL(successCallback, KrollCallback);
        KrollCallback* errorCallback = [saveCallbacks valueForKey:@"error"];
        ENSURE_TYPE_OR_NIL(errorCallback, KrollCallback);
    }
    
    if ([image isKindOfClass:[TiBlob class]])
    {
        TiBlob *blob = (TiBlob*)image;
        NSString *mime = [blob mimeType];
        
        if (mime==nil || [mime hasPrefix:@"image/"])
        {
            UIImage * savedImage = [blob image];
            if (savedImage == nil) return;
            UIImageWriteToSavedPhotosAlbum(savedImage, self, @selector(saveCompletedForImage:error:contextInfo:), [saveCallbacks retain]);
        }
        else if ([mime hasPrefix:@"video/"])
        {
            NSString* filePath;
            switch ([blob type]) {
                case TiBlobTypeFile: {
                    filePath = [blob path];
                    break;
                }
                case TiBlobTypeData: {
                    // In this case, we need to write the blob data to a /tmp file and then load it.
                    NSArray* typeinfo = [mime componentsSeparatedByString:@"/"];
                    TiFile* tempFile = [TiUtils createTempFile:[typeinfo objectAtIndex:1]];
                    filePath = [tempFile path];
                    
                    NSError* error = nil;
                    [blob writeTo:filePath error:&error];
                    
                    if (error != nil) {
                        NSString * message = [NSString stringWithFormat:@"problem writing to temporary file %@: %@", filePath, [TiUtils messageFromError:error]];
                        NSMutableDictionary * event = [TiUtils dictionaryWithCode:[error code] message:message];
                        [self dispatchCallback:[NSArray arrayWithObjects:@"error",event,[saveCallbacks valueForKey:@"error"],nil]];
                        return;
                    }
                    
                    // Have to keep the temp file from being deleted when we leave scope, so add it to the userinfo so it can be cleaned up there
                    [saveCallbacks setValue:tempFile forKey:@"tempFile"];
                    break;
                }
                default: {
                    NSMutableDictionary * event = [TiUtils dictionaryWithCode:-1 message:@"invalid media format: MIME type was video/, but data is image"];
                    [self dispatchCallback:[NSArray arrayWithObjects:@"error",event,[saveCallbacks valueForKey:@"error"],nil]];
                    return;
                }
            }
            UISaveVideoAtPathToSavedPhotosAlbum(filePath, self, @selector(saveCompletedForVideo:error:contextInfo:), [saveCallbacks retain]);
        }
    }
    else if ([image isKindOfClass:[TiFile class]])
    {
        TiFile *file = (TiFile*)image;
        NSString *mime = [Mimetypes mimeTypeForExtension:[file path]];
        if (mime == nil || [mime hasPrefix:@"image/"])
        {
            NSData *data = [NSData dataWithContentsOfFile:[file path]];
            UIImage *image = [[[UIImage alloc] initWithData:data] autorelease];
            UIImageWriteToSavedPhotosAlbum(image, self, @selector(saveCompletedForImage:error:contextInfo:), [saveCallbacks retain]);
        }
        else if ([mime hasPrefix:@"video/"])
        {
            UISaveVideoAtPathToSavedPhotosAlbum([file path], self, @selector(saveCompletedForVideo:error:contextInfo:), [saveCallbacks retain]);
        }
    }
    else
    {
        KrollCallback* errorCallback = [saveCallbacks valueForKey:@"error"];
        if (errorCallback != nil) {
            NSMutableDictionary * event = [TiUtils dictionaryWithCode:-1 message:[NSString stringWithFormat:@"invalid media type: Exepcted either TiBlob or TiFile, was: %@",JavascriptNameForClass([image class])]];
            [self dispatchCallback:[NSArray arrayWithObjects:@"error",event,errorCallback,nil]];
        } else {
            [self throwException:@"invalid media type"
                       subreason:[NSString stringWithFormat:@"expected either TiBlob or TiFile, was: %@",JavascriptNameForClass([image class])]
                        location:CODELOCATION];
        }
    }
}


/**
 Camera & Video Capture methods
 **/
-(void)showCamera:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    if (![NSThread isMainThread]) {
        [self rememberProxy:[args objectForKey:@"overlay"]];
        TiThreadPerformOnMainThread(^{[self showCamera:args];},NO);
        return;
    }
    
    [self showPicker:args isCamera:YES];
}

-(void)hideCamera:(id)args
{
    [self destroyPickerCallbacks];
    //Hopefully, if we remove the callbacks before going to the main thread, we may reduce deadlock.
    ENSURE_UI_THREAD(hideCamera,args);
    if (picker!=nil)
    {
        if (cameraView != nil) {
            [cameraView windowWillClose];
        }
        if (popover != nil) {
            [popover dismissPopoverAnimated:animatedPicker];
            RELEASE_TO_NIL(popover);
            
            //Unregister for interface change notification
            [[NSNotificationCenter defaultCenter] removeObserver:self name:UIApplicationWillChangeStatusBarOrientationNotification object:nil];
        }
        else {
            [[TiApp app] hideModalController:picker animated:animatedPicker];
            [[TiApp controller] repositionSubviews];
        }
        if (cameraView != nil) {
            [cameraView windowDidClose];
            [self forgetProxy:cameraView];
            RELEASE_TO_NIL(cameraView);
        }
        [self destroyPicker];
    }
}

-(void)takePicture:(id)args
{
    // must have a picker, doh
    if (picker==nil)
    {
        [self throwException:@"invalid state" subreason:nil location:CODELOCATION];
    }
    ENSURE_UI_THREAD(takePicture,args);
    [picker takePicture];
}

-(void)startVideoCapture:(id)args
{
    // Return nothing
    ENSURE_UI_THREAD(startVideoCapture,args);
    // must have a picker, doh
    if (picker==nil)
    {
        [self throwException:@"invalid state" subreason:nil location:CODELOCATION];
    }
    [picker startVideoCapture];
}

-(void)stopVideoCapture:(id)args
{
    ENSURE_UI_THREAD(stopVideoCapture,args);
    // must have a picker, doh
    if (picker!=nil)
    {
        [picker stopVideoCapture];
    }
}

-(void)switchCamera:(id)args
{
    ENSURE_SINGLE_ARG(args,NSNumber);
    ENSURE_UI_THREAD(switchCamera,args);
    
    // must have a picker, doh
    if (picker==nil)
    {
        [self throwException:@"invalid state" subreason:nil location:CODELOCATION];
    }
    [picker setCameraDevice:[TiUtils intValue:args]];
}

//Undocumented property
-(id)camera
{
    if (picker!=nil)
    {
        return NUMINT([picker cameraDevice]);
    }
    return NUMINT(UIImagePickerControllerCameraDeviceRear);
}

-(void)requestCameraAccess:(id)arg
{
    DEPRECATED_REPLACED(@"Media.requestCameraAccess", @"5.1.0", @"Media.requestCameraPermissions");

    [self requestCameraPermissions:arg];
}

//request camera access. for >= IOS7
-(void)requestCameraPermissions:(id)arg
{
    if (![TiUtils isIOS7OrGreater]) {
        return;
    }
    ENSURE_SINGLE_ARG(arg, KrollCallback);
    KrollCallback * callback = arg;
    TiThreadPerformOnMainThread(^(){
        [AVCaptureDevice requestAccessForMediaType:AVMediaTypeVideo completionHandler:^(BOOL granted){
            KrollEvent * invocationEvent = [[KrollEvent alloc] initWithCallback:callback
                                                                    eventObject:[TiUtils dictionaryWithCode:(granted ? 0 : 1) message:nil]
                                                                     thisObject:self];
            [[callback context] enqueue:invocationEvent];
            RELEASE_TO_NIL(invocationEvent);
        }];
    }, NO);
}

-(NSNumber*)hasCameraPermissions:(id)unused
{
    return NUMBOOL([AVCaptureDevice authorizationStatusForMediaType:AVMediaTypeVideo] == AVAuthorizationStatusAuthorized);
}

/**
 End Camera Support
 **/

-(void)openPhotoGallery:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    ENSURE_UI_THREAD(openPhotoGallery,args);
    [self showPicker:args isCamera:NO];
}

/**
 Video Editing Support
 **/
-(void)startVideoEditing:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    ENSURE_UI_THREAD(startVideoEditing,args);
    
    RELEASE_TO_NIL(editor);
    
    BOOL animated = [TiUtils boolValue:@"animated" properties:args def:YES];
    id media = [args objectForKey:@"media"];
    
    editorSuccessCallback = [args objectForKey:@"success"];
    ENSURE_TYPE_OR_NIL(editorSuccessCallback,KrollCallback);
    [editorSuccessCallback retain];
    
    editorErrorCallback = [args objectForKey:@"error"];
    ENSURE_TYPE_OR_NIL(editorErrorCallback,KrollCallback);
    [editorErrorCallback retain];
    
    editorCancelCallback = [args objectForKey:@"cancel"];
    ENSURE_TYPE_OR_NIL(pickerCancelCallback,KrollCallback);
    [editorCancelCallback retain];
    
    //TODO: check canEditVideoAtPath
    
    editor = [[UIVideoEditorController alloc] init];
    editor.delegate = self;
    editor.videoQuality = [TiUtils intValue:@"videoQuality" properties:args def:UIImagePickerControllerQualityTypeMedium];
    editor.videoMaximumDuration = [TiUtils doubleValue:@"videoMaximumDuration" properties:args def:600];
    
    if ([media isKindOfClass:[NSString class]])
    {
        NSURL *url = [TiUtils toURL:media proxy:self];
        editor.videoPath = [url path];
    }
    else if ([media isKindOfClass:[TiBlob class]])
    {
        TiBlob *blob = (TiBlob*)media;
        editor.videoPath = [blob path];
    }
    else if ([media isKindOfClass:[TiFile class]])
    {
        TiFile *file = (TiFile*)media;
        editor.videoPath = [file path];
    }
    else
    {
        RELEASE_TO_NIL(editor);
        NSLog(@"[ERROR] Unsupported video media: %@",[media class]);
        return;
    }
    
    TiApp * tiApp = [TiApp app];
    [tiApp showModalController:editor animated:animated];
}

-(void)stopVideoEditing:(id)args
{
    ENSURE_SINGLE_ARG_OR_NIL(args,NSDictionary);
    ENSURE_UI_THREAD(stopVideoEditing,args);
    
    if (editor!=nil)
    {
        BOOL animated = [TiUtils boolValue:@"animated" properties:args def:YES];
        [[TiApp app] hideModalController:editor animated:animated];
        RELEASE_TO_NIL(editor);
    }
}

/**
 Video Editing Support Ends
 **/

#pragma mark Internal Methods

-(void)destroyPickerCallbacks
{
	RELEASE_TO_NIL(editorSuccessCallback);
	RELEASE_TO_NIL(editorErrorCallback);
	RELEASE_TO_NIL(editorCancelCallback);
	RELEASE_TO_NIL(pickerSuccessCallback);
	RELEASE_TO_NIL(pickerErrorCallback);
	RELEASE_TO_NIL(pickerCancelCallback);
}

-(void)destroyPicker
{
	RELEASE_TO_NIL(popover);
	[self forgetProxy:cameraView];
    RELEASE_TO_NIL(cameraView);
	RELEASE_TO_NIL(editor);
	RELEASE_TO_NIL(editorSuccessCallback);
	RELEASE_TO_NIL(editorErrorCallback);
	RELEASE_TO_NIL(editorCancelCallback);
	RELEASE_TO_NIL(picker);
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

-(void)displayCamera:(UIViewController*)picker_
{
	TiApp * tiApp = [TiApp app];
	[tiApp showModalController:picker_ animated:animatedPicker];
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
    if (cameraView != nil) {
        [cameraView windowWillClose];
    }
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
    if (cameraView != nil) {
        [cameraView windowDidClose];
		[self forgetProxy:cameraView];
        RELEASE_TO_NIL(cameraView);
    }
}

-(void)showPicker:(NSDictionary*)args isCamera:(BOOL)isCamera
{
    if (picker!=nil)
    {
        [self sendPickerError:MediaModuleErrorBusy];
        return;
    }
    BOOL customPicker = isCamera;

    BOOL inPopOver = [TiUtils boolValue:@"inPopOver" properties:args def:NO] && isCamera && [TiUtils isIPad];

    if (customPicker) {
        customPicker = !inPopOver;
    }

    if (customPicker) {
        picker = [[TiImagePickerController alloc] initWithProperties:args];
    } else {
        picker = [[UIImagePickerController alloc] init];
    }

    [picker setDelegate:self];

    animatedPicker = YES;
    saveToRoll = NO;
    BOOL editable = NO;
    UIImagePickerControllerSourceType ourSource = (isCamera ? UIImagePickerControllerSourceTypeCamera : UIImagePickerControllerSourceTypePhotoLibrary);

    if (args!=nil)
    {
        [self commonPickerSetup:args];
        
        NSNumber * imageEditingObject = [args objectForKey:@"allowEditing"];
        saveToRoll = [TiUtils boolValue:@"saveToPhotoGallery" properties:args def:NO];
        
        editable = [TiUtils boolValue:[args objectForKey:@"allowEditing"] def:editable];

        [picker setAllowsEditing:editable];
        
        NSArray *sourceTypes = [UIImagePickerController availableMediaTypesForSourceType:ourSource];
        id types = [args objectForKey:@"mediaTypes"];
        
        BOOL movieRequired = NO;
        BOOL imageRequired = NO;
        
        if ([types isKindOfClass:[NSArray class]])
        {
            for (int c=0;c<[types count];c++)
            {
                if ([[types objectAtIndex:c] isEqualToString:(NSString*)kUTTypeMovie])
                {
                    movieRequired = YES;
                }
                else if ([[types objectAtIndex:c] isEqualToString:(NSString*)kUTTypeImage])
                {
                    imageRequired = YES;
                }
            }
            picker.mediaTypes = [NSArray arrayWithArray:types];
        }
        else if ([types isKindOfClass:[NSString class]])
        {
            if ([types isEqualToString:(NSString*)kUTTypeMovie] && ![sourceTypes containsObject:(NSString *)kUTTypeMovie])
            {
                // no movie type supported...
                [self sendPickerError:MediaModuleErrorNoVideo];
                return;
            }
            picker.mediaTypes = [NSArray arrayWithObject:types];
        }
        
        
        // if we require movie but not image and we don't support movie, bail...
        if (movieRequired == YES && imageRequired == NO && ![sourceTypes containsObject:(NSString *)kUTTypeMovie])
        {
            // no movie type supported...
            [self sendPickerError:MediaModuleErrorNoCamera];
            return ;
        }
        
        // introduced in 3.1
        id videoMaximumDuration = [args objectForKey:@"videoMaximumDuration"];
        if ([videoMaximumDuration respondsToSelector:@selector(doubleValue)] && [picker respondsToSelector:@selector(setVideoMaximumDuration:)])
        {
            [picker setVideoMaximumDuration:[videoMaximumDuration doubleValue]/1000];
        }
        id videoQuality = [args objectForKey:@"videoQuality"];
        if ([videoQuality respondsToSelector:@selector(doubleValue)] && [picker respondsToSelector:@selector(setVideoQuality:)])
        {
            [picker setVideoQuality:[videoQuality doubleValue]];
        }
    }

    // do this afterwards above so we can first check for video support

    if (![UIImagePickerController isSourceTypeAvailable:ourSource])
    {
        [self sendPickerError:MediaModuleErrorNoCamera];
        return;
    }
    [picker setSourceType:ourSource];

    // this must be done after we set the source type or you'll get an exception
    if (isCamera && ourSource == UIImagePickerControllerSourceTypeCamera)
    {
        // turn on/off camera controls - nice to turn off when you want to have your own UI
        [picker setShowsCameraControls:[TiUtils boolValue:@"showControls" properties:args def:YES]];
        
        // allow an overlay view
        TiViewProxy *cameraViewProxy = [args objectForKey:@"overlay"];
        if (cameraViewProxy!=nil)
        {
            ENSURE_TYPE(cameraViewProxy,TiViewProxy);
            cameraView = [cameraViewProxy retain];
			UIView *view = [cameraView getAndPrepareViewForOpening:[picker view].bounds];
            if (editable)
            {
                // turn off touch enablement if image editing is enabled since it will
                // interfere with editing
                [view performSelector:@selector(setTouchEnabled_:) withObject:NUMBOOL(NO)];
            }
            [picker setCameraOverlayView:view];
            [cameraView windowDidOpen];
            [cameraView layoutChildren:NO];
        }
        
        // allow a transform on the preview image
        id transform = [args objectForKey:@"transform"];
        if (transform!=nil)
        {
            ENSURE_TYPE(transform,Ti2DMatrix);
            [picker setCameraViewTransform:[transform matrix]];
        }
        else if (cameraView!=nil && customPicker)
        {
            //No transforms in popover
            CGSize screenSize = [[UIScreen mainScreen] bounds].size;
            if ([TiUtils isIOS8OrGreater]) {
                UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];
                if (!UIInterfaceOrientationIsPortrait(orientation)) {
                    screenSize = CGSizeMake(screenSize.height, screenSize.width);
                }
            }
            
            float cameraAspectRatio = 4.0 / 3.0;
            float camViewHeight = screenSize.width * cameraAspectRatio;
            float scale = screenSize.height/camViewHeight;
            
            CGAffineTransform translate = CGAffineTransformMakeTranslation(0, (screenSize.height - camViewHeight) / 2.0);
            picker.cameraViewTransform = CGAffineTransformScale(translate, scale, scale);
        }
    }

    if (isCamera) {
        if (inPopOver) {
            [self displayModalPicker:picker settings:args];
        }
        else {
            [self displayCamera:picker];
        }
    } else {
        [self displayModalPicker:picker settings:args];
    }
}

-(void)saveCompletedForImage:(UIImage*)image error:(NSError*)error contextInfo:(void*)contextInfo
{
	NSDictionary* saveCallbacks = (NSDictionary*)contextInfo;
	TiBlob* blob = [[[TiBlob alloc] initWithImage:image] autorelease];
	
	if (error != nil) {
		KrollCallback* errorCallback = [saveCallbacks objectForKey:@"error"];
		if (errorCallback != nil) {
			NSMutableDictionary * event = [TiUtils dictionaryWithCode:[error code] message:[TiUtils messageFromError:error]];
			[event setObject:blob forKey:@"image"];
			[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error",event,errorCallback,nil]];
		}
		return;
	}

	KrollCallback* successCallback = [saveCallbacks objectForKey:@"success"];
	if (successCallback != nil) {
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
		[event setObject:blob forKey:@"image"];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"success",event,successCallback,nil]];
	}
}

-(void)saveCompletedForVideo:(NSString*)path error:(NSError*)error contextInfo:(void*)contextInfo
{
	NSDictionary* saveCallbacks = (NSDictionary*)contextInfo;
	if (error != nil) {
		KrollCallback* errorCallback = [saveCallbacks objectForKey:@"error"];
		if (errorCallback != nil) {
			NSMutableDictionary * event = [TiUtils dictionaryWithCode:[error code] message:[TiUtils messageFromError:error]];
			[event setObject:path forKey:@"path"];
			[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error",event,errorCallback,nil]];			
		}
		return;
	}
	
	KrollCallback* successCallback = [saveCallbacks objectForKey:@"success"];
	if (successCallback != nil) {
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
		[event setObject:path forKey:@"path"];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"success",event,successCallback,nil]];
	}
    
    // This object was retained for use in this callback; release it.
    [saveCallbacks release]; 
}

-(void)handleTrimmedVideo:(NSURL*)theURL withDictionary:(NSDictionary*)dictionary
{
    TiBlob* media = [[[TiBlob alloc] initWithFile:[theURL path]] autorelease];
    NSMutableDictionary* eventDict = [NSMutableDictionary dictionaryWithDictionary:dictionary];
    [eventDict setObject:media forKey:@"media"];
    if (saveToRoll) {
        NSString *tempFilePath = [theURL absoluteString];
        UISaveVideoAtPathToSavedPhotosAlbum(tempFilePath, nil, nil, NULL);
    }
    
    [self sendPickerSuccess:eventDict];
}

#pragma mark UIPopoverControllerDelegate
- (void)popoverControllerDidDismissPopover:(UIPopoverController *)popoverController
{
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
    [self sendPickerCancel];
}

#pragma mark UIImagePickerControllerDelegate

- (void)imagePickerController:(UIImagePickerController *)picker_ didFinishPickingMediaWithInfo:(NSDictionary *)editingInfo
{
    if (autoHidePicker) {
        [self closeModalPicker:picker];
    }
	
    NSString *mediaType = [editingInfo objectForKey:UIImagePickerControllerMediaType];
    if (mediaType==nil) {
        mediaType = (NSString*)kUTTypeImage; // default to in case older OS
    }
    BOOL isVideo = [mediaType isEqualToString:(NSString*)kUTTypeMovie];
    
    NSURL *mediaURL = [editingInfo objectForKey:UIImagePickerControllerMediaURL];
	
    NSDictionary *cropRect = nil;
    TiBlob *media = nil;
    TiBlob *thumbnail = nil;

    BOOL imageWrittenToAlbum = NO;
	
    if (isVideo) {

        UIImage *thumbnailImage = [editingInfo objectForKey:UIImagePickerControllerOriginalImage];
        thumbnail = [[[TiBlob alloc] initWithImage:thumbnailImage] autorelease];

        if (picker.allowsEditing) {
            NSNumber *startTime = [editingInfo objectForKey:@"_UIImagePickerControllerVideoEditingStart"];
            NSNumber *endTime = [editingInfo objectForKey:@"_UIImagePickerControllerVideoEditingEnd"];
            
            if ( (startTime != nil) && (endTime != nil) ) {
                int startMilliseconds = ([startTime doubleValue] * 1000);
                int endMilliseconds = ([endTime doubleValue] * 1000);
                
                NSArray *paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
                NSString *documentsDirectory = [paths objectAtIndex:0];
                
                NSFileManager *manager = [NSFileManager defaultManager];
                NSString *outputURL = [documentsDirectory stringByAppendingPathComponent:@"editedVideo"];
                [manager createDirectoryAtPath:outputURL withIntermediateDirectories:YES attributes:nil error:nil];
                NSString* fileName = [[[NSString stringWithFormat:@"%f",CFAbsoluteTimeGetCurrent()] stringByReplacingOccurrencesOfString:@"." withString:@"-"] stringByAppendingString:@".MOV"];
                outputURL = [outputURL stringByAppendingPathComponent:fileName];
                AVURLAsset *videoAsset = [AVURLAsset URLAssetWithURL:mediaURL options:nil];
                AVAssetExportSession *exportSession = [[AVAssetExportSession alloc] initWithAsset:videoAsset presetName:AVAssetExportPresetHighestQuality];
                exportSession.outputURL = [NSURL fileURLWithPath:outputURL isDirectory:NO];
                exportSession.outputFileType = AVFileTypeQuickTimeMovie;
                CMTimeRange timeRange = CMTimeRangeMake(CMTimeMake(startMilliseconds, 1000), CMTimeMake(endMilliseconds - startMilliseconds, 1000));
                exportSession.timeRange = timeRange;
                
                NSMutableDictionary *dictionary = [TiUtils dictionaryWithCode:0 message:nil];
                [dictionary setObject:mediaType forKey:@"mediaType"];
                
                if (thumbnail!=nil) {
                    [dictionary setObject:thumbnail forKey:@"thumbnail"];
                }

                [exportSession exportAsynchronouslyWithCompletionHandler:^{
                    switch (exportSession.status) {
                        case AVAssetExportSessionStatusCompleted:
                            [self handleTrimmedVideo:exportSession.outputURL withDictionary:dictionary];
                            break;
                        default:
                            [self handleTrimmedVideo:mediaURL withDictionary:dictionary];
                            break;
                    }
                }];
                return;
            }
        }
        
        media = [[[TiBlob alloc] initWithFile:[mediaURL path]] autorelease];
        if ([media mimeType] == nil) {
            [media setMimeType:@"video/mpeg" type:TiBlobTypeFile];
        }
        if (saveToRoll) {
            NSString *tempFilePath = [mediaURL path];
            UISaveVideoAtPathToSavedPhotosAlbum(tempFilePath, nil, nil, NULL);
        }
    }
    else {
        UIImage *editedImage = [editingInfo objectForKey:UIImagePickerControllerEditedImage];
        if ((mediaURL!=nil) && (editedImage == nil)) {
            
            media = [[[TiBlob alloc] initWithFile:[mediaURL path]] autorelease];
            [media setMimeType:@"image/jpeg" type:TiBlobTypeFile];
			
            if (saveToRoll) {
                UIImage *image = [editingInfo objectForKey:UIImagePickerControllerOriginalImage];
                UIImageWriteToSavedPhotosAlbum(image, nil, nil, NULL);
            }
        }
        else {
            NSValue * ourRectValue = [editingInfo objectForKey:UIImagePickerControllerCropRect];
            if (ourRectValue != nil) {
                CGRect ourRect = [ourRectValue CGRectValue];
                cropRect = [NSDictionary dictionaryWithObjectsAndKeys:
                            [NSNumber numberWithFloat:ourRect.origin.x],@"x",
                            [NSNumber numberWithFloat:ourRect.origin.y],@"y",
                            [NSNumber numberWithFloat:ourRect.size.width],@"width",
                            [NSNumber numberWithFloat:ourRect.size.height],@"height",
                            nil];
            }
            
            UIImage *resultImage = nil;
            UIImage *originalImage = [editingInfo objectForKey:UIImagePickerControllerOriginalImage];
            if ( (editedImage != nil) && (ourRectValue != nil) && (originalImage != nil)) {
                
                CGRect ourRect = [ourRectValue CGRectValue];
                
                if ( (ourRect.size.width > editedImage.size.width) || (ourRect.size.height > editedImage.size.height) ){
                    UIGraphicsBeginImageContextWithOptions(ourRect.size, NO, originalImage.scale);
                    CGContextRef context = UIGraphicsGetCurrentContext();
                    
                    // translated rectangle for drawing sub image 
                    CGRect drawRect = CGRectMake(-ourRect.origin.x, -ourRect.origin.y, originalImage.size.width, originalImage.size.height);
                    
                    // clip to the bounds of the image context
                    CGContextClipToRect(context, CGRectMake(0, 0, ourRect.size.width, ourRect.size.height));
                    
                    // draw image
                    [originalImage drawInRect:drawRect];
                    
                    // grab image
                    resultImage = UIGraphicsGetImageFromCurrentImageContext();
                    
                    UIGraphicsEndImageContext();
                }
            }
            
            if (resultImage == nil) {
                resultImage = (editedImage != nil) ? editedImage : originalImage;
            }
            
            media = [[[TiBlob alloc] initWithImage:resultImage] autorelease];
            if (saveToRoll) {
                UIImageWriteToSavedPhotosAlbum(resultImage, nil, nil, NULL);
            }
        }
    }
	
    NSMutableDictionary *dictionary = [TiUtils dictionaryWithCode:0 message:nil];
    [dictionary setObject:mediaType forKey:@"mediaType"];
    [dictionary setObject:media forKey:@"media"];

    if (thumbnail!=nil) {
        [dictionary setObject:thumbnail forKey:@"thumbnail"];
    }

    if (cropRect != nil) {
        [dictionary setObject:cropRect forKey:@"cropRect"];
    }
	
    [self sendPickerSuccess:dictionary];
}

- (void)imagePickerControllerDidCancel:(UIImagePickerController *)picker_
{
	[self closeModalPicker:picker];
	[self sendPickerCancel];
}

#pragma mark UIVideoEditorControllerDelegate

- (void)videoEditorController:(UIVideoEditorController *)editor_ didSaveEditedVideoToPath:(NSString *)editedVideoPath
{
	id listener = [[editorSuccessCallback retain] autorelease];
	[self closeModalPicker:editor_];
	[self destroyPicker];

	if (listener!=nil)
	{
		TiBlob *media = [[[TiBlob alloc]initWithFile:editedVideoPath] autorelease];
		[media setMimeType:@"video/mpeg" type:TiBlobTypeFile];
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:0 message:nil];
		[event setObject:NUMBOOL(NO) forKey:@"cancel"];
		[event setObject:media forKey:@"media"];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error",event,listener,nil]];
	}
}

- (void)videoEditorControllerDidCancel:(UIVideoEditorController *)editor_
{ 
	id listener = [[editorCancelCallback retain] autorelease];
	[self closeModalPicker:editor_];
	[self destroyPicker];

	if (listener!=nil) 
	{
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:-1 message:@"The user cancelled"];
		[event setObject:NUMBOOL(YES) forKey:@"cancel"];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error",event,listener,nil]];
	}
}

- (void)videoEditorController:(UIVideoEditorController *)editor_ didFailWithError:(NSError *)error
{
	id listener = [[editorErrorCallback retain] autorelease];
	[self closeModalPicker:editor_];
	[self destroyPicker];

	if (listener!=nil)
	{
		NSMutableDictionary * event = [TiUtils dictionaryWithCode:[error code] message:[TiUtils messageFromError:error]];
		[event setObject:NUMBOOL(NO) forKey:@"cancel"];
		[NSThread detachNewThreadSelector:@selector(dispatchCallback:) toTarget:self withObject:[NSArray arrayWithObjects:@"error",event,listener,nil]];
	}
}


-(void)beep:(id)unused
{
    ENSURE_UI_THREAD(beep,unused);
    AudioServicesPlayAlertSound(kSystemSoundID_Vibrate);
}

-(void)vibrate:(id)args
{
    //No pattern support on iOS
    [self beep:nil];
}

@end

#endif
