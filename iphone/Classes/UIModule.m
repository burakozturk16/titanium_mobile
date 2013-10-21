/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
#import "TiBase.h"

#ifdef USE_TI_UI

#import "TiDimension.h"
#import "UIModule.h"
#import "TiProxy.h"

#ifdef USE_TI_UI2DMATRIX
	#import "Ti2DMatrix.h"
#endif

#ifdef USE_TI_UI3DMATRIX
    #import "Ti3DMatrix.h"
#endif

#ifdef USE_TI_UIANIMATION
	#import "TiAnimation.h"
#endif
#ifdef USE_TI_UIIPHONE
	#import "TiUIiPhoneProxy.h"
#endif
#ifdef USE_TI_UIIPAD
	#import "TiUIiPadProxy.h"
#endif
#ifdef USE_TI_UIIOS
#import "TiUIiOSProxy.h"
#endif
#ifdef USE_TI_UICLIPBOARD
#import "TiUIClipboardProxy.h"
#endif
#ifdef USE_TI_UICOVERFLOWVIEW
	#import "TiUIiOSCoverFlowViewProxy.h"
#endif
#ifdef USE_TI_UITOOLBAR
	#import "TiUIiOSToolbarProxy.h"
#endif
#ifdef USE_TI_UITABBEDBAR
    #import "TiUIiOSTabbedBarProxy.h"
#endif
#ifdef USE_TI_UIACTIVITYINDICATORSTYLE
#import "TiUIActivityIndicatorStyleProxy.h"
#endif

#ifdef USE_TI_UITABLEVIEWSEPARATORSTYLE
#import "TiUITableViewSeparatorStyleProxy.h"
#endif
#import "TiApp.h"
#import "ImageLoader.h"
#import "Webcolor.h"
#import "TiUtils.h"
#import "UIControl+TiUIView.h"

#ifdef USE_TI_UINAVIGATIONWINDOW
#import "TiUINavigationWindowProxy.h"
#endif
#ifdef USE_TI_UITRANSITIONSTYLE
#import "TiUITransitionStyleProxy.h"
#endif
#ifdef USE_TI_UIBLENDMODE
#import "TiUIBlendModeProxy.h"
#endif

