/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.picker;

import java.util.Calendar;
import java.util.Date;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;

public class TiUIDatePicker extends TiUIView
	implements OnDateChangedListener
{
	private boolean suppressChangeEvent = false;
	private static final String TAG = "TiUIDatePicker";

	protected Date minDate, maxDate;
//	protected int minuteInterval;
	
	public TiUIDatePicker(TiViewProxy proxy)
	{
		super(proxy);
	}
	public TiUIDatePicker(TiViewProxy proxy, Activity activity)
	{
		this(proxy);
		Log.d(TAG, "Creating a date picker", Log.DEBUG_MODE);
		
		DatePicker picker = new DatePicker(activity)
		{
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUIDatePicker.this);
			}
		};
		picker.setCalendarViewShown(false);
		setNativeView(picker);
	}
	
	private DatePicker getPicker() {
	    return (DatePicker) nativeView;
	}
		
    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_VALUE:
            setValue(newValue);
            break;
        case TiC.PROPERTY_MIN_DATE:
        {
            this.minDate = TiConvert.toDate(newValue);
            Log.d(Log.DEBUG_MODE, "minDate " + minDate.toLocaleString());
            if (TiC.HONEYCOMB_OR_GREATER) {
                getPicker().setMinDate(this.minDate.getTime() - 1000);
            }
            break;
        }
        case TiC.PROPERTY_MAX_DATE:
        {
            this.maxDate = TiConvert.toDate(newValue);
            if (TiC.HONEYCOMB_OR_GREATER) {
                getPicker().setMaxDate(this.maxDate.getTime());
            }
            break;
        }
        case TiC.PROPERTY_CALENDAR_VIEW_SHOWN:
            if (TiC.HONEYCOMB_OR_GREATER) {
                getPicker().setCalendarViewShown(TiConvert.toBoolean(newValue, false));
            }
            break;
        case "spinnerShown":
            if (TiC.HONEYCOMB_OR_GREATER) {
                getPicker().setSpinnersShown(TiConvert.toBoolean(newValue, false));
            }
            break;
        case "firstDayOfTheWeek":
            if (TiC.HONEYCOMB_OR_GREATER) {
                getPicker().setFirstDayOfWeek(TiConvert.toInt(newValue, 1));
            }
            break;
//        case "minuteInterval":
//            int mi = TiConvert.toInt(newValue, 0);
//            if (mi >= 1 && mi <= 30 && mi % 60 == 0) {
//                this.minuteInterval = mi; 
//            }
//            break;

        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
	
	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
		

        if (!d.containsKey(TiC.PROPERTY_VALUE)) {
            //make sure .init is called
            setValue(Calendar.getInstance().getTime());
        }
        
        //iPhone ignores both values if max <= min
        if (minDate != null && maxDate != null) {
            if (maxDate.compareTo(minDate) <= 0) {
                Log.w(TAG, "maxDate is less or equal minDate, ignoring both settings.");
                minDate = null;
                maxDate = null;
            }   
        }
	}
	
	@Override
	public void onDateChanged(DatePicker picker, int year, int monthOfYear, int dayOfMonth)
	{
    	Calendar targetCalendar = Calendar.getInstance();
    	targetCalendar.set(Calendar.YEAR, year);
    	targetCalendar.set(Calendar.MONTH, monthOfYear);
    	targetCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
    	targetCalendar.set(Calendar.HOUR_OF_DAY, 0);
    	targetCalendar.set(Calendar.MINUTE, 0);
    	targetCalendar.set(Calendar.SECOND, 0);
    	targetCalendar.set(Calendar.MILLISECOND, 0);

		if ((null != minDate) && (targetCalendar.getTime().before(minDate))) {
			targetCalendar.setTime(minDate);
			setValue(minDate.getTime(), true);
		}
		if ((null != maxDate) && (targetCalendar.getTime().after(maxDate))) {
			targetCalendar.setTime(maxDate);
			setValue(maxDate.getTime(), true);
		}
		
		Date newTime = targetCalendar.getTime();
		Object oTime = proxy.getProperty(TiC.PROPERTY_VALUE);
		Date oldTime = null;
		
		if (oTime instanceof Date) {
			oldTime = (Date) oTime;
		}
		
		// Due to a native Android bug in 4.x, this callback is called twice, so here 
		// we check if the dates are identical, we don't fire "change" event or reset value.
		if (oldTime != null && oldTime.equals(newTime)) {
			return;
		}

		if (!suppressChangeEvent) {
		    if (hasListeners(TiC.EVENT_CHANGE)) {
                KrollDict data = new KrollDict();
                data.put(TiC.PROPERTY_VALUE, targetCalendar.getTime());
                fireEvent(TiC.EVENT_CHANGE, data);
            }
	        proxy.setProperty(TiC.PROPERTY_VALUE, targetCalendar.getTime());
		}
	}
	
	public void setValue(long value)
	{
		setValue(value, false);
	}
	
	public void setValue(Date value, boolean suppressEvent) {
        long millis = (value != null)?value.getTime():Calendar.getInstance().getTimeInMillis();
        setValue(millis, suppressEvent);
    }
	
	public void setValue(Object value)
    {
        setValue(TiConvert.toDate(value), false);
    }
	
	public void setValue(long value, boolean suppressEvent)
	{
		DatePicker picker = (DatePicker) getNativeView();
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(value);
		suppressChangeEvent = suppressEvent;
		if (((TiViewProxy)proxy).viewRealised()) {
		    picker.updateDate(calendar.get(Calendar.YEAR), calendar
	                .get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
		}
		else {
	        suppressChangeEvent = true;
		    picker.init(calendar.get(Calendar.YEAR), calendar
	                .get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), this);
		}
        suppressChangeEvent = false;
		
	}
}
