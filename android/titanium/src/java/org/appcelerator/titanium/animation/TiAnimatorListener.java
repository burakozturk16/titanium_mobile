package org.appcelerator.titanium.animation;

import java.util.HashMap;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.Animator.AnimatorListener;
import com.nineoldandroids.animation.AnimatorSet;

import android.view.View;

@SuppressWarnings("rawtypes")
public class TiAnimatorListener implements AnimatorListener {
    private static final String TAG = "TiAnimatorListener";
    protected TiAnimatorSet tiSet;
    protected TiViewProxy viewProxy;
    protected TiAnimation animationProxy;
    protected View view;
    protected HashMap options;

    public TiAnimatorListener(TiViewProxy proxy, TiAnimatorSet tiSet,
            HashMap options) {
        super();
        this.viewProxy = proxy;
        this.tiSet = tiSet;
        this.animationProxy = tiSet.animationProxy;
        this.options = options;
    }

    public TiAnimatorListener(TiAnimatorSet tiSet, HashMap options) {
        super();
        this.tiSet = tiSet;
        this.animationProxy = tiSet.animationProxy;
        this.options = options;
    }

    public TiAnimatorListener(View view, TiAnimation aproxy, HashMap options) {
        super();
        this.view = view;
        this.animationProxy = aproxy;
        this.options = options;
    }

    public TiAnimatorListener(View view) {
        super();
        this.view = view;
    }

    public TiAnimatorListener() {
        super();
    }

//    private void restartAnimatorSet() {
//        // start repeat without reverse
//        if (this.animationProxy != null) {
//            this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
//        }
//        synchronized (tiSet) {
//            tiSet.currentRepeatCount = tiSet.currentRepeatCount - 1;
//            tiSet.resetReverseSet();
//            tiSet.set().start();
//        }
//
//    }

    public void onAnimationEnd(Animator animation) {
        if (tiSet == null) {
            return;// prevent double onEnd!
        }

        if (tiSet.cancelled) {
            tiSet.setAnimating(false);
            tiSet.handleFinish(); // will fire the EVENT_COMPLETE
            return;
        } else if (tiSet.getAnimating() == false) {
            return;
        }
        Log.d(TAG, "onAnimationEnd " + animation, Log.DEBUG_MODE);

        AnimatorSet set = tiSet.set();
        AnimatorSet reverseSet = tiSet.reverseSet();
        int repeatCount = tiSet.repeatCount;
        int currentRepeatCount = tiSet.currentRepeatCount;
        if (reverseSet != null) {
            // the case where we have a reverse
            if (animation == set) {
                // set.setupEndValues();
                tiSet.resetSet();
                reverseSet.start();
                return;
            } else if (animation == reverseSet) {
                if (repeatCount < 0 || currentRepeatCount > 0) {
                    // start repeat without reverse
                    if (this.animationProxy != null) {
                        this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
                    }
                    if (currentRepeatCount > 0)
                        tiSet.currentRepeatCount = tiSet.currentRepeatCount - 1;
                    // reverseSet.setupEndValues();
                    tiSet.resetReverseSet();
                    if (!tiSet.cancelled) {
                        set.start();

                    }
                    return;
                }
            }
        } else if (repeatCount < 0 || currentRepeatCount > 0) {
            // start repeat without reverse
            if (this.animationProxy != null) {
                this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
            }
            if (currentRepeatCount > 0)
                tiSet.currentRepeatCount = tiSet.currentRepeatCount - 1;
            tiSet.resetSet();
            tiSet.restartFromBeginning();
            if (!tiSet.cancelled) {
                tiSet.set().start();
            }
            return;
        }
        tiSet.setAnimating(false);
        tiSet.handleFinish(); // will fire the EVENT_COMPLETE
    }

    public void onAnimationStart(Animator animation) {
        if (tiSet != null
                && (animation == tiSet.reverseSet() || tiSet.getAnimating() == true))
            return;
        Log.d(TAG, "onAnimationStart " + animation, Log.DEBUG_MODE);
        if (tiSet != null)
            tiSet.setAnimating(true);
        if (this.animationProxy != null) {
            this.animationProxy.fireEvent(TiC.EVENT_START, null);
        }
    }

    public void onAnimationCancel(Animator animation) {
        if (tiSet == null || tiSet.getAnimating() == false || tiSet.cancelled)
            return;// prevent double onEnd!
        Log.d(TAG, "onAnimationCancel " + animation, Log.DEBUG_MODE);
        tiSet.setAnimating(false);
        tiSet.handleCancel(); // will fire the EVENT_COMPLETE
    }

    public void onAnimationRepeat(Animator animation) {
        if (animation == tiSet.reverseSet())
            return;
        Log.d(TAG, "onAnimationRepeat " + animation, Log.DEBUG_MODE);
        if (this.animationProxy != null) {
            this.animationProxy.fireEvent(TiC.EVENT_REPEAT, null);
        }
    }

}