#define DEFINE_SUBPROXY_AS(methodName,className, ivarName)	\
-(TiProxy*)methodName	\
{	\
if (ivarName==nil)	\
{	\
ivarName = [[TiUI##className##Proxy alloc] _initWithPageContext:[self executionContext]];	\
[self rememberProxy:ivarName]; \
}	\
return ivarName;	\
}	\

@implementation UIModule

+(void)swizzle
{
    [UIControl swizzle];
}

-(void)startup
{
    //should be done only once. And must be done before any TiUIButton is allocated
    [UIModule swizzle];
	[super startup];
}

-(void)dealloc
{
#ifdef USE_TI_UIIPHONE
	FORGET_AND_RELEASE(iphone);
#endif
#ifdef USE_TI_UIIPAD
    FORGET_AND_RELEASE(ipad);
#endif
#ifdef USE_TI_UIIOS
    FORGET_AND_RELEASE(ios);
#endif
#ifdef USE_TI_UICLIPBOARD
    FORGET_AND_RELEASE(clipboard);
#endif
#ifdef USE_TI_UIACTIVITYINDICATORSTYLE
    FORGET_AND_RELEASE(activityIndicatorStyle);
#endif
#ifdef USE_TI_UITABLEVIEWSEPARATORSTYLE
    FORGET_AND_RELEASE(tableViewSeparatorStyle);
#endif
#ifdef USE_TI_UILISTVIEWSEPARATORSTYLE
	FORGET_AND_RELEASE(listViewSeparatorStyle);
#endif
#ifdef USE_TI_UITRANSITIONSTYLE
    FORGET_AND_RELEASE(transitionStyle);
#endif
#ifdef USE_TI_UIBLENDMODE
    FORGET_AND_RELEASE(blendMode);
#endif
	[super dealloc];
}

-(NSString*)apiName
{
    return @"Ti.UI";
}

#pragma mark Public Constants

MAKE_SYSTEM_PROP(ANIMATION_CURVE_EASE_IN_OUT,UIViewAnimationOptionCurveEaseInOut);
MAKE_SYSTEM_PROP(ANIMATION_CURVE_EASE_IN,UIViewAnimationOptionCurveEaseIn);
MAKE_SYSTEM_PROP(ANIMATION_CURVE_EASE_OUT,UIViewAnimationOptionCurveEaseOut);
MAKE_SYSTEM_PROP(ANIMATION_CURVE_LINEAR,UIViewAnimationOptionCurveLinear);

MAKE_SYSTEM_PROP(TEXT_VERTICAL_ALIGNMENT_TOP,UIControlContentVerticalAlignmentTop);
MAKE_SYSTEM_PROP(TEXT_VERTICAL_ALIGNMENT_CENTER,UIControlContentVerticalAlignmentCenter);
MAKE_SYSTEM_PROP(TEXT_VERTICAL_ALIGNMENT_BOTTOM,UIControlContentVerticalAlignmentBottom);

MAKE_SYSTEM_PROP(TEXT_ALIGNMENT_LEFT,NSTextAlignmentLeft);
MAKE_SYSTEM_PROP(TEXT_ALIGNMENT_CENTER,NSTextAlignmentCenter);
MAKE_SYSTEM_PROP(TEXT_ALIGNMENT_RIGHT,NSTextAlignmentRight);

MAKE_SYSTEM_PROP(SCALE_TYPE_SCALE_TO_FILL,UIViewContentModeScaleToFill);
MAKE_SYSTEM_PROP(SCALE_TYPE_ASPECT_FIT,UIViewContentModeScaleAspectFit);
MAKE_SYSTEM_PROP(SCALE_TYPE_ASPECT_FILL,UIViewContentModeScaleAspectFill);
MAKE_SYSTEM_PROP(SCALE_TYPE_CENTER,UIViewContentModeCenter);
MAKE_SYSTEM_PROP(SCALE_TYPE_LEFT,UIViewContentModeLeft);
MAKE_SYSTEM_PROP(SCALE_TYPE_RIGHT,UIViewContentModeRight);

MAKE_SYSTEM_PROP(TEXT_ELLIPSIZE_NONE,UILineBreakModeWordWrap);
MAKE_SYSTEM_PROP(TEXT_ELLIPSIZE_HEAD,UILineBreakModeHeadTruncation);
MAKE_SYSTEM_PROP(TEXT_ELLIPSIZE_MIDDLE,UILineBreakModeMiddleTruncation);
MAKE_SYSTEM_PROP(TEXT_ELLIPSIZE_TAIL,UILineBreakModeTailTruncation);

MAKE_SYSTEM_PROP(RETURNKEY_DEFAULT,UIReturnKeyDefault);
MAKE_SYSTEM_PROP(RETURNKEY_GO,UIReturnKeyGo);
MAKE_SYSTEM_PROP(RETURNKEY_GOOGLE,UIReturnKeyGoogle);
MAKE_SYSTEM_PROP(RETURNKEY_JOIN,UIReturnKeyJoin);
MAKE_SYSTEM_PROP(RETURNKEY_NEXT,UIReturnKeyNext);
MAKE_SYSTEM_PROP(RETURNKEY_ROUTE,UIReturnKeyRoute);
MAKE_SYSTEM_PROP(RETURNKEY_SEARCH,UIReturnKeySearch);
MAKE_SYSTEM_PROP(RETURNKEY_SEND,UIReturnKeySend);
MAKE_SYSTEM_PROP(RETURNKEY_YAHOO,UIReturnKeyYahoo);
MAKE_SYSTEM_PROP(RETURNKEY_DONE,UIReturnKeyDone);
MAKE_SYSTEM_PROP(RETURNKEY_EMERGENCY_CALL,UIReturnKeyEmergencyCall);

MAKE_SYSTEM_PROP(KEYBOARD_DEFAULT,UIKeyboardTypeDefault);
MAKE_SYSTEM_PROP(KEYBOARD_ASCII,UIKeyboardTypeASCIICapable);
MAKE_SYSTEM_PROP(KEYBOARD_NUMBERS_PUNCTUATION,UIKeyboardTypeNumbersAndPunctuation);
MAKE_SYSTEM_PROP(KEYBOARD_URL,UIKeyboardTypeURL);
MAKE_SYSTEM_PROP(KEYBOARD_NUMBER_PAD,UIKeyboardTypeNumberPad);

/* Because this is a new feature in 4.1, we have to guard against it in both compiling AND runtime.*/
-(NSNumber*)KEYBOARD_DECIMAL_PAD
{
#if __IPHONE_4_1 <= __IPHONE_OS_VERSION_MAX_ALLOWED
	if([[[UIDevice currentDevice] systemVersion] floatValue] >= 4.1){
		return [NSNumber numberWithInt:UIKeyboardTypeDecimalPad];
	}
#endif
	return [NSNumber numberWithInt:UIKeyboardTypeNumbersAndPunctuation];
}

MAKE_SYSTEM_PROP(KEYBOARD_PHONE_PAD,UIKeyboardTypePhonePad);
MAKE_SYSTEM_PROP(KEYBOARD_NAMEPHONE_PAD,UIKeyboardTypeNamePhonePad);
MAKE_SYSTEM_PROP(KEYBOARD_EMAIL,UIKeyboardTypeEmailAddress);

MAKE_SYSTEM_PROP(KEYBOARD_APPEARANCE_DEFAULT,UIKeyboardAppearanceDefault);
MAKE_SYSTEM_PROP(KEYBOARD_APPEARANCE_ALERT,UIKeyboardAppearanceAlert);

MAKE_SYSTEM_PROP(TEXT_AUTOCAPITALIZATION_NONE,UITextAutocapitalizationTypeNone);
MAKE_SYSTEM_PROP(TEXT_AUTOCAPITALIZATION_WORDS,UITextAutocapitalizationTypeWords);
MAKE_SYSTEM_PROP(TEXT_AUTOCAPITALIZATION_SENTENCES,UITextAutocapitalizationTypeSentences);
MAKE_SYSTEM_PROP(TEXT_AUTOCAPITALIZATION_ALL,UITextAutocapitalizationTypeAllCharacters);

MAKE_SYSTEM_PROP(INPUT_BUTTONMODE_NEVER,UITextFieldViewModeNever);
MAKE_SYSTEM_PROP(INPUT_BUTTONMODE_ALWAYS,UITextFieldViewModeAlways);
MAKE_SYSTEM_PROP(INPUT_BUTTONMODE_ONFOCUS,UITextFieldViewModeWhileEditing);
MAKE_SYSTEM_PROP(INPUT_BUTTONMODE_ONBLUR,UITextFieldViewModeUnlessEditing);

MAKE_SYSTEM_PROP(INPUT_BORDERSTYLE_NONE,UITextBorderStyleNone);
MAKE_SYSTEM_PROP(INPUT_BORDERSTYLE_LINE,UITextBorderStyleLine);
MAKE_SYSTEM_PROP(INPUT_BORDERSTYLE_BEZEL,UITextBorderStyleBezel);
MAKE_SYSTEM_PROP(INPUT_BORDERSTYLE_ROUNDED,UITextBorderStyleRoundedRect);

MAKE_SYSTEM_PROP(PICKER_TYPE_PLAIN,-1);
MAKE_SYSTEM_PROP(PICKER_TYPE_DATE_AND_TIME,UIDatePickerModeDateAndTime);
MAKE_SYSTEM_PROP(PICKER_TYPE_DATE,UIDatePickerModeDate);
MAKE_SYSTEM_PROP(PICKER_TYPE_TIME,UIDatePickerModeTime);
MAKE_SYSTEM_PROP(PICKER_TYPE_COUNT_DOWN_TIMER,UIDatePickerModeCountDownTimer);

MAKE_SYSTEM_PROP(URL_ERROR_AUTHENTICATION,NSURLErrorUserAuthenticationRequired);
MAKE_SYSTEM_PROP(URL_ERROR_BAD_URL,NSURLErrorBadURL);
MAKE_SYSTEM_PROP(URL_ERROR_CONNECT,NSURLErrorCannotConnectToHost);
MAKE_SYSTEM_PROP(URL_ERROR_SSL_FAILED,NSURLErrorSecureConnectionFailed);
MAKE_SYSTEM_PROP(URL_ERROR_FILE,NSURLErrorCannotOpenFile);
MAKE_SYSTEM_PROP(URL_ERROR_FILE_NOT_FOUND,NSURLErrorFileDoesNotExist);
MAKE_SYSTEM_PROP(URL_ERROR_HOST_LOOKUP,NSURLErrorCannotFindHost);
MAKE_SYSTEM_PROP(URL_ERROR_REDIRECT_LOOP,NSURLErrorHTTPTooManyRedirects);
MAKE_SYSTEM_PROP(URL_ERROR_TIMEOUT,NSURLErrorTimedOut);
MAKE_SYSTEM_PROP(URL_ERROR_UNKNOWN,NSURLErrorUnknown);
MAKE_SYSTEM_PROP(URL_ERROR_UNSUPPORTED_SCHEME,NSURLErrorUnsupportedURL);

MAKE_SYSTEM_PROP(AUTOLINK_NONE,UIDataDetectorTypeNone);
MAKE_SYSTEM_PROP(AUTOLINK_ALL,UIDataDetectorTypeAll);
MAKE_SYSTEM_PROP(AUTOLINK_PHONE_NUMBERS,UIDataDetectorTypePhoneNumber);
MAKE_SYSTEM_PROP(AUTOLINK_URLS,UIDataDetectorTypeLink);
MAKE_SYSTEM_PROP(AUTOLINK_EMAIL_ADDRESSES,UIDataDetectorTypeLink);
MAKE_SYSTEM_PROP(AUTOLINK_MAP_ADDRESSES,UIDataDetectorTypeAddress);
MAKE_SYSTEM_PROP(AUTOLINK_CALENDAR,UIDataDetectorTypeCalendarEvent);

MAKE_SYSTEM_PROP(LIST_ITEM_TEMPLATE_DEFAULT,UITableViewCellStyleDefault);
MAKE_SYSTEM_PROP(LIST_ITEM_TEMPLATE_SETTINGS,UITableViewCellStyleValue1);
MAKE_SYSTEM_PROP(LIST_ITEM_TEMPLATE_CONTACTS,UITableViewCellStyleValue2);
MAKE_SYSTEM_PROP(LIST_ITEM_TEMPLATE_SUBTITLE,UITableViewCellStyleSubtitle);

MAKE_SYSTEM_PROP(LIST_ACCESSORY_TYPE_NONE,UITableViewCellAccessoryNone);
MAKE_SYSTEM_PROP(LIST_ACCESSORY_TYPE_CHECKMARK,UITableViewCellAccessoryCheckmark);
MAKE_SYSTEM_PROP(LIST_ACCESSORY_TYPE_DETAIL,UITableViewCellAccessoryDetailDisclosureButton);
MAKE_SYSTEM_PROP(LIST_ACCESSORY_TYPE_DISCLOSURE,UITableViewCellAccessoryDisclosureIndicator);

-(NSNumber*)INFINITE
{
    return [NSNumber numberWithDouble:HUGE_VALF];
}

MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_NONE,UIDataDetectorTypeNone, @"UI.AUTODETECT_NONE", @"1.8.0", @"Ti.UI.AUTOLINK_NONE");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_ALL,UIDataDetectorTypeAll, @"UI.AUTODETECT_ALL", @"1.8.0", @"Ti.UI.AUTOLINK_ALL");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_PHONE,UIDataDetectorTypePhoneNumber, @"UI.AUTODETECT_PHONE", @"1.8.0", @"Ti.UI.AUTOLINK_PHONE_NUMBERS");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_LINK,UIDataDetectorTypeLink, @"UI.AUTODETECT_LINK", @"1.8.0", @"Ti.UI.AUTOLINK_URLS");

MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_ADDRESS,UIDataDetectorTypeAddress, @"UI.AUTODETECT_ADDRESS", @"1.8.0", @"Ti.UI.AUTOLINK_MAP_ADDRESSES");
MAKE_SYSTEM_PROP_DEPRECATED_REPLACED(AUTODETECT_CALENDAR,UIDataDetectorTypeCalendarEvent, @"UI.AUTODETECT_CALENDAR", @"1.8.0", @"Ti.UI.AUTOLINK_CALENDAR");

#ifdef USE_TI_UILISTVIEWSEPARATORSTYLE
DEFINE_SUBPROXY_AS(ListViewSeparatorStyle, TableViewSeparatorStyle, listViewSeparatorStyle);
#endif

-(void)setBackgroundColor:(id)color
{
	TiRootViewController *controller = [[TiApp app] controller];
	[controller setBackgroundColor:[Webcolor webColorNamed:color]];
}

-(void)setBackgroundImage:(id)image
{
	TiRootViewController *controller = [[TiApp app] controller];
	UIImage *resultImage = [[ImageLoader sharedLoader] loadImmediateStretchableImage:[TiUtils toURL:image proxy:self]];
	if (resultImage==nil && [image isEqualToString:@"Default.png"])
	{
		// special case where we're asking for Default.png and it's in Bundle not path
		resultImage = [UIImage imageNamed:image];
	}
	[controller setBackgroundImage:resultImage];
}

