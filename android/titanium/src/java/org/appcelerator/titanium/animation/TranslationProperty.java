package org.appcelerator.titanium.animation;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;
import com.nineoldandroids.view.ViewHelper;

public class TranslationProperty extends FloatProperty<View> {

	public TranslationProperty(String name) {
		super(name);
	}

	@Override
	public void setValue(View view, float value) {
		if (getName().equals("y"))
			ViewHelper.setTranslationY(view, value);
		else
			ViewHelper.setTranslationX(view, value);
	}

	@Override
	public Float get(View view) {
		if (getName().equals("y"))
			return ViewHelper.getTranslationY(view);
		else
			return ViewHelper.getTranslationX(view);
	}
}