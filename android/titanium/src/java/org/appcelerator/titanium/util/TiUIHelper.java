/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.CurrentActivityListener;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.proxy.ParentingProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy;
import org.appcelerator.titanium.proxy.TiWindowProxy.PostOpenListener;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.view.TiBackgroundDrawable;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiGradientDrawable;
import org.appcelerator.titanium.view.TiUIView;

import com.squareup.picasso.Cache;
import com.trevorpage.tpsvg.SVGDrawable;
import com.trevorpage.tpsvg.SVGFlyweightFactory;
import com.udojava.evalex.Expression;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.v7.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

/**
 * A set of utility methods focused on UI and View operations.
 */
@SuppressWarnings("deprecation")
public class TiUIHelper
{
	private static final String TAG = "TiUIHelper";
	private static final String customFontPath = "Resources/fonts";

	public static final int PORTRAIT = 1;
	public static final int UPSIDE_PORTRAIT = 2;
	public static final int LANDSCAPE_LEFT = 3;
	public static final int LANDSCAPE_RIGHT = 4;
	public static final int FACE_UP = 5;
	public static final int FACE_DOWN = 6;
	public static final int UNKNOWN = 7;
	public static final Pattern SIZED_VALUE = Pattern.compile("(-?[0-9]*\\.?[0-9]+)\\W*(px|dp|dip|sp|sip|mm|cm|pt|in)?");
	public static final String MIME_TYPE_PNG = "image/png";

	private static Method overridePendingTransition;
	private static Map<String, String> resourceImageKeys = Collections.synchronizedMap(new HashMap<String, String>());
	private static Map<String, Typeface> mCustomTypeFaces = Collections.synchronizedMap(new HashMap<String, Typeface>());
	
	public static class FontDesc {
		public Float size = 15.0f;
		public int sizeUnit = TypedValue.COMPLEX_UNIT_PX;
		public Typeface typeface = null;
		public int style = Typeface.NORMAL;
		
		public void setDefaults(final Context context){
			typeface = toTypeface(context, null);
			float[] result = new float[2];
			getSizeAndUnits(null, result);
			sizeUnit = (int)result[0];
			size = result[1];
		}
	}
	
	public static class Shadow {
		public float radius = 3;
		public float dx = 0;
		public float dy = 0;
		public int color = Color.BLACK;
		
		public Shadow() 
		{
		}
		public Shadow(int color) 
		{
			this.color = color;
		}
	}
	
