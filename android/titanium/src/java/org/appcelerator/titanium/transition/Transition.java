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
	protected void prepareAnimators(View inTarget, View outTarget) {};
	public void setRect(Rect rect) {
		this.rect = rect;
	}

	public void prepare(View holder, View inTarget, View outTarget) {
		setRect(new Rect(0, 0, holder.getWidth(), holder.getHeight()));
		inAnimator = null;
		outAnimator = null;
		prepareAnimators(inTarget, outTarget);
		if (outAnimator != null) {
			inInversedAnimator = outAnimator.clone();
			inInversedAnimator.setInterpolator(new Interpolator() {
				public float getInterpolation(float input) {
					return 1.0f - input;
				}
			});
		}
		
		if (inAnimator != null) {
			outInversedAnimator = inAnimator.clone();
			outInversedAnimator.setInterpolator(new Interpolator() {
				public float getInterpolation(float input) {
					return 1.0f - input;
				}
			});
		}
	};
	protected void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		prepare(holder, inTarget, inTarget);
		if (inTarget != null) {
			TiViewHelper.resetValues(inTarget);
			ViewHelper.setAlpha(inTarget, 1.0f);
			inAnimator.setTarget(inTarget);
			inInversedAnimator.setTarget(inTarget);			
		} else inAnimator = inInversedAnimator = null;
		if (outTarget != null) {
			TiViewHelper.resetValues(outTarget);
			ViewHelper.setAlpha(outTarget, 1.0f);
			outAnimator.setTarget(outTarget);
			outInversedAnimator.setTarget(outTarget);
		} else outAnimator = outInversedAnimator = null;
	}
	
	public void setTargetsForReversed(View holder, View inTarget, View outTarget) {
		setTargets(true, holder, inTarget, outTarget);
	}
	public void setTargets(View holder, View inTarget, View outTarget) {
		setTargets(isReversed, holder, inTarget, outTarget);
	}
	
	public AnimatorSet getSet(AnimatorListener _listener) {
		if (isReversed) return getReversedSet(_listener);
		AnimatorSet set = new AnimatorSet();
		if (_listener != null) {
			set.addListener(_listener);
		}
		if (inAnimator !=null && outAnimator !=null)
				set.playTogether(inAnimator, outAnimator);
		else if (inAnimator !=null)
			set.playTogether(inAnimator);
		else if (outAnimator !=null)
			set.playTogether(outAnimator);
		return set;
	}
	
	public AnimatorSet getReversedSet(AnimatorListener _listener) {
		AnimatorSet set = new AnimatorSet();
		if (_listener != null) {
			set.addListener(_listener);
		}
		if (inInversedAnimator !=null && outInversedAnimator !=null)
			set.playTogether(inInversedAnimator, outInversedAnimator);
		else if (inInversedAnimator !=null)
			set.playTogether(inInversedAnimator);
		else if (outInversedAnimator !=null)
			set.playTogether(outInversedAnimator);
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
	

	public void transformView(View view, float position) {
	    boolean out = (position < 0);
      if (!TransitionHelper.isPushSubType(subType)) {
          out = !out;
      }
        float multiplier = 1.0f;
        if (!TransitionHelper.isPushSubType(subType)) {
            multiplier = -multiplier;
        }
        
        if (TransitionHelper.isVerticalSubType(subType)) {
            TiViewHelper.setTranslationRelativeY(view, position*multiplier);
        }
        else {
            TiViewHelper.setTranslationRelativeX(view, position*multiplier);
        }
	}
}