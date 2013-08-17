/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiColorHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.tableview.TiBaseTableViewItem;
import ti.modules.titanium.ui.widget.tableview.TiTableView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

@SuppressLint("NewApi")
public class TiListView extends TiUIView {

	private ListView listView;
	private TiBaseAdapter adapter;
	private ArrayList<ListSectionProxy> sections;
	private AtomicInteger itemTypeCount;
	private String defaultTemplateBinding;
	private HashMap<String, TiListViewTemplate> templatesByBinding;
	private int listItemId;
	public static int listContentId;
	public static int isCheck;
	public static int hasChild;
	public static int disclosure;
	public static int accessory;
	private int headerFooterId;
	public static LayoutInflater inflater;
	private int titleId;
	private View headerView;
	private View footerView;
	private static final String TAG = "TiListView";
	
	/* We cache properties that already applied to the recycled list tiem in ViewItem.java
	 * However, since Android randomly selects a cached view to recycle, our cached properties
	 * will not be in sync with the native view's properties when user changes those values via
	 * User Interaction - i.e click. For this reason, we create a list that contains the properties 
	 * that must be reset every time a view is recycled, to ensure synchronization. Currently, only
	 * "value" is in this list to correctly update the value of Ti.UI.Switch.
	 */
	public static List<String> MUST_SET_PROPERTIES = Arrays.asList(TiC.PROPERTY_VALUE);
	
	public static final String MIN_ROW_HEIGHT = "30dp";
	public static final int HEADER_FOOTER_ITEM_TYPE = 0;
	public static final int BUILT_IN_TEMPLATE_ITEM_TYPE = 1;
	
	class ListViewWrapper extends FrameLayout {

		public ListViewWrapper(Context context) {
			super(context);
		}
		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			// To prevent undesired "focus" and "blur" events during layout caused
			// by ListView temporarily taking focus, we will disable focus events until
			// layout has finished.
			// First check for a quick exit. listView can be null, such as if window closing.
			if (listView == null) {
				super.onLayout(changed, left, top, right, bottom);
				return;
			}
			OnFocusChangeListener focusListener = null;
			View focusedView = listView.findFocus();
			if (focusedView != null) {
				OnFocusChangeListener listener = focusedView.getOnFocusChangeListener();
				if (listener != null && listener instanceof TiUIView) {
					focusedView.setOnFocusChangeListener(null);
					focusListener = listener;
				}
			}
			
			//We are temporarily going to block focus to descendants 
			//because LinearLayout on layout will try to find a focusable descendant
			if (focusedView != null) {
				listView.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			}
			super.onLayout(changed, left, top, right, bottom);
			//Now we reset the descendant focusability
			listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

			TiViewProxy viewProxy = proxy;
			if (viewProxy != null && viewProxy.hasListeners(TiC.EVENT_POST_LAYOUT)) {
				viewProxy.fireEvent(TiC.EVENT_POST_LAYOUT, null);
			}

