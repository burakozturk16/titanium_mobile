/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.abslistview;

import java.util.ArrayList;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.UIModule;
import ti.modules.titanium.ui.widget.listview.TiListView;
import android.app.Activity;
import android.os.Handler;
import android.os.Message;

@Kroll.proxy(propertyAccessors = {
	TiC.PROPERTY_HEADER_TITLE,
	TiC.PROPERTY_FOOTER_TITLE,
    TiC.PROPERTY_TEMPLATES,
    TiC.PROPERTY_SECTIONS,
	TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE,
	TiC.PROPERTY_SHOW_VERTICAL_SCROLL_INDICATOR,
	TiC.PROPERTY_SEPARATOR_COLOR,
	TiC.PROPERTY_SEARCH_TEXT,
    TiC.PROPERTY_SEARCH_VIEW,
    TiC.PROPERTY_SEARCH_VIEW_EXTERNAL,
	TiC.PROPERTY_CASE_INSENSITIVE_SEARCH,
	TiC.PROPERTY_HEADER_DIVIDERS_ENABLED,
	TiC.PROPERTY_FOOTER_DIVIDERS_ENABLED,
	TiC.PROPERTY_SCROLL_HIDES_KEYBOARD
})
public abstract class AbsListViewProxy extends TiViewProxy {

	private static final String TAG = "ListViewProxy";
	
	private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;

	private static final int MSG_SECTION_COUNT = MSG_FIRST_ID + 399;
	private static final int MSG_SCROLL_TO_ITEM = MSG_FIRST_ID + 400;
	private static final int MSG_APPEND_SECTION = MSG_FIRST_ID + 401;
	private static final int MSG_INSERT_SECTION_AT = MSG_FIRST_ID + 402;
	private static final int MSG_DELETE_SECTION_AT = MSG_FIRST_ID + 403;
	private static final int MSG_REPLACE_SECTION_AT = MSG_FIRST_ID + 404;
	private static final int MSG_SCROLL_TO_TOP = MSG_FIRST_ID + 405;
	private static final int MSG_SCROLL_TO_BOTTOM = MSG_FIRST_ID + 406;
	private static final int MSG_GET_SECTIONS = MSG_FIRST_ID + 407;
    private static final int MSG_CLOSE_PULL_VIEW = MSG_FIRST_ID + 408;
    private static final int MSG_SHOW_PULL_VIEW = MSG_FIRST_ID + 409;
//	private static final int MSG_SET_SECTIONS = MSG_FIRST_ID + 408;

    protected static final int MSG_LAST_ID = MSG_SHOW_PULL_VIEW;


	//indicate if user attempts to add/modify/delete sections before TiListView is created 
	private boolean preload = false;
	private ArrayList<AbsListSectionProxy> preloadSections;
	private HashMap<String, Integer> preloadMarker;
	
	public AbsListViewProxy() {
		super();
	}
	
	public void handleCreationArgs(KrollModule createdInModule, Object[] args) {
		preloadSections = new ArrayList<AbsListSectionProxy>();
		defaultValues.put(TiC.PROPERTY_DEFAULT_ITEM_TEMPLATE, UIModule.LIST_ITEM_TEMPLATE_DEFAULT);
        defaultValues.put(TiC.PROPERTY_CASE_INSENSITIVE_SEARCH, true);
        defaultValues.put(TiC.PROPERTY_ROW_HEIGHT, 50);
        defaultValues.put(TiC.PROPERTY_SELECTED_BACKGROUND_COLOR, "#474747");
		super.handleCreationArgs(createdInModule, args);
		
	}
	
    @Override
    public void setActivity(Activity activity) {
        super.setActivity(activity);
        TiUIView listView = peekView();

        if (listView != null) {
            AbsListSectionProxy[] sections = ((TiAbsListView) listView).getSections();
            for (AbsListSectionProxy section : sections) {
                if (section != null) {
                    section.setActivity(activity);
                }
            }
        }
    }
    
//    @Override
//    public void releaseViews(boolean activityFinishing) {
//        TiUIView listView = peekView();
//        super.releaseViews(activityFinishing);
//    }

