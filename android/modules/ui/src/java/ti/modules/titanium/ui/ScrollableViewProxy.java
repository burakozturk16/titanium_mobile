/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;

import ti.modules.titanium.ui.widget.TiUIScrollableView;
import android.app.Activity;
import android.os.Message;

@Kroll.proxy(creatableInModule=UIModule.class, propertyAccessors={
	TiC.PROPERTY_PAGE_OFFSET,
	TiC.PROPERTY_PAGE_WIDTH,
	TiC.PROPERTY_CACHE_SIZE,
	TiC.PROPERTY_SHOW_PAGING_CONTROL,
	TiC.PROPERTY_OVER_SCROLL_MODE,
	TiC.PROPERTY_CURRENT_PAGE,
	TiC.PROPERTY_TRANSITION
})
public class ScrollableViewProxy extends TiViewProxy
{
	private static final String TAG = "TiScrollableView";

	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
	public static final int MSG_HIDE_PAGER = MSG_FIRST_ID + 101;
	public static final int MSG_MOVE_PREV = MSG_FIRST_ID + 102;
	public static final int MSG_MOVE_NEXT = MSG_FIRST_ID + 103;
	public static final int MSG_SCROLL_TO = MSG_FIRST_ID + 104;
	public static final int MSG_SET_VIEWS = MSG_FIRST_ID + 105;
	public static final int MSG_ADD_VIEW = MSG_FIRST_ID + 106;
	public static final int MSG_SET_CURRENT = MSG_FIRST_ID + 107;
	public static final int MSG_REMOVE_VIEW = MSG_FIRST_ID + 108;
//	public static final int MSG_SET_ENABLED = MSG_FIRST_ID + 109;
	public static final int MSG_LAST_ID = MSG_FIRST_ID + 999;
	
	private static final int DEFAULT_PAGING_CONTROL_TIMEOUT = 3000;

	protected AtomicBoolean inScroll;

	public ScrollableViewProxy()
	{
		super();
		inScroll = new AtomicBoolean(false);
		defaultValues.put(TiC.PROPERTY_SHOW_PAGING_CONTROL, false);
        defaultValues.put(TiC.PROPERTY_OVER_SCROLL_MODE, 0);
        defaultValues.put(TiC.PROPERTY_CURRENT_PAGE, 0);
	}

	public ScrollableViewProxy(TiContext context)
	{
		this();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		TiUIView view = new TiUIScrollableView(this, (TiBaseActivity) activity);
        LayoutParams params = view.getLayoutParams();
        params.sizeOrFillWidthEnabled = true;
        params.sizeOrFillHeightEnabled = true;
        params.autoFillsHeight = true;
        params.autoFillsWidth = true;
        return view; 
	}
	
	//only for tableview magic
	@Override
	public void clearViews()
	{
		super.clearViews();
		getView().clearViewsForTableView();
	}

	protected TiUIScrollableView getView()
	{
		return (TiUIScrollableView) getOrCreateView();
	}

