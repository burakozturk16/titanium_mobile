/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiCompositeLayout;
import org.appcelerator.titanium.view.TiUIView;

import com.akylas.view.DualScrollView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

@SuppressLint("NewApi")
@SuppressWarnings({"unchecked", "rawtypes"})
public class TiUIScrollView extends TiUIView
{
	public static final int TYPE_VERTICAL = 0;
	public static final int TYPE_HORIZONTAL = 1;

	private static final String TAG = "TiUIScrollView";
	private int offsetX = 0, offsetY = 0;
	private boolean setInitialOffset = false;
	private boolean mScrollingEnabled = true;


	public class TiScrollViewLayout extends TiCompositeLayout
	{
		private static final int AUTO = Integer.MAX_VALUE;
		private int parentWidth = 0;
		private int parentHeight = 0;
		private boolean canCancelEvents = true;
		
	    protected int contentWidth = AUTO;
	    protected int contentHeight = AUTO;

		public TiScrollViewLayout(Context context)
		{
			super(context, TiUIScrollView.this);
		}

		public void setParentWidth(int width)
		{
			parentWidth = width;
		}

		public void setParentHeight(int height)
		{
			parentHeight = height;
		}

		public void setCanCancelEvents(boolean value)
		{
			canCancelEvents = value;
		}
		
		public void setClipChildren(boolean clipChildren) {
            super.setClipChildren(clipChildren);
            ((ViewGroup) nativeView).setClipChildren(clipChildren);
        }
        
        public void setClipToOutline(boolean clipChildren) {
            super.setClipToOutline(clipChildren);
            ((ViewGroup) nativeView).setClipToOutline(clipChildren);
        }

		@Override
		public boolean dispatchTouchEvent(MotionEvent ev)
		{
			// If canCancelEvents is false, then we want to prevent the scroll view from canceling the touch
			// events of the child view
			if (!canCancelEvents) {
				requestDisallowInterceptTouchEvent(true);
			}
			return super.dispatchTouchEvent(ev);
		}

		protected int getContentProperty(String property)
        {
            Object value = getProxy().getProperty(property);
            if (value != null) {
                if (value.equals(TiC.SIZE_AUTO)) {
                    return AUTO;
                } else if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else {
                    int type = 0;
                    TiDimension dimension;
                    if (TiC.PROPERTY_CONTENT_HEIGHT.equals(property)) {
                        type = TiDimension.TYPE_HEIGHT;
                    } else if (TiC.PROPERTY_CONTENT_WIDTH.equals(property)) {
                        type = TiDimension.TYPE_WIDTH;
                    }
                    dimension = new TiDimension(value.toString(), type);
                    return dimension.getUnits() == TiDimension.COMPLEX_UNIT_AUTO ? AUTO : dimension.getIntValue();
                }
            }
            return AUTO;
        }

		@Override
		protected int getWidthMeasureSpec(View child)
		{
//			int contentWidth = getContentProperty(TiC.PROPERTY_CONTENT_WIDTH);
			if (contentWidth == AUTO) {
				return MeasureSpec.UNSPECIFIED;
			} else {
				return super.getWidthMeasureSpec(child);
			}
		}

		@Override
		protected int getHeightMeasureSpec(View child)
		{
//			int contentHeight = getContentProperty(TiC.PROPERTY_CONTENT_HEIGHT);
			if (contentHeight == AUTO) {
				return MeasureSpec.UNSPECIFIED;
			} else {
				return super.getHeightMeasureSpec(child);
			}
		}

		@Override
		protected int getMeasuredWidth(int maxWidth, int widthSpec)
		{
		    int theWidth = contentWidth;
//			int contentWidth = getContentProperty(TiC.PROPERTY_CONTENT_WIDTH);
			if (theWidth == AUTO) {
			    theWidth = maxWidth; // measuredWidth;
			}		

			// Returns the content's width when it's greater than the scrollview's width
			if (theWidth > parentWidth) {
				return theWidth;
			} else {
				return resolveSize(maxWidth, widthSpec);
			}
		}

