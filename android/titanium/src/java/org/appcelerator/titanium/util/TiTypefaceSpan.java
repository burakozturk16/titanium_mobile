package org.appcelerator.titanium.util;

import org.appcelerator.titanium.TiApplication;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

public class TiTypefaceSpan extends TypefaceSpan {
    private final Typeface newType;
    private final String fontFamily;

    public TiTypefaceSpan(String family, Typeface type) {
        super(family);
    	this.fontFamily = family;
        newType = type;
    }
    
    public TiTypefaceSpan(String family) {
        super(family);
        this.fontFamily = family;
        newType = null;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
    	if (fontFamily != null && fontFamily.length() > 0) {
    		applyCustomTypeFace(ds, newType);
    	}
    }

    @Override
    public void updateMeasureState(TextPaint paint) {
    	if (fontFamily != null && fontFamily.length() > 0) {
    		applyCustomTypeFace(paint, newType);
    	}
    }

    private void applyCustomTypeFace(Paint paint, Typeface tf) {
		tf = TiUIHelper.toTypeface(TiApplication.getInstance().getApplicationContext(), fontFamily);
        paint.setTypeface(tf);
    }
}