/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiContext;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import ti.modules.titanium.ui.PickerColumnProxy.PickerColumnListener;
import ti.modules.titanium.ui.widget.picker.TiDatePickerDialog;
import ti.modules.titanium.ui.widget.picker.TiTimePickerDialog;
import ti.modules.titanium.ui.widget.picker.TiUIDatePicker;
import ti.modules.titanium.ui.widget.picker.TiUIDateSpinner;
import ti.modules.titanium.ui.widget.picker.TiUINativePicker;
import ti.modules.titanium.ui.widget.picker.TiUIPicker;
import ti.modules.titanium.ui.widget.picker.TiUIPickerInterface;
import ti.modules.titanium.ui.widget.picker.TiUISpinner;
import ti.modules.titanium.ui.widget.picker.TiUITimePicker;
import ti.modules.titanium.ui.widget.picker.TiUITimeSpinner;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Message;
import android.widget.DatePicker;
import android.widget.TimePicker;

@Kroll.proxy(creatableInModule = UIModule.class, propertyAccessors = {
        "locale", "visibleItems",
        TiC.PROPERTY_SELECTION_OPENS, TiC.PROPERTY_CALENDAR_VIEW_SHOWN, TiC.PROPERTY_FONT })
@SuppressWarnings({ "unchecked", "rawtypes" })
public class PickerProxy extends ViewProxy implements PickerColumnListener {
    private int type = UIModule.PICKER_TYPE_PLAIN;
    private static final String TAG = "PickerProxy";
    public static final int DEFAULT_VISIBLE_ITEMS_COUNT = 5;
    private static final int MSG_FIRST_ID = TiViewProxy.MSG_LAST_ID + 1;
//     private static final int MSG_SELECT_ROW = MSG_FIRST_ID + 101;
    // private static final int MSG_SET_COLUMNS = MSG_FIRST_ID + 102;
    // private static final int MSG_ADD = MSG_FIRST_ID + 103;
    // private static final int MSG_REMOVE = MSG_FIRST_ID + 104;
    private static final int MSG_FIRE_COL_CHANGE = MSG_FIRST_ID + 105;
    private static final int MSG_FIRE_ROW_CHANGE = MSG_FIRST_ID + 106;
    private static final int MSG_FORCE_LAYOUT = MSG_FIRST_ID + 107;
    private boolean useSpinner = false;

    public PickerProxy() {
        super();
        defaultValues.put(TiC.PROPERTY_CALENDAR_VIEW_SHOWN, false);
    }

    public PickerProxy(TiContext tiContext) {
        this();
    }

    @Override
    public void handleCreationDict(KrollDict dict) {
        if (dict.containsKey("useSpinner")) {
            useSpinner = TiConvert.toBoolean(dict, "useSpinner");
        }
        if (dict.containsKey("type")) {
            type = TiConvert.toInt(TiConvert.toInt(dict, "type", UIModule.PICKER_TYPE_PLAIN));
        }
        super.handleCreationDict(dict);
        if (dict.containsKey("columns")) {
            setColumns(dict.get("columns"));
        } else if (dict.containsKey("rows")) {
            setRows(dict.get("rows"));
        }
    }
    
    @Override
    public void releaseViews(final boolean activityFinishing)
    {
        if (peekView() instanceof TiUIPicker) {
            setProperty(TiC.PROPERTY_SELECTED_ROW, getSelectedRows());
        }
        super.releaseViews(activityFinishing);
    }

