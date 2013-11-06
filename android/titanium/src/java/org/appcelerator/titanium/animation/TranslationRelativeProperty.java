package org.appcelerator.titanium.animation;

import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;

public class TranslationRelativeProperty extends FloatProperty<View> {

	public TranslationRelativeProperty(String name) {
		super(name);
	}

	@Override
	public void setValue(View view, float value) {
		if (getName().equals("y"))
			TiViewHelper.setTranslationRelativeY(view, value);
		else
			TiViewHelper.setTranslationRelativeX(view, value);
	}

	@Override
	public Float get(View view) {
		if (getName().equals("y"))
			return TiViewHelper.getTranslationRelativeY(view);
		else
			return TiViewHelper.getTranslationRelativeX(view);
	}
	
}