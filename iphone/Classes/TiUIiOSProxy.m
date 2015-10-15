/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

#import "TiUIiOSProxy.h"
#import "TiUtils.h"
#import "Webcolor.h"
#ifdef USE_TI_UIIOS

#ifdef USE_TI_UIIOSPREVIEWCONTEXT
#import "TiUIiOSPreviewContextProxy.h"
#import "TiUIiOSPreviewActionProxy.h"
#import "TiUIiOSPreviewActionGroupProxy.h"
#endif

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
#import "TiUIiOSTransitionAnimationProxy.h"
#endif

#ifdef USE_TI_UIIOSADVIEW
	#import "TiUIiOSAdViewProxy.h"
    #import <iAd/iAd.h>
#endif

#ifdef USE_TI_UIIOS3DMATRIX
	#import "Ti3DMatrix.h"
#endif

#ifdef USE_TI_UIIOSCOVERFLOWVIEW
	#import "TiUIiOSCoverFlowViewProxy.h"
#endif
#ifdef USE_TI_UIIOSTOOLBAR
	#import "TiUIiOSToolbarProxy.h"
#endif
#ifdef USE_TI_UIIOSTABBEDBAR
	#import "TiUIiOSTabbedBarProxy.h"
#endif

#if defined(USE_TI_UIIPADDOCUMENTVIEWER) || defined(USE_TI_UIIOSDOCUMENTVIEWER)
    #import "TiUIiOSDocumentViewerProxy.h"
#endif
#ifdef USE_TI_UIIOSACTIVITYVIEW
#import "TiUIiOSActivityViewProxy.h"
#endif
#ifdef USE_TI_UIIOSACTIVITY
#import "TiUIiOSActivityProxy.h"
#endif
#ifdef USE_TI_UIIOSSPLITWINDOW
#import "TiUIiOSSplitWindowProxy.h"
#endif

#ifdef USE_TI_UIIOSANIMATOR
#import "TiAnimatorProxy.h"
#ifdef USE_TI_UIIOSSNAPBEHAVIOR
#import "TiSnapBehavior.h"
#endif
#ifdef USE_TI_UIIOSPUSHBEHAVIOR
#import "TiPushBehavior.h"
#endif
#ifdef USE_TI_UIIOSGRAVITYBEHAVIOR
#import "TiGravityBehavior.h"
#endif
#ifdef USE_TI_UIIOSANCHORATTACHMENTBEHAVIOR
#import "TiAnchorAttachBehavior.h"
#endif
#ifdef USE_TI_UIIOSVIEWATTACHMENTBEHAVIOR
#import "TiViewAttachBehavior.h"
#endif
#ifdef USE_TI_UIIOSCOLLISIONBEHAVIOR
#import "TiCollisionBehavior.h"
#endif
#ifdef USE_TI_UIIOSDYNAMICITEMBEHAVIOR
#import "TiDynamicItemBehavior.h"
#endif
#endif

#ifdef USE_TI_UIIOSAPPLICATIONSHORTCUTS
#import "TiUIiOSApplicationShortcutsProxy.h"
#endif

@implementation TiUIiOSProxy

-(NSString*)apiName
{
    return @"Ti.UI.iOS";
}

-(NSNumber*)forceTouchSupported
{
    return NUMBOOL([TiUtils forceTouchSupported]);
}

-(NSNumber*)SCROLL_DECELERATION_RATE_NORMAL
{
    return NUMFLOAT(UIScrollViewDecelerationRateNormal);
}

-(NSNumber*)SCROLL_DECELERATION_RATE_FAST
{
    return NUMFLOAT(UIScrollViewDecelerationRateFast);
}

-(NSNumber*)CLIP_MODE_DEFAULT
{
    return NUMINT(0);
}
-(NSNumber*)CLIP_MODE_ENABLED
{
    return NUMINT(1);
}
-(NSNumber*)CLIP_MODE_DISABLED
{
    return NUMINT(-1);
}

#ifdef USE_TI_UILISTVIEW
-(NSNumber*) ROW_ACTION_STYLE_DEFAULT
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UITableViewRowActionStyleDefault);
    }
    return nil;
}
-(NSNumber*) ROW_ACTION_STYLE_DESTRUCTIVE
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UITableViewRowActionStyleDestructive);
    }
    return nil;
}
-(NSNumber*) ROW_ACTION_STYLE_NORMAL
{
    if ([TiUtils isIOS8OrGreater]) {
        return NUMINTEGER(UITableViewRowActionStyleNormal);
    }
    return nil;
}
#endif

