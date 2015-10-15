/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.picker;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.view.TiUIView;

import android.content.Context;
import android.graphics.Color;
import antistatic.spinnerwheel.AbstractWheel;
import antistatic.spinnerwheel.OnWheelChangedListener;
import antistatic.spinnerwheel.WheelVerticalView;
import antistatic.spinnerwheel.adapters.AbstractWheelTextAdapter;
import ti.modules.titanium.ui.PickerColumnProxy;
import ti.modules.titanium.ui.PickerProxy;
import ti.modules.titanium.ui.PickerRowProxy;

public class TiUISpinnerColumn extends TiUIView implements OnWheelChangedListener
{
	
	private static final String TAG = "TiUISpinnerColumn";
	private boolean suppressItemSelected = false;
	
	public TiUISpinnerColumn(TiViewProxy proxy)
	{
		super(proxy);
		if (proxy instanceof PickerColumnProxy && ((PickerColumnProxy)proxy).getCreateIfMissing()) {
			layoutParams.autoFillsWidth = true;
		}
		refreshNativeView();
		((AbstractWheel)nativeView).addChangingListener(this);
	}
	
	public AbstractWheel getWheelView() {
	    return (AbstractWheel)nativeView;
	}
	public TextWheelAdapter getWheelAdapter() {
	    if (nativeView != null) {
	        return (TextWheelAdapter) getWheelView().getViewAdapter();
	    }
	    return null;
    }
	@Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_FONT:
            AbstractWheelTextAdapter adapter = getWheelAdapter();
            FontDesc desc = TiUIHelper.getFontStyle(getContext(),
                    TiConvert.toKrollDict(newValue));            
            boolean dirty = false;
            if (!desc.typeface.equals(adapter.getTextTypeface())) {
                dirty = true;
                adapter.setTextTypeface(desc.typeface);
            }
            if (desc.size != adapter.getTextSize()) {
                dirty = true;
                adapter.setTextSize(desc.size.intValue());
            }
            if (dirty) {
                ((PickerColumnProxy)proxy).parentShouldRequestLayout();
            }
            break;
        case TiC.PROPERTY_COLOR:
            if (getWheelAdapter() != null) {
                getWheelAdapter().setTextColor(new Integer(TiConvert.toColor(newValue)));
            }
            break;
        case TiC.PROPERTY_VISIBLE_ITEMS:
            getWheelView().setVisibleItems(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_SELECTION_INDICATOR:
//            getWheelView().setShowSelectionIndicator(TiConvert.toBoolean(newValue));
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
		if (!d.containsKey(TiC.PROPERTY_VISIBLE_ITEMS)) {
		    getWheelView().setVisibleItems(PickerProxy.DEFAULT_VISIBLE_ITEMS_COUNT);
		}
		refreshNativeView();
	}

	public void refreshNativeView()
	{
	    AbstractWheel view = null;
	    TextWheelAdapter adapter = null;
		if (nativeView instanceof AbstractWheel) {
			view = (AbstractWheel)nativeView;
			adapter = getWheelAdapter();
		} else {
		    final Context context = proxy.getActivity();
			view = new WheelVerticalView(context);
            adapter = new TextWheelAdapter(context, new Object[] {});
            adapter.setTextColor(Color.WHITE);
            view.setViewAdapter(adapter);
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
		if (rows != null) {
	        adapter.setValues(rows);
		}
	}
	
	public void selectRow(final int rowIndex)
	{
		if (nativeView instanceof AbstractWheel) {
			final AbstractWheel view = (AbstractWheel)nativeView;
			if (rowIndex < 0 || rowIndex >= view.getViewAdapter().getItemsCount()) {
				Log.w(TAG, "Ignoring attempt to select out-of-bound row index " + rowIndex);
				return;
			}
			if (TiApplication.isUIThread()) {
	            view.setCurrentItem(rowIndex);
	        } else {
	            proxy.getActivity().runOnUiThread(new Runnable() {
	                @Override
	                public void run() {
	                    view.setCurrentItem(rowIndex);
	                }
	            });
	        }
		}
	}

	@Override
    public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
		if (suppressItemSelected) {
			return;
		}
		((PickerColumnProxy)proxy).onItemSelected(newValue);
	}
	
	public int getSelectedRowIndex()
	{
		int result = -1;
		if (nativeView instanceof AbstractWheel) {
			result = getWheelView().getCurrentItem();
		}
		return result;
	}

	public void forceRequestLayout()
	{
		if (nativeView instanceof AbstractWheel) {
			getWheelView().requestLayout();
		}
	}

}
