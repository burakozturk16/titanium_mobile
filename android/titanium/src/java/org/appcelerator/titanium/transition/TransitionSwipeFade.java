package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.TranslationProperty;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionSwipeFade extends Transition {
	private static final float destTrans = 0.3f;
	private static final float alpha = 0.5f;

	public TransitionSwipeFade(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 200);	
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionSwipeFade.ordinal();
	}
	
	protected void prepareAnimators() {
		float outdest = destTrans;
		float indest = 1;
		if (!TransitionHelper.isPushSubType(subType)) {
			indest = -indest;
			outdest = -outdest;
		}
		String translateProp = "x";
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
		}
		inAnimator = ObjectAnimator.ofFloat(null, new TranslationProperty(translateProp),indest, 0.0f);
		inAnimator.setDuration(duration);

		List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, alpha));
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp),0, -outdest));
		outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		outAnimator.setDuration(duration);
	}

	public void setTargets(boolean reversed, View inTarget, View outTarget) {
		super.setTargets(reversed, inTarget, outTarget);
		float dest = 1.0f;
		if (reversed) {
			dest = -destTrans;
			ViewHelper.setAlpha(inTarget, alpha);
			outTarget.bringToFront();
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