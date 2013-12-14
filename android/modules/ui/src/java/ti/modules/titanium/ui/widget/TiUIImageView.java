/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.transition.Transition;
import org.appcelerator.titanium.transition.TransitionHelper;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiDownloadListener;
import org.appcelerator.titanium.util.TiDownloadManager;
import org.appcelerator.titanium.util.TiImageLruCache;
import org.appcelerator.titanium.util.TiLoadImageListener;
import org.appcelerator.titanium.util.TiLoadImageManager;
import org.appcelerator.titanium.util.TiResponseCache;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUrl;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUINonViewGroupView;

import com.trevorpage.tpsvg.SVGDrawable;

import ti.modules.titanium.filesystem.FileProxy;
import ti.modules.titanium.ui.ImageViewProxy;
import ti.modules.titanium.ui.ScrollViewProxy;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View.MeasureSpec;
import android.widget.ImageView.ScaleType;

public class TiUIImageView extends TiUINonViewGroupView implements OnLifecycleEvent, Handler.Callback
{
	private static final String TAG = "TiUIImageView";
	private static final int FRAME_QUEUE_SIZE = 5;
	public static final int INFINITE = 0;
	public static final int MIN_DURATION = 30;
	public static final int DEFAULT_DURATION = 200;

	private Timer timer;
	private Animator animator;
	private Loader loader;
	private Thread loaderThread;
	private AtomicBoolean animating = new AtomicBoolean(false);
	private AtomicBoolean isLoading = new AtomicBoolean(false);
	private AtomicBoolean isStopping = new AtomicBoolean(false);
	private TiAnimationDrawable animDrawable = null;
	private boolean reverse = false;
	private boolean paused = false;
	private boolean localLoadSync = false;
	private boolean firedLoad;
	private ImageViewProxy imageViewProxy;
	private int duration;
	private int repeatCount = INFINITE;
	private Drawable currentImage = null;

	private ArrayList<TiDrawableReference> imageSources;
	private ArrayList<TiDrawableReference> animatedImageSources;
	private TiDrawableReference defaultImageSource;
	private TiDownloadListener downloadListener;
	private TiLoadImageListener loadImageListener;
	private Object releasedLock = new Object();
	
	private Handler mainHandler = new Handler(Looper.getMainLooper(), this);
	private static final int SET_IMAGE = 10001;
	private static final int START = 10002;
	private static final int STOP = 10003;
	private static final int SET_DRAWABLE = 10004;
	private static final int RESUME = 10005;

	// This handles the memory cache of images.
	private TiImageLruCache mMemoryCache = TiImageLruCache.getInstance();
	
	private HashMap transitionDict = null;

	public TiUIImageView(final TiViewProxy proxy)
	{
		super(proxy);
		imageViewProxy = (ImageViewProxy) proxy;
		Log.d(TAG, "Creating an ImageView", Log.DEBUG_MODE);

		TiImageView view = new TiImageView(proxy.getActivity(), proxy) {
			@Override
			protected void onLayout(boolean changed, int left, int top, int right, int bottom)
			{
				super.onLayout(changed, left, top, right, bottom);
				TiUIHelper.firePostLayoutEvent(TiUIImageView.this);
			}
		};
		setImage(null); //this actually creates a drawable which will allow transition

		downloadListener = new TiDownloadListener()
		{
			@Override
			public void downloadTaskFinished(URI uri)
			{
				if (!TiResponseCache.peek(uri)) {
					// The requested image did not make it into our TiResponseCache,
					// possibly because it had a header forbidding that. Now get it
					// via the "old way" (not relying on cache).
					TiLoadImageManager.getInstance().load(proxy.getActivity().getResources(), TiDrawableReference.fromUrl(imageViewProxy, uri.toString()), loadImageListener);
				}
			}

			@Override
			public void downloadTaskFailed(URI uri)
			{
				// If the download failed, fire an error event
				fireError("Download Failed", uri.toString());
			}

			// Handle decoding and caching in the background thread so it won't block UI.
			@Override
			public void postDownload(URI uri)
			{
				if (TiResponseCache.peek(uri)) {
					try {
						handleCacheAndSetImage(TiDrawableReference.fromUrl(imageViewProxy, uri.toString()));
					} catch (FileNotFoundException e) {
						fireError("Download Failed", uri.toString());
					}
				}
			}
		};

		loadImageListener = new TiLoadImageListener()
		{
			@Override
			public void loadImageFinished(int hash, Drawable drawable)
			{
				// Cache the image
				if (drawable != null) {
					if (mMemoryCache.get(hash) == null && drawable instanceof BitmapDrawable) {
						mMemoryCache.put(hash, ((BitmapDrawable) drawable).getBitmap());
					}

					// Update UI if the current image source has not been changed.
					if (imageSources != null && imageSources.size() == 1) {
						TiDrawableReference imgsrc = imageSources.get(0);
						if (imgsrc == null) {
							return;
						}
						if (imgsrc.hashCode() == hash
							|| (imgsrc.getUrl() != null && TiDrawableReference.fromUrl(imageViewProxy, TiUrl.getCleanUri(imgsrc.getUrl()).toString())
								.hashCode() == hash)) {
//							setImage(bitmap);
							setDrawable(drawable);
							if (!firedLoad) {
								if (drawable instanceof BitmapDrawable) {
									fireLoad(TiC.PROPERTY_IMAGE, ((BitmapDrawable) drawable).getBitmap());
								}
								else {
									fireLoad(TiC.PROPERTY_IMAGE);
								}
								firedLoad = true;
							}
						}
					}
				}
			}

			@Override
			public void loadImageFailed()
			{
				Log.w(TAG, "Unable to load image", Log.DEBUG_MODE);
			}
		};
		setNativeView(view);
	}

