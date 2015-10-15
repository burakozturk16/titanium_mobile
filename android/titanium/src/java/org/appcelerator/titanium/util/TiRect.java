package org.appcelerator.titanium.util;

import java.util.HashMap;

import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;

import android.graphics.Rect;
import android.graphics.RectF;

@SuppressWarnings("rawtypes")
public class TiRect {
	private TiDimension x, y, width, height;
	private RectF internalRect;
	/*
	 * Create a new rect with the 'x' and 'y' and 'width' and 'height'
	 * coordinates in pixel units.
	 */
	public TiRect(double x, double y, double width, double height) {
		this.x = new TiDimension(x, TiDimension.TYPE_LEFT);
		this.y = new TiDimension(y, TiDimension.TYPE_TOP);
		this.width = new TiDimension(width, TiDimension.TYPE_WIDTH);
		this.height = new TiDimension(height, TiDimension.TYPE_HEIGHT);
	}

	/*
	 * Create a new rect from an object
	 * with 'x' and 'y' and 'width' and 'height' properties. If any of these
	 * properties is missing, a default value of zero will be used.
	 */
	public TiRect(Object object) {
		if (object instanceof HashMap)
			set((HashMap)object, 0, 0, 0, 0);
		else if (object instanceof RectF) {
			internalRect = (RectF) object;
		}
		else if (object instanceof Rect) {
			internalRect = new RectF((Rect)object);
		}
	}
	
	private void set(HashMap object, double defaultValueX, double defaultValueY, double defaultValueWidth, double defaultValueHeight) {
		x = TiConvert.toTiDimension(object.get(TiC.PROPERTY_X), TiDimension.TYPE_LEFT);
		y = TiConvert.toTiDimension(object.get(TiC.PROPERTY_Y), TiDimension.TYPE_TOP);
		if (x == null) {
			if (y != null)
				x = y;
			else
				x = new TiDimension(defaultValueX, TiDimension.TYPE_LEFT);
		}

		if (y == null) {
			if (x != null)
				y = x;
			else
				y = new TiDimension(defaultValueY, TiDimension.TYPE_TOP);
		}
		
		width = TiConvert.toTiDimension(object.get(TiC.PROPERTY_WIDTH), TiDimension.TYPE_WIDTH);
		height = TiConvert.toTiDimension(object.get(TiC.PROPERTY_HEIGHT), TiDimension.TYPE_HEIGHT);
		if (width == null) {
			if (height != null)
				width = height;
			else
				width = new TiDimension(defaultValueWidth, TiDimension.TYPE_WIDTH);
		}

		if (height == null) {
			if (width != null)
				height = width;
			else
				height = new TiDimension(defaultValueHeight, TiDimension.TYPE_HEIGHT);
		}
	}

	/*
	 * Create a new rect from an object
	 * with 'x' and 'y' and 'width' and 'height' properties. If any of these
	 * properties is missing, the default values will be used.
	 */
	public TiRect(HashMap object, double defaultValueX, double defaultValueY, double defaultValueWidth, double defaultValueHeight) {
		set(object, defaultValueX, defaultValueY, defaultValueWidth, defaultValueHeight);
	}
	
	/*
	 * Create a new point with the 'x' and 'y' and 'width' and 'height'
	 * coordinates as string.
	 * @param x the x value as string.
	 * @param y the y value as string.
	 * @param width the width value as string.
	 * @param height the height value as string.
	 */
	public TiRect(String x, String y, String width, String height)
	{
		this.x = new TiDimension(x, TiDimension.TYPE_LEFT);
		this.y = new TiDimension(y, TiDimension.TYPE_TOP);
		this.width = new TiDimension(width, TiDimension.TYPE_WIDTH);
		this.height = new TiDimension(height, TiDimension.TYPE_HEIGHT);
	}
	
	public TiRect(Object x, Object y, Object width, Object height)
	{
		this(TiConvert.toString(x), TiConvert.toString(y), TiConvert.toString(width), TiConvert.toString(height));
	}

	public TiDimension getX() {
		return x;
	}

	public TiDimension getY() {
		return y;
	}
	
	public TiDimension getWidth() {
		return width;
	}

	public TiDimension getHeight() {
		return height;
	}
	
	public RectF getAsPixels(int w, int h) {
		if (internalRect != null) return internalRect;
		int rectX = x.getAsPixels( w, h);
		int rectY = y.getAsPixels(w, h);
		int rectW = width.getAsPixels(w, h);
		int rectH = height.getAsPixels(w, h);
		return new RectF(rectX, rectY, rectX + rectW, rectY + rectH);
	}
}
