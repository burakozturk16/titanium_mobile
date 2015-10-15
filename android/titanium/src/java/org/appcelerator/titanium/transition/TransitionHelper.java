package org.appcelerator.titanium.transition;

import java.util.HashMap;

import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionCube;
import org.appcelerator.titanium.transition.TransitionFlip;
import org.appcelerator.titanium.transition.TransitionSwipe;
import org.appcelerator.titanium.transition.TransitionSwipeFade;
import org.appcelerator.titanium.util.TiConvert;



import org.appcelerator.titanium.util.TiUIHelper;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.Animator.AnimatorListener;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

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
	
	
//	private static Interpolator stepsInterpolator(final int numSteps) {
//		return new Interpolator() {
//	        public float getInterpolation(float time) {
//	            return time/numSteps;
//	       }
//		};
//    }
	
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
	public static Transition transitionFromObject(Object options, HashMap defaultOptions, Transition defaultTransition)  
	{
	    if (options instanceof Transition) {
	        return (Transition) options;
	    }
        HashMap theOptions = null;
	    if (options instanceof HashMap) {
	        theOptions = (HashMap)options;
	    }
		Transition result = defaultTransition;
		int style = TiConvert.toInt(defaultOptions, TiC.PROPERTY_STYLE,-1);
		int substyle = TiConvert.toInt(defaultOptions, TiC.PROPERTY_SUBSTYLE,SubTypes.kRightToLeft.ordinal());
		int duration = TiConvert.toInt(defaultOptions, TiC.PROPERTY_DURATION, (defaultTransition != null)?defaultTransition.getDuration():200);
		boolean reverse = false;
		if (theOptions != null) {
			style = TiConvert.toInt(theOptions, TiC.PROPERTY_STYLE, style);
			substyle = TiConvert.toInt(theOptions, TiC.PROPERTY_SUBSTYLE, substyle);
			duration = TiConvert.toInt(theOptions, TiC.PROPERTY_DURATION, duration);
			reverse = TiConvert.toBoolean(theOptions, TiC.PROPERTY_REVERSE, reverse);
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
	
	public interface CompletionBlock 
    {
        public void transitionDidFinish(boolean success);
    }

    public static AnimatorSet transitionViews (final ViewGroup viewHolder, 
            final View viewToAdd, 
            final View viewToHide, 
            final CompletionBlock block, 
            Object args) {
        return transitionViews(viewHolder, viewToAdd, viewToHide, block, args, (viewToAdd != null)?viewToAdd.getLayoutParams():null);
    }
	
	public static AnimatorSet transitionViews (final ViewGroup viewHolder, 
	        final View viewToAdd, 
	        final View viewToHide, 
	        final CompletionBlock block, 
	        Object args,
            ViewGroup.LayoutParams layoutParams) {
        AnimatorSet set = null;
        if (viewHolder == null) return set;
        Transition transition = TransitionHelper.transitionFromObject(args, null, null);
        if (viewToAdd!=null) {
            viewToAdd.setVisibility(View.GONE);
            TiUIHelper.addView(viewHolder, viewToAdd, layoutParams); //make sure it s removed from its parent
        }
        if (transition != null) {
            transition.setTargets(viewHolder, viewToAdd, viewToHide);

            set = transition.getSet(new AnimatorListener() {
                public void onAnimationEnd(Animator arg0) { 
                    if (viewToHide!=null) {
                        viewHolder.removeView(viewToHide);
                    }
                    if (block != null) {
                        block.transitionDidFinish(true);
                    }
                }

                public void onAnimationCancel(Animator arg0) {
                    if (viewToHide!=null) {
                        viewHolder.removeView(viewToHide);
                    }
                    if (block != null) {
                        block.transitionDidFinish(false);
                    }
                }

                public void onAnimationRepeat(Animator arg0) {
                }

                public void onAnimationStart(Animator arg0) {
                }
            });
            set.start();
        }
        else {
            if (viewToHide!=null) {
                viewHolder.removeView(viewToHide);
            }
            if (block != null) {
                block.transitionDidFinish(true);
            }
        }
        if (viewToAdd!=null) viewToAdd.setVisibility(View.VISIBLE);
        return set;
	}
}
