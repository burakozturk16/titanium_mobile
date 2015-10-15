/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.ParentingProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.widget.TiUITableView;
import ti.modules.titanium.ui.widget.tableview.TableViewModel;
import ti.modules.titanium.ui.widget.tableview.TableViewModel.Item;
import ti.modules.titanium.ui.widget.tableview.TiTableViewRowProxyItem;
import android.app.Activity;
import android.os.Message;
import android.view.View;

@Kroll.proxy(creatableInModule=UIModule.class,
propertyAccessors = {
	TiC.PROPERTY_HAS_CHECK,
	TiC.PROPERTY_HAS_CHILD,
	TiC.PROPERTY_CLASS_NAME,
	TiC.PROPERTY_NAME,
	TiC.PROPERTY_LAYOUT,
	TiC.PROPERTY_LEFT_IMAGE,
	TiC.PROPERTY_RIGHT_IMAGE,
	TiC.PROPERTY_TITLE,
	TiC.PROPERTY_HEADER,
	TiC.PROPERTY_FOOTER
})
public class TableViewRowProxy extends ViewProxy
{
	private static final String TAG = "TableViewRowProxy";

	protected ArrayList<TiViewProxy> controls;
	protected TiTableViewRowProxyItem tableViewItem;
	public int index;

	private static final int MSG_SET_DATA = TiViewProxy.MSG_LAST_ID + 5001;
	private static final int MSG_ADD_CONTROL = TiViewProxy.MSG_LAST_ID + 5002;
	private static final int MSG_REMOVE_CONTROL = TiViewProxy.MSG_LAST_ID + 5003;

	public Boolean needsAnimation = false;

	public TableViewRowProxy()
	{
		super();
	}

	public TableViewRowProxy(TiContext tiContext)
	{
		this();
	}

	@Override
	public void setActivity(Activity activity)
	{
		super.setActivity(activity);
		if (controls != null) {
			for (TiViewProxy control : controls) {
				control.setActivity(activity);
			}
		}
	}

	@Override
	public void handleCreationDict(KrollDict options)
	{
		super.handleCreationDict(options);
		if (options.containsKey(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR)) {
			Log.w(TAG, "selectedBackgroundColor is deprecated, use backgroundSelectedColor instead");
			setProperty(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR, options.get(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR));
		}
		if (options.containsKey(TiC.PROPERTY_SELECTED_BACKGROUND_IMAGE)) {
			Log.w(TAG, "selectedBackgroundImage is deprecated, use backgroundSelectedImage instead");
			setProperty(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE, options.get(TiC.PROPERTY_SELECTED_BACKGROUND_IMAGE));
		}
	}

	public void setCreationProperties(KrollDict options)
	{
		for (String key : options.keySet()) {
			setProperty(key, options.get(key));
		}
	}

	@Override
	public TiUIView createView(Activity activity)
	{
		return null;
	}
	
	@Override
	public View parentViewForChild(TiViewProxy child)
	{
		if (tableViewItem == null) return null;
		return tableViewItem.getView();
	}

	public ArrayList<TiViewProxy> getControls()
	{
		return controls;
	}

	public boolean hasControls()
	{
		return (controls != null && controls.size() > 0);
	}

	@Override
	public TiViewProxy[] getChildren()
	{
		if (controls == null) {
			return new TiViewProxy[0];
		}
		return controls.toArray(new TiViewProxy[controls.size()]);
	}

