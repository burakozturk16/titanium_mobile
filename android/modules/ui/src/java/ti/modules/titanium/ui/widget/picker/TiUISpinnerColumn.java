/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.picker;

import java.util.ArrayList;
import java.util.HashMap;

import kankan.wheel.widget.WheelView;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.PickerColumnProxy;
import ti.modules.titanium.ui.PickerProxy;
import ti.modules.titanium.ui.PickerRowProxy;
import android.graphics.Typeface;

public class TiUISpinnerColumn extends TiUIView implements WheelView.OnItemSelectedListener
{
	
	private static final String TAG = "TiUISpinnerColumn";
	private boolean suppressItemSelected = false;
	
	public TiUISpinnerColumn(TiViewProxy proxy)
	{
		super(proxy);
		refreshNativeView();
		preselectRow();
		((WheelView)nativeView).setItemSelectedListener(this);
	}
	
	private void preselectRow()
	{
		if (proxy.getParent() instanceof PickerProxy) {
			ArrayList<Integer> preselectedRows = ((PickerProxy)proxy.getParent()).getPreselectedRows();
			if (preselectedRows == null || preselectedRows.size() == 0) {
				return;
			}
			int columnIndex = ((PickerColumnProxy)proxy).getThisColumnIndex();
			if (columnIndex >= 0 && columnIndex < preselectedRows.size()) {
				Integer rowIndex = preselectedRows.get(columnIndex);
				if (rowIndex != null && rowIndex.intValue() >= 0) {
					selectRow(rowIndex);
				}
			}
		}
	}

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
		if (d.containsKey(TiC.PROPERTY_FONT)) {			
			setFontProperties( d.getKrollDict(TiC.PROPERTY_FONT));
		}
		if (d.containsKey(TiC.PROPERTY_COLOR)) {
			((WheelView)nativeView).setTextColor(new Integer(TiConvert.toColor(d, TiC.PROPERTY_COLOR)));
		}
		if (d.containsKey(TiC.PROPERTY_VISIBLE_ITEMS)) {
			((WheelView)nativeView).setVisibleItems(TiConvert.toInt(d, TiC.PROPERTY_VISIBLE_ITEMS));
		} else {
			((WheelView)nativeView).setVisibleItems(PickerProxy.DEFAULT_VISIBLE_ITEMS_COUNT);
		}
		if (d.containsKey(TiC.PROPERTY_SELECTION_INDICATOR)) {
			((WheelView)nativeView).setShowSelectionIndicator(TiConvert.toBoolean(d, TiC.PROPERTY_SELECTION_INDICATOR));
		}
		refreshNativeView();
	}

	private void setFontProperties(KrollDict d)
	{
		WheelView view = (WheelView)nativeView;

		TiUIHelper.FontDesc desc = TiUIHelper.getFontStyle(view.getContext(), d);
		
		boolean dirty = false;
		if (!desc.typeface.equals(view.getTypeface())) {
			dirty = true;
			view.setTypeface(desc.typeface);
		}
		if (desc.style != view.getTypefaceWeight()) {
			dirty = true;
			view.setTypefaceWeight(desc.style);
		}
		if (desc.size != view.getTextSize()) {
			dirty = true;
			view.setTextSize(desc.size.intValue());
		}
		if (dirty) {
			((PickerColumnProxy)proxy).parentShouldRequestLayout();
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue,
			KrollProxy proxy)
	{
		if (key.equals(TiC.PROPERTY_FONT)) {
			setFontProperties((KrollDict) newValue);
		} else if (key.equals(TiC.PROPERTY_COLOR)) {
			((WheelView)nativeView).setTextColor(new Integer(TiConvert.toColor(TiConvert.toString(newValue))));
		} else if (key.equals(TiC.PROPERTY_VISIBLE_ITEMS)) {
			((WheelView)nativeView).setVisibleItems(TiConvert.toInt(newValue));
		} else if (key.equals(TiC.PROPERTY_SELECTION_INDICATOR)) {
			((WheelView)nativeView).setShowSelectionIndicator(TiConvert.toBoolean(newValue));
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);	
		}
	}
	
	
	public void refreshNativeView()
	{
		WheelView view = null;
		if (nativeView instanceof WheelView) {
			view = (WheelView)nativeView;
		} else {
			view = new WheelView(proxy.getActivity());
			Float defaultFontSize = new Float(TiUIHelper.getSize(TiUIHelper.getDefaultFontSize(proxy.getActivity())));
			view.setTextSize(defaultFontSize.intValue());
			setNativeView(view);
		}
		int selectedRow = view.getCurrentItem();
		PickerRowProxy[] rows = ((PickerColumnProxy)proxy).getRows();
		int rowCount = (rows == null) ? 0 : rows.length;
		if (selectedRow >= rowCount) {
			suppressItemSelected = true;
			if (rowCount > 0) {
				view.setCurrentItem(rowCount - 1);
			} else {
				view.setCurrentItem(0);
			}
			suppressItemSelected = false;
		}
		TextWheelAdapter adapter = null;
		if (rows != null) {
			adapter = new TextWheelAdapter(rows);
		}
		view.setAdapter(adapter);
	}
	
	public void selectRow(int rowIndex)
	{
		if (nativeView instanceof WheelView) {
			WheelView view = (WheelView)nativeView;
			if (rowIndex < 0 || rowIndex >= view.getAdapter().getItemsCount()) {
				Log.w(TAG, "Ignoring attempt to select out-of-bound row index " + rowIndex);
				return;
			}
			view.setCurrentItem(rowIndex);
		}
	}

	@Override
	public void onItemSelected(WheelView view, int index)
	{
		if (suppressItemSelected) {
			return;
		}
		((PickerColumnProxy)proxy).onItemSelected(index);
	}
	
	public int getSelectedRowIndex()
	{
		int result = -1;
		if (nativeView instanceof WheelView) {
			result = ((WheelView)nativeView).getCurrentItem();
		}
		return result;
	}

	public void forceRequestLayout()
	{
		if (nativeView instanceof WheelView) {
			((WheelView)nativeView).fullLayoutReset();
		}
	}

}
