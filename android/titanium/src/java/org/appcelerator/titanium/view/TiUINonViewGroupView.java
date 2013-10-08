package org.appcelerator.titanium.view;

import org.appcelerator.titanium.proxy.TiViewProxy;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class TiUINonViewGroupView extends TiUIView {

	public TiUINonViewGroupView(TiViewProxy proxy) {
		super(proxy);
	}

	private TiCompositeLayout childrenHolder;
	@Override
	public void add(TiUIView child, int index)
	{
		if (childrenHolder == null) {
			childrenHolder = new TiCompositeLayout(proxy.getActivity());
			TiCompositeLayout.LayoutParams params = new TiCompositeLayout.LayoutParams();
			params.height = ViewGroup.LayoutParams.MATCH_PARENT;
			params.width = ViewGroup.LayoutParams.MATCH_PARENT;
			getOrCreateBorderView().addView(childrenHolder, params);
			updateLayoutForChildren(proxy.getProperties());	
		}
		super.add(child, index);
	}

	@Override
	public View getParentViewForChild()
	{
		return childrenHolder != null ? childrenHolder: nativeView;
	}
}