	@Override
	public void setProxy(TiViewProxy proxy)
	{
		super.setProxy(proxy);
		imageViewProxy = (ImageViewProxy) proxy;
	}

	private TiImageView getView()
	{
		return (TiImageView) nativeView;
	}

	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
		
		case SET_IMAGE:
		{
			AsyncResult result = (AsyncResult) msg.obj;
			handleSetImage((Bitmap) result.getArg());
			result.setResult(null);
			return true;
		}
		case SET_DRAWABLE:
		{
			AsyncResult result = (AsyncResult) msg.obj;
			handleSetDrawable((Drawable) result.getArg());
			result.setResult(null);
			return true;
		}
		case START:
			handleStart();
			return true;
		case STOP:
			handleStop();
			return true;
//		case PAUSE:
//			handlePause();
//			return true;
		case RESUME:
			handleResume();
			return true;	
		default: return false;
		
		}
	}

	private void handleCacheAndSetImage(TiDrawableReference imageref) throws FileNotFoundException
	{
		// Don't update UI if the current image source has been changed.
		if (imageSources != null && imageSources.size() == 1) {
			TiDrawableReference imgsrc = imageSources.get(0);
			if (imgsrc == null || imgsrc.getUrl() == null) {
				return;
			}
			if (imageref.equals(imgsrc)
				|| imageref
					.equals(TiDrawableReference.fromUrl(imageViewProxy, TiUrl.getCleanUri(imgsrc.getUrl()).toString()))) {
				int hash = imageref.hashCode();
				Drawable drawable = imageref.getDrawable();
				if (drawable != null) {
					if (drawable instanceof BitmapDrawable &&  mMemoryCache.get(hash) == null) {
						mMemoryCache.put(hash, ((BitmapDrawable) drawable).getBitmap());
					}
					setDrawable(drawable);
					if (!firedLoad) {
						if (drawable instanceof BitmapDrawable) {
							fireLoad(TiC.PROPERTY_IMAGE, ((BitmapDrawable) drawable).getBitmap());
						}
						else {
							fireLoad(TiC.PROPERTY_IMAGE);
						}
						firedLoad = true;
					}
				}
			}
		}
	}

	private void setImage(final Bitmap bitmap)
	{
		setDrawable((bitmap!=null)?new BitmapDrawable(proxy.getActivity().getResources(), bitmap):null);
	}
	
	private void setDrawable(final Drawable drawable)
	{
		if (getDrawable() == drawable) return;
		if (!TiApplication.isUIThread()) {
			TiMessenger.sendBlockingMainMessage(mainHandler.obtainMessage(SET_DRAWABLE), drawable);
		} else {
			handleSetDrawable(drawable);
		}
	}
	
	private Drawable getDrawable()
	{
		TiImageView view = getView();
		if (view!= null)
			return view.getImageDrawable();
		else  return null;
	}

	private void handleSetImage(final Bitmap bitmap)
	{
		TiImageView view = getView();
		if (view != null) {
			Transition transition = TransitionHelper.transitionFromObject(transitionDict, null, null);
			view.setImageBitmapWithTransition(bitmap, transition);
			boolean widthDefined = view.getWidthDefined();
			boolean heightDefined = view.getHeightDefined();
			if ((!widthDefined || !heightDefined)) {
				//force re-calculating the layout dimension and the redraw of the view
				//This is a trick to prevent getMeasuredWidth and Height to be 0
				view.measure(MeasureSpec.makeMeasureSpec(0, widthDefined?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED), 
		                   MeasureSpec.makeMeasureSpec(0, heightDefined?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED));
				view.requestLayout();
			}
		}
	}
	
	private void handleSetDrawable(final Drawable drawable)
	{
		if (drawable != animDrawable) {
			currentImage = drawable;
			if (animDrawable != null) {
				animDrawable.stop();
			}
		}
		TiImageView view = getView();
		if (view != null) {
			Transition transition = TransitionHelper.transitionFromObject(transitionDict, null, null);
			view.setImageDrawableWithTransition(drawable, transition);
			boolean widthDefined = view.getWidthDefined();
			boolean heightDefined = view.getHeightDefined();
			if ((!widthDefined || !heightDefined)) {
				//force re-calculating the layout dimension and the redraw of the view
				//This is a trick to prevent getMeasuredWidth and Height to be 0
//				view.measure(MeasureSpec.makeMeasureSpec( widthDefined?view.getMeasuredWidth():0, widthDefined?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED), 
//		                   MeasureSpec.makeMeasureSpec(heightDefined?view.getMeasuredHeight():0, heightDefined?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED));
				view.requestLayout();
			}
		}
	}


	private class BitmapWithIndex
	{
		public BitmapWithIndex(Bitmap b, int i)
		{
			this.bitmap = b;
			this.index = i;
		}

		public Bitmap bitmap;
		public int index;
	}

	private class Loader implements Runnable
	{
		private ArrayBlockingQueue<BitmapWithIndex> bitmapQueue;
		private int waitTime = 0;
		private int sleepTime = 50; //ms
		private int repeatIndex = 0;

		public Loader()
		{
			bitmapQueue = new ArrayBlockingQueue<BitmapWithIndex>(FRAME_QUEUE_SIZE);
		}

		private boolean isRepeating()
		{
			if (repeatCount <= INFINITE) {
				return true;
			}
			return repeatIndex < repeatCount;
		}

		private int getStart()
		{
			if (imageSources == null) {
				return 0;
			}
			if (reverse) {
				return imageSources.size() - 1;
			}
			return 0;

		}

		private boolean isNotFinalFrame(int frame)
		{
			synchronized (releasedLock) {
				if (imageSources == null) {
					return false;
				}
				if (reverse) {
					return frame >= 0;
				}
				return frame < imageSources.size();
			}
		}

		private int getCounter()
		{
			if (reverse) {
				return -1;
			}
			return 1;
		}

		public void run()
		{
			if (getProxy() == null) {
				Log.d(TAG, "Multi-image loader exiting early because proxy has been gc'd");
				return;
			}
			repeatIndex = 0;
			isLoading.set(true);
			firedLoad = false;
			topLoop: while (isRepeating()) {

				if (imageSources == null) {
					break;
				}
				long time = System.currentTimeMillis();
				for (int j = getStart(); imageSources != null && isNotFinalFrame(j); j += getCounter()) {
					if (bitmapQueue.size() == FRAME_QUEUE_SIZE && !firedLoad) {
						fireLoad(TiC.PROPERTY_IMAGES);
						firedLoad = true;
					}
					if (paused && !Thread.currentThread().isInterrupted()) {
						try {
							Log.i(TAG, "Pausing", Log.DEBUG_MODE);
							// User backed-out while animation running
							if (loader == null) {
								break;
							}

							synchronized (this) {
								wait();
							}

							Log.i(TAG, "Waking from pause.", Log.DEBUG_MODE);
							// In the meantime, while paused, user could have backed out, which leads
							// to release(), which in turn leads to nullified imageSources.
							if (imageSources == null) {
								break topLoop;
							}
						} catch (InterruptedException e) {
							Log.w(TAG, "Interrupted from paused state.");
						}
					}

					if (!isLoading.get() || isStopping.get()) {
						break topLoop;
					}

					waitTime = 0;
					synchronized (releasedLock) {
						if (imageSources == null || j >= imageSources.size()) {
							break topLoop;
						}
						Bitmap b;
						try {
							b = imageSources.get(j).getBitmap(true);
						} catch (FileNotFoundException e1) {
							b = null;
						}
						BitmapWithIndex bIndex = new BitmapWithIndex(b,j);
						while (waitTime < duration * imageSources.size()) {
							try {
								if (!bitmapQueue.offer(bIndex)) {
									if (isStopping.get()) {
										break;
									} 
									Thread.sleep(sleepTime);
									waitTime += sleepTime;

								} else {
									break;
								}

							} catch (InterruptedException e) {
								Log.w(TAG, "Interrupted while adding Bitmap into bitmapQueue");
								break;
							}
						}
					}
					repeatIndex++;
				}

				Log.d(TAG, "TIME TO LOAD FRAMES: " + (System.currentTimeMillis() - time) + "ms", Log.DEBUG_MODE);

			}
			isLoading.set(false);
		}

		public ArrayBlockingQueue<BitmapWithIndex> getBitmapQueue()
		{
			return bitmapQueue;
		}
	}

	private void setImages()
	{
		if (imageSources == null || imageSources.size() == 0) {
			fireError("Missing Images", null);
			return;
		}
		animDrawable = null;

		if (loader == null) {
			paused = false;
			isStopping.set(false);
			firedLoad = false;
			loader = new Loader();
			loaderThread = new Thread(loader);
			Log.d(TAG, "STARTING LOADER THREAD " + loaderThread + " for " + this, Log.DEBUG_MODE);
			loaderThread.start();
		}

	}
	
	private void setAnimatedImages()
	{
		if (animatedImageSources == null || animatedImageSources.size() == 0) {
			handleSetDrawable(null);
			fireError("Missing Images", null);
			return;
		}
		animDrawable = new TiAnimationDrawable();
		animDrawable.setReverse(reverse);
		animDrawable.setAutoreverse(autoreverse);
		for (int i = 0; i < animatedImageSources.size(); i++) {
			TiDrawableReference imageref = animatedImageSources.get(i);
			// Check if the image is cached in memory
			int hash = imageref.hashCode();
			Bitmap bitmap = mMemoryCache.get(hash);
			if (bitmap != null) {
				if (!bitmap.isRecycled()) {
					animDrawable.addFrame(new BitmapDrawable(proxy.getActivity().getResources(), bitmap), duration);
					continue;
				} else { // If the cached image has been recycled, remove it from the cache.
					mMemoryCache.remove(hash);
				}
			}
			if (!imageref.isNetworkUrl()) {
				Drawable drawable = null;
				try {
					drawable = imageref.getDrawable();
				} catch (FileNotFoundException e) {
					Log.e(TAG, "Could not find image for url " + imageref.getUrl(), e);
				}
				if (drawable != null) {
					if (mMemoryCache.get(hash) == null && drawable instanceof BitmapDrawable) {
						mMemoryCache.put(hash, ((BitmapDrawable) drawable).getBitmap());
					}
					animDrawable.addFrame(drawable, duration);
				}
				else {
					Log.e(TAG, "Could not find image for url " + imageref.getUrl());
				}
			}
		}
		currentIndex = reverse?animDrawable.getNumberOfFrames()-1:0;
		animDrawable.selectDrawable(currentIndex);
		if (currentImage == null) setDrawable(animDrawable);
	}
	
	public boolean fireImageEvent(String eventName, KrollDict data) {
		return fireEvent(eventName, data, false);
	}

	private void fireLoad(String state)
	{
		if (proxy.hasListeners(TiC.EVENT_LOAD)) {
			KrollDict data = new KrollDict();
			data.put(TiC.EVENT_PROPERTY_STATE, state);
			fireImageEvent(TiC.EVENT_LOAD, data);
		}
	}
	
	private void fireLoad(String state, Bitmap bitmap)
	{
		if (proxy.hasListeners(TiC.EVENT_LOAD)) {
			KrollDict data = new KrollDict();
			data.put("image", TiBlob.blobFromImage(bitmap));
			data.put(TiC.EVENT_PROPERTY_STATE, state);
			fireImageEvent(TiC.EVENT_LOAD, data);
		}
	}

	private void fireStart()
	{
		if (proxy.hasListeners(TiC.EVENT_START)) {
			fireImageEvent(TiC.EVENT_START, null);
		}
	}

	private void fireChange(int index)
	{
		if (proxy.hasListeners(TiC.EVENT_CHANGE)) {
			KrollDict data = new KrollDict();
			data.put(TiC.EVENT_PROPERTY_INDEX, index);
			fireImageEvent(TiC.EVENT_CHANGE, data);
		}
	}

	private void fireStop()
	{
		if (proxy.hasListeners(TiC.EVENT_LOAD)) {
			fireImageEvent(TiC.EVENT_LOAD, null);
		}
	}

	private void fireError(String message, String imageUrl)
	{
		if (proxy.hasListeners(TiC.EVENT_ERROR)) {
			KrollDict data = new KrollDict();

			data.putCodeAndMessage(TiC.ERROR_CODE_UNKNOWN, message);
			if (imageUrl != null) {
				data.put(TiC.PROPERTY_IMAGE, imageUrl);
			}
			fireImageEvent(TiC.EVENT_ERROR, data);
		}
	}
	private int currentIndex = 0;
	private boolean autoreverse = false;

	private class Animator extends TimerTask
	{
		private Loader loader;

		public Animator(Loader loader)
		{
			this.loader = loader;
		}

		public void run()
		{
			boolean waitOnResume = false;
			try {
				if (paused) {
					synchronized (this) {
						if (proxy.hasListeners(TiC.EVENT_PAUSE)) {
							fireImageEvent(TiC.EVENT_PAUSE, null);
						}
						waitOnResume = true;
						wait();
					}
				}
				

				BitmapWithIndex b = loader.getBitmapQueue().take();
				Log.d(TAG, "set image: " + b.index, Log.DEBUG_MODE);
				setImage(b.bitmap);
				fireChange(b.index);

				// When the animation is paused, the timer will pause in the middle of a period.
				// When the animation resumes, the timer resumes from where it left off. As a result, it will look like
				// one frame is left out when resumed (TIMOB-10207).
				// To avoid this, we force the thread to wait for one period on resume.
				if (waitOnResume) {
					Thread.sleep(duration);
					waitOnResume = false;
				}
			} catch (InterruptedException e) {
				Log.e(TAG, "Loader interrupted");
			}
		}
	}

	public void start()
	{
		currentIndex = (animDrawable != null)?(reverse?animDrawable.getNumberOfFrames()-1:0):0;
		if (!TiApplication.isUIThread()) {
			Message message = mainHandler.obtainMessage(START);
			message.sendToTarget();
		} else {
			handleStart();
		}
	}

	public void handleStart()
	{
		if (animDrawable != null) {
			if (getDrawable() != animDrawable) {
				setDrawable(animDrawable);
			}
			animDrawable.start();
			return;
		}
		if (animator == null) {
			timer = new Timer();
			
			if (loader == null) {
				loader = new Loader();
				loaderThread = new Thread(loader);
				Log.d(TAG, "STARTING LOADER THREAD " + loaderThread + " for " + this, Log.DEBUG_MODE);
			}
			
			animator = new Animator(loader);
			if (!animating.get() && !loaderThread.isAlive()) {
				isStopping.set(false);
				if (loaderThread.getState() == Thread.State.NEW)
					loaderThread.start();
			}

			animating.set(true);
			fireStart();
			timer.schedule(animator, duration, duration);
		} else {
			resume();
		}
	}

	public void pause() 
	{
		paused = true;
		if (animDrawable != null) {
			animDrawable.pause();
			return;
		}
	}


	public void handleResume()
	{
		if (animDrawable != null) {
			animDrawable.resume();
			if (getDrawable() != animDrawable) {
				setDrawable(animDrawable);
			}
			return;
		}
		if (animator == null) {
			handleStart();
			return;
		}
		if (animator != null) {
			synchronized (animator) {
				animator.notify();
			}
		}
		
		if (loader != null) {
			synchronized (loader) {
				loader.notify();
			}
		}
	}

	public void resume()
	{
		paused = false;
		if (!TiApplication.isUIThread()) {
			Message message = mainHandler.obtainMessage(RESUME);
			message.sendToTarget();		
		} else {
			handleResume();
		}
	}

	public void stop()
	{
		if (!TiApplication.isUIThread()) {
			Message message = mainHandler.obtainMessage(STOP);
			message.sendToTarget();		
		} else {
			handleStop();
		}
	}
	public void handleStop()
	{
		if (animDrawable != null) {
			animDrawable.stop();
			if (currentImage != null) {
				setDrawable(currentImage);
			}
			return;
		}
		if (timer != null) {
			timer.cancel();
		}

		animating.set(false);
		isStopping.set(true);

		if (loaderThread != null) {
			try {
				loaderThread.join();
			} catch (InterruptedException e) {
				Log.e(TAG, "LoaderThread termination interrupted");
			}
			loaderThread = null;
		}
		if (loader != null) {
			synchronized (loader) {
				loader.notify();
			}
		}

		loader = null;
		timer = null;
		animator = null;
		paused = false;

		fireStop();
	}

	private void setImageSource(Object object)
	{
		imageSources = new ArrayList<TiDrawableReference>();
		if (object instanceof Object[]) {
			for (Object o : (Object[]) object) {
				imageSources.add(makeImageSource(o));
			}
		} else {
			imageSources.add(makeImageSource(object));
		}
	}
	
	private void setAnimatedImageSource(Object object)
	{
		animatedImageSources = new ArrayList<TiDrawableReference>();
		if (object instanceof Object[]) {
			for (Object o : (Object[]) object) {
				animatedImageSources.add(makeImageSource(o));
			}
		} else {
			animatedImageSources.add(makeImageSource(object));
		}
	}

	private void setImageSource(TiDrawableReference source)
	{
		imageSources = new ArrayList<TiDrawableReference>();
		imageSources.add(source);
	}

	private TiDrawableReference makeImageSource(Object object)
	{
		if (object instanceof FileProxy) {
			return TiDrawableReference.fromFile(proxy.getActivity(), ((FileProxy) object).getBaseFile());
		} else if (object instanceof String) {
			return TiDrawableReference.fromUrl(proxy, (String) object);
		} else {
			return TiDrawableReference.fromObject(proxy.getActivity(), object);
		}
	}

	private void setDefaultImageSource(Object object)
	{
		if (object instanceof FileProxy) {
			defaultImageSource = TiDrawableReference.fromFile(proxy.getActivity(), ((FileProxy) object).getBaseFile());
		} else if (object instanceof String) {
			defaultImageSource = TiDrawableReference.fromUrl(proxy, (String) object);
		} else {
			defaultImageSource = TiDrawableReference.fromObject(proxy.getActivity(), object);
		}
	}
	
	private void setImageInternal() {
		// Set default image or clear previous image first.
		if (defaultImageSource != null) {
			setDefaultImage();
		}

		if (imageSources == null || imageSources.size() == 0 || imageSources.get(0) == null
			|| imageSources.get(0).isTypeNull()) {
			setImage(null);
			return;
		}

		if (imageSources.size() == 1) {
			TiDrawableReference imageref = imageSources.get(0);

			// Check if the image is cached in memory
			int hash = imageref.hashCode();
			Bitmap bitmap = mMemoryCache.get(hash);
			if (bitmap != null) {
				if (!bitmap.isRecycled()) {
					setImage(bitmap);
					if (!firedLoad) {
						fireLoad(TiC.PROPERTY_IMAGE, bitmap);
						firedLoad = true;
					}
					return;
				} else { // If the cached image has been recycled, remove it from the cache.
					mMemoryCache.remove(hash);
				}
			}

			if (imageref.isNetworkUrl()) {
				boolean isCachedInDisk = false;
				URI uri = null;
				try {
					String imageUrl = TiUrl.getCleanUri(imageref.getUrl()).toString();
					uri = new URI(imageUrl);
					isCachedInDisk = TiResponseCache.peek(uri);
				} catch (URISyntaxException e) {
					Log.e(TAG, "URISyntaxException for url " + imageref.getUrl(), e);
				} catch (NullPointerException e) {
					Log.e(TAG, "NullPointerException for url " + imageref.getUrl(), e);
				}

				// Check if the image is not cached in disc and the uri is valid.
				if (!isCachedInDisk && uri != null) {
					TiDownloadManager.getInstance().download(uri, downloadListener);
					return;
				}
			}
			else {
				Drawable drawable = null;
				try {
					drawable = imageref.getDrawable();
				} catch (FileNotFoundException e) {
					Log.e(TAG, "Could not find image for url " + imageref.getUrl(), e);
				}
				if (drawable != null) {
					if (mMemoryCache.get(hash) == null && drawable instanceof BitmapDrawable) {
						mMemoryCache.put(hash, ((BitmapDrawable) drawable).getBitmap());
					}
//					if (!bitmap.isRecycled()) {
					setDrawable(drawable);
					if (!firedLoad) {
						if (drawable instanceof BitmapDrawable) {
							fireLoad(TiC.PROPERTY_IMAGE, ((BitmapDrawable) drawable).getBitmap());
						}
						else {
							fireLoad(TiC.PROPERTY_IMAGE);
						}
					}
					return;
//					}
				}
			}
			if (localLoadSync == true)
				TiLoadImageManager.getInstance().loadSync(imageref, loadImageListener);
			else
				TiLoadImageManager.getInstance().load(proxy.getActivity().getResources(), imageref, loadImageListener);
		} else {
			setImages();
		}
	}

	private void setDefaultImage()
	{
		if (defaultImageSource == null) {
			setImage(null);
			return;
		}
		// Have to set default image in the UI thread to make sure it shows before the image
		// is ready. Don't need to retry decode because we don't want to block UI.
		try {
			setDrawable(defaultImageSource.getDrawable());
		} catch (FileNotFoundException e) {
			setImage(null);
		}
	}

	@Override
	public void processProperties(KrollDict d)
	{
		boolean heightDefined = false;
		boolean widthDefined = false;
		TiImageView view = getView();

		if (view == null) {
			return;
		}
		
		super.processProperties(d);

		view.setWidthDefined(!(layoutParams.autoSizeWidth() && (layoutParams.optionLeft == null || layoutParams.optionRight == null)));
		view.setHeightDefined(!(layoutParams.autoSizeHeight() && (layoutParams.optionTop == null || layoutParams.optionBottom == null)));

		if (d.containsKey(TiC.PROPERTY_IMAGES)) {
			setImageSource(d.get(TiC.PROPERTY_IMAGES));
			setImages();
		}
		
		if (d.containsKey(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS)) {
			view.setEnableZoomControls(TiConvert.toBoolean(d, TiC.PROPERTY_ENABLE_ZOOM_CONTROLS, true));
		}
		if (d.containsKey(TiC.PROPERTY_DEFAULT_IMAGE)) {
			setDefaultImageSource(d.get(TiC.PROPERTY_DEFAULT_IMAGE));
		}
		if(d.containsKey(TiC.PROPERTY_TRANSITION)) {
			Object value = d.get(TiC.PROPERTY_TRANSITION);
			if (value instanceof HashMap) {
				transitionDict = (HashMap) value;
			}
			else {
				transitionDict = null;
			}
		}
		if(d.containsKey(TiC.PROPERTY_DURATION)) {
			duration = TiConvert.toInt(d.get(TiC.PROPERTY_DURATION), DEFAULT_DURATION);
			if (duration < MIN_DURATION) {
				duration = MIN_DURATION;
			}
		}
		if(d.containsKey(TiC.PROPERTY_REVERSE)) {
			reverse = TiConvert.toBoolean(d.get(TiC.PROPERTY_DURATION), false);
		}
		if(d.containsKey(TiC.PROPERTY_AUTOREVERSE)) {
			autoreverse = TiConvert.toBoolean(d.get(TiC.PROPERTY_AUTOREVERSE), false);
		}
		if(d.containsKey(TiC.PROPERTY_REPEAT_COUNT)) {
			repeatCount = TiConvert.toInt(d.get(TiC.PROPERTY_REPEAT_COUNT), INFINITE);
		}
		if(d.containsKey(TiC.PROPERTY_LOCAL_LOAD_SYNC)) {
			localLoadSync = TiConvert.toBoolean(d, TiC.PROPERTY_LOCAL_LOAD_SYNC, localLoadSync);
//			view.setAnimateTransition(!localLoadSync);
		}
		
		if (d.containsKey(TiC.PROPERTY_SCALE_TYPE)) {
			setWantedScaleType(TiConvert.toInt(d, TiC.PROPERTY_SCALE_TYPE));
		}
		
		if (d.containsKey(TiC.PROPERTY_IMAGE)) {
			// processProperties is also called from TableView, we need check if we changed before re-creating the
			// bitmap
			boolean changeImage = true;
			TiDrawableReference source = makeImageSource(d.get(TiC.PROPERTY_IMAGE));
			if (imageSources != null && imageSources.size() == 1) {
				if (imageSources.get(0).equals(source)) {
					changeImage = false;
				}
			}
			if (changeImage) {
				view.setImageBitmap(null);
				view.setImageDrawable(null);
				// Check for orientation and decodeRetries only if an image is specified
				Object autoRotate = d.get(TiC.PROPERTY_AUTOROTATE);
				if (autoRotate != null && TiConvert.toBoolean(autoRotate)) {
					view.setOrientation(source.getOrientation());
				}
				if (d.containsKey(TiC.PROPERTY_DECODE_RETRIES)) {
					source.setDecodeRetries(TiConvert.toInt(d.get(TiC.PROPERTY_DECODE_RETRIES), TiDrawableReference.DEFAULT_DECODE_RETRIES));
				}
				setImageSource(source);
				firedLoad = false;
				setImageInternal();
			}
		} else {
			if (!d.containsKey(TiC.PROPERTY_IMAGES)) {
//				getProxy().setProperty(TiC.PROPERTY_IMAGE, null);
				if (defaultImageSource != null) {
					setDefaultImage();
				}
			}
		}
		
		if (d.containsKey(TiC.PROPERTY_ANIMATED_IMAGES)) {
			setAnimatedImageSource(d.get(TiC.PROPERTY_ANIMATED_IMAGES));
			setAnimatedImages();
		}
		
		if (d.containsKey(TiC.PROPERTY_IMAGE_MASK)) {
			setImageMask(d.get(TiC.PROPERTY_IMAGE_MASK));
		}
		// If height and width is not defined, disable scaling for scrollview since an image
		// can extend beyond the screensize in scrollview.
		if (proxy.getParent() instanceof ScrollViewProxy && !heightDefined && !widthDefined) {
			view.setEnableScale(false);
		}
		view.setConfigured(true);
	}

	@Override
	public void propertyChanged(String key, Object oldValue, Object newValue, KrollProxy proxy)
	{
		TiImageView view = getView();
		if (view == null) {
			return;
		}

		if (key.equals(TiC.PROPERTY_ENABLE_ZOOM_CONTROLS)) {
			view.setEnableZoomControls(TiConvert.toBoolean(newValue));
		} else if(key.equals(TiC.PROPERTY_ANIMATION_DURATION)) {
//			view.setAnimationDuration( TiConvert.toInt(newValue));
		} else if(key.equals(TiC.PROPERTY_LOCAL_LOAD_SYNC)) {
			localLoadSync = TiConvert.toBoolean(newValue);
		} else if(key.equals(TiC.PROPERTY_SCALE_TYPE)) {
			setWantedScaleType(TiConvert.toInt(newValue));
		} else if (key.equals(TiC.PROPERTY_IMAGE_MASK)) {
			setImageMask(newValue);
		} else if (key.equals(TiC.PROPERTY_IMAGE)) {
			if (oldValue != null || newValue != null) {
				if (animator != null) {
					stop();
				}
				setImageSource(newValue);
				firedLoad = false;
				setImageInternal();
			}
		} else if (key.equals(TiC.PROPERTY_IMAGES)) {
			if (newValue instanceof Object[]) {
				if (oldValue == null || !oldValue.equals(newValue)) {
					setImageSource(newValue);
					setImages();
				}
			}
		} else if (key.equals(TiC.PROPERTY_ANIMATED_IMAGES)) {
			if (oldValue == null || !oldValue.equals(newValue)) {
				setAnimatedImageSource(newValue);
				setAnimatedImages();
			}
			
		} else if (key.equals(TiC.PROPERTY_TRANSITION)) {
			if (newValue instanceof HashMap) {
				transitionDict = (HashMap) newValue;
			}
			else {
				transitionDict = null;
			}
		} else if(key.equals(TiC.PROPERTY_DURATION)) {
			duration = TiConvert.toInt(newValue, DEFAULT_DURATION);
			if (duration < MIN_DURATION) {
				duration = MIN_DURATION;
			}
			if (animDrawable != null) {
				animDrawable.setDuration(duration);
			}
		} else if(key.equals(TiC.PROPERTY_REVERSE)) {
			reverse = TiConvert.toBoolean(newValue, false);
			if (animDrawable != null) {
				animDrawable.setReverse(reverse);
			}
		} else if(key.equals(TiC.PROPERTY_AUTOREVERSE)) {
			autoreverse = TiConvert.toBoolean(newValue, false);
			if (animDrawable != null) {
				animDrawable.setAutoreverse(autoreverse);
			}
		} else if(key.equals(TiC.PROPERTY_REPEAT_COUNT)) {
				repeatCount = TiConvert.toInt(newValue, INFINITE);
		} else {
			super.propertyChanged(key, oldValue, newValue, proxy);
			if (key.equals(TiC.PROPERTY_WIDTH) || key.equals(TiC.PROPERTY_LEFT) || key.equals(TiC.PROPERTY_RIGHT)) {
				view.setWidthDefined(!(layoutParams.autoSizeWidth() && (layoutParams.optionLeft == null || layoutParams.optionRight == null)));
			} else if (key.equals(TiC.PROPERTY_HEIGHT) || key.equals(TiC.PROPERTY_TOP) || key.equals(TiC.PROPERTY_BOTTOM)) {
				view.setHeightDefined(!(layoutParams.autoSizeHeight() && (layoutParams.optionTop == null || layoutParams.optionBottom == null)));
			}
		}
	}
	
	private void setImageMask(Object mask){
		TiImageView view = getView();
		if (view == null) return;
		Bitmap bitmap = null;
		if (mask instanceof TiBlob) {
			bitmap = ((TiBlob)mask).getImage();
		}
		else {
			BitmapDrawable drawable = ((BitmapDrawable) TiUIHelper.buildImageDrawable(TiConvert.toString(mask), false, proxy));
			if (drawable != null) {
				bitmap = drawable.getBitmap();
			}
		}
		
		
		disableHWAcceleration();
		view.setMask(bitmap);
	}

	public void onDestroy(Activity activity)
	{
	}

	public void onPause(Activity activity)
	{
		pause();
	}

	public void onResume(Activity activity)
	{
		resume();
	}

	public void onStart(Activity activity)
	{
	}

	public void onStop(Activity activity)
	{
		stop();
	}

	public boolean isAnimating()
	{
		return animating.get() && !paused;
	}
	
	public boolean isPaused()
	{
		return paused;
	}

	public boolean isReverse()
	{
		return reverse;
	}

	public TiBlob toBlob()
	{
		TiImageView view = getView();
		if (view != null) {
			Drawable drawable = view.getImageDrawable();
			if (drawable == null && imageSources != null && imageSources.size() == 1) {
				try {
					drawable = imageSources.get(0).getDrawable();
				} catch (FileNotFoundException e) {
				}
			}
			if (drawable != null) {
				Bitmap bitmap = null;
				if (drawable instanceof BitmapDrawable) {
					bitmap = ((BitmapDrawable) drawable).getBitmap();
					
				}
				else if (drawable instanceof SVGDrawable) {
					bitmap =  ((SVGDrawable) drawable).getBitmap();
				}
				return bitmap == null ? null : TiBlob.blobFromImage(bitmap);
			}
		}

		return null;
	}


	@Override
	public void release()
	{
		super.release();
		if (loader != null) {
			synchronized (loader) {
				loader.notify();
			}
			loader = null;
		}
		animating.set(false);
		isStopping.set(true);
		synchronized(releasedLock) {
			if (imageSources != null) {
				for (TiDrawableReference imageref : imageSources) {
					int hash = imageref.hashCode();
					mMemoryCache.remove(hash); //Release the cached images
				}
				imageSources.clear();
				imageSources = null;
			}
		}
		
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		defaultImageSource = null;
	}
	
	private void setWantedScaleType(int type){
		TiImageView view = getView();
		if (view == null) return;
		ScaleType result = ScaleType.FIT_XY;
		switch (type)
		{
			case 1:
				result = ScaleType.FIT_CENTER;
				break;
			case 2:
				result = ScaleType.CENTER_CROP;
				break;
			case 3:
				result = ScaleType.CENTER;
				break;
			case 4:
				result = ScaleType.FIT_START;
				break;
			case 5:
				result = ScaleType.FIT_END;
				break;
			case 0:
			default:
				break;
		}
		view.setWantedScaleType(result);
	}
}
