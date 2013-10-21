package org.appcelerator.titanium.transition;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.RotationProperty;
import org.appcelerator.titanium.animation.TranslationProperty;
import org.appcelerator.titanium.transition.TransitionHelper.SubTypes;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;

import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.PropertyValuesHolder;

public class TransitionPushRotate extends Transition {
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
			String rotateProp = "y";
			if (!TransitionHelper.isPushSubType(subType)) {
				destTrans = -destTrans;
				destAngle = -destAngle;
			}
			if (TransitionHelper.isVerticalSubType(subType)) {
				translateProp = "y";
				rotateProp = "x";
			}
			
			inAnimator = new AnimatorSet();
			List<PropertyValuesHolder> propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new TranslationProperty(translateProp), destTrans, 0));
			inAnimator = ObjectAnimator.ofPropertyValuesHolder(null,
					propertiesList.toArray(new PropertyValuesHolder[0]));
			inAnimator.setDuration(duration);

			outAnimator = new AnimatorSet();
			propertiesList = new ArrayList<PropertyValuesHolder>();
			propertiesList.add(PropertyValuesHolder.ofFloat(new AlphaProperty(), 1, 0));
			propertiesList.add(PropertyValuesHolder.ofFloat(new RotationProperty(rotateProp), 0, angle));
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