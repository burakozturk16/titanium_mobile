/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.util.HashMap;

import org.appcelerator.titanium.util.TiConvert;

import android.graphics.Point;
import android.graphics.PointF;

public class TiPoint {
	private TiDimension x, y;
	private String xString, yString;

	/*
	 * Create a new point with the 'x' and 'y'
	 * coordinates in pixel units.
	 */
	public TiPoint(double x, double y) {
		this.x = new TiDimension(x, TiDimension.TYPE_LEFT);
		this.xString = Double.toString(x);
		this.y = new TiDimension(y, TiDimension.TYPE_TOP);
        this.yString = Double.toString(y);
	}

	/*
	 * Create a new point from an object
	 * with 'x' and 'y' properties. If any of these
	 * properties is missing, a default value of zero will be used.
	 */
	public TiPoint(HashMap object) {
		this(object, 0, 0);
	}

	/*
	 * Create a new point from an object
	 * with 'x' and 'y' properties. If any of these
	 * properties is missing, the default values will be used.
	 */
	public TiPoint(HashMap object, double defaultValueX, double defaultValueY) {
	    xString = TiConvert.toString(object.get(TiC.PROPERTY_X));
		x = TiConvert.toTiDimension(xString, TiDimension.TYPE_LEFT);
		if (x == null) {
	        this.xString = Double.toString(defaultValueX);
			x = new TiDimension(defaultValueX, TiDimension.TYPE_LEFT);
		}

        yString = TiConvert.toString(object.get(TiC.PROPERTY_Y));
		y = TiConvert.toTiDimension(yString, TiDimension.TYPE_TOP);
		if (y == null) {
            this.xString = Double.toString(defaultValueY);
			y = new TiDimension(defaultValueY, TiDimension.TYPE_TOP);
		}
	}
	
	public TiPoint(Object xObj, Object yObj) {
        xString = TiConvert.toString(xObj);
        yString = TiConvert.toString(yObj);
		x = TiConvert.toTiDimension(xString, TiDimension.TYPE_LEFT);
		y = TiConvert.toTiDimension(yString, TiDimension.TYPE_TOP);
	}
	
	
	/*
	 * Create a new point with the 'x' and 'y'
	 * coordinates as string.
	 * @param x the x value as string.
	 * @param y the y value as string.
	 */
	public TiPoint(String x, String y)
	{
	    xString = x;
        yString = y;
		this.x = new TiDimension(xString, TiDimension.TYPE_LEFT);
		this.y = new TiDimension(yString, TiDimension.TYPE_TOP);
	}

	public TiPoint(String[] values) {
	    if (values.length > 1) {
	        xString = values[0];
	        yString = values[1];
	        this.x = new TiDimension(xString, TiDimension.TYPE_LEFT);
	        this.y = new TiDimension(yString, TiDimension.TYPE_TOP);
	    }
    }

    public TiDimension getX() {
		return x;
	}

	public TiDimension getY() {
		return y;
	}
	
	public String getXString() {
        return xString;
    }

    public String getYString() {
        return yString;
    }
	
	public Point compute(int width, int height) {
		return new Point(x.getAsPixels(width, height), y.getAsPixels(width, height));
	}
	
	public PointF computeFloat(int width, int height) {
		return new PointF((float)x.getPixels(width, height), (float)y.getPixels(width, height));
	}
}
