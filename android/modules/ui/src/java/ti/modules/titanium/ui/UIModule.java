/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiRootActivity;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.transition.TransitionHelper;
import org.appcelerator.titanium.util.TiColorHelper;
import org.appcelerator.titanium.util.TiOrientationHelper;
import org.appcelerator.titanium.util.TiUIHelper;

import ti.modules.titanium.ui.widget.TiUIActivityIndicator;
import ti.modules.titanium.ui.widget.TiUITableView;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.Message;
import android.text.util.Linkify;
import android.view.View;
import android.webkit.WebViewClient;
import android.widget.Toast;

@Kroll.module
@Kroll.dynamicApis(properties = {
	"currentWindow"
})
public class UIModule extends KrollModule implements Handler.Callback
{
	private static final String TAG = "TiUIModule";

	@Kroll.constant public static final int RETURNKEY_GO = 0;
	@Kroll.constant public static final int RETURNKEY_GOOGLE = 1;
	@Kroll.constant public static final int RETURNKEY_JOIN = 2;
	@Kroll.constant public static final int RETURNKEY_NEXT = 3;
	@Kroll.constant public static final int RETURNKEY_ROUTE = 4;
	@Kroll.constant public static final int RETURNKEY_SEARCH = 5;
	@Kroll.constant public static final int RETURNKEY_YAHOO = 6;
	@Kroll.constant public static final int RETURNKEY_DONE = 7;
	@Kroll.constant public static final int RETURNKEY_EMERGENCY_CALL = 8;
	@Kroll.constant public static final int RETURNKEY_DEFAULT = 9;
	@Kroll.constant public static final int RETURNKEY_SEND = 10;

	@Kroll.constant public static final int KEYBOARD_APPEARANCE_DEFAULT = -1; // Not supported
	@Kroll.constant public static final int KEYBOARD_APPEARANCE_ALERT = -1; // Not supported

	@Kroll.constant public static final int KEYBOARD_ASCII = 0;
	@Kroll.constant public static final int KEYBOARD_NUMBERS_PUNCTUATION = 1;
	@Kroll.constant public static final int KEYBOARD_URL = 2;
	@Kroll.constant public static final int KEYBOARD_NUMBER_PAD = 3;
	@Kroll.constant public static final int KEYBOARD_PHONE_PAD = 4;
	@Kroll.constant public static final int KEYBOARD_EMAIL = 5;
	@Kroll.constant public static final int KEYBOARD_NAMEPHONE_PAD = 6;
	@Kroll.constant public static final int KEYBOARD_DEFAULT = 7;
	@Kroll.constant public static final int KEYBOARD_DECIMAL_PAD = 8;
	
	@Kroll.constant public static final int AUTOLINK_ALL = Linkify.ALL;
	@Kroll.constant public static final int AUTOLINK_EMAIL_ADDRESSES = Linkify.EMAIL_ADDRESSES;
	@Kroll.constant public static final int AUTOLINK_MAP_ADDRESSES = Linkify.MAP_ADDRESSES;
	@Kroll.constant public static final int AUTOLINK_PHONE_NUMBERS = Linkify.PHONE_NUMBERS;
	@Kroll.constant public static final int AUTOLINK_URLS = Linkify.WEB_URLS;
	@Kroll.constant public static final int AUTOLINK_NONE = 16;

	@Kroll.constant public static final int INPUT_BORDERSTYLE_NONE = 0;
	@Kroll.constant public static final int INPUT_BORDERSTYLE_ROUNDED = 1;
	@Kroll.constant public static final int INPUT_BORDERSTYLE_BEZEL = 2;
	@Kroll.constant public static final int INPUT_BORDERSTYLE_LINE = 3;
	@Kroll.constant public static final int INPUT_BUTTONMODE_ONFOCUS = 0;
	@Kroll.constant public static final int INPUT_BUTTONMODE_ALWAYS = 1;
	@Kroll.constant public static final int INPUT_BUTTONMODE_NEVER = 2;
	
	@Kroll.constant public static final String LIST_ITEM_TEMPLATE_DEFAULT = "listDefaultTemplate";
	@Kroll.constant public static final int LIST_ACCESSORY_TYPE_NONE = 0;
	@Kroll.constant public static final int LIST_ACCESSORY_TYPE_CHECKMARK = 1;
	@Kroll.constant public static final int LIST_ACCESSORY_TYPE_DETAIL = 2;
	@Kroll.constant public static final int LIST_ACCESSORY_TYPE_DISCLOSURE = 3;


