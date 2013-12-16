/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiTouchDelegate;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.TiUIButton;
import ti.modules.titanium.ui.widget.TiUIProgressBar;
import ti.modules.titanium.ui.widget.TiUISlider;
import ti.modules.titanium.ui.widget.TiUISwitch;
import ti.modules.titanium.ui.widget.TiUIText;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class TiListItem extends TiUIView implements TiTouchDelegate {
	TiUIView mClickDelegate;
	View listItemLayout;
	public TiListItem(TiViewProxy proxy) {
		super(proxy);
	}

	public TiListItem(TiViewProxy proxy, View v, View item_layout) {
		super(proxy);
//		layoutParams = p;
		layoutParams.autoFillsHeight = true;
		layoutParams.autoFillsWidth = true;
		listItemLayout = item_layout;
		setNativeView(v);
		registerForTouch(v);
		v.setFocusable(false);
	}
	
	public void processProperties(KrollDict d) {

		if (d.containsKey(TiC.PROPERTY_ACCESSORY_TYPE)) {
			int accessory = TiConvert.toInt(d.get(TiC.PROPERTY_ACCESSORY_TYPE), -1);
			handleAccessory(accessory);
		}
		if (d.containsKey(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR)) {
			d.put(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR, d.get(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR));
		}
		if (d.containsKey(TiC.PROPERTY_SELECTED_BACKGROUND_IMAGE)) {
			d.put(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE, d.get(TiC.PROPERTY_SELECTED_BACKGROUND_IMAGE));
		}
		super.processProperties(d);
	}

	private void handleAccessory(int accessory) {
		
		ImageView accessoryImage = (ImageView) listItemLayout.findViewById(TiListView.accessory);

		switch(accessory) {

			case UIModule.LIST_ACCESSORY_TYPE_CHECKMARK:
				accessoryImage.setImageResource(TiListView.isCheck);
				break;
			case UIModule.LIST_ACCESSORY_TYPE_DETAIL:
				accessoryImage.setImageResource(TiListView.hasChild);
				break;

			case UIModule.LIST_ACCESSORY_TYPE_DISCLOSURE:
				accessoryImage.setImageResource(TiListView.disclosure);
				break;
	
			default:
				accessoryImage.setImageResource(0);
		}
	}
	
	protected void setOnClickListener(View view)
	{
		view.setOnClickListener(new OnClickListener()
		{
			public void onClick(View view)
			{
				KrollDict data = dictFromEvent(lastUpEvent);
				handleFireItemClick(new KrollDict(data));
				fireEvent(TiC.EVENT_CLICK, data);
			}
		});
	}
	
	protected void handleFireItemClick (KrollDict data) {
		TiViewProxy listViewProxy = ((ListItemProxy)proxy).getListProxy();
		if (listViewProxy != null && listViewProxy.hasListeners(TiC.EVENT_ITEM_CLICK)) {
			TiUIView listView = listViewProxy.peekView();
			if (listView != null) {
				KrollDict d = listView.getAdditionalEventData();
				if (d == null) {
					listView.setAdditionalEventData(new KrollDict((HashMap) additionalEventData));
				} else {
					d.clear();
					d.putAll(additionalEventData);
				}
				if (mClickDelegate == null) {
					listView.fireEvent(TiC.EVENT_ITEM_CLICK, data, false, false);
				}
			}
		}
	}
	
	public void release() {
		if (listItemLayout != null) {
			listItemLayout = null;
		}
		super.release();
	}
	@Override
	protected void handleTouchEvent(MotionEvent event) {
		mClickDelegate = null;
	}

	@Override
	public void onTouchEvent(MotionEvent event, TiUIView fromView) {
		if (fromView instanceof TiUIButton || 
				fromView instanceof TiUISwitch ||
				fromView instanceof TiUISlider ||
				fromView instanceof TiUIText) return;
		mClickDelegate = fromView;
		if (nativeView != null) {
			nativeView.onTouchEvent(event);
		}
//		mClickDelegate = null;
	}
	
}
