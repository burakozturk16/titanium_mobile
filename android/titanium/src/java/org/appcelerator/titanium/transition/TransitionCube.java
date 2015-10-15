package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.RotationProperty;
import org.appcelerator.titanium.animation.TranslationProperty;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionCube extends Transition {
	private static final float translation = 1.0f;
	private static final float angle = 90f;
//	private static final float scale = 0.5f;
	
	
	private int nbFaces = 4;

	public TransitionCube(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 400);
	}
	
	public int getType(){
		return TransitionHelper.Types.kTransitionCube.ordinal();
	}
	
	protected void prepareAnimators(View inTarget, View outTarget) {
		float destTranslation = rect.width();
		float destAngle = angle;
		
		String rotateProp = "y";
		String translateProp = "x";
		if (TransitionHelper.isVerticalSubType(subType)) {
			destTranslation = rect.height();
			translateProp = "y";
			rotateProp = "x";
		}
		if (!TransitionHelper.isPushSubType(subType)) {
			destTranslation = -destTranslation;
			destAngle = -destAngle;
		}
		
		
		List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp), destTranslation*translation, 0.0f));
		propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), destAngle, 0.0f));
		inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		inAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		inAnimator.setDuration(duration);

		propertiesList = new ArrayList<PropertyValuesHolder>();
		propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp), 0, -destTranslation*translation));
		propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), 0,
				-destAngle));
		outAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
				propertiesList.toArray(new PropertyValuesHolder[0]));
		outAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
		outAnimator.setDuration(duration);
	};

	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		
		float destTranslation = rect.width();
		float destAngle = angle;
		if (reversed) {
			destTranslation = -destTranslation;
			destAngle = -destAngle;
		}
		
		if (TransitionHelper.isVerticalSubType(subType)) {
			destTranslation = rect.height();
			if (outTarget != null) TiViewHelper.setPivotFloat(outTarget, 0.5f, reversed?0.f:1.0f);
			if (inTarget != null) {
				TiViewHelper.setPivotFloat(inTarget, 0.5f, reversed?1.0f:0.0f);
				ViewHelper.setTranslationY(inTarget, destTranslation*translation);
				ViewHelper.setRotationX(inTarget, destAngle);
			}
		}
		else {
			if (outTarget != null) TiViewHelper.setPivotFloat(outTarget, reversed?0.f:1.0f, 0.5f);
			if (inTarget != null) {
				TiViewHelper.setPivotFloat(inTarget, reversed?1.0f:0.0f, 0.5f);
				ViewHelper.setTranslationX(inTarget, destTranslation*translation);
				ViewHelper.setRotationY(inTarget, destAngle);
			}
		}
	}
	
	@Override
	public void transformView(View view, float position) {
		if (Math.abs(position) >= nbFaces - 1)
	    {
			ViewHelper.setAlpha(view, 0);
	        return;
	    }
		ViewHelper.setAlpha(view, 1);
		boolean out = (position < 0);
		float multiplier = 1;
		if (!TransitionHelper.isPushSubType(subType)) {
			multiplier = -1;
			out = !out;
		}
		float angle = (360 / nbFaces);
		float rot = angle * position;
		float alpha = (Math.abs(rot) <= 90.0f)?1.0f:0.0f;
		ViewHelper.setAlpha(view, alpha);
		if (TransitionHelper.isVerticalSubType(subType)) {
			TiViewHelper.setPivotFloat(view, 0.5f, out?1.0f:0.0f);
			TiViewHelper.setTranslationRelativeY(view, position * multiplier);
			ViewHelper.setRotationX(view, rot);
		}
		else {
			TiViewHelper.setPivotFloat(view, out?1.0f:0.0f, 0.5f);
			TiViewHelper.setTranslationRelativeX(view, position * multiplier);
			ViewHelper.setRotationY(view, rot);
		}
	}
}