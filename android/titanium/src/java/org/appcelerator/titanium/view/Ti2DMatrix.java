/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.util.ArrayList;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;

import android.graphics.Matrix;

import android.view.View;

@Kroll.proxy
public class Ti2DMatrix extends KrollProxy
{
	public static final float DEFAULT_ANCHOR_VALUE = -1f;
	public static final float VALUE_UNSPECIFIED = Float.MIN_VALUE;

	protected Ti2DMatrix next, prev;
	protected Matrix transformMatrix = null;

	protected static class Operation
	{
		protected static final int TYPE_SCALE = 0;
		protected static final int TYPE_TRANSLATE = 1;
		protected static final int TYPE_ROTATE = 2;
		protected static final int TYPE_MULTIPLY = 3;
		protected static final int TYPE_INVERT = 4;

		protected float scaleFromX, scaleFromY, scaleToX, scaleToY;
		protected float translateFromX, translateFromY, translateToX, translateToY;
		protected float rotateFrom, rotateTo;
		protected float anchorX = 0.5f, anchorY = 0.5f;
		protected Ti2DMatrix multiplyWith;
		protected int type;
		protected boolean scaleFromValuesSpecified = false;
		protected boolean rotationFromValueSpecified = false;

		public Operation(int type)
		{
			scaleFromX = scaleFromY = scaleToX = scaleToY = 1;
			translateToX =  translateToY = translateFromX = translateFromY = 0;
			rotateFrom = rotateTo = 0;
			this.type = type;
		}

		public void apply(View view, float interpolatedTime, Matrix matrix,
			int childWidth, int childHeight, float anchorX, float anchorY)
		{
			if (interpolatedTime == 0) return;
			anchorX = anchorX == DEFAULT_ANCHOR_VALUE ? this.anchorX : anchorX;
			anchorY = anchorY == DEFAULT_ANCHOR_VALUE ? this.anchorY : anchorY;
			switch (type) {
				case TYPE_SCALE:
					matrix.preScale((interpolatedTime * (scaleToX - scaleFromX)) + scaleFromX,
						(interpolatedTime * (scaleToY - scaleFromY)) + scaleFromY,
						anchorX * childWidth,
						anchorY * childHeight);
					break;
				case TYPE_TRANSLATE:
					float realTranslateToX = (new TiDimension(Float.toString(translateToX), TiDimension.TYPE_LEFT)).getAsPixels(view);
					float realTranslateToY = (new TiDimension(Float.toString(translateToY), TiDimension.TYPE_TOP)).getAsPixels(view);
					float realTranslateFromX = (new TiDimension(Float.toString(translateFromX), TiDimension.TYPE_LEFT)).getAsPixels(view);
					float realTranslateFromY = (new TiDimension(Float.toString(translateFromY), TiDimension.TYPE_TOP)).getAsPixels(view);
					matrix.preTranslate((interpolatedTime *  (realTranslateToX - realTranslateFromX)) + realTranslateFromX, (interpolatedTime * (realTranslateToY - realTranslateFromY)) + realTranslateFromY); break;
				case TYPE_ROTATE:
					matrix.preRotate((interpolatedTime * (rotateTo - rotateFrom)) + rotateFrom, anchorX * childWidth, anchorY * childHeight); break;
				case TYPE_MULTIPLY:
					matrix.preConcat(multiplyWith.interpolate(view, interpolatedTime, childWidth, childHeight, anchorX, anchorY)); break;
				case TYPE_INVERT:
					matrix.invert(matrix); break;
			}
		}
	}

	protected Operation op;

	public Ti2DMatrix() {}
	public Ti2DMatrix(Matrix m) {this.transformMatrix = new Matrix(m);}
	
	public Matrix getTransformMatrix() {
		return transformMatrix;
	}
	
	protected Ti2DMatrix(Ti2DMatrix prev, int opType)
	{
		if (prev != null) {
			// this.prev represents the previous matrix. This value does not change.
			this.prev = prev;
			// prev.next is not constant. Subsequent calls to Ti2DMatrix() will alter the value of prev.next.
			prev.next = this;
		}
		this.op = new Operation(opType);
	}
	
	public Ti2DMatrix(Ti2DMatrix prev)
	{
		if (prev != null) {
			// this.prev represents the previous matrix. This value does not change.
			this.prev = prev;
		}
	}

