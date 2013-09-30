package org.appcelerator.titanium.animation;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;
import com.nineoldandroids.view.ViewHelper;

public class AlphaProperty extends FloatProperty<View> {

	public AlphaProperty() {
		super("alpha");
	}

	@Override
	public void setValue(View view, float value) {
			ViewHelper.setAlpha(view, value);
	}

	@Override
	public Float get(View view) {
		return ViewHelper.getAlpha(view);
	}
}
