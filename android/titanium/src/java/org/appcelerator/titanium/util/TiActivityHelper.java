package org.appcelerator.titanium.util;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.TiMessenger.Command;
import org.appcelerator.kroll.common.TiMessenger.CommandNoReturn;
import org.appcelerator.titanium.ITiAppInfo;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.proxy.ActionBarProxy;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class TiActivityHelper {
    /**
     * Used to know if Apollo was sent into the background
     * 
     * @param context
     *            The {@link Context} to use
     */
    public static final boolean isApplicationSentToBackground(
            final Context context) {
        final ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        final List<RunningTaskInfo> tasks = activityManager.getRunningTasks(1);
        if (!tasks.isEmpty()) {
            final ComponentName topActivity = tasks.get(0).topActivity;
            if (!topActivity.getPackageName().equals(context.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private static class ActivityManagerHoneycomb {
        static int getLargeMemoryClass(ActivityManager activityManager) {
            return activityManager.getLargeMemoryClass();
        }
    }

    public static int calculateMemoryCacheSize(Context context) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        boolean largeHeap = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_LARGE_HEAP) != 0;
        int memoryClass = am.getMemoryClass();
        if (largeHeap && TiC.HONEYCOMB_OR_GREATER) {
            memoryClass = ActivityManagerHoneycomb.getLargeMemoryClass(am);
        }
        // Target ~15% of the available heap.
        return 1024 * 1024 * memoryClass / 7;
    }

    private static String capitalize(String line) {
        return Character.toUpperCase(line.charAt(0))
                + line.substring(1).toLowerCase();
    }

    private static String sMainActivityName = null;
    public static String getMainActivityName() {
        if (sMainActivityName == null) {
            Pattern pattern = Pattern.compile("[^A-Za-z0-9_]");
            ITiAppInfo appInfo = TiApplication.getInstance().getAppInfo();
            String str = appInfo.getName();
            String className = "";
            String[] splitStr = pattern.split(str);
            for (int i = 0; i < splitStr.length; i++) {
                className = className + capitalize(splitStr[i]);
            }
            Pattern pattern2 = Pattern.compile("^[0-9]");
            Matcher matcher = pattern2.matcher(className);
            if (matcher.matches()) {
                className = "_" + className;
            }
            sMainActivityName = appInfo.getId() + "." + className + "Activity";
        }
        return sMainActivityName;
    }

    public static ActionBar getActionBar(final Activity activity) {
        if (activity instanceof AppCompatActivity) {
            if (activity instanceof TiBaseActivity
                    && !((TiBaseActivity) activity).isReadyToQueryActionBar()) {
                return null;
            }
            try {
                return ((AppCompatActivity) activity).getDelegate().getSupportActionBar();
            } catch (NullPointerException e) {
                return null;
            }
        }
        return null;
    }

    public static double getActionBarHeight(final Activity activity) {
        // ActionBar actionBar = getActionBar(activity);
        // if (actionBar != null) {
        // Calculate ActionBar height
        int actionBarHeight = ActionBarProxy.getActionBarSize(activity);
        return new TiDimension(actionBarHeight, TiDimension.TYPE_HEIGHT)
                .getAsDefault();
        // }
        // return 0;
    }

    public static void setActionBarHidden(final Activity activity,
            final boolean hidden) {
        ActionBar actionBar = getActionBar(activity);
        if (actionBar != null) {
            try {
                if (hidden) {
                    actionBar.hide();
                } else {
                    actionBar.show();
                }
            } catch (NullPointerException e) {
                // no internal action bar
            }
        }
    }

    public static boolean setActionBarTitle(final Activity activity,
            final String title) {
        ActionBar actionBar = getActionBar(activity);
        if (actionBar != null) {
            try {
                actionBar.setTitle(title);
                return true;
            } catch (NullPointerException e) {
                // no internal action bar
            }
        }
        return false;
    }


    public static <T> T getValueInUIThread(final Activity activity,
            final KrollProxy proxy, final Command<T> command, String defaultProp) {
        return getValueInUIThread(activity, proxy, command,
                (T) proxy.getProperty(defaultProp));
    }

    public static <T> T getValueInUIThread(final Activity activity,
            final KrollProxy proxy, final Command<T> command, T defaultValue) {
        if (TiApplication.isUIThread()) {
            return command.execute();
        }

        FutureTask<T> futureResult = new FutureTask<T>(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return command.execute();
            }
        });
        // this block until the result is calculated!
        try {
            activity.runOnUiThread(futureResult);
            return futureResult.get();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static void runInUiThread(final Activity activity,
            final CommandNoReturn command) {
        if (TiApplication.isUIThread()) {
            command.execute();
        } else if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    command.execute();
                }
            });
        }
    }
}
