package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.transition.TransitionHelper.SubTypes;
import org.appcelerator.titanium.util.TiViewHelper;

import android.graphics.Rect;
import android.view.View;
import android.view.animation.Interpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.view.ViewHelper;

public abstract class Transition {
	public Animator inAnimator;
	public Animator inInversedAnimator;
	public Animator outAnimator;
	public Animator outInversedAnimator;
	public SubTypes subType;
	protected boolean isOut;
	protected int duration;
	protected Rect rect = null;
	protected boolean isReversed = false;

	public Transition(int subtype, boolean isOut, int duration, int defaultDuration) {
		this.duration = (duration > 0) ? duration : defaultDuration;
		subType = SubTypes.values()[subtype];
		this.isOut = isOut;
	}
	
	public Transition(int subtype, boolean isOut, int duration) {
		this.duration = duration;
		subType = SubTypes.values()[subtype];
		this.isOut = isOut;
	}
	protected void prepareAnimators() {};
	public void setRect(Rect rect) {
		this.rect = rect;
	}

	public void prepare(View inTarget, View outTarget) {
		if (rect == null) {
			View parent = (View) inTarget.getParent();
			if (parent != null) {
				rect = new Rect(0, 0, parent.getWidth(), parent.getHeight());
			}
			else {
				rect = new Rect(0, 0, inTarget.getWidth(), inTarget.getHeight());
			}
		}
		prepareAnimators();
		inInversedAnimator = outAnimator.clone();
		inInversedAnimator.setInterpolator(new Interpolator() {
			public float getInterpolation(float input) {
				return 1.0f - input;
			}
		});
		
		outInversedAnimator = inAnimator.clone();
		outInversedAnimator.setInterpolator(new Interpolator() {
			public float getInterpolation(float input) {
				return 1.0f - input;
			}
		});
	};
	protected void setTargets(boolean reversed, View inTarget, View outTarget) {
		if (inAnimator == null) {
			prepare(inTarget, inTarget);
		}
		TiViewHelper.resetValues(inTarget);
		TiViewHelper.resetValues(outTarget);
		ViewHelper.setAlpha(inTarget, 1.0f);
		ViewHelper.setAlpha(outTarget, 1.0f);
		
		inAnimator.setTarget(inTarget);
		outAnimator.setTarget(outTarget);
		inInversedAnimator.setTarget(inTarget);
		outInversedAnimator.setTarget(outTarget);
	}
	
	public void setTargetsForReversed(View inTarget, View outTarget) {
		setTargets(true, inTarget, outTarget);
	}
	public void setTargets(View inTarget, View outTarget) {
		setTargets(isReversed, inTarget, outTarget);
	}
	
	public AnimatorSet getSet(AnimatorListener _listener) {
		if (isReversed) return getReversedSet(_listener);
		AnimatorSet set = new AnimatorSet();
		if (_listener != null) {
			set.addListener(_listener);
		}
		set.playTogether(inAnimator, outAnimator);
		return set;
	}
	
	public AnimatorSet getReversedSet(AnimatorListener _listener) {
		AnimatorSet set = new AnimatorSet();
		if (_listener != null) {
			set.addListener(_listener);
		}
		set.playTogether(inInversedAnimator, outInversedAnimator);
		return set;
	}
	public int getType(){
		return -1;
	}
	
	public void setDuration(int duration){
		this.duration = duration;
		inAnimator = null;
		outAnimator = null;
	}
	
	public int getDuration(){
		return duration;
	}
	
	public boolean isReversed(){
		return isReversed;
	}
	
	public void setReversed(boolean reversed){
		isReversed = reversed;
	}
	
	public void transformView(View view, float position, boolean adjustScroll) {
		
	}

	public void transformView(View view, float position) {
		transformView(view, position, false);
	}
}