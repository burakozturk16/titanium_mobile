/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.util.KrollStreamHelper;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TitaniumBlob;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiMimeTypeHelper;
import org.appcelerator.titanium.util.TiNinePatchDrawable;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.util.Pair;
import android.util.Base64;

/**
 * A Titanium Blob object. A Blob can represent any opaque data or input stream.
 */
@Kroll.proxy
public class TiBlob extends KrollProxy {
    private static final String TAG = "TiBlob";

    /**
     * Represents a Blob that contains image data.
     * 
     * @module.api
     */
    public static final int TYPE_IMAGE = 0;

    /**
     * Represents a Blob that contains file data.
     * 
     * @module.api
     */
    public static final int TYPE_FILE = 1;

    /**
     * Represents a Blob that contains data.
     * 
     * @module.api
     */
    public static final int TYPE_DATA = 2;

    /**
     * Represents a Blob that contains String data.
     * 
     * @module.api
     */
    public static final int TYPE_STRING = 3;

    /**
     * Represents a Blob that contains stream data that needs to be converted to
     * base64.
     * 
     * @module.api
     */
    public static final int TYPE_STREAM_BASE64 = 4;

    public static final int TYPE_DRAWABLE = 5;

    private int type;
    private Object data;
    private String mimetype;
    private Bitmap image;
    private Drawable drawable;
    private int width, height;
    private KrollDict extraInfo;

    private TiBlob(int type, Object data, String mimetype) {
        super();
        this.type = type;
        this.data = data;
        this.mimetype = mimetype;
        this.image = null;
        this.drawable = null;
        this.width = 0;
        this.height = 0;
    }

    @Override
    public void release() {
        super.release();
        if (this.image != null) {
            this.image.recycle();
            this.image = null;
        }
    }

