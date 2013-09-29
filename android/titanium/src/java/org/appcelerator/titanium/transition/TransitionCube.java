package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.transition.TransitionHelper.SubTypes;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionCube extends Transition {
	private static final float translation = 0.8f;
	private static final float angle = 90f;
	private static final float scale = 0.5f;

	public TransitionCube(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 400);
	}
	
	public int getType(){
		return TransitionHelper.Types.kTransitionCube.ordinal();
	}
	
	protected void prepareAnimators() {
		float destTranslation = translation;
		float destAngle = angle;
		
		String rotateProp = "rotationY";
		String translateProp = "x";
		if (!TransitionHelper.isPushSubType(subType)) {
			destTranslation = -destTranslation;
			destAngle = -destAngle;
		}
		if (TransitionHelper.isVerticalSubType(subType)) {
			translateProp = "y";
			rotateProp = "rotationX";
		}
		
		List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationFloatProperty(translateProp), destTranslation, 0.0f));
		propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", scale, 1.0f));
		propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", scale, 1.0f));
		propertiesList.add(PropertyValuesHolder.ofFloat(rotateProp, destAngle, 0.0f));
		inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		inAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		inAnimator.setDuration(duration);

		propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationFloatProperty(translateProp), 0, -destTranslation));
		propertiesList.add(PropertyValuesHolder.ofFloat("scaleX", 1, scale));
		propertiesList.add(PropertyValuesHolder.ofFloat("scaleY", 1, scale));
		propertiesList.add(PropertyValuesHolder.ofFloat(rotateProp, 0,
				-destAngle));
		outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		outAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		outAnimator.setDuration(duration);
	};

	public void setTargets(boolean reversed, View inTarget, View outTarget) {
		super.setTargets(reversed, inTarget, outTarget);
		
		float destTranslation = translation;
		float destAngle = angle;
		if (reversed) {
			destTranslation = -destTranslation;
			destAngle = -destAngle;
		}
		ViewHelper.setScaleX(inTarget, scale);
		ViewHelper.setScaleY(inTarget, scale);
		TiViewHelper.setTranslationFloatX(inTarget, destTranslation);
		ViewHelper.setRotationY(inTarget, destAngle);
		
	}
}