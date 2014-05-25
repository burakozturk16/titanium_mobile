package org.appcelerator.titanium.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiFileProxy;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TiDatabaseHelper {
    private static String TAG = "TiDatabaseHelper";

    private static SQLiteDatabase openDatabase(final String name) {
        return TiApplication.getInstance().openOrCreateDatabase(name, Context.MODE_PRIVATE, null);
    }
    
    public static SQLiteDatabase getDatabase(final KrollProxy proxy, final String url, boolean writable) throws IOException
    {
        
        String path;
        if (url.startsWith("."))
            path = proxy.resolveUrl(null, url);
        else
            path = proxy.resolveUrl("appdata://", url);
        TiBaseFile file = TiFileFactory.createTitaniumFile(new String[] { path }, false);
        return getDatabase(file, writable);
    }
    
    public static SQLiteDatabase getDatabase(final TiBaseFile srcFile, boolean writable) throws IOException {
        
        final String name  = srcFile.getNativeFile().getName();
        try {
            String path = srcFile.nativePath();
            if (!path.startsWith(TiC.URL_ANDROID_ASSET_RESOURCES)) {
                return SQLiteDatabase.openDatabase(path.replace("file://", ""), null, (writable?SQLiteDatabase.OPEN_READWRITE:SQLiteDatabase.OPEN_READONLY) | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            }

            Context ctx = TiApplication.getInstance();
            for (String dbname : ctx.databaseList())
            {
                if (dbname.equals(name))
                {
                    return openDatabase(name);
                }
            }

            File dbPath = ctx.getDatabasePath(name);
    
            Log.d(TAG , "db path is = " + dbPath, Log.DEBUG_MODE);
    
            if (srcFile.isFile()) {
                InputStream is = null;
                OutputStream os = null;
    
                byte[] buf = new byte[8096];
                int count = 0;
                try
                {
                    is = new BufferedInputStream(srcFile.getInputStream());
                    os = new BufferedOutputStream(new FileOutputStream(dbPath));
    
                    while((count = is.read(buf)) != -1) {
                        os.write(buf, 0, count);
                    }
                }
                finally
                {
                    try { is.close(); } catch (Exception ig) { }
                    try { os.close(); } catch (Exception ig) { }
                }
            }
            
            return openDatabase(name);
    
        } catch (SQLException e) {
            String msg = "Error installing database: " + name + " msg=" + e.getMessage();
            Log.e(TAG, msg, e);
            throw e;
        }
        catch (IOException e) {
            String msg = "Error installing database: " + name + " msg=" + e.getMessage();
            Log.e(TAG, msg, e);
            throw e;
        }
    }
}