    public static String getMimeTypeOfFile(Object object) {
        Bitmap bitmap = null;
        if (object instanceof Bitmap) {
            bitmap = (Bitmap) object;
        } else if (object instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) object).getBitmap();
        }
        if (bitmap != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(CompressFormat.PNG, 0 /* ignored for PNG */, bos);
            byte[] bitmapdata = bos.toByteArray();
            ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(bs, null, opt);
            return opt.outMimeType;
        }
        return null;
    }

    public static TiBlob blobFromObject(Object object) {
        return blobFromObject(object, null);
    }

    public static TiBlob blobFromObject(Object object, String mimetype) {
        if (object instanceof byte[]) {
            byte[] data = (byte[]) object;

            if (mimetype == null || mimetype.length() == 0) {
                mimetype = "application/octet-stream";
            }
            TiBlob blob = new TiBlob(TYPE_DATA, data, mimetype);
            blob.loadBitmapInfo();
            return blob;
        } else if (object instanceof Drawable) {
            Drawable drawable = (Drawable) object;
            if (mimetype == null || mimetype.length() == 0) {
                mimetype = getMimeTypeOfFile(drawable);
            }
            TiBlob blob = new TiBlob(TYPE_DRAWABLE, null, mimetype);
            blob.drawable = drawable;
            blob.width = drawable.getIntrinsicWidth();
            blob.height = drawable.getIntrinsicHeight();
            return blob;
        } else if (object instanceof Bitmap) {
            Bitmap image = (Bitmap) object;
            if (mimetype == null || mimetype.length() == 0) {
                mimetype = getMimeTypeOfFile(image);
            }
            TiBlob blob = new TiBlob(TYPE_IMAGE, null, mimetype);
            blob.image = image;
            blob.width = image.getWidth();
            blob.height = image.getHeight();
            return blob;
        } else if (object instanceof TiBaseFile) {
            TiBaseFile file = (TiBaseFile) object;
            if (mimetype == null || mimetype.length() == 0) {
                mimetype = TiMimeTypeHelper.getMimeType(file.nativePath());
            }
            TiBlob blob = new TiBlob(TYPE_FILE, file, mimetype);
            blob.loadBitmapInfo();
            return blob;
        } else if (object instanceof InputStream) {
            InputStream stream = (InputStream) object;
            return new TiBlob(TYPE_STREAM_BASE64, stream, mimetype);
        } else if (object instanceof String) {
            String data = (String) object;
            if (mimetype == null || mimetype.length() == 0) {
                mimetype = "text/plain";
            }
            return new TiBlob(TYPE_STRING, data, mimetype);

        }
        return null;
    }

    /**
     * Determines the MIME-type by reading first few characters from the given
     * input stream.
     * 
     * @return the guessed MIME-type or null if the type could not be
     *         determined.
     */
    public String guessContentTypeFromStream() {
        String mt = null;
        InputStream is = getInputStream();
        if (is != null) {
            try {
                mt = URLConnection.guessContentTypeFromStream(is);
                if (mt == null) {
                    mt = guessAdditionalContentTypeFromStream(is);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e, Log.DEBUG_MODE);
            }
        }
        return mt;
    }

    /**
     * Check for additional content type reading first few characters from the
     * given input stream.
     * 
     * @return the guessed MIME-type or null if the type could not be
     *         determined.
     */
    private String guessAdditionalContentTypeFromStream(InputStream is) {
        String mt = null;

        if (is != null) {
            try {

                // Look ahead up to 64 bytes for the longest encoded header
//                is.mark(64);
                byte[] bytes = new byte[64];
                int length = is.read(bytes, 0, 64);
//                is.reset();
                if (length == -1) {
                    return null;
                }
                if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                        && bytes[3] == '8') {
                    mt = "image/gif";
                } else if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50
                        && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47
                        && bytes[4] == (byte) 0x0D && bytes[5] == (byte) 0x0A
                        && bytes[6] == (byte) 0x1A && bytes[7] == (byte) 0x0A) {
                    mt = "image/png";
                } else if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                        && bytes[2] == (byte) 0xFF) {
                    if ((bytes[3] == (byte) 0xE0)
                            || (bytes[3] == (byte) 0xE1 && bytes[6] == 'E'
                                    && bytes[7] == 'x' && bytes[8] == 'i'
                                    && bytes[9] == 'f' && bytes[10] == 0)) {
                        mt = "image/jpeg";
                    } else if (bytes[3] == (byte) 0xEE) {
                        mt = "image/jpg";
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e, Log.DEBUG_MODE);
            }
        }
        return mt;
    }

    /**
     * Update width and height if the file / data can be decoded into a bitmap
     * successfully.
     */
    public void loadBitmapInfo() {
        String mt = guessContentTypeFromStream();
        // Update mimetype based on the guessed MIME-type.
        if (mt != null && mt != mimetype) {
            mimetype = mt;
        }

        // If the MIME-type is "image/*" or undetermined, try to decode the file
        // / data into a bitmap.
        if (mimetype == null || mimetype.startsWith("image/")) {
            // Query the dimensions of a bitmap without allocating the memory
            // for its pixels
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;

            switch (type) {
            case TYPE_FILE:
                BitmapFactory.decodeStream(getInputStream(), null, opts);
                break;
            case TYPE_DATA:
                byte[] byteArray = (byte[]) data;
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length,
                        opts);
                break;
            }

            // Update width and height after the file / data is decoded
            // successfully
            if (opts.outWidth != -1 && opts.outHeight != -1) {
                width = opts.outWidth;
                height = opts.outHeight;
            }
        }
    }
    
    /**
     * Returns the content of blob in form of binary data. Exception will be
     * thrown if blob's type is unknown.
     * 
     * @return binary data.
     * @module.api
     */
    public byte[] getBytes() {
        byte[] bytes = null;

        switch (type) {
        case TYPE_STRING:
            try {
                bytes = ((String) data).getBytes("utf-8");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e.getMessage(), e);
            }
            break;
        case TYPE_DATA:
        case TYPE_IMAGE:
        case TYPE_DRAWABLE:
            // TODO deal with mimetypes.
            bytes = (byte[]) getData();
            break;
        case TYPE_FILE:
            InputStream stream = getInputStream();
            if (stream != null) {
                try {
                    bytes = KrollStreamHelper.toByteArray(stream, getLength());
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.w(TAG, e.getMessage(), e);
                    }
                }
            }
            break;
        case TYPE_STREAM_BASE64:
            InputStream inStream = (InputStream) data;
            if (inStream != null) {
                try {
                    bytes = KrollStreamHelper
                            .toByteArray(inStream, getLength());
                } finally {
                    try {
                        inStream.close();
                    } catch (IOException e) {
                        Log.w(TAG, e.getMessage(), e);
                    }
                }
            }
        default:
            throw new IllegalArgumentException("Unknown Blob type id " + type);
        }

        return bytes;
    }

    @SuppressLint("NewApi")
    protected int sizeOf(Bitmap data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
            return data.getRowBytes() * data.getHeight();
        } else {
            return data.getByteCount();
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public int getLength() {
        switch (type) {
        case TYPE_FILE:
            long fileSize;
            if (data instanceof TitaniumBlob) {
                fileSize = ((TitaniumBlob) data).getFile().length();
            } else {
                fileSize = ((TiBaseFile) data).size();
            }
            return (int) fileSize;
        case TYPE_DRAWABLE:
            return 0;
        case TYPE_DATA:
        case TYPE_IMAGE:
            if (image != null) {
                return sizeOf(image);
            }
            return ((byte[]) getData()).length;
        case TYPE_STREAM_BASE64:
            throw new IllegalStateException(
                    "Not yet implemented. TYPE_STREAM_BASE64");
        default:
            // this is probably overly expensive.. is there a better way?
            return getBytes().length;
        }
    }

    /**
     * @return An InputStream for reading the data of this blob.
     * @module.api
     */
    public InputStream getInputStream() {
        switch (type) {
        case TYPE_FILE:
            try {
                return ((TiBaseFile) data).getInputStream();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                return null;
            }
        case TYPE_STREAM_BASE64:
            return (InputStream) data;
        default:
            return new ByteArrayInputStream(getBytes());
        }
    }

    @Kroll.method
    public void append(TiBlob blob) {
        switch (type) {
        case TYPE_STRING:
            try {
                // String dataString = (String)data;
                data = (String) data + new String(blob.getBytes(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, e.getMessage(), e);
            }
            break;
        case TYPE_IMAGE:
        case TYPE_DATA:
            byte[] dataBytes = (byte[]) getData();
            byte[] appendBytes = blob.getBytes();
            byte[] newData = new byte[dataBytes.length + appendBytes.length];
            System.arraycopy(dataBytes, 0, newData, 0, dataBytes.length);
            System.arraycopy(appendBytes, 0, newData, dataBytes.length,
                    appendBytes.length);

            data = newData;
            break;
        case TYPE_FILE:
            throw new IllegalStateException("Not yet implemented. TYPE_FILE");
        case TYPE_STREAM_BASE64:
            throw new IllegalStateException(
                    "Not yet implemented. TYPE_STREAM_BASE64");
            // break;
        default:
            throw new IllegalArgumentException("Unknown Blob type id " + type);
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public String getText() {
        String result = null;

        // Only support String and Data. Same as iPhone
        switch (type) {
        case TYPE_STRING:
            result = (String) data;
        case TYPE_DATA:
        case TYPE_FILE:
            // Don't try to return a string if we can see the
            // mimetype is binary, unless it's application/octet-stream, which
            // means
            // we don't really know what it is, so assume the user-developer
            // knows
            // what she's doing.
            if (mimetype != null && TiMimeTypeHelper.isBinaryMimeType(mimetype)
                    && mimetype != "application/octet-stream") {
                return null;
            }
            try {
                result = new String(getBytes(), "utf-8");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "Unable to convert to string.");
            }
            break;
        case TYPE_STREAM_BASE64:
            throw new IllegalStateException(
                    "Not yet implemented. TYPE_STREAM_BASE64");
        }

        return result;
    }

    @Kroll.getProperty
    @Kroll.method
    public String getHexString() {
        byte[] bytes = getBytes();
        if (bytes != null) {
            return TiUtils.bytesToHex(bytes);
        }
        return null;
    }
    
    @Kroll.getProperty
    @Kroll.method
    public Number[] getByteArray() {
        byte[] bytes = getBytes();
        
        if (bytes != null) {
            Number[] outArray = new Number[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                outArray[i] = bytes[i];
            }
            return outArray;
        }
        return null;
    }

    @Kroll.getProperty
    @Kroll.method
    public String getMimeType() {
        return mimetype;
    }

    /**
     * @return the blob's data.
     * @module.api
     */
    public Object getData() {
        if (data == null && image != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            data = new byte[0];
            if (image.hasAlpha()) {
                if (image.compress(CompressFormat.PNG, 100, bos)) {
                    data = bos.toByteArray();
                }
            } else {
                if (image.compress(CompressFormat.JPEG, 100, bos)) {
                    data = bos.toByteArray();
                }
            }
        }
        return data;
    }

    /**
     * @return The type of this Blob.
     * @see TiBlob#TYPE_DATA
     * @see TiBlob#TYPE_FILE
     * @see TiBlob#TYPE_IMAGE
     * @see TiBlob#TYPE_STRING
     * @see TiBlob#TYPE_STREAM
     * @module.api
     */
    @Kroll.getProperty
    @Kroll.method
    public int getType() {
        return type;
    }
	@Kroll.method
	public String toBase64()
	{
		return Base64.encodeToString(getBytes(), Base64.NO_WRAP);
	}

    @Kroll.getProperty
    @Kroll.method
    public int getWidth() {
        return width;
    }

    @Kroll.getProperty
    @Kroll.method
    public int getHeight() {
        return height;
    }

    @Kroll.method
    public String toString() {
        // blob should return the text value on toString
        // if it's not null
        String result = null;
  
        switch (type) {
        case TYPE_STRING:
        case TYPE_STREAM_BASE64:
        case TYPE_DATA:
            result = getText();
        case TYPE_FILE:
            result = getNativePath();
        }
        if (result != null) {
            return result;
        }
        return "[object TiBlob]";
    }

    @Kroll.getProperty
    @Kroll.method
    public String getNativePath() {
        if (data == null) {
            return null;
        }
        if (this.type != TYPE_FILE) {
            Log.w(TAG, "getNativePath not supported for non-file blob types.");
            return null;
        } else if (!(data instanceof TiBaseFile)) {
            Log.w(TAG,
                    "getNativePath unable to return value: underlying data is not file, rather "
                            + data.getClass().getName());
            return null;
        } else {
            String path = ((TiBaseFile) data).nativePath();
            if (path != null && path.startsWith("content://")) {
                File f = ((TiBaseFile) data).getNativeFile();
                if (f != null) {
                    path = f.getAbsolutePath();
                    if (path != null && path.startsWith("/")) {
                        path = "file://" + path;
                    }
                }
            }
            return path;
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public TiFileProxy getFile() {
        if (data == null) {
            return null;
        }
        if (this.type != TYPE_FILE) {
            Log.w(TAG, "getFile not supported for non-file blob types.");
            return null;
        } else if (!(data instanceof TiBaseFile)) {
            Log.w(TAG,
                    "getFile unable to return value: underlying data is not file, rather "
                            + data.getClass().getName());
            return null;
        } else {
            return new TiFileProxy((TiBaseFile) data);
        }
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public Bitmap getImage() {
        // If the image is not available but the width and height of the image
        // are successfully fetched, the image can
        // be created by decoding the data.
        return getImage(null);
    }

    private Bitmap getImage(BitmapFactory.Options opts) {
        if (image == null && (width > 0 && height > 0)) {
            if (opts == null) {
                opts = new BitmapFactory.Options();
                opts.inPreferredConfig = Bitmap.Config.RGB_565;
            }
            try {
                switch (type) {
                case TYPE_DRAWABLE:
                    if (drawable instanceof BitmapDrawable) {
                        image = ((BitmapDrawable) drawable).getBitmap();
                    } else if (drawable instanceof TiNinePatchDrawable) {
                        image = ((TiNinePatchDrawable) drawable).getBitmap();
                    }
                    break;
                case TYPE_FILE:
                    image = BitmapFactory.decodeStream(
                            getInputStream(), null, opts);
                    int rotation = TiImageHelper
                            .getOrientation(getNativePath());
                    if (rotation % 360 != 0) {
                        image =  TiImageHelper.rotateImage(image, rotation);
                    }
                    break;
                case TYPE_DATA:
                    byte[] byteArray = (byte[]) data;
                    image = BitmapFactory.decodeByteArray(byteArray, 0,
                            byteArray.length, opts);
                    break;
                }
            } catch (OutOfMemoryError e) {
                Log.e(TAG,
                        "Unable to get the image. Not enough memory: "
                                + e.getMessage(), e);
                image = null;
            } finally {
            }
        }
        return image;
    }

    @Kroll.method
    public TiBlob imageAsCropped(Object params,
            @Kroll.argument(optional = true) HashMap options) {
        Bitmap img = getImage();
        if (img == null) {
            return null;
        }
        if (!(params instanceof HashMap)) {
            Log.e(TAG, "Argument for imageAsCropped must be a dictionary");
            return null;
        }
        float scale = 1.0f;
        if (options != null) {
            if (options.containsKey("scale")) {
                scale = TiConvert.toFloat(options, "scale", 1.0f);
            }
        }
        Context context = TiApplication.getInstance().getApplicationContext();

        KrollDict rect = new KrollDict((HashMap) params);
        int widthCropped = (int) (TiUIHelper.getRawDIPSize(
                rect.optInt(TiC.PROPERTY_WIDTH, width), context) * scale);
        int heightCropped = (int) (TiUIHelper.getRawDIPSize(
                rect.optInt(TiC.PROPERTY_HEIGHT, height), context) * scale);
        int x = (int) (TiUIHelper.getRawDIPSize(
                rect.optInt(TiC.PROPERTY_X, (width - widthCropped) / 2),
                context) * scale);
        int y = (int) (TiUIHelper.getRawDIPSize(
                rect.optInt(TiC.PROPERTY_Y, (height - heightCropped) / 2),
                context) * scale);
        try {
            Bitmap imageCropped = Bitmap.createBitmap(img, x, y, widthCropped,
                    heightCropped);
            return blobFromObject(imageCropped);
        } catch (OutOfMemoryError e) {
            Log.e(TAG,
                    "Unable to crop the image. Not enough memory: "
                            + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG,
                    "Unable to crop the image. Illegal Argument: "
                            + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            Log.e(TAG,
                    "Unable to crop the image. Unknown exception: "
                            + t.getMessage(), t);
            return null;
        }
    }

    @Kroll.method
    public TiBlob imageAsResized(Number width, Number height) {
        boolean valid = (image != null)
                || (image == null && (this.width > 0 && this.height > 0));
        if (!valid) {
            return null;
        }

        int dstWidth = width.intValue();
        int dstHeight = height.intValue();
        int imgWidth = this.width;
        int imgHeight = this.height;

        BitmapFactory.Options opts = null;
        boolean scaleDown = ((image == null) && (dstWidth < imgWidth) && (dstHeight < imgHeight));
        if (scaleDown) {
            int scaleWidth = imgWidth / dstWidth;
            int scaleHeight = imgHeight / dstHeight;

            int targetScale = (scaleWidth < scaleHeight) ? scaleWidth
                    : scaleHeight;
            int sampleSize = 1;
            while (targetScale >= 2) {
                sampleSize *= 2;
                targetScale /= 2;
            }

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
        }

        Bitmap img = getImage(opts);
        if (img == null) {
            return null;
        }

        try {
            Bitmap imageResized = null;
            imgWidth = img.getWidth();
            imgHeight = img.getHeight();
            imageResized = Bitmap.createScaledBitmap(img, dstWidth, dstHeight,
                    true);
            if (img != image && img != imageResized) {
                img.recycle();
                img = null;
            }
            return blobFromObject(imageResized);
        } catch (OutOfMemoryError e) {
            Log.e(TAG,
                    "Unable to resize the image. Not enough memory: "
                            + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG,
                    "Unable to resize the image. Illegal Argument: "
                            + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            Log.e(TAG,
                    "Unable to resize the image. Unknown exception: "
                            + t.getMessage(), t);
            return null;
        }
    }

    @Kroll.method
    public TiBlob imageAsThumbnail(Number size,
            @Kroll.argument(optional = true) Number borderSize,
            @Kroll.argument(optional = true) Number cornerRadius) {
        Bitmap img = getImage();
        if (img == null) {
            return null;
        }

        int thumbnailSize = size.intValue();

        float border = 1f;
        if (borderSize != null) {
            border = borderSize.floatValue();
        }
        float radius = 0f;
        if (cornerRadius != null) {
            radius = cornerRadius.floatValue();
        }

        try {
            Bitmap imageFinal = null;
            Bitmap imageThumbnail = ThumbnailUtils.extractThumbnail(img,
                    thumbnailSize, thumbnailSize);
            if (img != image && img != imageThumbnail) {
                img.recycle();
                img = null;
            }

            if (border == 0 && radius == 0) {
                imageFinal = imageThumbnail;
            } else {
                imageFinal = TiImageHelper.imageWithRoundedCorner(
                        imageThumbnail, radius, border);
                if (imageThumbnail != image && imageThumbnail != imageFinal) {
                    imageThumbnail.recycle();
                    imageThumbnail = null;
                }
            }

            return blobFromObject(imageFinal);

        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Unable to get the thumbnail image. Not enough memory: "
                    + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to get the thumbnail image. Illegal Argument: "
                    + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            Log.e(TAG, "Unable to get the thumbnail image. Unknown exception: "
                    + t.getMessage(), t);
            return null;
        }
    }

    @Kroll.method
    public TiBlob imageWithAlpha() {
        Bitmap img = getImage();
        if (img == null) {
            return null;
        }

        try {
            Bitmap imageWithAlpha = TiImageHelper.imageWithAlpha(img);
            if (img != image && img != imageWithAlpha) {
                img.recycle();
                img = null;
            }

            return blobFromObject(imageWithAlpha);
        } catch (OutOfMemoryError e) {
            Log.e(TAG,
                    "Unable to get the image with alpha. Not enough memory: "
                            + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Unable to get the image with alpha. Illegal Argument: "
                    + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            Log.e(TAG,
                    "Unable to get the image with alpha. Unknown exception: "
                            + t.getMessage(), t);
            return null;
        }
    }

    @Kroll.method
    public TiBlob imageWithRoundedCorner(Number cornerRadius,
            @Kroll.argument(optional = true) Number borderSize) {
        Bitmap img = getImage();
        if (img == null) {
            return null;
        }

        float radius = cornerRadius.floatValue();
        float border = 1f;
        if (borderSize != null) {
            border = borderSize.floatValue();
        }

        try {
            Bitmap imageRoundedCorner = TiImageHelper.imageWithRoundedCorner(
                    img, radius, border);
            if (img != image && img != imageRoundedCorner) {
                img.recycle();
                img = null;
            }
            return blobFromObject(imageRoundedCorner);
        } catch (OutOfMemoryError e) {
            Log.e(TAG,
                    "Unable to get the image with rounded corner. Not enough memory: "
                            + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG,
                    "Unable to get the image with rounded corner. Illegal Argument: "
                            + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            Log.e(TAG,
                    "Unable to get the image with rounded corner. Unknown exception: "
                            + t.getMessage(), t);
            return null;
        }
    }

    @Kroll.method
    public TiBlob imageWithTransparentBorder(Number size) {
        Bitmap img = getImage();
        if (img == null) {
            return null;
        }

        int borderSize = size.intValue();
        try {
            Bitmap imageWithBorder = TiImageHelper.imageWithTransparentBorder(
                    img, borderSize);
            if (img != image && img != imageWithBorder) {
                img.recycle();
                img = null;
            }
            return blobFromObject(imageWithBorder);
        } catch (OutOfMemoryError e) {
            Log.e(TAG,
                    "Unable to get the image with transparent border. Not enough memory: "
                            + e.getMessage(), e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG,
                    "Unable to get the image with transparent border. Illegal Argument: "
                            + e.getMessage(), e);
            return null;
        } catch (Throwable t) {
            Log.e(TAG,
                    "Unable to get the image with transparent border. Unknown exception: "
                            + t.getMessage(), t);
            return null;
        }
    }

    @Kroll.method
    public TiBlob imageAsFiltered(HashMap options) {
        Bitmap bitmap = getImage();
        if (bitmap != null) {
            return null;
        }
        Pair<Bitmap, KrollDict> result = TiImageHelper.imageFiltered(bitmap,
                options, true);
        TiBlob blob = TiBlob.blobFromObject(result.first);
        blob.addInfo(result.second);
        return blob;
    }

    @Override
    public String getApiName() {
        return "Ti.Blob";
    }

    public void setInfo(final KrollDict info) {
        this.extraInfo = info;
    }

    public void addInfo(final KrollDict info) {
        if (extraInfo == null) {
            setInfo(info);
        } else {
            this.extraInfo.putAll(info);
        }
    }

    @Kroll.getProperty
    @Kroll.method
    public KrollDict getInfo() {
        return extraInfo;
    }
}
