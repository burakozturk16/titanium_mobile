/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.picker;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.content.Context;
import android.widget.LinearLayout;
import antistatic.spinnerwheel.AbstractWheel;
import antistatic.spinnerwheel.OnWheelChangedListener;
import antistatic.spinnerwheel.WheelVerticalView;
import antistatic.spinnerwheel.adapters.AbstractWheelTextAdapter;

public class TiUITimeSpinner extends TiUIView
		implements OnWheelChangedListener
{
	private WheelVerticalView hoursWheel;
	private WheelVerticalView minutesWheel;
	private WheelVerticalView amPmWheel;
	private boolean suppressChangeEvent = false;
	private boolean ignoreItemSelection = false;
	private static final String TAG = "TiUITimeSpinner";
    boolean is24HourFormat = true;
	
	private Calendar calendar = Calendar.getInstance();
	
	public TiUITimeSpinner(TiViewProxy proxy)
	{
		super(proxy);
	}
	public TiUITimeSpinner(TiViewProxy proxy, Activity activity)
	{
		this(proxy);
		createNativeView(activity);
	}
	
	private FormatNumericWheelAdapter makeHoursAdapter(boolean format24) {
		DecimalFormat formatter = new DecimalFormat("00");
		return new FormatNumericWheelAdapter(getProxy().getActivity(),
				format24 ? 0 : 1,
				format24 ? 23 : 12,
				formatter, 6);
	}

	private WheelVerticalView makeAmPmWheel(Context context, int textSize)
	{
		ArrayList<Object> amPmRows = new ArrayList<Object>();
		amPmRows.add(" am ");
		amPmRows.add(" pm ");
		WheelVerticalView view = new WheelVerticalView(context);
		TextWheelAdapter adapter = new TextWheelAdapter(context, amPmRows);
		view.setViewAdapter(adapter);
		adapter.setTextSize(textSize);
		view.addChangingListener(this);
		return view;
	}
	private void createNativeView(Activity activity)
	{
        is24HourFormat = TiConvert.toBoolean( proxy.getProperty(  "format24" ), true );

		int minuteInterval = 1;
		if (proxy.hasProperty(TiC.PROPERTY_MINUTE_INTERVAL)) {
			int dirtyMinuteInterval = TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_MINUTE_INTERVAL));
			if((dirtyMinuteInterval > 0) && (dirtyMinuteInterval <= 30) && (60 % dirtyMinuteInterval == 0)  ){
				minuteInterval = dirtyMinuteInterval;
			} else {
				Log.w(TAG, "Clearing invalid minuteInterval property value of " + dirtyMinuteInterval);
				proxy.setProperty(TiC.PROPERTY_MINUTE_INTERVAL, null);
			}
		}
		
		DecimalFormat formatter = new DecimalFormat("00");
		FormatNumericWheelAdapter hours = makeHoursAdapter(is24HourFormat);
		FormatNumericWheelAdapter minutes = new FormatNumericWheelAdapter(activity, 0, 59, formatter, 6);
		hoursWheel = new WheelVerticalView(activity);
		minutesWheel = new WheelVerticalView(activity);
		hours.setTextSize(20);
		minutes.setTextSize(hours.getTextSize());
		hoursWheel.setViewAdapter(hours);
		minutesWheel.setViewAdapter(minutes);
		hoursWheel.addChangingListener(this);
		minutesWheel.addChangingListener(this);

		amPmWheel = null;
		
		if ( !is24HourFormat ) {
			amPmWheel = makeAmPmWheel(activity, hours.getTextSize());
		}

		LinearLayout layout = new LinearLayout(activity);
		layout.setOrientation(LinearLayout.HORIZONTAL);
		layout.addView(hoursWheel);
		layout.addView(minutesWheel);
		if ( !is24HourFormat ) {
			layout.addView(amPmWheel);
		}
		
		setNativeView(layout);
	}

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        switch (key) {
        case TiC.PROPERTY_FONT:
            FontDesc desc = TiUIHelper.getFontStyle(getContext(),
                    TiConvert.toKrollDict(newValue));
            ((AbstractWheelTextAdapter) hoursWheel.getViewAdapter()).setTextTypeface(desc.typeface);
            ((AbstractWheelTextAdapter) hoursWheel.getViewAdapter()).setTextSize(desc.size.intValue());
            ((AbstractWheelTextAdapter) minutesWheel.getViewAdapter()).setTextTypeface(desc.typeface);
            ((AbstractWheelTextAdapter) minutesWheel.getViewAdapter()).setTextSize(desc.size.intValue());
            hoursWheel.invalidate();
            minutesWheel.invalidate();
            if (amPmWheel != null) {
                ((AbstractWheelTextAdapter) amPmWheel.getViewAdapter()).setTextTypeface(desc.typeface);
                ((AbstractWheelTextAdapter) amPmWheel.getViewAdapter()).setTextSize(desc.size.intValue());
                amPmWheel.invalidate();
            }
            break;
        case TiC.PROPERTY_VALUE:
            setValue(newValue);
            break;
        case "format24":
            is24HourFormat = TiConvert.toBoolean(newValue, true);
            if (changedProperty) {
                ignoreItemSelection = true;
                suppressChangeEvent = true;
               int textSize = ((AbstractWheelTextAdapter) hoursWheel.getViewAdapter()).getTextSize();
                hoursWheel.setViewAdapter(makeHoursAdapter(is24HourFormat));
                LinearLayout vg = (LinearLayout) nativeView;
                if (is24HourFormat && vg.indexOfChild(amPmWheel) >= 0) {
                    vg.removeView(amPmWheel);
                } else if (!is24HourFormat && vg.getChildCount() < 3) {
                    amPmWheel = makeAmPmWheel(hoursWheel.getContext(), textSize);
                    vg.addView(amPmWheel);
                }
                setValue(calendar.getTimeInMillis(), true); // updates the time
                                                            // display
                ignoreItemSelection = false;
                suppressChangeEvent = false;
            }
            break;
        case TiC.PROPERTY_MINUTE_INTERVAL:
            if (changedProperty) {
                int interval = TiConvert.toInt(newValue);
                if ((interval > 0) && (interval <= 30) && (60 % interval == 0)) {
                    FormatNumericWheelAdapter adapter = (FormatNumericWheelAdapter) minutesWheel
                            .getViewAdapter();
                    adapter.setStepValue(interval);
                    minutesWheel.setViewAdapter(adapter); // forces the wheel to
                                                      // re-do its items listing
                } else {
                    // Reject it
                    Log.w(TAG, "Ignoring illegal minuteInterval value: "
                            + interval);
                    proxy.setProperty(TiC.PROPERTY_MINUTE_INTERVAL, oldValue,
                            false);
                }
            }
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

	@Override
	public void processProperties(KrollDict d) {
		super.processProperties(d);
		if (!d.containsKey(TiC.PROPERTY_VALUE)) {
            Calendar calendar = Calendar.getInstance();
            proxy.setProperty(TiC.PROPERTY_VALUE, calendar.getTime());
        }
	}
	
	public void setValue(long value)
	{
		setValue(value, false);
	}
	
	public void setValue(long value, boolean suppressEvent)
	{
		calendar.setTimeInMillis(value);
		
		suppressChangeEvent = true;
		ignoreItemSelection = true;
		
		if ( !is24HourFormat ) {
			int hour = calendar.get(Calendar.HOUR);
			if (hour == 0) {
				hoursWheel.setCurrentItem(11); // 12
			} else {
				hoursWheel.setCurrentItem(hour - 1); // i.e., the visible "1" on the wheel is index 0.
			}
			if (calendar.get(Calendar.HOUR_OF_DAY) <= 11) {
				amPmWheel.setCurrentItem(0);
			} else {
				amPmWheel.setCurrentItem(1);
			}
		} else {
			hoursWheel.setCurrentItem(calendar.get(Calendar.HOUR_OF_DAY));
		}
		
		suppressChangeEvent = suppressEvent || !((TiViewProxy)proxy).viewRealised();
		ignoreItemSelection = false;
		minutesWheel.setCurrentItem( ((FormatNumericWheelAdapter) minutesWheel.getViewAdapter()).getIndex(calendar.get(Calendar.MINUTE)));
		suppressChangeEvent = false;
	}
	
	public void setValue(Date value, boolean suppressEvent) {
        long millis = (value != null)?value.getTime():Calendar.getInstance().getTimeInMillis();
        setValue(millis, suppressEvent);
    }
    
    public void setValue(Object value)
    {
        setValue(TiConvert.toDate(value), false);
    }
	
	@Override
    public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
		if (ignoreItemSelection) {
			return;
		}
		boolean format24 = true;
		if (proxy.hasProperty("format24")) {
			format24 = TiConvert.toBoolean(proxy.getProperty("format24"));
		}
		calendar.set(Calendar.MINUTE, ((FormatNumericWheelAdapter) wheel.getViewAdapter()).getValue(minutesWheel.getCurrentItem()));
		if ( !format24 ) {
			int hourOfDay = 0;
			if (hoursWheel.getCurrentItem() == 11) { // "12" on the dial
				if (amPmWheel.getCurrentItem() == 0) { // "am"
					hourOfDay = 0;
				} else {
					hourOfDay = 12;
				}
			} else {
				hourOfDay = 1 + (12 * amPmWheel.getCurrentItem()) + hoursWheel.getCurrentItem();
			}
			calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
		} else {
			calendar.set(Calendar.HOUR_OF_DAY, hoursWheel.getCurrentItem());
		}
		Date dateval = calendar.getTime();
		proxy.setProperty("value", dateval);
		if (!suppressChangeEvent) {
			KrollDict data = new KrollDict();
			data.put("value", dateval);
			fireEvent("change", data);
		}
	}

}
