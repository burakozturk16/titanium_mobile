package org.appcelerator.titanium.util;

import org.appcelerator.titanium.view.Ti2DMatrix;

import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.graphics.Matrix;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class Ti2DMatrixEvaluator implements TypeEvaluator<Ti2DMatrix> {
	private View view;
	private float anchorX;
	private float anchorY;
	private int childWidth;
	private int childHeight;
	
	public Ti2DMatrixEvaluator(View view, float anchorX, float anchorY) {
		this.view = view;
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		this.childWidth = view.getMeasuredWidth();
		this.childHeight = view.getMeasuredHeight();
	}
	
	public Ti2DMatrix evaluate(float fraction, Ti2DMatrix startValue,
			Ti2DMatrix endValue) {
		if (startValue != null) {
			Matrix m = startValue.interpolate(view, 1, childWidth, childHeight, anchorX, anchorY);
			Matrix endM = endValue.interpolate(view, fraction, childWidth, childHeight, anchorX, anchorY);
			m.preConcat(endM);
			return new Ti2DMatrix(m);
		}
		else {
			return new Ti2DMatrix(endValue.interpolate(view, fraction, childWidth, childHeight, anchorX, anchorY));
		}
	}

}
