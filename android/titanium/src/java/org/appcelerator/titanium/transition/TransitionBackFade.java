package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.CubicBezierInterpolator;
import org.appcelerator.titanium.animation.ScaleProperty;

import android.view.View;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class TransitionBackFade extends Transition {
	private static final float scale = 0.2f;
	public TransitionBackFade(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 1000);
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionBackFade.ordinal();
	}
	protected void prepareAnimators(View inTarget, View outTarget) {
		
		inAnimator = new AnimatorSet();
		Animator anim1 = ObjectAnimator.ofFloat(null, new AlphaProperty(), 0, 1.0f);
		anim1.setInterpolator(new CubicBezierInterpolator(0.8f, 0.0f, 0.2f, 1.0f));
		Animator anim2 = ObjectAnimator.ofFloat(null, new ScaleProperty(), 1.0f, scale, 1.0f);
		((AnimatorSet) inAnimator).playTogether(anim1, anim2);
		inAnimator.setDuration(duration);

		outAnimator = new AnimatorSet();
		Animator anim3 = ObjectAnimator.ofFloat(null, new AlphaProperty(), 1, 0.0f);
		anim3.setInterpolator(new CubicBezierInterpolator(0.8f, 0.0f, 0.2f, 1.0f));
		Animator anim4 = ObjectAnimator.ofFloat(null, new ScaleProperty(), 1.0f, scale, 1.0f);
		((AnimatorSet) outAnimator).playTogether(anim3, anim4);
		outAnimator.setDuration(duration);
	}
	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		if (inTarget != null) ViewHelper.setAlpha(inTarget, 0.0f);
		if (outTarget != null) outTarget.bringToFront();
	}
}