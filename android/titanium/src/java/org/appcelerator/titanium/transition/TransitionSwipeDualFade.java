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

public class TransitionSwipeDualFade extends Transition {
	private static final float destTrans = 1.0f;
	private static final float alpha = 0.0f;

	public TransitionSwipeDualFade(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 200);	
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionSwipeDualFade.ordinal();
	}
	
	protected void prepareAnimators(View inTarget, View outTarget) {
		float indest = rect.width();
		String translateProp = "x";
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
			indest = rect.height();
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			indest = -indest;
		}
		
		List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), alpha, 1));
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp),indest, 0.0f));
		inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		inAnimator.setDuration(duration);

		propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, alpha));
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp),0, -indest*destTrans));
		outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		outAnimator.setDuration(duration);
	}

	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		float dest = rect.width();
		if (reversed) {
			dest = -dest;
			outTarget.bringToFront();
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		if (inTarget == null) return;
		ViewHelper.setAlpha(inTarget, alpha);
		if (TransitionHelper.isVerticalSubType(subType)) {
			dest = rect.height();
			TiViewHelper.setTranslationRelativeY(inTarget, dest*destTrans);
		}
		else {
			TiViewHelper.setTranslationRelativeX(inTarget, dest*destTrans);
		}
	}
	
	@Override
	public void transformView(View view, float position) {
        boolean out = (position < 0);
        float multiplier = 1;
        if (!TransitionHelper.isPushSubType(subType)) {
            multiplier = -multiplier;
            out = !out;
        }
        float alpha = 1 - Math.abs(position);
        float translate = position;
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