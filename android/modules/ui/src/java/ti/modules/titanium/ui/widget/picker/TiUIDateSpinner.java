/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget.picker;

import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.FontDesc;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import antistatic.spinnerwheel.AbstractWheel;
import antistatic.spinnerwheel.OnWheelChangedListener;
import antistatic.spinnerwheel.WheelVerticalView;

public class TiUIDateSpinner extends TiUIView implements
    OnWheelChangedListener {
    private static final String TAG = "TiUIDateSpinner";
    private WheelVerticalView monthWheel;
    private WheelVerticalView dayWheel;
    private WheelVerticalView yearWheel;

    private FormatNumericWheelAdapter monthAdapter;
    private FormatNumericWheelAdapter dayAdapter;
    private FormatNumericWheelAdapter yearAdapter;

    private boolean suppressChangeEvent = false;
    private boolean ignoreItemSelection = false;

    private Calendar maxDate = Calendar.getInstance(), minDate = Calendar
            .getInstance();
    private Locale locale = Locale.getDefault();
    private boolean dayBeforeMonth = false;
    private boolean numericMonths = false;

    private Calendar calendar = Calendar.getInstance();

    public TiUIDateSpinner(TiViewProxy proxy) {
        super(proxy);
    }

    public TiUIDateSpinner(TiViewProxy proxy, Activity activity) {
        this(proxy);
        createNativeView(activity);
    }

    private void createNativeView(Activity activity) {
        // defaults
        maxDate.set(calendar.get(Calendar.YEAR) + 100, 11, 31);
        minDate.set(calendar.get(Calendar.YEAR) - 100, 0, 1);

        monthWheel = new WheelVerticalView(activity);
        dayWheel = new WheelVerticalView(activity);
        yearWheel = new WheelVerticalView(activity);

//        monthWheel.setTextSize(20);
//        dayWheel.setTextSize(monthWheel.getTextSize());
//        yearWheel.setTextSize(monthWheel.getTextSize());

        monthWheel.addChangingListener(this);
        dayWheel.addChangingListener(this);
        yearWheel.addChangingListener(this);

        LinearLayout layout = new LinearLayout(activity) {
            @Override
            protected void onLayout(boolean changed, int left, int top,
                    int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                TiUIHelper.firePostLayoutEvent(TiUIDateSpinner.this);
            }
        };
        layout.setOrientation(LinearLayout.HORIZONTAL);

        if (proxy.hasProperty("dayBeforeMonth")) {
            // TODO dayBeforeMonth = TiConvert.toBoolean(proxy.getProperties(),
            // "dayBeforeMonth");
        }

        if (dayBeforeMonth) {
            addViewToPicker(dayWheel, layout);
            addViewToPicker(monthWheel, layout);
        } else {
            addViewToPicker(monthWheel, layout);
            addViewToPicker(dayWheel, layout);
        }

        addViewToPicker(yearWheel, layout);
        setAdapters();
        setNativeView(layout);

    }

    private void addViewToPicker(AbstractWheel v, LinearLayout layout) {
        layout.addView(v, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                (float) .33));
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
           Date date = TiConvert.toDate(newValue);
           if (date != null) {
                Calendar c = Calendar.getInstance();
                minDate.setTime(date);
                c.setTime(minDate.getTime());
            } else {
                minDate.set(calendar.get(Calendar.YEAR) - 100, 0, 1);
            }
            break;
        }
        case TiC.PROPERTY_MAX_DATE:
            Date date = TiConvert.toDate(newValue);
            if (date != null) {
                 Calendar c = Calendar.getInstance();
                 maxDate.setTime(date);
                 c.setTime(minDate.getTime());
             } else {
                 maxDate.set(calendar.get(Calendar.YEAR) + 100, 11, 31);
             }
            break;
        case "locale":
            setLocale(TiConvert.toString(newValue));
            break;
        case "dayBeforeMonth":
            dayBeforeMonth = TiConvert.toBoolean(newValue);
            break;
        case "numericMonths":
            numericMonths = TiConvert.toBoolean(newValue);
            break;
        case TiC.PROPERTY_FONT:
            FontDesc desc = TiUIHelper.getFontStyle(getContext(),
                    TiConvert.toKrollDict(newValue));
            dayAdapter.setTextTypeface(desc.typeface);
            monthAdapter.setTextTypeface(desc.typeface);
            yearAdapter.setTextTypeface(desc.typeface);
            dayAdapter.setTextSize(desc.size.intValue());
            monthAdapter.setTextSize(desc.size.intValue());
            yearAdapter.setTextSize(desc.size.intValue());
            yearWheel.invalidate();
            monthWheel.invalidate();
            dayWheel.invalidate();
            break;

        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

    @Override
    public void processProperties(KrollDict d) {
        super.processProperties(d);
        if (maxDate.before(minDate)) {
            maxDate.setTime(minDate.getTime());
        }

        // If initial value is out-of-bounds, set date to nearest bound
        if (calendar.after(maxDate)) {
            calendar.setTime(maxDate.getTime());
        } else if (calendar.before(minDate)) {
            calendar.setTime(minDate.getTime());
        }

        setValue(calendar.getTimeInMillis(), true);

        if (!d.containsKey(TiC.PROPERTY_VALUE)) {
            proxy.setProperty(TiC.PROPERTY_VALUE, calendar.getTime());
        }

    }

    private void setAdapters() {
        setYearAdapter();
        setMonthAdapter();
        setDayAdapter();
    }

    private void setYearAdapter() {
        int minYear = minDate.get(Calendar.YEAR);
        int maxYear = maxDate.get(Calendar.YEAR);
        if (yearAdapter != null && yearAdapter.getMinValue() == minYear
                && yearAdapter.getMaxValue() == maxYear) {
            return;
        }
        yearAdapter = new FormatNumericWheelAdapter(getProxy().getActivity(), minYear, maxYear,
                new DecimalFormat("0000"), 4);

        ignoreItemSelection = true;
        yearWheel.setViewAdapter(yearAdapter);
        ignoreItemSelection = false;
    }

    private void setMonthAdapter() {
        setMonthAdapter(false);
    }

    private void setMonthAdapter(boolean forceUpdate) {
        int setMinMonth = 1;
        int setMaxMonth = 12;

        int currentMin = -1, currentMax = -1;
        if (monthAdapter != null) {
            currentMin = monthAdapter.getMinValue();
            currentMax = monthAdapter.getMaxValue();
        }

        int maxYear = maxDate.get(Calendar.YEAR);
        int minYear = minDate.get(Calendar.YEAR);
        int selYear = getSelectedYear();

        if (selYear == maxYear) {
            setMaxMonth = maxDate.get(Calendar.MONTH) + 1;
        }

        if (selYear == minYear) {
            setMinMonth = minDate.get(Calendar.MONTH) + 1;
        }

        if (currentMin != setMinMonth || currentMax != setMaxMonth
                || forceUpdate) {
            NumberFormat format;
            int width = 4;
            if (numericMonths) {
                format = new DecimalFormat("00");
            } else {
                format = new MonthFormat(this.locale);
                width = ((MonthFormat) format).getLongestMonthName();
            }
            monthAdapter = new FormatNumericWheelAdapter(getProxy().getActivity(), setMinMonth,
                    setMaxMonth, format, width);
            ignoreItemSelection = true;
            monthWheel.setViewAdapter(monthAdapter);
            ignoreItemSelection = false;
        }

    }

    private void setDayAdapter() {
        int setMinDay = 1;
        int setMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int currentMin = -1, currentMax = -1;
        if (dayAdapter != null) {
            currentMin = dayAdapter.getMinValue();
            currentMax = dayAdapter.getMaxValue();
        }

        int maxYear = maxDate.get(Calendar.YEAR);
        int minYear = minDate.get(Calendar.YEAR);
        int selYear = getSelectedYear();
        int maxMonth = maxDate.get(Calendar.MONTH) + 1;
        int minMonth = minDate.get(Calendar.MONTH) + 1;
        int selMonth = getSelectedMonth();

        if (selYear == maxYear && selMonth == maxMonth) {
            setMaxDay = maxDate.get(Calendar.DAY_OF_MONTH);
        }

        if (selYear == minYear && selMonth == minMonth) {
            setMinDay = minDate.get(Calendar.DAY_OF_MONTH);
        }

        if (currentMin != setMinDay || currentMax != setMaxDay) {
            dayAdapter = new FormatNumericWheelAdapter(getProxy().getActivity(), setMinDay, setMaxDay,
                    new DecimalFormat("00"), 4);
            ignoreItemSelection = true;
            dayWheel.setViewAdapter(dayAdapter);
            ignoreItemSelection = false;
        }
    }

    private void syncWheels() {
        ignoreItemSelection = true;
        yearWheel.setCurrentItem(yearAdapter.getIndex(calendar
                .get(Calendar.YEAR)));
        monthWheel.setCurrentItem(monthAdapter.getIndex(calendar
                .get(Calendar.MONTH) + 1));
        dayWheel.setCurrentItem(dayAdapter.getIndex(calendar
                .get(Calendar.DAY_OF_MONTH)));
        ignoreItemSelection = false;
    }

    public void setValue(long value) {
        setValue(value, false);
    }

    public void setValue(long value, boolean suppressEvent) {
        if (((TiViewProxy) proxy).viewRealised()) {
            calendar.setTimeInMillis(value);
            return;
        }

        Date oldVal, newVal;
        oldVal = calendar.getTime();

        setCalendar(value);
        newVal = calendar.getTime();
        if (newVal.after(maxDate.getTime())) {
            newVal = maxDate.getTime();
            setCalendar(newVal);
        } else if (newVal.before(minDate.getTime())) {
            newVal = minDate.getTime();
            setCalendar(newVal);
        }

        boolean isChanged = (!newVal.equals(oldVal));

        setAdapters();

        syncWheels();
        proxy.setProperty(TiC.PROPERTY_VALUE, newVal);

        if (isChanged && !suppressEvent) {
            if (!suppressChangeEvent) {
                if (hasListeners(TiC.EVENT_CHANGE)) {
                    KrollDict data = new KrollDict();
                    data.put(TiC.PROPERTY_VALUE, newVal);
                    fireEvent(TiC.EVENT_CHANGE, data);
                }
            }

        }
    }

    public void setValue(Date value, boolean suppressEvent) {
        long millis = (value != null)?value.getTime():Calendar.getInstance().getTimeInMillis();
        setValue(millis, suppressEvent);
    }

    public void setValue(Date value) {
        setValue(value, false);
    }

    public void setValue(Object value) {
        setValue(TiConvert.toDate(value), false);
    }

    public void setValue() {
        setValue(getSelectedDate());
    }

    private void setLocale(String localeString) {
        Locale locale = Locale.getDefault();
        if (localeString != null && localeString.length() > 1) {
            String stripped = localeString.replaceAll("-", "").replaceAll("_",
                    "");
            if (stripped.length() == 2) {
                locale = new Locale(stripped);
            } else if (stripped.length() >= 4) {
                String language = stripped.substring(0, 2);
                String country = stripped.substring(2, 4);
                if (stripped.length() > 4) {
                    locale = new Locale(language, country,
                            stripped.substring(4));
                } else {
                    locale = new Locale(language, country);
                }
            } else {
                Log.w(TAG, "Locale string '" + localeString
                        + "' not understood.  Using default locale.");
            }
        }

        if (!this.locale.equals(locale)) {
            this.locale = locale;
            setMonthAdapter(true);
            syncWheels();
        }
    }

    private void setCalendar(long millis) {
        calendar.setTimeInMillis(millis);
    }

    private void setCalendar(Date date) {
        calendar.setTime(date);
    }

    private int getSelectedYear() {
        return yearAdapter.getValue(yearWheel.getCurrentItem());
    }

    private int getSelectedMonth() {
        return monthAdapter.getValue(monthWheel.getCurrentItem());
    }

    private int getSelectedDay() {
        return dayAdapter.getValue(dayWheel.getCurrentItem());
    }

    private Date getSelectedDate() {
        int year = getSelectedYear();
        int month = getSelectedMonth() - 1;
        int day = getSelectedDay();
        Calendar c = Calendar.getInstance();
        c.set(year, month, day);
        return c.getTime();
    }

    class MonthFormat extends NumberFormat {
        private static final long serialVersionUID = 1L;
        private DateFormatSymbols symbols = new DateFormatSymbols(
                Locale.getDefault());

        public MonthFormat(Locale locale) {
            super();
            setLocale(locale);
        }

        @Override
        public StringBuffer format(double value, StringBuffer buffer,
                FieldPosition position) {
            return format((long) value, buffer, position);
        }

        @Override
        public StringBuffer format(long value, StringBuffer buffer,
                FieldPosition position) {
            buffer.append(symbols.getMonths()[((int) value) - 1]);
            return buffer;
        }

        @Override
        public Number parse(String value, ParsePosition position) {
            String[] months = symbols.getMonths();
            for (int i = 0; i < months.length; i++) {
                if (months[i].equals(value)) {
                    return new Long(i + 1);
                }
            }
            return null;
        }

        public void setLocale(Locale locale) {
            symbols = new DateFormatSymbols(locale);
        }

        public int getLongestMonthName() {
            int max = 0;
            for (String month : symbols.getMonths()) {
                max = (month.length() > max) ? month.length() : max;
            }
            return max;
        }

    }

    @Override
    public void onChanged(AbstractWheel wheel, int oldValue, int newValue) {
        if (ignoreItemSelection) {
            return;
        }
        setValue();
    }

}
