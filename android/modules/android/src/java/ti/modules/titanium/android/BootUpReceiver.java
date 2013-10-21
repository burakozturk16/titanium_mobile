package ti.modules.titanium.android;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.ITiAppInfo;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiC;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class BootUpReceiver extends BroadcastReceiver {
	private static final String TAG = "BootUpReceiver";

	private String capitalize(String line)
	{
	  return Character.toUpperCase(line.charAt(0)) + line.substring(1).toLowerCase();
	}
	
	private String getMainActivityName(){
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
		return appInfo.getId() + "." + className + "Activity";
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		String action = intent.getAction();

		if (!action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			return;
		}
		String myClass = getMainActivityName();
		Log.w(TAG, "onReceive '" + myClass + "'");
		Class<?> clazz;
		try {
			clazz = Class.forName(myClass);
			Intent i = new Intent(context, clazz);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.setAction(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_LAUNCHER);
			i.putExtra(TiC.INTENT_PROPERTY_ON_BOOT, true);
			context.startActivity(i);
		} catch (ClassNotFoundException e) {
			Log.w(TAG, "Could not start activity '" + myClass + "'");
			e.printStackTrace();
		}
	}
}
