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
import org.appcelerator.titanium.TiPoint;
import org.appcelerator.titanium.util.AffineTransform;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.proxy.TiViewProxy;

import com.nineoldandroids.view.ViewHelper;

import android.content.Context;
import android.graphics.Matrix;

import android.view.View;

@Kroll.proxy
public class Ti2DMatrix extends KrollProxy {
	public static final TiPoint DEFAULT_ANCHOR_VALUE = new TiPoint("50%", "50%");
	public static final TiPoint DEFAULT_TRANSLATETO_VALUE = new TiPoint(0, 0);
	public static final float VALUE_UNSPECIFIED = Float.MIN_VALUE;

	public ArrayList<Operation> operations = new ArrayList<Operation>();

	public TiPoint anchor;
	protected boolean ownFrameCoord = false;

	protected AffineTransform transform = null;

	protected static class Operation {
		protected static final int TYPE_SCALE = 0;
		protected static final int TYPE_TRANSLATE = 1;
		protected static final int TYPE_ROTATE = 2;
		protected static final int TYPE_INVERT = 3;
		protected static final int TYPE_MULTIPLY = 4;

		protected float scaleToX, scaleToY;
		protected float rotateOf;
		protected TiPoint anchor;
		protected TiPoint translateTo;
		protected Ti2DMatrix multiplyWith;
		protected int type;

		public Operation(Ti2DMatrix matrix, int type) {

			if (matrix != null && matrix.anchor != null) {
				this.anchor = matrix.anchor;
			}
			scaleToX = scaleToY = 1;
			translateTo = DEFAULT_TRANSLATETO_VALUE;
			rotateOf = 0;
			this.type = type;
		}

		public void apply(Context context, int width, int height, int parentWidth, int parentHeight, AffineTransform transform) {
			float anchorX = 0;
			float anchorY = 0;
			if (type == TYPE_SCALE || type == TYPE_ROTATE) {
				TiPoint realAnchor = this.anchor;
				if (realAnchor == null)
					realAnchor = DEFAULT_ANCHOR_VALUE;
				anchorX = realAnchor.getX().getAsPixels(context, width, height);
				anchorY = realAnchor.getY().getAsPixels(context, width, height);
				
				anchorX = anchorX - width/2;
				anchorY = anchorY - height/2;
			}
			
			switch (type) {
			case TYPE_SCALE:
				transform.scale(scaleToX, scaleToY, anchorX, anchorY);
			case TYPE_TRANSLATE:
				
				float translateToX = translateTo.getX().getAsPixels(context, parentWidth, parentHeight);
				float translateToY = translateTo.getY().getAsPixels(context, parentWidth, parentHeight);
				transform.translate(translateToX, translateToY);
				break;
			case TYPE_ROTATE:
				transform.rotate(rotateOf, anchorX, anchorY);
				break;
			case TYPE_MULTIPLY:
				transform.multiply(multiplyWith.getAffineTransform(context, width, height, parentWidth, parentHeight));
				break;
			case TYPE_INVERT:
				transform.inverse();
				break;
			}
		}
	}

	// protected Operation op;

	public Ti2DMatrix() {
	}

	public Ti2DMatrix(AffineTransform transform) {
		this.transform = transform;
	}

	public Ti2DMatrix(Ti2DMatrix prev) {
		if (prev != null) {
			handleAnchorPoint(prev.getProperties());
			ownFrameCoord = prev.ownFrameCoord;
			operations.addAll(prev.operations);
		}
	}

	@Override
	public void handleCreationDict(KrollDict dict) {
		super.handleCreationDict(dict);
		handleAnchorPoint(dict);
		if (dict.containsKey(TiC.PROPERTY_ROTATE)) {

			Operation op = new Operation(this, Operation.TYPE_ROTATE);
			op.rotateOf = TiConvert.toFloat(dict, TiC.PROPERTY_ROTATE);
			operations.add(op);
			// If scale also specified in creation dict,
			// then we need to link a scaling matrix separately.
			if (dict.containsKey(TiC.PROPERTY_SCALE)) {
				KrollDict newDict = new KrollDict();
				newDict.put(TiC.PROPERTY_SCALE, dict.get(TiC.PROPERTY_SCALE));
				if (dict.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
					newDict.put(TiC.PROPERTY_ANCHOR_POINT,
							dict.get(TiC.PROPERTY_ANCHOR_POINT));
				}
				Operation scaleOp = new Operation(this, Operation.TYPE_SCALE);
				scaleOp.scaleToX = op.scaleToY = TiConvert.toFloat(dict,
						TiC.PROPERTY_SCALE);
				operations.add(0, op);
			}

		} else if (dict.containsKey(TiC.PROPERTY_SCALE)) {
			Operation op = new Operation(this, Operation.TYPE_SCALE);
			op.scaleToX = op.scaleToY = TiConvert.toFloat(dict,
					TiC.PROPERTY_SCALE);
			operations.add(op);
		}
		
		if (dict.containsKey(TiC.PROPERTY_OWN_FRAME_COORD)) {
			ownFrameCoord = dict.optBoolean(TiC.PROPERTY_OWN_FRAME_COORD, ownFrameCoord);
		}
	}