			// Layout is finished, re-enable focus events.
			if (focusListener != null) {
				// If the configuration changed, we manually fire the blur event
				if (changed) {
					focusedView.setOnFocusChangeListener(focusListener);
					focusListener.onFocusChange(focusedView, false);
				} else {
					//Ok right now focus is with listView. So set it back to the focusedView
					focusedView.requestFocus();
					focusedView.setOnFocusChangeListener(focusListener);
				}
			}
		}
	}
	
	public class TiBaseAdapter extends BaseAdapter {

		Activity context;
		
		public TiBaseAdapter(Activity activity) {
			context = activity;
		}

		@Override
		public int getCount() {
			int count = 0;
			for (int i = 0; i < sections.size(); i++) {
				ListSectionProxy section = sections.get(i);
				count += section.getItemCount();
			}
			return count;
		}

		@Override
		public Object getItem(int arg0) {
			//not using this method
			return arg0;
		}

		@Override
		public long getItemId(int position) {
			//not using this method
			return position;
		}
		
		//One type for header/footer, One type for built-in template, and one type per custom template.
		@Override
		public int getViewTypeCount() {
			return 2 + templatesByBinding.size();
			
		}
		@Override
		public int getItemViewType(int position) {
			Pair<ListSectionProxy, Integer> info = getSectionInfoByEntryIndex(position);
			ListSectionProxy section = info.first;
			int sectionItemIndex = info.second;
			if (section.isHeaderView(sectionItemIndex) || section.isFooterView(sectionItemIndex))
				return HEADER_FOOTER_ITEM_TYPE;
			return section.getTemplateByIndex(sectionItemIndex).getType();			
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			//Get section info from index
			Pair<ListSectionProxy, Integer> info = getSectionInfoByEntryIndex(position);
			ListSectionProxy section = info.first;
			int sectionItemIndex = info.second;
			View content = convertView;

			//Handling section header/footer titles
			if (section.isHeaderView(sectionItemIndex) || section.isFooterView(sectionItemIndex)) {
				if (content == null) {
					content = inflater.inflate(headerFooterId, null);
				}
				TextView title = (TextView)content.findViewById(titleId);
				title.setText(section.getHeaderOrFooterTitle(sectionItemIndex));
				return content;
			}
			
			//Handling templates
			KrollDict data = section.getListItemData(sectionItemIndex);
			TiListViewTemplate template = section.getTemplateByIndex(sectionItemIndex);
			int sectionIndex = sections.indexOf(section);

			if (content != null) {
				TiBaseListViewItem itemContent = (TiBaseListViewItem) content.findViewById(listContentId);
				section.populateViews(data, itemContent, template, sectionItemIndex, sectionIndex, content);
			} else {
				content = inflater.inflate(listItemId, null);
				TiBaseListViewItem itemContent = (TiBaseListViewItem) content.findViewById(listContentId);
				LayoutParams params = new LayoutParams();
				params.autoFillsWidth = true;
				itemContent.setLayoutParams(params);
				section.generateCellContent(sectionIndex, data, template, itemContent, sectionItemIndex, content);
			}
			return content;

		}

	}

	public TiListView(TiViewProxy proxy, Activity activity) {
		super(proxy);
		
		//initializing variables
		sections = new ArrayList<ListSectionProxy>();
		itemTypeCount = new AtomicInteger(2);
		templatesByBinding = new HashMap<String, TiListViewTemplate>();
		defaultTemplateBinding = UIModule.LIST_ITEM_TEMPLATE_DEFAULT;
		
		//initializing listView and adapter
		ListViewWrapper wrapper = new ListViewWrapper(activity);
		wrapper.setFocusable(false);
		wrapper.setFocusableInTouchMode(false);
		wrapper.setAddStatesFromChildren(true);
		listView = new ListView(activity);
		listView.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		listView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		wrapper.addView(listView);
		adapter = new TiBaseAdapter(activity);
		
		final KrollProxy fProxy = proxy;
		listView.setOnScrollListener(new OnScrollListener()
		{
			private boolean scrollValid = false;
			private int lastValidfirstItem = 0;
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					scrollValid = false;
					if (!fProxy.hasListeners(TiC.EVENT_SCROLLEND)) return;
					KrollDict eventArgs = new KrollDict();
					KrollDict size = new KrollDict();
					size.put("width", TiListView.this.getNativeView().getWidth());
					size.put("height", TiListView.this.getNativeView().getHeight());
					eventArgs.put("size", size);
					KrollDict scrollEndArgs = new KrollDict(eventArgs);
					fProxy.fireEvent(TiC.EVENT_SCROLLEND, eventArgs);
				}
				else if (scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
					scrollValid = true;
				}
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
				boolean fireScroll = scrollValid;
				if (!fireScroll && visibleItemCount > 0) {
					//Items in a list can be selected with a track ball in which case
					//we must check to see if the first visibleItem has changed.
					fireScroll = (lastValidfirstItem != firstVisibleItem);
				}
				if(fireScroll && fProxy.hasListeners(TiC.EVENT_SCROLL)) {
					lastValidfirstItem = firstVisibleItem;
					KrollDict eventArgs = new KrollDict();
					eventArgs.put("firstVisibleItem", firstVisibleItem);
					eventArgs.put("visibleItemCount", visibleItemCount);
					eventArgs.put("totalItemCount", totalItemCount);
					KrollDict size = new KrollDict();
					size.put("width", TiListView.this.getNativeView().getWidth());
					size.put("height", TiListView.this.getNativeView().getHeight());
					eventArgs.put("size", size);
					fProxy.fireEvent(TiC.EVENT_SCROLL, eventArgs);
				}
			}
		});
		
		//init inflater
		if (inflater == null) {
			inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		listView.setCacheColorHint(Color.TRANSPARENT);
		getLayoutParams().autoFillsHeight = true;
		getLayoutParams().autoFillsWidth = true;
		listView.setFocusable(true);
		listView.setFocusableInTouchMode(true);
		listView.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

		try {
			headerFooterId = TiRHelper.getResource("layout.titanium_ui_list_header_or_footer");
			listItemId = TiRHelper.getResource("layout.titanium_ui_list_item");
			titleId = TiRHelper.getResource("id.titanium_ui_list_header_or_footer_title");
			listContentId = TiRHelper.getResource("id.titanium_ui_list_item_content");
			isCheck = TiRHelper.getResource("drawable.btn_check_buttonless_on_64");
			hasChild = TiRHelper.getResource("drawable.btn_more_64");
			disclosure = TiRHelper.getResource("drawable.disclosure_64");
			accessory = TiRHelper.getResource("id.titanium_ui_list_item_accessoryType");
		} catch (ResourceNotFoundException e) {
			Log.e(TAG, "XML resources could not be found!!!", Log.DEBUG_MODE);
		}
		
		
		setNativeView(wrapper);
	}
	
	public void setHeaderTitle(String title) {
		TextView textView = (TextView) headerView.findViewById(titleId);
		textView.setText(title);
		if (textView.getVisibility() == View.GONE) {
			textView.setVisibility(View.VISIBLE);
		}
	}
	
	public void setFooterTitle(String title) {
		TextView textView = (TextView) footerView.findViewById(titleId);
		textView.setText(title);
		if (textView.getVisibility() == View.GONE) {
			textView.setVisibility(View.VISIBLE);
		}
	}

	private TiUIView layoutHeaderOrFooter(TiViewProxy viewProxy)
	{
		//We are always going to create a new view here. So detach outer view here and recreate
		View outerView = (viewProxy.peekView() == null) ? null : viewProxy.peekView().getOuterView();
		if (outerView != null) {
			ViewParent vParent = outerView.getParent();
			if ( vParent instanceof ViewGroup ) {
				((ViewGroup)vParent).removeView(outerView);
			}
		}
		TiUIView tiView = viewProxy.forceCreateView();
		View nativeView = tiView.getOuterView();
		TiCompositeLayout.LayoutParams params = tiView.getLayoutParams();

		int width = AbsListView.LayoutParams.WRAP_CONTENT;
		int height = AbsListView.LayoutParams.WRAP_CONTENT;
		if (params.sizeOrFillHeightEnabled) {
			if (params.autoFillsHeight) {
				height = AbsListView.LayoutParams.MATCH_PARENT;
			}
		} else if (params.optionHeight != null) {
			height = params.optionHeight.getAsPixels(listView);
		}
		if (params.sizeOrFillWidthEnabled) {
			if (params.autoFillsWidth) {
				width = AbsListView.LayoutParams.MATCH_PARENT;
			}
		} else if (params.optionWidth != null) {
			width = params.optionWidth.getAsPixels(listView);
		}
		AbsListView.LayoutParams p = new AbsListView.LayoutParams(width, height);
		nativeView.setLayoutParams(p);
		return tiView;
	}

	public void setSeparatorColor(String colorstring) {
		int sepColor = TiColorHelper.parseColor(colorstring);
		int dividerHeight = listView.getDividerHeight();
		listView.setDivider(new ColorDrawable(sepColor));
		listView.setDividerHeight(dividerHeight);
	}

	public void setSeparatorStyle(int separatorHeight) {
		Drawable drawable = listView.getDivider();
		listView.setDivider(drawable);
		listView.setDividerHeight(separatorHeight);
	}

	@Override
	public void registerForTouch()
	{
		registerForTouch(listView);
	}
	
	public void processProperties(KrollDict d) {
		
		if (d.containsKey(TiC.PROPERTY_TEMPLATES)) {
			Object templates = d.get(TiC.PROPERTY_TEMPLATES);
			if (templates != null) {
				processTemplates(new KrollDict((HashMap)templates));
			}
		} 
		
		if (d.containsKey(TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR)) {
			listView.setVerticalScrollBarEnabled(TiConvert.toBoolean(d, TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR, true));
		}

		if (d.containsKey(TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE)) {
			defaultTemplateBinding = TiConvert.toString(d, TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE);
		}
		
		ListViewProxy listProxy = (ListViewProxy) proxy;
		if (d.containsKey(TiC.PROPERTY_SECTIONS)) {
			//if user didn't append/modify/delete sections before this is called, we process sections
			//as usual. Otherwise, we process the preloadSections, which should also contain the section(s)
			//from this dictionary as well as other sections that user append/insert/deleted prior to this.
			if (!listProxy.isPreload()) {
				processSections((Object[])d.get(TiC.PROPERTY_SECTIONS));
			} else {
				processSections(listProxy.getPreloadSections().toArray());
			}
		} else if (listProxy.isPreload()) {
			//if user didn't specify 'sections' property upon creation of listview but append/insert it afterwards
			//we process them instead.
			processSections(listProxy.getPreloadSections().toArray());
		}

		listProxy.clearPreloadSections();

		if (proxy.hasProperty(TiC.PROPERTY_SEPARATOR_COLOR)) {
			setSeparatorColor(TiConvert.toString(proxy.getProperty(TiC.PROPERTY_SEPARATOR_COLOR)));
		}

		if (proxy.hasProperty(TiC.PROPERTY_SEPARATOR_STYLE)) {
			setSeparatorStyle(TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_SEPARATOR_STYLE)));
		}

		if (proxy.hasProperty(TiC.PROPERTY_OVER_SCROLL_MODE)) {
			if (Build.VERSION.SDK_INT >= 9) {
				listView.setOverScrollMode(TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_OVER_SCROLL_MODE), View.OVER_SCROLL_ALWAYS));
			}
		}

		if (proxy.hasProperty(TiC.PROPERTY_HEADER_VIEW)) {
			TiViewProxy view = (TiViewProxy) proxy.getProperty(TiC.PROPERTY_HEADER_VIEW);
			headerView = layoutHeaderOrFooter(view).getOuterView();
		} else if (d.containsKey(TiC.PROPERTY_HEADER_TITLE)) {
			headerView = inflater.inflate(headerFooterId, null);
			setHeaderTitle(TiConvert.toString(d, TiC.PROPERTY_HEADER_TITLE));
		}

		if (proxy.hasProperty(TiC.PROPERTY_FOOTER_VIEW)) {
			TiViewProxy view = (TiViewProxy) proxy.getProperty(TiC.PROPERTY_FOOTER_VIEW);
			footerView = layoutHeaderOrFooter(view).getOuterView();
		} else if (d.containsKey(TiC.PROPERTY_FOOTER_TITLE)) {
			footerView = inflater.inflate(headerFooterId, null);
			setFooterTitle(TiConvert.toString(d, TiC.PROPERTY_FOOTER_TITLE));
		}

		//Check to see if headerTitle and footerTitle are specified. If not, we hide the views
		if (headerView == null) {
			headerView = inflater.inflate(headerFooterId, null);
			headerView.findViewById(titleId).setVisibility(View.GONE);
		}
		
		if (footerView == null) {
			footerView = inflater.inflate(headerFooterId, null);
			footerView.findViewById(titleId).setVisibility(View.GONE);
		}

		//Have to add header and footer before setting adapter
		listView.addHeaderView(headerView);
		listView.addFooterView(footerView);

		listView.setAdapter(adapter);

		super.processProperties(d);
		
	}
	
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy) {
		if (key.equals(TiC.PROPERTY_SEPARATOR_COLOR)) {
			setSeparatorColor(TiConvert.toString(newValue));
		} else if (key.equals(TiC.PROPERTY_SEPARATOR_STYLE)) {
			setSeparatorStyle(TiConvert.toInt(newValue));
		} else if (TiC.PROPERTY_OVER_SCROLL_MODE.equals(key)){
			if (Build.VERSION.SDK_INT >= 9) {
				listView.setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
			}
		} else if (key.equals(TiC.PROPERTY_HEADER_TITLE)) {
			setHeaderTitle(TiConvert.toString(newValue));
		} else if (key.equals(TiC.PROPERTY_FOOTER_TITLE)) {
			setFooterTitle(TiConvert.toString(newValue));
		} else if (key.equals(TiC.PROPERTY_SECTIONS) && newValue instanceof Object[] ) {
			processSections((Object[])newValue);
			if (adapter != null) {
				adapter.notifyDataSetChanged();
			}
		} else if (key.equals(TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR) && newValue != null) {
			listView.setVerticalScrollBarEnabled(TiConvert.toBoolean(newValue));
		} else if (key.equals(TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE) && newValue != null) {
			defaultTemplateBinding = TiConvert.toString(newValue);
			refreshItems();
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	private void refreshItems() {
		for (int i = 0; i < sections.size(); i++) {
			ListSectionProxy section = sections.get(i);
			section.refreshItems();
		}
	}

	protected void processTemplates(KrollDict templates) {
		for (String key : templates.keySet()) {
			//Here we bind each template with a key so we can use it to look up later
			KrollDict properties = new KrollDict((HashMap)templates.get(key));
			TiListViewTemplate template = new TiListViewTemplate(key, properties);
			//Set type to template, for recycling purposes.
			template.setType(getItemType());
			templatesByBinding.put(key, template);
			//set parent of root item
			template.setRootParent(proxy);
		}
	}

	protected void processSections(Object[] sections) {
		
		this.sections.clear();
		for (int i = 0; i < sections.length; i++) {
			processSection(sections[i], -1);
		}
	}
	
	protected void processSection(Object sec, int index) {
		if (sec instanceof ListSectionProxy) {
			ListSectionProxy section = (ListSectionProxy) sec;
			if (this.sections.contains(section)) {
				return;
			}
			if (index == -1 || index >= sections.size()) {
				this.sections.add(section);	
			} else {
				this.sections.add(index, section);
			}
			section.setAdapter(adapter);
			section.setListView(this);
			//Attempts to set type for existing templates.
			section.setTemplateType();
			//Process preload data if any
			section.processPreloadData();
		}
	}
	
	protected Pair<ListSectionProxy, Integer> getSectionInfoByEntryIndex(int index) {
		if (index < 0) {
			return null;
		}
		for (int i = 0; i < sections.size(); i++) {
			ListSectionProxy section = sections.get(i);
			int sectionItemCount = section.getItemCount();
			if (index <= sectionItemCount - 1) {
				return new Pair<ListSectionProxy, Integer>(section, index);
			} else {
				index -= sectionItemCount;
			}
		}

		return null;
	}
	
	public int getItemType() {
		return itemTypeCount.getAndIncrement();
	}
	
	public TiListViewTemplate getTemplateByBinding(String binding) {
		return templatesByBinding.get(binding);
	}
	
	public String getDefaultTemplateBinding() {
		return defaultTemplateBinding;
	}
	
	public int getSectionCount() {
		return sections.size();
	}
	
	public void appendSection(Object section) {
		if (section instanceof Object[]) {
			Object[] secs = (Object[]) section;
			for (int i = 0; i < secs.length; i++) {
				processSection(secs[i], -1);
			}
		} else {
			processSection(section, -1);
		}
		adapter.notifyDataSetChanged();
	}
	
	public void deleteSectionAt(int index) {
		if (index >= 0 && index < sections.size()) {
			sections.remove(index);
			adapter.notifyDataSetChanged();
		} else {
			Log.e(TAG, "Invalid index to delete section");
		}
	}
	
	public void insertSectionAt(int index, Object section) {
		if (index > sections.size()) {
			Log.e(TAG, "Invalid index to insert/replace section");
			return;
		}
		if (section instanceof Object[]) {
			Object[] secs = (Object[]) section;
			for (int i = 0; i < secs.length; i++) {
				processSection(secs[i], index);
				index++;
			}
		} else {
			processSection(section, index);
		}
		adapter.notifyDataSetChanged();
	}
	
	public void replaceSectionAt(int index, Object section) {
		deleteSectionAt(index);
		insertSectionAt(index, section);
	}
	
	private int findItemPosition(int sectionIndex, int sectionItemIndex) {
		int position = 0;
		for (int i = 0; i < sections.size(); i++) {
			ListSectionProxy section = sections.get(i);
			if (i == sectionIndex) {
				if (sectionItemIndex >= section.getContentCount()) {
					Log.e(TAG, "Invalid item index");
					return -1;
				}
				position += sectionItemIndex;
				if (section.getHeaderTitle() != null) {
					position += 1;			
				}
				break;
			} else {
				position += section.getItemCount();
			}
		}
		return position;
	}

	private int getCount() {
		if (adapter != null) {
			return adapter.getCount();
		}
		return 0;
	}
	
	public void scrollToItem(int sectionIndex, int sectionItemIndex) {
		int position = findItemPosition(sectionIndex, sectionItemIndex);
		if (position > -1) {
			listView.smoothScrollToPosition(position + 1);
		}
	}

	public void scrollToTop(final int y, boolean animated)
	{
		if (animated) {
			listView.smoothScrollToPosition(0);
		}
		else {
			listView.setSelectionFromTop(0, y);
		}
	}

	public void scrollToBottom(final int y, boolean animated)
	{
		if (animated) {
			listView.smoothScrollToPosition(getCount() - 1);
		}
		else {
			listView.setSelection(getCount() - 1);
		}
	}

	
	public void release() {
		for (int i = 0; i < sections.size(); i++) {
			sections.get(i).releaseViews();
		}
		
		templatesByBinding.clear();
		sections.clear();
		if (listView != null) {
			listView.setAdapter(null);
			listView = null;
		}
		if (headerView != null) {
			headerView = null;
		}
		if (footerView != null) {
			footerView = null;
		}

		super.release();
	}
	
}
