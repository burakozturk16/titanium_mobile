/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUIScrollView;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors = {
	TiC.PROPERTY_CONTENT_HEIGHT, TiC.PROPERTY_CONTENT_WIDTH,
	TiC.PROPERTY_SHOW_HORIZONTAL_SCROLL_INDICATOR,
	TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR,
	TiC.PROPERTY_SCROLL_TYPE,
//	TiC.PROPERTY_CONTENT_OFFSET,
	TiC.PROPERTY_CAN_CANCEL_EVENTS,
	TiC.PROPERTY_OVER_SCROLL_MODE
})
public class ScrollViewProxy extends ViewProxy
	implements Handler.Callback
{
	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_SCROLL_TO = MSG_FIRST_ID + 100;
	private static final int MSG_SCROLL_TO_BOTTOM = MSG_FIRST_ID + 101;
	private static final int MSG_SET_CONTENT_OFFSET = MSG_FIRST_ID + 102;
	protected static final int MSG_LAST_ID = MSG_FIRST_ID + 999;

	public ScrollViewProxy()
	{
		super();
		defaultValues.put(TiC.PROPERTY_OVER_SCROLL_MODE, 0);
		
		KrollDict offset = new KrollDict();
		offset.put(TiC.EVENT_PROPERTY_X, 0);
		offset.put(TiC.EVENT_PROPERTY_Y, 0);
		defaultValues.put(TiC.PROPERTY_CONTENT_OFFSET, offset);
	}

	public ScrollViewProxy(TiContext context)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity) {
		return new TiUIScrollView(this);
	}

	public TiUIScrollView getScrollView() {
		return (TiUIScrollView) getOrCreateView();
	}

	@Kroll.method
	public void scrollTo(int x, int y) {
		if (!TiApplication.isUIThread()) {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO, x, y), getActivity());


			//TiApplication.getInstance().getMessageQueue().sendBlockingMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO, x, y), getActivity());
			//sendBlockingUiMessage(MSG_SCROLL_TO, getActivity(), x, y);
		} else {
			handleScrollTo(x,y);
		}
	}
	
	@Kroll.setProperty @Kroll.method
	public void setScrollingEnabled(Object enabled)
	{
		getScrollView().setScrollingEnabled(enabled);
	}

	@Kroll.getProperty @Kroll.method
	public boolean getScrollingEnabled()
	{
		return getScrollView().getScrollingEnabled();
	}
	
	@Kroll.method
	public void setContentOffset(Object offset, @Kroll.argument(optional = true) Object obj)
	{
		Boolean animated = true;
		if (obj instanceof KrollDict) {
			animated = ((KrollDict)obj).optBoolean("animated", animated);
		}
		if (!TiApplication.isUIThread()) {
			getMainHandler().removeMessages(MSG_SET_CONTENT_OFFSET);
			getMainHandler().obtainMessage(MSG_SET_CONTENT_OFFSET, animated?1:0, 0, offset).sendToTarget();
		} else {
			handleSetContentOffset(offset, animated);
		}
	}
	
	@Kroll.setProperty
	public void setContentOffset(Object offset)
	{
		setContentOffset(offset, null);
	}
	
	@Kroll.getProperty @Kroll.method
	public Object getContentOffset()
	{
		return getProperty(TiC.PROPERTY_CONTENT_OFFSET);
	}

	@Kroll.method
	public void scrollToBottom() {
		if (!TiApplication.isUIThread()) {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO_BOTTOM), getActivity());

			//TiApplication.getInstance().getMessageQueue().sendBlockingMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO_BOTTOM), getActivity());
			//sendBlockingUiMessage(MSG_SCROLL_TO_BOTTOM, getActivity());
		} else {
			handleScrollToBottom();
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (msg.what == MSG_SCROLL_TO) {
			handleScrollTo(msg.arg1, msg.arg2);
			AsyncResult result = (AsyncResult) msg.obj;
			result.setResult(null); // signal scrolled
			return true;
		} else if (msg.what == MSG_SET_CONTENT_OFFSET) {
			handleSetContentOffset(msg.obj, msg.arg1 == 1);
			return true;
		} else if (msg.what == MSG_SCROLL_TO_BOTTOM) {
			handleScrollToBottom();
			AsyncResult result = (AsyncResult) msg.obj;
			result.setResult(null); // signal scrolled
			return true;
		}
		return super.handleMessage(msg);
	}

	public void handleScrollTo(int x, int y) {
		getScrollView().scrollTo(x, y);
	}
	
	public void handleSetContentOffset(Object offset, boolean animated) {
		getScrollView().setContentOffset(offset, animated);
	}
	
	
	public void handleScrollToBottom() {
		getScrollView().scrollToBottom();
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.ScrollView";
	}
}
