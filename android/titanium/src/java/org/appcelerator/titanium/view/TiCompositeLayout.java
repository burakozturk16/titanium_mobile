/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.view;

import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.List;
import java.util.ArrayList;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiLaunchActivity;
import org.appcelerator.titanium.util.TiUIHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.OnHierarchyChangeListener;
import android.view.MotionEvent;

/**
 * Base layout class for all Titanium views.
 */
public class TiCompositeLayout extends FreeLayout implements
		OnHierarchyChangeListener {
	/**
	 * Supported layout arrangements
	 * 
	 * @module.api
	 */
	public enum LayoutArrangement {
		/**
		 * The default Titanium layout arrangement.
		 */
		DEFAULT,
		/**
		 * The layout arrangement for Views and Windows that set layout:
		 * "vertical".
		 */
		VERTICAL,
		/**
		 * The layout arrangement for Views and Windows that set layout:
		 * "horizontal".
		 */
		HORIZONTAL
	}

	protected static final String TAG = "TiCompositeLayout";

	public static final int NOT_SET = Integer.MIN_VALUE;

	private TreeSet<View> viewSorter;
	private boolean needsSort;
	protected LayoutArrangement arrangement;

	// Used by horizonal arrangement calculations
	private int horizontalLayoutTopBuffer = 0;
	private int horizontalLayoutCurrentLeft = 0;
	private int horizontalLayoutLineHeight = 0;
	private boolean enableHorizontalWrap = false;
	private int horizontalLayoutLastIndexBeforeWrap = 0;
	private int horiztonalLayoutPreviousRight = 0;

	private WeakReference<TiUIView> view;
	private static final int HAS_SIZE_FILL_CONFLICT = 1;
	private static final int NO_SIZE_FILL_CONFLICT = 2;
	
	private TiDimension mMaxWidth = null;
	private TiDimension mMaxHeight = null;
	private TiDimension mMinWidth = null;
	private TiDimension mMinHeight = null;

	// We need these two constructors for backwards compatibility with modules

	/**
	 * Constructs a new TiCompositeLayout object.
	 * 
	 * @param context
	 *            the associated context.
	 * @module.api
	 */
	public TiCompositeLayout(Context context) {
		this(context, LayoutArrangement.DEFAULT, null);
	}

	/**
	 * Constructs a new TiCompositeLayout object.
	 * 
	 * @param context
	 *            the associated context.
	 * @param arrangement
	 *            the associated LayoutArrangement
	 * @module.api
	 */
	public TiCompositeLayout(Context context, LayoutArrangement arrangement) {
		this(context, LayoutArrangement.DEFAULT, null);
	}

	public TiCompositeLayout(Context context, AttributeSet set) {
		this(context, LayoutArrangement.DEFAULT, null);
	}

	/**
	 * Constructs a new TiCompositeLayout object.
	 * 
	 * @param context
	 *            the associated context.
	 * @param proxy
	 *            the associated proxy.
	 */
	public TiCompositeLayout(Context context, TiUIView view) {
		this(context, LayoutArrangement.DEFAULT, view);
	}

	/**
	 * Constructs a new TiCompositeLayout object.
	 * 
	 * @param context
	 *            the associated context.
	 * @param arrangement
	 *            the associated LayoutArrangement
	 * @param proxy
	 *            the associated proxy.
	 */
	public TiCompositeLayout(Context context, LayoutArrangement arrangement,
			TiUIView view) {
		super(context);
		this.arrangement = arrangement;
		this.viewSorter = new TreeSet<View>(new Comparator<View>() {

			public int compare(View o1, View o2) {
				TiCompositeLayout.LayoutParams p1 = (TiCompositeLayout.LayoutParams) o1
						.getLayoutParams();
				TiCompositeLayout.LayoutParams p2 = (TiCompositeLayout.LayoutParams) o2
						.getLayoutParams();

				int result = 0;

				if (p1.optionZIndex != NOT_SET && p2.optionZIndex != NOT_SET) {
					if (p1.optionZIndex < p2.optionZIndex) {
						result = -1;
					} else if (p1.optionZIndex > p2.optionZIndex) {
						result = 1;
					}
				} else if (p1.optionZIndex != NOT_SET) {
					if (p1.optionZIndex < 0) {
						result = -1;
					}
					if (p1.optionZIndex > 0) {
						result = 1;
					}
				} else if (p2.optionZIndex != NOT_SET) {
					if (p2.optionZIndex < 0) {
						result = 1;
					}
					if (p2.optionZIndex > 0) {
						result = -1;
					}
				}

				if (result == 0) {
					if (p1.index < p2.index) {
						result = -1;
					} else if (p1.index > p2.index) {
						result = 1;
					} else {
						Log.w(TAG, "Ambiguous Z-Order");
						// throw new IllegalStateException("Ambiguous Z-Order");
					}
				}

				return result;
			}
		});

		setNeedsSort(true);
		setOnHierarchyChangeListener(this);
		this.view = new WeakReference<TiUIView>(view);
	}

	private String viewToString(View view) {
		return view.getClass().getSimpleName() + "@"
				+ Integer.toHexString(view.hashCode());
	}

	public void resort() {
		if (getVisibility() == View.INVISIBLE || getVisibility() == View.GONE)
			return;
		setNeedsSort(true);
		requestLayout();
		invalidate();
	}

	public void onChildViewAdded(View parent, View child) {
		setNeedsSort(true);
		if (Log.isDebugModeEnabled() && parent != null && child != null) {
			Log.d(TAG, "Attaching: " + viewToString(child) + " to "
					+ viewToString(parent), Log.DEBUG_MODE);
		}
	}

	public void onChildViewRemoved(View parent, View child) {
		setNeedsSort(true);
		if (Log.isDebugModeEnabled()) {
			Log.d(TAG, "Removing: " + viewToString(child) + " from "
					+ viewToString(parent), Log.DEBUG_MODE);
		}
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
		return p instanceof TiCompositeLayout.LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	private static int getAsPercentageValue(double percentage, int value) {
		return (int) Math.round((percentage / 100.0) * value);
	}

	protected int getViewWidthPadding(View child, LayoutParams params, View parent) {
		int padding = 0;
		padding += getLayoutOptionAsPixels(params.optionLeft, TiDimension.TYPE_LEFT, params, parent);
		padding += getLayoutOptionAsPixels(params.optionRight, TiDimension.TYPE_RIGHT, params, parent);
		return padding;
	}

	protected int getViewHeightPadding(View child, LayoutParams params, View parent) {
		int padding = 0;
		padding += getLayoutOptionAsPixels(params.optionTop, TiDimension.TYPE_TOP, params, parent);
		padding += getLayoutOptionAsPixels(params.optionBottom, TiDimension.TYPE_BOTTOM, params, parent);
		return padding;
	}
	
	private boolean viewShouldFillHorizontalLayout(View view, LayoutParams params)
	{
		if (params.sizeOrFillWidthEnabled == false) return false;
		if (params.autoFillsWidth) return true;
		boolean borderView = (view instanceof TiBorderWrapperView);
		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup)view;
	        for (int i=0; i<viewGroup.getChildCount(); i++) {
	            View child = viewGroup.getChildAt(i);
	        	ViewGroup.LayoutParams childParams = borderView?params:child.getLayoutParams();
	        	if (childParams instanceof LayoutParams && viewShouldFillHorizontalLayout(child, (LayoutParams) childParams)) {
	        		return true;
	        	}
	        }
		}
		return false;
	}
	
	private boolean viewShouldFillVerticalLayout(View view, LayoutParams params)
	{
		if (params.sizeOrFillHeightEnabled == false) return false;
		if (params.autoFillsHeight) return true;
		if (view instanceof ViewGroup) {
			ViewGroup viewGroup = (ViewGroup)view;
	        for (int i=0; i<viewGroup.getChildCount(); i++) {
	            View child = viewGroup.getChildAt(i);
	        	ViewGroup.LayoutParams childParams = child.getLayoutParams();
	        	if (childParams instanceof LayoutParams && viewShouldFillVerticalLayout(child, (LayoutParams) childParams)) {
	        		return true;
	        	}
	        }
		}
		return false;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int childCount = getChildCount();
		int wFromSpec = MeasureSpec.getSize(widthMeasureSpec);
		int hFromSpec = MeasureSpec.getSize(heightMeasureSpec);
		int wSuggested = getSuggestedMinimumWidth();
		int hSuggested = getSuggestedMinimumHeight();
		int w = Math.max(wFromSpec, wSuggested);
		int wMode = MeasureSpec.getMode(widthMeasureSpec);
		int h = Math.max(hFromSpec, hSuggested);
		int hMode = MeasureSpec.getMode(heightMeasureSpec);

		int maxWidth = 0;
		int maxHeight = 0;

		// Used for horizontal layout only
		int horizontalRowWidth = 0;
		int horizontalRowHeight = 0;

		// we need to first get the list/number of autoFillsWidth views
		List<View> autoFillWidthViews = new ArrayList<View>();
		List<View> autoFillHeightViews = new ArrayList<View>();

		boolean horizontal = isHorizontalArrangement();
		boolean horizontalNoWrap = horizontal && !enableHorizontalWrap;
		boolean horizontalWrap = horizontal && enableHorizontalWrap;
		boolean vertical = isVerticalArrangement();
		for (int i = 0; i < childCount; i++) {
			View child = getChildAt(i);
			if (child.getVisibility() == View.INVISIBLE
					|| child.getVisibility() == View.GONE) {
				continue;
			}
			TiCompositeLayout.LayoutParams params = (TiCompositeLayout.LayoutParams) child
					.getLayoutParams();
			Boolean needsProcessing = true;
			if (horizontalNoWrap && viewShouldFillHorizontalLayout(child, params)) {
				autoFillWidthViews.add(child);
				needsProcessing = false;
			}
			if ((vertical || horizontalWrap) && viewShouldFillVerticalLayout(child, params)) {
				autoFillHeightViews.add(child);
				needsProcessing = false;
			}

			if (!needsProcessing)
				continue;
			
			int widthPadding = getViewWidthPadding(child, params, this);
			int heightPadding = getViewHeightPadding(child, params, this);
			constrainChild(child, params, enableHorizontalWrap?w:(w - horizontalRowWidth), wMode, h, hMode, widthPadding, heightPadding);

			int childWidth = child.getMeasuredWidth() + widthPadding;
			int childHeight = child.getMeasuredHeight() + heightPadding;

			if (horizontal) {
				if (enableHorizontalWrap) {

					if ((horizontalRowWidth + childWidth) > w) {
						horizontalRowWidth = childWidth;
						maxHeight += horizontalRowHeight;
						horizontalRowHeight = childHeight;

					} else {
						horizontalRowWidth += childWidth;
						maxWidth = Math.max(maxWidth, horizontalRowWidth);
					}

				} else {

					// For horizontal layout without wrap, just keep on adding
					// the widths since it doesn't wrap
					maxWidth += childWidth;
				}
				horizontalRowHeight = Math
						.max(horizontalRowHeight, childHeight);

			} else {
				maxWidth = Math.max(maxWidth, childWidth);

				if (vertical) {
					maxHeight += childHeight;
				} else {
					maxHeight = Math.max(maxHeight, childHeight);
				}
			}
		}
		int countFillWidth = autoFillWidthViews.size() ;
		if (countFillWidth > 0) {
			for (int i = 0; i < countFillWidth; i++) {
				int childW = (w - maxWidth) / (countFillWidth - i);
				View child = autoFillWidthViews.get(i);
				TiCompositeLayout.LayoutParams params = (TiCompositeLayout.LayoutParams) child
						.getLayoutParams();
				
				int widthPadding = getViewWidthPadding(child, params, this);
				int heightPadding = getViewHeightPadding(child, params, this);
				constrainChild(child, params, childW, wMode, h, hMode, widthPadding, heightPadding);
				int childWidth = child.getMeasuredWidth() + widthPadding;
				int childHeight = child.getMeasuredHeight() + heightPadding;
				
				maxWidth += childWidth;
				horizontalRowHeight = Math
						.max(horizontalRowHeight, childHeight);
			}
		}

		int countFillHeight = autoFillHeightViews.size() ;
		if (countFillHeight > 0) {
			for (int i = 0; i < countFillHeight; i++) {
				int childH = (h - maxHeight) / (countFillHeight - i);
				View child = autoFillHeightViews.get(i);
				TiCompositeLayout.LayoutParams params = (TiCompositeLayout.LayoutParams) child
						.getLayoutParams();
				
				int widthPadding = getViewWidthPadding(child, params, this);
				int heightPadding = getViewHeightPadding(child, params, this);
				constrainChild(child, params, w, wMode, childH, hMode, widthPadding, heightPadding);
				int childWidth = child.getMeasuredWidth() + widthPadding;
				int childHeight = child.getMeasuredHeight() + heightPadding;
				
				maxHeight += childHeight;
				maxWidth = Math.max(maxWidth, childWidth);
			}
		}

		// Add height for last row in horizontal layout
		if (horizontal) {
			maxHeight += horizontalRowHeight;
		}

		// account for padding
		maxWidth += getPaddingLeft() + getPaddingRight();
		maxHeight += getPaddingTop() + getPaddingBottom();
		
		ViewGroup.LayoutParams params = getLayoutParams();
		LayoutParams tiParams = (params instanceof LayoutParams)?(LayoutParams)params:null;
		if (tiParams != null) {
			//if we are fill we need to fill ….
			if (tiParams.optionWidth == null && tiParams.autoFillsWidth) {
				maxWidth = w;
			}
			if (tiParams.optionHeight == null && tiParams.autoFillsHeight) {
				maxHeight = h;
			}
			
		}
		
		// check minimums
		maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());
		maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
		
		
		int measuredWidth = getMeasuredWidth(maxWidth, widthMeasureSpec);
		int measuredHeight = getMeasuredHeight(maxHeight, heightMeasureSpec);
		
		if (mMaxWidth != null) {
			measuredWidth = Math.min(measuredWidth, mMaxWidth.getAsPixels(getContext(), w, h));
			widthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY);
		}
		if (mMaxWidth != null || mMinWidth != null) {
			int minMeasuredWidth = measuredWidth;
			if (mMinWidth != null) minMeasuredWidth = Math.max(minMeasuredWidth, mMinWidth.getAsPixels(getContext(), w, h));
			if (mMaxWidth != null) minMeasuredWidth = Math.min(minMeasuredWidth, mMaxWidth.getAsPixels(getContext(), w, h));
			if (minMeasuredWidth != measuredHeight) {
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(minMeasuredWidth, MeasureSpec.EXACTLY);
				measuredHeight = getMeasuredHeight(minMeasuredWidth, widthMeasureSpec);
			}
		}

		if (mMaxHeight != null || mMinHeight != null) {
			int minMeasuredHeight = measuredHeight;
			if (mMinHeight != null) minMeasuredHeight = Math.max(minMeasuredHeight, mMinHeight.getAsPixels(getContext(), w, h));
			if (mMaxHeight != null) minMeasuredHeight = Math.min(minMeasuredHeight, mMaxHeight.getAsPixels(getContext(), w, h));
			if (minMeasuredHeight != measuredHeight) {
				heightMeasureSpec = MeasureSpec.makeMeasureSpec(minMeasuredHeight, MeasureSpec.EXACTLY);
				measuredHeight = getMeasuredHeight(minMeasuredHeight, heightMeasureSpec);
			}
		}


		setMeasuredDimension(measuredWidth, measuredHeight);
	}

	protected void constrainChild(View child, LayoutParams p, int width, int wMode, int height,
			int hMode, int widthPadding, int heightPadding) {

		int sizeFillConflicts[] = { NOT_SET, NOT_SET };
		boolean checkedForConflict = false;

		// If autoFillsWidth is false, and optionWidth is null, then we use size
		// behavior.
		int childDimension = LayoutParams.WRAP_CONTENT;
		if (p.optionWidth != null) {
			if (p.optionWidth.isUnitPercent() && width > 0) {
				childDimension = getAsPercentageValue(p.optionWidth.getValue(),
						width);
			} else {
				childDimension = p.optionWidth.getAsPixels(this);
			}
		} else {
			if (p.autoFillsWidth) {
				childDimension = LayoutParams.MATCH_PARENT;
			} else {
				// Look for sizeFill conflicts
				hasSizeFillConflict(child, sizeFillConflicts, true);
				checkedForConflict = true;
				if (sizeFillConflicts[0] == HAS_SIZE_FILL_CONFLICT) {
					childDimension = LayoutParams.MATCH_PARENT;
				}
			}
		}

		int widthSpec = ViewGroup.getChildMeasureSpec(
				MeasureSpec.makeMeasureSpec(width, wMode), widthPadding,
				childDimension);
		// If autoFillsHeight is false, and optionHeight is null, then we use
		// size behavior.
		childDimension = LayoutParams.WRAP_CONTENT;
		if (p.optionHeight != null) {
			if (p.optionHeight.isUnitPercent() && height > 0) {
				childDimension = getAsPercentageValue(
						p.optionHeight.getValue(), height);
			} else {
				childDimension = p.optionHeight.getAsPixels(this);
			}
		} else {
			// If we already checked for conflicts before, we don't need to
			// again
			if (p.autoFillsHeight
					|| (checkedForConflict && sizeFillConflicts[1] == HAS_SIZE_FILL_CONFLICT)) {
				childDimension = LayoutParams.MATCH_PARENT;
			} else if (!checkedForConflict) {
				hasSizeFillConflict(child, sizeFillConflicts, true);
				if (sizeFillConflicts[1] == HAS_SIZE_FILL_CONFLICT) {
					childDimension = LayoutParams.MATCH_PARENT;
				}
			}
		}

		int heightSpec = ViewGroup.getChildMeasureSpec(
				MeasureSpec.makeMeasureSpec(height, hMode), heightPadding,
				childDimension);

		child.measure(widthSpec, heightSpec);
		int wid = child.getMeasuredWidth();
		int hei = child.getMeasuredHeight();
		
		if (p instanceof AnimationLayoutParams) {
			float fraction = ((AnimationLayoutParams) p).animationFraction;
			if (fraction < 1.0f) {
				Rect startRect = ((AnimationLayoutParams) p).startRect;
				if (startRect != null) {
					int childWidth = child.getMeasuredWidth();
					int childHeight = child.getMeasuredHeight();
					childWidth = (int) (childWidth * fraction + (1 - fraction)
							* startRect.width());
					childHeight = (int) (childHeight * fraction + (1 - fraction)
							* startRect.height());
					
					int newWidthSpec = MeasureSpec.makeMeasureSpec(childWidth,
							MeasureSpec.EXACTLY);
					int newHeightSpec = MeasureSpec.makeMeasureSpec(childHeight,
							MeasureSpec.EXACTLY);
					child.measure(newWidthSpec, newHeightSpec);
				}
			}
		}
	}

	// Try to calculate width from pins, if we couldn't calculate from pins or
	// we don't need to, then return the
	// measured width
	private int calculateWidthFromPins(LayoutParams params, int parentLeft,
			int parentRight, int parentWidth, int measuredWidth, boolean canResizeFill) {
		int width = measuredWidth;

		if (params.optionWidth != null || params.sizeOrFillWidthEnabled) {
			if (canResizeFill && params.sizeOrFillWidthEnabled && params.autoFillsWidth) {
				return parentWidth - getLayoutOptionAsPixels(params.optionRight, TiDimension.TYPE_RIGHT, params, this) -
						getLayoutOptionAsPixels(params.optionLeft, TiDimension.TYPE_LEFT , params, this);
			}
			else 
				return width;
		}

		TiDimension left = params.optionLeft;
		TiDimension centerX = params.optionCenterX;
		TiDimension right = params.optionRight;

		if (left != null) {
			if (centerX != null) {
				width = (getLayoutOptionAsPixels(centerX, TiDimension.TYPE_CENTER_X, params, this) -
						getLayoutOptionAsPixels(left, TiDimension.TYPE_LEFT, params, this) - parentLeft) * 2;
			} else if (right != null) {
				width = parentWidth - getLayoutOptionAsPixels(right, TiDimension.TYPE_RIGHT, params, this) -
						getLayoutOptionAsPixels(left, TiDimension.TYPE_LEFT, params, this);
			}
		} else if (centerX != null && right != null) {
			width =  (parentRight - getLayoutOptionAsPixels(right, TiDimension.TYPE_RIGHT, params, this) -
					getLayoutOptionAsPixels(centerX, TiDimension.TYPE_CENTER_X , params, this)) * 2;
		}
		return width;
	}

	// Try to calculate height from pins, if we couldn't calculate from pins or
	// we don't need to, then return the
	// measured height
	private int calculateHeightFromPins(LayoutParams params, int parentTop,
			int parentBottom, int parentHeight, int measuredHeight, boolean canResizeFill) {
		int height = measuredHeight;

		// Return if we don't need undefined behavior
		if (params.optionHeight != null || params.sizeOrFillHeightEnabled) {
			if (canResizeFill && params.sizeOrFillHeightEnabled && params.autoFillsHeight) {
				return parentHeight - getLayoutOptionAsPixels(params.optionTop, TiDimension.TYPE_TOP, params, this) -
						getLayoutOptionAsPixels(params.optionBottom, TiDimension.TYPE_BOTTOM , params, this);
			}
			else 
				return height;
		}

		TiDimension top = params.optionTop;
		TiDimension centerY = params.optionCenterY;
		TiDimension bottom = params.optionBottom;

		if (top != null) {
			if (centerY != null) {
				height = (getLayoutOptionAsPixels(centerY, TiDimension.TYPE_CENTER_Y, params, this) -
						getLayoutOptionAsPixels(top, TiDimension.TYPE_TOP, params, this) - parentTop) * 2;
			} else if (bottom != null) {
				height = parentHeight - getLayoutOptionAsPixels(top, TiDimension.TYPE_TOP, params, this) -
						getLayoutOptionAsPixels(bottom, TiDimension.TYPE_BOTTOM,  params, this);
			}
		} else if (centerY != null && bottom != null) {
			height =  (parentBottom - getLayoutOptionAsPixels(bottom, TiDimension.TYPE_BOTTOM, params, this) -
					getLayoutOptionAsPixels(centerY, TiDimension.TYPE_CENTER_Y , params, this)) * 2;
		}

		return height;
	}

	protected int getMeasuredWidth(int maxWidth, int widthSpec) {
		return resolveSize(maxWidth, widthSpec);
	}

	protected int getMeasuredHeight(int maxHeight, int heightSpec) {
		return resolveSize(maxHeight, heightSpec);
	}

	public int getChildSize(View child, TiCompositeLayout.LayoutParams params,
			int left, int top, int bottom, int right, int currentHeight,
			int[] horizontal, int[] vertical) {

		if (child.getVisibility() == View.GONE
				|| child.getVisibility() == View.INVISIBLE)
		{
			return currentHeight;
		}

		int i = indexOfChild(child);
		// Dimension is required from Measure. Positioning is determined here.

		int childMeasuredHeight = child.getMeasuredHeight();
		int childMeasuredWidth = child.getMeasuredWidth();

		if (isHorizontalArrangement()) {
			computeHorizontalLayoutPosition(params, childMeasuredWidth,
					childMeasuredHeight, right, top, bottom, horizontal,
					vertical, i);

		} else {
			boolean verticalArr = isVerticalArrangement();
			// Try to calculate width/height from pins, and default to measured
			// width/height. We have to do this in
			// onLayout since we can't get the correct top, bottom, left, and
			// right values inside constrainChild().
			childMeasuredHeight = calculateHeightFromPins(params, top, bottom,
					getHeight(), childMeasuredHeight, !verticalArr);
			childMeasuredWidth = calculateWidthFromPins(params, left, right,
					getWidth(), childMeasuredWidth, true);

			computePosition(this, params.optionLeft, params.optionCenterX,
					params.optionRight, childMeasuredWidth, left, right,
					horizontal);
			if (verticalArr) {
				computeVerticalLayoutPosition(currentHeight, params.optionTop,
						childMeasuredHeight, top, vertical, bottom, params);
				// Include bottom in height calculation for vertical layout
				// (used as padding)
				currentHeight +=  (params.optionBottom != null)?params.optionBottom.getAsPixels(this):0;
			} else {
				computePosition(this, params.optionTop, params.optionCenterY,
						params.optionBottom, childMeasuredHeight, top, bottom,
						vertical);
				//we dont need to use AnimationLayoutParams as the fraction has already been applied in
				// the onMeasure
				if (params instanceof AnimationLayoutParams) {
					float fraction = ((AnimationLayoutParams) params).animationFraction;
					if (fraction < 1.0f) {
						Rect startRect = ((AnimationLayoutParams) params).startRect;
						if (startRect != null) {
							horizontal[0] = (int) (horizontal[0] * fraction + (1 - fraction)
									* startRect.left);
							horizontal[1] = horizontal[0] + childMeasuredWidth;

							vertical[0] = (int) (vertical[0] * fraction + (1 - fraction)
									* startRect.top);
							vertical[1] = vertical[0] + childMeasuredHeight;
						}
					}
				}
			}
			
			
		}

		Log.d(TAG, child.getClass().getName() + " {" + horizontal[0] + ","
				+ vertical[0] + "," + horizontal[1] + "," + vertical[1] + "}",
				Log.DEBUG_MODE);

		return currentHeight;
	}

	private void resetHorizontalLayout(int left, int right) {
		horizontalLayoutCurrentLeft = left;
		horizontalLayoutLineHeight = 0;
		horizontalLayoutTopBuffer = 0;
		horizontalLayoutLastIndexBeforeWrap = 0;
		horiztonalLayoutPreviousRight = 0;
		updateRowForHorizontalWrap(right, 0);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		int count = getChildCount();

		int left = 0;
		int top = 0;
		int right = r - l;
		int bottom = b - t;

		if (needsSort) {
			viewSorter.clear();
			if (count > 1) { // No need to sort one item.
				for (int i = 0; i < count; i++) {
					View child = getChildAt(i);
					if (child == null || child.getVisibility() == View.GONE
							|| child.getVisibility() == View.INVISIBLE)
						continue;
					TiCompositeLayout.LayoutParams params = (TiCompositeLayout.LayoutParams) child
							.getLayoutParams();
					params.index = i;
					viewSorter.add(child);
				}

				detachAllViewsFromParent();
				int i = 0;
				for (View child : viewSorter) {
					attachViewToParent(child, i++, child.getLayoutParams());
				}
			}
			setNeedsSort(false);
		}
		// viewSorter is not needed after this. It's a source of
		// memory leaks if it retains the views it's holding.
		viewSorter.clear();

		int[] horizontal = new int[2];
		int[] vertical = new int[2];

		int currentHeight = 0; // Used by vertical arrangement calcs
		
		resetHorizontalLayout(left, right);
		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			if (child == null || child.getVisibility() == View.GONE
					|| child.getVisibility() == View.INVISIBLE)
				continue;

			TiCompositeLayout.LayoutParams params = (LayoutParams) child
					.getLayoutParams();

			currentHeight = getChildSize(child, params, left, top, bottom,
					right, currentHeight, horizontal, vertical);

			if (!TiApplication.getInstance().isRootActivityAvailable()) {
				Activity currentActivity = TiApplication
						.getAppCurrentActivity();
				if (currentActivity instanceof TiLaunchActivity) {
					if (!((TiLaunchActivity) currentActivity).isJSActivity()) {
						Log.w(TAG,
								"The root activity is no longer available.  Skipping layout pass.",
								Log.DEBUG_MODE);
						return;
					}
				}
			}


			int newWidth = horizontal[1] - horizontal[0];
			int newHeight = vertical[1] - vertical[0];
			// If the old child measurements do not match the new measurements
			// that we calculated, then update the
			// child measurements accordingly
			if (newWidth != child.getMeasuredWidth()
					|| newHeight != child.getMeasuredHeight()) {
				int newWidthSpec = MeasureSpec.makeMeasureSpec(newWidth,
						MeasureSpec.EXACTLY);
				int newHeightSpec = MeasureSpec.makeMeasureSpec(newHeight,
						MeasureSpec.EXACTLY);
				child.measure(newWidthSpec, newHeightSpec);
			}
			child.layout(horizontal[0], vertical[0], horizontal[1], vertical[1]);
			currentHeight += newHeight;
	
			currentHeight += getLayoutOptionAsPixels(params.optionTop, TiDimension.TYPE_TOP, params, this);
		}

		TiUIView view = (this.view == null ? null : this.view.get());
		TiUIHelper.firePostLayoutEvent(view);

	}

	// option0 is left/top, option1 is right/bottom
	public static void computePosition(View parent, TiDimension leftOrTop,
			TiDimension optionCenter, TiDimension rightOrBottom,
			int measuredSize, int layoutPosition0, int layoutPosition1,
			int[] pos) {
		int dist = layoutPosition1 - layoutPosition0;
		if (leftOrTop != null && !leftOrTop.isUnitUndefined()) {
			// peg left/top
			int leftOrTopPixels = leftOrTop.getAsPixels(parent);
			pos[0] = layoutPosition0 + leftOrTopPixels;
			pos[1] = layoutPosition0 + leftOrTopPixels + measuredSize;
		} else if (optionCenter != null && !optionCenter.isUnitUndefined()
				&& optionCenter.getValue() != 0.0) {
			// Don't calculate position based on center dimension if it's 0.0
			int halfSize = measuredSize / 2;
			pos[0] = layoutPosition0 + optionCenter.getAsPixels(parent)
					- halfSize;
			pos[1] = pos[0] + measuredSize;
		} else if (rightOrBottom != null && !rightOrBottom.isUnitUndefined()) {
			// peg right/bottom
			int rightOrBottomPixels = rightOrBottom.getAsPixels(parent);
			pos[0] = dist - rightOrBottomPixels - measuredSize;
			pos[1] = dist - rightOrBottomPixels;
		} else {
			// Center
			int offset = (dist - measuredSize) / 2;
			pos[0] = layoutPosition0 + offset;
			pos[1] = pos[0] + measuredSize;
		}
	}

	private void computeVerticalLayoutPosition(int currentHeight,
			TiDimension optionTop, int measuredHeight, int layoutTop,
			int[] pos, int maxBottom, LayoutParams params) {
		int top = layoutTop + currentHeight;
		top += (optionTop != null)?optionTop.getAsPixels(this):0;
		// cap the bottom to make sure views don't go off-screen when user
		// supplies a height value that is >= screen
		// height and this view is below another view in vertical layout.
		int bottom = Math.min(top + measuredHeight, maxBottom);
		pos[0] = top;
		pos[1] = bottom;
	}
	
	private static int getLayoutOptionAsPixels(TiDimension option, int type, LayoutParams params, View parent) {
		int result = (option != null)?option.getAsPixels(parent):0;
		if (params instanceof AnimationLayoutParams) {
			float fraction = ((AnimationLayoutParams) params).animationFraction;
			LayoutParams oldParams = ((AnimationLayoutParams) params).oldParams;
			if (fraction < 1.0f) {
				TiDimension oldParam = null;
				switch (type) {
				case TiDimension.TYPE_LEFT:
					oldParam = oldParams.optionLeft;
					break;
				case TiDimension.TYPE_RIGHT:
					oldParam = oldParams.optionRight;
					break;
				case TiDimension.TYPE_TOP:
					oldParam = oldParams.optionTop;
					break;
				case TiDimension.TYPE_BOTTOM:
					oldParam = oldParams.optionBottom;
					break;
				case TiDimension.TYPE_WIDTH:
					oldParam = oldParams.optionWidth;
					break;
				case TiDimension.TYPE_HEIGHT:
					oldParam = oldParams.optionHeight;
					break;
				case TiDimension.TYPE_CENTER_X:
					oldParam = oldParams.optionCenterX;
					break;
				case TiDimension.TYPE_CENTER_Y:
					oldParam = oldParams.optionCenterY;
					break;
				default:
					break;
				}
				int oldValue = (oldParam != null)?oldParam.getAsPixels(parent):0;
				result = (int) (result * fraction + (1 - fraction)* oldValue);
			}
		}
		return result;
	}
	
	private void computeHorizontalLayoutPosition(
			TiCompositeLayout.LayoutParams params, int measuredWidth,
			int measuredHeight, int layoutRight, int layoutTop,
			int layoutBottom, int[] hpos, int[] vpos, int currentIndex) {

		TiDimension optionLeft = params.optionLeft;
		TiDimension optionRight = params.optionRight;
		int left = horizontalLayoutCurrentLeft + horiztonalLayoutPreviousRight;
		int optionLeftValue = getLayoutOptionAsPixels(optionLeft, TiDimension.TYPE_LEFT, params, this);
			left += optionLeftValue;
		horiztonalLayoutPreviousRight = getLayoutOptionAsPixels(optionRight, TiDimension.TYPE_RIGHT, params,this);

		int right;
		
		
		
		// If it's fill width with horizontal wrap, just take up remaining
		// space.
		if (enableHorizontalWrap && params.autoFillsWidth
				&& params.sizeOrFillWidthEnabled) {
			right = measuredWidth;
		} else {
			right = left + measuredWidth;
		}

		if (enableHorizontalWrap
				&& ((right + horiztonalLayoutPreviousRight) > layoutRight || left >= layoutRight)) {
			// Too long for the current "line" that it's on. Need to move it
			// down.
			left = optionLeftValue;
			right = measuredWidth + left;
			horizontalLayoutTopBuffer = horizontalLayoutTopBuffer
					+ horizontalLayoutLineHeight;
			horizontalLayoutLineHeight = 0;
		} else if (!enableHorizontalWrap && params.autoFillsWidth
				&& params.sizeOrFillWidthEnabled) {
			// If there is no wrap, and width is fill behavior, cap it off at
			// the width of the screen
			right = Math.min(right, layoutRight);
		}

		hpos[0] = left;
		hpos[1] = right;
		

		
		horizontalLayoutCurrentLeft = right;

		if (enableHorizontalWrap) {
			// Don't update row on the first iteration since we already do it
			// beforehand
			if (currentIndex != 0
					&& currentIndex > horizontalLayoutLastIndexBeforeWrap) {
				updateRowForHorizontalWrap(layoutRight, currentIndex);
			}
			measuredHeight = calculateHeightFromPins(params,
					horizontalLayoutTopBuffer, horizontalLayoutTopBuffer
							+ horizontalLayoutLineHeight,
					horizontalLayoutLineHeight, measuredHeight, true);
			layoutBottom = horizontalLayoutLineHeight;
		}
		else {
			measuredHeight = calculateHeightFromPins(params,
					layoutTop, layoutBottom,
					layoutBottom - layoutTop, measuredHeight, true);
		}

		// Get vertical position into vpos
		computePosition(this, params.optionTop, params.optionCenterY,
				params.optionBottom, measuredHeight, layoutTop, layoutBottom,
				vpos);
		if (params.optionTop != null && !params.optionTop.isUnitUndefined() &&
				params.optionBottom != null && !params.optionBottom.isUnitUndefined())
		{
			int height = vpos[1] - vpos[0];
			vpos[0] = layoutTop + (layoutBottom - layoutTop)/2 - height/2;
			vpos[1] = vpos[0] + height;
		}
		// account for moving the item "down" to later line(s) if there has been
		// wrapping.
		vpos[0] = vpos[0] + horizontalLayoutTopBuffer;
		vpos[1] = vpos[1] + horizontalLayoutTopBuffer;
	}

	private void updateRowForHorizontalWrap(int maxRight, int currentIndex) {
		int rowWidth = 0;
		int rowHeight = 0;
		int i = 0;
		horizontalLayoutLineHeight = 0;

		for (i = currentIndex; i < getChildCount(); i++) {
			View child = getChildAt(i);
			LayoutParams params = (LayoutParams) child.getLayoutParams();
			// Calculate row width/height with padding
			rowWidth += child.getMeasuredWidth()
					+ getViewWidthPadding(child, params, this);
			rowHeight = child.getMeasuredHeight()
					+ getViewHeightPadding(child, params, this);

			if (rowWidth > maxRight) {
				horizontalLayoutLastIndexBeforeWrap = i - 1;
				return;

			} else if (rowWidth == maxRight) {
				break;
			}

			if (horizontalLayoutLineHeight < rowHeight) {
				horizontalLayoutLineHeight = rowHeight;
			}
		}

		if (horizontalLayoutLineHeight < rowHeight) {
			horizontalLayoutLineHeight = rowHeight;
		}
		horizontalLayoutLastIndexBeforeWrap = i;
	}

	// Determine whether we have a conflict where a parent has size behavior,
	// and child has fill behavior.
	private boolean hasSizeFillConflict(View parent, int[] conflicts,
			boolean firstIteration) {
		if (parent instanceof TiCompositeLayout) {
			TiCompositeLayout currentLayout = (TiCompositeLayout) parent;
			LayoutParams currentParams = (LayoutParams) currentLayout
					.getLayoutParams();

			// During the first iteration, the parent view needs to have size
			// behavior.
			if (firstIteration
					&& (currentParams.autoFillsWidth || currentParams.optionWidth != null)) {
				conflicts[0] = NO_SIZE_FILL_CONFLICT;
			}
			if (firstIteration
					&& (currentParams.autoFillsHeight || currentParams.optionHeight != null)) {
				conflicts[1] = NO_SIZE_FILL_CONFLICT;
			}

			// We don't check for sizeOrFillHeightEnabled. The calculations
			// during the measure phase (which includes
			// this method) will be adjusted to undefined behavior accordingly
			// during the layout phase.
			// sizeOrFillHeightEnabled is used during the layout phase to
			// determine whether we want to use the fill/size
			// measurements that we got from the measure phase.
			// if (currentParams.autoFillsWidth && currentParams.optionWidth ==
			// null && conflicts[0] == NOT_SET) {
			// conflicts[0] = HAS_SIZE_FILL_CONFLICT;
			// }
			// if (currentParams.autoFillsHeight && currentParams.optionHeight
			// == null && conflicts[1] == NOT_SET) {
			// conflicts[1] = HAS_SIZE_FILL_CONFLICT;
			// }

			// Stop traversing if we've determined whether there is a conflict
			// for both width and height
			if (conflicts[0] != NOT_SET && conflicts[1] != NOT_SET) {
				return true;
			}

			// If the child has size behavior, continue traversing through
			// children and see if any of them have fill
			// behavior
			for (int i = 0; i < currentLayout.getChildCount(); ++i) {
				if (hasSizeFillConflict(currentLayout.getChildAt(i), conflicts,
						false)) {
					return true;
				}
			}
		}

		// Default to false if we couldn't find conflicts
		if (firstIteration && conflicts[0] == NOT_SET) {
			conflicts[0] = NO_SIZE_FILL_CONFLICT;
		}
		if (firstIteration && conflicts[1] == NOT_SET) {
			conflicts[1] = NO_SIZE_FILL_CONFLICT;
		}
		return false;
	}

	protected int getWidthMeasureSpec(View child) {
		return MeasureSpec.EXACTLY;
	}

	protected int getHeightMeasureSpec(View child) {
		return MeasureSpec.EXACTLY;
	}

	/**
	 * A TiCompositeLayout specific version of
	 * {@link android.view.ViewGroup.LayoutParams}
	 */
	public static class LayoutParams extends FreeLayout.LayoutParams {
		protected int index;

		public int optionZIndex = NOT_SET;
		public TiDimension optionLeft = null;
		public TiDimension optionTop = null;
		public TiDimension optionCenterX = null;
		public TiDimension optionCenterY = null;
		public TiDimension optionRight = null;
		public TiDimension optionBottom = null;
		public TiDimension optionWidth = null;
		public TiDimension optionHeight = null;
		public Ti2DMatrix optionTransform = null;

		// This are flags to determine whether we are using fill or size
		// behavior
		public boolean sizeOrFillHeightEnabled = false;
		public boolean sizeOrFillWidthEnabled = false;

		/**
		 * If this is true, and {@link #sizeOrFillWidthEnabled} is true, then
		 * the current view will follow the fill behavior, which fills available
		 * parent width. If this value is false and
		 * {@link #sizeOrFillWidthEnabled} is true, then we use the size
		 * behavior, which constrains the view width to fit the width of its
		 * contents.
		 * 
		 * @module.api
		 */
		public boolean autoFillsWidth = false;

		/**
		 * If this is true, and {@link #sizeOrFillHeightEnabled} is true, then
		 * the current view will follow fill behavior, which fills available
		 * parent height. If this value is false and
		 * {@link #sizeOrFillHeightEnabled} is true, then we use the size
		 * behavior, which constrains the view height to fit the height of its
		 * contents.
		 * 
		 * @module.api
		 */
		public boolean autoFillsHeight = false;

		public LayoutParams() {
			super(WRAP_CONTENT, WRAP_CONTENT);

			index = Integer.MIN_VALUE;
		}

		public LayoutParams(TiCompositeLayout.LayoutParams params) {
			super(params);

			autoFillsWidth = params.autoFillsWidth;
			autoFillsHeight = params.autoFillsHeight;
			optionZIndex = params.optionZIndex;
			optionLeft = params.optionLeft;
			optionTop = params.optionTop;
			optionCenterX = params.optionCenterX;
			optionCenterY = params.optionCenterY;
			optionRight = params.optionRight;
			optionBottom = params.optionBottom;
			optionWidth = params.optionWidth;
			optionHeight = params.optionHeight;
			optionTransform = params.optionTransform;
			sizeOrFillHeightEnabled = params.sizeOrFillHeightEnabled;
			sizeOrFillWidthEnabled = params.sizeOrFillWidthEnabled;
		}

		public boolean autoSizeHeight() {
			return ((!this.sizeOrFillHeightEnabled && !this.autoFillsHeight && this.optionHeight == null) || (this.sizeOrFillHeightEnabled && !this.autoFillsHeight));
		}

		public boolean autoSizeWidth() {
			return ((!this.sizeOrFillWidthEnabled && !this.autoFillsWidth && this.optionWidth == null) || (this.sizeOrFillWidthEnabled && !this.autoFillsWidth));
		}
	}

	public static class AnimationLayoutParams extends LayoutParams {
		public float animationFraction = 1.0f;
		public LayoutParams oldParams = null;
		public Rect startRect = null;

		public AnimationLayoutParams() {
			super();
		}

		public AnimationLayoutParams(TiCompositeLayout.LayoutParams params) {
			super(params);
			oldParams = params;
		}
	}

	protected boolean isVerticalArrangement() {
		return (arrangement == LayoutArrangement.VERTICAL);
	}

	protected boolean isHorizontalArrangement() {
		return (arrangement == LayoutArrangement.HORIZONTAL);
	}

	protected boolean isDefaultArrangement() {
		return (arrangement == LayoutArrangement.DEFAULT);
	}

	public void setLayoutArrangement(String arrangementProperty) {
		Boolean needsUpdate = false;
		if (arrangementProperty != null
				&& arrangementProperty.equals(TiC.LAYOUT_HORIZONTAL)) {
			needsUpdate = (arrangement != LayoutArrangement.HORIZONTAL);
			arrangement = LayoutArrangement.HORIZONTAL;
		} else if (arrangementProperty != null
				&& arrangementProperty.equals(TiC.LAYOUT_VERTICAL)) {
			needsUpdate = (arrangement != LayoutArrangement.VERTICAL);
			arrangement = LayoutArrangement.VERTICAL;
		} else {
			needsUpdate = (arrangement != LayoutArrangement.DEFAULT);
			arrangement = LayoutArrangement.DEFAULT;
		}
		if (needsUpdate) {
			requestLayout();
			invalidate();
		}
	}

	public void setEnableHorizontalWrap(boolean enable) {
		if (enable != enableHorizontalWrap) {
			enableHorizontalWrap = enable;
			requestLayout();
			invalidate();
		}
	}

	public void setView(TiUIView view) {
		this.view = new WeakReference<TiUIView>(view);
	}
	@Override
	public void dispatchSetPressed(boolean pressed) {
		TiUIView view = (this.view == null ? null : this.view.get());
		if (view != null && (view.getDispatchPressed() == true))
		{
			int count = getChildCount();
			for (int i = 0; i < count; i++) {
	            final View child = getChildAt(i);
	            child.setPressed(pressed);
	        }
		}
	};

	
//	@Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//		TiUIView view = (this.view == null ? null : this.view.get());
//		if (view != null && (view.getTouchPassThrough() == true))
//			return false;
//        return super.onInterceptTouchEvent(ev);
//    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		TiUIView view = (this.view == null ? null : this.view.get());
		if (view != null && (view.getTouchPassThrough() == true))
			return false;
		return super.dispatchTouchEvent(event);
	}

	private void setNeedsSort(boolean value) {
		// For vertical and horizontal layouts, since the controls doesn't
		// overlap, we shouldn't sort based on the zIndex, the original order
		// that controls added should be preserved
		if (isHorizontalArrangement() || isVerticalArrangement()) {
			value = false;
		}
		needsSort = value;
	}
	
	public void setMinWidth(TiDimension value) {
		mMinWidth = value;
	}
	
	public void setMinHeight(TiDimension value) {
		mMinHeight = value;
	}
	
	public void setMaxWidth(TiDimension value) {
		mMaxWidth = value;
	}
	
	public void setMaxHeight(TiDimension value) {
		mMaxHeight = value;
	}
}