	public boolean handleMessage(Message msg)
	{
		boolean handled = false;
		TiUIScrollableView view = getView();
        if (view == null) {
            return true;
        }
		switch(msg.what) {
			case MSG_HIDE_PAGER:
			    view.hidePager();
				handled = true;
				break;
			case MSG_MOVE_PREV:
				inScroll.set(true);
				view.movePrevious(msg.arg1 == 1);
				inScroll.set(false);
				handled = true;
				break;
			case MSG_MOVE_NEXT:
				inScroll.set(true);
				view.moveNext(msg.arg1 == 1);
				inScroll.set(false);
				handled = true;
				break;
			case MSG_SCROLL_TO:
				inScroll.set(true);
				view.scrollTo(msg.obj, msg.arg1 == 1);
				inScroll.set(false);
				handled = true;
				break;
			case MSG_SET_CURRENT:
			    view.setCurrentPage(msg.obj);
				handled = true;
				break;
			case MSG_SET_VIEWS: {
				AsyncResult holder = (AsyncResult) msg.obj;
				Object views = holder.getArg(); 
				view.setViews(views);
				holder.setResult(null);
				handled = true;
				break;
			}
			case MSG_ADD_VIEW: {
				AsyncResult holder = (AsyncResult) msg.obj;
				Object proxy = holder.getArg();
				if (proxy instanceof TiViewProxy) {
				    view.addView((TiViewProxy) proxy);
					handled = true;
				} else if (view != null) {
					Log.w(TAG, "addView() ignored. Expected a Titanium view object, got " + view.getClass().getSimpleName());
				}
				holder.setResult(null);
				break;
			}
			case MSG_REMOVE_VIEW: {
				AsyncResult holder = (AsyncResult) msg.obj;
				Object proxy = holder.getArg(); 
				if (proxy instanceof TiViewProxy) {
				    view.removeView((TiViewProxy) proxy);
					handled = true;
				} else if (proxy instanceof Integer) {
				    view.removeView((Integer) proxy);
					handled = true;
				} else if (view != null) {
					Log.w(TAG, "removeView() ignored. Expected a Titanium view object, got " + view.getClass().getSimpleName());
				}
				holder.setResult(null);
				break;
			}
//			case MSG_SET_ENABLED: {
//				getView().setEnabled(msg.obj);
//				handled = true;
//				break;
//			}
			default:
				handled = super.handleMessage(msg);
		}

		return handled;
	}

	@Kroll.getProperty @Kroll.method
	public Object getViews()
	{
		List<TiViewProxy> list = new ArrayList<TiViewProxy>();
		return getView().getViews().toArray(new TiViewProxy[list.size()]);
	}

