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
	
	public Ti2DMatrixEvaluator(View view, float anchorX, float anchorY, int childWidth, int childHeight) {
		this.view = view;
		this.anchorX = anchorX;
		this.anchorY = anchorY;
		this.childWidth = childWidth;
		this.childHeight = childHeight;
	}
	
	public Ti2DMatrix evaluate(float fraction, Ti2DMatrix startValue,
			Ti2DMatrix endValue) {
		Matrix m = endValue.interpolate(view, fraction, childWidth, childHeight, anchorX, anchorY);
		if (startValue != null){
			Matrix mFrom = startValue.interpolate(view, (1 - fraction), childWidth, childHeight, anchorX, anchorY);
			m.preConcat(mFrom);
		}
		return new Ti2DMatrix(m);
	}

}
