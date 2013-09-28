package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.transition.TransitionHelper.SubTypes;
import org.appcelerator.titanium.util.TiViewHelper;
import org.appcelerator.titanium.view.FreeLayout;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class TransitionSwipe extends Transition {
	public TransitionSwipe(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 300);
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionSwipe.ordinal();
	}
	protected void prepareAnimators() {
		float dest = 1;
		
		String translateProp = "x";
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
		}

		inAnimator = ObjectAnimator.ofFloat(null, new TranslationFloat(translateProp), dest, 0.0f);
		inAnimator.setDuration(duration);

		outAnimator = ObjectAnimator.ofFloat(null, new TranslationFloat(translateProp), 0, -dest);
		outAnimator.setDuration(duration);
	}

	public void setTargets(boolean reversed, View inTarget, View outTarget) {
		super.setTargets(reversed, inTarget, outTarget);
		float dest = 1.0f;
		if (reversed) {
			dest = -dest;
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		
		if (TransitionHelper.isVerticalSubType(subType)) {
			TiViewHelper.setTranslationFloatY(inTarget, dest);
		}
		else {
			TiViewHelper.setTranslationFloatX(inTarget, dest);
		}
	}
}