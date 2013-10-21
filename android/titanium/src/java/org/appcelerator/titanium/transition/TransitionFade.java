package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.animation.AlphaProperty;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class TransitionFade extends Transition {
	public TransitionFade(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 300);
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionFade.ordinal();
	}
	protected void prepareAnimators() {
		inAnimator = ObjectAnimator
				.ofFloat(null, new AlphaProperty(), 0.0f, 1.0f);
		inAnimator.setDuration(duration);
		
		outAnimator = ObjectAnimator
				.ofFloat(null, new AlphaProperty(), 1.0f, 0.0f);
		outAnimator.setDuration(duration);
	}
	public void setTargets(boolean reversed, View inTarget, View outTarget) {
		super.setTargets(reversed, inTarget, outTarget);
		ViewHelper.setAlpha(inTarget, 0.0f);
	}
}