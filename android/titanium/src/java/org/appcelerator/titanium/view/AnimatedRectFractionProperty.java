package org.appcelerator.titanium.view;

import org.appcelerator.titanium.view.TiCompositeLayout.AnimationLayoutParams;

import android.view.View;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;

import com.nineoldandroids.util.FloatProperty;

public class AnimatedRectFractionProperty extends FloatProperty<View> {

	public AnimatedRectFractionProperty() {
		super("AnimatedRectFraction");
	}

	@Override
	public void setValue(View view, float value) {
		LayoutParams params = view.getLayoutParams();
		if (params instanceof AnimationLayoutParams) {
			((AnimationLayoutParams)params).animationFraction = value;
			view.setLayoutParams(params);
			ViewParent viewParent = view.getParent();
			if (view.getVisibility() == View.VISIBLE && viewParent instanceof View) {
				((View) viewParent).postInvalidate();
			}
		}
	}

	@Override
	public Float get(View view) {
		LayoutParams params = view.getLayoutParams();
		if (params instanceof AnimationLayoutParams)
			return ((AnimationLayoutParams)params).animationFraction;
		return 0.0f;
	}
	
}
