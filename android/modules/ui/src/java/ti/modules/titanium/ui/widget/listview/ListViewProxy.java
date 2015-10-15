/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.listview;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.TiMessenger.CommandNoReturn;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.titanium.view.TiCompositeLayout.LayoutParams;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.abslistview.AbsListViewProxy;
import android.app.Activity;
import android.os.Message;

@SuppressWarnings("unused")
@Kroll.proxy(creatableInModule = UIModule.class)
public class ListViewProxy extends AbsListViewProxy {

    private static final String TAG = "ListViewProxy";

    private static final int MSG_FIRST_ID = AbsListViewProxy.MSG_LAST_ID + 1;


    private static final int MSG_CLOSE_SWIPE_MENU = MSG_FIRST_ID + 1;
    protected static final int MSG_LAST_ID = MSG_CLOSE_SWIPE_MENU;

    public ListViewProxy() {
        super();
    }

    @Override
    public TiUIView createView(Activity activity) {
        TiUIView view = new TiListView(this, activity);
        LayoutParams params = view.getLayoutParams();
        params.sizeOrFillWidthEnabled = true;
        params.sizeOrFillHeightEnabled = true;
        params.autoFillsHeight = true;
        params.autoFillsHeight = true;
        params.autoFillsWidth = true;
        return view;
    }

    @Override
    public boolean handleMessage(final Message msg) {

        switch (msg.what) {
        case MSG_CLOSE_SWIPE_MENU: {
            handleCloseSwipeMenu(msg.obj);
            return true;
        }
        default:
            return super.handleMessage(msg);
        }
    }

    @Override
    public String getApiName() {
        return "Ti.UI.ListView";
    }

    public void handleCloseSwipeMenu(Object obj) {
        Boolean animated = true;
        if (obj != null) {
            animated = TiConvert.toBoolean(obj);
        }
        TiUIView listView = peekView();
        if (listView != null) {
            ((TiListView) listView).closeSwipeMenu(animated);
        }
    }

    @Kroll.method()
    public void closeSwipeMenu(final @Kroll.argument(optional = true) Object obj) {
        runInUiThread(new CommandNoReturn() {
            @Override
            public void execute() {
                handleCloseSwipeMenu(obj);                
            }
        }, false);
    }

}