    @Override
    public TiUIView createView(Activity activity) {
        if (type == UIModule.PICKER_TYPE_COUNT_DOWN_TIMER) {
            Log.w(TAG, "Countdown timer not supported in Titanium for Android");
            return null;
        } else if (type == UIModule.PICKER_TYPE_DATE_AND_TIME) {
            Log.w(TAG, "Date+Time timer not supported in Titanium for Android");
            return null;
        } else if (type == UIModule.PICKER_TYPE_PLAIN) {
            return createPlainPicker(activity, useSpinner);
        } else if (type == UIModule.PICKER_TYPE_DATE) {
            if (useSpinner) {
                return createDateSpinner(activity);
            } else {
                return createDatePicker(activity);
            }
        } else if (type == UIModule.PICKER_TYPE_TIME) {
            if (useSpinner) {
                return createTimeSpinner(activity);
            } else {
                return createTimePicker(activity);
            }
        } else {
            Log.w(TAG, "Unknown picker type");
            return null;
        }
    }

    private TiUIView createPlainPicker(Activity activity, boolean useSpinner) {
        TiUIPicker picker = useSpinner ? new TiUISpinner(this, activity)
                : new TiUINativePicker(this, activity);
        return picker;
    }

    private TiUIView createDatePicker(Activity activity) {
        return new TiUIDatePicker(this, activity);
    }

    private TiUIView createTimePicker(Activity activity) {
        return new TiUITimePicker(this, activity);
    }

    private TiUIView createTimeSpinner(Activity activity) {
        return new TiUITimeSpinner(this, activity);
    }

    private TiUIView createDateSpinner(Activity activity) {
        return new TiUIDateSpinner(this, activity);
    }

    @Kroll.getProperty
    @Kroll.method
    public boolean getUseSpinner() {
        return useSpinner;
    }
    

    @Kroll.getProperty
    @Kroll.method
    public Object getValue() {
        TiUIView view = peekView();
        if (view instanceof TiUIPickerInterface) {
            return ((TiUIPickerInterface) view).getValue();
        }
        return getProperty(TiC.PROPERTY_VALUE);
    }


    @Kroll.setProperty
    @Kroll.method
    public void setUseSpinner(boolean value) {
        if (peekView() != null) {
            Log.w(TAG,
                    "Attempt to change useSpinner property after view has already been created. Ignoring.");
        } else {
            useSpinner = value;
            if (children != null && children.size() > 0) {
                for (KrollProxy child : children) {
                    if (child instanceof PickerColumnProxy) {
                        ((PickerColumnProxy) child).setUseSpinner(value);
                    }
                }
            }
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public int getType() {
        return type;
    }

    @Kroll.setProperty
    @Kroll.method
    public void setType(int type) {
        if (peekView() != null) {
            Log.e(TAG,
                    "Attempt to change picker type after view has been created.");
            throw new IllegalStateException(
                    "You cannot change the picker type after it has been rendered.");
        }
        this.type = type;
    }

    private boolean isPlainPicker() {
        return (type == UIModule.PICKER_TYPE_PLAIN);
    }

//    @Override
//    protected void removeProxy(Object child, final boolean shouldDetach) {
//        {
//            int index = children.indexOf(child);
//            super.removeProxy(child, shouldDetach);
//            if (peekView() instanceof TiUIPicker) {
//                ((TiUIPicker) peekView()).onColumnRemoved(index);
//            }
//        }
//        // if (TiApplication.isUIThread() || peekView() == null) {
//        // handleRemoveColumn(child);
//        // } else {
//        // TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_REMOVE),
//        // child);
//        // }
//    }

    // private void handleRemoveColumn(Object child)
    // {
    // int index = -1;
    // if (children.contains(child)){
    // index = children.indexOf(child);
    // }
    // super.remove(child);
    // if (peekView() instanceof TiUIPicker){
    // ((TiUIPicker)peekView()).onColumnRemoved(index);
    // }
    // }

    // We need a special add() method above and beyond the TiViewProxy add()
    // because
    // because we can also accept array of PickerRowProxys
    // @Kroll.method
    // public void add(Object child, @Kroll.argument(optional = true) Object
    // index)
    // {
    // if (!isPlainPicker()) {
    // Log.w(TAG, "Attempt to add to date/time or countdown picker ignored.",
    // Log.DEBUG_MODE);
    // return;
    // }
    // if (TiApplication.isUIThread() || peekView() == null) {
    // handleAddObject(child);
    //
    // } else {
    // TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_ADD),
    // child);
    // }
    // }
    private static final String DEFAULT_TEMPLATE_TYPE = "Ti.UI.PickerColumn";

    @Override
    protected String defaultProxyTypeFromTemplate() {
        return DEFAULT_TEMPLATE_TYPE;
    }

    @Override
    protected void addProxy(Object child, final int index) {
        if (child instanceof PickerColumnProxy) {
            PickerColumnProxy column = (PickerColumnProxy) child;
//            prepareColumn(column);
            super.addProxy(column, index);
            column.setUseSpinner(useSpinner);
            column.setColumnListener(this);
        } else if (child instanceof PickerRowProxy) {
            getFirstColumn(true).add((PickerRowProxy) child);
        }
    }
    
    @Override
    protected void handleChildAdded(KrollProxy child, int index) {
        super.handleChildAdded(child, index);
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).onColumnAdded(index);
        }
    }
    