	@Kroll.setProperty @Kroll.method
	public void setViews(Object viewsObject)
	{
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_VIEWS), viewsObject);
	}

	@Kroll.method
	public void addView(Object viewObject)
	{
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ADD_VIEW), viewObject);
	}

	@Kroll.method
	public void removeView(Object viewObject)
	{
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REMOVE_VIEW), viewObject);
	}

	@Kroll.method
	public void scrollToView(Object view, @Kroll.argument(optional = true) Object obj)
	{
		if (inScroll.get()) return;

		Boolean animated = true;
		if (obj != null) {
			animated = TiConvert.toBoolean(obj);
		}

		getMainHandler().obtainMessage(MSG_SCROLL_TO, animated?1:0, 0, view).sendToTarget();
	}

	@Kroll.method
	public void movePrevious(@Kroll.argument(optional = true) Object obj)
	{
		if (inScroll.get()) return;


		Boolean animated = true;
		if (obj != null) {
			animated = TiConvert.toBoolean(obj);
		}

		getMainHandler().removeMessages(MSG_MOVE_PREV);
		getMainHandler().obtainMessage(MSG_MOVE_PREV, animated?1:0, 0, null).sendToTarget();
	}

	@Kroll.method
	public void moveNext(@Kroll.argument(optional = true) Object obj)
	{
		if (inScroll.get()) return;

		Boolean animated = true;
		if (obj != null) {
			animated = TiConvert.toBoolean(obj);
		}

		getMainHandler().removeMessages(MSG_MOVE_NEXT);
		getMainHandler().obtainMessage(MSG_MOVE_NEXT, animated?1:0, 0, null).sendToTarget();
	}

	public void setPagerTimeout()
	{
		getMainHandler().removeMessages(MSG_HIDE_PAGER);

		int timeout = DEFAULT_PAGING_CONTROL_TIMEOUT;
		Object o = getProperty(TiC.PROPERTY_PAGING_CONTROL_TIMEOUT);
		if (o != null) {
			timeout = TiConvert.toInt(o);
		}

		if (timeout > 0) {
			getMainHandler().sendEmptyMessageDelayed(MSG_HIDE_PAGER, timeout);
		}
	}

	public void fireDragEnd(int currentPage, TiViewProxy currentView) {
		setProperty(TiC.PROPERTY_CURRENT_PAGE, currentPage);
		if (hasListeners(TiC.EVENT_DRAGEND)) {
			KrollDict options = new KrollDict();
            options.put(TiC.PROPERTY_VIEW, currentView);
			options.put(TiC.PROPERTY_CURRENT_PAGE, currentPage);
			fireEvent(TiC.EVENT_DRAGEND, options, false, false);
		}
	}

	public void fireScrollEnd(int currentPage, TiViewProxy currentView)
	{
		setProperty(TiC.PROPERTY_CURRENT_PAGE, currentPage);
		if (hasListeners(TiC.EVENT_SCROLLEND)) {
			KrollDict options = new KrollDict();
            options.put(TiC.PROPERTY_VIEW, currentView);
			options.put(TiC.PROPERTY_CURRENT_PAGE, currentPage);
			fireEvent(TiC.EVENT_SCROLLEND, options, false, false);
		}
	}

	public void fireScroll(int currentPage, float currentPageAsFloat, TiViewProxy currentView)
	{
		if (hasListeners(TiC.EVENT_SCROLL)) {
			KrollDict options = new KrollDict();
			options.put(TiC.PROPERTY_VIEW, currentView);
			options.put(TiC.PROPERTY_CURRENT_PAGE, currentPage);
			options.put("currentPageAsFloat", currentPageAsFloat);
			fireEvent(TiC.EVENT_SCROLL, options);
		}
	}
	
	public void fireScrollStart(int currentPage, TiViewProxy currentView)
	{
		if (hasListeners(TiC.EVENT_SCROLLSTART)) {
			KrollDict options = new KrollDict();
			options.put(TiC.PROPERTY_VIEW, currentView);
			options.put(TiC.PROPERTY_CURRENT_PAGE, currentPage);
			fireEvent(TiC.EVENT_SCROLLSTART, options, false, false);
		}
	}
	
	public void firePageChange(int currentPage, TiViewProxy currentView)
	{
		if (hasListeners(TiC.EVENT_CHANGE)) {
			KrollDict options = new KrollDict();
            options.put(TiC.PROPERTY_VIEW, currentView);
			options.put(TiC.PROPERTY_CURRENT_PAGE, currentPage);
			fireEvent(TiC.EVENT_CHANGE, options, false, false);
		}
	}
	
//	@Kroll.setProperty @Kroll.method
//	public void setScrollingEnabled(Object enabled)
//	{
//		getMainHandler().obtainMessage(MSG_SET_ENABLED, enabled).sendToTarget();
//	}

//	@Kroll.getProperty @Kroll.method
//	public boolean getScrollingEnabled()
//	{
//		return getView().getEnabled();
//	}

	@Kroll.getProperty @Kroll.method
	public int getCurrentPage()
	{
		return getView().getCurrentPage();
	}

	@Kroll.setProperty @Kroll.method
	public void setCurrentPage(Object page)
	{
		//getView().setCurrentPage(page);
		getMainHandler().obtainMessage(MSG_SET_CURRENT, page).sendToTarget();
	}

	@Override
	public void releaseViews(boolean activityFinishing)
	{
		getMainHandler().removeMessages(MSG_HIDE_PAGER);
		super.releaseViews(activityFinishing);
	}
	
	@Override
	public void setActivity(Activity activity)
	{
	    super.setActivity(activity);
		Object viewsObject = getProperty(TiC.PROPERTY_VIEWS);
		if (viewsObject instanceof Object[]) {
			Object[] views = (Object[])viewsObject;
			for (int i = 0; i < views.length; i++) {
				if (views[i] instanceof TiViewProxy) {
					TiViewProxy proxy = (TiViewProxy)views[i];
					proxy.setActivity(activity);
				}
			}
		}
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.ScrollableView";
	}
}