#ifdef USE_TI_UIIOSPREVIEWCONTEXT
-(NSNumber*) PREVIEW_ACTION_STYLE_DEFAULT
{
#if IS_XCODE_7
    if ([TiUtils isIOS9OrGreater]) {
        return NUMINTEGER(UIPreviewActionStyleDefault);
    }
#endif
    return nil;
}
-(NSNumber*) PREVIEW_ACTION_STYLE_DESTRUCTIVE
{
#if IS_XCODE_7
    if ([TiUtils isIOS9OrGreater]) {
        return NUMINTEGER(UIPreviewActionStyleDestructive);
    }
#endif
    return nil;
}
-(NSNumber*) PREVIEW_ACTION_STYLE_SELECTED
{
#if IS_XCODE_7
    if ([TiUtils isIOS9OrGreater]) {
        return NUMINTEGER(UIPreviewActionStyleSelected);
    }
#endif
    return nil;
}
#endif

#ifdef USE_TI_UIIOSADVIEW

-(NSString*)AD_SIZE_PORTRAIT 
{
    return [TiUIiOSAdViewProxy portraitSize];
}

-(NSString*)AD_SIZE_LANDSCAPE 
{
    return [TiUIiOSAdViewProxy landscapeSize];
}

-(id)createAdView:(id)args
{
	return [[[TiUIiOSAdViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

#endif

#ifdef USE_TI_UIIOS3DMATRIX
-(id)create3DMatrix:(id)args
{
	DEPRECATED_REPLACED(@"UI.iOS.create3DMatrix()", @"2.1.0", @"Ti.UI.create3DMatrix()");
    if (args==nil || [args count] == 0)
	{
		return [[[Ti3DMatrix alloc] init] autorelease];
	}
	ENSURE_SINGLE_ARG(args,NSDictionary);
	Ti3DMatrix *matrix = [[Ti3DMatrix alloc] initWithProperties:args];
	return [matrix autorelease];
}
#endif
#ifdef USE_TI_UIIOSCOVERFLOWVIEW
-(id)createCoverFlowView:(id)args
{
	return [[[TiUIiOSCoverFlowViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSTOOLBAR
-(id)createToolbar:(id)args
{
	return [[[TiUIiOSToolbarProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSATTRIBUTEDSTRING
-(id)createAttributedString:(id)args
{
	DEPRECATED_REPLACED(@"UI.iOS.createAttributedString()", @"3.6.0", @"Ti.UI.createAttributedString()");
    return [[[TiUIAttributedStringProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSTABBEDBAR
-(id)createTabbedBar:(id)args
{
    return [[[TiUIiOSTabbedBarProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#if defined(USE_TI_UIIPADDOCUMENTVIEWER) || defined(USE_TI_UIIOSDOCUMENTVIEWER)
-(id)createDocumentViewer:(id)args
{
	return [[[TiUIiOSDocumentViewerProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSACTIVITYVIEW
-(id)createActivityView:(id)args
{
    return [[[TiUIiOSActivityViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSACTIVITY
-(id)createActivity:(id)args
{
    return [[[TiUIiOSActivityProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSSPLITWINDOW
-(id)createSplitWindow:(id)args
{
    return [[[TiUIiOSSplitWindowProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSTRANSITIONANIMATION
-(id)createTransitionAnimation:(id)args;
{
    return [[[TiUIiOSTransitionAnimationProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#if IS_XCODE_7
#ifdef USE_TI_UIIOSPREVIEWCONTEXT
-(id)createPreviewAction:(id)args
{
    return [[[TiUIiOSPreviewActionProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

-(id)createPreviewActionGroup:(id)args
{
    return [[[TiUIiOSPreviewActionGroupProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

-(id)createPreviewContext:(id)args
{
    return [[[TiUIiOSPreviewContextProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#endif

#ifdef USE_TI_UIIOSANIMATOR
-(id)createAnimator:(id)args
{
    return [[[TiAnimatorProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#ifdef USE_TI_UIIOSSNAPBEHAVIOR
-(id)createSnapBehavior:(id)args
{
    return [[[TiSnapBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif
#ifdef USE_TI_UIIOSPUSHBEHAVIOR
-(id)createPushBehavior:(id)args
{
    return [[[TiPushBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
//TiPushBehavior Constants
MAKE_SYSTEM_PROP(PUSH_MODE_CONTINUOUS, 0);
MAKE_SYSTEM_PROP(PUSH_MODE_INSTANTANEOUS, 1);
#endif

#ifdef USE_TI_UIIOSGRAVITYBEHAVIOR
-(id)createGravityBehavior:(id)args
{
    return [[[TiGravityBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSANCHORATTACHMENTBEHAVIOR
-(id)createAnchorAttachmentBehavior:(id)args
{
    return [[[TiAnchorAttachBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSVIEWATTACHMENTBEHAVIOR
-(id)createViewAttachmentBehavior:(id)args
{
    return [[[TiViewAttachBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIIOSCOLLISIONBEHAVIOR
-(id)createCollisionBehavior:(id)args
{
    return [[[TiCollisionBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
//TiCollisionBehavior Constants
MAKE_SYSTEM_PROP(COLLISION_MODE_ITEM, 0);
MAKE_SYSTEM_PROP(COLLISION_MODE_BOUNDARY, 1);
MAKE_SYSTEM_PROP(COLLISION_MODE_ALL, 2);
#endif

#ifdef USE_TI_UIIOSDYNAMICITEMBEHAVIOR
-(id)createDynamicItemBehavior:(id)args
{
    return [[[TiDynamicItemBehavior alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#endif


#ifdef USE_TI_UIIOS
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(ANIMATION_CURVE_EASE_IN_OUT, UIViewAnimationOptionCurveEaseInOut, @"UI.iOS.ANIMATION_CURVE_EASE_IN_OUT", @"2.1.0", @"Ti.UI.ANIMATION_CURVE_EASE_IN_OUT");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(ANIMATION_CURVE_EASE_IN, UIViewAnimationOptionCurveEaseIn, @"UI.iOS.ANIMATION_CURVE_EASE_IN", @"2.1.0", @"Ti.UI.ANIMATION_CURVE_EASE_IN");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(ANIMATION_CURVE_EASE_OUT,UIViewAnimationOptionCurveEaseOut,  @"UI.iOS.ANIMATION_CURVE_EASE_OUT", @"2.1.0", @"Ti.UI.ANIMATION_CURVE_EASE_OUT");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(ANIMATION_CURVE_LINEAR,UIViewAnimationOptionCurveLinear, @"UI.iOS.ANIMATION_CURVE_LINEAR", @"2.1.0", @"Ti.UI.ANIMATION_CURVE_LINEAR");

MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_NONE,UIDataDetectorTypeNone, @"UI.iOS.AUTODETECT_NONE", @"3.0.0", @"Ti.UI.AUTOLINK_NONE");
-(NSNumber*)AUTODETECT_ALL
{
    DEPRECATED_REPLACED(@"UI.iOS.AUTODETECT_ALL", @"3.0.0", @"Ti.UI.AUTOLINK_ALL")
    return NUMUINTEGER(UIDataDetectorTypeAll);
}
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_PHONE,UIDataDetectorTypePhoneNumber, @"UI.iOS.AUTODETECT_PHONE", @"3.0.0", @"Ti.UI.AUTOLINK_PHONE_NUMBERS");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_LINK,UIDataDetectorTypeLink, @"UI.iOS.AUTODETECT_LINK", @"3.0.0", @"Ti.UI.AUTOLINK_URLS");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_ADDRESS,UIDataDetectorTypeAddress, @"UI.iOS.AUTODETECT_ADDRESS", @"3.0.0", @"Ti.UI.AUTOLINK_MAP_ADDRESSES");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_CALENDAR,UIDataDetectorTypeCalendarEvent, @"UI.iOS.AUTODETECT_CALENDAR", @"3.0.0", @"Ti.UI.AUTOLINK_CALENDAR");


MAKE_SYSTEM_STR(COLOR_GROUP_TABLEVIEW_BACKGROUND, IOS_COLOR_GROUP_TABLEVIEW_BACKGROUND);
MAKE_SYSTEM_STR(TABLEVIEW_INDEX_SEARCH, UITableViewIndexSearch);

-(NSString*)COLOR_SCROLLVIEW_BACKGROUND
{
    DEPRECATED_REMOVED(@"UI.iOS.COLOR_SCROLLVIEW_BACKGROUND",@"3.4.2",@"3.6.0")
    return IOS_COLOR_SCROLLVIEW_TEXTURED_BACKGROUND;
}
-(NSString*)COLOR_VIEW_FLIPSIDE_BACKGROUND
{
    DEPRECATED_REMOVED(@"UI.iOS.COLOR_VIEW_FLIPSIDE_BACKGROUND",@"3.4.2",@"3.6.0")
    return IOS_COLOR_VIEW_FLIPSIDE_BACKGROUND;
}
-(NSString*)COLOR_UNDER_PAGE_BACKGROUND
{
    DEPRECATED_REMOVED(@"UI.iOS.COLOR_UNDER_PAGE_BACKGROUND",@"3.4.2",@"3.6.0")
    return IOS_COLOR_UNDER_PAGE_BACKGROUND;
}


MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_LINK_CLICKED,UIWebViewNavigationTypeLinkClicked);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_FORM_SUBMITTED,UIWebViewNavigationTypeFormSubmitted);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_BACK_FORWARD,UIWebViewNavigationTypeBackForward);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_RELOAD,UIWebViewNavigationTypeReload);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_FORM_RESUBMITTED,UIWebViewNavigationTypeFormResubmitted);
MAKE_SYSTEM_PROP(WEBVIEW_NAVIGATIONTYPE_OTHER,UIWebViewNavigationTypeOther);


MAKE_SYSTEM_PROP(ACTIVITY_CATEGORY_ACTION,  UIActivityCategoryAction);
MAKE_SYSTEM_PROP(ACTIVITY_CATEGORY_SHARE,   UIActivityCategoryShare);

MAKE_SYSTEM_STR(ACTIVITY_TYPE_FACEBOOK,     UIActivityTypePostToFacebook);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TWITTER,      UIActivityTypePostToTwitter);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_WEIBO,        UIActivityTypePostToWeibo);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_MESSAGE,      UIActivityTypeMessage);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_MAIL,         UIActivityTypeMail);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_PRINT,        UIActivityTypePrint);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_COPY,         UIActivityTypeCopyToPasteboard);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TO_CONTACT,   UIActivityTypeAssignToContact);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_CAMERA_ROLL,  UIActivityTypeSaveToCameraRoll);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TO_READING_LIST, UIActivityTypeAddToReadingList);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_FLICKR,       UIActivityTypePostToFlickr);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_VIMEO,        UIActivityTypePostToVimeo);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_TENCENT_WEIBO, UIActivityTypePostToTencentWeibo);
MAKE_SYSTEM_STR(ACTIVITY_TYPE_AIRDROP,      UIActivityTypeAirDrop);


#endif

#if IS_XCODE_7
#ifdef USE_TI_UIIOSAPPLICATIONSHORTCUTS

-(id)createApplicationShortcuts:(id)args
{
    return [[[TiUIiOSApplicationShortcutsProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}

MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_COMPOSE,UIApplicationShortcutIconTypeCompose);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_PLAY,UIApplicationShortcutIconTypePlay);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_PAUSE,UIApplicationShortcutIconTypePause);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_ADD,UIApplicationShortcutIconTypeAdd);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_LOCATION,UIApplicationShortcutIconTypeLocation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_SEARCH,UIApplicationShortcutIconTypeSearch);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_SHARE,UIApplicationShortcutIconTypeShare);

#ifdef __IPHONE_9_1

MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_PROHIBIT,UIApplicationShortcutIconTypeProhibit);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CONTACT,UIApplicationShortcutIconTypeContact);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_HOME,UIApplicationShortcutIconTypeHome);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_MARK_LOCATION,UIApplicationShortcutIconTypeMarkLocation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_FAVORITE,UIApplicationShortcutIconTypeFavorite);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_LOVE,UIApplicationShortcutIconTypeLove);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CLOUD,UIApplicationShortcutIconTypeCloud);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_INVITATION,UIApplicationShortcutIconTypeInvitation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CONFIRMATION,UIApplicationShortcutIconTypeConfirmation);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_MAIL,UIApplicationShortcutIconTypeMail);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_MESSAGE,UIApplicationShortcutIconTypeMessage);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_DATE,UIApplicationShortcutIconTypeDate);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_TIME,UIApplicationShortcutIconTypeTime);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CAPTURE_PHOTO,UIApplicationShortcutIconTypeCapturePhoto);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_CAPTURE_VIDEO,UIApplicationShortcutIconTypeCaptureVideo);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_TASK,UIApplicationShortcutIconTypeTask);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_TASK_COMPLETED,UIApplicationShortcutIconTypeTaskCompleted);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_ALARM,UIApplicationShortcutIconTypeAlarm);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_BOOKMARK,UIApplicationShortcutIconTypeBookmark);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_SHUFFLE,UIApplicationShortcutIconTypeShuffle);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_AUDIO,UIApplicationShortcutIconTypeAudio);
MAKE_SYSTEM_PROP(SHORTCUT_ICON_TYPE_UPDATE,UIApplicationShortcutIconTypeUpdate);

#endif
#endif
#endif

@end

#endif
