package org.appcelerator.titanium.animation;

import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;
import com.nineoldandroids.view.ViewHelper;

public class ScaleProperty extends FloatProperty<View> {

	public ScaleProperty() {
		super("scale");
	}

	@Override
	public void setValue(View view, float value) {
		TiViewHelper.setScale(view, value);
	}

	@Override
	public Float get(View view) {
		return ViewHelper.getScaleX(view);
	}
	
}