#pragma mark Factory methods 

#ifdef USE_TI_UI2DMATRIX
-(id)create2DMatrix:(id)args
{
	if (args==nil || [args count] == 0)
	{
		return [[[Ti2DMatrix alloc] init] autorelease];
	}
	ENSURE_SINGLE_ARG(args,NSDictionary);
	Ti2DMatrix *matrix = [[Ti2DMatrix alloc] initWithProperties:args];
	return [matrix autorelease];
}
#endif


#ifdef USE_TI_UIANIMATION
-(id)createAnimation:(id)args
{
	if (args!=nil && [args isKindOfClass:[NSArray class]])
	{
		id properties = [args objectAtIndex:0];
		id callback = [args count] > 1 ? [args objectAtIndex:1] : nil;
		ENSURE_TYPE_OR_NIL(callback,KrollCallback);
		if ([properties isKindOfClass:[NSDictionary class]])
		{
			TiAnimation *a = [[[TiAnimation alloc] initWithDictionary:properties context:[self pageContext] callback:callback] autorelease];
			return a;
		}
	}
	return [[[TiAnimation alloc] _initWithPageContext:[self executionContext]] autorelease];
}
#endif

-(void)setOrientation:(id)mode
{
    DebugLog(@"Ti.UI.setOrientation is deprecated since 1.7.2 . Ignoring call.");
    return;
}