	@Kroll.constant public static final int MAP_VIEW_STANDARD = 1;
	@Kroll.constant public static final int MAP_VIEW_SATELLITE = 2;
	@Kroll.constant public static final int MAP_VIEW_HYBRID = 3;
	
	@Kroll.constant public static final int SCALE_TYPE_SCALE_TO_FILL = 0; //ImageView.ScaleType.FIT_XY
	@Kroll.constant public static final int SCALE_TYPE_ASPECT_FIT = 1; //ImageView.ScaleType.FIT_CENTER
	@Kroll.constant public static final int SCALE_TYPE_ASPECT_FILL = 2; //ImageView.ScaleType.CENTER_CROP
	@Kroll.constant public static final int SCALE_TYPE_CENTER = 3; //ImageView.ScaleType.CENTER
	@Kroll.constant public static final int SCALE_TYPE_LEFT = 4; //ImageView.ScaleType.FIT_START
	@Kroll.constant public static final int SCALE_TYPE_RIGHT = 5; //ImageView.ScaleType.FIT_END

	@Kroll.constant public static final int TABLEVIEW_POSITION_ANY = 0;
	@Kroll.constant public static final int TABLEVIEW_POSITION_TOP = 1;
	@Kroll.constant public static final int TABLEVIEW_POSITION_MIDDLE = 2;
	@Kroll.constant public static final int TABLEVIEW_POSITION_BOTTOM = 3;
	
	@Kroll.constant public static final String TEXT_ALIGNMENT_LEFT = "left";
	@Kroll.constant public static final String TEXT_ALIGNMENT_CENTER = "center";
	@Kroll.constant public static final String TEXT_ALIGNMENT_RIGHT = "right";
	@Kroll.constant public static final String TEXT_VERTICAL_ALIGNMENT_BOTTOM = "bottom";
	@Kroll.constant public static final String TEXT_VERTICAL_ALIGNMENT_CENTER = "middle";
	@Kroll.constant public static final String TEXT_VERTICAL_ALIGNMENT_TOP = "top";
	@Kroll.constant public static final String TEXT_ELLIPSIS_NONE = "none";
	@Kroll.constant public static final String TEXT_ELLIPSIZE_HEAD = "START";
	@Kroll.constant public static final String TEXT_ELLIPSIZE_MIDDLE = "MIDDLE";
	@Kroll.constant public static final String TEXT_ELLIPSIZE_TAIL = "END";
	
	@Kroll.constant public static final int PORTRAIT = TiOrientationHelper.ORIENTATION_PORTRAIT;
	@Kroll.constant public static final int UPSIDE_PORTRAIT = TiOrientationHelper.ORIENTATION_PORTRAIT_REVERSE;
	@Kroll.constant public static final int LANDSCAPE_LEFT = TiOrientationHelper.ORIENTATION_LANDSCAPE;
	@Kroll.constant public static final int LANDSCAPE_RIGHT = TiOrientationHelper.ORIENTATION_LANDSCAPE_REVERSE;
	@Kroll.constant public static final int FACE_UP = TiUIHelper.FACE_UP;
	@Kroll.constant public static final int FACE_DOWN = TiUIHelper.FACE_DOWN;
	@Kroll.constant public static final int UNKNOWN = TiOrientationHelper.ORIENTATION_UNKNOWN;
	
	@Kroll.constant public static final int PICKER_TYPE_PLAIN = -1;
	@Kroll.constant public static final int PICKER_TYPE_TIME = 0;
	@Kroll.constant public static final int PICKER_TYPE_DATE = 1;
	@Kroll.constant public static final int PICKER_TYPE_DATE_AND_TIME = 2;
	@Kroll.constant public static final int PICKER_TYPE_COUNT_DOWN_TIMER = 3;
	
	@Kroll.constant public static final int NOTIFICATION_DURATION_LONG = Toast.LENGTH_LONG;
	@Kroll.constant public static final int NOTIFICATION_DURATION_SHORT = Toast.LENGTH_SHORT;
	
