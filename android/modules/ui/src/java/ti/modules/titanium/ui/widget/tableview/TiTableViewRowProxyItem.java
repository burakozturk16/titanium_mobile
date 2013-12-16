/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.tableview;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.LabelProxy;
import ti.modules.titanium.ui.TableViewProxy;
import ti.modules.titanium.ui.TableViewRowProxy;
import ti.modules.titanium.ui.widget.TiUILabel;
import ti.modules.titanium.ui.widget.tableview.TableViewModel.Item;
import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.widget.ImageView;

public class TiTableViewRowProxyItem extends TiBaseTableViewItem
{
	private static final String TAG = "TitaniumTableViewItem";

	// Only check this once, since we potentially use this information
	// every time we add a row. No sense checking it each time.
	private static boolean ICS_OR_GREATER = (Build.VERSION.SDK_INT >= TiC.API_LEVEL_ICE_CREAM_SANDWICH);

	private static final int LEFT_MARGIN = 5;
	private static final int RIGHT_MARGIN = 7;
	private static final int MIN_HEIGHT = 48;

	private BitmapDrawable hasChildDrawable, hasCheckDrawable;
	private ImageView leftImage;
	private ImageView rightImage;
	private TiCompositeLayout content;
	private ArrayList<TiUIView> views;
	private TiDimension height = null;
	private Item item;
	private Object selectorSource;
	private Drawable selectorDrawable;

