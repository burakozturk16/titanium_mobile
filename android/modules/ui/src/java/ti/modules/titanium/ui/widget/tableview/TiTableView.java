/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.tableview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import ti.modules.titanium.ui.TableViewProxy;
import ti.modules.titanium.ui.TableViewRowProxy;
import ti.modules.titanium.ui.widget.CustomListView;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar.OnSearchChangeListener;
import ti.modules.titanium.ui.widget.tableview.TableViewModel.Item;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

@SuppressLint("DefaultLocale")
public class TiTableView extends FrameLayout
	implements OnSearchChangeListener
{
	public static final int TI_TABLE_VIEW_ID = 101;
	private static final String TAG = "TiTableView";

	//TODO make this configurable
	protected static final int MAX_CLASS_NAMES = 32;

	private TableViewModel viewModel;
	private CustomListView listView;
	private TTVListAdapter adapter;
	private OnItemClickedListener itemClickListener;
	private OnItemLongClickedListener itemLongClickListener;

	private HashMap<String, Integer> rowTypes;
	private AtomicInteger rowTypeCounter;

	private String filterAttribute;
	private String filterText;

	private TableViewProxy proxy;
	private boolean filterCaseInsensitive = true;
	private boolean filterAnchored = false;
	private StateListDrawable selector;

	public interface OnItemClickedListener {
		public void onClick(KrollDict item);
	}

	public interface OnItemLongClickedListener {
		public boolean onLongClick(KrollDict item);
	}

	class TTVListAdapter extends BaseAdapter implements StickyListHeadersAdapter {
		TableViewModel viewModel;
		ArrayList<Integer> index;
		private boolean filtered;
        private TableViewProxy proxy;

		TTVListAdapter(TableViewModel viewModel, TableViewProxy proxy) {
			this.viewModel = viewModel;
            this.proxy = proxy;
			this.index = new ArrayList<Integer>(viewModel.getRowCount());
			reIndexItems();
		}

		protected void registerClassName(String className) {
			if (!rowTypes.containsKey(className)) {
				Log.d(TAG, "registering new className " + className, Log.DEBUG_MODE);
				rowTypes.put(className, rowTypeCounter.incrementAndGet());
			}
		}

		public void reIndexItems() {
			ArrayList<Item> items = viewModel.getViewModel();
			int count = items.size();
			index.clear();

			filtered = false;
			if (filterAttribute != null && filterText != null && filterAttribute.length() > 0 && filterText.length() > 0) {
				filtered = true;
				String filter = filterText;
				if (filterCaseInsensitive) {
					filter = filterText.toLowerCase();
				}
				for(int i = 0; i < count; i++) {
					boolean keep = true;
					Item item = items.get(i);
					registerClassName(item.className);
					if (item.proxy.hasProperty(filterAttribute)) {
						String t = TiConvert.toString(item.proxy.getProperty(filterAttribute));
						if (filterCaseInsensitive) {
							t = t.toLowerCase();
						}
						if (filterAnchored) {
						    if(!t.startsWith(filter)) {
							keep = false;
							}
						}
						else {
						    if(t.indexOf(filter) < 0) {
							keep = false;
							}
						}
					}
					if (keep) {
						index.add(i);
					}
				}
			} else {
				for(int i = 0; i < count; i++) {
					Item item = items.get(i);
					registerClassName(item.className);
					index.add(i);
				}
			}
			if (index.size() == 0) {
				proxy.fireEvent(TiC.EVENT_NO_RESULTS, null);
			}
		}

		public int getCount() {
			//return viewModel.getViewModel().length();
			return index.size();
		}

		public Object getItem(int position) {
			if (position >= index.size()) {
				return null;
			}

			return viewModel.getViewModel().get(index.get(position));
		}

		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getViewTypeCount() {
			return MAX_CLASS_NAMES;
		}

		@Override
		public int getItemViewType(int position) {
			Item item = (Item) getItem(position);
			registerClassName(item.className);
			return rowTypes.get(item.className);
		}

		/*
		 * IMPORTANT NOTE:
		 * getView() is called by the Android framework whenever it needs a view.
		 * The call to getView() could come on a measurement pass or on a layout 
		 * pass.  It's not possible to tell from the arguments whether the framework
		 * is calling getView() for a measurement pass or for a layout pass.  Therefore,
		 * it is important that getView() and all methods call by getView() only create
		 * the views and fill them in with the appropriate data.  What getView() and the
		 * methods call by getView MUST NOT do is to make any associations between 
		 * proxies and views.   Those associations must be made only for the views
		 *  that are used for layout, and should be driven from the onLayout() callback.
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			Item item = (Item) getItem(position);
			TiViewProxy newProxy = item.proxy;
			TiBaseTableViewItem v = null;
			boolean sameView = false;
			//we have a conversion view, first lets check if it s compatible with our proxy
			if (convertView != null) {
				v = (TiBaseTableViewItem) convertView;
				// Default creates view for each Item
				
				if (newProxy instanceof TableViewRowProxy) {
					TableViewRowProxy row = (TableViewRowProxy)newProxy;
					if (row.getTableViewRowProxyItem() != null) {
						sameView = row.getTableViewRowProxyItem().equals(convertView);
					}
				}
				if (!sameView) {
					if (v.getClassName().equals(TableViewProxy.CLASSNAME_DEFAULT)) {
						if (v.getRowData() != item) {
							v = null;
						}
					} else if (v.getClassName().equals(TableViewProxy.CLASSNAME_HEADERVIEW)) {
						//Always recreate the header view
						v = null;
					} else {
						// otherwise compare class names
						if (!(item.proxy instanceof TableViewRowProxy) || !v.getClassName().equals(item.className)) {
							Log.w(TAG, "Handed a view to convert with className " + v.getClassName() + " expected "
								+ item.className, Log.DEBUG_MODE);
							v = null;
						}
					}
				}
				if (v!= null)
				{
					TiViewProxy oldProxy = v.getRowData().proxy;
					//last check, verify that we can transfer proxy
					Boolean canreproxy = true;
					KrollProxy[] oldproxies = oldProxy.getChildren();
					KrollProxy[] newproxies = newProxy.getChildren();
					if (oldproxies.length != newproxies.length)
					{
						canreproxy = false;
					}
					else
					{
						for (int i = 0; i < oldproxies.length; i++) {
						    KrollProxy newSubProxy = newproxies[i];
						    KrollProxy oldSubProxy = oldproxies[i];
							
							if (!(oldSubProxy instanceof TiViewProxy) || (oldSubProxy.getClass().equals(newSubProxy.getClass())) || ((TiViewProxy) oldSubProxy).validateTransferToProxy((TiViewProxy) newSubProxy, true) == false)
							{
								canreproxy = false;
								break;
							}
						}
					}
					
					if (canreproxy == false && ((ViewGroup)v.getView()).getChildCount() > 0)
					{
						Log.w(TAG, "We cant reuse that view, will create a new one", Log.DEBUG_MODE);
						
						//we must clear this viewItem. As a consequence it will be as creating a new one
						v.clearViews();
					}
				}
			}
			if (v == null) {
				if (item.className.equals(TableViewProxy.CLASSNAME_HEADERVIEW)) {
					TiViewProxy vproxy = item.proxy;
					TiUIView headerView = layoutHeaderOrFooter(vproxy);
					TiUIHelper.removeViewFromSuperView(headerView.getOuterView());
					v = new TiTableViewHeaderItem(proxy.getActivity(), headerView);
					v.setClassName(TableViewProxy.CLASSNAME_HEADERVIEW);
					return v;
				} else if (item.className.equals(TableViewProxy.CLASSNAME_HEADER)) {
					v = new TiTableViewHeaderItem(proxy.getActivity());
					v.setClassName(TableViewProxy.CLASSNAME_HEADER);
				} else if (item.className.equals(TableViewProxy.CLASSNAME_NORMAL)) {
					v = new TiTableViewRowProxyItem(proxy.getActivity());
					v.setClassName(TableViewProxy.CLASSNAME_NORMAL);
				} else if (item.className.equals(TableViewProxy.CLASSNAME_DEFAULT)) {
					v = new TiTableViewRowProxyItem(proxy.getActivity());
					v.setClassName(TableViewProxy.CLASSNAME_DEFAULT);
				} else {
					v = new TiTableViewRowProxyItem(proxy.getActivity());
					v.setClassName(item.className);
				}
				v.setLayoutParams(new AbsListView.LayoutParams(
					AbsListView.LayoutParams.MATCH_PARENT, AbsListView.LayoutParams.MATCH_PARENT));
			}
            else if (sameView == false) 
            {
            	v.prepareForReuse();
            }
            if (v.getRowData() != item || sameView == false)
            {
                v.setRowData(item);
            }
			return v;
		}

		@Override
		public boolean areAllItemsEnabled() {
			return false;
		}

		@Override
		public boolean isEnabled(int position) {
			Item item = (Item) getItem(position);
			boolean enabled = true;
			if (item != null && item.className.equals(TableViewProxy.CLASSNAME_HEADER)) {
				enabled = false;
			}
			return enabled;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public void notifyDataSetChanged() {
			reIndexItems();
			super.notifyDataSetChanged();
		}

		public boolean isFiltered() {
			return filtered;
		}

        @Override
        public long getHeaderId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getHeaderView(int arg0, View arg1, ViewGroup arg2) {
            // TODO Auto-generated method stub
            return null;
        }
	}

	public TiTableView(TableViewProxy proxy)
	{
		super(proxy.getActivity());
		this.proxy = proxy;

		rowTypes = new HashMap<String, Integer>();
		rowTypeCounter = new AtomicInteger(-1);
		rowTypes.put(TableViewProxy.CLASSNAME_HEADER, rowTypeCounter.incrementAndGet());
		rowTypes.put(TableViewProxy.CLASSNAME_NORMAL, rowTypeCounter.incrementAndGet());
		rowTypes.put(TableViewProxy.CLASSNAME_DEFAULT, rowTypeCounter.incrementAndGet());

		this.viewModel = new TableViewModel(proxy);
		this.listView = new CustomListView(getContext());
		listView.setId(TI_TABLE_VIEW_ID);

		listView.setFocusable(true);
		listView.setFocusableInTouchMode(true);
		listView.setBackgroundColor(Color.TRANSPARENT);
		getInternalListView().setCacheColorHint(Color.TRANSPARENT);
		getInternalListView().setScrollingCacheEnabled(false);
		final KrollProxy fProxy = proxy;
		listView.setOnScrollListener(new OnScrollListener()
		{
			private boolean scrollValid = false;
			private int lastValidfirstItem = 0;
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				view.requestDisallowInterceptTouchEvent(scrollState != ViewPager.SCROLL_STATE_IDLE);		
				if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
					scrollValid = false;
					if (fProxy.hasListeners(TiC.EVENT_SCROLLEND)) {
						KrollDict eventArgs = new KrollDict();
						KrollDict size = new KrollDict();
						size.put(TiC.PROPERTY_WIDTH, TiTableView.this.getWidth());
						size.put(TiC.PROPERTY_HEIGHT, TiTableView.this.getHeight());
						eventArgs.put(TiC.PROPERTY_SIZE, size);
						fProxy.fireEvent(TiC.EVENT_SCROLLEND, eventArgs);
					}
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
				if(fireScroll) {
					lastValidfirstItem = firstVisibleItem;
					if (fProxy.hasListeners(TiC.EVENT_SCROLL)) {
						KrollDict eventArgs = new KrollDict();
						eventArgs.put("firstVisibleItem", firstVisibleItem);
						eventArgs.put("visibleItemCount", visibleItemCount);
						eventArgs.put("totalItemCount", totalItemCount);
						KrollDict size = new KrollDict();
						size.put(TiC.PROPERTY_WIDTH, TiTableView.this.getWidth());
						size.put(TiC.PROPERTY_HEIGHT, TiTableView.this.getHeight());
						eventArgs.put(TiC.PROPERTY_SIZE, size);
						fProxy.fireEvent(TiC.EVENT_SCROLL, eventArgs);
					}
				}
			}
		});

		if (proxy.hasProperty(TiC.PROPERTY_SEPARATOR_COLOR)) {
			setSeparatorColor(TiConvert.toColor(proxy.getProperty(TiC.PROPERTY_SEPARATOR_COLOR)));
		}
		adapter = new TTVListAdapter(viewModel, proxy);
		if (proxy.hasProperty(TiC.PROPERTY_HEADER_VIEW)) {
			TiViewProxy view = (TiViewProxy) proxy.getProperty(TiC.PROPERTY_HEADER_VIEW);
			listView.addHeaderView(layoutHeaderOrFooter(view).getOuterView(), null, false);
		}
		if (proxy.hasProperty(TiC.PROPERTY_FOOTER_VIEW)) {
			TiViewProxy view = (TiViewProxy) proxy.getProperty(TiC.PROPERTY_FOOTER_VIEW);
			listView.addFooterView(layoutHeaderOrFooter(view).getOuterView(), null, false);
		}

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (itemClickListener != null) {
					if (!(view instanceof TiBaseTableViewItem)) {
						return;
					}
					rowClicked((TiBaseTableViewItem)view, position, false);
				}
			}
		});
		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				if (itemLongClickListener == null) {
					return false;
				}
				TiBaseTableViewItem tvItem = null;
				if (view instanceof TiBaseTableViewItem) {
					tvItem = (TiBaseTableViewItem)view;
				} else {
					tvItem = getParentTableViewItem(view);
				}
				if (tvItem == null) {
					return false;
				}
				return rowClicked(tvItem, position, true);
			}
		});
		addView(listView);
	}
	
	public void removeHeaderView(TiViewProxy viewProxy)
	{
		TiUIView peekView = viewProxy.peekView();
		View outerView = (peekView == null) ? null : peekView.getOuterView();
		if (outerView != null) {
			listView.removeHeaderView(outerView);
		}
	}

	public void setHeaderView()
	{
		if (proxy.hasProperty(TiC.PROPERTY_HEADER_VIEW)) {
			listView.setAdapter(null);
			TiViewProxy view = (TiViewProxy) proxy.getProperty(TiC.PROPERTY_HEADER_VIEW);
			listView.addHeaderView(layoutHeaderOrFooter(view).getOuterView(), null, false);
			listView.setAdapter(adapter);
		}
	}

	public void removeFooterView(TiViewProxy viewProxy)
	{
		TiUIView peekView = viewProxy.peekView();
		View outerView = (peekView == null) ? null : peekView.getOuterView();
		if (outerView != null) {
			listView.removeFooterView(outerView);
		}
	}

	public void setFooterView()
	{
		if (proxy.hasProperty(TiC.PROPERTY_FOOTER_VIEW)) {
			listView.setAdapter(null);
			TiViewProxy view = (TiViewProxy) proxy.getProperty(TiC.PROPERTY_FOOTER_VIEW);
			listView.addFooterView(layoutHeaderOrFooter(view).getOuterView(), null, false);
			listView.setAdapter(adapter);
		}
	}
	
	public TiTableView(TiContext tiContext, TableViewProxy proxy)
	{
		this(proxy);
	}

	private TiBaseTableViewItem getParentTableViewItem(View view)
	{
		ViewParent parent = view.getParent();
		while (parent != null) {
			if (parent instanceof TiBaseTableViewItem) {
				return (TiBaseTableViewItem) parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

    private AbsListView getInternalListView() {
        return listView.getWrappedList();
    }
    
	public void enableCustomSelector() {
		Drawable currentSelector = getInternalListView().getSelector();
		if (currentSelector != selector) {
			selector = new StateListDrawable();
			TiTableViewSelector selectorDrawable = new TiTableViewSelector (listView);
			selector.addState(new int[] {android.R.attr.state_pressed}, selectorDrawable);
			listView.setSelector(selector);
		}
	}
	
	public Item getItemAtPosition(int position)
	{
		if (proxy.hasProperty(TiC.PROPERTY_HEADER_VIEW)) {
			position -= 1;
		}
		if (position == -1 || position == adapter.getCount()) {
			return null;
		}
		return viewModel.getViewModel().get(adapter.index.get(position));
	}
	
	public int getPositionForView(View view) {
		int result = -1;
		try {
			result = listView.getPositionForView(view);
		} catch(NullPointerException e){
			
		}
		return result;
	}

	public int getIndexFromXY(double x, double y) {
		int bound = listView.getLastVisiblePosition() - listView.getFirstVisiblePosition();
		for (int i = 0; i <= bound; i++) {
			View child = listView.getChildAt(i);
			if (child != null && x >= child.getLeft() && x <= child.getRight() && y >= child.getTop() && y <= child.getBottom()) {
				return listView.getFirstVisiblePosition() + i;
			}
		}
		return -1;
	}
	
	protected boolean rowClicked(TiBaseTableViewItem rowView, int position, boolean longClick) {
		String viewClicked = rowView.getLastClickedViewName();
		Item item = getItemAtPosition(position);
		KrollDict event = new KrollDict();
		String eventName = longClick ? TiC.EVENT_LONGCLICK : TiC.EVENT_CLICK;
		TableViewRowProxy.fillClickEvent(event, viewModel, item);
		if (viewClicked != null) {
			event.put(TiC.EVENT_PROPERTY_LAYOUT_NAME, viewClicked);
		}
		event.put(TiC.EVENT_PROPERTY_SEARCH_MODE, adapter.isFiltered());

		boolean longClickFired = false;
		if (item.proxy != null && item.proxy instanceof TableViewRowProxy) {
			TableViewRowProxy rp = (TableViewRowProxy) item.proxy;
			event.put(TiC.EVENT_PROPERTY_SOURCE, rp);
			// The event will bubble up to the parent.
			if (rp.hierarchyHasListener(eventName)) {
				rp.fireEvent(eventName, event);
				longClickFired = true;
			}
		}
		if (longClick && !longClickFired) {
			return itemLongClickListener.onLongClick(event);
		} else if (longClickFired) {
			return true;
		} else {
			return false; // standard (not-long) click handling has no return value.
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
		viewProxy.clearViews();
		TiUIView tiView = viewProxy.forceCreateView();
		View nativeView = tiView.getOuterView();
		TiCompositeLayout.LayoutParams params = tiView.getLayoutParams();

		// Set width to MATCH_PARENT to be consistent with iPhone
		int width = AbsListView.LayoutParams.MATCH_PARENT;
		int height = AbsListView.LayoutParams.WRAP_CONTENT;
		if (params.sizeOrFillHeightEnabled) {
			if (params.autoFillsHeight) {
				height = AbsListView.LayoutParams.MATCH_PARENT;
			}
		} else if (params.optionHeight != null) {
			height = params.optionHeight.getAsPixels(listView);
		}
		
		AbsListView.LayoutParams p = new AbsListView.LayoutParams(width, height);
		nativeView.setLayoutParams(p);
		return tiView;
	}

	public void dataSetChanged() {
		if (adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public int getCount() {
		if (adapter != null) {
			return adapter.getCount();
		}
		return 0;
	}

	public void setOnItemClickListener(OnItemClickedListener listener) {
		this.itemClickListener = listener;
	}

	public void setOnItemLongClickListener(OnItemLongClickedListener listener)
	{
		this.itemLongClickListener = listener;
	}

	public void setSeparatorColor(int sepColor) {
		int dividerHeight = listView.getDividerHeight();
		listView.setDivider(new ColorDrawable(sepColor));
		listView.setDividerHeight(dividerHeight);
	}
	
	public void setSeparatorStyle(int separatorHeight) {
		Drawable drawable = listView.getDivider();
		listView.setDivider(drawable);
		listView.setDividerHeight(separatorHeight);
	}

	public TableViewModel getTableViewModel() {
		return this.viewModel;
	}

	public CustomListView getListView() {
		return listView;
	}

	@Override
	public void filterBy(String text) {
		filterText = text;
		if (adapter != null) {
			proxy.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					dataSetChanged();
				}
			});
		}
	}

	public void setFilterAttribute(String filterAttribute) {
		this.filterAttribute = filterAttribute;
	}

	public void setFilterAnchored(boolean filterAnchored) {
		this.filterAnchored  = filterAnchored;
	}

	public void setFilterCaseInsensitive(boolean filterCaseInsensitive) {
		this.filterCaseInsensitive  = filterCaseInsensitive;
	}

	public void release() {
		adapter = null;
		if (listView != null) {
			listView.setAdapter(null);
		}
		listView = null;
		if (viewModel != null) {
			viewModel.release();
		}
		viewModel = null;
		itemClickListener = null;
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

		super.onLayout(changed, left, top, right, bottom);

		if (proxy != null) {
			TiUIHelper.firePostLayoutEvent(proxy.peekView());
		}

		// Layout is finished, re-enable focus events.
		if (focusListener != null) {
			focusedView.setOnFocusChangeListener(focusListener);
			// If the configuration changed, we manually fire the blur event
			if (changed) {
				focusListener.onFocusChange(focusedView, false);
			}
		}
	}
}
