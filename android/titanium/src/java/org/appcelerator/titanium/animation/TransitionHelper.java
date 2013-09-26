package org.appcelerator.titanium.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.view.FreeLayout;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorInflater;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Pair;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;


@SuppressLint("NewApi")
public class TransitionHelper {
	public enum Types {
		kTransitionSwipe, kTransitionCube, kTransitionSwipeFade, kTransitionFlip, kTransitionNb
	}

	static HashMap<Types, Transition> animations = new HashMap<Types, Transition>();
	
	static final int TRANSITION_ID_START = 120472;
	public static final int CUBE_LEFT_OUT = TRANSITION_ID_START;
	public static final int CUBE_RIGHT_IN = TRANSITION_ID_START + 1;
	public static final int CUBE_LEFT_IN = TRANSITION_ID_START + 2;
	public static final int CUBE_RIGHT_OUT = TRANSITION_ID_START + 3;
	
//	private static Pair<Integer, Integer> pairFromString(String str1, String str2) throws ResourceNotFoundException {
//		return new Pair<Integer, Integer>(TiRHelper.getResource(str1),TiRHelper.getResource(str2));
//	}
	
	private static String prefixForTye(Types type) {
		String prefix = "slide";
		switch (type) {
		case kTransitionSwipe:
			prefix = "swipe";
			break;
		case kTransitionCube:
			prefix = "cube";
			break;
		case kTransitionSwipeFade:
			prefix = "swipefade";
			break;
		case kTransitionFlip:
			prefix = "flip";
			break;
		default:
		}
		return prefix;
	}
	
	private static Animator loadAnimation(Context c, int animatorId) {
		return AnimatorInflater.loadAnimator(c, animatorId);
	}
	
	private static Pair<Animator, Animator> inAnimationForType(Context c, Types type) throws ResourceNotFoundException {
		int  idIn;
		int idOut;
//		if (type == Types.kTransitionCube) {
//			idIn = CUBE_RIGHT_IN;
//			idOut = CUBE_LEFT_OUT;
//		}
//		else {
			String prefix = prefixForTye(type);
			idIn = TiRHelper.getResource(new StringBuilder("anim.").append(prefix).append("_right_in").toString(), false);
			idOut = TiRHelper.getResource(new StringBuilder("anim.").append(prefix).append("_left_out").toString(), false);
//		}
		return new Pair<Animator, Animator>(loadAnimation(c, idIn),loadAnimation(c, idOut));
	}
	
	private static Pair<Animator, Animator> outAnimationForType(Context c, Types type) throws ResourceNotFoundException {
		int  idIn;
		int idOut;
//		if (type == Types.kTransitionCube) {
//			idIn = CUBE_LEFT_IN;
//			idOut = CUBE_RIGHT_OUT;
//		}
//		else {
			String prefix = prefixForTye(type);
			idIn = TiRHelper.getResource(new StringBuilder("anim.").append(prefix).append("_left_in").toString(), false);
			idOut = TiRHelper.getResource(new StringBuilder("anim.").append(prefix).append("_right_out").toString(), false);
//		}
		return new Pair<Animator, Animator>(loadAnimation(c, idIn),loadAnimation(c, idOut));
	}
	
	public static class TransitionFlip extends Transition {
		public TransitionFlip(boolean isOut, int duration){
			super(isOut, duration);
			int defaultDuration = 300;
			int realDuration = (duration > 0)?duration:defaultDuration;
			
			float destAngle = 180;
			if (isOut) {
				destAngle = -destAngle;
			}
			inAnimator = new AnimatorSet();
			Animator anim1 = ObjectAnimator.ofFloat(null, "rotationY", destAngle, 0);
			anim1.setDuration(realDuration);
			Animator anim2 = ObjectAnimator.ofFloat(null, "alpha", 1);
			anim2.setDuration(0);
			anim2.setStartDelay(realDuration/2);
			((AnimatorSet)inAnimator).playTogether(anim1, anim2);
			
			outAnimator = new AnimatorSet();
			Animator anim3 = ObjectAnimator.ofFloat(null, "rotationY", 0.0f, -destAngle);
			anim3.setDuration(realDuration);
			Animator anim4 = ObjectAnimator.ofFloat(null, "alpha", 0);
			anim4.setDuration(0);
			anim4.setStartDelay(realDuration/2);
			((AnimatorSet)outAnimator).playTogether(anim3, anim4);
		}
		public void setTargets(View inTarget, View outTarget) {
			super.setTargets(inTarget, outTarget);
			ViewHelper.setAlpha(inTarget, 0);
		}	
	}
	
	public static class TransitionSwipe extends Transition {
		public TransitionSwipe(boolean isOut, int duration){
			super(isOut, duration);
			int defaultDuration = 300;
			int realDuration = (duration > 0)?duration:defaultDuration;
			
			float dest = -1;
			if (isOut) {
				dest = -dest;
			}
			
			inAnimator = ObjectAnimator.ofFloat(null, "translationFloatX", 0.0f);
			inAnimator.setDuration(realDuration);
			
			outAnimator = ObjectAnimator.ofFloat(null, "translationFloatX", dest);
			outAnimator.setDuration(realDuration);
		}
		public void setTargets(View inTarget, View outTarget) {
			super.setTargets(inTarget, outTarget);
			if (inTarget instanceof FreeLayout) {
				((FreeLayout)inTarget).setTranslationFloatX(isOut?-1:1);
			}
		}	
	}
	
