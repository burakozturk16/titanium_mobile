package org.appcelerator.titanium.util;

import org.appcelerator.titanium.view.TiAnimation;

import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TiAnimatorSet extends TiAnimationBuilder {
	private AnimatorSet set = new AnimatorSet();

	public TiAnimatorSet() {
	}

	public AnimatorSet set() {
		return set;
	}

}