    @Override
    protected void handleChildRemoved(KrollProxy child, final int index,
            final boolean shouldDetach) {
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).onColumnRemoved(index);
        }
    }

    // private void handleAddObject(Object child)
    // {
    // if (child instanceof PickerColumnProxy) {
    // PickerColumnProxy column = (PickerColumnProxy)child;
    // addColumn(column);
    // } else if (child instanceof PickerRowProxy) {
    // getFirstColumn(true).add((PickerRowProxy)child);
    // } else if (child.getClass().isArray()) {
    // Object[] obj = (Object[])child;
    // Object firstObj = obj[0];
    // if (firstObj instanceof PickerRowProxy) {
    // getFirstColumn(true).addRows(obj);
    // } else if (firstObj instanceof PickerColumnProxy) {
    // addColumns(obj);
    // }
    // } else {
    // Log.w(TAG, "Unexpected type not added to picker: " +
    // child.getClass().getName(), Log.DEBUG_MODE);
    // }
    // }

    // private void addColumns(Object[] columns)
    // {
    // for (Object obj :columns) {
    // if (obj instanceof PickerColumnProxy) {
    // addColumn((PickerColumnProxy)obj);
    // } else {
    // Log.w(TAG, "Unexpected type not added to picker: " +
    // obj.getClass().getName(), Log.DEBUG_MODE);
    // }
    // }
    // }

    // private void addColumn(PickerColumnProxy column)
    // {
    // prepareColumn(column);
    // super.add(column, null);
    // if (peekView() instanceof TiUIPicker) {
    // ((TiUIPicker)peekView()).onColumnAdded(children.indexOf(column));
    // }
    // }

//    private void prepareColumn(PickerColumnProxy column) {
//        column.setUseSpinner(useSpinner);
//        column.setColumnListener(this);
//    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
//        case MSG_SELECT_ROW: {
//            AsyncResult result = (AsyncResult) msg.obj;
//            handleSelectRow((KrollDict) result.getArg());
//            result.setResult(null);
//            return true;
//        }
//        case MSG_SET_COLUMNS: {
//            AsyncResult result = (AsyncResult) msg.obj;
//            handleSetColumns(result.getArg());
//            result.setResult(null);
//            return true;
//        }
        // case MSG_ADD: {
        // AsyncResult result = (AsyncResult)msg.obj;
        // handleAddObject(result.getArg());
        // result.setResult(null);
        // return true;
        // }
        // case MSG_REMOVE: {
        // AsyncResult result = (AsyncResult)msg.obj;
        // handleRemoveColumn((TiViewProxy)result.getArg());
        // result.setResult(null);
        // return true;
        // }
        case MSG_FIRE_COL_CHANGE: {
            handleFireColumnModelChange(msg.arg1);
            return true;
        }
        case MSG_FIRE_ROW_CHANGE: {
            handleFireRowChange(msg.arg1, msg.arg2);
            return true;
        }
        case MSG_FORCE_LAYOUT: {
            handleForceRequestLayout();
            return true;
        }
        }
        return super.handleMessage(msg);
    }
    