	public static class TransitionSwipeFade extends Transition {
		private static final float destTrans = 0.3f;
		public TransitionSwipeFade(boolean isOut, int duration){
			super(isOut, duration);
			int defaultDuration = 200;
			int realDuration = (duration > 0)?duration:defaultDuration;
			
			float dest = -destTrans;
			if (isOut) {
				dest = 1.0f;
			}
			inAnimator = new AnimatorSet();
			Animator anim1 = ObjectAnimator.ofFloat(null, "translationFloatX", 0.0f);
			anim1.setDuration(realDuration);
			Animator anim2 = ObjectAnimator.ofFloat(null, "alpha", 1);
			anim2.setDuration(realDuration/2);
			((AnimatorSet)inAnimator).playTogether(anim1, anim2);
			
			outAnimator = new AnimatorSet();
			Animator anim3 = ObjectAnimator.ofFloat(null, "translationFloatX", dest);
			anim3.setDuration(realDuration);
			Animator anim4 = ObjectAnimator.ofFloat(null, "alpha", isOut?1.0f:0.5f);
			anim4.setDuration(realDuration/2);
			((AnimatorSet)outAnimator).playTogether(anim3, anim4);
		}
		public void setTargets(View inTarget, View outTarget) {
			super.setTargets(inTarget, outTarget);
			float dest = 1.0f;
			if (isOut) {
				dest = -destTrans;
			}
			if (inTarget instanceof FreeLayout) {
				((FreeLayout)inTarget).setTranslationFloatX(dest);
			}
			if (isOut) {
				ViewHelper.setAlpha(inTarget, 0.5f);
				outTarget.bringToFront();
			}
		}	
	}
	
	public static class TransitionCube extends Transition {
		private static final float translation = 0.7f;
		private static final float angle = 90f;
		private static final float scale = 0.5f;
		public TransitionCube(boolean isOut, int duration){
			super(isOut, duration);
			int defaultDuration = 400;
			int realDuration = (duration > 0)?duration:defaultDuration;
			
			float destTranslation = -translation;
			float destAngle = -angle;
			if (isOut) {
				destTranslation = -destTranslation;
				destAngle = -destAngle;
			}
			
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat("translationFloatX", 0.0f));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", 1.0f));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", 1.0f));
			propertiesList.add(PropertyValuesHolder.ofFloat("rotationY", 0.0f));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null, propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
			inAnimator.setDuration(realDuration);
			
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat("translationFloatX", destTranslation));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", scale));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", scale));
			propertiesList.add(PropertyValuesHolder.ofFloat("rotationY", destAngle));
			outAnimator = ObjectAnimator.ofPropertyValuesHolder(null, propertiesList.toArray(new PropertyValuesHolder[0]));
			outAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
			outAnimator.setDuration(realDuration);

		}
		public void setTargets(View inTarget, View outTarget) {
			super.setTargets(inTarget, outTarget);
			float destTranslation = translation;
			float destAngle = angle;
			if (isOut) {
				destTranslation = -destTranslation;
				destAngle = -destAngle;
			}
			if (inTarget instanceof FreeLayout) {
				((FreeLayout)inTarget).setTranslationFloatX(destTranslation);
			}
			ViewHelper.setRotationY(inTarget, destAngle);
			ViewHelper.setScaleX(inTarget, scale);
			ViewHelper.setScaleY(inTarget, scale);
		}	
	}
	
	public static abstract class Transition {
		public Animator inAnimator;
		public Animator outAnimator;
		protected boolean isOut;
		public Transition(boolean isOut, int duration){this.isOut = isOut;}
		public void setTargets(View inTarget, View outTarget) {
			inAnimator.setTarget(inTarget);
			outAnimator.setTarget(outTarget);
		}
		
		public AnimatorSet getSet(AnimatorListener _listener) {
			AnimatorSet set = new AnimatorSet();
			if (_listener != null) {
				set.addListener(_listener);
			}
			set.playTogether(inAnimator, outAnimator);
			return set;
		}
	}
	
	public static Transition transitionForType(int type, boolean isOut, int duration)
	{
		if (type < 0 || type >= Types.values().length) {
			return null;
		}
		Types realType = Types.values()[type];
		switch (realType) {
		case kTransitionSwipe:
			return new TransitionSwipe(isOut, duration);
		case kTransitionCube:
			return new TransitionCube(isOut, duration);
		case kTransitionSwipeFade:
			return new TransitionSwipeFade(isOut, duration);
		case kTransitionFlip:
			return new TransitionFlip(isOut, duration);
		default:
		}
		return null;
	}
	
}
