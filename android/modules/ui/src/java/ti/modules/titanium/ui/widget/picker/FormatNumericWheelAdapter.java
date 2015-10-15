/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.picker;

import java.text.NumberFormat;

import android.content.Context;
import antistatic.spinnerwheel.adapters.NumericWheelAdapter;

public class FormatNumericWheelAdapter extends NumericWheelAdapter {
    private NumberFormat formatter;
    private int maxCharacterLength = 2;
    private int stepValue = 1;
	
	public FormatNumericWheelAdapter(Context context, int minValue, int maxValue, NumberFormat formatter, int maxCharLength)
	{
		this(context, minValue,maxValue,formatter,maxCharLength, 1);
	}
	
	public FormatNumericWheelAdapter(Context context, int minValue, int maxValue, NumberFormat formatter, int maxCharLength, int stepValue)
	{
		super(context, minValue, maxValue);
		this.formatter = formatter;
        this.maxCharacterLength = maxCharLength;
        this.stepValue = stepValue;
    }

    public void setFormatter(NumberFormat formatter) {
        this.formatter = formatter;
    }
    
    public int getIndex(int value) {
        return (value - getMinValue()) / stepValue;
    }
    public void setStepValue(int value)
    {
        this.stepValue = value;
    }
    public int getValue(int index) {
        int tmpValue = (getMinValue() + index * stepValue);
        if (tmpValue > getMaxValue())
            return getMaxValue();
        else
            return tmpValue;    
    }

    @Override
    public int getItemsCount() {
        int itemCount = ( (getMaxValue() - getMinValue()) / stepValue) + 1;
        return itemCount;
    }

    @Override
    public CharSequence getItemText(int index) {
        int actualValue = getValue(index);
        if (formatter == null) {
            return Integer.toString(actualValue);
        } else {
            return formatter.format(actualValue);
        }
    }

    public int getMaximumLength()
	{
		return maxCharacterLength;
	}
	
	public void setMaximumLength(int value) 
	{
		maxCharacterLength = value;
	}
}
