/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.lang.Math;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiBaseActivity.ConfigurationChangedListener;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiEventHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;
import org.appcelerator.titanium.view.TiUIView;

import com.lambergar.verticalviewpager.VerticalViewPager;

import ti.modules.titanium.ui.ScrollableViewProxy;
import ti.modules.titanium.ui.SlideMenuProxy;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

@SuppressLint("NewApi")
public class TiUIScrollableView extends TiUIView implements  ViewPager.OnPageChangeListener, VerticalViewPager.OnPageChangeListener
//, ConfigurationChangedListener
{
	private static final String TAG = "TiUIScrollableView";

	private static final int PAGE_LEFT = 200;
	private static final int PAGE_RIGHT = 201;
	private static final Class<?>[] DISALLOWINTERCEPTTOUCH_CLASSES = {ScrollView.class, ListView.class};
	private int pageMargin = 0;
	private TiDimension pageDimension = new TiDimension("100%", TiDimension.TYPE_WIDTH);
	private TiDimension pageOffset = new TiDimension("50%", TiDimension.TYPE_WIDTH);
	private boolean verticalLayout = false;
	boolean mNeedsRedraw = false;
//	public boolean needsRepopulate = false;
	private IViewPager mTouchTarget;
	private int mPreviousState = ViewPager.SCROLL_STATE_IDLE;
	private interface IPageAdapter {

		void notifyDataSetChanged();
		
	}

	private interface IViewPager {
		public void setOffscreenPageLimit(int limit);
		public void setOverScrollMode(int overScrollMode);
		public void setCurrentItem(int item);
		public void setCurrentItem(int item, boolean smoothScroll);
		public int getChildCount();
		public void removeViewAt (int index);
		public void removeView(View child);
		public void addView(View child, int index, ViewGroup.LayoutParams params);
		public void addView(View child, ViewGroup.LayoutParams params);
		public void setAdapter(IPageAdapter adapter);
		public void setPageMargin (int marginPixels);
	}
	
	private class HViewPager extends ViewPager implements IViewPager{

		public HViewPager(Context context) {
			super(context);
			setClipChildren(false);
		}
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (mEnabled) {
				return super.onTouchEvent(event);
			}

			return false;
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent event) {
			if (mEnabled) {
				return super.onInterceptTouchEvent(event);
			}

			return false;
		}
		@Override
		public void setAdapter(IPageAdapter adapter) {
			setAdapter((PagerAdapter)adapter);
			
		}
//		@Override
//		protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
////			int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
////			float realDimension = pageDimension.getAsPixels(getContext(), parentWidth, parentWidth);
////			pageWidth = realDimension/parentWidth;
////			if (needsRepopulate == true) {
////				mAdapter.notifyDataSetChanged(); //heavy but seems to be the only way to have pageWidth
////				//ask for again after orientation change
////				needsRepopulate = false;
////			}
//			
//			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		}
	}
	private class VViewPager extends VerticalViewPager implements IViewPager{

		public VViewPager(Context context) {
			super(context);
			setClipChildren(false);
		}
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (mEnabled) {
				return super.onTouchEvent(event);
			}

			return false;
		}

		@Override
		public boolean onInterceptTouchEvent(MotionEvent event) {
			if (mEnabled) {
				return super.onInterceptTouchEvent(event);
			}

			return false;
		}

		@Override
		public void setAdapter(IPageAdapter adapter) {
			setAdapter((com.lambergar.verticalviewpager.PagerAdapter)adapter);
			
		}
