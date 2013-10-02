/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import android.graphics.drawable.Drawable;

public interface TiLoadImageListener
{
	public void loadImageFinished(int hash, Drawable drawable);

	public void loadImageFailed();
}