	protected void handleAnchorPoint(KrollDict dict) {
		if (dict.containsKey(TiC.PROPERTY_ANCHOR_POINT)) {
			KrollDict anchorPoint = dict
					.getKrollDict(TiC.PROPERTY_ANCHOR_POINT);
			setProperty(TiC.PROPERTY_ANCHOR_POINT, anchorPoint);
			anchor = TiConvert.toPoint(anchorPoint);
		}
	}

	@Kroll.method
	public Ti2DMatrix translate(Object args[]) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		if (args.length == 2) {
			Operation op = new Operation(newMatrix, Operation.TYPE_TRANSLATE);
			op.translateTo = new TiPoint(TiConvert.toString(args[0]),
					TiConvert.toString(args[1]));
			newMatrix.operations.add(op);
		}
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix scale(Object args[]) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		Operation op = new Operation(newMatrix, Operation.TYPE_SCALE);
		if (args.length == 2) {
			op.scaleToX = TiConvert.toFloat(args[0]);
			op.scaleToY = TiConvert.toFloat(args[1]);
		} else if (args.length == 1) {
			op.scaleToX = op.scaleToY = TiConvert.toFloat(args[0]);
		}
		newMatrix.operations.add(op);
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix rotate(Object[] args) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);

		if (args.length >= 1) {
			Operation op = new Operation(newMatrix, Operation.TYPE_ROTATE);
			op.rotateOf = TiConvert.toFloat(args[0]);
			newMatrix.operations.add(op);
		}
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix invert() {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		Operation op = new Operation(newMatrix, Operation.TYPE_INVERT);
		newMatrix.operations.add(op);
		return newMatrix;
	}

	@Kroll.method
	public Ti2DMatrix multiply(Ti2DMatrix other) {
		Ti2DMatrix newMatrix = new Ti2DMatrix(this);
		Operation op = new Operation(newMatrix, Operation.TYPE_MULTIPLY);
		op.multiplyWith = other;
		newMatrix.operations.add(op);
		return newMatrix;
	}
	
	@Kroll.method @Kroll.setProperty
	public void setAnchorPoint(KrollDict dict) {
		setProperty(TiC.PROPERTY_ANCHOR_POINT, dict);
		anchor = TiConvert.toPoint(dict);
	}

	@Kroll.method
	public float[] finalValuesAfterInterpolation(TiViewProxy proxy) {
		View view = proxy.getOuterView();
		if (view != null) {
			float[] result = new float[9];
			Matrix m = getMatrix(proxy);
			m.getValues(result);
			return result;
		}
		return null;
	}

	public Matrix getMatrix(TiViewProxy proxy) {
		return getMatrix(proxy.getOuterView());
	}
	
	public AffineTransform getAffineTransform(Context context, int width, int height, int parentWidth, int parentHeight) {
		if (transform != null) return transform;
		if (ownFrameCoord) {
			parentWidth = width;
			parentHeight = height;
		}
		AffineTransform result = new AffineTransform();
		if (width == 0 || height == 0 || parentWidth == 0 || parentHeight == 0 ) return result;
		for (Operation op : operations) {
			if (op != null) {
				op.apply(context, width, height, parentWidth, parentHeight, result);
			}
		}
		return result;
	}
	
	public AffineTransform getAffineTransform() {
		return transform;
	}
	
	public AffineTransform getAffineTransform(View view) {
		View parent = (View) view.getParent();
		if (parent == null)
			parent = view;
		ViewHelper.setPivotX(view, (float) 0.5);
		ViewHelper.setPivotY(view, (float) 0.5);
		return getAffineTransform(view.getContext(),
				view.getMeasuredWidth(), view.getMeasuredHeight(),
				parent.getMeasuredWidth(), parent.getMeasuredHeight());
	}

	public Matrix getMatrix(View view) {
		View parent = (View) view.getParent();
		if (parent == null)
			parent = view;
		return getMatrix(view.getContext(),
				view.getMeasuredWidth(), view.getMeasuredHeight(),
				parent.getMeasuredWidth(), parent.getMeasuredHeight());
	}
	
	public Matrix getMatrix(Context context, int width, int height, int parentWidth, int parentHeight) {
		AffineTransform transform = getAffineTransform(context, width, height, parentWidth, parentHeight);
		return (transform != null) ? transform.toMatrix() : null;
	}
}
