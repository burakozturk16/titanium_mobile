package org.appcelerator.titanium.animation;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;
import com.nineoldandroids.view.ViewHelper;

public class RotationProperty extends FloatProperty<View> {

	public RotationProperty(String name) {
		super(name);
	}

	@Override
	public void setValue(View view, float value) {
		if (getName().equals("y"))
			ViewHelper.setRotationY(view, value);
		else
			ViewHelper.setRotationX(view, value);
	}

	@Override
	public Float get(View view) {
		if (getName().equals("y"))
			return ViewHelper.getRotationY(view);
		else
			return ViewHelper.getRotationX(view);
	}
	
}