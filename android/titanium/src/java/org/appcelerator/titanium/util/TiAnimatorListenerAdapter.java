package org.appcelerator.titanium.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiAnimation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@SuppressWarnings("rawtypes")
public class TiAnimatorListenerAdapter extends AnimatorListenerAdapter {
	protected TiAnimatorSet tiSet;
	protected TiViewProxy viewProxy;
	protected TiAnimation animationProxy;
	protected View view;
	protected HashMap options;
	protected boolean cancelled = false;
	
	public TiAnimatorListenerAdapter(TiViewProxy proxy,
			TiAnimatorSet tiSet, HashMap options) {
		super();
		this.viewProxy = proxy;
		this.tiSet = tiSet;
		this.animationProxy = tiSet.animationProxy;
		this.options = options;
	}

	public TiAnimatorListenerAdapter(View view, TiAnimation aproxy,
			HashMap options) {
		super();
		this.tiSet = tiSet;
		this.view = view;
		this.animationProxy = aproxy;
		this.options = options;
	}

	public TiAnimatorListenerAdapter(View view) {
		super();
		this.view = view;
	}

	public TiAnimatorListenerAdapter() {
		super();
	}

	
	public void cancel(){
		cancelled = true;
	}
	
	public void onAnimationEnd(Animator animation) {
		if (cancelled) return;

		if (tiSet != null) {
			tiSet.handleFinish();
		}
	}

	public void onAnimationStart(Animator animation) {
		if (this.animationProxy != null) {
			this.animationProxy.fireEvent(TiC.EVENT_START, null);
		}
	}

	protected void onComplete() {
		if (viewProxy != null) {
			Iterator it = null;
			if (animationProxy != null) {
				it = animationProxy.getProperties().entrySet().iterator();
			} else if (options != null) {
				it = options.entrySet().iterator();
			}
			if (it != null) {
				while (it.hasNext()) {
					Map.Entry pairs = (Map.Entry) it.next();
					String key = (String) pairs.getKey();
					if (key.compareTo(TiC.PROPERTY_DURATION) != 0
							&& key.compareTo(TiC.PROPERTY_DELAY) != 0
							&& key.compareTo(TiC.PROPERTY_REPEAT) != 0)
						viewProxy.setProperty(key, pairs.getValue());
				}
			}
			viewProxy.clearAnimation(tiSet);
		}
	}
}