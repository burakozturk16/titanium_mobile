package org.appcelerator.titanium;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;

import org.appcelerator.titanium.util.TiFileHelper;
import org.appcelerator.titanium.util.TiPlatformHelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

public class TiContentProvider extends ContentProvider {
    
    private static Uri CONTENT_URI = null;
    public static Uri getContentUri() {
        if (CONTENT_URI == null) {
            CONTENT_URI = Uri.parse("content://" + TiPlatformHelper.getInstance().getAppId() + ".provider");
        }
        return CONTENT_URI;
    }
    
    @Override
    public boolean onCreate() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        // TODO Auto-generated method stub
        return 0;
    }
    
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        uri = cleanUri(uri);
        try {
            return TiFileHelper.getInstance().openAssetFileDescriptor(uri.toString());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return super.openAssetFile(uri, mode);
        }
    }
    
    private Uri cleanUri(Uri uri) {
        String toReplace = getContentUri().toString() + "/";
        String path = uri.toString().replace(toReplace, "");
        return Uri.parse(path);
    }
    
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) {
        uri = cleanUri(uri);
        int imode = 0;
        if (mode.contains("w"))
            imode |= ParcelFileDescriptor.MODE_WRITE_ONLY;
        if (mode.contains("r"))
            imode |= ParcelFileDescriptor.MODE_READ_ONLY;
        if (mode.contains("+"))
            imode |= ParcelFileDescriptor.MODE_APPEND;
        try {
            return ParcelFileDescriptor.open(new File(uri.getEncodedPath()),
                    imode);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
