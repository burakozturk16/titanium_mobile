/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import android.widget.Toast;

public class TiUINotification extends TiUIView
{
	private static final String TAG = "TiUINotifier";

    private Toast toast;
    private String message = null;
    private float horizontalMargin;
    private float verticalMargin;
    private int offsetX;
    private int offsetY;
    private int gravity;

	public TiUINotification(TiViewProxy proxy) {
		super(proxy);
		Log.d(TAG, "Creating a notifier", Log.DEBUG_MODE);
		toast = Toast.makeText(proxy.getActivity(), "", Toast.LENGTH_SHORT);
		horizontalMargin = toast.getHorizontalMargin();
	    verticalMargin = toast.getVerticalMargin();
	    offsetX = toast.getXOffset();
	    offsetY = toast.getYOffset();       
	    gravity = toast.getGravity();
	}

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_MESSAGE:
            message = TiConvert.toString(newValue);
            break;

        case TiC.PROPERTY_DURATION:
            toast.setDuration(TiConvert.toInt(newValue));
            break;

        case "verticalMargin":
            verticalMargin = TiConvert.toFloat(newValue, verticalMargin);
            toast.setMargin(horizontalMargin, verticalMargin);
            break;

        case "horizontalMargin":
            horizontalMargin = TiConvert.toFloat(newValue, horizontalMargin);
            toast.setMargin(horizontalMargin, verticalMargin);
            break;
        case "offsetX":
            offsetX = TiConvert.toInt(newValue, offsetX);
            toast.setGravity(gravity, offsetX, offsetY);
            break;
        case "offsetY":
            offsetY = TiConvert.toInt(newValue, offsetY);
            toast.setGravity(gravity, offsetX, offsetY);
            break;
        case "gravity":
            gravity = TiConvert.toInt(newValue, gravity);
            toast.setGravity(gravity, offsetX, offsetY);
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	public void show(KrollDict options) {

		toast.setText(message);
		toast.show();
	}

	public void hide(KrollDict options) {
		toast.cancel();
	}
}