		@Override
		protected int getMeasuredHeight(int maxHeight, int heightSpec)
		{
            int theHeight = contentHeight;
			if (theHeight == AUTO) {
			    theHeight = maxHeight; // measuredHeight;
			}

			// Returns the content's height when it's greater than the scrollview's height
			if (theHeight > parentHeight) {
				return theHeight;
			} else {
				return resolveSize(maxHeight, heightSpec);
			}
		} 
	}

	// same code, different super-classes
	private class TiScrollView extends DualScrollView
	{
		private TiScrollViewLayout layout;

		public TiScrollView(Context context)
		{
			super(context);
			setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
			setScrollContainer(true);

			layout = new TiScrollViewLayout(context);
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
			layout.setLayoutParams(params);
			super.addView(layout, params);
		}

		public TiScrollViewLayout getLayout()
		{
			return layout;
		}


		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_MOVE && !mScrollingEnabled) {
				return false;
			}
			//There's a known Android bug (version 3.1 and above) that will throw an exception when we use 3+ fingers to touch the scrollview.
			//Link: http://code.google.com/p/android/issues/detail?id=18990
			try {
				return super.onTouchEvent(event);
			} catch (IllegalArgumentException e) {
				return false;
			}
		}
		
		@Override
		public boolean onInterceptTouchEvent(MotionEvent event) {
            if (mScrollingEnabled && isTouchEnabled) {
				return super.onInterceptTouchEvent(event);
			}

			return false;
		}
		
		@Override
		public void addView(View child, android.view.ViewGroup.LayoutParams params)
		{
			layout.addView(child, params);
		}

		public void onDraw(Canvas canvas)
		{
			super.onDraw(canvas);
			// setting offset once when this view is visible
			if (!setInitialOffset) {
				scrollTo(offsetX, offsetY);
				setInitialOffset = true;
			}

		}

		@Override
		protected void onScrollChanged(int l, int t, int oldl, int oldt)
		{
			super.onScrollChanged(l, t, oldl, oldt);
            setContentOffset(l, t);
			if (hasListeners(TiC.EVENT_SCROLL)) {
				getProxy().fireEvent(TiC.EVENT_SCROLL, getContentOffset(), false, false);
			}
		}

		@Override
		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
		{
			layout.setParentHeight(MeasureSpec.getSize(heightMeasureSpec));
			layout.setParentWidth(MeasureSpec.getSize(widthMeasureSpec));
			super.onMeasure(widthMeasureSpec, heightMeasureSpec);

			// This is essentially doing the same logic as if you did setFillViewPort(true). In native Android, they
			// don't measure the child again if measured height of content view < scrollViewheight. But we want to do
			// this in all cases since we allow the content view height to be greater than the scroll view. We force
			// this to allow fill behavior: TIMOB-8243.
			if (getChildCount() > 0) {
				final View child = getChildAt(0);
				int height = getMeasuredHeight();
				final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();

				int childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, getPaddingLeft() + getPaddingRight(),
					lp.width);
				height -= getPaddingTop();
				height -= getPaddingBottom();

				// If we measure the child height to be greater than the parent height, use it in subsequent
				// calculations to make sure the children are measured correctly the second time around.
				height = Math.max(child.getMeasuredHeight(), height);
				int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

			}
		}

         @Override
         public boolean dispatchTouchEvent(MotionEvent event) {
             if (touchPassThrough(this, event)) {
                 return false;
             }
             return super.dispatchTouchEvent(event);
         }
	}
	

	public View getParentViewForChild()
	{
		return getLayout();
	}
	
	@Override
	protected View getTouchView()
	{
		return getLayout();		
	}

