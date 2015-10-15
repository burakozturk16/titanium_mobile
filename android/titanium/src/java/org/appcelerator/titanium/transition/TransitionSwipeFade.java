package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.TranslationProperty;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionSwipeFade extends Transition {
	private static final float destTrans = 0.3f;
	private static final float alpha = 0.0f;

	public TransitionSwipeFade(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 200);	
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionSwipeFade.ordinal();
	}
	
	protected void prepareAnimators(View inTarget, View outTarget) {
		float outdestFactor = destTrans;
		float dest = rect.width();
		String translateProp = "x";
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
			dest = rect.height();
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		
		inAnimator = ObjectAnimator.ofFloat(null, new TranslationProperty(translateProp),dest, 0.0f);
		inAnimator.setDuration(duration);

		List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, alpha));
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp),0, -dest*outdestFactor));
		outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		outAnimator.setDuration(duration);
	}

	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		float dest = rect.width();
		float destFactor = 1.0f;
		if (reversed) {
			destFactor = -destTrans;
			if (inTarget != null) ViewHelper.setAlpha(inTarget, alpha);
			if (outTarget != null) outTarget.bringToFront();
		}
		if (inTarget == null) return;
		if (!TransitionHelper.isPushSubType(subType)) {
			destFactor = -destFactor;
		}
		
		if (TransitionHelper.isVerticalSubType(subType)) {
			dest = rect.height();
			TiViewHelper.setTranslationRelativeY(inTarget, dest*destFactor);
		}
		else {
			TiViewHelper.setTranslationRelativeX(inTarget, dest*destFactor);
		}
	}
	
	@Override
	public void transformView(View view, float position) {
	    float decale = 1 - destTrans;
		boolean out = (position < 0);
		float multiplier = 1;
		if (!TransitionHelper.isPushSubType(subType)) {
			multiplier = -multiplier;
			out = !out;
		}
		float alpha = 1;
		float translate = position;
		if (out) {
	        translate += Math.abs(position)*decale;
			alpha = 1 - Math.abs(position);
		}
		translate *= multiplier;
		ViewHelper.setAlpha(view, alpha);
		if (TransitionHelper.isVerticalSubType(subType)) {
			TiViewHelper.setTranslationRelativeY(view, translate);
		}
		else {
			TiViewHelper.setTranslationRelativeX(view, translate);
		}
	}
}