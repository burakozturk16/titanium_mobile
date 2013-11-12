package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.ScaleProperty;
import org.appcelerator.titanium.animation.TranslationProperty;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionSlide extends Transition {
	private static final float scale = 0.7f;
	public TransitionSlide(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 1000);
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionSlide.ordinal();
	}
	protected void prepareAnimators(View inTarget, View outTarget) {
		
		float destTrans = -rect.width();
		
		String translateProp = "x";
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
			destTrans = -rect.height();
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			destTrans = -destTrans;
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
	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		float dest = rect.width();
		if (reversed) {
			dest = -dest;
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		
		if (inTarget == null) return;
		ViewHelper.setAlpha(inTarget, 0);
		if (TransitionHelper.isVerticalSubType(subType)) {
			dest = rect.height();
			ViewHelper.setTranslationY(inTarget, dest);
		}
		else {
			ViewHelper.setTranslationX(inTarget, dest);
		}
	}
}