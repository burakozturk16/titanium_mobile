/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiTouchDelegate;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.ViewProxy;
import ti.modules.titanium.ui.widget.listview.TiListView.TiBaseAdapter;
import ti.modules.titanium.ui.widget.listview.TiListViewTemplate.DataItem;
import android.annotation.SuppressLint;
import android.os.Message;
import android.view.View;

@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = {})
@SuppressWarnings({ "unchecked", "rawtypes" })
@SuppressLint("DefaultLocale")
public class ListSectionProxy extends ViewProxy {

	private static final String TAG = "ListSectionProxy";
	private ArrayList<ListItemData> listItemData;
	private int itemCount;
	private TiBaseAdapter adapter;
	private ArrayList<Object> itemProperties;
	private ArrayList<Integer> filterIndices;
	private boolean preload;
	private ArrayList<Boolean> hiddenItems;
	boolean hidden = false;

	private String headerTitle;
	private String footerTitle;

	private Object headerView;
	private Object footerView;

	private WeakReference<TiListView> listView;

	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;

	private static final int MSG_SET_ITEMS = MSG_FIRST_ID + 700;
	private static final int MSG_APPEND_ITEMS = MSG_FIRST_ID + 701;
	private static final int MSG_INSERT_ITEMS_AT = MSG_FIRST_ID + 702;
	private static final int MSG_DELETE_ITEMS_AT = MSG_FIRST_ID + 703;
	private static final int MSG_GET_ITEM_AT = MSG_FIRST_ID + 704;
	private static final int MSG_REPLACE_ITEMS_AT = MSG_FIRST_ID + 705;
	private static final int MSG_UPDATE_ITEM_AT = MSG_FIRST_ID + 706;
	private static final int MSG_GET_ITEMS = MSG_FIRST_ID + 707;

	private static HashMap<String, String> toPassProps;

	public class ListItemData {
		private KrollDict properties;
		private String searchableText;
		private String template = null;
		private boolean visible = true;

		public ListItemData(KrollDict properties) {
			this.properties = properties;
			if (properties.containsKey(TiC.PROPERTY_TEMPLATE)) {
				this.template = properties.getString(TiC.PROPERTY_TEMPLATE);
			}
			else {
				this.template = getListView().getDefaultTemplateBinding();
			}
			// set searchableText
			if (properties.containsKey(TiC.PROPERTY_PROPERTIES)) {
				Object props = properties.get(TiC.PROPERTY_PROPERTIES);
				if (props instanceof HashMap) {
					HashMap<String, Object> propsHash = (HashMap<String, Object>) props;
					if (propsHash.containsKey(TiC.PROPERTY_SEARCHABLE_TEXT)) {
						searchableText = TiConvert.toString(propsHash,
								TiC.PROPERTY_SEARCHABLE_TEXT);
					}
					if (propsHash.containsKey(TiC.PROPERTY_VISIBLE)) {
						visible = TiConvert.toBoolean(propsHash,
								TiC.PROPERTY_VISIBLE, true);
					}
				}
			}

		}

		public KrollDict getProperties() {
			return properties;
		}

		public String getSearchableText() {
			return searchableText;
		}
		

		public boolean isVisible() {
			return visible;
		}


		public String getTemplate() {
			return template;
		}
	}