	@Override
	public void handleCreationDict(KrollDict dict)
	{
		super.handleCreationDict(dict);
		if (dict.containsKey(TiC.PROPERTY_ROTATE)) {
			op = new Operation(Operation.TYPE_ROTATE);
			op.rotateTo = TiConvert.toFloat(dict, TiC.PROPERTY_ROTATE);
			handleAnchorPoint(dict);

			// If scale also specified in creation dict,
			// then we need to link a scaling matrix separately.
			if (dict.containsKey(TiC.PROPERTY_SCALE)) {
				KrollDict newDict = new KrollDict();
				newDict.put(TiC.PROPERTY_SCALE, dict.get(TiC.PROPERTY_SCALE));
				if (dict.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
					newDict.put(TiC.PROPERTY_ANCHOR_POINT, dict.get(TiC.PROPERTY_ANCHOR_POINT));
				}
				prev = new Ti2DMatrix();
				prev.handleCreationDict(newDict);
			}

		} else if (dict.containsKey(TiC.PROPERTY_SCALE)) {
			op = new Operation(Operation.TYPE_SCALE);
			op.scaleToX = op.scaleToY = TiConvert.toFloat(dict, TiC.PROPERTY_SCALE);
			handleAnchorPoint(dict);
		}
	}

	protected void handleAnchorPoint(KrollDict dict)
	{
		if (dict.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			KrollDict anchorPoint = dict.getKrollDict(TiC.PROPERTY_ANCHOR_POINT);
			if (anchorPoint != null) {
				op.anchorX = TiConvert.toFloat(anchorPoint, TiC.PROPERTY_X);
				op.anchorY = TiConvert.toFloat(anchorPoint, TiC.PROPERTY_Y);
			}
		}
	}