	@Kroll.constant public static final int TEXT_AUTOCAPITALIZATION_NONE = 0;
	@Kroll.constant public static final int TEXT_AUTOCAPITALIZATION_SENTENCES = 1;
	@Kroll.constant public static final int TEXT_AUTOCAPITALIZATION_WORDS = 2;
	@Kroll.constant public static final int TEXT_AUTOCAPITALIZATION_ALL = 3;
	
//	@Kroll.constant public static final int TEXT_ELLIPSIZE_TRUNCATE_START = 0;
//	@Kroll.constant public static final int TEXT_ELLIPSIZE_TRUNCATE_MIDDLE = 1;
//	@Kroll.constant public static final int TEXT_ELLIPSIZE_TRUNCATE_END = 2;
//	@Kroll.constant public static final int TEXT_ELLIPSIZE_TRUNCATE_MARQUEE = 3;

	@Kroll.constant public static final String SIZE = TiC.LAYOUT_SIZE;
    @Kroll.constant public static final String FILL = TiC.LAYOUT_FILL;
    @Kroll.constant public static final String MATCH = TiC.LAYOUT_MATCH;
	@Kroll.constant public static final String UNIT_PX = TiDimension.UNIT_PX;
	@Kroll.constant public static final String UNIT_MM = TiDimension.UNIT_MM;
	@Kroll.constant public static final String UNIT_CM = TiDimension.UNIT_CM;
	@Kroll.constant public static final String UNIT_IN = TiDimension.UNIT_IN;
	@Kroll.constant public static final String UNIT_DIP = TiDimension.UNIT_DIP;

	// TiWebViewClient onReceivedError error codes.
	@Kroll.constant public static final int URL_ERROR_AUTHENTICATION = WebViewClient.ERROR_AUTHENTICATION;
	@Kroll.constant public static final int URL_ERROR_BAD_URL = WebViewClient.ERROR_BAD_URL;
	@Kroll.constant public static final int URL_ERROR_CONNECT = WebViewClient.ERROR_CONNECT;
	@Kroll.constant public static final int URL_ERROR_SSL_FAILED = WebViewClient.ERROR_FAILED_SSL_HANDSHAKE;
	@Kroll.constant public static final int URL_ERROR_FILE = WebViewClient.ERROR_FILE;
	@Kroll.constant public static final int URL_ERROR_FILE_NOT_FOUND = WebViewClient.ERROR_FILE_NOT_FOUND;
	@Kroll.constant public static final int URL_ERROR_HOST_LOOKUP = WebViewClient.ERROR_HOST_LOOKUP;
	@Kroll.constant public static final int URL_ERROR_REDIRECT_LOOP = WebViewClient.ERROR_REDIRECT_LOOP;
	@Kroll.constant public static final int URL_ERROR_TIMEOUT = WebViewClient.ERROR_TIMEOUT;
	@Kroll.constant public static final int URL_ERROR_UNKNOWN = WebViewClient.ERROR_UNKNOWN;
	@Kroll.constant public static final int URL_ERROR_UNSUPPORTED_SCHEME = WebViewClient.ERROR_UNSUPPORTED_SCHEME;
	
	@Kroll.constant public static final int ATTRIBUTE_FONT = 0;
	@Kroll.constant public static final int ATTRIBUTE_FOREGROUND_COLOR = 1;
	@Kroll.constant public static final int ATTRIBUTE_BACKGROUND_COLOR = 2;
	@Kroll.constant public static final int ATTRIBUTE_STRIKETHROUGH_STYLE = 3;
	@Kroll.constant public static final int ATTRIBUTE_UNDERLINES_STYLE = 4;
	@Kroll.constant public static final int ATTRIBUTE_LINK = 5;
	@Kroll.constant public static final int ATTRIBUTE_UNDERLINE_COLOR = 6;
	
	@Kroll.constant public static final int LEFT_VIEW = 0;
    @Kroll.constant public static final int RIGHT_VIEW = 1;
    