//		@Override
//		protected void onMeasure (int widthMeasureSpec, int heightMeasureSpec) {
//			
//			int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
//			float realDimension = pageDimension.getAsPixels(getContext(), parentHeight, parentHeight);
//			pageWidth = realDimension/parentHeight;
//			if (needsRepopulate == true) {
//				mAdapter.notifyDataSetChanged(); //heavy but seems to be the only way to have pageWidth
//				//ask for again after orientation change
//				needsRepopulate = false;
//			}
//			
//			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//		}
	}
	
	private IViewPager mPager;
	private final ArrayList<TiViewProxy> mViews;
	private IPageAdapter mAdapter;
	private final TiCompositeLayout mContainer;
	private final RelativeLayout mPagingControl;

	private int mCurIndex = 0;
	private boolean mEnabled = true;
	
	

	public TiUIScrollableView(ScrollableViewProxy proxy, TiBaseActivity activity)
	{
		super(proxy);
//		activity.addConfigurationChangedListener(this);

		mViews = new ArrayList<TiViewProxy>();
		
		buildViewPager(activity);

		mContainer = new TiViewPagerLayout(activity);
		mContainer.addView((View)mPager);
		mPagingControl = buildPagingControl(activity);
		mContainer.addView(mPagingControl, buildFillLayoutParams());

		setNativeView(mContainer);
	}
	
	private boolean isValidScroll = false;
	private boolean justFiredDragEnd = false;

	@Override
	public void onPageScrollStateChanged(int scrollState) {
		mNeedsRedraw = (scrollState != ViewPager.SCROLL_STATE_IDLE);
		
		// All of this is to inhibit any scrollable container from consuming our touch events as the user is changing pages
        if (mPreviousState == ViewPager.SCROLL_STATE_IDLE) {
            if (scrollState == ViewPager.SCROLL_STATE_DRAGGING) {
                mTouchTarget = mPager;
                mContainer.requestDisallowInterceptTouchEvent(true);
           }
        } else {
            if (scrollState == ViewPager.SCROLL_STATE_IDLE || scrollState == ViewPager.SCROLL_STATE_SETTLING) {
                mTouchTarget = null;
                mContainer.requestDisallowInterceptTouchEvent(false);
           }
        }

        mPreviousState = scrollState;
		
		if (scrollState == ViewPager.SCROLL_STATE_DRAGGING) {
			((ScrollableViewProxy)proxy).fireScrollStart(mCurIndex, mViews.get(mCurIndex));
		}
		else if ((scrollState == ViewPager.SCROLL_STATE_IDLE) && isValidScroll) {
			int oldIndex = mCurIndex;
			{
				((ScrollableViewProxy)proxy).fireScrollEnd(mCurIndex, mViews.get(mCurIndex));
			}
			if (mCurIndex >= 0) {
				if (oldIndex >=0 && oldIndex != mCurIndex && oldIndex < mViews.size()) {
					// Don't know what these focused and unfocused
					// events are good for, but they were in our previous
					// scrollable implementation.
					// cf. https://github.com/appcelerator/titanium_mobile/blob/20335d8603e2708b59a18bafbb91b7292278de8e/android/modules/ui/src/ti/modules/titanium/ui/widget/TiScrollableView.java#L260
					TiEventHelper.fireFocused(mViews.get(oldIndex));
				}

				TiEventHelper.fireUnfocused(mViews.get(mCurIndex));
				if (oldIndex >= 0) {
					// oldIndex will be -1 if the view has just
					// been created and is setting currentPage
					// to something other than 0. In that case we
					// don't want a `scrollend` to fire.
					((ScrollableViewProxy)proxy).fireScrollEnd(mCurIndex, mViews.get(mCurIndex));
				}

				if (shouldShowPager()) {
					showPager();
				}
			}

			// If we don't use this state variable to check if it's a valid
			// scroll, this event will fire when the view is first created
			// because on creation, the scroll state is initialized to 
			// `idle` and this handler is called.
			isValidScroll = false;
		} else if (scrollState == ViewPager.SCROLL_STATE_SETTLING) {
			((ScrollableViewProxy)proxy).fireDragEnd(mCurIndex, mViews.get(mCurIndex));

			// Note that we just fired a `dragend` so the `onPageSelected`
			// handler below doesn't fire a `scrollend`.  Read below comment.
			justFiredDragEnd = true;
		}
		
		 mPreviousState = scrollState;
	}

	@Override
	public void onPageSelected(int page)
	{

		// If we didn't just fire a `dragend` event then this is the case
		// where a user drags the view and settles it on a different view.
		// Since the OS settling logic is never run, the
		// `onPageScrollStateChanged` handler is never run, and therefore
		// we forgot to inform the Javascripters that the user just scrolled
		// their thing.

		if (!justFiredDragEnd && mCurIndex != -1 && mCurIndex < mViews.size()) {
			((ScrollableViewProxy)proxy).fireScrollEnd(mCurIndex, mViews.get(mCurIndex));

			if (shouldShowPager()) {
				showPager();
			}
		}
	}

	@Override
	public void onPageScrolled(int positionRoundedDown, float positionOffset, int positionOffsetPixels)
	{
		isValidScroll = true;

		// When we touch and drag the view and hold it inbetween the second
		// and third sub-view, this function will have been called with values
		// similar to:
		//		positionRoundedDown:	1
		//		positionOffset:			 0.5
		// ie, the first parameter is always rounded down; the second parameter
		// is always just an offset between the current and next view, it does
		// not take into account the current view.

		// If we add positionRoundedDown to positionOffset, positionOffset will
		// have the 'correct' value; ie, will be a natural number when we're on
		// one particular view, something.5 when inbetween views, etc.
		float positionFloat = positionOffset + positionRoundedDown;

		// `positionFloat` can now be used to calculate the correct value for
		// the current index. We add 0.5 so that positionFloat will be rounded
		// half up; ie, if it has a value of 1.5, it will be rounded up to 2; if
		// it has a value of 1.4, it will be rounded down to 1.
		mCurIndex = (int) Math.floor(positionFloat + 0.5);
		mCurIndex = Math.min(Math.max(mCurIndex, 0), mViews.size() - 1);
		((ScrollableViewProxy)proxy).fireScroll(mCurIndex, positionFloat, mViews.get(mCurIndex));

		// Note that we didn't just fire a `dragend`.  See the above comment
		// in `onPageSelected`.
		justFiredDragEnd = false;
		
		//Force the container to redraw on scrolling.
        //Without this the outer pages render initially and then stay static
        if (mNeedsRedraw) nativeView.invalidate();
	}
	private void buildViewPager(Context context)
	{
		if (proxy.hasProperty(TiC.PROPERTY_LAYOUT) && TiConvert.toString(proxy.getProperty(TiC.PROPERTY_LAYOUT)).equals("vertical")) {
			mPager = new VViewPager(context);
			mAdapter = new VViewPagerAdapter(mViews);
			((VViewPager)mPager).setOnPageChangeListener(this);
			verticalLayout = true;
		}
		else {
			mPager = new HViewPager(context);
			mAdapter = new HViewPagerAdapter(mViews);
			((HViewPager)mPager).setOnPageChangeListener(this);
		}
		mPager.setAdapter(mAdapter);
		mPager.setPageMargin(-pageMargin);
	}

	private boolean shouldShowPager()
	{
		Object showPagingControl = proxy.getProperty(TiC.PROPERTY_SHOW_PAGING_CONTROL);
		if (showPagingControl != null) {
			return TiConvert.toBoolean(showPagingControl);
		} else {
			return false;
		}
	}

	private TiCompositeLayout.LayoutParams buildFillLayoutParams()
	{
		TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
		params.autoFillsHeight = true;
		params.autoFillsWidth = true;
		return params;
	}

	private RelativeLayout buildPagingControl(Context context)
	{
		RelativeLayout layout = new RelativeLayout(context);
		layout.setFocusable(false);
		layout.setFocusableInTouchMode(false);

		TiArrowView left = new TiArrowView(context);
		left.setVisibility(View.INVISIBLE);
		left.setId(PAGE_LEFT);
		left.setMinimumWidth(80); // TODO density?
		left.setMinimumHeight(80);
		left.setOnClickListener(new OnClickListener(){
			public void onClick(View v)
			{
				if (mEnabled) {
					movePrevious();
				}
			}});
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		layout.addView(left, params);

		TiArrowView right = new TiArrowView(context);
		right.setLeft(false);
		right.setVisibility(View.INVISIBLE);
		right.setId(PAGE_RIGHT);
		right.setMinimumWidth(80); // TODO density?
		right.setMinimumHeight(80);
		right.setOnClickListener(new OnClickListener(){
			public void onClick(View v)
			{
				if (mEnabled) {
					moveNext();
				}
			}});
		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		params.addRule(RelativeLayout.CENTER_VERTICAL);
		layout.addView(right, params);

		layout.setVisibility(View.GONE);

		return layout;
	}

	private void setCacheSize(int size) {
		int newCacheSize = size;
			if (newCacheSize < 3) {
		 		// WHAT.  Let's make it something sensible.
				newCacheSize = 3;
			}
			if (newCacheSize % 2 == 0) {
				newCacheSize -= 1;
			}
			int cacheSize = newCacheSize/2;
			Log.w(TAG, "cacheSize " + cacheSize);
			mPager.setOffscreenPageLimit(cacheSize);
	}

	@Override
	public void processProperties(KrollDict d)
	{
		if (d.containsKey(TiC.PROPERTY_VIEWS)) {
			setViews(d.get(TiC.PROPERTY_VIEWS));
		} 

		if (d.containsKey(TiC.PROPERTY_CURRENT_PAGE)) {
			int page = TiConvert.toInt(d, TiC.PROPERTY_CURRENT_PAGE);
			if (page > 0) {
				setCurrentPage(page);
			}
		}

		if (d.containsKey(TiC.PROPERTY_SHOW_PAGING_CONTROL)) {
			if (TiConvert.toBoolean(d, TiC.PROPERTY_SHOW_PAGING_CONTROL)) {
				showPager();
			}
		}

		if (d.containsKey(TiC.PROPERTY_SCROLLING_ENABLED)) {
			mEnabled = TiConvert.toBoolean(d, TiC.PROPERTY_SCROLLING_ENABLED);
		}
		
		if (d.containsKey(TiC.PROPERTY_OVER_SCROLL_MODE)) {
			if (Build.VERSION.SDK_INT >= 9) {
				mPager.setOverScrollMode(TiConvert.toInt(d.get(TiC.PROPERTY_OVER_SCROLL_MODE), View.OVER_SCROLL_ALWAYS));
			}
		}

		if (d.containsKey(TiC.PROPERTY_CACHE_SIZE)) {
			setCacheSize(TiConvert.toInt(d, TiC.PROPERTY_CACHE_SIZE));
		}
		
		if (d.containsKey(TiC.PROPERTY_PAGE_WIDTH)) {
			pageDimension = new TiDimension(d.getString(TiC.PROPERTY_PAGE_WIDTH), TiDimension.TYPE_WIDTH);
		}
		if (d.containsKey(TiC.PROPERTY_PAGE_OFFSET)) {
			pageOffset = new TiDimension(d.getString(TiC.PROPERTY_PAGE_OFFSET), TiDimension.TYPE_WIDTH);
		}
		super.processProperties(d);

	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue,
			KrollProxy proxy)
	{
		if (TiC.PROPERTY_CURRENT_PAGE.equals(key)) {
			setCurrentPage(TiConvert.toInt(newValue));
		} else if (TiC.PROPERTY_SHOW_PAGING_CONTROL.equals(key)) {
			boolean show = TiConvert.toBoolean(newValue);
			if (show) {
				showPager();
			} else {
				hidePager();
			}
		} else if (TiC.PROPERTY_SCROLLING_ENABLED.equals(key)) {
			mEnabled = TiConvert.toBoolean(newValue);
		} else if (TiC.PROPERTY_OVER_SCROLL_MODE.equals(key)){
			if (Build.VERSION.SDK_INT >= 9) {
				mPager.setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
			}
		} else if (TiC.PROPERTY_CACHE_SIZE.equals(key)){
			setCacheSize(TiConvert.toInt(newValue));
		} else if (TiC.PROPERTY_PAGE_WIDTH.equals(key)){
			pageDimension = new TiDimension(TiConvert.toString(newValue), TiDimension.TYPE_WIDTH);
		} else if (TiC.PROPERTY_PAGE_OFFSET.equals(key)){
			pageOffset = new TiDimension(TiConvert.toString(newValue), TiDimension.TYPE_WIDTH);
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
		}
	}

	public void addView(TiViewProxy proxy)
	{
		if (!mViews.contains(proxy)) {
			proxy.setActivity(proxy.getActivity());
			mViews.add(proxy);
			getProxy().setProperty(TiC.PROPERTY_VIEWS, mViews.toArray());
			mAdapter.notifyDataSetChanged();
		}
	}

	public void removeView(Object view)
	{
		if (view instanceof Number) {
			int viewIndex = TiConvert.toInt(view);
			if (viewIndex >= 0 && viewIndex < mViews.size()) {
				mViews.remove(viewIndex);
				getProxy().setProperty(TiC.PROPERTY_VIEWS, mViews.toArray());
				mAdapter.notifyDataSetChanged();
			}
		} else if (view instanceof TiViewProxy) {
			TiViewProxy proxy = (TiViewProxy)view;
			if (mViews.contains(proxy)) {
				mViews.remove(proxy);
				getProxy().setProperty(TiC.PROPERTY_VIEWS, mViews.toArray());
				mAdapter.notifyDataSetChanged();
			}
		}
		
	}

	public void showPager()
	{
		View v = null;
		v = mContainer.findViewById(PAGE_LEFT);
		if (v != null) {
			v.setVisibility(mCurIndex > 0 ? View.VISIBLE : View.INVISIBLE);
		}

		v = mContainer.findViewById(PAGE_RIGHT);
		if (v != null) {
			v.setVisibility(mCurIndex < (mViews.size() - 1) ? View.VISIBLE : View.INVISIBLE);
		}

		mPagingControl.setVisibility(View.VISIBLE);
		((ScrollableViewProxy) proxy).setPagerTimeout();
	}

	public void hidePager()
	{
		mPagingControl.setVisibility(View.INVISIBLE);
	}

	public void moveNext(boolean animated)
	{
		move(mCurIndex + 1, animated);
	}

	public void moveNext()
	{
		moveNext(true);
	}

	public void movePrevious(boolean animated)
	{
		move(mCurIndex - 1, animated);
	}

	public void movePrevious()
	{
		movePrevious(true);
	}

	private void move(int index, boolean animated)
	{
		if (index < 0 || index >= mViews.size()) {
			Log.w(TAG, "Request to move to index " + index+ " ignored, as it is out-of-bounds.");
			return;
		}
		mCurIndex = index;
		mPager.setCurrentItem(index, animated);
	}

	private void move(int index)
	{
		move(index, true);
	}

	public void scrollTo(Object view, boolean animated)
	{
		Log.i(TAG, "scrollTo " + animated);
		if (view instanceof Number) {
			move(((Number) view).intValue(), animated);
		} else if (view instanceof TiViewProxy) {
			move(mViews.indexOf(view), animated);
		}
	}
	public void scrollTo(Object view)
	{
		scrollTo(view, true);
	}

	public int getCurrentPage()
	{
		return mCurIndex;
	}

	public void setCurrentPage(Object view)
	{
		scrollTo(view, false);
	}

	public void setEnabled(Object value)
	{
		mEnabled = TiConvert.toBoolean(value);
	}

	public boolean getEnabled()
	{
		return mEnabled;
	}

	private void clearViewsList()
	{
		if (mViews == null || mViews.size() == 0) {
			return;
		}
		for (TiViewProxy viewProxy : mViews) {
			viewProxy.releaseViews();
		}
		mViews.clear();
	}

	public void setViews(Object viewsObject)
	{
		boolean changed = false;
		clearViewsList();

		if (viewsObject instanceof Object[]) {
			Object[] views = (Object[])viewsObject;
			Activity activity = proxy.getActivity();
			for (int i = 0; i < views.length; i++) {
				if (views[i] instanceof TiViewProxy) {
					TiViewProxy tv = (TiViewProxy)views[i];
					tv.setActivity(activity);
					mViews.add(tv);
					changed = true;
				}
			}
		}
		if (changed) {
			mAdapter.notifyDataSetChanged();
		}
	}

	public ArrayList<TiViewProxy> getViews()
	{
		return mViews;
	}

	@Override
	public void release()
	{
		if (mPager != null) {
			for (int i = mPager.getChildCount() - 1; i >=  0; i--) {
				mPager.removeViewAt(i);
			}
		}
		if (mViews != null) {
			for (TiViewProxy viewProxy : mViews) {
				viewProxy.releaseViews();
			}
			mViews.clear();
		}
		super.release();
	}

	public class HViewPagerAdapter extends PagerAdapter implements IPageAdapter
	{
		private final ArrayList<TiViewProxy> mViewProxies;
		private  HashMap<TiViewProxy, TiCompositeLayout> mHolders;
		public HViewPagerAdapter(ArrayList<TiViewProxy> viewProxies)
		{
			mViewProxies = viewProxies;
			mHolders = new HashMap<TiViewProxy, TiCompositeLayout>();
		}

		@Override
		public void destroyItem(View container, int position, Object object)
		{
			TiCompositeLayout layout = mHolders.get(object);
			if (layout != null) {
				((IViewPager) container).removeView(layout);
				mHolders.remove(object);
			}
			TiViewProxy tiProxy = (TiViewProxy) object;
			if (tiProxy != null) {
				tiProxy.releaseViews();
			}
		}

		@Override
		public void finishUpdate(View container) {}

		@Override
		public int getCount()
		{
			return mViewProxies.size();
		}
//
//		@Override
//		public float getPageWidth(int position) {
//	        return pageWidth;
//	    }

		@Override
		public Object instantiateItem(View container, int position)
		{
			IViewPager pager = (IViewPager) container;
			TiViewProxy tiProxy = mViewProxies.get(position);
			TiUIView tiView = tiProxy.getOrCreateView();
			View view = tiView.getOuterView();
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			TiCompositeLayout layout = new TiCompositeLayout(tiProxy.getActivity());
			ViewParent parent = view.getParent();
			if (parent instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) parent;
				group.removeView(view);
			}
			layout.addView(view, tiView.getLayoutParams());
			if (position < pager.getChildCount()) {
				pager.addView(layout, position, params);
			} else {
				pager.addView(layout, params);
			}
			mHolders.put(tiProxy, layout);
			return tiProxy;
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			TiCompositeLayout layout = mHolders.get(object);
			return (layout != null && layout.equals(view));
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {}

		@Override
		public Parcelable saveState() {return null;}

		@Override
		public void startUpdate(View container) {}

		@Override
		public int getItemPosition(Object object)
		{

			TiViewProxy proxy = (TiViewProxy) object;
//			if (proxy == null || needsRepopulate == true) return POSITION_NONE;
			if (proxy == null) return POSITION_NONE;

			for(int i = 0; i < getCount(); i++) {
				if(mViewProxies.get(i).equals(proxy)) {
					// item still exists in dataset; return position
					return i;
				}
			}
			// if we arrive here, the data-item for which the Proxy was created
			// does not exist anymore.
			proxy.releaseViews();

			return POSITION_NONE;
		}
	}
	
	public class VViewPagerAdapter extends com.lambergar.verticalviewpager.PagerAdapter  implements IPageAdapter
	{
		private final ArrayList<TiViewProxy> mViewProxies;
		private  HashMap<TiViewProxy, TiCompositeLayout> mHolders;
		public VViewPagerAdapter(ArrayList<TiViewProxy> viewProxies)
		{
			mViewProxies = viewProxies;
			mHolders = new HashMap<TiViewProxy, TiCompositeLayout>();
		}

		@Override
		public void destroyItem(View container, int position, Object object)
		{
			TiCompositeLayout layout = mHolders.get(object);
			if (layout != null) {
				((ViewPager) container).removeView(layout);
				mHolders.remove(object);
			}
			TiViewProxy tiProxy = (TiViewProxy) object;
			if (tiProxy != null) {
				tiProxy.releaseViews();
			}
		}

		@Override
		public void finishUpdate(View container) {}

		@Override
		public int getCount()
		{
			return mViewProxies.size();
		}

//		@Override
//		public float getPageHeight(int position) {
//	        return pageWidth;
//	    }

		@Override
		public Object instantiateItem(View container, int position)
		{
			IViewPager pager = (IViewPager) container;
			TiViewProxy tiProxy = mViewProxies.get(position);
			TiUIView tiView = tiProxy.getOrCreateView();
			View view = tiView.getOuterView();
			ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			TiCompositeLayout layout = new TiCompositeLayout(tiProxy.getActivity());
			ViewParent parent = view.getParent();
			if (parent instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) parent;
				group.removeView(view);
			}
			layout.addView(view, tiView.getLayoutParams());
			if (position < pager.getChildCount()) {
				pager.addView(layout, position, params);
			} else {
				pager.addView(layout, params);
			}
			mHolders.put(tiProxy, layout);
			return tiProxy;
		}

		@Override
		public boolean isViewFromObject(View view, Object object)
		{
			TiCompositeLayout layout = mHolders.get(object);
			return (layout != null && layout.equals(view));
		}

		@Override
		public void restoreState(Parcelable state, ClassLoader loader) {}

		@Override
		public Parcelable saveState() {return null;}

		@Override
		public void startUpdate(View container) {}

		@Override
		public int getItemPosition(Object object)
		{

			TiViewProxy proxy = (TiViewProxy) object;
			if (proxy == null) return POSITION_NONE;

			for(int i = 0; i < getCount(); i++) {
				if(mViewProxies.get(i).equals(proxy)) {
					// item still exists in dataset; return position
					return i;
				}
			}
			// if we arrive here, the data-item for which the Proxy was created
			// does not exist anymore.
			proxy.releaseViews();

			return POSITION_NONE;
		}
	}

	public class TiViewPagerLayout extends TiCompositeLayout
	{
		
		private boolean hardwareDisabled = false;
		public TiViewPagerLayout(Context context)
		{
			super(context, proxy);      
			setFocusable(true);
			setFocusableInTouchMode(true);
			setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);			
		}
		
		private Point mCenter = new Point();
	    private Point mInitialTouch = new Point();
	 
	    @Override
	    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
	        mCenter.x = w / 2;
	        mCenter.y = h / 2;