//    @Kroll.setProperty
//    public void setSelectedRow(Object value) {
//        if (value instanceof Object[]) {
//            Object[] params = (Object[])value;
//            if (params.length == 2) {
//                setSelectedRow(TiConvert.toInt(params[0]), TiConvert.toInt(params[1]), false);
//            } else if (params.length >= 3) {
//                setSelectedRow(TiConvert.toInt(params[0]), TiConvert.toInt(params[1]), TiConvert.toBoolean(params[2]));
//            }
//        }
//    }

//    @Kroll.method
//    public void setSelectedRow(int column, int row,
//            @Kroll.argument(optional = true) boolean animated) {
//        if (!isPlainPicker()) {
//            Log.w(TAG,
//                    "Selecting row in date/time or countdown picker is not supported.",
//                    Log.DEBUG_MODE);
//            return;
//        }
//        TiUIView view = peekView();
//        if (view == null) {
//            // assign it to be selected after view creation
//            if (preselectedRows == null) {
//                preselectedRows = new ArrayList<Integer>();
//            }
//            while (preselectedRows.size() < (column + 1)) {
//                preselectedRows.add(null);
//            }
//            if (preselectedRows.size() >= (column + 1)) {
//                preselectedRows.remove(column);
//            }
//            preselectedRows.add(column, Integer.valueOf(row));
//            return;
//        }
//
//        // View exists
//        if (TiApplication.isUIThread()) {
//            handleSelectRow(column, row, animated);
//
//        } else {
//            KrollDict dict = new KrollDict();
//            dict.put("column", Integer.valueOf(column));
//            dict.put("row", Integer.valueOf(row));
//            dict.put("animated", Boolean.valueOf(animated));
//
//            TiMessenger.sendBlockingMainMessage(
//                    getMainHandler().obtainMessage(MSG_SELECT_ROW), dict);
//        }
//    }

    @Kroll.method
    public PickerRowProxy getSelectedRow(int columnIndex) {
        if (!isPlainPicker()) {
            Log.w(TAG,
                    "Cannot get selected row in date/time or countdown picker.",
                    Log.DEBUG_MODE);
            return null;
        }
        if (!(peekView() instanceof TiUIPicker)) {
            return null;
        }
        PickerRowProxy row = null;
        if (peekView() instanceof TiUIPicker) {
            int rowIndex = ((TiUIPicker) peekView())
                    .getSelectedRowIndex(columnIndex);
            if (rowIndex >= 0) {
                row = getRow(columnIndex, rowIndex);
            }
        }
        return row;
    }
    
    @Kroll.getProperty
    @Kroll.method
    public Object[] getSelectedRows() {
        if (!isPlainPicker()) {
            Log.w(TAG,
                    "Cannot get selected rows in date/time or countdown picker.",
                    Log.DEBUG_MODE);
            return null;
        }
        if (!(peekView() instanceof TiUIPicker)) {
            return getPreselectedRows();
        }
        int columnsCount = getColumnCount();
        Object[] result = new Object[columnsCount];
        for (int i = 0; i < columnsCount; i++) {
            result[i] =  ((TiUIPicker) peekView()).getSelectedRowIndex(i);
        }
        return result;
    }

    @Kroll.getProperty
    @Kroll.method
    public Object[] getColumns() {
        if (!isPlainPicker()) {
            Log.w(TAG,
                    "Cannot get columns from date/time or countdown picker.",
                    Log.DEBUG_MODE);
            return null;
        }
        return getChildren();
    }

    @Kroll.setProperty
    @Kroll.method
    public void setColumns(Object value) {
        if (!isPlainPicker()) {
            Log.w(TAG, "Cannot set columns in date/time or countdown picker.",
                    Log.DEBUG_MODE);
            return;
        }
//        if (TiApplication.isUIThread() || peekView() == null) {
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).batchModelChange = true;
        }
        removeAllChildren();
        if (value instanceof Object[]) {
            Object[] columns = (Object[])value;
            for (int i = 0; i < columns.length; i++) {
                Object column = columns[i];
                if (column instanceof Object[]) {
                    Object[] rows = (Object[])column;
                    PickerColumnProxy columnProxy = getColumn(i, true);
                    for (int j = 0; j < rows.length; j++) {
                        Object row = rows[j];
                        HashMap params = null;
                        if (row instanceof HashMap) {
                            params = (HashMap) row;
                        } else {
                            params = new HashMap<>();
                            params.put(TiC.PROPERTY_TITLE, TiConvert.toString(row));
                        }
                        columnProxy.add(params, null);
                    }
                }
            }
        }
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).batchModelChange = false;
            ((TiUIPicker) peekView()).onModelReplaced();
        }
    }
    
    @Kroll.setProperty
    @Kroll.method
    public void setRows(Object value) {
        if (!isPlainPicker()) {
            Log.w(TAG, "Cannot set columns in date/time or countdown picker.",
                    Log.DEBUG_MODE);
            return;
        }
//        if (TiApplication.isUIThread() || peekView() == null) {
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).batchModelChange = true;
        }
        removeAllChildren();
        if (value instanceof Object[]) {
            Object[] rows = (Object[])value;
            PickerColumnProxy columnProxy = getColumn(0, true);
            for (int i = 0; i < rows.length; i++) {
                Object row = rows[i];
                HashMap params = null;
                if (row instanceof HashMap) {
                    params = (HashMap) row;
                } else {
                    params = new HashMap<>();
                    params.put(TiC.PROPERTY_TITLE, TiConvert.toString(row));
                }
                columnProxy.add(params, null);
            }
        }
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).batchModelChange = false;
            ((TiUIPicker) peekView()).onModelReplaced();
        }
    }