	public TiTableViewRowProxyItem(Activity activity) {
		super(activity);

		this.handler = new Handler(this);
		this.leftImage = new ImageView(activity);
		leftImage.setVisibility(GONE);
		addView(leftImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

		this.content = new TiCompositeLayout(activity);
		addView(content, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

		this.rightImage = new ImageView(activity);
		rightImage.setVisibility(GONE);
		addView(rightImage, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
	}

	public TiTableViewRowProxyItem(TiContext tiContext)
	{
		this(tiContext.getActivity());
	}
	
	protected TableViewRowProxy getRowProxy() {
		if (item == null) return null;
		return (TableViewRowProxy)item.proxy;
	}
	
	@Override
	public View getView() {
		return content;
	}
	
	@Override
	public void clearViews()
	{
		TableViewRowProxy rowProxy = getRowProxy();
		if (rowProxy != null)
			rowProxy.clearViews();
		content.removeAllViews();
		views = null;		
	}

	
	public void setRowData(Item item) {
		TableViewRowProxy rp = (TableViewRowProxy)item.proxy;
		TableViewRowProxy oldProxy = getRowProxy();
		boolean firstSet = (oldProxy == null);
		boolean changingProxy = (oldProxy != null && oldProxy != rp);
		
		if (changingProxy)
		{
			//we are reusing its view so make sure it doesnt think it still has views!
			oldProxy.clearViews();
			oldProxy.setTableViewItem(null);
		}
		
		setRowData(rp);

		rp.setTableViewItem(this);
		this.item = item;
		
		KrollDict  p;
		if (changingProxy)
			p = getOnlyChangedProperties(oldProxy, rp);
		else
			p = rp.getProperties();
		processProperties(p);
	}

	public Item getRowData() {
		return this.item;
	}

	protected TiViewProxy addViewToOldRow(int index, TiUIView titleView, TiViewProxy newViewProxy) {
		Log.w(TAG, newViewProxy + " was added an old style row, reusing the title TiUILabel", Log.DEBUG_MODE);
		LabelProxy label = new LabelProxy();
		label.handleCreationDict(titleView.getProxy().getProperties());
		label.setView(titleView);
		label.setModelListener(titleView);
		titleView.setProxy(label);

		getRowProxy().getControls().add(index, label);
		views.add(newViewProxy.getOrCreateView());
		return label;
	}
	
	public void addControl(TiViewProxy proxy)
	{
		proxy.clearViews();
		TiUIView view = proxy.forceCreateView(); 
		views.add(view);
		View v = view.getOuterView();

		if (v.getParent() == null) {
			content.addView(v, view.getLayoutParams());
			if (v instanceof TiCompositeLayout) {
				((TiCompositeLayout) v).resort();
			}
			v.requestLayout();
		}
	}
	
	public void removeControl(TiViewProxy proxy)
	{
		if (proxy.peekView() == null) return;
		TiUIView view = proxy.peekView(); 
		int index = views.indexOf(view);
		if (index != -1)
		{
			View v = view.getOuterView();
			content.removeView(v);
			views.remove(index);
		}
	}
	
	/*
	 * Create views for measurement or for layout.  For each view, apply the
	 * properties from the appropriate proxy to the view.
	 */
	protected void createControls(TableViewRowProxy rp)
	{
		ArrayList<TiViewProxy> proxies = rp.getControls();
		int len = proxies.size();

		if (views == null) {
			views = new ArrayList<TiUIView>(len);
		} else if (views.size() != len) {
			for (TiUIView view : views) {
				View v = view.getOuterView();
				if (v != null && v.getParent().equals(content)) {
					content.removeView(v);
				}
			}
			views = new ArrayList<TiUIView>(len);
		}

		for (int i = 0; i < len; i++) {
			
			Boolean needsTransfer = true;
			TiUIView view = views.size() > i ? views.get(i) : null;
			TiViewProxy proxy = proxies.get(i);
			if (view != null && view.getProxy() instanceof TableViewRowProxy) {
				proxy = addViewToOldRow(i, view, proxy);
				needsTransfer = false;
				len++;
			}
			else if (view == null) {
				proxy.clearViews();
				view = proxy.forceCreateView();  // false means don't set modelListener, second false not to process Properties
				if (i >= views.size()) {
					views.add(view);
				} else {
					views.set(i, view);
				}
				needsTransfer = false;
			}

			View v = view.getOuterView();

			if (v.getParent() == null) {
				content.addView(v, view.getLayoutParams());
			}
			
			if (!needsTransfer) continue;
			
			TiViewProxy oldProxy = view.getProxy();
			proxy.setView(view);
			view.setParent(rp);
			view.setProxy(proxy);
			proxy.setModelListener(view, false); //applying proxy properties
			view.registerForTouch();
			view.registerForKeyPress();
			if (oldProxy != proxy)
				view.processProperties(getOnlyChangedProperties(oldProxy, proxy));
			else
				view.processProperties(proxy.getProperties());
			associateProxies(proxy.getChildren(), view.getChildren());
		}
	}

	protected void applyChildProperties(TiViewProxy viewProxy, TiUIView view)
	{
		int i = 0;
		TiViewProxy childProxies[] = viewProxy.getChildren();
		for (TiUIView childView : view.getChildren()) {
			TiViewProxy childProxy = childProxies[i];
			childView.processProperties(childProxy.getProperties());
			applyChildProperties(childProxy, childView);
			i++;
		}
	}

	protected void refreshOldStyleRow(TableViewRowProxy rp)
	{
		// old-style row
		
		if (!rp.hasProperty(TiC.PROPERTY_TOUCH_ENABLED)) {
			// We have traditionally always made the label untouchable, but since
			// version 3.0.0 we support explore-by-touch on ICS and above, so for
			// accessibility purposes we should not be disabling touch if
			// accessibility is currently turned on.
			if (!ICS_OR_GREATER || !TiApplication.getInstance().getAccessibilityManager().isEnabled()) {
				rp.setProperty(TiC.PROPERTY_TOUCH_ENABLED, false);
			}
		}
		// Check if this was a regular row and the control was removed
		// if so, cleanup the views
		if (views != null && views.size() > 0) {
			TiUIView rv = views.get(0);
			if (!(rv instanceof TiUILabel)) {
				content.removeAllViews();
				views.clear();
				views = null;
			}
		}
		if (views == null) {
			views = new ArrayList<TiUIView>();
			views.add(new TiUILabel(rp));
		}
		TiUILabel t = (TiUILabel) views.get(0);
		t.setProxy(rp);
		t.processProperties(filterProperties(rp.getProperties()));
		View v = t.getOuterView();
		if (v.getParent() == null) {
			TiCompositeLayout.LayoutParams params = (TiCompositeLayout.LayoutParams) t.getLayoutParams();
			if (params.optionLeft == null) {
				params.optionLeft = new TiDimension(LEFT_MARGIN, TiDimension.TYPE_LEFT);
			}

			if (params.optionRight == null) {
				params.optionRight = new TiDimension(LEFT_MARGIN, TiDimension.TYPE_RIGHT);
			}
			params.autoFillsWidth = true;
			content.addView(v, params);
		}
	}
	
	@Override
	public void processProperties(KrollDict p)
	{
		Object newSelectorSource = null;
		if (p.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)) {
			newSelectorSource = p.get(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE);
		} else if (p.containsKey(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR)){
			newSelectorSource = p.get(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR);
		}
		if (selectorSource != newSelectorSource){
			selectorDrawable = null;
			selectorSource = newSelectorSource;
			if (selectorSource != null) {
				getRowProxy().getTable().getTableView().getTableView().enableCustomSelector();
			}
		}
		if (p.containsKey(TiC.PROPERTY_BACKGROUND_IMAGE) ||
				p.containsKey(TiC.PROPERTY_BACKGROUND_COLOR))
		{
			setBackgroundFromProxy(getRowProxy());
		}

		// Handle right image
		boolean clearRightImage = true;
		// It's one or the other, check or child.  If you set them both, child's gonna win.
		if (p.containsKey(TiC.PROPERTY_HAS_CHECK)) {
			if (TiConvert.toBoolean(p, TiC.PROPERTY_HAS_CHECK) && hasCheckDrawable == null) {
				hasCheckDrawable = createHasCheckDrawable();
				rightImage.setImageDrawable(hasCheckDrawable);
				rightImage.setVisibility(VISIBLE);
				clearRightImage = false;
			}
		}

		if (p.containsKey(TiC.PROPERTY_HAS_CHILD)) {
			if (TiConvert.toBoolean(p, TiC.PROPERTY_HAS_CHILD) && hasChildDrawable == null) {
				hasChildDrawable = createHasChildDrawable();
				rightImage.setImageDrawable(hasChildDrawable);
				rightImage.setVisibility(VISIBLE);
				clearRightImage = false;
			}
		}
		if (p.containsKey(TiC.PROPERTY_RIGHT_IMAGE)) {
			String path = TiConvert.toString(p, TiC.PROPERTY_RIGHT_IMAGE);
				String url = getRowProxy().resolveUrl(null, path);
				Drawable d = loadDrawable(url);
				if (d != null) {
					rightImage.setImageDrawable(d);
					rightImage.setVisibility(VISIBLE);
					clearRightImage = false;
				}
			
		}

		if (clearRightImage && rightImage.getVisibility() == VISIBLE) {
			hasCheckDrawable = null;
			hasChildDrawable = null;
			rightImage.setImageDrawable(null);
			rightImage.setVisibility(GONE);
		}

		// Handle left image
		boolean clearleftImage = true;
		if (p.containsKey(TiC.PROPERTY_LEFT_IMAGE)) {
			String path = TiConvert.toString(p, TiC.PROPERTY_LEFT_IMAGE);
				String url = getRowProxy().resolveUrl(null, path);
				Drawable d = loadDrawable(url);
				if (d != null) {
					leftImage.setImageDrawable(d);
					leftImage.setVisibility(VISIBLE);
					clearleftImage = false;
				}
		}
		if (clearleftImage && leftImage.getVisibility() == VISIBLE) 
		{
			leftImage.setImageDrawable(null);
			leftImage.setVisibility(GONE);
		}
		
		if (p.containsKey(TiC.PROPERTY_HEIGHT)) {
			if (!p.get(TiC.PROPERTY_HEIGHT).equals(TiC.SIZE_AUTO)
				&& !p.get(TiC.PROPERTY_HEIGHT).equals(TiC.LAYOUT_SIZE)) {
				height = TiConvert.toTiDimension(TiConvert.toString(p, TiC.PROPERTY_HEIGHT), TiDimension.TYPE_HEIGHT);
			}
		}

		if (p.containsKey(TiC.PROPERTY_LAYOUT)) {
				content.setLayoutArrangement(TiConvert.toString(p, TiC.PROPERTY_LAYOUT));
		}
		else content.setLayoutArrangement(null);
		
		if (p.containsKey(TiC.PROPERTY_HORIZONTAL_WRAP)) {
			content.setEnableHorizontalWrap(TiConvert.toBoolean(p, TiC.PROPERTY_HORIZONTAL_WRAP));
		}
		
		if (ICS_OR_GREATER) {
			Object accessibilityHiddenVal = p.get(TiC.PROPERTY_ACCESSIBILITY_HIDDEN);
			if (accessibilityHiddenVal != null) {
				boolean hidden = TiConvert.toBoolean(accessibilityHiddenVal);
				if (hidden) {
					ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO);
				} else {
					ViewCompat.setImportantForAccessibility(this, ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
				}
			}
		}
	}
	
	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		if (key.equals(TiC.PROPERTY_HEIGHT))
		{
			height = null;
			if (!newValue.equals(TiC.SIZE_AUTO) && !newValue.equals(TiC.LAYOUT_SIZE)) {
				height = TiConvert.toTiDimension(TiConvert.toString(newValue), TiDimension.TYPE_HEIGHT);
			}
		}
		else if (key.equals(TiC.PROPERTY_BACKGROUND_IMAGE) || 
				key.equals(TiC.PROPERTY_BACKGROUND_COLOR) ) {
			setBackgroundFromProxy(getRowProxy());
		}
		else if (key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE) || 
				key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR) ) {
			if (selectorSource != key){
				selectorDrawable = null;
				selectorSource = key;
				if (selectorSource != null) {
					getRowProxy().getTable().getTableView().getTableView().enableCustomSelector();
				}
			}
			setBackgroundFromProxy(getRowProxy());
		}
		else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	public void setRowData(TableViewRowProxy rp) {
		Boolean oldStyleRow = !rp.hasControls();
		// hasControls() means that the proxy has children
		if (oldStyleRow) {
			// no children means that this is an old-style row
			refreshOldStyleRow(rp);
		} else {
			createControls(rp);
		}
	}

