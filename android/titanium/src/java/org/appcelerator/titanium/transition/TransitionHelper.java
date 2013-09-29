package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionCube;
import org.appcelerator.titanium.transition.TransitionFlip;
import org.appcelerator.titanium.transition.TransitionSwipe;
import org.appcelerator.titanium.transition.TransitionSwipeFade;
import org.appcelerator.titanium.util.TiViewHelper;


import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.util.Property;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.view.View;
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
	
	private static double bezierPoint(double point1, double point2, double point3, double point4, double time) {
	    double t1 = 1.0f - time;
	    return point1*t1*t1*t1 + 3*point2*time*t1*t1 + 3*point3*time*time*t1 + point4*time*time*time;
	}
	
	public static Interpolator cubicBezierInterpolator(final double p1, final double p2, final double p3, final double p4) {
	    return new Interpolator() {
	        public float getInterpolation(float time) {
	            return (float) bezierPoint(p1, p2, p3, p4, time);
	        }
	    };
	}
	
	public static class TransitionBackFade extends Transition {
		private static final float scale = 0.5f;
		public TransitionBackFade(int subtype, boolean isOut, int duration) {
			super(subtype, isOut, duration, 300);
		}
		public int getType(){
			return TransitionHelper.Types.kTransitionBackFade.ordinal();
		}
		protected void prepareAnimators() {
			
			inAnimator = new AnimatorSet();
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat("alpha", 0, 1.0f));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", scale, 1.0f));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", scale, 1.0f));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);
			inAnimator.setInterpolator(cubicBezierInterpolator(0.8, 0.0, 0.0, 0.2));
			
			outAnimator = new AnimatorSet();
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat("alpha", 1,0.0f));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", 1, scale));
			propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", 1, scale));
			outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			outAnimator.setDuration(duration);
			outAnimator.setInterpolator(cubicBezierInterpolator(0.8, 0.0, 0.0, 0.2));
		}
		public void setTargets(boolean reversed, View inTarget, View outTarget) {
			super.setTargets(reversed, inTarget, outTarget);
			ViewHelper.setAlpha(inTarget, 0.0f);
			ViewHelper.setScaleX(inTarget, scale);
			ViewHelper.setScaleY(inTarget, scale);
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
			
			String rotateProp = "rotationY";
			if (isVerticalSubType(subType)) {
				rotateProp = "rotationX";
			}
			if (!isPushSubType(subType)) {
				destAngle = -destAngle;
			}
			
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(rotateProp, -destAngle, 0));
			propertiesList.add(PropertyValuesHolder.ofFloat("alpha",0,  1.0f));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);
			
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(rotateProp, 0, destAngle));
			propertiesList.add(PropertyValuesHolder.ofFloat("alpha", 1, 0));
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
			String rotateProp = "rotationY";
			if (!TransitionHelper.isPushSubType(subType)) {
				destTrans = -destTrans;
				destAngle = -destAngle;
			}
			if (TransitionHelper.isVerticalSubType(subType)) {
				translateProp = "y";
				rotateProp = "rotationX";
			}
			
			inAnimator = new AnimatorSet();
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationFloatProperty(translateProp), destTrans, 0));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);

			outAnimator = new AnimatorSet();
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat("alpha", 1, 0));
			propertiesList.add(PropertyValuesHolder.ofFloat(rotateProp, 0, angle));
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
