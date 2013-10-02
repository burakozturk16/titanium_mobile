/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.lang.ref.SoftReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.view.TiDrawableReference;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

/**
 * Manages the asynchronous opening of InputStreams from URIs so that
 * the resources get put into our TiResponseCache.
 */
public class TiLoadImageManager implements Handler.Callback
{
	private static final String TAG = "TiLoadImageManager";
	private static final int MSG_FIRE_LOAD_FINISHED = 1000;
	private static final int MSG_FIRE_LOAD_FAILED = 1001;
	protected static TiLoadImageManager _instance;
	public static final int THREAD_POOL_SIZE = 2;

	protected SparseArray<ArrayList<SoftReference<TiLoadImageListener>>> listeners = new SparseArray<ArrayList<SoftReference<TiLoadImageListener>>>();
	protected ArrayList<Integer> loadingImageRefs = new ArrayList<Integer>();
	protected ExecutorService threadPool;
	protected Handler handler;

	public static TiLoadImageManager getInstance()
	{
		if (_instance == null) {
			_instance = new TiLoadImageManager();
		}
		return _instance;
	}

	protected TiLoadImageManager()
	{
		handler = new Handler(this);
		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}

	public void load(TiDrawableReference imageref, TiLoadImageListener listener)
	{
		int hash = imageref.hashCode();
		if (listener != null) {
			ArrayList<SoftReference<TiLoadImageListener>> listenerList = null;
			synchronized (listeners) {
				if (listeners.get(hash) == null) {
					listenerList = new ArrayList<SoftReference<TiLoadImageListener>>();
					listeners.put(hash, listenerList);
				} else {
					listenerList = listeners.get(hash);
				}
				// We don't allow duplicate listeners for the same image.
				for (SoftReference<TiLoadImageListener> l : listenerList) {
					if (l.get() == listener) {
						return;
					}
				}
				listenerList.add(new SoftReference<TiLoadImageListener>(listener));
			}
		}
		
		synchronized (loadingImageRefs) {
			if (!loadingImageRefs.contains(hash)) {
				loadingImageRefs.add(hash);
				threadPool.execute(new LoadImageJob(imageref));
			}
		}
	}
	
	public void loadSync(TiDrawableReference imageref, TiLoadImageListener listener)
	{
		try {
			Bitmap bitmap = imageref.getBitmap(false);
			int hash = imageref.hashCode();
			listener.loadImageFinished(hash, bitmap);
		} catch (Exception e) {
			listener.loadImageFailed();
		}
	}

	protected void handleLoadImageMessage(int what, int hash, Bitmap bitmap)
	{
		ArrayList<SoftReference<TiLoadImageListener>> toRemove = new ArrayList<SoftReference<TiLoadImageListener>>();
		synchronized (listeners) {
			ArrayList<SoftReference<TiLoadImageListener>> listenerList = listeners.get(hash);
			for (SoftReference<TiLoadImageListener> listener : listenerList) {
				TiLoadImageListener l = listener.get();
				if (l != null) {
					if (what == MSG_FIRE_LOAD_FINISHED) {
						l.loadImageFinished(hash, bitmap);
						toRemove.add(listener);
					} else {
						l.loadImageFailed();
						toRemove.add(listener);
					}
				}
			}
			for (SoftReference<TiLoadImageListener> listener : toRemove) {
				listenerList.remove(listener);
			}
		}
	}

	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_FIRE_LOAD_FINISHED:
				handleLoadImageMessage(MSG_FIRE_LOAD_FINISHED, (Integer)msg.arg1, (Bitmap)msg.obj);
				return true;
			case MSG_FIRE_LOAD_FAILED:
				if ((Integer)msg.arg2 == 1) { //retry download
					TiDrawableReference imageref = (TiDrawableReference) msg.obj;
					try {
						String imageUrl = TiUrl.getCleanUri(imageref.getUrl()).toString();
						URI uri = new URI(imageUrl);
						TiResponseCache.remove(uri);
						load(imageref, null);
					} catch (URISyntaxException e) {
						handleLoadImageMessage(MSG_FIRE_LOAD_FAILED, (Integer)msg.arg1, null);
					}
				}
				else {
					handleLoadImageMessage(MSG_FIRE_LOAD_FAILED, (Integer)msg.arg1, null);
				}
				return true;
		}
		return false;
	}

	protected class LoadImageJob implements Runnable
	{
		protected TiDrawableReference imageref;

		public LoadImageJob (TiDrawableReference imageref)
		{
			this.imageref = imageref;
		}

		public void run()
		{
			try {
				Bitmap b = imageref.getBitmap(true);
				synchronized (loadingImageRefs) {
					loadingImageRefs.remove((Integer)imageref.hashCode());
				}
				if (b != null) {
					if (b.getWidth() > 4096 || b.getHeight() > 4096) { //too big!
						int width = b.getWidth();
						int height = b.getHeight();
						int dstWidth = width;
						int dstHeight = height;
						if (width > height) {
							dstWidth = 4096;
							dstHeight = dstWidth * height / width;
						}
						else {
							dstHeight = 4096;
							dstWidth = dstHeight * width / height;
						}
						
						try {
							b = Bitmap.createScaledBitmap(b, dstWidth, dstHeight, true);
						} catch (OutOfMemoryError e) {
							Log.e(TAG, "Unable to resize the image. Not enough memory: " + e.getMessage(), e);
						}
						
					}
					Message msg = handler.obtainMessage(MSG_FIRE_LOAD_FINISHED);
					msg.obj = b;
					msg.arg1 = imageref.hashCode();
					msg.sendToTarget();
				}
				else {
					Message msg = handler.obtainMessage(MSG_FIRE_LOAD_FAILED);
					msg.obj = imageref;
					msg.arg1 = imageref.hashCode();
					msg.arg2 = 1;
					msg.sendToTarget();
				}
			} catch (Exception e) {
				// fire a download fail event if we are unable to download
				Log.e(TAG, "Exception loading image: " + e.getLocalizedMessage());
				Message msg = handler.obtainMessage(MSG_FIRE_LOAD_FAILED);
				msg.arg1 = imageref.hashCode();
				msg.arg2 = 0;
				msg.sendToTarget();
			}
		}
	}
}