	protected boolean hasView(TiUIView view) {
		if (views == null) return false;
		for (TiUIView v : views) {
			if (v == view) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		int w = MeasureSpec.getSize(widthMeasureSpec);
		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int h = MeasureSpec.getSize(heightMeasureSpec);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);
		int imageHMargin = 0;

		int leftImageWidth = 0;
		int leftImageHeight = 0;
		if (leftImage != null && leftImage.getVisibility() != View.GONE) {
			measureChild(leftImage, widthMeasureSpec, heightMeasureSpec);
			leftImageWidth = leftImage.getMeasuredWidth();
			leftImageHeight = leftImage.getMeasuredHeight();
			imageHMargin += LEFT_MARGIN;
		}

		int rightImageWidth = 0;
		int rightImageHeight = 0;
		if (rightImage != null && rightImage.getVisibility() != View.GONE) {
			measureChild(rightImage, widthMeasureSpec, heightMeasureSpec);
			rightImageWidth = rightImage.getMeasuredWidth();
			rightImageHeight = rightImage.getMeasuredHeight();
			imageHMargin += RIGHT_MARGIN;
		}

		int adjustedWidth = w - leftImageWidth - rightImageWidth - imageHMargin;
		// int adjustedWidth = w;

		if (content != null) {

			// If there is a child view, we don't set a minimum height for the row.
			// Otherwise, we set a minimum height.
			boolean hasChildView = ((TableViewRowProxy) item.proxy).hasControls();
			if (hasChildView) {
				content.setMinimumHeight(0);
			} else {
				content.setMinimumHeight(MIN_HEIGHT);
			}

			measureChild(content, MeasureSpec.makeMeasureSpec(adjustedWidth, wMode), heightMeasureSpec);
			if (hMode == MeasureSpec.UNSPECIFIED) {
				TableViewProxy table = ((TableViewRowProxy) item.proxy).getTable();
				int minRowHeight = -1;
				if (table != null && table.hasProperty(TiC.PROPERTY_MIN_ROW_HEIGHT)) {
					minRowHeight = TiConvert.toTiDimension(
						TiConvert.toString(table.getProperty(TiC.PROPERTY_MIN_ROW_HEIGHT)), TiDimension.TYPE_HEIGHT)
						.getAsPixels(this);
				}

				if (height == null) {
					h = Math.max(h, Math.max(content.getMeasuredHeight(), Math.max(leftImageHeight, rightImageHeight)));
					h = Math.max(h, minRowHeight);
				} else {
					h = Math.max(minRowHeight, height.getAsPixels(this));
				}
				if (hasChildView) {
					content.getLayoutParams().height = h;
				}

				if (Log.isDebugModeEnabled()) {
					Log.d(TAG, "Row content measure (" + adjustedWidth + "x" + h + ")", Log.DEBUG_MODE);
				}
				measureChild(content, MeasureSpec.makeMeasureSpec(adjustedWidth, wMode),
					MeasureSpec.makeMeasureSpec(h, hMode));
			}
		}

		setMeasuredDimension(w, Math.max(h, Math.max(leftImageHeight, rightImageHeight)));
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		int contentLeft = left;
		int contentRight = right;
		bottom = bottom - top;
		top = 0;

		int height = bottom - top;

		if (leftImage != null && leftImage.getVisibility() != GONE) {
			int w = leftImage.getMeasuredWidth();
			int h = leftImage.getMeasuredHeight();
			int leftMargin = LEFT_MARGIN;

			contentLeft += w + leftMargin;
			int offset = (height - h) / 2;
			leftImage.layout(left+leftMargin, top+offset, left+leftMargin+w, top+offset+h);
		}

		if (rightImage != null && rightImage.getVisibility() != GONE) {
			int w = rightImage.getMeasuredWidth();
			int h = rightImage.getMeasuredHeight();
			int rightMargin = RIGHT_MARGIN;

			contentRight -= w + rightMargin;
			int offset = (height - h) / 2;
			rightImage.layout(right-w-rightMargin, top+offset, right-rightMargin, top+offset+h);
		}

//		if (hasControls) {
//			contentLeft = left + LEFT_MARGIN;
//			contentRight = right - RIGHT_MARGIN;
//		}

		if (content != null) {
			content.layout(contentLeft, top, contentRight, bottom);
		}
	}

