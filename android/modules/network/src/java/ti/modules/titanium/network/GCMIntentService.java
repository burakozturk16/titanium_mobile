package ti.modules.titanium.network;

import java.util.HashMap;
import java.util.Set;

import org.appcelerator.kroll.common.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

public class GCMIntentService extends IntentService {

    private static final String TAG = "TiGCMIntentService";

    public static final int NOTIFICATION_ID = 1;


    public GCMIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        Log.d(TAG , "onHandleIntent " + messageType, Log.DEBUG_MODE);

        if (!extras.isEmpty()) { // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that
             * GCM will be extended in the future with new message types, just
             * ignore any message types you're not interested in, or that you
             * don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR
                    .equals(messageType)) {
//                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED
                    .equals(messageType)) {
//                sendNotification("Deleted messages on server: "
//                        + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE
                    .equals(messageType)) {
                HashMap data = new HashMap();
                Set<String> keys = intent.getExtras().keySet();
                keys.remove("android.support.content.wakelockid");
                keys.remove("collapse_key");
                keys.remove("from");
                for (String key : keys) {
                    String value = intent.getExtras().getString(key);
                    if (value != null) {
                        Log.d(TAG, "Message key: " + key + " value: "
                                + intent.getExtras().getString(key));
                        String eventKey = key.startsWith("data.") ? key.substring(5) : key;
                        data.put(eventKey, value);
                    }
                }
                NetworkModule.gcmOnMessage(data);
                // Post notification of received message.
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }

}
