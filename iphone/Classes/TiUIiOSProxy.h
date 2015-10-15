/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiProxy.h"

#ifdef USE_TI_UIIOS

#ifdef USE_TI_UIIOSADVIEW
	#import "TiUIiOSAdViewProxy.h"
#endif

#endif
@interface TiUIiOSProxy : TiProxy {
@private

}

@property (nonatomic,readonly) NSNumber* SCROLL_DECELERATION_RATE_NORMAL;
@property (nonatomic,readonly) NSNumber* SCROLL_DECELERATION_RATE_FAST;
@property (nonatomic,readonly) NSNumber* CLIP_MODE_DEFAULT;
@property (nonatomic,readonly) NSNumber* CLIP_MODE_ENABLED;
@property (nonatomic,readonly) NSNumber* CLIP_MODE_DISABLED;

#ifdef USE_TI_UIIOSPREVIEWCONTEXT
@property (nonatomic,readonly) NSNumber* PREVIEW_ACTION_STYLE_DEFAULT;
@property (nonatomic,readonly) NSNumber* PREVIEW_ACTION_STYLE_DESTRUCTIVE;
@property (nonatomic,readonly) NSNumber* PREVIEW_ACTION_STYLE_SELECTED;
#endif

@property(nonatomic,readonly) NSNumber *ACTIVITY_CATEGORY_SHARE;
@property(nonatomic,readonly) NSNumber *ACTIVITY_CATEGORY_ACTION;

@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_FACEBOOK;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_TWITTER;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_WEIBO;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_MESSAGE;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_MAIL;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_PRINT;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_COPY;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_TO_CONTACT;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_CAMERA_ROLL;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_TO_READING_LIST;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_FLICKR;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_VIMEO;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_TENCENT_WEIBO;
@property(nonatomic,readonly) NSString *ACTIVITY_TYPE_AIRDROP;


#ifdef USE_TI_UIIOSADVIEW
-(id)createAdView:(id)args;

@property(nonatomic,readonly) NSString* AD_SIZE_PORTRAIT;
@property(nonatomic,readonly) NSString* AD_SIZE_LANDSCAPE;

#endif

/**
    Checks the force touch capibility of the current device.
 */
-(NSNumber*)forceTouchSupported;

#ifdef USE_TI_UIIOS3DMATRIX
-(id)create3DMatrix:(id)args;
#endif
#ifdef USE_TI_UIIOSCOVERFLOWVIEW
-(id)createCoverFlowView:(id)args;
#endif
#ifdef USE_TI_UIIOSTOOLBAR
-(id)createToolbar:(id)args;
#endif
#ifdef USE_TI_UIIOSTABBEDBAR
-(id)createTabbedBar:(id)args;
#endif
#if defined(USE_TI_UIIPADDOCUMENTVIEWER) || defined(USE_TI_UIIOSDOCUMENTVIEWER)
-(id)createDocumentViewer:(id)args;
#endif
#ifdef USE_TI_UIIOSACTIVITYVIEW
-(id)createActivityView:(id)args;
#endif
#ifdef USE_TI_UIIOSACTIVITY
-(id)createActivity:(id)args;
#endif
#ifdef USE_TI_UIIOSSPLITWINDOW
-(id)createSplitWindow:(id)args;
#endif
#ifdef USE_TI_UIIOSATTRIBUTEDSTRING
-(id)createAttributedString:(id)args;
#endif
#ifdef USE_TI_UIIOSANIMATOR
-(id)createAnimator:(id)args;
#ifdef USE_TI_UIIOSSNAPBEHAVIOR
-(id)createSnapBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSPUSHBEHAVIOR
-(id)createPushBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSGRAVITYBEHAVIOR
-(id)createGravityBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSANCHORATTACHMENTBEHAVIOR
-(id)createAnchorAttachmentBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSVIEWATTACHMENTBEHAVIOR
-(id)createViewAttachmentBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSCOLLISIONBEHAVIOR
-(id)createCollisionBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSDYNAMICITEMBEHAVIOR
-(id)createDynamicItemBehavior:(id)args;
#endif
#ifdef USE_TI_UIIOSTRANSITIONANIMATION
-(id)createTransitionAnimation:(id)args;
#endif
#ifdef USE_TI_UIIOSPREVIEWCONTEXT
-(id)createPreviewAction:(id)args;
-(id)createPreviewActionGroup:(id)args;
-(id)createPreviewContext:(id)args;
#endif
#endif

#if IS_XCODE_7
#ifdef USE_TI_UIIOSAPPLICATIONSHORTCUTS
-(id)createApplicationShortcuts:(id)args;
#endif
#endif
@end

