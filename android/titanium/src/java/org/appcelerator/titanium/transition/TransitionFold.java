package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.RotationProperty;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;
import com.nineoldandroids.view.ViewHelper;

public class TransitionFold extends Transition {
	private static final float angle = 90;
	public TransitionFold(int subtype, boolean isOut, int duration) {
		super(subtype, isOut, duration, 300);
	}
	public int getType(){
		return TransitionHelper.Types.kTransitionFold.ordinal();
	}
	protected void prepareAnimators(View inTarget, View outTarget) {
		
		float destAngle = angle;
		
		String rotateProp = "y";
		if (TransitionHelper.isVerticalSubType(subType)) {
			rotateProp = "x";
		}
		if (!TransitionHelper.isPushSubType(subType)) {
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
	public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
		super.setTargets(reversed, holder, inTarget, outTarget);
		float valuex = 0.5f, valuey = 0.5f;
		if (TransitionHelper.isVerticalSubType(subType)) {
			valuey = (reversed && TransitionHelper.isPushSubType(subType))?0:1;
		}
		else {
			valuex = (reversed && TransitionHelper.isPushSubType(subType))?0:1;
		}
		if (inTarget != null) {
			ViewHelper.setAlpha(inTarget, 0);
			TiViewHelper.setPivotFloat(inTarget, valuex, valuey);			
		}
		if (outTarget != null) TiViewHelper.setPivotFloat(outTarget, 1-valuex, 1-valuey);
	}
}