MAKE_SYSTEM_PROP(PORTRAIT,UIInterfaceOrientationPortrait);
MAKE_SYSTEM_PROP(LANDSCAPE_LEFT,UIInterfaceOrientationLandscapeLeft);
MAKE_SYSTEM_PROP(LANDSCAPE_RIGHT,UIInterfaceOrientationLandscapeRight);
MAKE_SYSTEM_PROP(UPSIDE_PORTRAIT,UIInterfaceOrientationPortraitUpsideDown);
MAKE_SYSTEM_PROP(UNKNOWN,UIDeviceOrientationUnknown);
MAKE_SYSTEM_PROP(FACE_UP,UIDeviceOrientationFaceUp);
MAKE_SYSTEM_PROP(FACE_DOWN,UIDeviceOrientationFaceDown);

MAKE_SYSTEM_PROP(EXTEND_EDGE_NONE,0);   //UIRectEdgeNone
MAKE_SYSTEM_PROP(EXTEND_EDGE_TOP,1);    //UIRectEdgeTop
MAKE_SYSTEM_PROP(EXTEND_EDGE_LEFT,2);   //UIEdgeRectLeft
MAKE_SYSTEM_PROP(EXTEND_EDGE_BOTTOM,4); //UIEdgeRectBottom
MAKE_SYSTEM_PROP(EXTEND_EDGE_RIGHT,8);  //UIEdgeRectRight
MAKE_SYSTEM_PROP(EXTEND_EDGE_ALL,15);   //UIEdgeRectAll

