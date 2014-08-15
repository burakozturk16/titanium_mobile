package org.appcelerator.titanium.util;

import java.util.ArrayList;
import java.util.List;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiApplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class TiIntentHelper {
    public static Object[] queryIntentActivities(Intent intent)
    {
        PackageManager pm = TiApplication.getInstance().getApplicationContext().getPackageManager();
        List<KrollDict> results = new ArrayList<KrollDict>();
        List<ResolveInfo> resInfo = pm.queryIntentActivities(intent, 0);
        for (ResolveInfo resolveInfo : resInfo) {
            KrollDict info = new KrollDict();
            info.put("packageName", resolveInfo.activityInfo.packageName);
            info.put("icon", resolveInfo.icon);
            info.put("label", resolveInfo.loadLabel(pm));
            info.put("flags", resolveInfo.activityInfo.flags);
            info.put("className", resolveInfo.activityInfo.name);
            results.add(info);
        }
        return results.toArray();
    }
    
}