//	        float realDimension = pageDimension.getAsPixels(getContext(), parentWidth, parentWidth);
////		pageWidth = realDimension/parentWidth;
	    }
	    
	    @Override
	    protected void onLayout(boolean changed, int l, int t, int r, int b) {
	    	super.onLayout(changed, l, t, r, b);
	    	if (verticalLayout) {
	    		View view = (View) mPager;
	    		int height =  getMeasuredHeight();
	    		
	    		int realHeight = pageDimension.getAsPixels(getContext(), height, height);
	    		int offset = 0;
	    		if (realHeight != height) {
	    			if (hardwareDisabled == false) {
	    		        setClipChildren(false);
	    		        setLayerType(LAYER_TYPE_SOFTWARE, null);
	    		        hardwareDisabled = true;
	    			}
		    		offset = pageOffset.getAsPixels(getContext(), height - realHeight, height);
	    			int newWidthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(),
							MeasureSpec.EXACTLY);
					int newHeightSpec = MeasureSpec.makeMeasureSpec(realHeight,
							MeasureSpec.EXACTLY);
					view.measure(newWidthSpec, newHeightSpec);
	    		}
		    	view.layout(0, offset, getMeasuredWidth(),offset + realHeight);
	    	}
	    	else {
	    		View view = (View) mPager;
	    		int width =  getMeasuredWidth();
	    		
	    		int realWidth = pageDimension.getAsPixels(getContext(), width, width);
	    		int offset = 0;
	    		if (realWidth != width) {
	    			if (hardwareDisabled == false) {
	    		        setClipChildren(false);
	    		        setLayerType(LAYER_TYPE_SOFTWARE, null);
	    		        hardwareDisabled = true;
	    			}
		    		offset = pageOffset.getAsPixels(getContext(), width - realWidth, width);
	    			int newWidthSpec = MeasureSpec.makeMeasureSpec(realWidth,
							MeasureSpec.EXACTLY);
					int newHeightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(),
							MeasureSpec.EXACTLY);
					view.measure(newWidthSpec, newHeightSpec);
	    		}
		    	view.layout(offset, 0,offset + realWidth, getMeasuredHeight());
	    	}
	    }
	    
	    @Override
	    public boolean onTouchEvent(MotionEvent ev) {
	        //We capture any touches not already handled by the ViewPager
	        // to implement scrolling from a touch outside the pager bounds.
	        switch (ev.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	                mInitialTouch.x = (int)ev.getX();
	                mInitialTouch.y = (int)ev.getY();
	            case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					requestDisallowInterceptTouchEvent(false);
					break;
	            default:
	                ev.offsetLocation(mCenter.x - mInitialTouch.x, mCenter.y - mInitialTouch.y);
	                break;
	        }
	 
	        return ((View) mPager).onTouchEvent(ev);
	    }

		@Override
		public boolean onTrackballEvent(MotionEvent event)
		{
			// Any trackball activity should show the pager.
			if (shouldShowPager() && mPagingControl.getVisibility() != View.VISIBLE) {
				showPager();
			}
			return super.onTrackballEvent(event);
		}


		@Override
		public boolean dispatchTouchEvent(MotionEvent ev)
		{
			if (mTouchTarget != null) {
	            boolean wasProcessed = mTouchTarget.dispatchTouchEvent(ev);

	            if (!wasProcessed) {
	                mTouchTarget = null;
					requestDisallowInterceptTouchEvent(false);
	            }

	            return wasProcessed;
	        }
			// If inside a scroll view or a ListView, then we prevent the scroll view from intercepting touch events
//			if (TiUIHelper.isViewInsideViewOfClass(this, DISALLOWINTERCEPTTOUCH_CLASSES)) {
//				int action = ev.getAction();
//				switch (action) {
//					case MotionEvent.ACTION_DOWN:
//						requestDisallowInterceptTouchEvent(true);
//						break;
//
//					case MotionEvent.ACTION_UP:
//					case MotionEvent.ACTION_CANCEL:
//						requestDisallowInterceptTouchEvent(false);
//						break;
//				}
//			}
			return super.dispatchTouchEvent(ev);
		}

		@Override
		public boolean dispatchKeyEvent(KeyEvent event)
		{
			boolean handled = false;
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				switch (event.getKeyCode()) {
					case KeyEvent.KEYCODE_DPAD_LEFT: {
						movePrevious();
						handled = true;
						break;
					}
					case KeyEvent.KEYCODE_DPAD_RIGHT: {
						moveNext();
						handled = true;
						break;
					}
				}
			}
			return handled || super.dispatchKeyEvent(event);
		}
	}
	
//	@Override
//	public void onConfigurationChanged(TiBaseActivity activity,
//			Configuration newConfig) {
//		needsRepopulate = true;
//		
//	}
}
