package ti.modules.titanium.platform;

import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;

import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
//import android.os.PowerManager;
//import android.os.PowerManager.WakeLock;
//import android.app.KeyguardManager;
//import android.app.KeyguardManager.KeyguardLock;
//import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
//import android.os.Build;

@Kroll.module(parentModule=PlatformModule.class)
public class AndroidModule extends PlatformModule{

	@Kroll.constant public static final int API_LEVEL = Build.VERSION.SDK_INT;

	@Kroll.constant public static final int PHYSICAL_SIZE_CATEGORY_UNDEFINED = Configuration.SCREENLAYOUT_SIZE_UNDEFINED;
	@Kroll.constant public static final int PHYSICAL_SIZE_CATEGORY_SMALL = Configuration.SCREENLAYOUT_SIZE_SMALL;
	@Kroll.constant public static final int PHYSICAL_SIZE_CATEGORY_NORMAL = Configuration.SCREENLAYOUT_SIZE_NORMAL;
	@Kroll.constant public static final int PHYSICAL_SIZE_CATEGORY_LARGE = Configuration.SCREENLAYOUT_SIZE_LARGE;
	@Kroll.constant public static final int PHYSICAL_SIZE_CATEGORY_XLARGE = 4; // Configuration.SCREENLAYOUT_SIZE_XLARGE (API 9)
	
	@Kroll.getProperty @Kroll.method
	public int getPhysicalSizeCategory() {
		int size = TiApplication.getInstance().getApplicationContext().getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		switch(size) {
			case Configuration.SCREENLAYOUT_SIZE_SMALL :
			case Configuration.SCREENLAYOUT_SIZE_NORMAL :
			case Configuration.SCREENLAYOUT_SIZE_LARGE : 
			case 4 : // Configuration.SCREENLAYOUT_SIZE_XLARGE (API 9)
				return size;
			case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
			default :
				return PHYSICAL_SIZE_CATEGORY_UNDEFINED;
		}
	}

	public AndroidModule()
	{
		super();
	}

	@Override
	public String getApiName()
	{
		return "Ti.Platform.Android";
	}
	
//    @Override
//    public void onResume(Activity activity) {
//        super.onResume(activity);
//        if (batteryStateReceiver != null) {
//            Log.i(TAG, "Reregistering battery changed receiver", Log.DEBUG_MODE);
//            registerBatteryReceiver(batteryStateReceiver);
//        }
//    }
//
//    @Override
//    public void onPause(Activity activity) {
//        super.onPause(activity);
//        partialWakeLock.acquire();
//
//    }
//	
	@Kroll.method
    public boolean isPackageInstalled(final String packagename) {
	    PackageManager pm = TiApplication.getInstance().getApplicationContext().getPackageManager();
	    try {
	        pm.getPackageInfo(packagename, PackageManager.GET_ACTIVITIES);
	        return true;
	    } catch (NameNotFoundException e) {
	        return false;
	    }
    }
	
    @Kroll.method
    public void wakeUpScreen() {
        WakeLock screenLock = ((PowerManager) TiApplication
                .getAppSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
        screenLock.acquire();

        // later
        screenLock.release();
    }
    
    @Kroll.method
    public void unlockDevice() {
        wakeUpScreen();
        KeyguardManager keyguardManager = (KeyguardManager) TiApplication.getAppSystemService(Context.KEYGUARD_SERVICE); 
        KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("TAG");
        keyguardLock.disableKeyguard();
    }
}
