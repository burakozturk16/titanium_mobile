/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.animation;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;

@Kroll.proxy
public class TiAnimation extends KrollProxy {
	private TiAnimator animator;
	
	public TiAnimation(){
		animator = null;
	}

	public void setAnimator(TiAnimator animator) {
		this.animator = animator;
	}
	
	@Kroll.getProperty
	public boolean animating() {
		if (animator != null)
			return animator.animating();
		return false;
	}
	
	@Kroll.method
	public void cancel() {
		if (animator != null)
			animator.cancel();
	}
	
	@Kroll.method
	public void cancelWithoutResetting() {
		if (animator != null)
			animator.cancelWithoutResetting();
	}

}
