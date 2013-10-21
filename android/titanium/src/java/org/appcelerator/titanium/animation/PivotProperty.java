package org.appcelerator.titanium.animation;

import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;

public class PivotProperty extends FloatProperty<View> {

	public PivotProperty(String name) {
		super(name);
	}

	@Override
	public void setValue(View view, float value) {
		if (getName().equals("y"))
			TiViewHelper.setPivotFloatY(view, value);
		else
			TiViewHelper.setPivotFloatX(view, value);
	}

	@Override
	public Float get(View view) {
		if (getName().equals("y"))
			return TiViewHelper.getPivotFloatY(view);
		else
			return TiViewHelper.getPivotFloatX(view);
	}
	
}