-(NSString*)TEXT_STYLE_HEADLINE
{
    return [TiUtils isIOS7OrGreater] ? UIFontTextStyleHeadline : @"INVALID";
}
-(NSString*)TEXT_STYLE_SUBHEADLINE
{
    return [TiUtils isIOS7OrGreater] ? UIFontTextStyleSubheadline : @"INVALID";
}
-(NSString*)TEXT_STYLE_BODY
{
    return [TiUtils isIOS7OrGreater] ? UIFontTextStyleBody : @"INVALID";
}
-(NSString*)TEXT_STYLE_FOOTNOTE
{
    return [TiUtils isIOS7OrGreater] ? UIFontTextStyleFootnote : @"INVALID";
}
-(NSString*)TEXT_STYLE_CAPTION1
{
    return [TiUtils isIOS7OrGreater] ? UIFontTextStyleCaption1 : @"INVALID";
}
-(NSString*)TEXT_STYLE_CAPTION2
{
    return [TiUtils isIOS7OrGreater] ? UIFontTextStyleCaption2 : @"INVALID";
}

-(NSNumber*)isLandscape:(id)args
{
	return NUMBOOL([UIApplication sharedApplication].statusBarOrientation!=UIInterfaceOrientationPortrait);
}

-(NSNumber*)isPortrait:(id)args
{
	return NUMBOOL([UIApplication sharedApplication].statusBarOrientation==UIInterfaceOrientationPortrait);
}

//Deprecated since 1.7.2
-(NSNumber*)orientation
{
    DebugLog(@"Ti.UI.orientation is deprecated since 1.7.2 .");
	return NUMINT([UIApplication sharedApplication].statusBarOrientation);
}

#pragma mark iPhone namespace

