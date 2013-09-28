package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.transition.TransitionHelper.SubTypes;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class TransitionFlip extends Transition {
	public TransitionFlip(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 300);
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionFlip.ordinal();
	}
	protected void prepareAnimators() {
		float destAngle = 180;
		if (isOut) {
			destAngle = -destAngle;
		}
		
		String rotateProp = "rotationY";
		if (TransitionHelper.isVerticalSubType(subType)) {
			rotateProp = "rotationX";
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			destAngle = -destAngle;
		}
		
		inAnimator = new AnimatorSet();
		Animator anim1 = ObjectAnimator.ofFloat(null, rotateProp,
				destAngle, 0);
		anim1.setInterpolator(new AccelerateDecelerateInterpolator());
		anim1.setDuration(duration);
		Animator anim2 = ObjectAnimator.ofFloat(null, "alpha", 0, 1);
		anim2.setDuration(0);
		anim2.setStartDelay(duration / 2);
		((AnimatorSet) inAnimator).playTogether(anim1, anim2);

		outAnimator = new AnimatorSet();
		Animator anim3 = ObjectAnimator.ofFloat(null, rotateProp, 0.0f,
				-destAngle);
		anim3.setDuration(duration);
		anim3.setInterpolator(new AccelerateDecelerateInterpolator());
		Animator anim4 = ObjectAnimator.ofFloat(null, "alpha", 1, 0);
		anim4.setDuration(0);
		anim4.setStartDelay(duration / 2);
		((AnimatorSet) outAnimator).playTogether(anim3, anim4);
	}
	
	public void setTargets(boolean reversed, View inTarget, View outTarget) {
		super.setTargets(reversed, inTarget, outTarget);
		ViewHelper.setAlpha(inTarget, 0);
	}
}