//	private class TiHorizontalScrollView extends HorizontalScrollView
//	{
//		private TiScrollViewLayout layout;
//
//		public TiHorizontalScrollView(Context context, LayoutArrangement arrangement)
//		{
//			super(context);
//			setScrollBarStyle(SCROLLBARS_INSIDE_OVERLAY);
//			setScrollContainer(true);
//
//			layout = new TiScrollViewLayout(context, arrangement);
//			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//				ViewGroup.LayoutParams.MATCH_PARENT);
//			layout.setLayoutParams(params);
//			super.addView(layout, params);
//
//		}
//
//		public TiScrollViewLayout getLayout()
//		{
//			return layout;
//		}
//
//		@Override
//		public boolean onTouchEvent(MotionEvent event) {
//			if (event.getAction() == MotionEvent.ACTION_MOVE && !mScrollingEnabled) {
//				return false;
//			}
//			//There's a known Android bug (version 3.1 and above) that will throw an exception when we use 3+ fingers to touch the scrollview.
//			//Link: http://code.google.com/p/android/issues/detail?id=18990
//			try {
//				return super.onTouchEvent(event);
//			} catch (IllegalArgumentException e) {
//				return false;
//			}
//		}
//		
//		@Override
//		public boolean onInterceptTouchEvent(MotionEvent event) {
//			if (mScrollingEnabled) {
//				return super.onInterceptTouchEvent(event);
//			}
//
//			return false;
//		}
//
//		@Override
//		public void addView(View child, android.view.ViewGroup.LayoutParams params)
//		{
//			layout.addView(child, params);
//		}
//
//		public void onDraw(Canvas canvas)
//		{
//			super.onDraw(canvas);
//			// setting offset once this view is visible
//			if (!setInitialOffset) {
//				scrollTo(offsetX, offsetY);
//				setInitialOffset = true;
//			}
//
//		}
//
//		@Override
//		protected void onScrollChanged(int l, int t, int oldl, int oldt)
//		{
//			super.onScrollChanged(l, t, oldl, oldt);
//
//			KrollDict data = new KrollDict();
//			data.put(TiC.EVENT_PROPERTY_X, l);
//			data.put(TiC.EVENT_PROPERTY_Y, t);
//			setContentOffset(l, t);
//			getProxy().fireEvent(TiC.EVENT_SCROLL, data);
//		}
//
//		@Override
//		protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
//		{
//			layout.setParentHeight(MeasureSpec.getSize(heightMeasureSpec));
//			layout.setParentWidth(MeasureSpec.getSize(widthMeasureSpec));
//			super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//			// This is essentially doing the same logic as if you did setFillViewPort(true). In native Android, they
//			// don't measure the child again if measured width of content view < scroll view width. But we want to do
//			// this in all cases since we allow the content view width to be greater than the scroll view. We force this
//			// to allow fill behavior: TIMOB-8243.
//			if (getChildCount() > 0) {
//				final View child = getChildAt(0);
//				int width = getMeasuredWidth();
//				final FrameLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
//
//				int childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, getPaddingTop() + getPaddingBottom(),
//					lp.height);
//				width -= getPaddingLeft();
//				width -= getPaddingRight();
//
//				// If we measure the child width to be greater than the parent width, use it in subsequent
//				// calculations to make sure the children are measured correctly the second time around.
//				width = Math.max(child.getMeasuredWidth(), width);
//				int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
//
//				child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
//			}
//
//		}
//	}

	public TiUIScrollView(TiViewProxy proxy)
	{
		// we create the view after the properties are processed
		super(proxy);
        getLayoutParams().sizeOrFillWidthEnabled = true;
        getLayoutParams().autoFillsHeight = true;
        getLayoutParams().sizeOrFillHeightEnabled = true;
		getLayoutParams().autoFillsWidth = true;
		TiScrollView view = new TiScrollView(proxy.getActivity());
		setNativeView(view);
	}

	public void setContentOffset(int x, int y)
	{
		offsetX = x;
		offsetY = y;
	}

	public void setContentOffset(Object hashMap)
	{
		if (hashMap instanceof HashMap) {
			HashMap contentOffset = (HashMap) hashMap;
			
			offsetX = TiConvert.toInt(contentOffset, TiC.PROPERTY_X);
			offsetY = TiConvert.toInt(contentOffset, TiC.PROPERTY_Y);
		} else {
			Log.e(TAG, "ContentOffset must be an instance of HashMap");
		}
	}
	
	public KrollDict getContentOffset()
    {
	    KrollDict offset = new KrollDict();
	    TiDimension x = new TiDimension(offsetX, TiDimension.TYPE_WIDTH);
        TiDimension y = new TiDimension(offsetY, TiDimension.TYPE_HEIGHT);

        offset.put(TiC.EVENT_PROPERTY_X, x.getAsDefault(nativeView));
        offset.put(TiC.EVENT_PROPERTY_Y, y.getAsDefault(nativeView));
	    return offset;
    }

    
	
	public void setContentOffset(Object hashMap, boolean animated)
	{
		setContentOffset(hashMap);
		if (nativeView == null) return;
		((TiScrollView) nativeView).setShouldClamp(false);
		if (animated) {
			smoothScrollTo(offsetX, offsetY);
		}
		else {
			scrollTo(offsetX, offsetY);
		}
	}


	
	@Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_HORIZONTAL_WRAP:
            getLayout().setEnableHorizontalWrap(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_CONTENT_WIDTH:
            getLayout().contentWidth = getLayout().getContentProperty(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_CONTENT_HEIGHT:
            getLayout().contentHeight = getLayout().getContentProperty(TiConvert.toString(newValue));
            break;
        case TiC.PROPERTY_SCROLLING_ENABLED:
            mScrollingEnabled = TiConvert.toBoolean(newValue, true);
            break;
        case TiC.PROPERTY_SHOW_HORIZONTAL_SCROLL_INDICATOR:
            getNativeView().setHorizontalScrollBarEnabled(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR:
            getNativeView().setVerticalScrollBarEnabled(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_OVER_SCROLL_MODE:
            getNativeView().setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
            break;
        case TiC.PROPERTY_CAN_CANCEL_EVENTS:
            ((TiScrollView) getNativeView()).getLayout().setCanCancelEvents(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_CONTENT_OFFSET:
            setContentOffset(newValue);
            ((TiScrollView) getNativeView()).setShouldClamp(false);
            scrollTo(offsetX, offsetY);
            break;
        
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	public TiScrollViewLayout getLayout()
	{
		if (nativeView != null){
			return ((TiScrollView) nativeView).layout;
		} 
		return null;
	}
	
	@Override
	protected void setOnClickListener(View view)
	{
		View targetView = view;
		// Get the layout and attach the listeners to it
//		if (view instanceof TiVerticalScrollView) {
//			targetView = ((TiVerticalScrollView) nativeView).layout;
//		}
//		if (view instanceof TiHorizontalScrollView) {
			targetView = ((TiScrollView) nativeView).layout;
//		}
		super.setOnClickListener(targetView);
	}


	public boolean getScrollingEnabled()
	{
		return mScrollingEnabled;
	}

	public void scrollTo(int x, int y)
	{
		getNativeView().scrollTo(x, y);
		getNativeView().computeScroll();
	}

	public void smoothScrollTo(int x, int y)
	{
		((TiScrollView)getNativeView()).smoothScrollTo(x, y);
	}

	public void scrollToBottom()
	{
		View view = getNativeView();
//		if (view instanceof TiHorizontalScrollView) {
//			TiHorizontalScrollView scrollView = (TiHorizontalScrollView) view;
//			scrollView.fullScroll(View.FOCUS_RIGHT);
//		} else if (view instanceof TiVerticalScrollView) {
			TiScrollView scrollView = (TiScrollView) view;
			scrollView.fullScroll(View.FOCUS_DOWN, false);
//		}
	}

	@Override
	public void add(TiUIView child, int index)
	{
		if (child.hWAccelerationDisabled()) {
			hardwareAccEnabled = false;
			disableHWAcceleration(getOuterView());	
		}
		super.add(child, index);

		if (getNativeView() != null) {
			getLayout().requestLayout();
			if (child.getNativeView() != null) {
				child.getNativeView().requestLayout();
			}
		}
	}

	@Override
	public void remove(TiUIView child)
	{
		if (child != null) {
			View cv = child.getOuterView();
			if (cv != null) {
				View nv = getLayout();
				if (nv instanceof ViewGroup) {
					((ViewGroup) nv).removeView(cv);
					children.remove(child);
					child.setParent(null);
				}
			}
		}
	}
	
	@Override
	public void resort()
	{
		View v = getLayout();
		if ( v instanceof TiCompositeLayout) {
			((TiCompositeLayout) v).resort();
		}
 	}

}
