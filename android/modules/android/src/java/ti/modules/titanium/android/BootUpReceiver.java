package ti.modules.titanium.android;

import org.appcelerator.kroll.util.KrollAssetHelper;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiProperties;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootUpReceiver extends BroadcastReceiver {
	private static final String TAG = "BootUpReceiver";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();

		if (!action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			return;
		}
		TiApplication app = TiApplication.getInstance();
        KrollAssetHelper.init(app.getApplicationContext());
        TiProperties appProperties = app.getAppProperties();
        
        Object prop = appProperties.getPreference("boot.services");
        if (prop instanceof JSONArray) {
            JSONArray services = (JSONArray) prop;
            int len = services.length();
            for (int i = 0; i < len; i++) {
                try {
                    Intent actIntent = new Intent();
                    actIntent.setClassName(context, services.get(i).toString());
                    context.startService(actIntent);
                } catch (JSONException e) {
                    
                }
            }
        }
        
	}
}