    private static KrollDict BlendMode = null;
    @Kroll.getProperty
    public KrollDict BlendMode() {
        if (BlendMode == null) {
            BlendMode = new KrollDict();
            BlendMode.put("DARKEN", Mode.DARKEN.ordinal());
            BlendMode.put("LIGHTEN", Mode.LIGHTEN.ordinal());
            BlendMode.put("MULTIPLY", Mode.MULTIPLY.ordinal());
            BlendMode.put("ADD", (TiC.HONEYCOMB_OR_GREATER?Mode.ADD.ordinal():Mode.SRC_OVER.ordinal()));
            BlendMode.put("SCREEN", Mode.SCREEN.ordinal());
            BlendMode.put("CLEAR", Mode.CLEAR.ordinal());
            BlendMode.put("DST", Mode.DST.ordinal());
            BlendMode.put("DST_ATOP", Mode.DST_ATOP.ordinal());
            BlendMode.put("DST_IN", Mode.DST_IN.ordinal());
            BlendMode.put("DST_OUT", Mode.DST_OUT.ordinal());
            BlendMode.put("DST_OVER", Mode.DST_OVER.ordinal());
            BlendMode.put("SRC_ATOP", Mode.SRC_ATOP.ordinal());
            BlendMode.put("SRC_IN", Mode.SRC_IN.ordinal());
            BlendMode.put("SRC_OUT", Mode.SRC_OUT.ordinal());
            BlendMode.put("SRC_OVER", Mode.SRC_OVER.ordinal());
            BlendMode.put("OVERLAY", TiC.HONEYCOMB_OR_GREATER?Mode.OVERLAY.ordinal():Mode.MULTIPLY.ordinal());
            BlendMode.put("XOR", Mode.XOR.ordinal());
        }
        return BlendMode;
    }
    
    private static KrollDict TableViewSeparatorStyle= null;
    @Kroll.getProperty
    public KrollDict TableViewSeparatorStyle() {
        if (TableViewSeparatorStyle == null) {
            TableViewSeparatorStyle = new KrollDict();
            TableViewSeparatorStyle.put("NONE", TiUITableView.SEPARATOR_NONE);
            TableViewSeparatorStyle.put("SINGLE_LINE", TiUITableView.SEPARATOR_SINGLE_LINE);
        }
        return TableViewSeparatorStyle;
    }
    
    @Kroll.getProperty
    public KrollDict ListViewSeparatorStyle() {
        return TableViewSeparatorStyle();
    }
    
    private static KrollDict  TransitionStyle = null;
    @Kroll.getProperty
    public KrollDict  TransitionStyle() {
        if ( TransitionStyle == null) {
            TransitionStyle = new KrollDict();
            TransitionStyle.put("CUBE", TransitionHelper.Types.kTransitionCube.ordinal());
            TransitionStyle.put("CAROUSEL", TransitionHelper.Types.kTransitionCarousel.ordinal());
            TransitionStyle.put("SWIPE", TransitionHelper.Types.kTransitionSwipe.ordinal());
            TransitionStyle.put("SWIPE_FADE", TransitionHelper.Types.kTransitionSwipeFade.ordinal());
            TransitionStyle.put("FLIP",TransitionHelper.Types.kTransitionFlip.ordinal());
            TransitionStyle.put("FADE",TransitionHelper.Types.kTransitionFade.ordinal());
            TransitionStyle.put("BACK_FADE", TransitionHelper.Types.kTransitionBackFade.ordinal());
            TransitionStyle.put("FOLD", TransitionHelper.Types.kTransitionFold.ordinal());
            TransitionStyle.put("PUSH_ROTATE", TransitionHelper.Types.kTransitionPushRotate.ordinal());
            TransitionStyle.put("SCALE", TransitionHelper.Types.kTransitionScale.ordinal());
            TransitionStyle.put("SLIDE", TransitionHelper.Types.kTransitionSlide.ordinal());
            TransitionStyle.put("SWIPE_DUAL_FADE", TransitionHelper.Types.kTransitionSwipeDualFade.ordinal());
            TransitionStyle.put("MODERN_PUSH", TransitionHelper.Types.kTransitionModernPush.ordinal());
        }
        return  TransitionStyle;
    }
    
    private static KrollDict  TransitionSubStyle = null;
    @Kroll.getProperty
    public KrollDict  TransitionSubStyle() {
        if ( TransitionSubStyle == null) {
            TransitionSubStyle = new KrollDict();
            TransitionSubStyle.put("LEFT_TO_RIGHT", TransitionHelper.SubTypes.kLeftToRight.ordinal());
            TransitionSubStyle.put("RIGHT_TO_LEFT", TransitionHelper.SubTypes.kRightToLeft.ordinal());
            TransitionSubStyle.put("TOP_TO_BOTTOM", TransitionHelper.SubTypes.kTopToBottom.ordinal());
            TransitionSubStyle.put("BOTTOM_TO_TOP", TransitionHelper.SubTypes.kBottomToTop.ordinal());

        }
        return  TransitionSubStyle;
    }
    private static KrollDict  ActivityIndicatorStyle = null;
    @Kroll.getProperty
    public KrollDict  ActivityIndicatorStyle() {
        if ( ActivityIndicatorStyle == null) {
            ActivityIndicatorStyle = new KrollDict();
            ActivityIndicatorStyle.put("PLAIN", TiUIActivityIndicator.PLAIN);
            ActivityIndicatorStyle.put("BIG", TiUIActivityIndicator.BIG);
            ActivityIndicatorStyle.put("BIG_DARK", TiUIActivityIndicator.BIG_DARK);
            ActivityIndicatorStyle.put("DARK", TiUIActivityIndicator.DARK);

        }
        return  ActivityIndicatorStyle;
    }
    
	    
	@SuppressLint("InlinedApi")
	@Kroll.constant public static final int INFINITE = ValueAnimator.INFINITE;

