package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;

public class TranslationFloatProperty extends FloatProperty<View> {

	public TranslationFloatProperty(String name) {
		super(name);
	}

	@Override
	public void setValue(View view, float value) {
		if (getName().equals("y"))
			TiViewHelper.setTranslationFloatY(view, value);
		else
			TiViewHelper.setTranslationFloatX(view, value);
	}

	@Override
	public Float get(View view) {
		if (getName().equals("y"))
			return TiViewHelper.getTranslationFloatY(view);
		else
			return TiViewHelper.getTranslationFloatX(view);
	}
	
}