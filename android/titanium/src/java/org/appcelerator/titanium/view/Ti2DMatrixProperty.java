package org.appcelerator.titanium.view;

import org.appcelerator.titanium.util.AffineTransform;
import org.appcelerator.titanium.util.AffineTransform.DecomposedType;

import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;

import com.nineoldandroids.util.Property;
import com.nineoldandroids.view.ViewHelper;

public class Ti2DMatrixProperty extends Property<View, Ti2DMatrix> {

	public Ti2DMatrixProperty() {
		super(Ti2DMatrix.class, "matrix");
	}

	@Override
    final public void set(View view, Ti2DMatrix value) {
		LayoutParams params = view.getLayoutParams();
		
		if (params instanceof FreeLayout.LayoutParams) {
			((FreeLayout.LayoutParams)params).matrix = value;
			view.setLayoutParams(params);
			ViewParent viewParent = view.getParent();
			if (view.getVisibility() == View.VISIBLE && viewParent instanceof View) {
				((View) viewParent).postInvalidate();
			}
		}
		else {
			DecomposedType decompose = value.getAffineTransform(view, 0.5f, 0.5f,true).decompose();
			ViewHelper.setTranslationX(view, (float) decompose.translateX);
			ViewHelper.setTranslationY(view, (float) decompose.translateY);
			ViewHelper.setRotation(view, (float) (decompose.angle*180/Math.PI));
			ViewHelper.setScaleX(view, (float) decompose.scaleX);
			ViewHelper.setScaleY(view, (float) decompose.scaleY);
		}
    }

	@Override
	public Ti2DMatrix get(View view) {
		LayoutParams params = view.getLayoutParams();
		if (params instanceof FreeLayout.LayoutParams) {
			return  ((FreeLayout.LayoutParams)params).matrix;
		}
		else {
			DecomposedType decompose =new DecomposedType();
			decompose.translateX = ViewHelper.getTranslationX(view);
			decompose.translateY = ViewHelper.getTranslationY(view);
			decompose.angle = ViewHelper.getRotation(view)*Math.PI / 180;
			decompose.scaleX = ViewHelper.getScaleX(view);
			decompose.scaleY = ViewHelper.getScaleY(view);
			return new Ti2DMatrix(new AffineTransform(decompose));
		}
	}
	
}