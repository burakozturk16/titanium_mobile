package org.appcelerator.titanium.transition;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.animation.TranslationRelativeProperty;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionCube;
import org.appcelerator.titanium.transition.TransitionFlip;
import org.appcelerator.titanium.transition.TransitionSwipe;
import org.appcelerator.titanium.transition.TransitionSwipeFade;
import org.appcelerator.titanium.util.TiConvert;



import android.annotation.SuppressLint;
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
		kTransitionSwipeDualFade, 
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
		case kTransitionSwipeDualFade:
			return new TransitionSwipeDualFade(subtype, isOut, duration);
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
	
	@SuppressWarnings("unchecked")
	public static Transition transitionFromObject(HashMap options, HashMap defaultOptions, Transition defaultTransition)  
	{
		Transition result = defaultTransition;
		int style = TiConvert.toInt(defaultOptions, TiC.PROPERTY_STYLE,-1);
		int substyle = TiConvert.toInt(defaultOptions, TiC.PROPERTY_SUBSTYLE,SubTypes.kRightToLeft.ordinal());
		int duration = TiConvert.toInt(defaultOptions, TiC.PROPERTY_DURATION, (defaultTransition != null)?defaultTransition.getDuration():300);
		boolean reverse = false;
		if (options != null) {
			style = TiConvert.toInt(options, TiC.PROPERTY_STYLE, style);
			substyle = TiConvert.toInt(options, TiC.PROPERTY_SUBSTYLE, substyle);
			duration = TiConvert.toInt(options, TiC.PROPERTY_DURATION, duration);
			reverse = TiConvert.toBoolean(options, TiC.PROPERTY_REVERSE, reverse);
		}

		if (style != -1  && (defaultTransition == null || substyle != defaultTransition.subType.ordinal())) {
			result = TransitionHelper.transitionForType(style, substyle, duration);
		}
		if (result != null) {
			result.setDuration(duration);
			result.setReversed(reverse);
		}
		return result;
	}
}
