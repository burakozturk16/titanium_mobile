package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.util.FloatProperty;

public class PivotFloat extends FloatProperty<View> {

	public PivotFloat(String name) {
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