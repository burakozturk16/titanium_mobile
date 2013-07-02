/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.util.TiAnimationBuilder;

@Kroll.proxy
public class TiAnimation extends KrollProxy {
	private TiAnimationBuilder builder;
	
	public TiAnimation(){
		builder = null;
	}

	public void setBuilder(TiAnimationBuilder tiAnimationBuilder) {
		builder = tiAnimationBuilder;
	}
	
	@Kroll.getProperty
	public boolean animating() {
		if (builder != null)
			return builder.animating();
		return false;
	}
	
	@Kroll.method
	public void cancel() {
		if (builder != null)
			builder.cancel();
	}

}
