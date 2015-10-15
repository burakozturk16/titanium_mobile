/**
 * Akylas
 * Copyright (c) 2014-2015 by Akylas. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.collectionview;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;

import com.nhaarman.listviewanimations.appearance.StickyListHeadersAdapterDecorator;
import com.nhaarman.listviewanimations.util.StickyListHeadersListViewWrapper;

import se.emilsjolander.stickylistheaders.ListViewGridAdapter;
import ti.modules.titanium.ui.widget.CustomListView;
import ti.modules.titanium.ui.widget.abslistview.TiAbsListView;
import android.app.Activity;
import android.graphics.Canvas;
import android.view.MotionEvent;

public class TiCollectionView extends TiAbsListView<CustomListView> {

    private static final String TAG = "TiCollectionView";
	public TiCollectionView(TiViewProxy proxy, Activity activity) {
		super(proxy, activity);
	}
	
    private TiDimension mColumnsWidth = null;
    private int mNumColumns = 0;
	private ListViewGridAdapter gridAdapter = null;
	
    @Override
    protected CustomListView createListView(final Activity activity) {
        final KrollProxy fProxy = this.proxy;
        CustomListView result = new CustomListView(activity) {
            
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
                
                super.onLayout(changed, left, top, right, bottom);
                if (changed && fProxy != null && fProxy.hasListeners(TiC.EVENT_POST_LAYOUT, false)) {
                    fProxy.fireEvent(TiC.EVENT_POST_LAYOUT, null);
                }
                
            }
            
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                if (mColumnsWidth != null && !mColumnsWidth.isUnitFixed()) {
                    gridAdapter.setColumnWidth(mColumnsWidth.getAsPixels(this));
                } else if(gridAdapter.updateNumColumns()) {
                    gridAdapter.notifyDataSetChanged();
                }
            }
            
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (touchPassThrough == true)
                    return false;
                return super.dispatchTouchEvent(event);
            }
            
            @Override
            protected void dispatchDraw(Canvas canvas) {
                try {
                    super.dispatchDraw(canvas);
                } catch (IndexOutOfBoundsException e) {
                    // samsung error
                }
            }
        };

        return result;
    }
    
    @Override
    protected void notifyDataSetChanged() {
        if (gridAdapter != null) {
            gridAdapter.notifyDataSetChanged();
        }
    }
    
    @Override
    protected void setListViewAdapter (TiBaseAdapter adapter) {
        gridAdapter = new ListViewGridAdapter(listView.getWrappedList(), adapter);
        if (mColumnsWidth != null) {
            gridAdapter.setColumnWidth(mColumnsWidth.getAsPixels(listView));
        } else {
            gridAdapter.setNumColumns( mNumColumns );
        }
        
        StickyListHeadersAdapterDecorator stickyListHeadersAdapterDecorator = new StickyListHeadersAdapterDecorator(gridAdapter);
        stickyListHeadersAdapterDecorator.setListViewWrapper(new StickyListHeadersListViewWrapper(listView));
        listView.setAdapter(stickyListHeadersAdapterDecorator);
    }
    
    protected static String getCellProxyRootType() {
        return "Ti.UI.CollectionItem";
    }
    
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_SCROLLING_ENABLED:
            listView.setScrollingEnabled(newValue);
            break;
        case TiC.PROPERTY_COLUMN_WIDTH:
            mColumnsWidth = TiConvert.toTiDimension(newValue, TiDimension.TYPE_WIDTH);
            if (gridAdapter != null) {
                if (mColumnsWidth != null) {
                    gridAdapter.setColumnWidth(mColumnsWidth.getAsPixels(listView));
                }
            }
            
            break;
        case TiC.PROPERTY_NUM_COLUMNS:
            mColumnsWidth = null;
            mNumColumns = TiConvert.toInt(newValue);
            if (gridAdapter != null) {
                gridAdapter.setNumColumns(mNumColumns);
            }
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
}
