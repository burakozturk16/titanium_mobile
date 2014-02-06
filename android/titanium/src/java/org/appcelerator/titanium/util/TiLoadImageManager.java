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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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

	public void load(Resources resources, TiDrawableReference imageref, TiLoadImageListener listener)
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
				threadPool.execute(new LoadImageJob(resources, imageref));
			}
		}
	}
	
	public void loadSync(TiDrawableReference imageref, TiLoadImageListener listener)
	{
		try {
			Drawable drawable = imageref.getDrawable();
			int hash = imageref.hashCode();
			listener.loadImageFinished(hash, drawable);
		} catch (Exception e) {
			listener.loadImageFailed();
		}
	}

	protected void handleLoadImageMessage(int what, int hash, Drawable drawable)
	{
		ArrayList<SoftReference<TiLoadImageListener>> toRemove = new ArrayList<SoftReference<TiLoadImageListener>>();
		synchronized (listeners) {
			ArrayList<SoftReference<TiLoadImageListener>> listenerList = listeners.get(hash);
			for (SoftReference<TiLoadImageListener> listener : listenerList) {
				TiLoadImageListener l = listener.get();
				if (l != null) {
					if (what == MSG_FIRE_LOAD_FINISHED) {
						l.loadImageFinished(hash, drawable);
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
				handleLoadImageMessage(MSG_FIRE_LOAD_FINISHED, (Integer)msg.arg1, (Drawable)msg.obj);
				return true;
			case MSG_FIRE_LOAD_FAILED:
				if ((Integer)msg.arg2 == 1) { //retry download
					LoadImageJob job = (LoadImageJob) msg.obj;
					TiDrawableReference imageref = job.imageref;
					try {
						String imageUrl = TiUrl.getCleanUri(imageref.getUrl()).toString();
						URI uri = new URI(imageUrl);
						TiResponseCache.remove(uri);
						load(job.resources, imageref, null);
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
		protected Resources resources;

		public LoadImageJob (Resources resources, TiDrawableReference imageref)
		{
			this.imageref = imageref;
			this.resources = resources;
		}

		public void run()
		{
			try {
				Drawable d = imageref.getDrawable(true);
				synchronized (loadingImageRefs) {
					loadingImageRefs.remove((Integer)imageref.hashCode());
				}
				if (d == null) {
					Message msg = handler.obtainMessage(MSG_FIRE_LOAD_FAILED);
					msg.obj = this;
					msg.arg1 = imageref.hashCode();
					msg.arg2 = 0;
					msg.sendToTarget();
				}
				if (d instanceof BitmapDrawable) {
					Bitmap b = ((BitmapDrawable) d).getBitmap();
					if (b == null) {
						Message msg = handler.obtainMessage(MSG_FIRE_LOAD_FAILED);
						msg.obj = this;
						msg.arg1 = imageref.hashCode();
						msg.arg2 = 0;
						msg.sendToTarget();
						return;
					}
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
					
				}
				Message msg = handler.obtainMessage(MSG_FIRE_LOAD_FINISHED);
				msg.obj = d;
				msg.arg1 = imageref.hashCode();
				msg.sendToTarget();
				
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
