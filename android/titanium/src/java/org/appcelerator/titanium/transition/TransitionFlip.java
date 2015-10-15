package org.appcelerator.titanium.transition;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.animation.AlphaProperty;
import org.appcelerator.titanium.animation.RotationProperty;
import org.appcelerator.titanium.transition.TransitionHelper.SubTypes;
import org.appcelerator.titanium.util.TiViewHelper;

import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.view.ViewHelper;

public class TransitionFlip extends Transition {
    private static float CAMERA_DISTANCE = 10000;
    public TransitionFlip(int subtype, boolean isOut, int duration) {
        super(subtype, isOut, duration, 300);
    }

    public int getType() {
        return TransitionHelper.Types.kTransitionFlip.ordinal();
    }

    protected void prepareAnimators(View inTarget, View outTarget) {
        float destAngle = 180;
        if (isOut) {
            destAngle = -destAngle;
        }

        String rotateProp = "y";
        if (TransitionHelper.isVerticalSubType(subType)) {
            rotateProp = "x";
        }
        if (subType == SubTypes.kLeftToRight
                || subType == SubTypes.kBottomToTop) {
            destAngle = -destAngle;
        }

        inAnimator = new AnimatorSet();
        Animator anim1 = ObjectAnimator.ofFloat(null, new RotationProperty(
                rotateProp), destAngle, 0);
        anim1.setInterpolator(new AccelerateDecelerateInterpolator());
        anim1.setDuration(duration);
        Animator anim2 = ObjectAnimator
                .ofFloat(null, new AlphaProperty(), 0, 1);
        anim2.setDuration(0);
        anim2.setStartDelay(duration / 2);
        ((AnimatorSet) inAnimator).playTogether(anim1, anim2);

        outAnimator = new AnimatorSet();
        Animator anim3 = ObjectAnimator.ofFloat(null, new RotationProperty(
                rotateProp), 0.0f, -destAngle);
        anim3.setDuration(duration);
        anim3.setInterpolator(new AccelerateDecelerateInterpolator());
        Animator anim4 = ObjectAnimator
                .ofFloat(null, new AlphaProperty(), 1, 0);
        anim4.setDuration(0);
        anim4.setStartDelay(duration / 2);
        ((AnimatorSet) outAnimator).playTogether(anim3, anim4);
    }

    public void setTargets(boolean reversed, View holder, View inTarget,
            View outTarget) {
        super.setTargets(reversed, holder, inTarget, outTarget);
        if (inTarget != null)
            ViewHelper.setAlpha(inTarget, 0);
    }

    @Override
    public void transformView(View view, float position) {
        boolean out = (position < 0);
        if (!TransitionHelper.isPushSubType(subType)) {
            out = !out;
        }
        
        float alpha = (Math.abs(position) > 0.5) ? 0 : 1;
        ViewHelper.setAlpha(view, alpha);
        float rot = 180 * position;
        view.setVisibility((Math.abs(rot) >= 180)?View.INVISIBLE:View.VISIBLE);
        TiViewHelper.setPivotFloat(view, 0.5f, 0.5f);
        if (TiC.ICS_OR_GREATER) {
            view.setCameraDistance(CAMERA_DISTANCE * TiApplication.getAppDensity());
        }
        if (TransitionHelper.isVerticalSubType(subType)) {
            ViewHelper.setRotationX(view, rot);
        } else {
            ViewHelper.setRotationY(view, rot);
        }
    }
}