	@Kroll.method
	public Ti2DMatrix translate(Object args[])
	{
		Ti2DMatrix newMatrix = new Ti2DMatrix(this, Operation.TYPE_TRANSLATE);
		newMatrix.handleAnchorPoint(this.getProperties());
		newMatrix.op.translateFromX = newMatrix.op.translateFromY = newMatrix.op.translateToX = newMatrix.op.translateToY = 0;
		if (args.length == 4) {
			// translate(fromX, fromY, toX, toY)
			newMatrix.op.translateFromX = TiConvert.toFloat(args[0]);
			newMatrix.op.translateFromY = TiConvert.toFloat(args[1]);
			newMatrix.op.translateToX = TiConvert.toFloat(args[2]);
			newMatrix.op.translateToY = TiConvert.toFloat(args[3]);
		}
		if (args.length == 2) {
			// translate(toX, toY)
			newMatrix.op.translateToX = TiConvert.toFloat(args[0]);
			newMatrix.op.translateToY = TiConvert.toFloat(args[1]);
		}
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix scale(Object args[])
	{
		Ti2DMatrix newMatrix = new Ti2DMatrix(this, Operation.TYPE_SCALE);
		newMatrix.handleAnchorPoint(this.getProperties());
		newMatrix.op.scaleToX = newMatrix.op.scaleToY = 1.0f;
		// varargs for API backwards compatibility
		if (args.length == 4) {
			// scale(fromX, fromY, toX, toY)
			newMatrix.op.scaleFromValuesSpecified = true;
			newMatrix.op.scaleFromX = TiConvert.toFloat(args[0]);
			newMatrix.op.scaleFromY = TiConvert.toFloat(args[1]);
			newMatrix.op.scaleToX = TiConvert.toFloat(args[2]);
			newMatrix.op.scaleToY = TiConvert.toFloat(args[3]);
		}
		if (args.length == 2) {
			// scale(toX, toY)
			newMatrix.op.scaleFromValuesSpecified = false;
			newMatrix.op.scaleToX = TiConvert.toFloat(args[0]);
			newMatrix.op.scaleToY = TiConvert.toFloat(args[1]);
		} else if (args.length == 1) {
			// scale(scaleFactor)
			newMatrix.op.scaleFromValuesSpecified = false;
			newMatrix.op.scaleToX = newMatrix.op.scaleToY = TiConvert.toFloat(args[0]);
		}
		// TODO newMatrix.handleAnchorPoint(newMatrix.getProperties());
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix rotate(Object[] args)
	{
		Ti2DMatrix newMatrix = new Ti2DMatrix(this, Operation.TYPE_ROTATE);
		newMatrix.handleAnchorPoint(this.getProperties());

		if (args.length == 1) {
			newMatrix.op.rotationFromValueSpecified = false;
			newMatrix.op.rotateFrom = VALUE_UNSPECIFIED;
			newMatrix.op.rotateTo = TiConvert.toFloat(args[0]);
		} else if (args.length == 2) {
			newMatrix.op.rotationFromValueSpecified = true;
			newMatrix.op.rotateFrom = TiConvert.toFloat(args[0]);
			newMatrix.op.rotateTo = TiConvert.toFloat(args[1]);
		}
		// TODO newMatrix.handleAnchorPoint(newMatrix.getProperties());
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix invert()
	{
		return new Ti2DMatrix(this, Operation.TYPE_INVERT);
	}

	@Kroll.method
	public Ti2DMatrix multiply(Ti2DMatrix other)
	{
		Ti2DMatrix newMatrix = new Ti2DMatrix(this, Operation.TYPE_MULTIPLY);
		newMatrix.handleAnchorPoint(this.getProperties());
		newMatrix.op.multiplyWith = other;
		return newMatrix;
	}
	
	@Kroll.method
	public float[] finalValuesAfterInterpolation (TiViewProxy proxy)
	{
		View view = proxy.getNativeView();
		if (view != null) {
			int width = view.getWidth();
			int height = view.getHeight();
			float[] result = new float[9];
			Matrix m = finalMatrixAfterInterpolation(proxy);
			m.getValues(result);
			return result;
		}
		return null;
	}
	
	public Matrix finalMatrixAfterInterpolation (TiViewProxy proxy)
	{
		View view = proxy.getOuterView();
		if (view != null) {
			int width = view.getWidth();
			int height = view.getHeight();
			Matrix m = interpolate(view, 1f, width, height, 0.5f, 0.5f);
			return m;
		}
		return null;
	}
	
	public Matrix finalMatrixAfterInterpolation (View view)
	{
		if (transformMatrix != null)
			return transformMatrix;
		if (view != null) {
			int width = view.getWidth();
			int height = view.getHeight();
			Matrix m = interpolate(view, 1f, width, height, 0.5f, 0.5f);
			return m;
		}
		return null;
	}


	public Matrix interpolate(View view, float interpolatedTime, int childWidth, int childHeight, float anchorX, float anchorY)
	{
		Ti2DMatrix first = this;
		ArrayList<Ti2DMatrix> preMatrixList = new ArrayList<Ti2DMatrix>();
		
		while (first.prev != null)
		{
			first = first.prev;
			// It is safe to use prev matrix to trace back the transformation matrix list,
			// since prev matrix is constant.
			preMatrixList.add(0, first);
		}

		Matrix matrix = new Matrix();
		for (Ti2DMatrix current : preMatrixList) {
			if (current.op != null) {
				current.op.apply(view, interpolatedTime, matrix, childWidth, childHeight, anchorX, anchorY);
			}
		}
		if (op != null) {
			op.apply(view, interpolatedTime, matrix, childWidth, childHeight, anchorX, anchorY);
		}
		return matrix;
	}

	/**
	 * Check if this matrix has an operation of a particular type, or if any
	 * in the chain of operations preceding it does.
	 * @param operationType Operation.TYPE_SCALE, etc.
	 * @return true if this matrix or any of the "prev" matrices is of the given type, false otherwise
	 */
	private boolean containsOperationOfType(int operationType)
	{
		Ti2DMatrix check = this;
		while (check != null) {
			if (check.op != null && check.op.type == operationType) {
				return true;
			}
			check = check.prev;
		}
		return false;
	}

	public boolean hasScaleOperation()
	{
		return containsOperationOfType(Operation.TYPE_SCALE);
	}

	public boolean hasRotateOperation()
	{
		return containsOperationOfType(Operation.TYPE_ROTATE);
	}

	public float[] getRotateOperationParameters()
	{
		if (this.op == null) {
			return new float[4];
		}

		return new float[] {
			this.op.rotateFrom,
			this.op.rotateTo,
			this.op.anchorX,
			this.op.anchorY
		};
	}

	public void setRotationFromDegrees(float degrees)
	{
		if (this.op != null) {
			this.op.rotateFrom = degrees;
		}
	}
}