	@Override
    protected void addProxy(Object args, final int index)
	{
		TiViewProxy child = null;
		if (args instanceof TiViewProxy)
			child = (TiViewProxy) args;
		if (child == null) {
		    return;
		}
		if (controls == null) {
			controls = new ArrayList<TiViewProxy>();
		}
		controls.add(child);
		child.setParent(this);
		if (tableViewItem != null) {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ADD_CONTROL), child);
		}
	}

	@Override
	public void remove(Object args)
	{
		if (controls == null) {
			return;
		}
		TiViewProxy control = null;
        if (args instanceof TiViewProxy)
            control = (TiViewProxy) args;
        if (control == null) {
            return;
        }
		control.setParent(null);
		controls.remove(control);
		if (tableViewItem != null) {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REMOVE_CONTROL), control);
		}
	}

	public void setTableViewItem(TiTableViewRowProxyItem item)
	{
		this.tableViewItem = item;
		setModelListener(item, false);
	}

	public TableViewProxy getTable()
	{
	    ParentingProxy parent = getParent();
		while (!(parent instanceof TableViewProxy) && parent != null) {
			parent = parent.getParent();
		}
		return (TableViewProxy) parent;
	}

	@Override
    public void setPropertyAndFire(String name, Object value)
	{
		super.setPropertyAndFire(name, value);
		if (tableViewItem != null) {
			if (TiApplication.isUIThread()) {
				tableViewItem.setRowData(this);
			} else {
				Message message = getMainHandler().obtainMessage(MSG_SET_DATA);
				// Message msg = getUIHandler().obtainMessage(MSG_SET_DATA);
				message.sendToTarget();
			}
		}
	}

	@Override
	public boolean handleMessage(Message msg)
	{
		if (msg.what == MSG_SET_DATA) {
			if (tableViewItem != null) {
				tableViewItem.setRowData(this);
				// update/refresh table view when a row's data changed.
				TiUITableView table = getTable().getTableView();
				table.setModelDirty();
				table.updateView();
			}
			return true;
		}
		else if (msg.what == MSG_ADD_CONTROL) {
			if (tableViewItem != null) {
				AsyncResult holder = (AsyncResult) msg.obj;
				Object view = holder.getArg();
				if (view instanceof TiViewProxy) {
					tableViewItem.addControl((TiViewProxy) view);
				} else if (view != null) {
					Log.w(TAG, "addView() ignored. Expected a Titanium view object, got " + view.getClass().getSimpleName());
				}
				holder.setResult(null);
			}
			return true;
		}
		else if (msg.what == MSG_REMOVE_CONTROL) {
			if (tableViewItem != null) {
				AsyncResult holder = (AsyncResult) msg.obj;
				Object view = holder.getArg();
				if (view instanceof TiViewProxy) {
					tableViewItem.removeControl((TiViewProxy) view);
				} else if (view != null) {
					Log.w(TAG, "addView() ignored. Expected a Titanium view object, got " + view.getClass().getSimpleName());
				}
				holder.setResult(null);
			}
			return true;
		}
		return super.handleMessage(msg);
	}

	public static void fillClickEvent(HashMap<String, Object> data, TableViewModel model, Item item)
	{
		// Don't include rowData if we click on a section
		if (!(item.proxy instanceof TableViewSectionProxy)) {
			data.put(TiC.PROPERTY_ROW_DATA, item.rowData);
		}

		data.put(TiC.PROPERTY_SECTION, model.getSection(item.sectionIndex));
		data.put(TiC.EVENT_PROPERTY_ROW, item.proxy);
		data.put(TiC.EVENT_PROPERTY_INDEX, item.index);
		data.put(TiC.EVENT_PROPERTY_DETAIL, false);
	}

	@Override
	public boolean fireEvent(String eventName, Object data, boolean bubbles)
	{
		// Inject row click data for events coming from row children.
		TableViewProxy table = getTable();
		if (tableViewItem != null) {
			Item item = tableViewItem.getRowData();
			if (table != null && item != null && data instanceof HashMap) {
				// The data object may already be in use by the runtime thread
				// due to a child view's event fire. Create a copy to be thread safe.
				@SuppressWarnings("unchecked")
				KrollDict dataCopy = new KrollDict((HashMap<String, Object>) data);
				fillClickEvent(dataCopy, table.getTableView().getModel(), item);
				data = dataCopy;
			}
		}

		return super.fireEvent(eventName, data, bubbles);
	}

	@Override
	public void firePropertyChanged(String name, Object oldValue, Object newValue)
	{
		super.firePropertyChanged(name, oldValue, newValue);
		TableViewProxy table = getTable();
		if (table != null) {
			table.updateView();
		}
	}

	public void setLabelsClickable(boolean clickable)
	{
		if (controls != null) {
			for (TiViewProxy control : controls) {
				if (control instanceof LabelProxy) {
					((LabelProxy) control).setClickable(clickable);
				}
			}
		}
	}

	@Override
	public void releaseViews(boolean activityFinishing)
	{
		super.releaseViews(activityFinishing);
		if (tableViewItem != null) {
			tableViewItem.release();
			tableViewItem = null;
		}
		if (controls != null) {
			for (TiViewProxy control : controls) {
				control.releaseViews(activityFinishing);
			}
		}
	}

	public TiTableViewRowProxyItem getTableViewRowProxyItem()
	{
		return tableViewItem;
	}
	
	@Override
	public TiUIView peekView()
	{
		return null;
	}

	@Override
	public View getOuterView()
	{
		return tableViewItem;
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.TableViewRow";
	}
}
