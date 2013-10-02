package org.appcelerator.titanium.view;

import org.appcelerator.titanium.proxy.TiViewProxy;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class TiUINonViewGroupView extends TiUIView {

	public TiUINonViewGroupView(TiViewProxy proxy) {
		super(proxy);
	}

	private FreeLayout layout;
	private TiCompositeLayout childrenHolder;
	@Override
	public void add(TiUIView child, int index)
	{
		if (childrenHolder == null) {
			childrenHolder = new TiCompositeLayout(proxy.getActivity());
			TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
			params.height = ViewGroup.LayoutParams.MATCH_PARENT;
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			if (borderView != null) {
				borderView.addView(childrenHolder, params);
			}
			else {
				layout = new FreeLayout(proxy.getActivity());	
				ViewGroup savedParent = null;
				int savedIndex = 0;
				if (nativeView.getParent() != null) {
					ViewParent nativeParent = nativeView.getParent();
					if (nativeParent instanceof ViewGroup) {
						savedParent = (ViewGroup) nativeParent;
						savedIndex = savedParent.indexOfChild(nativeView);
						savedParent.removeView(nativeView);
					}
				}
				layout.addView(nativeView, params);
				layout.addView(childrenHolder, params);
				if (savedParent != null) {
					savedParent.addView(layout, savedIndex,getLayoutParams());
				}
			}
			updateLayoutForChildren(proxy.getProperties());	
		}
		super.add(child, index);
	}

	@Override
	public View getParentViewForChild()
	{
		return childrenHolder == null ? nativeView : childrenHolder;
	}

	@Override
	public View getOuterView()
	{
		return borderView == null ? (layout == null ? nativeView : layout) : borderView;
	}

	@Override
	public View getRootView()
	{
		return layout != null ? layout : nativeView;
	}
}
