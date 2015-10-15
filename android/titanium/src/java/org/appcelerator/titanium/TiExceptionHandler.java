/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2015 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium;

import java.util.LinkedList;

import org.appcelerator.kroll.KrollApplication;
import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollExceptionHandler;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.CurrentActivityListener;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Handler;
import android.os.Message;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A utility class for creating a dialog that displays Javascript errors
 */
public class TiExceptionHandler implements Handler.Callback, KrollExceptionHandler
{
	private static final String TAG = "TiExceptionHandler";
	private static final int MSG_OPEN_ERROR_DIALOG = 10011;
	private static LinkedList<ExceptionMessage> errorMessages = new LinkedList<ExceptionMessage>();
	private static boolean dialogShowing = false;
	private static Handler mainHandler;
	private static int layoutId = -1;
    private static int styleId;
	private static int layoutLocationId;
	private static int layoutMessageId;
	private static int layoutSourceId;
	private static int layoutCallstackId;

	public void printError(String title, String message, String sourceName, int line, String lineSource,
		int lineOffset, final String callstack)
	{
		Log.e(TAG, "----- Titanium Javascript " + title + " -----");
		Log.e(TAG, "- In " + sourceName + ":" + line + "," + lineOffset);
		Log.e(TAG, "- Message: " + message);
		Log.e(TAG, "- Source: " + lineSource);
		Log.e(TAG, "- Callstack: " + callstack);
	}

	public TiExceptionHandler()
	{
		mainHandler = new Handler(TiMessenger.getMainMessenger().getLooper(), this);
	}

	public void openErrorDialog(ExceptionMessage error)
	{
		if (TiApplication.isUIThread()) {
			handleOpenErrorDialog(error);
		} else {
			TiMessenger.sendBlockingMainMessage(mainHandler.obtainMessage(MSG_OPEN_ERROR_DIALOG), error);
		}
	}

	protected void handleOpenErrorDialog(ExceptionMessage error)
	{
		KrollApplication application = KrollRuntime.getInstance().getKrollApplication();
		if (application == null) {
			return;
		}

		Activity activity = application.getCurrentActivity();
		if (activity == null || activity.isFinishing()) {
			Log.w(TAG, "Activity is null or already finishing, skipping dialog.");
			return;
		}
		if (error.callstack != null) {
		    String[] callstackArray = error.callstack.split("\n");
	        StringBuilder sb = new StringBuilder();
	        int i = 0;
	        for (i = 1; i < callstackArray.length; i++) {
	            String value = callstackArray[i];
	            if (value.indexOf("at Module._runScript") != -1) {
	                break;
	            }
	            if (i != 1) {
	                sb.append("\n");
	            }
	            sb.append(value);
	        }
	        error.callstack = sb.toString();
		}
		
		KrollDict dict = new KrollDict();
		dict.put("title", error.title);
		dict.put("message", error.message);
		dict.put("sourceName", error.sourceName);
		dict.put("line", error.line);
		dict.put("lineSource", error.lineSource);
		dict.put("lineOffset", error.lineOffset);
		TiApplication.getInstance().fireAppEvent("uncaughtException", dict);
		printError(error.title, error.message, error.sourceName, error.line, error.lineSource, error.lineOffset, error.callstack);

		TiApplication tiApplication = TiApplication.getInstance();
		if (tiApplication.getDeployType().equals(TiApplication.DEPLOY_TYPE_PRODUCTION)) {
			return;
		}

		if (!dialogShowing) {
			dialogShowing = true;
			final ExceptionMessage fError = error;
			application.waitForCurrentActivity(new CurrentActivityListener()
			{
				@Override
				public void onCurrentActivityReady(Activity activity)
				{
					createDialog(fError);
				}
			});
		} else {
			errorMessages.add(error);
		}
	}

	protected static void createDialog(final ExceptionMessage error)
	{
		TiApplication application = TiApplication.getInstance();
		if (layoutId == -1) {
            try {
                layoutId = TiRHelper.getApplicationResource("layout.ti_exception_dialog");
                styleId = TiRHelper.getApplicationResource("style.TiExceptionDialogStyle");
                layoutLocationId = TiRHelper.getApplicationResource("id.tiexd_location");
                layoutSourceId = TiRHelper.getApplicationResource("id.tiexd_source");
                layoutMessageId = TiRHelper.getApplicationResource("id.tiexd_message");
                layoutCallstackId = TiRHelper.getApplicationResource("id.tiexd_callstack");
            } catch (ResourceNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }
		if (application == null || layoutId <= 0) {
			return;
		}
		
		Activity activity = application.getCurrentActivity();

		ViewGroup layout = (ViewGroup) application.getCurrentActivity().getLayoutInflater().inflate(layoutId, null);
		
		if (layout == null) {
		    return;
            
		}
		
		((TextView) layout.findViewById(layoutLocationId)).setText("[" + error.line + "," + error.lineOffset + "] " + error.sourceName);
        ((TextView) layout.findViewById(layoutMessageId)).setText(error.message);
        if (error.lineSource != null) {
            ((TextView) layout.findViewById(layoutSourceId)).setText(error.lineSource.trim());
        }
        if (error.callstack != null) {
            ((TextView) layout.findViewById(layoutCallstackId)).setText(error.callstack);
        }
		OnClickListener clickListener = new OnClickListener()
		{
			public void onClick(DialogInterface dialog, int which)
			{
				if (which == DialogInterface.BUTTON_POSITIVE) {
		            TiApplication.getInstance().getRootActivity().finish();
		            System.exit(0);

				} else if (which == DialogInterface.BUTTON_NEUTRAL) {
					// Continue
				} else if (which == DialogInterface.BUTTON_NEGATIVE) {
					// TODO: Reload (Fastdev)
					// if (error.tiContext != null && error.tiContext.get() != null) {
					// reload(error.sourceName);
					// }

				}
				if (!errorMessages.isEmpty()) {
					createDialog(errorMessages.removeFirst());

				} else {
					dialogShowing = false;
				}
			}
		};

		AlertDialog.Builder builder = new AlertDialog.Builder(activity, styleId)
			.setTitle(error.title).setView(layout)
			.setPositiveButton("Kill", clickListener)
			.setNeutralButton("Continue", clickListener)
			.setCancelable(false);

		// TODO: Enable when we have fastdev working
		// if (TiFastDev.isFastDevEnabled()) {
		// builder.setNegativeButton("Reload", clickListener);
		// }
		builder.create().show();
	}

	protected static void reload(String sourceName)
	{
		// try {
		// TODO: Enable this when we have fastdev
		// KrollContext.getKrollContext().evalFile(sourceName);
		/*
		 * } catch (IOException e) {
		 * Log.e(TAG, e.getMessage(), e);
		 * }
		 */
	}

	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_OPEN_ERROR_DIALOG:
				AsyncResult asyncResult = (AsyncResult) msg.obj;
				ExceptionMessage errorMessage = (ExceptionMessage) asyncResult.getArg();
				handleOpenErrorDialog(errorMessage);
				asyncResult.setResult(null);
				return true;
			default:
				break;
		}

		return false;
	}

	/**
	 * Handles the exception by opening an error dialog with an error message
	 * @param error An error message containing line number, error title, message, etc
	 * @module.api
	 */
	public void handleException(ExceptionMessage error)
	{
		openErrorDialog(error);
	}
}
