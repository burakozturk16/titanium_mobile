/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.searchbar;

import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;

import ti.modules.titanium.ui.widget.TiUIText;
import android.text.TextUtils.TruncateAt;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public class TiUISearchBar extends TiUIText
{
	protected ImageButton cancelBtn;
	private TiEditText tv;
	private FocusFixedEditText fullTv;
	private TextView promptText;
	
	public interface OnSearchChangeListener {
		public void filterBy(String text);
	}
	
	protected OnSearchChangeListener searchChangeListener;
	
	public TiUISearchBar(final TiViewProxy proxy)
	{
		super(proxy, true);
		fullTv = (FocusFixedEditText)getNativeView();
		tv = (TiEditText) fullTv.getRealEditText();
		tv.setImeOptions(EditorInfo.IME_ACTION_DONE);
		promptText = new TextView(proxy.getActivity());
		promptText.setEllipsize(TruncateAt.END);
		promptText.setSingleLine(true);

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
				tv.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
				fireEvent(TiC.EVENT_CANCEL, null, false, false);
			}
		});
		fullTv.setRightView(cancelBtn);
		RelativeLayout layout = new RelativeLayout(proxy.getActivity())
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUISearchBar.this);
			}
		};

		layout.setGravity(Gravity.NO_GRAVITY);
		layout.setPadding(0,0,0,0);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
			LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		promptText.setGravity(Gravity.CENTER_HORIZONTAL);
		layout.addView(promptText, params);

		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		layout.addView(fullTv, params);

		setNativeView(layout);
	}
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		if (this.searchChangeListener != null) {
			this.searchChangeListener.filterBy(s.toString());
		}
		super.onTextChanged(s, start, before, count);
	}
	
	@Override
	public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_SHOW_CANCEL:
            boolean showCancel = TiConvert.toBoolean(newValue, false);
            if (showCancel)
                fullTv.showRightView();
            else
                fullTv.hideRightView();
            break;
        case TiC.PROPERTY_BAR_COLOR:
            nativeView.setBackgroundColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_PROMPT:
            promptText.setText(TiConvert.toString(newValue));
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
	
    @Override
    public void release()
    {
        searchChangeListener = null;
        super.release();
    }

	public void setOnSearchChangeListener(OnSearchChangeListener listener) {
		this.searchChangeListener = listener;
	}
}
