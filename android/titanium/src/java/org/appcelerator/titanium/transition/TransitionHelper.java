package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.RotationProperty;
import org.appcelerator.titanium.animation.ScaleProperty;
import org.appcelerator.titanium.animation.TranslationProperty;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionCube;
import org.appcelerator.titanium.transition.TransitionFlip;
import org.appcelerator.titanium.transition.TransitionSwipe;
import org.appcelerator.titanium.transition.TransitionSwipeFade;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiViewHelper;


import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.animation.Interpolator;

@SuppressLint("NewApi")
public class TransitionHelper {
	public enum Types {
		kTransitionScale, 
		kTransitionFade, 
		kTransitionBackFade, 
		kTransitionPushRotate, 
		kTransitionFold, 
		kTransitionSwipe, 
		kTransitionCube, 
		kTransitionSwipeFade, 
		kTransitionFlip, 
		kTransitionSlide, 
		kTransitionModernPush, 
		kTransitionGhost, 
		kTransitionZoom, 
		kTransitionSwap, 
		kTransitionCarousel, 
		kTransitionCross, 
		kTransitionGlue, 
		kTransitionNb
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
			propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), -1.2f*destAngle, 0));
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
	
	public static class TransitionSlide extends Transition {
		private static final float scale = 0.7f;
		public TransitionSlide(int subtype, boolean isOut, int duration) {
			super(subtype, isOut, duration, 1000);
		}
		public int getType(){
			return TransitionHelper.Types.kTransitionSlide.ordinal();
		}
		protected void prepareAnimators() {
			
			float destTrans = -1;
			
			String translateProp = "x";
			if (!TransitionHelper.isPushSubType(subType)) {
				destTrans = -destTrans;
			}
			if (TransitionHelper.isVerticalSubType(subType)) {
				translateProp = "y";
			}
			
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp), -destTrans, -destTrans, 0, 0));
			propertiesList.add(PropertyValuesHolder.ofFloat(new ScaleProperty(), 1, scale, scale, 1));
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(),0, 0,0.5f, 1.0f));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);
			
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp), 0,0,destTrans, destTrans));
			propertiesList.add(PropertyValuesHolder.ofFloat(new ScaleProperty(), 1, scale, scale, 1));
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1.0f, 0.5f, 0, 0));
			outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			outAnimator.setDuration(duration);
		}
		public void setTargets(boolean reversed, View inTarget, View outTarget) {
			super.setTargets(reversed, inTarget, outTarget);
			ViewHelper.setAlpha(inTarget, 0);
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
		case kTransitionCarousel:
			return new TransitionCarousel(subtype, isOut, duration);
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
		case kTransitionSlide:
			return new TransitionSlide(subtype, isOut, duration);
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
	
	public static Transition transitionFromObject(HashMap options, HashMap defaultOptions, Transition defaultTransition)  
	{
		Transition result = defaultTransition;
		int style = TiConvert.toInt(defaultOptions, TiC.PROPERTY_STYLE,-1);
		int substyle = TiConvert.toInt(defaultOptions, TiC.PROPERTY_SUBSTYLE,SubTypes.kRightToLeft.ordinal());
		int duration = TiConvert.toInt(defaultOptions, TiC.PROPERTY_DURATION, (defaultTransition != null)?defaultTransition.getDuration():300);
		
		if (options != null) {
			style = TiConvert.toInt(options, TiC.PROPERTY_STYLE, style);
			substyle = TiConvert.toInt(options, TiC.PROPERTY_SUBSTYLE, substyle);
			duration = TiConvert.toInt(options, TiC.PROPERTY_DURATION, duration);
		}

		if (style != -1  && (defaultTransition == null || substyle != defaultTransition.subType.ordinal())) {
			result = TransitionHelper.transitionForType(style, substyle, duration);
		}
		else if (result != null) {
			result.setDuration(duration);
		}
		return result;
	}
}