	@Override
	public void handleCreationDict(KrollDict options) {
		super.handleCreationDict(options);
		//Adding sections to preload sections, so we can handle appendSections/insertSection
		//accordingly if user call these before TiListView is instantiated.
		if (options.containsKey(TiC.PROPERTY_SECTIONS)) {
		    preload = true;
			Object obj = options.get(TiC.PROPERTY_SECTIONS);
			if (obj instanceof Object[]) {
				addPreloadSections((Object[]) obj, -1, true);
			}
		}
	}
	
	public void clearPreloadSections() {
	    //dont clear the preloaded because if we are 
		if (preloadSections != null) {
			preloadSections.clear();
		}
		preload = false;
	}
	
	public ArrayList<AbsListSectionProxy> getPreloadSections() {
		return preloadSections;
	}
	
	public boolean getPreload() {
		return preload;
	}
	
	public void setPreload(boolean pload)
	{
		preload = pload;
	}
	
	public HashMap<String, Integer> getPreloadMarker()
	{
		return preloadMarker;
	}

	private void addPreloadSections(Object secs, int index, boolean arrayOnly) {
		if (secs instanceof Object[]) {
			Object[] sections = (Object[]) secs;
			for (int i = 0; i < sections.length; i++) {
				Object section = sections[i];
				addPreloadSection(section, -1);
			}
		} else if (!arrayOnly) {
			addPreloadSection(secs, -1);
		}
	}
	
	private void addPreloadSection(Object section, int index) {
	    if(section instanceof HashMap) {
            section =  KrollProxy.createProxy(AbsListSectionProxy.class, null, new Object[]{section}, null);
        }
		if (section instanceof AbsListSectionProxy) {
			if (index == -1) {
				preloadSections.add((AbsListSectionProxy) section);
			} else {
				preloadSections.add(index, (AbsListSectionProxy) section);
			}
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public int getSectionCount() {
		// if (TiApplication.isUIThread()) {
		// 	return handleSectionCount();
		// } else {
		// 	return (Integer) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SECTION_COUNT));
		// }
		TiAbsListView listView = (TiAbsListView)peekView();
		if (listView != null) {
			return listView.getSectionCount();
		} else {
			return preloadSections.size();

		}
	}

	@Kroll.method
	public int getSectionItemsCount(int sectionIndex) {
		AbsListSectionProxy section = getSectionAt(sectionIndex);
		if (section != null) {
			return section.getLength();
		} else {
			return 0;
		}
	}
	
	@Kroll.method
	public AbsListSectionProxy getSectionAt(int sectionIndex) {
		TiAbsListView listView = (TiAbsListView)peekView();
		if (listView != null) {
			return listView.getSectionAt(sectionIndex);
		} else {
			if (sectionIndex < 0 || sectionIndex >= preloadSections.size()) {
				Log.e(TAG, "getItem Invalid section index");
				return null;
			}
			
			return preloadSections.get(sectionIndex);
		}
	}
	
	// public int handleSectionCount () {
	// 	TiUIView listView = peekView();
	// 	if (listView != null) {
	// 		return ((TiAbsListView) listView).getSectionCount();
	// 	}
	// 	return 0;
	// }

	@Kroll.method
	public void scrollToItem(int sectionIndex, int itemIndex, @Kroll.argument(optional = true) KrollDict options) {
		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		if (TiApplication.isUIThread()) {
			handleScrollToItem(sectionIndex, itemIndex, animated);
		} else {
			KrollDict d = new KrollDict();
			d.put("itemIndex", itemIndex);
			d.put("sectionIndex", sectionIndex);
			d.put(TiC.PROPERTY_ANIMATED, animated);
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_SCROLL_TO_ITEM), d);
		}
	}
	