	public ListSectionProxy() {
		// initialize variables
		if (toPassProps == null) {
			toPassProps = new HashMap<String, String>();
			toPassProps.put(TiC.PROPERTY_ACCESSORY_TYPE,
					TiC.PROPERTY_ACCESSORY_TYPE);
			toPassProps.put(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR,
					TiC.PROPERTY_BACKGROUND_SELECTED_COLOR);
			toPassProps.put(TiC.PROPERTY_SELECTED_BACKGROUND_IMAGE,
					TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE);
			toPassProps.put(TiC.PROPERTY_SELECTED_BACKGROUND_GRADIENT,
					TiC.PROPERTY_BACKGROUND_SELECTED_GRADIENT);
			toPassProps.put(TiC.PROPERTY_ROW_HEIGHT, TiC.PROPERTY_HEIGHT);
			toPassProps.put(TiC.PROPERTY_MIN_ROW_HEIGHT, TiC.PROPERTY_MIN_HEIGHT);
			toPassProps.put(TiC.PROPERTY_MAX_ROW_HEIGHT, TiC.PROPERTY_MAX_HEIGHT);
		}
		listItemData = new ArrayList<ListItemData>();
		filterIndices = new ArrayList<Integer>();
		hiddenItems = new ArrayList<Boolean>();
		itemCount = 0;
		preload = false;
	}

	public void handleCreationDict(KrollDict dict) {
		// getting header/footer titles from creation dictionary
		if (dict.containsKey(TiC.PROPERTY_HEADER_TITLE)) {
			headerTitle = TiConvert.toString(dict, TiC.PROPERTY_HEADER_TITLE);
		}
		if (dict.containsKey(TiC.PROPERTY_FOOTER_TITLE)) {
			footerTitle = TiConvert.toString(dict, TiC.PROPERTY_FOOTER_TITLE);
		}
		if (dict.containsKey(TiC.PROPERTY_HEADER_VIEW)) {
			headerView = dict.get(TiC.PROPERTY_HEADER_VIEW);
		}
		if (dict.containsKey(TiC.PROPERTY_FOOTER_VIEW)) {
			footerView = dict.get(TiC.PROPERTY_FOOTER_VIEW);
		}
		if (dict.containsKey(TiC.PROPERTY_ITEMS)) {
			handleSetItems(dict.get(TiC.PROPERTY_ITEMS));
		}
		if (dict.containsKey(TiC.PROPERTY_VISIBLE)) {
			setVisible(dict.optBoolean(TiC.PROPERTY_VISIBLE, true));
		}
	}

	public void setAdapter(TiBaseAdapter a) {
		adapter = a;
	}

	@Kroll.method
	@Kroll.setProperty
	public void setHeaderView(TiViewProxy headerView) {
		this.headerView = headerView;
		notifyDataChange();
	}

	@Kroll.method
	@Kroll.getProperty
	public Object getHeaderView() {
		return headerView;
	}

	@Kroll.method
	@Kroll.setProperty
	public void setFooterView(TiViewProxy footerView) {
		this.footerView = footerView;
		notifyDataChange();
	}

	@Kroll.method
	@Kroll.getProperty
	public Object getFooterView() {
		return footerView;
	}

	@Kroll.method
	@Kroll.setProperty
	public void setHeaderTitle(String headerTitle) {
		this.headerTitle = headerTitle;
		notifyDataChange();
	}

	@Kroll.method
	@Kroll.getProperty
	public String getHeaderTitle() {
		return headerTitle;
	}

	@Kroll.method
	@Kroll.setProperty
	public void setFooterTitle(String headerTitle) {
		this.footerTitle = headerTitle;
		notifyDataChange();
	}

	@Kroll.method
	@Kroll.getProperty
	public String getFooterTitle() {
		return footerTitle;
	}

	public void notifyDataChange() {
		if (adapter == null) return;
		getMainHandler().post(new Runnable() {
			@Override
			public void run() {
				adapter.notifyDataSetChanged();
			}
		});
	}

	public String getHeaderOrFooterTitle(int index) {
		if (isHeaderTitle(index)) {
			return headerTitle;
		} else if (isFooterTitle(index)) {
			return footerTitle;
		}
		return "";
	}

