/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.searchbar;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.view.TiCompositeLayout;

import ti.modules.titanium.ui.widget.TiUIText;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;

public class TiUISearchBar extends TiUIText
{
	protected ImageButton cancelBtn;
	protected TiUIText textfield;
	
	public interface OnSearchChangeListener {
		public void filterBy(String text);
	}
	
	protected OnSearchChangeListener searchChangeListener;
	
	public TiUISearchBar(final TiViewProxy proxy)
	{
		super(proxy, true);
		textfield = this;
		TiCompositeLayout.LayoutParams params = getLayoutParams();
		params.autoFillsWidth = true;

		// TODO Add Filter support

		// Steal the Text's nativeView. We're going to replace it with our layout.
		cancelBtn = new ImageButton(proxy.getActivity());
		cancelBtn.isFocusable();
		cancelBtn.setImageResource(android.R.drawable.ic_input_delete);
		// set some minimum dimensions for the cancel button, in a density-independent way.
		final float scale = cancelBtn.getContext().getResources().getDisplayMetrics().density;
		cancelBtn.setMinimumWidth((int) (48 * scale));
		cancelBtn.setMinimumHeight((int) (20 * scale));
		cancelBtn.setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				/* TODO try {
					proxy.set(getProxy().getTiContext().getScope(), "value", "");
				} catch (NoSuchFieldException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				proxy.setProperty(TiC.PROPERTY_VALUE, "", true);
				proxy.fireEvent("cancel", null);
			}
		});
		((FocusFixedEditText)getNativeView()).setRightView(cancelBtn);

	}
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (this.searchChangeListener != null) {
			this.searchChangeListener.filterBy(s.toString());
		}
		super.onTextChanged(s, start, before, count);
	}

	@Override
	public void processProperties(KrollDict d)
	{
		super.processProperties(d);

		if (d.containsKey("showCancel")) {
			boolean showCancel = TiConvert.toBoolean(d, "showCancel");
			cancelBtn.setVisibility(showCancel ? View.VISIBLE : View.GONE);
		} else if (d.containsKey("barColor")) {
			nativeView.setBackgroundColor(TiConvert.toColor(d, "barColor"));
		}
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (key.equals("showCancel")) {
			boolean showCancel = TiConvert.toBoolean(newValue);
			cancelBtn.setVisibility(showCancel ? View.VISIBLE : View.GONE);
		} else if (key.equals("barColor")) {
			nativeView.setBackgroundColor(TiConvert.toColor(TiConvert.toString(newValue)));
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}
	
	public void setOnSearchChangeListener(OnSearchChangeListener listener) {
		this.searchChangeListener = listener;
	}
}
