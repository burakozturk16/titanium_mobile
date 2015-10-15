/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.lang.ref.WeakReference;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBaseActivity;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiLaunchActivity;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.view.TiUIView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class TiUIProgressIndicator extends TiUIView
	implements Handler.Callback, DialogInterface.OnCancelListener
{
	private static final String TAG = "TiUIProgressDialog";

	private static final int MSG_SHOW = 100;
    private static final int MSG_PROGRESS = 101;
	private static final int MSG_HIDE = 102;
    private static final int MSG_PROGRESS_MINMAX = 103;

	public static final int INDETERMINANT = 0;
	public static final int DETERMINANT = 1;

	public static final int STATUS_BAR = 0;
	public static final int DIALOG = 1;

	protected Handler handler;

	protected boolean visible;
	protected ProgressDialog progressDialog;
	protected String statusBarTitle;
	protected int incrementFactor;
	protected int location = DIALOG;
	protected int min = 0;
    protected int max = 100;
    protected int value = 0;
    protected int type = INDETERMINANT;
    protected boolean cancelable = false;
	private String message = "";

	public TiUIProgressIndicator(TiViewProxy proxy) {
		super(proxy);
		Log.d(TAG, "Creating an progress indicator", Log.DEBUG_MODE);
		handler = new Handler(Looper.getMainLooper(), this);
	}

	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_SHOW : {
				handleShow();
				return true;
			}
			case MSG_PROGRESS : {
	            int thePos = (value - min) * incrementFactor;
                if (progressDialog != null) {
                    progressDialog.setProgress(thePos);
                } else {
                    TiBaseActivity parent = (TiBaseActivity) this.proxy.getActivity();
                    parent.setSupportProgress(thePos);
                }
                return true;
            }
			case MSG_PROGRESS_MINMAX : {
                if (progressDialog != null) {
                    progressDialog.setMax(max-min);
                }
                return true;
            }
            case MSG_HIDE : {
				handleHide();
				return true;
			}
		}

		return false;
	}

	public void show(KrollDict options)
	{
		if (visible) {
			return;
		}

		// Don't try to show indicator if the root activity is not available
		if (!TiApplication.getInstance().isRootActivityAvailable()) {
			Activity currentActivity = TiApplication.getAppCurrentActivity();
			if (currentActivity instanceof TiLaunchActivity) {
				if (!((TiLaunchActivity) currentActivity).isJSActivity()) {
					return;
				}
			}
		}

		handleShow();
	}
	

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {

        switch (key) {
        case TiC.PROPERTY_MESSAGE:
            message = TiConvert.toString(newValue, message);
            if (visible) {
                if (progressDialog != null) {
                    progressDialog.setMessage(message);

                } else {
                    Activity parent = (Activity) this.proxy.getActivity();
                    parent.setTitle(message);
                }
            }
            break;
        case TiC.PROPERTY_VALUE:
            value = TiConvert.toInt(newValue, value);
            updateProgress();
            break;
        case TiC.PROPERTY_MIN:
            min = TiConvert.toInt(newValue, min);
            updateProgressMinMax();
            updateProgress();
            break;
        case TiC.PROPERTY_MAX:
            max = TiConvert.toInt(newValue, max);
            updateProgressMinMax();
            updateProgress();
            break;
        case TiC.PROPERTY_TYPE:
            type = TiConvert.toInt(newValue, type);
            updateProgress();
            break;
        case TiC.PROPERTY_LOCATION:
            location = TiConvert.toInt(newValue, location);
            break;
        
        case TiC.PROPERTY_CANCELABLE:
            cancelable = TiConvert.toBoolean(newValue, cancelable);
            if (progressDialog != null) {
                progressDialog.setCancelable(TiConvert.toBoolean(newValue));
            }
            break;
 
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }
    private void updateProgress() {
        if (location == STATUS_BAR) {
            incrementFactor = 10000 / (max - min);
        } else if (location == DIALOG){
            incrementFactor = 1;
        }
        if (visible) {
            handler.obtainMessage(MSG_PROGRESS).sendToTarget();
        }
        
    }
    
    private void updateProgressMinMax() {
        if (visible) {
            handler.obtainMessage(MSG_PROGRESS_MINMAX).sendToTarget();
        }
        
    }

	protected void handleShow()
	{

		if (location == STATUS_BAR) {
		    TiBaseActivity parent = (TiBaseActivity) proxy.getActivity();
			if (type == INDETERMINANT) {
				parent.setSupportProgressBarIndeterminate(true);
				parent.setSupportProgressBarIndeterminateVisibility(true);
				statusBarTitle = parent.getTitle().toString();
				parent.setSupportProgress((value-min ) * incrementFactor);
				parent.setTitle(message);
			} else if (type == DETERMINANT) {
				parent.setSupportProgressBarIndeterminate(false);
				parent.setSupportProgressBarIndeterminateVisibility(false);
				parent.setSupportProgressBarVisibility(true);
                parent.setSupportProgress((value-min ) * incrementFactor);
				statusBarTitle = parent.getTitle().toString();
				parent.setTitle(message);
			} else {
				Log.w(TAG, "Unknown type: " + type);
			}
		} else if (location == DIALOG) {
			if (progressDialog == null) {
				Activity a = TiApplication.getInstance().getCurrentActivity();
				if (a == null) {
					a = TiApplication.getInstance().getRootActivity();
				}
				progressDialog = new ProgressDialog(a);
				if (a instanceof TiBaseActivity) {
					TiBaseActivity baseActivity = (TiBaseActivity) a;
					baseActivity.addDialog(baseActivity.new DialogWrapper(progressDialog, true, new WeakReference<TiBaseActivity> (baseActivity)));
					progressDialog.setOwnerActivity(a);
				}
				progressDialog.setOnShowListener(new DialogInterface.OnShowListener(){
			        @Override
			        public void onShow(DialogInterface dialog) {
			        	TiApplication.getInstance().cancelPauseEvent();
			        }
				});
				progressDialog.setOnCancelListener(this);
			}

			progressDialog.setMessage(message);
			progressDialog.setCanceledOnTouchOutside(proxy.getProperties().optBoolean(TiC.PROPERTY_CANCELED_ON_TOUCH_OUTSIDE, true));
			progressDialog.setCancelable(cancelable);

			if (type == INDETERMINANT) {
				progressDialog.setIndeterminate(true);
			} else if (type == DETERMINANT) {
				progressDialog.setIndeterminate(false);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setMax(max-min);
				progressDialog.setProgress((value-min ) * incrementFactor);
			} else {
				Log.w(TAG, "Unknown type: " + type);
			}
			progressDialog.show();
		} else {
			Log.w(TAG, "Unknown location: " + location);
		}
		visible = true;
	}

	public void hide(KrollDict options)
	{
		if (!visible) {
			return;
		}
		handler.sendEmptyMessage(MSG_HIDE);
	}

	protected void handleHide() {
		if (progressDialog != null) {
			Activity ownerActivity = progressDialog.getOwnerActivity();
			if (ownerActivity != null && !ownerActivity.isFinishing()) {
				((TiBaseActivity)ownerActivity).removeDialog(progressDialog);
				progressDialog.dismiss();
			}
			progressDialog = null;
		} else {
			Activity parent = (Activity) proxy.getActivity();
			parent.setProgressBarIndeterminate(false);
			parent.setProgressBarIndeterminateVisibility(false);
			parent.setProgressBarVisibility(false);
			parent.setTitle(statusBarTitle);
			statusBarTitle = null;
		}
		visible = false;
	}

	@Override
	public void onCancel(DialogInterface dialog) {
		visible = false;
		fireEvent(TiC.EVENT_CANCEL, null, false);
	}
}
