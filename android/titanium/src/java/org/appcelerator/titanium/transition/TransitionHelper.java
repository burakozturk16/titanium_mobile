package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.CubicBezierInterpolator;
import org.appcelerator.titanium.animation.RotationProperty;
import org.appcelerator.titanium.animation.ScaleProperty;
import org.appcelerator.titanium.animation.TranslationProperty;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionCube;
import org.appcelerator.titanium.transition.TransitionFlip;
import org.appcelerator.titanium.transition.TransitionSwipe;
import org.appcelerator.titanium.transition.TransitionSwipeFade;
import org.appcelerator.titanium.util.TiViewHelper;


import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.util.Property;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.PointF;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;

@SuppressLint("NewApi")
public class TransitionHelper {
	public enum Types {
		kTransitionScale, kTransitionFade, kTransitionBackFade, kTransitionPushRotate, kTransitionFold, kTransitionSwipe, kTransitionCube, kTransitionSwipeFade, kTransitionFlip, kTransitionNb
	}
	public enum SubTypes {
		kRightToLeft, kLeftToRight, kTopToBottom, kBottomToTop
	}
	
	public static boolean isPushSubType(SubTypes subtype) {
		return (subtype == SubTypes.kRightToLeft || subtype == SubTypes.kBottomToTop);
	}
	
	public static boolean isVerticalSubType(SubTypes subtype) {
		return (subtype == SubTypes.kTopToBottom || subtype == SubTypes.kBottomToTop);
	}
	
	
	private static Interpolator stepsInterpolator(final int numSteps) {
		return new Interpolator() {
	        public float getInterpolation(float time) {
	            return time/numSteps;
	       }
		};
    }
	
	public static class TransitionBackFade extends Transition {
		private static final float scale = 0.2f;
		public TransitionBackFade(int subtype, boolean isOut, int duration) {
			super(subtype, isOut, duration, 1000);
		}
		public int getType(){
			return TransitionHelper.Types.kTransitionBackFade.ordinal();
		}
		protected void prepareAnimators() {
			
			inAnimator = new AnimatorSet();
			Animator anim1 = ObjectAnimator.ofFloat(null, new AlphaProperty(), 0, 1.0f);
			anim1.setInterpolator(new CubicBezierInterpolator(0.8f, 0.0f, 0.0f, 0.2f, duration));
			Animator anim2 = ObjectAnimator.ofFloat(null, new ScaleProperty(), 1.0f, scale, 1.0f);
			((AnimatorSet) inAnimator).playTogether(anim1, anim2);
			inAnimator.setDuration(duration);

			outAnimator = new AnimatorSet();
			Animator anim3 = ObjectAnimator.ofFloat(null, new AlphaProperty(), 1, 0.0f);
			anim3.setInterpolator(new CubicBezierInterpolator(0.8f, 0.0f, 0.0f, 0.2f, duration));
			Animator anim4 = ObjectAnimator.ofFloat(null, new ScaleProperty(), 1.0f, scale, 1.0f);
			((AnimatorSet) outAnimator).playTogether(anim3, anim4);
			outAnimator.setDuration(duration);
		}
		public void setTargets(boolean reversed, View inTarget, View outTarget) {
			super.setTargets(reversed, inTarget, outTarget);
			ViewHelper.setAlpha(inTarget, 0.0f);
		}
	}
	
	public static class TransitionFold extends Transition {
		private static final float angle = 90;
		public TransitionFold(int subtype, boolean isOut, int duration) {
			super(subtype, isOut, duration, 300);
		}
		public int getType(){
			return TransitionHelper.Types.kTransitionFold.ordinal();
		}
		protected void prepareAnimators() {
			
			float destAngle = angle;
			
			String rotateProp = "y";
			if (isVerticalSubType(subType)) {
				rotateProp = "x";
			}
			if (!isPushSubType(subType)) {
				destAngle = -destAngle;
			}
			
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), -destAngle, 0));
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(),0,  1.0f));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);
			
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), 0, destAngle));
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, 0));
			outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			outAnimator.setDuration(duration);
		}
		public void setTargets(boolean reversed, View inTarget, View outTarget) {
			super.setTargets(reversed, inTarget, outTarget);
			ViewHelper.setAlpha(inTarget, 0);
			float valuex = 0.5f, valuey = 0.5f;
			if (isVerticalSubType(subType)) {
				valuey = (reversed && isPushSubType(subType))?0:1;
			}
			else {
				valuex = (reversed && isPushSubType(subType))?0:1;
			}
			TiViewHelper.setPivotFloat(inTarget, valuex, valuey);
			TiViewHelper.setPivotFloat(outTarget, 1-valuex, 1-valuey);
		}
	}
	
	public static class TransitionPushRotate extends Transition {
		private static final float angle = 90;
		public TransitionPushRotate(int subtype, boolean isOut, int duration) {
			super(subtype, isOut, duration, 300);
		}
		public int getType(){
			return TransitionHelper.Types.kTransitionPushRotate.ordinal();
		}
		protected void prepareAnimators() {
			
			float destAngle = -angle;		
			float destTrans = 1;
			
			String translateProp = "x";
			String rotateProp = "y";
			if (!TransitionHelper.isPushSubType(subType)) {
				destTrans = -destTrans;
				destAngle = -destAngle;
			}
			if (TransitionHelper.isVerticalSubType(subType)) {
				translateProp = "y";
				rotateProp = "x";
			}
			
			inAnimator = new AnimatorSet();
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp), destTrans, 0));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);

			outAnimator = new AnimatorSet();
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, 0));
			propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), 0, angle));
			outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			outAnimator.setDuration(duration);

		}
		public void setTargets(boolean reversed, View inTarget, View outTarget) {
			super.setTargets(reversed, inTarget, outTarget);
//			ViewHelper.setAlpha(inTarget, 0);
			if (!reversed) {
				float dest = 1.0f;
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
			View target = reversed?inTarget:outTarget;
			TiViewHelper.setPivotFloatX(target, (subType == SubTypes.kLeftToRight)?1:0);
			TiViewHelper.setPivotFloatY(target, (subType == SubTypes.kTopToBottom)?1:0);
		}
	}

	public static Transition transitionForType(int type, int subtype, boolean isOut,
			int duration) {
		if (type < 0 || type >= Types.values().length) {
			return null;
		}
		Types realType = Types.values()[type];
		switch (realType) {
		case kTransitionSwipe:
			return new TransitionSwipe(subtype, isOut, duration);
		case kTransitionCube:
			return new TransitionCube(subtype, isOut, duration);
		case kTransitionSwipeFade:
			return new TransitionSwipeFade(subtype, isOut, duration);
		case kTransitionFlip:
			return new TransitionFlip(subtype, isOut, duration);
		case kTransitionBackFade:
			return new TransitionBackFade(subtype, isOut, duration);
		case kTransitionFade:
			return new TransitionFade(subtype, isOut, duration);
		case kTransitionScale:
			return new TransitionScale(subtype, isOut, duration);
		case kTransitionFold:
			return new TransitionFold(subtype, isOut, duration);
		case kTransitionPushRotate:
			return new TransitionPushRotate(subtype, isOut, duration);
		default:
		}
		return null;
	}
	
	public static Transition transitionForType(int type, int subtype,
			int duration) {
		return transitionForType(type, subtype, false, duration);
	}
	public static TransitionInAndOut transitionInAndOutForType(int type, int subtype,
			int duration) {
		if (type < 0 || type >= Types.values().length) {
			return null;
		}

		return new TransitionInAndOut(type, subtype, duration);
	}
}
