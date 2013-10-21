/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.TiConvert;

import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.SweepGradient;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;

public class TiGradientDrawable extends ShapeDrawable {
	public enum GradientType {
		LINEAR_GRADIENT, RADIAL_GRADIENT, SWEEP_GRADIENT
	}

//	private static final float DEFAULT_START_ANGLE = 0;
	private static final TiPoint DEFAULT_START_POINT = new TiPoint(0, 0);
	private static final TiPoint DEFAULT_END_POINT = new TiPoint("0", "100%");
	private static final TiDimension DEFAULT_RADIUS = new TiDimension("100%", TiDimension.TYPE_UNDEFINED);
	private static final String TAG = "TiGradientDrawable";

	private GradientType gradientType;
	private TiPoint startPoint = DEFAULT_START_POINT, endPoint = DEFAULT_END_POINT;
	private TiDimension startRadius = DEFAULT_RADIUS;
//	private double startAngle = DEFAULT_START_ANGLE;
	private int[] colors;
	private float[] offsets;

	@SuppressWarnings("rawtypes")
	public TiGradientDrawable(KrollDict properties) {
		super(new RectShape());

		// Determine which type of gradient is being used.
		// Supported types are 'linear' and 'radial'.
		String type = properties.optString("type", "linear");
		if (type.equals("linear")) {
			gradientType = GradientType.LINEAR_GRADIENT;
		} else if (type.equals("radial")) {
			gradientType = GradientType.RADIAL_GRADIENT;
			startPoint = new TiPoint("50%", "50%");
		} else if (type.equals("sweep")) {
			startPoint = new TiPoint("50%", "50%");
			gradientType = GradientType.SWEEP_GRADIENT;
		} else {
			throw new IllegalArgumentException("Invalid gradient type. Must be linear or radial.");
		}
		
		if (properties.containsKey("startPoint")) {
			startPoint = new TiPoint((HashMap) properties.get("startPoint"));
		}
		
		if (properties.containsKey("endPoint")) {
			endPoint = new TiPoint((HashMap) properties.get("endPoint"));
		}
		if (properties.containsKey("startRadius")) {
			startRadius = TiConvert.toTiDimension(properties, "startRadius", TiDimension.TYPE_WIDTH);
		}
		
//		if (properties.containsKey("startAngle")) {
//			startAngle = properties.optFloat("startRadius", DEFAULT_START_ANGLE) * Math.PI/180.0f;
//		}

		Object colors = properties.get("colors");
		if (!(colors instanceof Object[])) {
			Log.w(TAG, "Android does not support gradients without colors.");
			throw new IllegalArgumentException("Must provide an array of colors.");
		}
		loadColors((Object[])colors);

//		this.view = view;

		setShaderFactory(new GradientShaderFactory());
	}
	
//	public void setView(View view)
//	{
//		this.view = view;
//	}

	public GradientType getGradientType() {
		return gradientType;
	}

	private void loadColors(Object[] colors) {
		this.colors = new int[colors.length];
		int offsetCount = 0;
		for (int i = 0; i < colors.length; i++) {
			Object color = colors[i];
			if (color instanceof HashMap) {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				HashMap<String, Object> colorRefObject = (HashMap)color;
				this.colors[i] = TiConvert.toColor(colorRefObject, "color");

				if (offsets == null) {
					offsets = new float[colors.length];
				}

				float offset = TiConvert.toFloat(colorRefObject, "offset", -1);
				if (offset >= 0.0f && offset <= 1.0f) {
					offsets[offsetCount++] = offset;
				}

			} else {
				this.colors[i] = TiConvert.toColor(color.toString());
			}
		}

		// If the number of offsets doesn't match the number of colors,
		// just distribute the colors evenly along the gradient line.
		if (offsetCount != this.colors.length) {
			offsets = null;
		}
	}

	private class GradientShaderFactory extends ShaderFactory {
		@Override
		public Shader resize(int width, int height) {
//			Context context = getContext();
			float x0 = startPoint.getX().getAsPixels(null, width, height);
			float y0 = startPoint.getY().getAsPixels(null, width, height);
			float x1 = endPoint.getX().getAsPixels(null, width, height);
			float y1 = endPoint.getY().getAsPixels(null, width, height);

			switch (gradientType) {
			case LINEAR_GRADIENT:
				return new LinearGradient(x0, y0, x1, y1, colors, offsets, TileMode.CLAMP);
			case RADIAL_GRADIENT:
				startRadius.setValueType((width>height)?TiDimension.TYPE_HEIGHT:TiDimension.TYPE_WIDTH);
				float radius0 = startRadius.getAsPixels(null, width, height);
				if (radius0 <= 0) return null; 
				return new RadialGradient(x0, y0, radius0, colors, offsets, TileMode.CLAMP);
			case SWEEP_GRADIENT:
				return new SweepGradient(x0, y0, colors, offsets);
			default:
				throw new AssertionError("No valid gradient type set.");
			}
		}
	}

	public int[] getColors()
	{
		return colors;
	}
}