	public View getHeaderOrFooterView(int index) {
		if (isHeaderView(index)) {
			return getListView().layoutHeaderOrFooterView(headerView, this);
		} else if (isFooterView(index)) {
			return getListView().layoutHeaderOrFooterView(footerView, this);
		}
		return null;
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {

		case MSG_SET_ITEMS: {
			AsyncResult result = (AsyncResult) msg.obj;
			handleSetItems(result.getArg());
			result.setResult(null);
			return true;
		}

		case MSG_GET_ITEMS: {
			AsyncResult result = (AsyncResult) msg.obj;
			result.setResult(itemProperties.toArray());
			return true;
		}

		case MSG_APPEND_ITEMS: {
			AsyncResult result = (AsyncResult) msg.obj;
			handleAppendItems(result.getArg());
			result.setResult(null);
			return true;
		}

		case MSG_INSERT_ITEMS_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			handleInsertItemsAt(index, data.get(TiC.PROPERTY_DATA));
			result.setResult(null);
			return true;
		}

		case MSG_DELETE_ITEMS_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			int count = data.getInt(TiC.PROPERTY_COUNT);
			handleDeleteItemsAt(index, count);
			result.setResult(null);
			return true;
		}

		case MSG_REPLACE_ITEMS_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			int count = data.getInt(TiC.PROPERTY_COUNT);
			handleReplaceItemsAt(index, count, data.get(TiC.PROPERTY_DATA));
			result.setResult(null);
			return true;
		}

		case MSG_GET_ITEM_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict item = handleGetItemAt(TiConvert.toInt(result.getArg()));
			result.setResult(item);
			return true;
		}

		case MSG_UPDATE_ITEM_AT: {
			AsyncResult result = (AsyncResult) msg.obj;
			KrollDict data = (KrollDict) result.getArg();
			int index = data.getInt(TiC.EVENT_PROPERTY_INDEX);
			handleUpdateItemAt(index, data.get(TiC.PROPERTY_DATA));
			result.setResult(null);
			return true;
		}
		default: {
			return super.handleMessage(msg);
		}

		}
	}

	@Kroll.method
	public KrollDict getItemAt(int index) {
		if (TiApplication.isUIThread()) {
			return handleGetItemAt(index);
		} else {
			return (KrollDict) TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_GET_ITEM_AT), index);
		}
	}

	private KrollDict handleGetItemAt(int index) {
		if (itemProperties != null && index >= 0
				&& index < itemProperties.size()) {
			return new KrollDict((HashMap) itemProperties.get(index));
		}
		return null;
	}

	private int getRealPosition(int position) {
		int hElements = getHiddenCountUpTo(position);
		int diff = 0;
		for (int i = 0; i < hElements; i++) {
			diff++;
			if (hiddenItems.get(position + diff))
				i--;
		}
		return (position + diff);
	}
	
	private int getInverseRealPosition(int position) {
		int hElements = getHiddenCountUpTo(position);
		int diff = 0;
		for (int i = 0; i < hElements; i++) {
			diff++;
			if (hiddenItems.get(position + diff))
				i--;
		}
		return (position - diff);
	}


	private int getHiddenCountUpTo(int location) {
		int count = 0;
		for (int i = 0; i <= location; i++) {
			if (hiddenItems.get(i))
				count++;
		}
		return count;
	}

	@Kroll.method
	@Kroll.setProperty
	public void setItems(Object data) {
		if (TiApplication.isUIThread()) {
			handleSetItems(data);
		} else {
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_SET_ITEMS), data);
		}
	}

	@Kroll.method
	@Kroll.getProperty
	public Object[] getItems() {
		if (itemProperties == null) {
			return new Object[0];
		} else if (TiApplication.isUIThread()) {
			return itemProperties.toArray();
		} else {
			return (Object[]) TiMessenger
					.sendBlockingMainMessage(getMainHandler().obtainMessage(
							MSG_GET_ITEMS));
		}
	}

	@Kroll.method
	public void appendItems(Object data) {
		if (TiApplication.isUIThread()) {
			handleAppendItems(data);
		} else {
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_APPEND_ITEMS), data);
		}
	}

	public boolean isIndexValid(int index) {
		return (index >= 0) ? true : false;
	}

	@Kroll.method
	public void insertItemsAt(int index, Object data) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleInsertItemsAt(index, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.PROPERTY_DATA, data);
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_INSERT_ITEMS_AT), d);
		}
	}

	@Kroll.method
	public void deleteItemsAt(int index, int count) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleDeleteItemsAt(index, count);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_COUNT, count);
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_DELETE_ITEMS_AT), d);
		}
	}

	@Kroll.method
	public void replaceItemsAt(int index, int count, Object data) {
		if (!isIndexValid(index)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleReplaceItemsAt(index, count, data);
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_COUNT, count);
			d.put(TiC.PROPERTY_DATA, data);
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_REPLACE_ITEMS_AT), d);
		}
	}

	@Kroll.method
	public void updateItemAt(int index, Object data) {
		if (!isIndexValid(index) || !(data instanceof HashMap)) {
			return;
		}

		if (TiApplication.isUIThread()) {
			handleUpdateItemAt(index, new Object[] { data });
		} else {
			KrollDict d = new KrollDict();
			d.put(TiC.EVENT_PROPERTY_INDEX, index);
			d.put(TiC.PROPERTY_DATA, new Object[] { data });
			TiMessenger.sendBlockingMainMessage(
					getMainHandler().obtainMessage(MSG_UPDATE_ITEM_AT), d);
		}
	}
	
	@Kroll.method
	public void hide() {
		if (hidden) return;
		notifyDataChange();
		hidden = true;
	}
	
	@Kroll.method
	public void show() {
		if (!hidden) return;
		notifyDataChange();
		hidden = false;
	}
	
	@Kroll.method
	@Kroll.setProperty
	public void setVisible(boolean value) {
		if (hidden == !value) return;
		notifyDataChange();
		hidden = !value;
	}
	
	@Kroll.method
	@Kroll.getProperty
	public boolean getVisible() {
		return !hidden;
	}

	
	public void processPreloadData() {
		if (itemProperties != null && preload) {
			handleSetItems(itemProperties.toArray());
			preload = false;
		}
	}

	public void refreshItems() {
		handleSetItems(itemProperties.toArray());
	}

	private void processData(Object[] items, int offset) {
		if (listItemData == null) {
			return;
		}

		// Second pass we would merge properties
		for (int i = 0; i < items.length; i++) {
			Object itemData = items[i];
			if (itemData instanceof HashMap) {
				KrollDict d = new KrollDict((HashMap) itemData);
				ListItemData itemD = new ListItemData(d);
				listItemData.add(i + offset, itemD);
				hiddenItems.add(i + offset, !itemD.isVisible());
			}
		}
		// Notify adapter that data has changed.
		adapter.notifyDataSetChanged();
	}

	private void handleSetItems(Object data) {

		if (data instanceof Object[]) {
			Object[] items = (Object[]) data;
			itemProperties = new ArrayList<Object>(Arrays.asList(items));
			listItemData.clear();
			hiddenItems.clear();
			// only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			itemCount = items.length;
			processData(items, 0);

		} else {
			Log.e(TAG, "Invalid argument type to setData", Log.DEBUG_MODE);
		}
	}

	private void handleAppendItems(Object data) {
		if (data instanceof Object[]) {
			Object[] views = (Object[]) data;
			if (itemProperties == null) {
				itemProperties = new ArrayList<Object>(Arrays.asList(views));
			} else {
				for (Object view : views) {
					itemProperties.add(view);
				}
			}
			// only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}
			// we must update the itemCount before notify data change. If we
			// don't, it will crash
			int count = itemCount;
			itemCount += views.length;

			processData(views, count);

		} else {
			Log.e(TAG, "Invalid argument type to setData", Log.DEBUG_MODE);
		}
	}

	private void handleInsertItemsAt(int index, Object data) {
		if (data instanceof Object[]) {
			Object[] views = (Object[]) data;

			if (itemProperties == null) {
				itemProperties = new ArrayList<Object>(Arrays.asList(views));
			} else {
				if (index < 0 || index > itemProperties.size()) {
					Log.e(TAG, "Invalid index to handleInsertItem",
							Log.DEBUG_MODE);
					return;
				}
				int counter = index;
				for (Object view : views) {
					itemProperties.add(counter, view);
					counter++;
				}
			}
			// only process items when listview's properties is processed.
			if (getListView() == null) {
				preload = true;
				return;
			}

			itemCount += views.length;
			processData(views, index);
		} else {
			Log.e(TAG, "Invalid argument type to insertItemsAt", Log.DEBUG_MODE);
		}
	}

	private boolean deleteItems(int index, int count) {
		boolean delete = false;
		while (count > 0) {
			if (index < itemProperties.size()) {
				itemProperties.remove(index);
				itemCount--;
				delete = true;
			}
			if (index < listItemData.size()) {
				listItemData.remove(index);
			}
			if (index < hiddenItems.size()) {
				hiddenItems.remove(index);
			}
			count--;
		}
		return delete;
	}

	private void handleDeleteItemsAt(int index, int count) {
		deleteItems(index, count);
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	private void handleReplaceItemsAt(int index, int count, Object data) {
		if (count == 0) {
			handleInsertItemsAt(index, data);
		} else if (deleteItems(index, count)) {
			handleInsertItemsAt(index, data);
		}
	}

	private void handleUpdateItemAt(int index, Object data) {
		handleReplaceItemsAt(index, 1, data);
		setProperty(TiC.PROPERTY_ITEMS, itemProperties.toArray());
	}

	/**
	 * This method creates a new cell and fill it with content. getView() calls
	 * this method when a view needs to be created.
	 * 
	 * @param index
	 *            Entry's index relative to its section
	 * @return
	 */
	public void generateCellContent(int sectionIndex, KrollDict data,
			ListItemProxy itemProxy, TiBaseListViewItem itemContent, TiListViewTemplate template,
			int itemPosition, View item_layout) {
		// Create corresponding TiUIView for item proxy
		TiListItem item = new TiListItem(itemProxy, itemContent, item_layout);
		itemProxy.setView(item);
		itemContent.setView(item);
		itemProxy.realizeViews();

		if (data != null && template != null) {
			populateViews(data, itemContent, template, itemPosition,
					sectionIndex, item_layout, false);
		}
	}

	public void appendExtraEventData(TiUIView view, int itemIndex,
			int sectionIndex, String bindId, String itemId) {
		KrollDict existingData = view.getAdditionalEventData();
		if (existingData == null) {
			existingData = new KrollDict();
			view.setAdditionalEventData(existingData);
		}

		// itemIndex = realItemIndex + header (if exists). We want the real item
		// index.
		if (headerTitle != null || headerView != null) {
			itemIndex -= 1;
		}

		existingData.put(TiC.PROPERTY_SECTION, this);
		existingData.put(TiC.PROPERTY_SECTION_INDEX, sectionIndex);
		existingData.put(TiC.PROPERTY_ITEM_INDEX, getRealPosition(itemIndex));

		if (bindId != null && !bindId.equals(TiC.PROPERTY_PROPERTIES)) {
			existingData.put(TiC.PROPERTY_BIND_ID, bindId);
		} else if (existingData.containsKey(TiC.PROPERTY_BIND_ID)) {
			existingData.remove(TiC.PROPERTY_BIND_ID);
		}

		if (itemId != null) {
			existingData.put(TiC.PROPERTY_ITEM_ID, itemId);
		} else if (existingData.containsKey(TiC.PROPERTY_ITEM_ID)) {
			existingData.remove(TiC.PROPERTY_ITEM_ID);
		}

	}

	public void populateViews(KrollDict data, TiBaseListViewItem cellContent, TiListViewTemplate template, int itemIndex, int sectionIndex,
			View item_layout, boolean reusing) {
		TiListItem listItem = (TiListItem)cellContent.getView();
		// Handling root item, since that is not in the views map.
		if (listItem == null) {
			return;
		}
		cellContent.setCurrentItem(sectionIndex, itemIndex);
		
		data = template.prepareDataDict(data);
		ListItemProxy itemProxy = (ListItemProxy) cellContent.getView().getProxy();

		KrollDict listItemProperties;
//		KrollDict templateProperties = template.getProperties();
		KrollDict listViewProperties = getListView().getProxy().getProperties();
		String itemId = null;

		if (data.containsKey(TiC.PROPERTY_PROPERTIES)) {
			listItemProperties = new KrollDict(
					(HashMap) data.get(TiC.PROPERTY_PROPERTIES));
		} else {
			listItemProperties = new KrollDict();
		}
		ViewItem rootItem = itemProxy.getViewItem();
		
		for (Map.Entry<String, String> entry : toPassProps.entrySet()) {
			String inProp = entry.getKey();
			String outProp = entry.getValue();
			if (!listItemProperties.containsKey(outProp) && !rootItem.containsKey(outProp) && listViewProperties.containsKey(inProp)) {
				listItemProperties.put(outProp, listViewProperties.get(inProp));
			}
		}

		// find out if we need to update itemId
		if (listItemProperties.containsKey(TiC.PROPERTY_ITEM_ID)) {
			itemId = TiConvert.toString(listItemProperties
					.get(TiC.PROPERTY_ITEM_ID));
		}

		// update extra event data for list item
		appendExtraEventData(listItem, itemIndex, sectionIndex,
				TiC.PROPERTY_PROPERTIES, itemId);

		HashMap<String, ViewItem> views = itemProxy.getViewsMap();
		// Loop through all our views and apply default properties
		for (String binding : views.keySet()) {
			DataItem dataItem = template.getDataItem(binding);
			ViewItem viewItem = views.get(binding);
			TiUIView view = viewItem.getViewProxy().peekView();
			if (view == null)
				continue;
			view.setTouchDelegate((TiTouchDelegate) listItem);
			// update extra event data for views
			appendExtraEventData(view, itemIndex, sectionIndex, binding, itemId);
			// if binding is contain in data given to us, process that data,
			// otherwise
			// apply default properties.
			if (reusing) {
				view.setReusing(true);
			}
			if (data.containsKey(binding) && view != null) {
				KrollDict properties = new KrollDict(
						(HashMap) data.get(binding));
				KrollDict diffProperties = viewItem
						.generateDiffProperties(properties);
				if (!diffProperties.isEmpty()) {
					view.processProperties(diffProperties);
				}

			} else if (dataItem != null && view != null) {
				KrollDict diffProperties = viewItem
						.generateDiffProperties(null);
				if (!diffProperties.isEmpty()) {
					view.processProperties(diffProperties);
				}
			} else {
				Log.w(TAG, "Sorry, " + binding
						+ " isn't a valid binding. Perhaps you made a typo?",
						Log.DEBUG_MODE);
			}
			if (reusing) {
				view.setReusing(false);
			}
		}
		
		for (TiViewProxy viewProxy : itemProxy.getNonBindedViews()) {
			TiUIView view = viewProxy.peekView();
			view.setTouchDelegate((TiTouchDelegate) listItem);
			appendExtraEventData(view, itemIndex, sectionIndex, null, itemId);
		}

		// process listItem properties
		KrollDict listItemDiff = itemProxy.getViewItem()
				.generateDiffProperties(listItemProperties);
		if (!listItemDiff.isEmpty()) {
			listItem.processProperties(listItemDiff);
		}

	}

	public String getTemplateByIndex(int index) {
		if (headerTitle != null || headerView != null) {
			index -= 1;
		}

		if (isFilterOn()) {
			return getItemDataAt(filterIndices.get(index)).getTemplate();
		} else {
			return getItemDataAt(index).getTemplate();
		}
	}

	public int getContentCount() {
		int totalCount = 0;
		if (isFilterOn()) {
			totalCount = filterIndices.size();
		} else if (!hidden) {
			totalCount = itemCount;
		}
		return totalCount - getHiddenCount();
	}

	/**
	 * @return number of entries within section
	 */
	public int getItemCount() {
		int totalCount = 0;

		if (isFilterOn()) {
			totalCount = filterIndices.size();
		} else if (!hidden) {
			totalCount = itemCount;
		}

		if (!hideHeaderOrFooter()) {
			if (headerTitle != null || headerView != null) {
				totalCount += 1;
			}
			if (footerTitle != null || footerView != null) {
				totalCount += 1;
			}
		}
		return totalCount - getHiddenCount();
	}

	private int getHiddenCount() {
		int count = 0;
		if (hidden || hiddenItems == null) return count;
		for (int i = 0; i < hiddenItems.size(); i++)
			if (hiddenItems.get(i) == true)
				count++;
		return count;
	}

	private boolean hideHeaderOrFooter() {
		TiListView listview = getListView();
		return (listview.getSearchText() != null && filterIndices.isEmpty());
	}

	public boolean isHeaderView(int pos) {
		return (headerView != null && pos == 0);
	}

	public boolean isFooterView(int pos) {
		return (footerView != null && pos == getItemCount() - 1);
	}

	public boolean isHeaderTitle(int pos) {
		return (headerTitle != null && pos == 0);
	}

	public boolean isFooterTitle(int pos) {
		return (footerTitle != null && pos == getItemCount() - 1);
	}

	public void setListView(TiListView l) {
		listView = new WeakReference<TiListView>(l);
	}

	public TiListView getListView() {
		if (listView != null) {
			return listView.get();
		}
		return null;
	}

	private ListItemData getItemDataAt(int position)
	{
		return listItemData.get(getRealPosition(position));
	}

	public KrollDict getListItemData(int position) {
		if (headerTitle != null || headerView != null) {
			position -= 1;
		}

		if (isFilterOn()) {
			return getItemDataAt(filterIndices.get(position))
					.getProperties();
		} else if (position >= 0 && position < getItemCount()) {
			return getItemDataAt(position).getProperties();
		}
		return null;
	}

	public ListItemData getListItem(int position) {
		if (headerTitle != null || headerView != null) {
			position -= 1;
		}

		if (isFilterOn()) {
			return getItemDataAt(filterIndices.get(position));
		} else if (position >= 0 && position < getItemCount()) {
			return getItemDataAt(position);
		}
		return null;
	}

	public boolean isFilterOn() {
		if (getListView().getSearchText() != null) {
			return true;
		}
		return false;
	}

	public void applyFilter(String searchText) {
		// Clear previous result
		filterIndices.clear();
		boolean caseInsensitive = getListView().getCaseInsensitive();
		// Add new results
		for (int i = 0; i < listItemData.size(); ++i) {
			ListItemData data = listItemData.get(i);
			String searchableText = data.getSearchableText();
			// Handle case sensitivity
			if (caseInsensitive) {
				searchText = searchText.toLowerCase();
				searchableText = searchableText.toLowerCase();
			}
			// String comparison
			if (data.isVisible() && searchableText != null && searchableText.contains(searchText)) {
				filterIndices.add(getInverseRealPosition(i));
			}
		}
	}

	public void release() {
		if (listItemData != null) {
			listItemData.clear();
			listItemData = null;
		}
		
		if (hiddenItems != null) {
			hiddenItems.clear();
			hiddenItems = null;
		}

		if (itemProperties != null) {
			itemProperties.clear();
			itemProperties = null;
		}

		super.release();
	}

	public void releaseViews() {
		listView = null;
	}

	@Override
	public String getApiName() {
		return "Ti.UI.ListSection";
	}
}