	@Kroll.method
	public KrollProxy getChildByBindId(int sectionIndex, int itemIndex, String bindId) {
		TiUIView listView = peekView();
		if (listView != null) {
			return ((TiAbsListView) listView).getChildByBindId(sectionIndex, itemIndex, bindId);
		}
		return null;
	}
	
	@Kroll.method
	public KrollDict getItemAt(int sectionIndex, int itemIndex) {
		TiUIView listView = peekView();
		if (listView != null) {
			return ((TiAbsListView) listView).getItem(sectionIndex, itemIndex);
		} else {
			if (sectionIndex < 0 || sectionIndex >= preloadSections.size()) {
				Log.e(TAG, "getItem Invalid section index");
				return null;
			}
			
			return preloadSections.get(sectionIndex).getItemAt(itemIndex);
		}
	}
	
	
	@Kroll.method
	public void setMarker(Object marker) {
		if (marker instanceof HashMap) {
			HashMap<String, Integer> m = (HashMap<String, Integer>) marker;
			TiUIView listView = peekView();
			if (listView != null) {
				((TiAbsListView)listView).setMarker(m);
			} else {
				preloadMarker = m;
			}
		}
	}
	
	@Kroll.method
	public void scrollToTop(int y, @Kroll.argument(optional = true) KrollDict options)
	{
		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		Message message = getMainHandler().obtainMessage(MSG_SCROLL_TO_TOP);
		message.arg1 = y;
		message.arg2 = animated?1:0;
		message.sendToTarget();
	}

	@Kroll.method
	public void scrollToBottom(int y, @Kroll.argument(optional = true) KrollDict options)
	{
		boolean animated = TiConvert.toBoolean(options, TiC.PROPERTY_ANIMATED, true);
		Message message = getMainHandler().obtainMessage(MSG_SCROLL_TO_BOTTOM);
		message.arg1 = y;
		message.arg2 = animated?1:0;
		message.sendToTarget();
	}

