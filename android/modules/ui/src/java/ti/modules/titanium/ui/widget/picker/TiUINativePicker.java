/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.picker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiViewHelper;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.PickerColumnProxy;
import ti.modules.titanium.ui.PickerProxy;
import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.AppCompatSpinner;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

public class TiUINativePicker extends TiUIPicker 
		implements OnItemSelectedListener
{
	private static final String TAG = "TiUINativePicker";
	private boolean firstSelectedFired = false;
	
	public static class TiSpinnerAdapter<T> extends ArrayAdapter<T>
	{
	    FontDesc fontDesc;

		public TiSpinnerAdapter(Context context, int textViewResourceId, List<T> objects)
		{
			super(context, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			TextView tv = (TextView) super.getView(position, convertView, parent);
			if (fontDesc != null) {
                TiUIHelper.styleText(tv, fontDesc);
			}
			return tv;
		}

		@Override
		public View getDropDownView(int position, View convertView, ViewGroup parent)
		{
			TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
			if (fontDesc != null) {
				TiUIHelper.styleText(tv, fontDesc);
			}
			return tv;
		}
		
		public void setFontDesc(FontDesc desc)
		{
		    fontDesc = desc;
		}
	}
	
	public TiUINativePicker(TiViewProxy proxy) 
	{
		super(proxy);
	}
	public TiUINativePicker(final TiViewProxy proxy, Activity activity)
	{
		this(proxy);
		AppCompatSpinner spinner = new AppCompatSpinner(activity)
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUINativePicker.this);
			}
			
			@Override
			public boolean onTouchEvent(MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP && hierarchyHasListener(TiC.EVENT_CLICK)) {
					fireEventNoCheck(TiC.EVENT_CLICK, TiViewHelper
                            .dictFromMotionEvent(getTouchView(), lastUpEvent));
				}
				return super.onTouchEvent(event);
			}
		};
		setNativeView(spinner);
		refreshNativeView();

		spinner.setOnItemSelectedListener(this);
	}
	
	private AppCompatSpinner getSpinner() {
	    return (AppCompatSpinner)getNativeView();
	}
	
	@Override
	protected void handlePreselectedRows(Object[] preselectedRows){
	    Spinner spinner = getSpinner();
        if (spinner == null)return;
        try {
            spinner.setOnItemSelectedListener(null);
            for (int i = 0; i < preselectedRows.length; i++) {
                Integer rowIndex = TiConvert.toInt(preselectedRows[i], -1);
                if (rowIndex == 0 || rowIndex.intValue() < 0) {
                    continue;
                }
                selectRow(i, rowIndex, false);
            }
        } finally {
            spinner.setOnItemSelectedListener(this);
            firstSelectedFired = true;
        }
    }

	@Override
	public void selectRow(final int columnIndex, final int rowIndex, final boolean animated)
	{
		// At the moment we only support one column.
		if (columnIndex != 0) {
			Log.w(TAG, "Only one column is supported. Ignoring request to set selected row of column " + columnIndex);
			return;
		}
        final Spinner spinner = getSpinner();
		int rowCount = spinner.getAdapter().getCount();
		if (rowIndex < 0 || rowIndex >= rowCount) {
			Log.w(TAG, "Ignoring request to select out-of-bounds row index " + rowIndex);
			return;
		}
		if (TiApplication.isUIThread()) {
	        spinner.setSelection(rowIndex, animated);
        } else {
            proxy.getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    spinner.setSelection(rowIndex, animated);
                }
            });
        }
	}
	
	@Override
	public void openPicker()
	{
        getSpinner().performClick();
	};

	@Override
	public int getSelectedRowIndex(int columnIndex)
	{
		if (columnIndex != 0) {
			Log.w(TAG, "Ignoring request to get selected row from out-of-bounds columnIndex " + columnIndex);
			return -1;
		}
		return getSpinner().getSelectedItemPosition();
	}

	@Override
	protected void refreshNativeView() 
	{
		// Don't allow change events here
		suppressChangeEvent = true;
		Spinner spinner = getSpinner();
		if (spinner == null) {
			return;
		}
		try {
			spinner.setOnItemSelectedListener(null);
			int rememberSelectedRow = getSelectedRowIndex(0);
			// Just one column - the first column - for now.
			// Maybe someday we'll support multiple columns.
			PickerColumnProxy column = getPickerProxy().getFirstColumn(false);
			if (column == null) {
				return;
			}
			KrollProxy[] rowArray = column.getChildren();
			if (rowArray == null || rowArray.length == 0) {
				return;
			}
			ArrayList<KrollProxy> rows = new ArrayList<KrollProxy>(Arrays.asList(rowArray));
			// At the moment we're using the simple spinner layouts provided
			// in android because we're only supporting a piece of text, which
			// is fetched via PickerRowProxy.toString(). If we allow
			// anything beyond a string, we'll have to implement our own
			// layouts (maybe our own Adapter too.)
			TiSpinnerAdapter<KrollProxy> adapter = new TiSpinnerAdapter<KrollProxy>(spinner.getContext(),
				android.R.layout.simple_spinner_item, rows);
			processProperties(getProxy().getProperties());
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			spinner.setAdapter(adapter);
			if (rememberSelectedRow >= 0) {
				selectRow(0, rememberSelectedRow, false);
			}
			
		} catch(Throwable t) {
			Log.e(TAG, "Unable to refresh native spinner control: " + t.getMessage(), t);
		} finally {
			suppressChangeEvent = false;
			spinner.setOnItemSelectedListener(this);
		}
	}
	

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long itemId)
	{
		if (!firstSelectedFired) {
			// swallow the first selected event that gets fired after the adapter gets set, so as to avoid
			// firing our change event in that case.
			firstSelectedFired = true;
			return;
		}
		fireSelectionChange(0, position);

		// Invalidate the parent view after the item is selected (TIMOB-13540).
		if (TiC.HONEYCOMB_OR_GREATER) {
			ViewParent p = nativeView.getParent();
			if (p instanceof View) {
				((View) p).invalidate();
			}
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0)
	{
	}
	public void add(TiUIView child)
	{
		// Don't do anything.  We don't add/remove views to the native picker (the Android "Spinner").
	}
	@Override
	public void remove(TiUIView child)
	{
		// Don't do anything.  We don't add/remove views to the native picker (the Android "Spinner").
	}
	@Override
	public void onColumnAdded(int columnIndex)
	{
		if (!batchModelChange) {
			refreshNativeView();
		}
	}
	@Override
	public void onColumnRemoved(int oldColumnIndex)
	{
		if (!batchModelChange) {
			refreshNativeView();
		}
	}
	@Override
	public void onColumnModelChanged(int columnIndex)
	{
		if (!batchModelChange) {
			refreshNativeView();
		}
	}
	@Override
	public void onRowChanged(int columnIndex, int rowIndex)
	{
		if (!batchModelChange) {
			refreshNativeView();
		}
	}
	protected void fireSelectionChange(int columnIndex, int rowIndex)
	{
		((PickerProxy)proxy).fireSelectionChange(columnIndex, rowIndex);
	}
	
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_FONT:
            if (changedProperty) {
                FontDesc desc = TiUIHelper.getFontStyle(getContext(),
                        TiConvert.toKrollDict(newValue));    
                TiSpinnerAdapter<TiViewProxy> adapter = (TiSpinnerAdapter<TiViewProxy>) getSpinner().getAdapter();
                adapter.setFontDesc(desc);
                adapter.notifyDataSetChanged();
            }
            break;

        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
}