	private static String[] filteredProperties = new String[]{
		TiC.PROPERTY_BACKGROUND_IMAGE, TiC.PROPERTY_BACKGROUND_COLOR,
		TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE, TiC.PROPERTY_BACKGROUND_SELECTED_COLOR
	};
	private KrollDict filterProperties(KrollDict d) {
		if (d == null) return new KrollDict();
		
		KrollDict filtered = new KrollDict(d);
		for (int i = 0;i < filteredProperties.length; i++) {
			if (filtered.containsKey(filteredProperties[i])) {
				filtered.remove(filteredProperties[i]);
			}
		}
		return filtered;
	}

	@Override
	public boolean hasSelector() {
		TableViewRowProxy rowProxy = getRowProxy();
		return rowProxy.hasProperty(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)
			|| rowProxy.hasProperty(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR);
	}
	
	@Override
	public Drawable getSelectorDrawable() {
		TableViewRowProxy rowProxy = getRowProxy();
		if (selectorDrawable == null && selectorSource != null) {
			if (rowProxy.hasProperty(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE)) {
				String path = TiConvert.toString(
					rowProxy.getProperty(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE));
				String url = rowProxy.resolveUrl(null, path);
				selectorDrawable = loadDrawable(url);
			} else if (rowProxy.hasProperty(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR)) {
				int color = TiConvert.toColor(rowProxy.getProperty(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR).toString());
				selectorDrawable = new TiTableViewColorSelector(color);
			}
		}
		return selectorDrawable;
	}
	
