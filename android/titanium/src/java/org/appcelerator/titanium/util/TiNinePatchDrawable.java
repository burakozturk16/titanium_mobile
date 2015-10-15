package org.appcelerator.titanium.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;

public class TiNinePatchDrawable extends NinePatchDrawable {
    
    private final Bitmap mBitmap;
    private final byte[] mChunk;
    private final Rect mPadding;
    private final String mSrcName;
    
    public TiNinePatchDrawable(Resources res, Bitmap bitmap, byte[] chunk,
            Rect padding, String srcName) {
        super(res, bitmap, chunk, padding, srcName);
        mBitmap = bitmap;
        mChunk = chunk;
        mPadding = padding;
        mSrcName = srcName;
    }
    
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public byte[] getChunk() {
        return mChunk;
    }
    public Rect getPadding() {
        return mPadding;
    }
}
