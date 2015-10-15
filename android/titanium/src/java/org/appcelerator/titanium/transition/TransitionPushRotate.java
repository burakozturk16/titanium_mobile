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
import com.nineoldandroids.view.ViewHelper;

public class TransitionPushRotate extends Transition {
		private static final float angle = 90;
		public TransitionPushRotate(int subtype, boolean isOut, int duration) {
			super(subtype, isOut, duration, 300);
		}
		public int getType(){
			return TransitionHelper.Types.kTransitionPushRotate.ordinal();
		}
		protected void prepareAnimators(View inTarget, View outTarget) {
			
			float destAngle = -angle;		
			float destTrans = rect.width();
			
			String translateProp = "x";
			String rotateProp = "y";
			
			if (TransitionHelper.isVerticalSubType(subType)) {
				destTrans = rect.height();
				translateProp = "y";
				rotateProp = "x";
			}
			if (!TransitionHelper.isPushSubType(subType)) {
				destTrans = -destTrans;
				destAngle = -destAngle;
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
		public void setTargets(boolean reversed, View holder, View inTarget, View outTarget) {
			super.setTargets(reversed, holder, inTarget, outTarget);
			if (!reversed && inTarget != null) {
				float multiplier = 1.0f;
				float dest = rect.width();
				if (!TransitionHelper.isPushSubType(subType)) {
					multiplier = -dest;
				}
				
				if (TransitionHelper.isVerticalSubType(subType)) {
					dest = rect.height();
					ViewHelper.setTranslationY(inTarget, dest*multiplier);
				}
				else {
					ViewHelper.setTranslationX(inTarget, dest*multiplier);
				}
			}
			View target = reversed?inTarget:outTarget;
			if (target != null) {
				TiViewHelper.setPivotFloatX(target, (subType == SubTypes.kLeftToRight)?1:0);
				TiViewHelper.setPivotFloatY(target, (subType == SubTypes.kTopToBottom)?1:0);
			}
			
		}
	}