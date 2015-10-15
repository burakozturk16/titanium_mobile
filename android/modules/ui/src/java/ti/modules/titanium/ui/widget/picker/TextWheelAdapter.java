/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */

package ti.modules.titanium.ui.widget.picker;


import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import antistatic.spinnerwheel.adapters.AbstractWheelTextAdapter;


public class TextWheelAdapter extends AbstractWheelTextAdapter
{
	private ArrayList<Object> values = null;
	
	public TextWheelAdapter(Context context, ArrayList<Object> values)
	{
	    super(context);
		setValues(values);
		
	}

	public TextWheelAdapter(Context context, Object[] values)
	{
		this(context, new ArrayList<Object>( Arrays.asList(values) ) );
	}

	@Override
	public CharSequence getItemText(int index)
	{
		if (values == null || index < values.size()) {
			return values.get(index).toString();
		} else {
			throw new ArrayIndexOutOfBoundsException(index);
		}
	}

	public void setValues(Object[] newValues)
	{
		setValues( new ArrayList<Object>( Arrays.asList(newValues) ) );
	}

	public void setValues(ArrayList<Object> newValues)
	{
		if (values != null) values.clear();
		this.values = newValues;
	}

	@Override
	public int getItemsCount()
	{
		return (values == null) ? 0 : values.size();
	}
	
}
