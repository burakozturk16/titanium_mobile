package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.ScaleProperty;
import org.appcelerator.titanium.animation.TranslationRelativeProperty;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionScale extends Transition {
	private static final float alpha = 0.0f;
	private static final float scale = 0.8f;
	public TransitionScale(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 300);		
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionScale.ordinal();
	}
	protected void prepareAnimators(View inTarget, View outTarget) {
		float dest = 1;		
		String translateProp = "x";
		if (!TransitionHelper.isPushSubType(subType)) {
			dest = -dest;
		}
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
		}
			inAnimator = ObjectAnimator
					.ofFloat(null, new TranslationRelativeProperty(translateProp), dest, 0.0f);
			inAnimator.setDuration(duration);

			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, alpha));
			propertiesList.add(PropertyValuesHolder.ofFloat(new ScaleProperty(), 1, scale));
			outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			outAnimator.setDuration(duration);
	}

	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		if (reversed) {
			if (inTarget != null) {
				ViewHelper.setAlpha(inTarget, alpha);
				TiViewHelper.setScale(inTarget, scale);
			}
			if (outTarget != null) outTarget.bringToFront();
		}
		else if (inTarget != null) {
			float dest = 1.0f;
			if (!TransitionHelper.isPushSubType(subType)) {
				dest = -dest;
			}
			if (TransitionHelper.isVerticalSubType(subType)) {
				TiViewHelper.setTranslationRelativeY(inTarget, dest);
			}
			else {
				TiViewHelper.setTranslationRelativeX(inTarget, dest);
			}
		}
		
		
	}
}