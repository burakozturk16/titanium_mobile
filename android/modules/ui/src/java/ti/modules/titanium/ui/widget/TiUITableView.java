/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiBorderWrapperView;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.SearchBarProxy;
import ti.modules.titanium.ui.TableViewProxy;
import ti.modules.titanium.ui.widget.searchbar.TiUISearchBar;
import ti.modules.titanium.ui.widget.searchview.TiUISearchView;
import ti.modules.titanium.ui.widget.tableview.TableViewModel;
import ti.modules.titanium.ui.widget.tableview.TiTableView;
import ti.modules.titanium.ui.widget.tableview.TiTableView.OnItemClickedListener;
import ti.modules.titanium.ui.widget.tableview.TiTableView.OnItemLongClickedListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class TiUITableView extends TiUIView implements OnItemClickedListener,
        OnItemLongClickedListener, OnLifecycleEvent {
    private static final String TAG = "TitaniumTableView";

	private static final int SEARCHVIEW_ID = 102;

    public static final int SEPARATOR_NONE = 0;
    public static final int SEPARATOR_SINGLE_LINE = 1;

    protected TiTableView tableView;

    public TiUITableView(TiViewProxy proxy) {
        super(proxy);
        getLayoutParams().autoFillsHeight = true;
        getLayoutParams().autoFillsWidth = true;

        Log.d(TAG, "Creating a tableView", Log.DEBUG_MODE);
        tableView = new TiTableView((TableViewProxy) proxy) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (touchPassThrough == true)
                    return false;
                return super.dispatchTouchEvent(event);
            }
        };
        Activity activity = proxy.getActivity();
        if (activity instanceof TiBaseActivity) {
            ((TiBaseActivity) activity).addOnLifecycleEventListener(this);
        }
        tableView.setOnItemClickListener(this);
        tableView.setOnItemLongClickListener(this);
        tableView.setFilterCaseInsensitive(true);
        tableView.setFilterAnchored(false);
    }

    @Override
    public void onClick(KrollDict data) {
        proxy.fireEvent(TiC.EVENT_CLICK, data);
    }

    @Override
    public boolean onLongClick(KrollDict data) {
        return proxy.fireEvent(TiC.EVENT_LONGCLICK, data);
    }

    public void setModelDirty() {
        tableView.getTableViewModel().setDirty();
    }

    public TableViewModel getModel() {
        return tableView.getTableViewModel();
    }

    public void updateView() {
        tableView.dataSetChanged();
    }

    public void scrollToIndex(final int index) {
        tableView.getListView().smoothScrollToPosition(index);
    }

    public void scrollToTop(final int y, boolean animated) {
        if (animated) {
            tableView.getListView().smoothScrollToPosition(0);
        } else {
            tableView.getListView().setSelectionFromTop(0, y);
        }
    }

    public void scrollToBottom(final int y, boolean animated) {
        if (animated) {
            tableView.getListView().smoothScrollToPosition(
                    tableView.getCount() - 1);
        } else {
            tableView.getListView().setSelection(tableView.getCount() - 1);
        }
    }

    public void selectRow(final int row_id) {
        tableView.getListView().setSelection(row_id);
    }

    public TiTableView getTableView() {
        return tableView;
    }
    
    @Override
    protected View getTouchView()
    {
        return getTableView();
    }
    
    
    @Override
    protected void setOnClickListener(View view)
    {
        if (view == tableView) {
            tableView.setOnItemClickListener(this);
        }
    }
    
    @Override
    protected void setOnLongClickListener(View view)
    {
        if (view == tableView) {
            tableView.setOnItemLongClickListener(this);
        }
    }
    
    @Override
    protected void removeOnClickListener(View view)
    {
        if (view == tableView) {
            tableView.setOnItemClickListener(null);
        }
    }
    
    @Override
    protected void removeOnLongClickListener(View view)
    {
        if (view == tableView) {
            tableView.setOnItemLongClickListener(null);
        }
    }
    protected void doSetClickable(View view, boolean clickable)
    {
        if (view == null) {
            return;
        }
        if (!clickable) {
            view.setOnClickListener(null); // This will set clickable to true in the view, so make sure it stays here so the next line turns it off.
            view.setOnLongClickListener(null);
        } else if ( ! (view instanceof AdapterView) ){
            // n.b.: AdapterView throws if click listener set.
            // n.b.: setting onclicklistener automatically sets clickable to true.
            setOnClickListener(view);
            setOnLongClickListener(view);
        }
        view.setClickable(clickable);
        view.setLongClickable(clickable);
    }

    
    public CustomListView getListView() {
        return tableView.getListView();
    }

    private ListView getInternalListView() {
        return (ListView) getListView().getWrappedList();
    }
    
    private void handleSetSearch(final Object searchObj) {
        TiViewProxy searchView = (TiViewProxy) searchObj;
        if (searchView != null) {
            TiUIView search = searchView.getOrCreateView();
            if (searchView instanceof SearchBarProxy) {
                ((TiUISearchBar) search).setOnSearchChangeListener(tableView);
            } else {
                ((TiUISearchView) search).setOnSearchChangeListener(tableView);
            }
            View sView = search.getNativeView();

            RelativeLayout layout = new RelativeLayout(proxy.getActivity());
            layout.setGravity(Gravity.NO_GRAVITY);
            layout.setPadding(0, 0, 0, 0);

            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            p.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

            TiDimension rawHeight;
            if (searchView.hasProperty("height")) {
                rawHeight = TiConvert.toTiDimension(searchView.getProperty("height"), 0);
            } else {
                rawHeight = TiConvert.toTiDimension("52dp", 0);
            }
            p.height = rawHeight.getAsPixels(layout);

            //Check to see if searchView has a border
            ViewParent parent = sView.getParent();
            if (parent instanceof TiBorderWrapperView) {
                TiBorderWrapperView v = (TiBorderWrapperView) parent;
                v.setId(SEARCHVIEW_ID);
                layout.addView(v, p);
            } else if (parent == null) {
                sView.setId(SEARCHVIEW_ID);
                layout.addView(sView, p);
            } else {
                Log.e(TAG, "Searchview already has parent, cannot add to tableview.", Log.DEBUG_MODE);
            }

            p = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT);
            p.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            p.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            p.addRule(RelativeLayout.BELOW, SEARCHVIEW_ID);
            layout.addView(tableView, p);
            setNativeView(layout);
        } else {
            setNativeView(tableView);
        }
        
    }
    
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_FOOTER_DIVIDERS_ENABLED:
            getInternalListView().setFooterDividersEnabled(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_HEADER_DIVIDERS_ENABLED:
            getInternalListView().setHeaderDividersEnabled(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_SEARCH:
            handleSetSearch(newValue);
            break;
        case TiC.PROPERTY_FILTER_ATTRIBUTE:
            tableView.setFilterAttribute(TiConvert.toString(newValue, TiC.PROPERTY_TITLE));
            break;
        case TiC.PROPERTY_OVER_SCROLL_MODE:
            getListView().setOverScrollMode(TiConvert.toInt(newValue, View.OVER_SCROLL_ALWAYS));
            break;
        case TiC.PROPERTY_SEPARATOR_COLOR:
            tableView.setSeparatorColor(TiConvert.toColor(newValue));
            break;
        case TiC.PROPERTY_SEPARATOR_STYLE:
            tableView.setSeparatorStyle(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_SCROLLING_ENABLED:
            getListView().setScrollingEnabled(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_FILTER_CASE_INSENSITIVE:
            tableView.setFilterCaseInsensitive(TiConvert.toBoolean(newValue, true));
            break;
        case TiC.PROPERTY_FILTER_ANCHORED:
            tableView.setFilterAnchored(TiConvert.toBoolean(newValue, false));
            break;
        case TiC.PROPERTY_MIN_ROW_HEIGHT:
            if (changedProperty) {
                updateView();
            }
            break;
        case TiC.PROPERTY_HEADER_VIEW:
            if (changedProperty) {
                if (oldValue != null) {
                    tableView.removeHeaderView((TiViewProxy) oldValue);
                }
                tableView.setHeaderView();
            }
            break;
        case TiC.PROPERTY_FOOTER_VIEW:
            if (changedProperty) {
                if (oldValue != null) {
                    tableView.removeFooterView((TiViewProxy) oldValue);
                }
                tableView.setFooterView();
            }
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

    @Override
    public void onResume(Activity activity) {
        if (tableView != null) {
            tableView.dataSetChanged();
        }
    }

    @Override
    public void onStop(Activity activity) {
    }

    @Override
    public void onStart(Activity activity) {
    }

    @Override
    public void onPause(Activity activity) {
    }

    @Override
    public void onDestroy(Activity activity) {
    }
    
    @Override
    public void onCreate(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void release() {
        // Release search bar if there is one
        if (nativeView instanceof RelativeLayout) {
            ((RelativeLayout) nativeView).removeAllViews();
            TiViewProxy searchView = (TiViewProxy) (proxy
                    .getProperty(TiC.PROPERTY_SEARCH));
            searchView.release();
        }

        if (tableView != null) {
            tableView.release();
            tableView = null;
        }
        if (proxy != null && proxy.getActivity() != null) {
            ((TiBaseActivity) proxy.getActivity())
                    .removeOnLifecycleEventListener(this);
        }
        nativeView = null;
        super.release();
    }


    @Override
    public void registerForTouch() {
        registerForTouch(tableView.getListView());
    }

}