	@Override
	public void release() {
		super.release();
		if (views != null) {
			for (TiUIView view : views) {
				view.release();
			}
			views = null;
		}
		if (content != null) {
			content.removeAllViews();
			content = null;
		}
		if (hasCheckDrawable != null) {
			hasCheckDrawable.setCallback(null);
			hasCheckDrawable = null;
		}
		if (hasChildDrawable != null) {
			hasChildDrawable.setCallback(null);
			hasChildDrawable = null;
		}
	}
	
	KrollDict getOnlyChangedProperties(TiViewProxy oldProxy, TiViewProxy newProxy)
	{
		KrollDict realProps = new KrollDict();
		KrollDict oldprops = oldProxy.getProperties();
		KrollDict newprops = newProxy.getProperties();
		Set<String> goneProps = oldprops.minusKeys(newprops);
		for(String key:goneProps) {
			realProps.put(key, newProxy.getDefaultValue(key));
		}
		for(Entry<String, Object> e: newprops.entrySet()){
			String key = e.getKey();
			Object newvalue = e.getValue();
			if (oldprops.containsKeyWithValue(key, newvalue) == false || key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_COLOR) || key.equals(TiC.PROPERTY_BACKGROUND_SELECTED_IMAGE))
				realProps.put(key, newvalue);
		}
		return realProps;
	}
	
	protected void associateProxies(TiViewProxy[] proxies, List<TiUIView> views)
	{
		int i = 0;
		for (TiUIView view : views) {
			if (proxies.length < (i+1)) {
				break;
			}
			TiViewProxy proxy = proxies[i];
			TiViewProxy oldProxy = view.getProxy();
			proxy.setView(view);
			view.setProxy(proxy);
			proxy.setModelListener(view, false); //applying proxy properties
			view.registerForTouch();
			view.registerForKeyPress();
			view.processProperties(getOnlyChangedProperties(oldProxy, proxy));
			associateProxies(proxy.getChildren(), view.getChildren());
			i++;
		}
	}

}