	public static OnClickListener createDoNothingListener() {
		return new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// Do nothing
			}
		};
	}

	public static OnClickListener createKillListener() {
		return new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Process.killProcess(Process.myPid());
			}
		};
	}

	public static OnClickListener createFinishListener(final Activity me) {
		return new OnClickListener(){
			public void onClick(DialogInterface dialog, int which) {
				me.finish();
			}
		};
	}

	public static void doKillOrContinueDialog(Context context, String title, String message, OnClickListener positiveListener, OnClickListener negativeListener) {
		if (positiveListener == null) {
			positiveListener = createDoNothingListener();
		}
		if (negativeListener == null) {
			negativeListener = createKillListener();
		}
		
		new AlertDialog.Builder(context).setTitle(title).setMessage(message)
			.setPositiveButton("Continue", positiveListener)
			.setNegativeButton("Kill", negativeListener)
			.setCancelable(false).create().show();
	}
	
	public static void linkifyIfEnabled(final TextView tv, final Object autoLink)
	{ 
		if (autoLink != null) {
			//Default to Ti.UI.AUTOLINK_NONE
			boolean success = Linkify.addLinks(tv, TiConvert.toInt(autoLink, 16));
			if (!success && tv.getText() instanceof Spanned) {
				tv.setMovementMethod(LinkMovementMethod.getInstance());
			}
		}
	}

	/**
	 * Waits for the current activity to be ready, then invokes
	 * {@link CurrentActivityListener#onCurrentActivityReady(Activity)}.
	 * @param l the CurrentActivityListener.
	 */
	public static void waitForCurrentActivity(final CurrentActivityListener l)
	{
		// Some window opens are async, so we need to make sure we don't
		// sandwich ourselves in between windows when transitioning
		// between activities TIMOB-3644
		TiWindowProxy waitingForOpen = TiWindowProxy.getWaitingForOpen();
		if (waitingForOpen != null) {
			waitingForOpen.setPostOpenListener(new PostOpenListener() {
				// TODO @Override
				public void onPostOpen(TiWindowProxy window)
				{
					TiApplication app = TiApplication.getInstance();
					Activity activity = app.getCurrentActivity();
					if (activity != null) {
						l.onCurrentActivityReady(activity);
					}
				}
			});
		} else {
			TiApplication app = TiApplication.getInstance();
			Activity activity = app.getCurrentActivity();
			if (activity != null) {
				l.onCurrentActivityReady(activity);
			}
		}
	}

	/**
	 * Creates and shows a dialog with an OK button given title and message.
	 * The dialog's creation context is the current activity.
	 * @param title  the title of dialog.
	 * @param message  the dialog's message.
	 * @param listener the click listener for click events.
	 */
	public static void doOkDialog(final String title, final String message, OnClickListener listener) {
		if (listener == null) {
			listener = new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Activity ownerActivity = ((AlertDialog)dialog).getOwnerActivity();
					//if activity is not finishing, remove dialog to free memory
					if (ownerActivity != null && !ownerActivity.isFinishing()) {
						((TiBaseActivity)ownerActivity).removeDialog((AlertDialog) dialog);
					}
				}};
		}
		final OnClickListener fListener = listener;
		waitForCurrentActivity(new CurrentActivityListener() {
			// TODO @Override
			public void onCurrentActivityReady(final Activity activity)
			{
				//add dialog to activity for cleaning up purposes
				if (!activity.isFinishing()) {
					AlertDialog dialog = new AlertDialog.Builder(activity).setTitle(title).setMessage(message)
							.setPositiveButton(android.R.string.ok, fListener)
							.setCancelable(false).create();
					if (activity instanceof TiBaseActivity) {
						TiBaseActivity baseActivity = (TiBaseActivity) activity;
						baseActivity.addDialog(baseActivity.new DialogWrapper(dialog, true, new WeakReference<TiBaseActivity>(baseActivity)));
						dialog.setOwnerActivity(activity);
					}
					dialog.setOnShowListener(new DialogInterface.OnShowListener(){
				        @Override
				        public void onShow(DialogInterface dialog) {
				        	TiApplication.getInstance().cancelPauseEvent();
				        }
					});
					dialog.show();

				}

			}
		});
	}

	public static int toTypefaceStyle(final String fontWeight, final String fontStyle)
	{
		int style = Typeface.NORMAL;

		if (fontWeight != null) {
			if (fontWeight.equals("bold")) {
				if (fontStyle != null && fontStyle.equals("italic")) {
					style = Typeface.BOLD_ITALIC;
				} else {
					style = Typeface.BOLD;
				}
			} else if (fontStyle != null && fontStyle.equals("italic")) {
				style = Typeface.ITALIC;
			}
		} else if (fontStyle != null && fontStyle.equals("italic")) {
			style = Typeface.ITALIC;
		}
		return style;
	}

	public static int getSizeUnits(final String size) {
		int units = TypedValue.COMPLEX_UNIT_PX;
		String unitString = null;

		if (size != null) {
			Matcher m = SIZED_VALUE.matcher(size.trim());
			if (m.matches()) {
				if (m.groupCount() == 2) {
					unitString = m.group(2);
				}
			}
		}

		if (unitString == null) {
			unitString = TiApplication.getInstance().getDefaultUnit();
		}

		if (TiDimension.UNIT_PX.equals(unitString) || TiDimension.UNIT_SYSTEM.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_PX;
		} else if (TiDimension.UNIT_PT.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_PT;
		} else if (TiDimension.UNIT_DP.equals(unitString) || TiDimension.UNIT_DIP.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_DIP;
		} else if (TiDimension.UNIT_SP.equals(unitString) || TiDimension.UNIT_SIP.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_SP;
		} else if (TiDimension.UNIT_MM.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_MM;
		} else if (TiDimension.UNIT_CM.equals(unitString)) {
			units = TiDimension.COMPLEX_UNIT_CM;
		} else if (TiDimension.UNIT_IN.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_IN;
		} else {
			if (unitString != null) {
				Log.w(TAG, "Unknown unit: " + unitString, Log.DEBUG_MODE);
			}
		}

		return units;
	}
	
	public static void getSizeAndUnits(final String size, final float[] result) {
		int units = TypedValue.COMPLEX_UNIT_PX;
		float value = 15.0f;
		String unitString = null;

		if (size != null) {
			Matcher m = SIZED_VALUE.matcher(size.trim());
			if (m.matches()) {
				if (m.groupCount() == 2) {
					unitString = m.group(2);
					value = Float.parseFloat(m.group(1));
				}
			}
		}

		if (unitString == null) {
			unitString = TiApplication.getInstance().getDefaultUnit();
		}

		if (TiDimension.UNIT_PX.equals(unitString) || TiDimension.UNIT_SYSTEM.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_PX;
		} else if (TiDimension.UNIT_PT.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_PT;
		} else if (TiDimension.UNIT_DP.equals(unitString) || TiDimension.UNIT_DIP.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_DIP;
		} else if (TiDimension.UNIT_SP.equals(unitString) || TiDimension.UNIT_SIP.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_SP;
		} else if (TiDimension.UNIT_MM.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_MM;
		} else if (TiDimension.UNIT_CM.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_MM;
			value *= 10;
		} else if (TiDimension.UNIT_IN.equals(unitString)) {
			units = TypedValue.COMPLEX_UNIT_IN;
		} else {
			if (unitString != null) {
				Log.w(TAG, "Unknown unit: " + unitString, Log.DEBUG_MODE);
			}
		}
		result[0] = units;
		result[1] = value;
	}

	public static float getSize(final String size) {
		float value = 15.0f;
		if (size != null) {
			Matcher m = SIZED_VALUE.matcher(size.trim());
			if (m.matches()) {
				value = Float.parseFloat(m.group(1));
			}
		}

		return value;
	}
	
	public static float getRawSize(final int unit, final float size, final Context context) {
		Resources r;
		if (context != null) {
			r = context.getResources();
		} else {
			r = Resources.getSystem();
		}
		return TypedValue.applyDimension(unit, size, r.getDisplayMetrics());
	}

	public static float getRawSize(final int unit, final float size) {
		return getRawSize(unit, size, null);
	}
	
	public static float getRawDIPSize(final float size, final Context context) {
		return getRawSize(TypedValue.COMPLEX_UNIT_DIP, size, context);
	}
	
	public static float getRawSize(final String size, final Context context) {
		float[] result = new float[2];
		getSizeAndUnits(size, result);
		return getRawSize((int)result[0], result[1], context);
	}
	
	public static float getRawSize(final int size, final Context context) {
		float[] result = new float[2];
		getSizeAndUnits(null, result);
		return getRawSize((int)result[0], size, context);
	}

	public static float getInPixels(final String size) {
		
		return getRawSize(size, null);
	}
	
	public static float getInPixels(final String size, final Context context) {
		return getInPixels(size, 0.0f, context);
	}
	
	public static float getInPixels(final String size, final float defaultValue, final Context context) {
        if (size == null || size.length() == 0) {
            if (defaultValue > 0) {
                return getRawSize(TypedValue.COMPLEX_UNIT_DIP, defaultValue, context);
            }
            return 0;
        }
        return getRawSize(size, context);
    }

	public static float getInPixels(final HashMap dict, final String property,
	        final float defaultValue, final Context context) {
		if (dict.containsKey(property)) {
			return getRawSize(TiConvert.toString(dict.get(property)), context);
		}
		if (defaultValue > 0) {
	        return getRawSize(TypedValue.COMPLEX_UNIT_DIP, defaultValue, context);
		}
		return 0;
	}
	
	public static float getInPixels(final HashMap dict, final String property,
            Context context) {
        return getInPixels(dict, property, 0.0f, context);
    }

	public static float getInPixels(final HashMap dict, final String property) {
		return getInPixels(dict, property, null);
	}
	
    @Deprecated
	public static float getRawSizeOrZero(final HashMap dict, final String property) {
        return getInPixels(dict, property, null);
    }
	
	public static float getInPixels(final HashMap dict, final String property, final float defaultValue) {
        return getInPixels(dict, property, defaultValue, null);
    }

	public static float getInPixels(final Object value) {
		return getInPixels(TiConvert.toString(value), null);
	}
	
	@Deprecated
	public static float getRawSizeOrZero(final Object value) {
        return getInPixels(TiConvert.toString(value), null);
    }
	
	public static float getInPixels(final Object value, final float defaultValue) {
        return getInPixels(TiConvert.toString(value), defaultValue, null);
    }
	
	public static FontDesc getFontStyle(final Context context, final HashMap<String, Object> d) {
		FontDesc desc = new FontDesc();
		if (d == null) {
			desc.setDefaults(context);
			return desc;
		}
		String fontSize = null;
		if (d.containsKey("size")) {
			fontSize = TiConvert.toString(d, "size");
		}
		float[] result = new float[2];
		getSizeAndUnits(fontSize, result);
		desc.sizeUnit = (int)result[0]; 
		desc.size = result[1]; 
		
		String fontWeight = null;
		String fontStyle = null;
		if (d.containsKey("weight")) {
			fontWeight = TiConvert.toString(d, "weight");
		}
		if (d.containsKey("style")) {
			fontStyle = TiConvert.toString(d, "style");
		}
		desc.style = toTypefaceStyle(fontWeight, fontStyle);
		
		String fontFamily = null;
        if (d.containsKey("family")) {
            fontFamily = TiConvert.toString(d, "family");
        }
        if (fontWeight != null && desc.style == Typeface.NORMAL && 
                fontWeight != "normal") {
            desc.typeface = toTypeface(context, fontFamily, fontWeight);
        }
        else {
            desc.typeface = toTypeface(context, fontFamily, null);
        }
		return desc;
	}
	
	public static void setPadding(final View view, final RectF padding) {
		view.setPadding((int)padding.left, (int)padding.top, (int)padding.right,
				(int)padding.bottom);
	}

	public static void styleText(final TextView tv, final HashMap<String, Object> d) {
	    styleText(tv, getFontStyle(tv.getContext(), d));
	}
	
	public static void styleText(final TextView tv, final FontDesc desc) {
        tv.setTypeface(desc.typeface, desc.style);
        tv.setTextSize(desc.sizeUnit, desc.size);
    }
	
	public static boolean isAndroidTypeface(String fontFamily)
    {
        if (fontFamily != null) {
            if ("monospace".equals(fontFamily)) {
                return true;
            } else if ("serif".equals(fontFamily)) {
                return true;
            } else if ("sans-serif".equals(fontFamily)) {
                return true;
            }
        }
        return false;
    }

	public static Typeface toTypeface(final Context context, String fontFamily, String weight)
	{
		Typeface tf = Typeface.SANS_SERIF; // default
		if (weight != null) {
		    if (fontFamily == null && weight != "regular") {
	            fontFamily = "sans-serif-" + weight.toLowerCase();
		    }
		    else {
                fontFamily += "-" + weight.toLowerCase();
		    }
        }
		if (fontFamily != null) {
			if ("monospace".equals(fontFamily)) {
				tf = Typeface.MONOSPACE;
			} else if ("serif".equals(fontFamily)) {
				tf = Typeface.SERIF;
			} else if ("sans-serif".equals(fontFamily)) {
				tf = Typeface.SANS_SERIF;
			} else {
				Typeface loadedTf = null;
				if (context != null) {
					try {
						loadedTf = loadTypeface(context, fontFamily);
						
					} catch (Exception e) {
						loadedTf = null;
				Log.e(TAG, "Unable to load font " + fontFamily + ": " + e.getMessage());
					}
				}
				if (loadedTf == null) {
					Log.w(TAG, "Unsupported font: '" + fontFamily
						+ "' supported fonts are 'monospace', 'serif', 'sans-serif'.", Log.DEBUG_MODE);
				} else {
					tf = loadedTf;
				}
			}
		}
		return tf;
	}
	
	public static Typeface toTypeface(final Context context, String fontFamily)
    {
	    return toTypeface(context, fontFamily, null);
    }
	public static Typeface toTypeface(final String fontFamily) {
		return toTypeface(null, fontFamily);
	}

	@SuppressLint("DefaultLocale")
	private static Typeface loadTypeface(final Context context, final String fontFamily)
	{
		if (context == null) {
			return null;
		}
		if (mCustomTypeFaces.containsKey(fontFamily)) {
			return mCustomTypeFaces.get(fontFamily);
		}
		AssetManager mgr = context.getAssets();
		try {
		    Typeface tf = null;
		    String[] fontFiles = mgr.list(customFontPath);
            for (String f : fontFiles) {
                if (f.toLowerCase() == fontFamily.toLowerCase() || f.toLowerCase().startsWith(fontFamily.toLowerCase() + ".")) {
                    tf = Typeface.createFromAsset(mgr, customFontPath + "/" + f);
                    synchronized(mCustomTypeFaces) {
                        mCustomTypeFaces.put(fontFamily, tf);
                    }
                    return tf;
                }
            }
		    tf = Typeface.create(fontFamily, Typeface.NORMAL);
		    if (tf != null) {
		        synchronized(mCustomTypeFaces) {
                    mCustomTypeFaces.put(fontFamily, tf);
                }
                return tf;
		    }
		} catch (IOException e) {
			Log.e(TAG, "Unable to load 'fonts' assets. Perhaps doesn't exist? " + e.getMessage());
		}

		mCustomTypeFaces.put(fontFamily, null);
		return null;
	}

	public static String getDefaultFontSize(final Context context) {
		String size = "15.0px";
		TextView tv = new TextView(context);
		if (tv != null) {
			size = String.valueOf(tv.getTextSize()) + "px";
			tv = null;
		}

		return size;
	}

	public static String getDefaultFontWeight(final Context context) {
		String style = "normal";
		TextView tv = new TextView(context);
		if (tv != null) {
			Typeface tf = tv.getTypeface();
			if (tf != null && tf.isBold()) {
				style = "bold";
			}
		}

		return style;
	}

	public static int getGravity(final String align, final boolean vertical) {
		if (align != null) {
			if ("left".equals(align)) {
				 return Gravity.LEFT;
			} else if ("center".equals(align)) {
				return vertical?Gravity.CENTER_VERTICAL:Gravity.CENTER_HORIZONTAL;
			} else if ("right".equals(align)) {
				return Gravity.RIGHT;
			} else if ("top".equals(align)) {
				return Gravity.TOP;
			} else if ("bottom".equals(align)) {
				return Gravity.BOTTOM;
			}
		}
		return Gravity.NO_GRAVITY;
	}

	public static void setAlignment(final TextView tv, final String textAlign, final String verticalAlign) 
	{
		int gravity = Gravity.NO_GRAVITY;
		
		if (textAlign != null) {
			if ("left".equals(textAlign)) {
				 gravity |= Gravity.LEFT;
			} else if ("center".equals(textAlign) || "middle".equals(textAlign)) {
				gravity |=  Gravity.CENTER_HORIZONTAL;
			} else if ("right".equals(textAlign)) {
				gravity |=  Gravity.RIGHT;
			} else {
				Log.w(TAG, "Unsupported horizontal alignment: " + textAlign);
			}
		} else {
			// Nothing has been set - let's set if something was set previously
			// You can do this with shortcut syntax - but long term maint of code is easier if it's explicit
//			Log.w(TAG,
//				"No alignment set - old horizontal align was: " + (tv.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK),
//				Log.DEBUG_MODE);
			
			if ((tv.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK) != Gravity.NO_GRAVITY) {
				// Something was set before - so let's use it
				gravity |= tv.getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;
			}
		}
		
		if (verticalAlign != null) {
			if ("top".equals(verticalAlign)) {
				gravity |= Gravity.TOP;
			} else if ("middle".equals(verticalAlign) || "center".equals(verticalAlign)) {
				gravity |= Gravity.CENTER_VERTICAL;			
			} else if ("bottom".equals(verticalAlign)) {
				gravity |= Gravity.BOTTOM;			
			} else {
				Log.w(TAG, "Unsupported vertical alignment: " + verticalAlign);
			}
		} else {
			// Nothing has been set - let's set if something was set previously
			// You can do this with shortcut syntax - but long term maint of code is easier if it's explicit
//			Log.w(TAG, "No alignment set - old vertical align was: " + (tv.getGravity() & Gravity.VERTICAL_GRAVITY_MASK),
//				Log.DEBUG_MODE);
			if ((tv.getGravity() & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.NO_GRAVITY) {
				// Something was set before - so let's use it
				gravity |= tv.getGravity() & Gravity.VERTICAL_GRAVITY_MASK;
			}			
		}
		
		tv.setGravity(gravity);
	}

	public static final int FONT_SIZE_POSITION = 0;
	public static final int FONT_FAMILY_POSITION = 1;
	public static final int FONT_WEIGHT_POSITION = 2;
	public static final int FONT_STYLE_POSITION = 3;
	
	public static String[] getFontProperties(final KrollDict fontProps)
	{
		boolean bFontSet = false;
		String[] fontProperties = new String[4];
		if (fontProps.containsKey(TiC.PROPERTY_FONT) && fontProps.get(TiC.PROPERTY_FONT) instanceof HashMap) {
			bFontSet = true;
			HashMap font = (HashMap)fontProps.get(TiC.PROPERTY_FONT);
			if (font.containsKey(TiC.PROPERTY_FONTSIZE)) {
				fontProperties[FONT_SIZE_POSITION] = TiConvert.toString(font, TiC.PROPERTY_FONTSIZE);
			}
			if (font.containsKey(TiC.PROPERTY_FONTFAMILY)) {
				fontProperties[FONT_FAMILY_POSITION] = TiConvert.toString(font, TiC.PROPERTY_FONTFAMILY);
			}
			if (font.containsKey(TiC.PROPERTY_FONTWEIGHT)) {
				fontProperties[FONT_WEIGHT_POSITION] = TiConvert.toString(font, TiC.PROPERTY_FONTWEIGHT);
			}
			if (font.containsKey(TiC.PROPERTY_FONTSTYLE)) {
				fontProperties[FONT_STYLE_POSITION] = TiConvert.toString(font, TiC.PROPERTY_FONTSTYLE);
			}
		} else {
			if (fontProps.containsKey(TiC.PROPERTY_FONT_FAMILY)) {
				bFontSet = true;
				fontProperties[FONT_FAMILY_POSITION] = TiConvert.toString(fontProps, TiC.PROPERTY_FONT_FAMILY);
			}
			if (fontProps.containsKey(TiC.PROPERTY_FONT_SIZE)) {
				bFontSet = true;
				fontProperties[FONT_SIZE_POSITION] = TiConvert.toString(fontProps, TiC.PROPERTY_FONT_SIZE);
			}
			if (fontProps.containsKey(TiC.PROPERTY_FONT_WEIGHT)) {
				bFontSet = true;
				fontProperties[FONT_WEIGHT_POSITION] = TiConvert.toString(fontProps, TiC.PROPERTY_FONT_WEIGHT);
			}
			if (fontProps.containsKey(TiC.PROPERTY_FONT_STYLE)) {
				bFontSet = true;
				fontProperties[FONT_STYLE_POSITION] = TiConvert.toString(fontProps, TiC.PROPERTY_FONT_STYLE);
			}
		}
		if (!bFontSet) {
			return null;
		}
		return fontProperties;
	}
	public static void setTextViewDIPPadding(final TextView textView, final int horizontalPadding, final int verticalPadding) {
		int rawHPadding = (int)getRawDIPSize(horizontalPadding, textView.getContext());
		int rawVPadding = (int)getRawDIPSize(verticalPadding, textView.getContext());
		textView.setPadding(rawHPadding, rawVPadding, rawHPadding, rawVPadding);
	}

	public static final int[] BACKGROUND_DEFAULT_STATE_1 = {
		android.R.attr.state_window_focused,
		android.R.attr.state_enabled
	};
	public static final int[] BACKGROUND_DEFAULT_STATE_2 = {
		android.R.attr.state_enabled
	};
	public static final int[] BACKGROUND_SELECTED_STATE = {
		android.R.attr.state_window_focused,
		android.R.attr.state_enabled,
		android.R.attr.state_pressed
	};

	public static final int[] BACKGROUND_CHECKED_STATE = {
		android.R.attr.state_window_focused,
		android.R.attr.state_enabled,
		android.R.attr.state_checked
	};
	public static final int[] BACKGROUND_FOCUSED_STATE = {
		android.R.attr.state_focused,
		android.R.attr.state_window_focused,
		android.R.attr.state_enabled
	};
	public static final int[] BACKGROUND_DISABLED_STATE = {
		-android.R.attr.state_enabled
	};

	public static ColorDrawable buildColorDrawable(final Object color) {	
	    if (color instanceof Integer) {
	        return new ColorDrawable((Integer)color);
	    } else if (color instanceof String) {
	        ColorDrawable colorDrawable = null;
	        if (color != null) {
	            colorDrawable = buildColorDrawable(TiColorHelper.parseColor((String) color));
	        }           
	        return colorDrawable;
	    }
	    return null;
	}
	
	private static String resolveImageUrl(final String path, final KrollProxy proxy) {
		return path.length() > 0 ? proxy.resolveUrl(null, path) : null;
	}

	public static Drawable buildImageDrawable(final Context context, final Object object, final boolean tileImage, final KrollProxy proxy) {
        
	    if (object instanceof TiBlob) {
	        switch (((TiBlob) object).getType()) {
            case TiBlob.TYPE_DRAWABLE:
                return buildImageDrawable(context, ((TiBlob) object).getDrawable(), tileImage, proxy);
            case TiBlob.TYPE_IMAGE:
                return buildImageDrawable(context, ((TiBlob) object).getImage(), tileImage, proxy);
            default:
                return null;
            }
        } else if (object instanceof String) {
            String url = (String) object;
            if (url != null) {
                url = resolveImageUrl(url, proxy);
            }
            Drawable imageDrawable = null;
            if (url != null) {
                Cache cache = TiApplication.getImageMemoryCache();
                Bitmap bitmap = cache.get(url);
                if (bitmap == null) {
                    imageDrawable = TiFileHelper.loadDrawable(url);
                    if (imageDrawable instanceof BitmapDrawable) {
                        bitmap = ((BitmapDrawable)imageDrawable).getBitmap();
                        cache.set(url, ((BitmapDrawable)imageDrawable).getBitmap());
                    }
                } else {
                    imageDrawable = new BitmapDrawable(proxy.getActivity().getResources(), bitmap);
                }
                

                if (tileImage) {
                    if (imageDrawable instanceof BitmapDrawable) {
                        BitmapDrawable tiledBackground = (BitmapDrawable) imageDrawable;
                        tiledBackground.setTileModeX(Shader.TileMode.REPEAT);
                        tiledBackground.setTileModeY(Shader.TileMode.REPEAT);
                    }
                }
            }
            return imageDrawable;
        } else if (object instanceof Drawable) {
            Drawable imageDrawable = (Drawable) object;
            if (tileImage&& imageDrawable instanceof BitmapDrawable) {
                BitmapDrawable tiledBackground = (BitmapDrawable) imageDrawable;
                tiledBackground.setTileModeX(Shader.TileMode.REPEAT);
                tiledBackground.setTileModeY(Shader.TileMode.REPEAT);
            }
            return imageDrawable;
        } else if (object instanceof Bitmap) {
	        BitmapDrawable imageDrawable = new BitmapDrawable(context.getResources(), (Bitmap) object);
	        if (tileImage) {
	            if (imageDrawable instanceof BitmapDrawable) {
	                BitmapDrawable tiledBackground = (BitmapDrawable) imageDrawable;
	                tiledBackground.setTileModeX(Shader.TileMode.REPEAT);
	                tiledBackground.setTileModeY(Shader.TileMode.REPEAT);
	                imageDrawable = tiledBackground;
	            }
	        }
	        return imageDrawable;
	    }
        return null;
    }
	
	public static TiGradientDrawable buildGradientDrawable(final KrollDict gradientProperties) {
		TiGradientDrawable gradientDrawable = null;
		if (gradientProperties != null) {
			try {
				gradientDrawable = new TiGradientDrawable(gradientProperties);
			}
			catch (IllegalArgumentException e) {
				gradientDrawable = null;
			}
		}
		return gradientDrawable;
	}

	public static KrollDict createDictForImage(final int width, final int height, final byte[] data)
	{
		KrollDict d = new KrollDict();
		d.put(TiC.PROPERTY_X, 0);
		d.put(TiC.PROPERTY_Y, 0);
		d.put(TiC.PROPERTY_WIDTH, width);
		d.put(TiC.PROPERTY_HEIGHT, height);
		d.put(TiC.PROPERTY_MIMETYPE, MIME_TYPE_PNG);

		KrollDict cropRect = new KrollDict();
		cropRect.put(TiC.PROPERTY_X, 0);
		cropRect.put(TiC.PROPERTY_X, 0);
		cropRect.put(TiC.PROPERTY_WIDTH, width);
		cropRect.put(TiC.PROPERTY_HEIGHT, height);
		d.put(TiC.PROPERTY_CROP_RECT, cropRect);
		d.put(TiC.PROPERTY_MEDIA, TiBlob.blobFromObject(data, MIME_TYPE_PNG));

		return d;
	}

	public static TiBlob getImageFromDict(final KrollDict dict)
	{
		if (dict != null) {
			if (dict.containsKey(TiC.PROPERTY_MEDIA)) {
				Object media = dict.get(TiC.PROPERTY_MEDIA);
				if (media instanceof TiBlob) {
					return (TiBlob) media;
				}
			}
		}
		return null;
	}
	
	 /**
     * Draw the view into a bitmap.
     */
    public static Bitmap getViewBitmap(final View v) {
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e(TAG, "failed getViewBitmap(" + v + ")");
            return null;
        }
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createBitmap(cacheBitmap);
        } catch (Exception e) {
            bitmap = null;
        }

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);

        return bitmap;
    }
	
	public static Bitmap viewToBitmap(final KrollDict proxyDict, final View view)
	{
		Bitmap bitmap = null;

		if (view != null) {
			int width = view.getWidth();
			int height = view.getHeight();
			bitmap = getViewBitmap(view);
			if (bitmap == null) {
				if (view.getWidth() == 0 || view.getHeight() == 0) {
					// maybe move this out to a separate method once other refactor regarding "getWidth", etc is done
					if (view.getWidth() == 0 && proxyDict != null && proxyDict.containsKey(TiC.PROPERTY_WIDTH)) {
						TiDimension widthDimension = new TiDimension(proxyDict.getString(TiC.PROPERTY_WIDTH), TiDimension.TYPE_WIDTH);
						width = widthDimension.getAsPixels(view);
					}
					if (view.getHeight() == 0 && proxyDict != null && proxyDict.containsKey(TiC.PROPERTY_HEIGHT)) {
						TiDimension heightDimension = new TiDimension(proxyDict.getString(TiC.PROPERTY_HEIGHT),
							TiDimension.TYPE_HEIGHT);
						height = heightDimension.getAsPixels(view);
					}
		
					int wmode = width == 0 ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
					int hmode = height == 0 ? MeasureSpec.UNSPECIFIED : MeasureSpec.EXACTLY;
					view.measure(MeasureSpec.makeMeasureSpec(width, wmode), MeasureSpec.makeMeasureSpec(height, hmode));
		
					// Will force the view to layout itself, grab dimensions
					width = view.getMeasuredWidth();
					height = view.getMeasuredHeight();
					
					// set a default BS value if the dimension is still 0 and log a warning
					if (width == 0) {
						width = 100;
						Log.e(TAG, "Width property is 0 for view, display view before calling toImage()", Log.DEBUG_MODE);
					}
					if (height == 0) {
						height = 100;
						Log.e(TAG, "Height property is 0 for view, display view before calling toImage()", Log.DEBUG_MODE);
					}
	
				}
				
				if (view.getParent() == null) {
					Log.i(TAG, "View does not have parent, calling layout", Log.DEBUG_MODE);
					view.layout(0, 0, width, height);
				}
	//			float viewRatio = height / width;
	
	//			int bmpWidth =  (int) (width * scale);
	//			int bmpHeight = (int) (width * viewRatio);
				// opacity should support transparency by default
				Config bitmapConfig = Config.ARGB_8888;
	
				Drawable viewBackground = view.getBackground();
				if (viewBackground != null) {
					/*
					 * If the background is opaque then we should be able to safely use a space saving format that
					 * does not support the alpha channel. Basically, if a view has a background color set then the
					 * the pixel format will be opaque. If a background image supports an alpha channel, the pixel
					 * format will report transparency (even if the image doesn't actually look transparent). In
					 * short, most of the time the Config.ARGB_8888 format will be used when viewToImage is used
					 * but in the cases where the background is opaque, the lower memory approach will be used.
					 */
					if (viewBackground.getOpacity() == PixelFormat.OPAQUE) {
						bitmapConfig = Config.RGB_565;
					}
				}
				try {
	                bitmap = Bitmap.createBitmap(width, height, bitmapConfig);
	                Canvas canvas = new Canvas(bitmap);
	                view.draw(canvas);
	                canvas = null;
                } catch (Exception e) {
                    bitmap = null;
                }
				
			}
		}

		return bitmap;
	}

	public static TiBlob viewToImage(final KrollDict proxyDict, final View view)
	{
		Bitmap bitmap = viewToBitmap(proxyDict, view);
		if (bitmap != null) {
			return TiBlob.blobFromObject(bitmap);
		}
		return null;
	}

	/**
	 * Creates and returns a Bitmap from an InputStream.
	 * @param stream an InputStream to read bitmap data.
	 * @param opts BitmapFactory options
	 * @return a new bitmap instance.
	 * @module.api
	 */
	public static Bitmap createBitmap(final InputStream stream, BitmapFactory.Options opts)
	{
		Rect pad = new Rect();
		if (opts == null) {
			opts = new BitmapFactory.Options();
			opts.inPurgeable = true;
			opts.inInputShareable = true;
		}

		Bitmap b = null;
		try {
			b = BitmapFactory.decodeResourceStream(null, null, stream, pad, opts);
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "Unable to load bitmap. Not enough memory: " + e.getMessage());
		}
		return b;
	}
	/**
	 * Creates and returns a Bitmap from an InputStream.
	 * @param stream an InputStream to read bitmap data.
	 * @return a new bitmap instance.
	 * @module.api
	 */
	public static Bitmap createBitmap(final InputStream stream)
	{
		return createBitmap(stream, null);
	}
	
    private static final Pattern drawablePattern = Pattern.compile("/Resources/(.*)\\.png$", Pattern.CASE_INSENSITIVE);
    private static final Pattern imagePattern = Pattern.compile("^.*/Resources/images/(.*$)");
	
    
    private static BitmapFactory.Options bmpOptions = null;
    private static BitmapFactory.Options getBitmapOptions() {
        if (bmpOptions == null) {
            bmpOptions = new BitmapFactory.Options();
            bmpOptions.inPurgeable = true;
            bmpOptions.inInputShareable = true;
            bmpOptions.inDensity = DisplayMetrics.DENSITY_DEFAULT;
            bmpOptions.inTargetDensity = TiApplication.getAppDensityDpi();
            bmpOptions.inScaled = true;
        }
        return bmpOptions;
    }
	/**
	 * Creates and returns a density scaled Bitmap from an InputStream.
	 * @param stream an InputStream to read bitmap data.
	 * @return a new bitmap instance.
	 */
	public static Bitmap createDensityScaledBitmap(final InputStream stream)
	{
		Bitmap b = null;
		try {
			b = BitmapFactory.decodeResourceStream(null, null, stream, null, getBitmapOptions());
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "Unable to load bitmap. Not enough memory: " + e.getMessage());
		}
		return b;
	}
	
	private static String getResourceKeyForImage(final String url)
	{
		if (resourceImageKeys.containsKey(url)) {
			return resourceImageKeys.get(url);
		}
		
		Matcher matcher = imagePattern.matcher(url);
		String chopped = null;
		if (!matcher.matches()) {
		    matcher = drawablePattern.matcher(url);
		    if (matcher.find()) {
	            chopped = matcher.group(1);
		    }
		    if (chopped != null) {
		        if (chopped.endsWith(".9")) {
	                chopped = chopped.substring(0, chopped.lastIndexOf(".9"));
	            }
	            resourceImageKeys.put(url, chopped);
		    }
            return chopped;
		}
		
		chopped = matcher.group(1);
		if (chopped == null) {
			return null;
		}

		chopped = chopped.toLowerCase();
		String forHash = chopped;
		if (forHash.endsWith(".9.png")) {
			forHash = forHash.replace(".9.png", ".png");
		}
		String withoutExtension = chopped;
		
		if (chopped.matches("^.*\\..*$")) {
			if (chopped.endsWith(".9.png")) {
				withoutExtension = chopped.substring(0, chopped.lastIndexOf(".9.png"));
			} else {
				withoutExtension = chopped.substring(0, chopped.lastIndexOf('.'));
			}
		}
		
		String cleanedWithoutExtension = withoutExtension.replaceAll("[^a-z0-9_]", "_");
		StringBuilder result = new StringBuilder(100);
		result.append(cleanedWithoutExtension.substring(0, Math.min(cleanedWithoutExtension.length(), 80))) ;
		result.append("_");
		result.append(DigestUtils.md5Hex(forHash).substring(0, 10));
		String sResult = result.toString();
		resourceImageKeys.put(url, sResult);
		return sResult;
	}
	
	public static int getResourceId(final String url)
	{
		if (!url.contains("Resources/")) {
			return 0;
		}
		
		String key = getResourceKeyForImage(url);
		if (key == null) {
			return 0;
		}
		
		try {
			return TiRHelper.getResource("drawable." + key, false);
		} catch (TiRHelper.ResourceNotFoundException e) {
			return 0;
		}
	}
	
	public static int getResourceId(final Object value, final KrollProxy proxy)
    {
	    if (value instanceof Number) {
	        return ((Number)value).intValue();
        } else {
            String iconUrl = TiConvert.toString(value);
            if (iconUrl == null) {
                return 0;
            }
            String iconFullUrl = proxy.resolveUrl(null, iconUrl);
            return TiUIHelper.getResourceId(iconFullUrl);
        }
    }
	
	/**
	 * Creates and returns a bitmap from its url.
	 * @param url the bitmap url.
	 * @return a new bitmap instance
	 * @module.api
	 */
	public static Bitmap getResourceBitmap(final String url)
	{
		int id = getResourceId(url);
		if (id == 0) {
			return null;
		} else {
			return getResourceBitmap(id);
		}
	}
	
	/**
	 * Creates and returns a bitmap for the specified resource ID.
	 * @param res_id the bitmap id.
	 * @return a new bitmap instance.
	 * @module.api
	 */
	public static Bitmap getResourceBitmap(final int res_id)
	{
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPurgeable = true;
		opts.inInputShareable = true;
		
		Bitmap bitmap = null;
		try {
			bitmap = BitmapFactory.decodeResource(TiApplication.getInstance().getResources(), res_id, opts);
		} catch (OutOfMemoryError e) {
			Log.e(TAG, "Unable to load bitmap. Not enough memory: " + e.getMessage());
		}
		return bitmap;
	}

	public static Drawable loadFastDevDrawable(final String url)
	{
		try {
			TiBaseFile tbf = TiFileFactory.createTitaniumFile(new String[] { url }, false);
			InputStream stream = tbf.getInputStream();
			Drawable d = BitmapDrawable.createFromStream(stream, url);
			stream.close();
			return d;
		} catch (IOException e) {
			Log.w(TAG, e.getMessage(), e);
		}
		return null;
	}

	public static Drawable getResourceDrawable(final String url)
	{
		int id = getResourceId(url);
		if (id == 0) {
			return null;
		}
		if (url.endsWith(".svg")) {
			return new SVGDrawable(SVGFlyweightFactory.getInstance().get(id, TiApplication.getInstance().getCurrentActivity()));
		}
		else {
			return getResourceDrawable(id);
		}
	}
	
	public static Drawable getResourceDrawable(final int res_id)
	{
		return TiApplication.getInstance().getResources().getDrawable(res_id);
	}

	public static Drawable getResourceDrawable(final Object path)
	{
		Drawable d = null;
		
		try {
	
		    if (path instanceof Number) {
                return getResourceDrawable(((Number) path).intValue());
            } else if (path instanceof String) {
				TiUrl imageUrl = new TiUrl((String) path);
				d = TiFileHelper.loadDrawable(imageUrl.resolve());
			} else {
				d = TiDrawableReference.fromObject(TiApplication.getInstance().getCurrentActivity(), path).getDrawable();
			}
		} catch (Exception e) {
			Log.w(TAG, "Could not load drawable "+e.getMessage(), Log.DEBUG_MODE);
			d = null;
		}
		return d;
	}

	public static void overridePendingTransition(final Activity activity) 
	{
		if (overridePendingTransition == null) {
			try {
				overridePendingTransition = Activity.class.getMethod("overridePendingTransition", Integer.TYPE, Integer.TYPE);
			} catch (NoSuchMethodException e) {
				Log.w(TAG, "Activity.overridePendingTransition() not found");
			}
			
		}
		
		if (overridePendingTransition != null) {
			try {
				overridePendingTransition.invoke(activity, new Object[]{0,0});
			} catch (InvocationTargetException e) {
				Log.e(TAG, "Called incorrectly: " + e.getMessage());
			} catch (IllegalAccessException e) {
				Log.e(TAG, "Illegal access: " + e.getMessage());
			}
		}
	}
	
	public static ColorFilter createColorFilterForOpacity(final float opacity) {
		// 5x4 identity color matrix + fade the alpha to achieve opacity
		float[] matrix = {
			1, 0, 0, 0, 0,
			0, 1, 0, 0, 0,
			0, 0, 1, 0, 0,
			0, 0, 0, opacity, 0
		};
		
		return new ColorMatrixColorFilter(new ColorMatrix(matrix));
	}
	
	public static void setDrawableOpacity(final Drawable drawable, final float opacity) {
		if (drawable instanceof ColorDrawable || drawable instanceof TiBackgroundDrawable) {
			drawable.setAlpha(Math.round(opacity * 255));
		} else if (drawable != null) {
			drawable.setColorFilter(createColorFilterForOpacity(opacity));
		}
	}
	
	public static void setPaintOpacity(final Paint paint, final float opacity) {
		paint.setColorFilter(createColorFilterForOpacity(opacity));
	}

	public static void requestSoftInputChange(final TiUIView uiView, final View view) 
	{
		int focusState = uiView.getFocusState();
		if (focusState > TiUIView.SOFT_KEYBOARD_DEFAULT_ON_FOCUS) {
			if (focusState == TiUIView.SOFT_KEYBOARD_SHOW_ON_FOCUS) {
				showSoftKeyboard(view, true);
			} else if (focusState == TiUIView.SOFT_KEYBOARD_HIDE_ON_FOCUS) {
				showSoftKeyboard(view, false);
			} else {
				Log.w(TAG, "Unknown onFocus state: " + focusState);
			}
		}
	}
	
	/**
	 * hides the soft keyboard.
	 * @param view the current focused view.
	 */
	public static void hideSoftKeyboard(final View view) 
	{
		showSoftKeyboard(view, false);
	}
	
	/**
	 * Shows/hides the soft keyboard.
	 * @param view the current focused view.
	 * @param show whether to show soft keyboard.
	 */
	public static void showSoftKeyboard(final View view, final boolean show) 
	{
		if (view == null) return;
		InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);

		if (imm != null) {
			boolean useForce = (Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT || Build.VERSION.SDK_INT >= 8) ? true : false;
			String model = TiPlatformHelper.getInstance().getModel(); 
			if (model != null && model.toLowerCase().startsWith("droid")) {
				useForce = true;
			}
			if (show) {
				imm.showSoftInput(view, useForce ? InputMethodManager.SHOW_FORCED : InputMethodManager.SHOW_IMPLICIT);
			} else {
				imm.hideSoftInputFromWindow(view.getWindowToken(), useForce ? 0 : InputMethodManager.HIDE_IMPLICIT_ONLY);
			}
		}
	}

	/**
	 * Run the Runnable "delayed" by using an AsyncTask to first require a new
	 * thread and only then, in onPostExecute, run the Runnable on the UI thread.
	 * @param runnable Runnable to run on UI thread.
	 */
	public static void runUiDelayed(final Runnable runnable)
	{
		(new AsyncTask<Void, Void, Void>()
		{
			@Override
			protected Void doInBackground(Void... arg0)
			{
				return null;
			}
			/**
			 * Always invoked on UI thread.
			 */
			@Override
			protected void onPostExecute(Void result)
			{
				Handler handler = new Handler(Looper.getMainLooper());
				handler.post(runnable);
			}
		}).execute();
	}

	/**
	 * If there is a block on the UI message queue, run the Runnable "delayed".
	 * @param runnable Runnable to run on UI thread.
	 */
	public static void runUiDelayedIfBlock(final Runnable runnable)
	{
		//if (TiApplication.getInstance().getMessageQueue().isBlocking()) {
		if (TiMessenger.getMainMessenger().isBlocking()) {
			runUiDelayed(runnable);
		} else {
			//Handler handler = new Handler(Looper.getMainLooper());
			//handler.post(runnable);
			TiMessenger.getMainMessenger().getHandler().post(runnable);
		}
	}

	public static void firePostLayoutEvent(final TiUIView view)
	{
		if (view != null && view.getProxy() != null) {
			view.getProxy().fireEvent(TiC.EVENT_POST_LAYOUT, null, false);
		}
	}

	public static boolean isViewInsideViewOfClass(final View view, final Class<?>[] testClass) {
		ViewParent parent = view.getParent();
		if (parent != null) {
			for (int i = 0; i < testClass.length; i++) {
				if (testClass[i].isAssignableFrom(parent.getClass()))
					return true;
			}
			if (parent instanceof View)
				return isViewInsideViewOfClass((View)parent, testClass);
		}
		return false;
	}
	
	public static void removeViewFromSuperView(final View view) {
		if (view == null) return;
		 ViewGroup parentViewGroup = (ViewGroup) view.getParent();
        if (parentViewGroup != null) {
            parentViewGroup.removeView(view);
        }
	}
	
	public static void removeViewFromSuperView(final TiViewProxy viewProxy) {
	    ParentingProxy parentProxy = viewProxy.getParent();
        //Remove parent view if possible
        if (parentProxy != null && parentProxy instanceof TiViewProxy) {
            TiUIView tiView = viewProxy.peekView();
            TiUIView parentView = ((TiViewProxy) parentProxy).peekView();
            if (parentView != null && tiView != null) {
                parentView.remove(tiView);
            }
            viewProxy.setParent(null);
        }
	}
	
    public static void safeAddView(final ViewGroup parent, final View view) {
        if (parent == null || view == null)
            return;
        removeViewFromSuperView(view);
        parent.addView(view);
    }

	public static void addView(final ViewGroup parent, final View view) {
		removeViewFromSuperView(view);
		parent.addView(view);
	}
	
	public static void addView(final ViewGroup parent, final TiViewProxy viewProxy) {
	    TiUIView tiView = viewProxy.getOrCreateView();
	    if (tiView == null) return;
        View view = tiView.getOuterView();
        if (view == null) return;
        removeViewFromSuperView(view);
        parent.addView(view, tiView.getLayoutParams());
    }
    
	
	public static void addView(final ViewGroup parent, final View view, final int index) {
		removeViewFromSuperView(view);
		parent.addView(view, index);
	}
	
	public static void addView(final ViewGroup parent, final View view, final ViewGroup.LayoutParams params) {
		removeViewFromSuperView(view);
		parent.addView(view, params);
	}
	
	public static void addView(final ViewGroup parent, final View view, final int index, ViewGroup.LayoutParams params) {
		removeViewFromSuperView(view);
		parent.addView(view, index, params);
	}
	
	public static KrollDict getViewRectDict(final View view) {
		TiDimension nativeWidth = new TiDimension(view.getWidth(), TiDimension.TYPE_WIDTH);
		TiDimension nativeHeight = new TiDimension(view.getHeight(), TiDimension.TYPE_HEIGHT);
		TiDimension nativeLeft = new TiDimension(view.getLeft(), TiDimension.TYPE_LEFT);
		TiDimension nativeTop = new TiDimension(view.getTop(), TiDimension.TYPE_TOP);

        KrollDict d = new KrollDict();
		Activity activity  = TiApplication.getAppCurrentActivity();
        if (activity != null) {
            View decorView = activity.getWindow().getDecorView();
            d.put(TiC.PROPERTY_WIDTH, nativeWidth.getAsDefault(decorView));
            d.put(TiC.PROPERTY_HEIGHT, nativeHeight.getAsDefault(decorView));
            d.put(TiC.PROPERTY_X, nativeLeft.getAsDefault(decorView));
            d.put(TiC.PROPERTY_Y, nativeTop.getAsDefault(decorView));
        }
		return d;
	}
	
	public static RectF insetRect(final RectF source, final RectF inset) {
		if (inset == null) return source;
		return new RectF(source.left + inset.left, 
				source.top + inset.top, 
				source.right - inset.right, 
				source.bottom - inset.bottom);
	}
	
	public static RectF insetRect(final RectF source, final float inset) {
		if (inset == 0.0) return source;
		return new RectF(source.left + inset, 
				source.top + inset, 
				source.right - inset, 
				source.bottom - inset);
	}
	
	public static Shadow getShadow(final KrollDict dict) {
		Shadow result = new Shadow();
		if (dict == null) return result;
		if (dict.containsKey(TiC.PROPERTY_OFFSET)) 
		{
			HashMap offset = (HashMap) dict.get(TiC.PROPERTY_OFFSET);
			result.dx = TiUIHelper.getInPixels(offset, TiC.PROPERTY_X);
			result.dy = TiUIHelper.getInPixels(offset, TiC.PROPERTY_Y);
		}
		result.radius = dict.optFloat(TiC.PROPERTY_RADIUS, 3);
		if (dict.containsKey(TiC.PROPERTY_COLOR))
		{
			result.color = TiConvert.toColor(dict, TiC.PROPERTY_COLOR, result.color);
		}
		return result;
	}
	
	public static int adjustColorAlpha(final int color, final float factor) {
	    int alpha = Math.round(Color.alpha(color) * factor);
	    int red = Color.red(color);
	    int green = Color.green(color);
	    int blue = Color.blue(color);
	    return Color.argb(alpha, red, green, blue);
	}
	
	
	private static Object getValueForKeyPath(final String key, final HashMap object) {
	    Object current = object;
	    Object result = null;
	    String[] parts = TiUtils.fastSplit(key, '.');
	    int length = parts.length;
	    int canReturnIndex = length - 1;
	    for (int i = 0; i < length; i++) {
	        final String part = parts[i];
	        if (current instanceof KrollProxy) {
	            result = ((KrollProxy)current).getProperty(part);
	            if (result == null) {
	                final String getter = "get" + Character.toUpperCase(part.charAt(0)) + part.substring(1);
	                Method method;
	                try {
	                    method = current.getClass().getMethod(getter, (Class<?>[]) null);
	                    result = method.invoke(current, (Object[]) null);
	                } catch (Exception e) {
	                    result = null;
	                }
	            }
	            
	        } else if (current instanceof HashMap) {
	            result = ((HashMap) current).get(parts[i]);
	        } else {
	            result = null;
	        }
            if (result != null){
                current = result;
            } else {
                if (i != canReturnIndex) {
                    return null;
                }
                break;
            }
        }
	    return result;
	}

    private static final char VAR_PREFIX = '_';
    public static void applyMathDict(final KrollDict mathDict, final KrollDict event, final KrollProxy source) {
        if (event == null) return;
        KrollDict expressions = new KrollDict();
	    
        HashMap<String, Object> vars = mathDict.getHashMap("variables");
	    if (vars != null) {
	        for (Map.Entry<String, Object> entry : vars.entrySet()) {
	            expressions.put(VAR_PREFIX+entry.getKey(), getValueForKeyPath(TiConvert.toString(entry.getValue()), event));
	        }
	    }
	    
	    String condition  = mathDict.getString("condition");
	    if (condition != null) {
	        try {
                Expression expression = new Expression(condition);
                  for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
                      String value  = TiConvert.toString(entry2.getValue());
                      if (value != null) {
                          expression.with(entry2.getKey(), value);
                      }
                  }
                if (expression.eval().intValue() == 0) {
                    return;
                }
              } catch (Exception e) {
              }
	    }
	    
        HashMap<String, Object> exps = mathDict.getHashMap("expressions");
	    if (exps != null) {
	        for (Map.Entry<String, Object> entry : exps.entrySet()) {
	            
//	            Scope scope = Scope.create();
//	            try {
//                    for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
//                        Variable v = scope.getVariable("$"+entry2.getKey());
//                        v.setValue(TiConvert.toFloat(entry2.getValue()));
//                    }
//                    parsii.eval.Expression expr = Parser.parse(TiConvert.toString(entry.getValue()), scope);
//                    expressions.put(entry.getKey(), expr.evaluate());
//                    
//                } catch (ParseException e1) {
//                } 
	            
	            try {
                  Expression expression = new Expression(TiConvert.toString(entry.getValue()));
	                for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
	                    String value  = TiConvert.toString(entry2.getValue());
	                    if (value != null) {
	                        expression.with(entry2.getKey(), value);
	                    }
                    }
                  expressions.put(VAR_PREFIX+entry.getKey(), expression.eval().toPlainString());
                } catch (Exception e) {
                    
                    return;
                }
            }
	    }
	    
	    Object[] targets = mathDict.getArray("targets");
	    
	    if (targets != null) {
	        for (Object obj : targets) {
	            HashMap<String, Object> targetDict = TiConvert.toHashMap(obj);
                if (targetDict != null) {
                    Object target = targetDict.get("target");
                    if (target == null) {
                        target = source;
                    } else if(target instanceof String) {
                        target = source.getProperty((String) target);
                    }
                    if (target instanceof KrollProxy) {
                        HashMap<String, Object> targetVariables = TiConvert.toHashMap(targetDict.get("targetVariables"));
                        if (targetVariables != null) {
                            for (Map.Entry<String, Object> entry : targetVariables.entrySet()) {
                                final String key = entry.getKey();
                                Object current = ((KrollProxy) target).getProperty(TiConvert.toString(entry.getValue()), true);
                                if (current != null) {
                                    expressions.put(VAR_PREFIX + key, current);
                                }
                                else {
                                    expressions.put(VAR_PREFIX + key, 0);
                                }
                            }
                        }
                        
                        
                        HashMap<String, Object> props = TiConvert.toHashMap(targetDict.get("properties"));
                        KrollDict realProps = new KrollDict();
                        for (Map.Entry<String, Object> entry : props.entrySet()) {
                            final String key = entry.getKey();
                            final String value = TiConvert.toString(entry.getValue());
                            Object current = ((KrollProxy) target).getProperty(key);
                            if (current != null) {
                                expressions.put(VAR_PREFIX+"current", current);
                            }
                            else {
                                expressions.put(VAR_PREFIX+"current", 0);
                            }
                            
//                            Scope scope = Scope.create();
//                            try {
//                                for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
//                                    Variable v = scope.getVariable("$"+entry2.getKey());
//                                    v.setValue(TiConvert.toFloat(entry2.getValue(), 0));
//                                }
//                                parsii.eval.Expression expr = Parser.parse(value, scope);
//                                realProps.put(entry.getKey(), expr.evaluate());
//                                
//                            } catch (ParseException e1) {
//                                String result = new String(value);
//                              for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
//                                  result = replace(result, "$"+entry2.getKey(), TiConvert.toString(entry2.getValue()));
//                              }
//                              realProps.put(key, result);
//                            } 
                            
                            Expression expression = new Expression(value);
                            for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
                                Object value2 = entry2.getValue();
                                if (value2 != null) {
                                    expression.with(entry2.getKey(), TiConvert.toString(value2));
                                }
                            }
                            try {
                                realProps.put(key, expression.eval().toPlainString());
                            } catch (Exception e) {
                                String result = new String(value);
                                for (Map.Entry<String, Object> entry2 : expressions.entrySet()) {
                                    result = TiUtils.fastReplace(result, entry2.getKey(), TiConvert.toString(entry2.getValue()));
                                }
                                realProps.put(key, result);
                            }
                        }
                        ((KrollProxy) target).applyPropertiesInternal(realProps, false, false);
                    }
                }
            }
	    }
	}


	/**
	 * To get the redirected Uri
	 * @param Uri
	 */
	public static Uri getRedirectUri(Uri mUri) throws MalformedURLException, IOException
	{
		if (!TiC.HONEYCOMB_OR_GREATER &&
				("http".equals(mUri.getScheme()) || "https".equals(mUri.getScheme()))) {
			// Media player doesn't handle redirects, try to follow them
			// here. (Redirects work fine without this in ICS.)
			while (true) {
				// java.net.URL doesn't handle rtsp
				if (mUri.getScheme() != null && mUri.getScheme().equals("rtsp"))
					break;

				URL url = new URL(mUri.toString());
				HttpURLConnection cn = (HttpURLConnection) url.openConnection();
				cn.setInstanceFollowRedirects(false);
				String location = cn.getHeaderField("Location");
				if (location != null) {
					String host = mUri.getHost();
					int port = mUri.getPort();
					String scheme = mUri.getScheme();
					mUri = Uri.parse(location);
					if (mUri.getScheme() == null) {
						// Absolute URL on existing host/port/scheme
						if (scheme == null) {
							scheme = "http";
						}
						String authority = port == -1 ? host : host + ":" + port;
						mUri = mUri.buildUpon().scheme(scheme).encodedAuthority(authority).build();
					}
				} else {
					break;
				}
			}
		}
		return mUri;
	}
	
	public static FragmentTransaction transactionFragment(final Fragment fragment, final View container, final FragmentActivity activity) {
	    FragmentManager manager = activity.getSupportFragmentManager();
        Fragment tabFragment = manager.findFragmentById(android.R.id.tabcontent);
        // check if is opened inside an actionbar tab, which is
        // another fragment
        if (tabFragment != null) {
            manager = tabFragment.getChildFragmentManager();
        }
        FragmentTransaction transaction = null;
        transaction = manager.beginTransaction();
        transaction.add(container.getId(), fragment);
        transaction.commit();
        return transaction;
	}
	
	public static int getColorAccent(final Context context) {
	    TypedArray values = null;
        try {
            int resourceId = TiRHelper.getResource("android.support.v7.appcompat.R$", "attr.colorAccent");
            int[] attrs = {resourceId};
            values = context.getTheme().obtainStyledAttributes(attrs);
            return values.getColor(0, 0);
        } catch (ResourceNotFoundException e) {
            return 0;
        } finally {
            if (values != null) {
                values.recycle();
            }
        }
	}
}