//    private void handleSelectRow(KrollDict dict) {
//        handleSelectRow(dict.getInt("column"), dict.getInt("row"),
//                dict.getBoolean("animated"));
//    }

	private void handleSelectRow(int column, int row, boolean animated)
	{
		if (peekView() == null) {
			return;
		}
		((TiUIPicker)peekView()).selectRow(column, row, animated);
		if (TiConvert.toBoolean(getProperty(TiC.PROPERTY_SELECTION_OPENS), false)) {
			((TiUIPicker)peekView()).openPicker();
		}
	}

    public int getColumnCount() {
        return getChildrenCount();
    }

    public PickerColumnProxy getColumn(int index) {
        if (children == null || index >= children.size()
                || (!(children.get(index) instanceof PickerColumnProxy))) {
            return null;
        } else {
            return (PickerColumnProxy) children.get(index);
        }
    }
    
    public PickerColumnProxy getColumn(int index, final boolean createIfMissing) {
        PickerColumnProxy column = getColumn(index);
        if (column == null && createIfMissing) {
            column = new PickerColumnProxy();
            column.handleCreationDict(getProperties());
            column.setCreateIfMissing(true);
            add(column);
        }
        return column;
    }

    public int getColumnIndex(PickerColumnProxy column) {
        if (children != null && children.size() > 0) {
            return children.indexOf(column);
        } else {
            return -1;
        }
    }

    public PickerRowProxy getRow(int columnIndex, int rowIndex) {
        PickerColumnProxy column = getColumn(columnIndex);
        if (column == null) {
            return null;
        }
        KrollProxy[] rowArray = column.getChildren();
        if (rowArray == null || rowIndex >= rowArray.length
                || (!(rowArray[rowIndex] instanceof PickerRowProxy))) {
            return null;
        } else {
            return (PickerRowProxy) rowArray[rowIndex];
        }
    }

    public PickerColumnProxy getFirstColumn(boolean createIfMissing) {
        return getColumn(0, createIfMissing);
    }

    // This is meant to be a kind of "static" method, in the sense that
    // it doesn't use any state except for context. It's a quick hit way
    // of getting a date dialog up, in other words.
    @SuppressLint("NewApi")
    @Kroll.method
    public void showDatePickerDialog(Object[] args) {
        HashMap settings = new HashMap();
        final AtomicInteger callbackCount = new AtomicInteger(0); // just a flag
                                                                  // to be sure
                                                                  // dismiss
                                                                  // doesn't
                                                                  // fire
                                                                  // callback if
                                                                  // ondateset
                                                                  // did
                                                                  // already.
        if (args.length > 0) {
            settings = (HashMap) args[0];
        }
        Calendar calendar = Calendar.getInstance();
        if (settings.containsKey("value")) {
            calendar.setTime(TiConvert.toDate(settings, "value"));
        }

        final KrollFunction callback;
        if (settings.containsKey("callback")) {
            Object typeTest = settings.get("callback");
            if (typeTest instanceof KrollFunction) {
                callback = (KrollFunction) typeTest;
            } else {
                callback = null;
            }
        } else {
            callback = null;
        }
        DatePickerDialog.OnDateSetListener dateSetListener = null;
        DialogInterface.OnDismissListener dismissListener = null;
        if (callback != null) {
            dateSetListener = new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker picker, int year,
                        int monthOfYear, int dayOfMonth) {
                    if (callback != null) {
                        callbackCount.incrementAndGet();
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.YEAR, year);
                        calendar.set(Calendar.MONTH, monthOfYear);
                        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        Date value = calendar.getTime();
                        KrollDict data = new KrollDict();
                        data.put("cancel", false);
                        data.put("value", value);
                        callback.callAsync(getKrollObject(),
                                new Object[] { data });
                    }
                }
            };
            dismissListener = new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (callbackCount.get() == 0 && callback != null) {
                        callbackCount.incrementAndGet();
                        KrollDict data = new KrollDict();
                        data.put("cancel", true);
                        data.put("value", null);
                        callback.callAsync(getKrollObject(),
                                new Object[] { data });
                    }
                }
            };
        }

        /*
         * use getAppCurrentActivity over getActivity since technically the
         * picker should show up on top of the current activity when called -
         * not just the activity it was created in
         */

        // DatePickerDialog has a bug in Android 4.x
        // If build version is using Android 4.x, use
        // our TiDatePickerDialog. It was fixed from Android 5.0.
        DatePickerDialog dialog;

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
            dialog = new TiDatePickerDialog(
                    TiApplication.getAppCurrentActivity(), dateSetListener,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
        } else {
            dialog = new DatePickerDialog(
                    TiApplication.getAppCurrentActivity(), dateSetListener,
                    calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
        }

        Date minMaxDate = null;
        if (settings.containsKey(TiC.PROPERTY_MIN_DATE)) {
            minMaxDate = (Date) settings.get(TiC.PROPERTY_MIN_DATE);
        } else if (properties.containsKey(TiC.PROPERTY_MIN_DATE)) {
            minMaxDate = (Date) properties.get(TiC.PROPERTY_MIN_DATE);
        }
        if (minMaxDate != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            dialog.getDatePicker().setMinDate(trimDate(minMaxDate).getTime());
        }
        minMaxDate = null;
        if (settings.containsKey(TiC.PROPERTY_MAX_DATE)) {
            minMaxDate = (Date) settings.get(TiC.PROPERTY_MAX_DATE);
        } else if (properties.containsKey(TiC.PROPERTY_MAX_DATE)) {
            minMaxDate = (Date) properties.get(TiC.PROPERTY_MAX_DATE);
        }
        if (minMaxDate != null
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            dialog.getDatePicker().setMaxDate(trimDate(minMaxDate).getTime());
        }

        dialog.setCancelable(true);
        if (dismissListener != null) {
            dialog.setOnDismissListener(dismissListener);
        }
        if (settings.containsKey("title")) {
            dialog.setTitle(TiConvert.toString(settings, "title"));
        }
        dialog.show();
        if (settings.containsKey("okButtonTitle")) {
            dialog.getButton(DatePickerDialog.BUTTON_POSITIVE).setText(
                    TiConvert.toString(settings, "okButtonTitle"));
        }
    }

    /**
     * Trim hour, minute, second and millisecond from the date
     * 
     * @param inDate
     *            input date
     * @return return the trimmed date
     */
    public static Date trimDate(Date inDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inDate);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // This is meant to be a kind of "static" method, in the sense that
    // it doesn't use any state except for context. It's a quick hit way
    // of getting a date dialog up, in other words.
    @Kroll.method
    public void showTimePickerDialog(Object[] args) {
        HashMap settings = new HashMap();
        boolean is24HourView = false;
        final AtomicInteger callbackCount = new AtomicInteger(0); // just a flag
                                                                  // to be sure
                                                                  // dismiss
                                                                  // doesn't
                                                                  // fire
                                                                  // callback if
                                                                  // ondateset
                                                                  // did
                                                                  // already.
        if (args.length > 0) {
            settings = (HashMap) args[0];
        }
        if (settings.containsKey("format24")) {
            is24HourView = TiConvert.toBoolean(settings, "format24");
        }
        Calendar calendar = Calendar.getInstance();
        if (settings.containsKey("value")) {
            calendar.setTime(TiConvert.toDate(settings, "value"));
        }

        final KrollFunction callback;
        if (settings.containsKey("callback")) {
            Object typeTest = settings.get("callback");
            if (typeTest instanceof KrollFunction) {
                callback = (KrollFunction) typeTest;
            } else {
                callback = null;
            }
        } else {
            callback = null;
        }
        TimePickerDialog.OnTimeSetListener timeSetListener = null;
        DialogInterface.OnDismissListener dismissListener = null;
        if (callback != null) {
            timeSetListener = new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker field, int hourOfDay,
                        int minute) {
                    if (callback != null) {
                        callbackCount.incrementAndGet();
                        Calendar calendar = Calendar.getInstance();
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        Date value = calendar.getTime();
                        KrollDict data = new KrollDict();
                        data.put("cancel", false);
                        data.put("value", value);
                        callback.callAsync(getKrollObject(),
                                new Object[] { data });
                    }
                }
            };
            dismissListener = new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (callbackCount.get() == 0 && callback != null) {
                        callbackCount.incrementAndGet();
                        KrollDict data = new KrollDict();
                        data.put("cancel", true);
                        data.put("value", null);
                        callback.callAsync(getKrollObject(),
                                new Object[] { data });
                    }
                }
            };
        }

        // TimePickerDialog has a bug in Android 4.x
        // If build version is using Android 4.x, use
        // our TiTimePickerDialog. It was fixed from Android 5.0.
        TimePickerDialog dialog;

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                && (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)) {
            dialog = new TiTimePickerDialog(getActivity(), timeSetListener,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), is24HourView);
        } else {
            dialog = new TimePickerDialog(getActivity(), timeSetListener,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE), is24HourView);
        }

        dialog.setCancelable(true);
        if (dismissListener != null) {
            dialog.setOnDismissListener(dismissListener);
        }
        if (settings.containsKey("title")) {
            dialog.setTitle(TiConvert.toString(settings, "title"));
        }
        dialog.show();
        if (settings.containsKey("okButtonTitle")) {
            dialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setText(
                    TiConvert.toString(settings, "okButtonTitle"));
        }
    }

    private void fireColumnModelChange(int columnIndex) {
        if (!(peekView() instanceof TiUIPicker)) {
            return;
        }
        if (TiApplication.isUIThread()) {
            handleFireColumnModelChange(columnIndex);
        } else {
            Message message = getMainHandler().obtainMessage(
                    MSG_FIRE_COL_CHANGE);
            // Message msg = getUIHandler().obtainMessage(MSG_FIRE_COL_CHANGE);
            message.arg1 = columnIndex;
            message.sendToTarget();
        }
    }

    private void handleFireColumnModelChange(int columnIndex) {
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).onColumnModelChanged(columnIndex);
        }
    }

    private void fireRowChange(int columnIndex, int rowIndex) {
        if (!(peekView() instanceof TiUIPicker)) {
            return;
        }
        if (TiApplication.isUIThread()) {
            handleFireRowChange(columnIndex, rowIndex);
        } else {
            Message message = getMainHandler().obtainMessage(
                    MSG_FIRE_ROW_CHANGE);
            // Message msg = getUIHandler().obtainMessage(MSG_FIRE_ROW_CHANGE);
            message.arg1 = columnIndex;
            message.arg2 = rowIndex;
            message.sendToTarget();
        }
    }

    private void handleFireRowChange(int columnIndex, int rowIndex) {
        if (peekView() instanceof TiUIPicker) {
            ((TiUIPicker) peekView()).onRowChanged(columnIndex, rowIndex);
        }
    }

    public void fireSelectionChange(int columnIndex, int rowIndex) {
        if (hasListeners(TiC.EVENT_CHANGE)) {
            KrollDict d = new KrollDict();
            d.put("columnIndex", columnIndex);
            d.put("rowIndex", rowIndex);
            PickerColumnProxy column = getColumn(columnIndex);
            PickerRowProxy row = getRow(columnIndex, rowIndex);
            d.put("column", column);
            d.put("row", row);
            int columnCount = getColumnCount();
            ArrayList<String> selectedValues = new ArrayList<String>(
                    columnCount);
            for (int i = 0; i < columnCount; i++) {
                PickerRowProxy rowInColumn = getSelectedRow(i);
                if (rowInColumn != null) {
                    selectedValues.add(rowInColumn.toString());
                } else {
                    selectedValues.add(null);
                }
            }
            d.put("selectedValue", selectedValues.toArray());
            fireEvent(TiC.EVENT_CHANGE, d, false, false);
        }
    }

    @Override
    public void rowAdded(PickerColumnProxy column, int rowIndex) {
        fireColumnModelChange(children.indexOf(column));
    }

    @Override
    public void rowRemoved(PickerColumnProxy column, int oldRowIndex) {
        fireColumnModelChange(children.indexOf(column));
    }

    @Override
    public void rowsReplaced(PickerColumnProxy column) {
        fireColumnModelChange(children.indexOf(column));
    }

    @Override
    public void rowChanged(PickerColumnProxy column, int rowIndex) {
        fireRowChange(children.indexOf(column), rowIndex);
    }

    @Override
    public void rowSelected(PickerColumnProxy column, int rowIndex) {
        int columnIndex = children.indexOf(column);
        fireSelectionChange(columnIndex, rowIndex);
    }

    public Object[] getPreselectedRows() {
        Object value = getProperty(TiC.PROPERTY_SELECTED_ROW);
        if (value instanceof Object[]) {
            return (Object[]) value;
        }
        return null;
    }

    public void forceRequestLayout() {
        if (!(peekView() instanceof TiUISpinner)) {
            return;
        }
        if (TiApplication.isUIThread()) {
            handleForceRequestLayout();
        } else {
            getMainHandler().obtainMessage(MSG_FORCE_LAYOUT).sendToTarget();
            // getUIHandler().obtainMessage(MSG_FORCE_LAYOUT).sendToTarget();
        }
    }

    private void handleForceRequestLayout() {
        ((TiUISpinner) view).forceRequestLayout();
    }

    @Override
    public String getApiName() {
        return "Ti.UI.Picker";
    }
}
