package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.animation.TranslationProperty;

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
	protected void prepareAnimators(View inTarget, View outTarget) {
		float dest = rect.width();
		
		String translateProp = "x";
		if (TransitionHelper.isVerticalSubType(subType)) {
			dest = rect.height();
			translateProp = "y";
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		

		inAnimator = ObjectAnimator.ofFloat(null, new TranslationProperty(translateProp), dest, 0.0f);
		inAnimator.setDuration(duration);

		outAnimator = ObjectAnimator.ofFloat(null, new TranslationProperty(translateProp), 0, -dest);
		outAnimator.setDuration(duration);
	}

	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		if (inTarget == null) return;
		float dest = rect.width();
		float multiplier = 1.0f;
		if (reversed) {
			multiplier = -multiplier;
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			multiplier = -multiplier;
		}
		
		if (TransitionHelper.isVerticalSubType(subType)) {
			dest = rect.height();
			ViewHelper.setTranslationY(inTarget, dest*multiplier);
		}
		else {
			ViewHelper.setTranslationX(inTarget, dest*multiplier);
		}
	}
}