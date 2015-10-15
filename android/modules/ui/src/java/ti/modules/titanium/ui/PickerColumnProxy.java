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
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;

import ti.modules.titanium.ui.PickerRowProxy.PickerRowListener;
import ti.modules.titanium.ui.widget.picker.TiUIPickerColumn;
import ti.modules.titanium.ui.widget.picker.TiUISpinnerColumn;
import android.app.Activity;
import android.util.Log;

@Kroll.proxy(creatableInModule=UIModule.class)
public class PickerColumnProxy extends ViewProxy implements PickerRowListener
{
	private static final String TAG = "PickerColumnProxy";
	private PickerColumnListener columnListener  = null;
	private boolean useSpinner = false;
	private boolean suppressListenerEvents = false;

	// Indicate whether this picker column is not created by users.
	// Users can directly add picker rows to the picker. In this case, we create a picker column for them and this is
	// the only column in the picker.
	private boolean createIfMissing = false;


	public PickerColumnProxy()
	{
		super();
	}

	public PickerColumnProxy(TiContext tiContext)
	{
		this();
	}

	public void setColumnListener(PickerColumnListener listener)
	{
		columnListener = listener;
		if (columnListener != null) {
            columnListener.rowsReplaced(this);
        }
	}
	public void setUseSpinner(boolean value)
	{
		useSpinner = value;
	}

	@Override
	public void handleCreationDict(KrollDict dict) {
		super.handleCreationDict(dict);
		if (dict.containsKey("rows")) {
			Object rowsAtCreation = dict.get("rows");
			if (rowsAtCreation.getClass().isArray()) {
				setRows((Object[]) rowsAtCreation);
			}
		}
	}
	
	private static final String DEFAULT_TEMPLATE_TYPE = "Ti.UI.PickerRow";
    @Override
    protected String defaultProxyTypeFromTemplate() {
        return DEFAULT_TEMPLATE_TYPE;
    }

	@Override
    protected void addProxy(Object args, final int index)
	{
	    if (args instanceof PickerRowProxy) {
            ((PickerRowProxy)args).setRowListener(this);
            super.addProxy((PickerRowProxy)args, index);
        }
	}
	
    @Override
    protected void handleChildAdded(KrollProxy child, int index) {
        super.handleChildAdded(child, index);
        if (columnListener != null && !suppressListenerEvents) {
            columnListener.rowAdded(this, index);
        }
    }
    
    @Override
    protected void handleChildRemoved(KrollProxy child, final int index,
            final boolean shouldDetach) {
        if (columnListener != null && !suppressListenerEvents) {
            columnListener.rowRemoved(this, index);
        }
    }

	@Kroll.method
	public void addRow(Object row)
	{
		if (row instanceof PickerRowProxy) {
			this.add((PickerRowProxy) row);
		} else {
			Log.w(TAG, "Unable to add the row. Invalid type for row.");
		}
	}


	@Kroll.method
	public void removeRow(Object row)
	{
		if (row instanceof PickerRowProxy) {
			this.remove((PickerRowProxy) row);
		} else {
			Log.w(TAG, "Unable to remove the row. Invalid type for row.");
		}
	}

	@Kroll.getProperty @Kroll.method
	public PickerRowProxy[] getRows()
	{
		if (children == null || children.size() == 0) {
			return null;
		}
		return children.toArray(new PickerRowProxy[children.size()]);
	}
	
	@Kroll.setProperty @Kroll.method
	public void setRows(Object[] rows)
	{
//		if (TiApplication.isUIThread() || peekView() == null) {
			handleSetRows(rows);
//
//		} else {
//			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SET_ROWS), rows);
//		}
	}

	private void handleSetRows(Object[] rows)
	{
		try {
			suppressListenerEvents = true;
			removeAllChildren();
//			if (children != null && children.size() > 0) {
//				int count = children.size();
//				for (int i = (count - 1); i >= 0; i--) {
//					remove(children.get(i));
//				}
//			}
			add(rows, Integer.valueOf(-1));
		} finally {
			suppressListenerEvents = false;
		}
		if (columnListener != null) {
			columnListener.rowsReplaced(this);
		}
	}

	@Kroll.getProperty @Kroll.method
	public int getRowCount()
	{
		return children.size();
	}

	@Override
	public TiUIView createView(Activity activity)
	{
	    TiUIView view = null;
		if (useSpinner) {
		    view = new TiUISpinnerColumn(this);
		} else {
		    view = new TiUIPickerColumn(this);
		}
		LayoutParams params = view.getLayoutParams();
        params.sizeOrFillWidthEnabled = true;
        params.autoFillsWidth = true;
        return view;
	}
	
	public interface PickerColumnListener
	{
		void rowAdded(PickerColumnProxy column, int rowIndex);
		void rowRemoved(PickerColumnProxy column, int oldRowIndex);
		void rowChanged(PickerColumnProxy column, int rowIndex);
		void rowSelected(PickerColumnProxy column, int rowIndex);
		void rowsReplaced(PickerColumnProxy column); // wholesale replace of rows
	}

	@Override
	public void rowChanged(PickerRowProxy row)
	{
		if (columnListener != null && !suppressListenerEvents) {
			int index = children.indexOf(row);
			columnListener.rowChanged(this, index);
		}
		
	}
	
	public void onItemSelected(int rowIndex)
	{
		if (columnListener != null && !suppressListenerEvents) {
			columnListener.rowSelected(this, rowIndex);
		}
	}

	public PickerRowProxy getSelectedRow()
	{
		if (!(peekView() instanceof TiUISpinnerColumn)) {
			return null;
		}
		int rowIndex = ((TiUISpinnerColumn)peekView()).getSelectedRowIndex();
		if (rowIndex < 0) {
			return null;
		} else {
			return (PickerRowProxy)children.get(rowIndex);
		}
	}
	
	public int getThisColumnIndex()
	{
		return ((PickerProxy)getParent()).getColumnIndex(this);
	}

	public void parentShouldRequestLayout()
	{
		if (getParent() instanceof PickerProxy) {
			((PickerProxy)getParent()).forceRequestLayout();
		}
	}

	public void setCreateIfMissing(boolean flag)
	{
		createIfMissing = flag;
	}

	public boolean getCreateIfMissing()
	{
		return createIfMissing;
	}

	@Override
	public String getApiName()
	{
		return "Ti.UI.PickerColumn";
	}
}