#ifdef USE_TI_UIIPHONE
-(id)iPhone
{
	if (iphone==nil)
	{
		// cache it since it's used alot
		iphone = [[TiUIiPhoneProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:iphone];
	}
	return iphone;
}
#endif

#ifdef USE_TI_UIIPAD
-(id)iPad
{
	if (ipad==nil)
	{
        ipad = [[TiUIiPadProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:ipad];
	}
	return ipad;
}
#endif

#ifdef USE_TI_UIIOS
-(id)iOS
{
	if (ios==nil)
	{
        ios = [[TiUIiOSProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:ios];
	}
	return ios;
}
#endif

#ifdef USE_TI_UI3DMATRIX
 -(id)create3DMatrix:(id)args
{
    if (args==nil || [args count] == 0)
	{
	    return [[[Ti3DMatrix alloc] init] autorelease];
	}
 	ENSURE_SINGLE_ARG(args,NSDictionary);
 	Ti3DMatrix *matrix = [[Ti3DMatrix alloc] initWithProperties:args];
 	return [matrix autorelease];
}
#endif

#ifdef USE_TI_UICLIPBOARD
-(id)Clipboard
{
	if (clipboard==nil)
	{
		clipboard = [[TiUIClipboardProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:clipboard];
	}
	return clipboard;
}
#endif

#ifdef USE_TI_UICOVERFLOWVIEW
-(id)createCoverFlowView:(id)args
{
	DEPRECATED_REPLACED(@"UI.createCoverFlowView()",@"1.8.0",@"Ti.UI.iOS.createCoverFlowView()");
	return [[[TiUIiOSCoverFlowViewProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UITOOLBAR
-(id)createToolbar:(id)args
{
	DEPRECATED_REPLACED(@"UI.createToolBar()",@"1.8.0",@"Ti.UI.iOS.createToolbar()");
	return [[[TiUIiOSToolbarProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UITABBEDBAR
-(id)createTabbedBar:(id)args
{
    DEPRECATED_REPLACED(@"UI.createTabbedBar()", @"1.8.0",@"Ti.UI.iOS.createTabbedBar()");
    return [[[TiUIiOSTabbedBarProxy alloc] _initWithPageContext:[self executionContext] args:args] autorelease];
}
#endif

#ifdef USE_TI_UIACTIVITYINDICATORSTYLE
-(id)ActivityIndicatorStyle
{
	if (activityIndicatorStyle==nil)
	{
		activityIndicatorStyle = [[TiUIActivityIndicatorStyleProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:activityIndicatorStyle];
	}
	return activityIndicatorStyle;
}
#ifdef USE_TI_UITABLEVIEWSEPARATORSTYLE
-(id)TableViewSeparatorStyle
{
	if (tableViewSeparatorStyle==nil)
	{
		tableViewSeparatorStyle = [[TiUITableViewSeparatorStyleProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:tableViewSeparatorStyle];
	}
	return tableViewSeparatorStyle;
}
#endif
#ifdef USE_TI_UITRANSITIONSTYLE
-(id)TransitionStyle
{
	if (transitionStyle==nil)
	{
		transitionStyle = [[TiUITransitionStyleProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:transitionStyle];
	}
	return transitionStyle;
}
#endif
#ifdef USE_TI_UIBLENDMODE
-(id)BlendMode
{
	if (blendMode==nil)
	{
		blendMode = [[TiUIBlendModeProxy alloc] _initWithPageContext:[self executionContext]];
        [self rememberProxy:blendMode];
	}
	return blendMode;
}
#endif
#endif
#pragma mark Internal Memory Management

-(void)didReceiveMemoryWarning:(NSNotification*)notification
{
#ifdef USE_TI_UIIPHONE
	FORGET_AND_RELEASE(iphone);
#endif
#ifdef USE_TI_UIIPAD
	FORGET_AND_RELEASE(ipad);
#endif
#ifdef USE_TI_UIIOS
	FORGET_AND_RELEASE(ios);
#endif
#ifdef USE_TI_UICLIPBOARD
	FORGET_AND_RELEASE(clipboard);
#endif
#ifdef USE_TI_UIACTIVITYINDICATORSTYLE
	FORGET_AND_RELEASE(activityIndicatorStyle);
#endif
#ifdef USE_TI_UITABLEVIEWSEPARATORSTYLE
	FORGET_AND_RELEASE(tableViewSeparatorStyle);
#endif
#ifdef USE_TI_UILISTVIEWSEPARATORSTYLE
	FORGET_AND_RELEASE(listViewSeparatorStyle);
#endif
#ifdef USE_TI_UITRANSITIONSTYLE
	FORGET_AND_RELEASE(transitionStyle);
#endif
#ifdef USE_TI_UIBLENDMODE
	FORGET_AND_RELEASE(blendMode);
#endif
	[super didReceiveMemoryWarning:notification];
}

-(NSString*)SIZE
{
    return kTiBehaviorSize;
}
-(NSString*)FILL
{
    return kTiBehaviorFill;
}
-(NSString*)UNIT_PX
{
    return kTiUnitPixel;
}
-(NSString*)UNIT_CM
{
    return kTiUnitCm;
}
-(NSString*)UNIT_MM
{
    return kTiUnitMm;
}
-(NSString*)UNIT_IN
{
    return kTiUnitInch;
}
-(NSString*)UNIT_DIP
{
    return kTiUnitDip;
}

-(NSNumber*)convertUnits:(id)args
{
    ENSURE_ARG_COUNT(args, 2);
    
	NSString* convertFromValue = nil;
	NSString* convertToUnits = nil;
    
	ENSURE_ARG_AT_INDEX(convertFromValue, args, 0, NSString);
	ENSURE_ARG_AT_INDEX(convertToUnits, args, 1, NSString);  
    
    float result = 0.0;
    if (convertFromValue != nil && convertToUnits != nil) {
        //Convert to DIP first
        TiDimension fromVal = TiDimensionFromObject(convertFromValue);
        
        if (TiDimensionIsDip(fromVal)) {
            if ([convertToUnits caseInsensitiveCompare:kTiUnitDip]==NSOrderedSame) {
                result = fromVal.value;
            }
            else if ([convertToUnits caseInsensitiveCompare:kTiUnitPixel]==NSOrderedSame) {
                if ([TiUtils isRetinaDisplay]) {
                    result = fromVal.value*2;
                }
                else {
                    result = fromVal.value;
                }
            }
            else if ([convertToUnits caseInsensitiveCompare:kTiUnitInch]==NSOrderedSame) {
                result = convertDipToInch(fromVal.value);
            }
            else if ([convertToUnits caseInsensitiveCompare:kTiUnitCm]==NSOrderedSame) {
                result = convertDipToInch(fromVal.value)*INCH_IN_CM;
            }
            else if ([convertToUnits caseInsensitiveCompare:kTiUnitMm]==NSOrderedSame) {
                result = convertDipToInch(fromVal.value)*INCH_IN_MM;
            }
        }
    }
    
    return [NSNumber numberWithFloat:result];
}


@end

#endif