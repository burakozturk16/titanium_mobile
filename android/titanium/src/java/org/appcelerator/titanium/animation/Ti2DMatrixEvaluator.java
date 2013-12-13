package org.appcelerator.titanium.animation;

import org.appcelerator.titanium.util.AffineTransform;
import org.appcelerator.titanium.view.Ti2DMatrix;

import com.nineoldandroids.animation.TypeEvaluator;

import android.view.View;

public class Ti2DMatrixEvaluator implements TypeEvaluator<Ti2DMatrix> {
	private View view;
	
	public Ti2DMatrixEvaluator(View view) {
		this.view = view;
	}
	
	public Ti2DMatrix evaluate(float fraction, Ti2DMatrix startValue,
			Ti2DMatrix endValue) {
		if (fraction == 0) return startValue;
		if (fraction == 1) return endValue;
		AffineTransform a = (startValue != null)?startValue.getAffineTransform(view):(new AffineTransform());
		AffineTransform b = endValue.getAffineTransform(view);
		b.blend(a, fraction);
		return new Ti2DMatrix(b);

	}

}
