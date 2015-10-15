/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.TiC;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

/**
 * An extension of HashMap, used to access and store data.
 */
public class KrollDict
	extends HashMap<String, Object>
{
	private static final String TAG = "KrollDict";
	private static final long serialVersionUID = 1L;
	private static final int INITIAL_SIZE = 5;

	/**
	 * Constructs a KrollDict with a default capacity.
	 * @module.api
	 */
	public KrollDict() {
		this(INITIAL_SIZE);
	}

	public KrollDict(JSONObject object) throws JSONException {
		for (Iterator<String> iter = object.keys(); iter.hasNext();) {
			String key = iter.next();
			Object value = object.get(key);			
			Object json = fromJSON(value);
			put(key, json);
		}
	}
	
	public KrollDict(final String jsonString) throws JSONException {
	    this(new JSONObject(jsonString));
	}
		
	public static Object fromJSON(Object value) {
		try {
			if (value instanceof JSONObject) {
				return new KrollDict((JSONObject)value);

			} else if (value instanceof JSONArray) {
				JSONArray array = (JSONArray)value;
				Object[] values = new Object[array.length()];
				for (int i = 0; i < array.length(); i++) {
					values[i] = fromJSON(array.get(i));
				}
				return values;

			} else if (value == JSONObject.NULL) {
				return null;

			}
		} catch (JSONException e) {
			Log.e(TAG, "Error parsing JSON", e);
		}

		return value;
	}

	/**
	 * Constructs a KrollDict by copying an existing Map
	 * @param map the existing map to copy
	 * @module.api
	 */
	public KrollDict(Map<? extends String, ? extends Object> map) {
		super(map);
	}

	/**
	 * Constructs a KrollDict with the specified capacity.
	 * @param size the specified capacity.
	 * @module.api
	 */
	public KrollDict(int size) {
		super(size);
	}

	public void putCodeAndMessage(int code, String message) {
		this.put(TiC.PROPERTY_SUCCESS,new Boolean(code==0));
		this.put(TiC.PROPERTY_CODE,new Integer(code));
		if (message != null){
			this.put(TiC.EVENT_PROPERTY_ERROR,message);
		}
	}
	
	public void putCodeAndMessage(boolean success, int code, String message) {
        this.put(TiC.PROPERTY_SUCCESS,new Boolean(success));
        this.put(TiC.PROPERTY_CODE,new Integer(code));
        if (message != null){
            this.put(TiC.EVENT_PROPERTY_ERROR,message);
        }
    }


	public boolean containsKeyAndNotNull(String key) {
		return containsKey(key) && get(key) != null;
	}

	public boolean containsKeyStartingWith(String keyStartsWith) {
		if (keySet() != null) { 
			for (String key : keySet()) {
				if (key.startsWith(keyStartsWith)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean equalsKrollDict(KrollDict otherDict)
	{
		if (otherDict.size() != size()) return false;
		for(Map.Entry<String, Object> e: entrySet()){
			String key = e.getKey();
			Object newvalue = e.getValue();
			if (!otherDict.containsKeyWithValue(key, newvalue))
				return false;
		}
		return true;
	}
	
	public boolean containsKeyWithValue(String key, Object value) {
		
		Object myValue = get(key);
		
		if (myValue == null || value == null) return (myValue == value);
		boolean result;
		if (myValue instanceof KrollDict && value instanceof KrollDict)
			result = ((KrollDict)myValue).equalsKrollDict((KrollDict)value);
		else
		{
			result = value.equals(myValue);
		}
		return result;
	}
	
	public boolean getBoolean(String key) {
		return TiConvert.toBoolean(get(key));
	}

	public boolean optBoolean(String key, boolean defaultValue) {
		boolean result = defaultValue;

		if (containsKey(key) && get(key) != null) {
			result = getBoolean(key);
		}
		return result;
	}

	public String getString(String key) {
		return TiConvert.toString(get(key));
	}

	public String optString(String key, String defalt) {
		if (containsKey(key)) {
			return getString(key);
		}
		return defalt;
	}

	public Integer getInt(String key) {
		return TiConvert.toInt(get(key));
	}

	public Integer optInt(String key, Integer defaultValue) {
		Integer result = defaultValue;

		if (containsKey(key)) {
			result = TiConvert.toInt(get(key), defaultValue);
		}
		return result;
	}

	public Double getDouble(String key) {
		return TiConvert.toDouble(get(key));
	}

	public Double optDouble(String key, Double defaultValue) {
		Double result = defaultValue;

		if (containsKey(key)) {
			result =  TiConvert.toDouble(get(key), defaultValue);
		}
		return result;
	}

	public float getFloat(String key) {
		return TiConvert.toFloat(get(key));
	}

	public float optFloat(String key, float defaultValue) {
		float result = defaultValue;

		if (containsKey(key)) {
			result = TiConvert.toFloat(get(key), defaultValue);
		}
		return result;
	}

	public int getColor(String key) {
		return TiConvert.toColor(getString(key));
	}

	public int optColor(String key, int defaultValue) {
		int result = defaultValue;

		if (get(key) != null) {
			result = getColor(key);
		}
		return result;
	}

	public String[] getStringArray(String key) {
		return TiConvert.toStringArray((Object[])get(key));
	}

	public String[] optStringArray(String key, String[] defaultValue) {
		String[] result = defaultValue;

		if (containsKey(key)) {
			result = getStringArray(key);
		}
		return result;
	}

	public int[] getIntArray(String key) {
		return TiConvert.toIntArray((Object[])get(key));
	}

	public int[] optIntArray(String key, int[] defaultValue) {
		int[] result = defaultValue;

		if (containsKey(key)) {
			result = getIntArray(key);
		}
		return result;
	}

	public float[] getFloatArray(String key) {
		return TiConvert.toFloatArray((Object[])get(key));
	}

	public float[] optFloatArray(String key, float[] defaultValue) {
		float[] result = defaultValue;

		if (containsKey(key)) {
			result = getFloatArray(key);
		}
		return result;
	}

	public double[] getDoubleArray(String key) {
		return TiConvert.toDoubleArray((Object[])get(key));
	}

	public double[] optDoubleArray(String key, double[] defaultValue) {
		double[] result = defaultValue;

		if (containsKey(key)) {
			result = getDoubleArray(key);
		}
		return result;
	}

	public Number[] getNumberArray(String key) {
		return TiConvert.toNumberArray((Object[])get(key));
	}
	
	public Number[] optIntArray(String key, Number[] defaultValue) {
		Number[] result = defaultValue;

		if (containsKey(key)) {
			result = getNumberArray(key);
		}
		return result;
	}
	
	public Object[] getArray(String key) {
	    Object result = get(key);
	    if (result instanceof Object[]) {
	        return (Object[]) result;
	    }
        return null;
    }

	public Object[] optArray(String key, Object[] defaultValue) {
	    Object[] result = defaultValue;

        if (containsKey(key)) {
            result = getArray(key);
        }
        return result;
    }

	public KrollDict getKrollDict(String key) {
	    return TiConvert.toKrollDict(get(key));
	}
	
	@SuppressWarnings("unchecked")
    public HashMap getHashMap(String key) {
        Object value = get(key);
        if (value instanceof KrollDict) {
            return (KrollDict) value;
        } else if (value instanceof HashMap) {
            return (HashMap<String, Object>) value;
        } else {
            return null;
        }
    }

	public boolean isNull(String key) {
		return (get(key) == null);
	}
	
	@Override
	public String toString() {
		return new JSONObject(this).toString();
	}
	
	public Set<String> minusKeys(final KrollDict that) {
		Set<String> keys = new HashSet<String>(keySet());
		Set<String> thatkeys = that.keySet();
		keys.removeAll(thatkeys);
		return keys;
    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
    public static KrollDict merge ( HashMap map1, HashMap map2 )
	{
	    if ( map1 == null || map2 == null )
	    {
	        if ( map1 != null )
	        {
	           return new KrollDict( map1 );
	        }
	        if ( map2 != null )
	        {
	           return new KrollDict( map2 );
	        }
	        return null;
	    }
	    KrollDict merged = new KrollDict(map1);

	    Set<String> allKeys = new HashSet<String>();
	    allKeys.addAll( map2.keySet() );

	    for ( String key : allKeys )
	    {
	        Object v1 = merged.get( key );
	        Object v2 = map2.get( key );
	        if ( v1 instanceof HashMap || v2 instanceof HashMap )
	        {
	            HashMap m1 = v1 instanceof HashMap ? (HashMap) v1 : null;
	            HashMap m2 = v2 instanceof HashMap ? (HashMap) v2 : null;
	            merged.put( key, merge( m1, m2 ) );
	        }
	        else {
	            merged.put(key, v2);
	        }

	    }
	    return merged;
	}
}