	@Override
	public boolean handleMessage(final Message msg) 	{

		switch (msg.what) {

			// case MSG_SECTION_COUNT: {
			// 	AsyncResult result = (AsyncResult)msg.obj;
			// 	result.setResult(handleSectionCount());
			// 	return true;
			// }

			case MSG_SCROLL_TO_ITEM: {
				AsyncResult result = (AsyncResult)msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int sectionIndex = data.getInt("sectionIndex");
				int itemIndex = data.getInt("itemIndex");
				boolean animated = data.getBoolean(TiC.PROPERTY_ANIMATED);
				handleScrollToItem(sectionIndex, itemIndex, animated);
				result.setResult(null);
				return true;
			}
			case MSG_SCROLL_TO_TOP: {
				handleScrollToTop(msg.arg1, msg.arg2 == 1);
				return true;
			}
			case MSG_SCROLL_TO_BOTTOM: {
				handleScrollToBottom(msg.arg1, msg.arg2 == 1);
				return true;
			}
			case MSG_APPEND_SECTION: {
				AsyncResult result = (AsyncResult)msg.obj;
				handleAppendSection(result.getArg());
				result.setResult(null);
				return true;
			}
			case MSG_DELETE_SECTION_AT: {
				AsyncResult result = (AsyncResult)msg.obj;
				handleDeleteSectionAt(TiConvert.toInt(result.getArg()));
				result.setResult(null);
				return true;
			}
			case MSG_INSERT_SECTION_AT: {
				AsyncResult result = (AsyncResult)msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt("index");
				Object section = data.get("section");
				handleInsertSectionAt(index, section);
				result.setResult(null);
				return true;
			}
			case MSG_REPLACE_SECTION_AT: {
				AsyncResult result = (AsyncResult)msg.obj;
				KrollDict data = (KrollDict) result.getArg();
				int index = data.getInt("index");
				Object section = data.get("section");
				handleReplaceSectionAt(index, section);
				result.setResult(null);
				return true;
			}
			
			case MSG_GET_SECTIONS: {
				AsyncResult result = (AsyncResult)msg.obj;
				result.setResult(handleSections());
				return true;
			}
	        case MSG_SHOW_PULL_VIEW: {
	            handleShowPullView(msg.obj);
	            return true;
	        }
	        case MSG_CLOSE_PULL_VIEW: {
	            handleClosePullView(msg.obj);
	            return true;
	        }
//			case MSG_SET_SECTIONS: {
//				AsyncResult result = (AsyncResult)msg.obj;
//				TiUIView listView = peekView();
//				if (listView != null) {
//					((TiAbsListView)listView).processSectionsAndNotify((Object[])result.getArg());
//				} else {
//					Log.e(TAG, "Unable to set sections, listView is null", Log.DEBUG_MODE);
//				}
//				result.setResult(null);
//				return true;
//			}
//			
			default:
				return super.handleMessage(msg);
		}
	}
	private void handleScrollToItem(int sectionIndex, int itemIndex, boolean animated) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).scrollToItem(sectionIndex, itemIndex, animated);
		}
	}

	private void handleScrollToTop(int y, boolean animated) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).scrollToTop(y, animated);
		}
	}

	private void handleScrollToBottom(int y, boolean animated) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).scrollToBottom(y, animated);
		}
	}

	@Kroll.method
	public void appendSection(Object section) {
		if (TiApplication.isUIThread()) {
			handleAppendSection(section);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_APPEND_SECTION), section);
		}
	}

	private void handleAppendSection(Object section) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).appendSection(section);
		} else {
			preload = true;
			addPreloadSections(section, -1, false);
		}
	}
	
	@Kroll.method
	public void deleteSectionAt(int index) {
		if (TiApplication.isUIThread()) {
			handleDeleteSectionAt(index);
		} else {
			TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_DELETE_SECTION_AT), index);
		}
	}
	
	private void handleDeleteSectionAt(int index) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).deleteSectionAt(index);
		} else {
			if (index < 0 || index >= preloadSections.size()) {
				Log.e(TAG, "Invalid index to delete section");
				return;
			}
			preload = true;
			preloadSections.remove(index);
		}
	}
	
	@Kroll.method
	public void insertSectionAt(int index, Object section) {
		if (TiApplication.isUIThread()) {
			handleInsertSectionAt(index, section);
		} else {
			sendInsertSectionMessage(index, section);
		}
	}
	
	private void sendInsertSectionMessage(int index, Object section) {
		KrollDict data = new KrollDict();
		data.put("index", index);
		data.put("section", section);
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_INSERT_SECTION_AT), data);
	}
	
	private void handleInsertSectionAt(int index, Object section) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).insertSectionAt(index, section);
		} else {
			if (index < 0 || index > preloadSections.size()) {
				Log.e(TAG, "Invalid index to insertSection");
				return;
			}
			preload = true;
			addPreloadSections(section, index, false);
		}
	}
	
	@Kroll.method
	public void replaceSectionAt(int index, Object section) {
		if (TiApplication.isUIThread()) {
			handleReplaceSectionAt(index, section);
		} else {
			sendReplaceSectionMessage(index, section);
		}
	}
	
	private void sendReplaceSectionMessage(int index, Object section) {
		KrollDict data = new KrollDict();
		data.put("index", index);
		data.put("section", section);
		TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REPLACE_SECTION_AT), data);
	}

	private void handleReplaceSectionAt(int index, Object section) {
		TiUIView listView = peekView();
		if (listView != null) {
			((TiAbsListView) listView).replaceSectionAt(index, section);
		} else {
			handleDeleteSectionAt(index);
			handleInsertSectionAt(index,  section);
			
		}
	}
	
	@Kroll.method @Kroll.getProperty
	public AbsListSectionProxy[] getSections()
	{
		if (TiApplication.isUIThread()) {
			return handleSections();
		} else {
			return (AbsListSectionProxy[]) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GET_SECTIONS));
		}
	}
	
	@Kroll.setProperty @Kroll.method
	public void setSections(Object sections)
	{
		if (!(sections instanceof Object[])) {
			Log.e(TAG, "Invalid argument type to setSection(), needs to be an array", Log.DEBUG_MODE);
			return;
		}
		setPropertyJava(TiC.PROPERTY_SECTIONS, sections);

		Object[] sectionsArray = (Object[]) sections;
		TiUIView listView = peekView();
		//Preload sections if listView is not opened.
		if (listView == null) {
			clearPreloadSections();
            preload = true;
			addPreloadSections(sectionsArray, -1, true);
		}
		else {
			((TiAbsListView)listView).processSectionsAndNotify(sectionsArray);
		}
	}
	
	private AbsListSectionProxy[] handleSections()
	{
		TiUIView listView = peekView();

		if (listView != null) {
			return ((TiAbsListView) listView).getSections();
		}
		ArrayList<AbsListSectionProxy> preloadedSections = getPreloadSections();
		return preloadedSections.toArray(new AbsListSectionProxy[preloadedSections.size()]);
	}
	   

	
	@Kroll.method
	public void appendItems(int sectionIndex, Object data) {
		AbsListSectionProxy section = getSectionAt(sectionIndex);
		if (section != null){
			section.appendItems(data);
		}
		else {
			Log.e(TAG, "appendItems wrong section index");
		}
	}
	
	@Kroll.method
	public void insertItemsAt(int sectionIndex, int index, Object data) {
		AbsListSectionProxy section = getSectionAt(sectionIndex);
		if (section != null){
			section.insertItemsAt(index, data);
		}
		else {
			Log.e(TAG, "insertItemsAt wrong section index");
		}
	}

	@Kroll.method
	public void deleteItemsAt(int sectionIndex, int index, int count) {
		AbsListSectionProxy section = getSectionAt(sectionIndex);
		if (section != null){
			section.deleteItemsAt(index, count);
		}
		else {
			Log.e(TAG, "deleteItemsAt wrong section index");
		}
	}

	@Kroll.method
	public void replaceItemsAt(int sectionIndex, int index, int count, Object data) {
		AbsListSectionProxy section = getSectionAt(sectionIndex);
		if (section != null){
			section.replaceItemsAt(index, count, data);
		}
		else {
			Log.e(TAG, "replaceItemsAt wrong section index");
		}
	}

	@Kroll.method
	public void updateItemAt(int sectionIndex, int index, Object data, @Kroll.argument(optional = true) Object options) {
        AbsListSectionProxy section = getSectionAt(sectionIndex);
        if (section != null){
            section.updateItemAt(index, data, options);
        }
        else {
            Log.e(TAG, "updateItemAt wrong section index");
        }
	}
	

    public void handleShowPullView(Object obj) {
        Boolean animated = true;
        if (obj != null) {
            animated = TiConvert.toBoolean(obj);
        }
        TiUIView listView = peekView();
        if (listView != null) {
            ((TiAbsListView) listView).showPullView(animated);
        }
    }

    public void handleClosePullView(Object obj) {
        Boolean animated = true;
        if (obj != null) {
            animated = TiConvert.toBoolean(obj);
        }
        TiUIView listView = peekView();
        if (listView != null) {
            ((TiAbsListView) listView).closePullView(animated);
        }
    }

    @Kroll.method()
    public void showPullView(@Kroll.argument(optional = true) Object obj) {
        if (TiApplication.isUIThread()) {
            handleShowPullView(obj);
        } else {
            Handler handler = getMainHandler();
            handler.sendMessage(handler.obtainMessage(MSG_SHOW_PULL_VIEW, obj));
        }
    }

    @Kroll.method()
    public void closePullView(@Kroll.argument(optional = true) Object obj) {
        if (TiApplication.isUIThread()) {
            handleClosePullView(obj);
        } else {
            Handler handler = getMainHandler();
            handler.sendMessage(handler.obtainMessage(MSG_CLOSE_PULL_VIEW, obj));
        }
    }

}
