/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package org.appcelerator.titanium.util;

import java.util.Arrays;
import java.util.HashMap;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageBoxBlurFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGaussianBlurFilter;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;

/**
 * Utility class for image manipulations.
 */
public class TiImageHelper
{
	private static final String TAG = "TiImageHelper";
	private static GPUImage mGPUImage;
	
	public enum FilterType {
		kFilterBoxBlur, kFilterGaussianBlur
	}

	private static GPUImage getGPUImage()
	{
		if (mGPUImage == null) {
			mGPUImage = new GPUImage(TiApplication.getInstance().getBaseContext());
		}
		return mGPUImage;
	}
	/**
	 * Add an alpha channel to the given image if it does not already have one.
	 * 
	 * @param image
	 *            the image to add an alpha channel to.
	 * @return a copy of the given image with an alpha channel. If the image already have the alpha channel, return the
	 *         image itself.
	 */
	public static Bitmap imageWithAlpha(Bitmap image)
	{
		if (image == null) {
			return null;
		}
		if (image.hasAlpha()) {
			return image;
		}
		return image.copy(Bitmap.Config.ARGB_8888, true);
	}

	/**
	 * Create a copy of the given image with rounded corners and a transparent border around its edges.
	 * 
	 * @param image
	 *            the image to add rounded corners to.
	 * @param cornerRadius
	 *            the radius of the rounded corners.
	 * @param borderSize
	 *            the size of the border to be added.
	 * @return a copy of the given image with rounded corners and a transparent border. If the cornerRadius <= 0 or
	 *         borderSize < 0, return the image itself.
	 */
	public static Bitmap imageWithRoundedCorner(Bitmap image, float cornerRadius, float borderSize)
	{
		if (image == null) {
			return null;
		}
		if (cornerRadius <= 0 || borderSize < 0) {
			Log.w(TAG, "Unable to add rounded corners. Invalid corner radius or borderSize for imageWithRoundedCorner");
			return image;
		}

		int width = image.getWidth();
		int height = image.getHeight();
		Bitmap imageRoundedCorner = Bitmap.createBitmap(width + (int) (borderSize * 2), height + (int) (borderSize * 2),
			Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(imageRoundedCorner);

		Path clipPath = new Path();
		RectF imgRect = new RectF(borderSize, borderSize, width + borderSize, height + borderSize);

		float radii[] = new float[8];
		Arrays.fill(radii, cornerRadius);
		clipPath.addRoundRect(imgRect, radii, Direction.CW);

		// This still happens sometimes when hw accelerated so, catch and warn
		try {
			canvas.clipPath(clipPath);
		} catch (Exception e) {
			Log.e(TAG, "Unable to create the image with rounded corners. clipPath failed on canvas: " + e.getMessage());
			canvas.clipRect(imgRect);
		}

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		canvas.drawBitmap(imageWithAlpha(image), borderSize, borderSize, paint);
		return imageRoundedCorner;
	}

	/**
	 * Add a transparent border to the given image around its edges.
	 * 
	 * @param image
	 *            the image to add a transparent border to.
	 * @param borderSize
	 *            the size of the border to be added.
	 * @return a copy of the given image with a transparent border. If the borderSize <= 0, return the image itself.
	 */
	public static Bitmap imageWithTransparentBorder(Bitmap image, int borderSize)
	{
		if (image == null) {
			return null;
		}
		if (borderSize <= 0) {
			Log.w(TAG, "Unable to add a transparent border. Invalid border size for imageWithTransparentBorder.");
			return image;
		}

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);

		int width = image.getWidth();
		int height = image.getHeight();
		Bitmap imageBorder = Bitmap.createBitmap(width + borderSize * 2, height + borderSize * 2, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(imageBorder);
		canvas.drawBitmap(imageWithAlpha(image), borderSize, borderSize, paint);
		return imageBorder;
	}
	
	private static Bitmap getFilteredBitmap(Bitmap bitmap, FilterType filterType, HashMap options) {
		switch (filterType) {
		case kFilterBoxBlur:
		{
			float radius = 1.0f;
			if (options != null) {
				radius = TiConvert.toFloat(options.get("radius"), radius);
			}
			radius*=2.0f;
			getGPUImage().setFilter(new GPUImageBoxBlurFilter(radius));
			break;
		}
		case kFilterGaussianBlur:
		{
			float radius = 1.0f;
			if (options != null) {
				radius = TiConvert.toFloat(options.get("radius"), radius);
			}
			radius*=4.0f;
			getGPUImage().setFilter(new GPUImageGaussianBlurFilter(radius));
			break;
		}
		default:
			return null;
		}
		return mGPUImage.getBitmapWithFilterApplied(bitmap);
	}
	
	public static Bitmap imageTinted(Bitmap bitmap, int tint, Mode mode) {
		if (tint != 0) {
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(tint, mode);
		}
		return bitmap;
	}
	
	public static Bitmap imageCropped(Bitmap bitmap, TiRect rect) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		RectF realRect = rect.getAsPixels(TiApplication.getInstance().getBaseContext(), width, height);
		try {
			bitmap = Bitmap.createBitmap(bitmap, (int)realRect.left, (int)realRect.top, (int)realRect.width(), (int)realRect.height());
		} catch (Exception e) {
			Log.e(TAG, "Unable to crop the image. Not enough memory: " + e.getMessage(), e);
			return bitmap;
		}
		return bitmap;
	}
	
	public static Bitmap imageScaled(Bitmap bitmap, float scale) {
		if (scale != 1.0f) {
			int width = bitmap.getWidth();
			int height = bitmap.getHeight();
			int dstWidth = (int) (width * scale);
			int dstHeight = (int) (height * scale);
			try {
				bitmap = Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, true);
				
			} catch (OutOfMemoryError e) {
				Log.e(TAG, "Unable to resize the image. Not enough memory: " + e.getMessage(), e);
			}
		}
		return bitmap;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Bitmap imageFiltered(Bitmap bitmap, FilterType filterType, @Kroll.argument(optional=true) HashMap options) {		
		return getFilteredBitmap(bitmap, filterType, options);
	}
}
