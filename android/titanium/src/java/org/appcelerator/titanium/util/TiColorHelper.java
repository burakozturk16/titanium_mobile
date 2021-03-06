/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appcelerator.kroll.common.Log;

import android.graphics.Color;
import android.os.Build;

/**
 * This class contain utility methods that converts a String color, like "red", into its corresponding RGB/RGBA representation.
 */
public class TiColorHelper
{
	static Pattern shortHexPattern = Pattern.compile("#([0-9a-f])([0-9a-f])([0-9a-f])([0-9a-f]?)");
	static Pattern rgbPattern = Pattern.compile("(rgb|rgba)\\((\\d+),\\s*(\\d+),\\s*(\\d+)(?:,\\s*(\\d+(?:\\.\\d+)?))?\\)");
	
	

	private static final String TAG = "TiColorHelper";
	private static HashMap<String, Integer> colorTable;
	private static List<String> alphaMissingColors = Arrays.asList(new String[] {"aqua", "fuchsia", "lime", "maroon", "navy", "olive", "purple", "silver", "teal"});
	

	/**
	 * Convert string representations of colors, like "red" into the corresponding RGB/RGBA representation.
	 * @param value the color value to convert. For example, "red".
	 * @return the RGB/RGBA representation (int) of the color.
	 */
	public static int parseColor(String value) {
		int color = Color.TRANSPARENT;
		if (value != null) {
			String lowval = value.trim().toLowerCase();

			Matcher m = null;
			if (lowval.startsWith("#")) {
			    if (lowval.length() == 4) {
			        StringBuilder sb = new StringBuilder();
	                sb.append("#");
	                for(int i = 1; i < lowval.length(); i++) {
	                    char s = lowval.charAt(i);
	                    sb.append(s).append(s);
	                }
	                String newColor = sb.toString();
	                color = Color.parseColor(newColor);
			    } else {
                    color = Color.parseColor(lowval);
			    }
				
			} else if (lowval.startsWith("rgb") && (m = rgbPattern.matcher(lowval)).matches()) {
			    String first = m.group(1);
			    if (first.equalsIgnoreCase("rgba")) {
			        color = Color.argb(
                            (int) (Float.valueOf(m.group(5))*255.0f),
		                    Integer.valueOf(m.group(2)),
		                    Integer.valueOf(m.group(3)),
		                    Integer.valueOf(m.group(4))
		                    );
			    } else {
                    color = Color.rgb(
                            Integer.valueOf(m.group(2)),
                            Integer.valueOf(m.group(3)),
                            Integer.valueOf(m.group(4))
                            );
                }
				
			} else {
				// Try the parser, will throw illegalArgument if it can't parse it.
				try {
					
					color = Color.parseColor(lowval);
					// In 4.3, Google introduced some new string color constants and they forgot to
					// add the alpha bits to them! This is a temporary workaround 
					// until they fix it. I've created a Google ticket for this:
					// https://code.google.com/p/android/issues/detail?id=58352&thanks=58352
					if (Build.VERSION.SDK_INT > 17 && alphaMissingColors.contains(lowval)) {
						color = Color.parseColor(lowval) | 0xFF000000;
					} else {
						color = Color.parseColor(lowval);
					}
				} catch (IllegalArgumentException e) {
					if (colorTable == null) {
						buildColorTable();
					}

					if (colorTable.containsKey(lowval)) {
						color = colorTable.get(lowval);
					} else {
						Log.w(TAG, "Unknown color: " + value);
					}
				}
			}
		}
		return color;
	}

	private static void buildColorTable() {
		synchronized(TiColorHelper.class) {
			colorTable = new HashMap<String, Integer>(20);

			colorTable.put("black", Color.BLACK);
			colorTable.put("red", Color.RED);
			colorTable.put("purple", Color.rgb(0x80, 0, 0x80));
			colorTable.put("orange", Color.rgb(0xff, 0x80, 0));
			colorTable.put("gray", Color.GRAY);
			colorTable.put("darkgray", Color.DKGRAY);
			colorTable.put("lightgray", Color.LTGRAY);
			colorTable.put("cyan", Color.CYAN);
			colorTable.put("magenta",Color.MAGENTA);
			colorTable.put("transparent", Color.TRANSPARENT);
			colorTable.put("aqua", Color.rgb(0, 0xff, 0xff));
			colorTable.put("fuchsia", Color.rgb(0xff, 0, 0xff));
			colorTable.put("lime", Color.rgb(0, 0xff, 0));
			colorTable.put("maroon", Color.rgb(0x88,0 ,0x88));
			colorTable.put("pink", Color.rgb(0xff,0xc0, 0xcb));
			colorTable.put("navy", Color.rgb(0, 0, 0x80));
			colorTable.put("silver", Color.rgb(0xc0, 0xc0, 0xc0));
			colorTable.put("olive", Color.rgb(0x80, 0x80, 0));
			colorTable.put("teal", Color.rgb(0x0, 0x80, 0x80));
			colorTable.put("brown", Color.rgb(0x99, 0x66, 0x33));
		}
	}
	
	public static String toHexString(final int color) {
	    int alpha = Color.alpha(color);
	    if (alpha != 255) {
	        return '#' + Integer.toHexString(color);
	    }
	    return '#' + Integer.toHexString(color).substring(2);
	}
}
