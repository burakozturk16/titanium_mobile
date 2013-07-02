package org.appcelerator.titanium.util;

import android.animation.AnimatorSet;
import android.annotation.TargetApi;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class TiAnimatorSet extends TiAnimationBuilder {
	private AnimatorSet set;

	public TiAnimatorSet() {
		set = new AnimatorSet();
	}

	public AnimatorSet set() {
		return set;
	}
	
	@Override
	public void cancel() {
		set.cancel();
		animating = false; //will prevent the call the handleFinish
	}
	
	public void setAnimating (boolean value) {
		animating = value;
	}
	
	public boolean getAnimating () {
		return animating;
	}
}