	protected static final int MSG_SET_BACKGROUND_COLOR = KrollProxy.MSG_LAST_ID + 100;
	protected static final int MSG_SET_BACKGROUND_IMAGE = KrollProxy.MSG_LAST_ID + 101;
	protected static final int MSG_LAST_ID = MSG_SET_BACKGROUND_IMAGE;
	
	public UIModule()
	{
		super();
	}

	public UIModule(TiContext tiContext)
	{
		this();
	}

	@Kroll.setProperty(runOnUiThread=true) @Kroll.method(runOnUiThread=true)
	public void setBackgroundColor(String color)
	{
		if (TiApplication.isUIThread()) {
			doSetBackgroundColor(color);

		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_BACKGROUND_COLOR, color);
			message.sendToTarget();
		}
	}

	protected void doSetBackgroundColor(String color)
	{
		TiRootActivity root = TiApplication.getInstance().getRootActivity();
		if (root != null) {
			root.setBackgroundColor(color != null ? TiColorHelper.parseColor(color) : Color.TRANSPARENT);
		}
	}

	@Kroll.setProperty(runOnUiThread=true) @Kroll.method(runOnUiThread=true)
	public void setBackgroundImage(Object image)
	{
		if (TiApplication.isUIThread()) {
			doSetBackgroundImage(image);

		} else {
			Message message = getMainHandler().obtainMessage(MSG_SET_BACKGROUND_IMAGE, image);
			message.sendToTarget();
		}
	}

	protected void doSetBackgroundImage(Object image)
	{
		TiRootActivity root = TiApplication.getInstance().getRootActivity();
		if (root != null) {
			root.setBackgroundImage(TiUIHelper.getResourceDrawable(image));
		}
	}


	@Kroll.method
	public double convertUnits(String convertFromValue, String convertToUnits)
	{
		double result = 0;
		TiDimension dimension = new TiDimension(convertFromValue, TiDimension.TYPE_UNDEFINED);

		// TiDimension needs a view to grab the window manager, so we'll just use the decorview of the current window
		View view = TiApplication.getAppCurrentActivity().getWindow().getDecorView();

		if (view != null) {
			if (convertToUnits.equals(UNIT_PX)) {
				result = (double) dimension.getAsPixels(view);
			} else if (convertToUnits.equals(UNIT_MM)) {
				result = dimension.getAsMillimeters(view);
			} else if (convertToUnits.equals(UNIT_CM)) {
				result = dimension.getAsCentimeters(view);
			} else if (convertToUnits.equals(UNIT_IN)) {
				result = dimension.getAsInches(view);
			} else if (convertToUnits.equals(UNIT_DIP)) {
				result = (double) dimension.getAsDIP(view);
			}
		}

		return result;
	}

	protected void doSetOrientation(int tiOrientationMode)
	{
		Activity activity = TiApplication.getInstance().getCurrentActivity();
		if (activity instanceof TiBaseActivity)
		{
			int[] orientationModes;

			if (tiOrientationMode == -1)
			{
				orientationModes = new int[] {};
			}
			else
			{
				orientationModes = new int[] {tiOrientationMode};
			}

			// this should only be entered if a LW window is created on top of the root activity
			TiBaseActivity tiBaseActivity = (TiBaseActivity)activity;
			TiWindowProxy windowProxy = tiBaseActivity.getWindowProxy();

			windowProxy.setOrientationModes(orientationModes);
		}	
	}

	public boolean handleMessage(Message message)
	{
		switch (message.what) {
			case MSG_SET_BACKGROUND_COLOR: {
				doSetBackgroundColor((String)message.obj);

				return true;
			}
			case MSG_SET_BACKGROUND_IMAGE: {
				doSetBackgroundImage(message.obj);

				return true;
			}
		}

		return super.handleMessage(message);
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI";
	}
}
