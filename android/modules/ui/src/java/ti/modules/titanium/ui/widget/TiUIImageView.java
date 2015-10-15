/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.TiMessenger;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiLifecycle.OnLifecycleEvent;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiImageHelper.TiDrawableTarget;
import org.appcelerator.titanium.view.TiDrawableReference;
import org.appcelerator.titanium.view.TiUINonViewGroupView;

import com.squareup.picasso.Cache;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.trevorpage.tpsvg.SVGDrawable;

import ti.modules.titanium.ui.ImageViewProxy;
import ti.modules.titanium.ui.ScrollViewProxy;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View.MeasureSpec;
import android.widget.ImageView.ScaleType;
import android.os.Bundle;
public class TiUIImageView extends TiUINonViewGroupView implements
        OnLifecycleEvent, Handler.Callback, TiDrawableTarget {
    private static final String TAG = "TiUIImageView";
    private static final int FRAME_QUEUE_SIZE = 5;
    public static final int INFINITE = 0;
    public static final int MIN_DURATION = 30;
    public static final int DEFAULT_DURATION = 200;
    
    private TiDrawableReference loadingRef = null;
    private TiDrawableReference currentRef = null;

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
    private boolean onlyTransitionIfRemote = false;
    private boolean firedLoad;
    private ImageViewProxy imageViewProxy;
    private int duration;
    private int repeatCount = INFINITE;
    private Drawable currentImage = null;
    private HashMap filterOptions = null;
    private KrollDict bitmapInfo = null;

    private ArrayList<TiDrawableReference> imageSources;
    private ArrayList<TiDrawableReference> animatedImageSources;
    private TiDrawableReference defaultImageSource;
//    private TiDownloadListener downloadListener;
//    private TiLoadImageListener loadImageListener;
    private Object releasedLock = new Object();

    private Handler mainHandler = new Handler(Looper.getMainLooper(), this);
    private static final int SET_IMAGE = 10001;
    private static final int START = 10002;
    private static final int STOP = 10003;
    private static final int SET_DRAWABLE = 10004;
    private static final int RESUME = 10005;
    private static final int SET_PROGRESS = 10006;
    private static final int SET_INDEX = 10007;

    private HashMap transitionDict = null;
    
    private class FilterAndSetTask extends AsyncTask<Bitmap, Void, Bitmap> {
        private final boolean shouldTransition;
        private final TiDrawableReference imageRef;
        
        FilterAndSetTask(final TiDrawableReference imageRef, final boolean shouldTransition) { 
            this.shouldTransition = shouldTransition;
            this.imageRef = imageRef;
       }
        @Override
        protected Bitmap doInBackground(Bitmap... params) {
            Bitmap bitmap = params[0];
            Pair<Bitmap, KrollDict> result  = TiImageHelper.imageFiltered(bitmap, filterOptions, true);
            if (result != null) {
                bitmapInfo = result.second;
                return result.first;
            }
            return bitmap;
        }
        
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Context context = getContext();
            if (context != null && currentRef == this.imageRef) {
                handleSetDrawable(new BitmapDrawable(context.getResources(), bitmap), shouldTransition);
                fireLoad(TiC.PROPERTY_IMAGE, bitmap);
            }
            
        }
    }    
    
    private static final ArrayList<String> KEY_SEQUENCE;
    static{
      ArrayList<String> tmp = new ArrayList<String>();
      tmp.add(TiC.PROPERTY_FILTER_OPTIONS);
      KEY_SEQUENCE = tmp;
    }
    @Override
    protected ArrayList<String> keySequence() {
        return KEY_SEQUENCE;
    }

    public TiUIImageView(final TiViewProxy proxy) {
        super(proxy);
        imageViewProxy = (ImageViewProxy) proxy;
        Log.d(TAG, "Creating an ImageView", Log.DEBUG_MODE);

        TiImageView view = new TiImageView(proxy.getActivity(), proxy) {
            @Override
            protected void onLayout(boolean changed, int left, int top,
                    int right, int bottom) {
                super.onLayout(changed, left, top, right, bottom);
                TiUIHelper.firePostLayoutEvent(TiUIImageView.this);
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent event) {
                if (TiUIImageView.this.touchPassThrough(childrenHolder, event))
                    return false;
                if (touchPassThrough == true)
                    return false;
                return super.dispatchTouchEvent(event);
            }

            @Override
            public void dispatchSetPressed(boolean pressed) {
                if (propagateSetPressed(this, pressed)) {
                    super.dispatchSetPressed(pressed);
                }
            }
        };
        setImage(null, false); // this actually creates a drawable which will allow
                        // transition

        setNativeView(view);
    }

    @Override
    public void setReusing(boolean value) {
        super.setReusing(value);
        if (value) {
            TiImageView view = getView();
            if (view != null)
                view.cancelCurrentTransition();
        }
    }

    @Override
    public void setProxy(TiViewProxy proxy) {
        super.setProxy(proxy);
        imageViewProxy = (ImageViewProxy) proxy;
    }

    private TiImageView getView() {
        return (TiImageView) nativeView;
    }

    public boolean handleMessage(Message msg) {
        switch (msg.what) {

        case SET_IMAGE: {
            AsyncResult result = (AsyncResult) msg.obj;
            handleSetImage((Bitmap) result.getArg());
            result.setResult(null);
            return true;
        }
        case SET_DRAWABLE: {
            handleSetDrawable((Drawable) msg.obj, msg.arg1 == 1);
            return true;
        }
        case START:
            handleStart();
            return true;
        case STOP:
            handleStop();
            return true;
        case RESUME:
            handleResume();
            return true;
        case SET_INDEX: {
            AsyncResult result = (AsyncResult) msg.obj;
            handleSetCurrentIndex(((Number) result.getArg()).intValue());
            result.setResult(null);
            return true;
        }
        case SET_PROGRESS: {
            AsyncResult result = (AsyncResult) msg.obj;
            handleSetProgress(((Number) result.getArg()).floatValue());
            result.setResult(null);
            return true;
        }
        default:
            return false;

        }
    }

    private void setImage(final Bitmap bitmap, boolean shouldTransition) {
        setDrawable((bitmap != null) ? new BitmapDrawable(getContext()
                .getResources(), bitmap) : null, shouldTransition);
    }

    private void setDrawable(final Drawable drawable, boolean shouldTransition) {
        if (getDrawable() == drawable)
            return;
        if (!TiApplication.isUIThread()) {
            mainHandler.obtainMessage(SET_DRAWABLE, shouldTransition?1:0, 0, drawable).sendToTarget();
        } else {
            handleSetDrawable(drawable, shouldTransition);
        }
    }
    
    private void setDrawable(final Drawable drawable) {
        setDrawable(drawable, true);
    }

    private Drawable getDrawable() {
        TiImageView view = getView();
        if (view != null)
            return view.getImageDrawable();
        else
            return null;
    }

    private void handleSetImage(final Bitmap bitmap) {
        TiImageView view = getView();
        if (view != null) {
            view.setImageBitmapWithTransition(bitmap, transitionDict);
            boolean widthDefined = view.getWidthDefined();
            boolean heightDefined = view.getHeightDefined();
            if ((!widthDefined || !heightDefined)) {
                // force re-calculating the layout dimension and the redraw of
                // the view
                // This is a trick to prevent getMeasuredWidth and Height to be
                // 0
                view.measure(MeasureSpec.makeMeasureSpec(0,
                        widthDefined ? MeasureSpec.EXACTLY
                                : MeasureSpec.UNSPECIFIED), MeasureSpec
                        .makeMeasureSpec(0, heightDefined ? MeasureSpec.EXACTLY
                                : MeasureSpec.UNSPECIFIED));
                view.requestLayout();
            }
        }
    }

    private void handleSetDrawable(final Drawable drawable, boolean shouldTransition) {
        if (drawable != animDrawable) {
            currentImage = drawable;
            if (animDrawable != null) {
                animDrawable.stop();
            }
        }
        TiImageView view = getView();
        if (view != null) {
            view.setImageDrawableWithTransition(drawable, shouldTransition?transitionDict:null);
            boolean widthDefined = view.getWidthDefined();
            boolean heightDefined = view.getHeightDefined();
            if ((!widthDefined || !heightDefined)) {
                // force re-calculating the layout dimension and the redraw of
                // the view
                // This is a trick to prevent getMeasuredWidth and Height to be
                // 0
                // view.measure(MeasureSpec.makeMeasureSpec(
                // widthDefined?view.getMeasuredWidth():0,
                // widthDefined?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED),
                // MeasureSpec.makeMeasureSpec(heightDefined?view.getMeasuredHeight():0,
                // heightDefined?MeasureSpec.EXACTLY:MeasureSpec.UNSPECIFIED));
                view.requestLayout();
            }
        }
    }

    private class BitmapWithIndex {
        public BitmapWithIndex(Bitmap b, int i) {
            this.bitmap = b;
            this.index = i;
        }

        public Bitmap bitmap;
        public int index;
    }

    private class Loader implements Runnable {
        private ArrayBlockingQueue<BitmapWithIndex> bitmapQueue;
		private LinkedList<String> hashTable;
        private int waitTime = 0;
        private int sleepTime = 50; // ms
        private int repeatIndex = 0;

        public Loader() {
            bitmapQueue = new ArrayBlockingQueue<BitmapWithIndex>(
                    FRAME_QUEUE_SIZE);
			hashTable = new LinkedList<String>();
        }

        private boolean isRepeating() {
			if (repeatCount <= 0) {
				return true;
			}
			return repeatIndex < repeatCount;
        }

        private int getStart() {
            if (imageSources == null) {
                return 0;
            }
            if (reverse) {
                return imageSources.size() - 1;
            }
            return 0;

        }

        private boolean isNotFinalFrame(int frame) {
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

        private int getCounter() {
            if (reverse) {
                return -1;
            }
            return 1;
        }

        public void run() {
            if (getProxy() == null) {
                Log.d(TAG,
                        "Multi-image loader exiting early because proxy has been gc'd");
                return;
            }
            repeatIndex = 0;
            isLoading.set(true);
            firedLoad = false;
			boolean shouldCache = repeatCount >= 5 ? true : false;
            topLoop: while (isRepeating()) {

                if (imageSources == null) {
                    break;
                }
                long time = System.currentTimeMillis();
                for (int j = getStart(); imageSources != null
                        && isNotFinalFrame(j); j += getCounter()) {
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
                            // In the meantime, while paused, user could have
                            // backed out, which leads
                            // to release(), which in turn leads to nullified
                            // imageSources.
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
                        TiDrawableReference imageRef = imageSources.get(j);
						Bitmap b = null;
						if (shouldCache) {
					        Cache cache = TiApplication.getImageMemoryCache();
					        b = cache.get(imageRef.getUrl());
							if (b == null) {
								Log.i(TAG, "Image isn't cached");
								b = imageRef.getBitmap(true);
				                cache.set(imageRef.getUrl(), b);
								hashTable.add(imageRef.getUrl());
							}
						} else {
							b = imageRef.getBitmap(true);
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
                                Log.w(TAG,
                                        "Interrupted while adding Bitmap into bitmapQueue");
                                break;
                            }
                        }
                    }
                    repeatIndex++;
                }

                Log.d(TAG,
                        "TIME TO LOAD FRAMES: "
                                + (System.currentTimeMillis() - time) + "ms",
                        Log.DEBUG_MODE);

            }
            isLoading.set(false);
			//clean out the cache after animation
//			while (!hashTable.isEmpty()) {
//				mMemoryCache.remove(hashTable.pop());
//			}
        }

        public ArrayBlockingQueue<BitmapWithIndex> getBitmapQueue() {
            return bitmapQueue;
        }
    }

    private void setImages() {
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
            Log.d(TAG, "STARTING LOADER THREAD " + loaderThread + " for "
                    + this, Log.DEBUG_MODE);
            loaderThread.start();
        }

    }
    
    private Drawable getDrawableFromLocal(final TiDrawableReference imageref,
            final Cache cache) {
        Bitmap bitmap = cache.get(imageref.getUrl());
        Drawable drawable = null;
        if (bitmap == null) {
            drawable = imageref.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                bitmap = ((BitmapDrawable) drawable).getBitmap();
                cache.set(imageref.getUrl(), bitmap);
            }
        } else {
            drawable = new BitmapDrawable(getContext().getResources(), bitmap);
        }
        return drawable;
    }

    private void setAnimatedImages() {
        if (animatedImageSources == null || animatedImageSources.size() == 0) {
            handleSetDrawable(null, false);
            fireError("Missing Images", null);
            return;
        }
        animDrawable = new TiAnimationDrawable();
        animDrawable.setReverse(reverse);
        animDrawable.setAutoreverse(autoreverse);
        Cache cache = TiApplication.getImageMemoryCache();
        for (int i = 0; i < animatedImageSources.size(); i++) {
            TiDrawableReference imageref = animatedImageSources.get(i);
            if (!imageref.isNetworkUrl()) {
                Drawable drawable = getDrawableFromLocal(imageref, cache);

                if (drawable != null) {
                    animDrawable.addFrame(drawable, duration);
                } else {
                    Log.e(TAG,
                            "Could not find image for url " + imageref.getUrl());
                }
            }
        }
        currentIndex = reverse ? animDrawable.getNumberOfFrames() - 1 : 0;
        animDrawable.selectDrawable(currentIndex);
        if (currentImage == null)
            setDrawable(animDrawable, true);
    }

    public boolean fireImageEvent(String eventName, KrollDict data) {
        return fireEvent(eventName, data, false, false);
    }

    private void fireLoad(String state) {
        fireLoad(state, null);
    }

    private void fireLoad(String state, Bitmap bitmap) {
        if (hasListeners(TiC.EVENT_LOAD)) {
            KrollDict data = new KrollDict();
            if (bitmap != null) {
                data.put("image", TiBlob.blobFromObject(bitmap));
            }
            data.put(TiC.EVENT_PROPERTY_STATE, state);
            if (bitmapInfo != null) {
                data = KrollDict.merge(data, bitmapInfo);
            }
            fireImageEvent(TiC.EVENT_LOAD, data);
        }
    }

    private void fireStart() {
        if (hasListeners(TiC.EVENT_START)) {
            fireImageEvent(TiC.EVENT_START, null);
        }
    }

    private void fireChange(int index) {
        if (hasListeners(TiC.EVENT_CHANGE)) {
            KrollDict data = new KrollDict();
            data.put(TiC.EVENT_PROPERTY_INDEX, index);
            fireImageEvent(TiC.EVENT_CHANGE, data);
        }
    }

    private void fireStop() {
        if (hasListeners(TiC.EVENT_LOAD)) {
            fireImageEvent(TiC.EVENT_LOAD, null);
        }
        
    }

    private void fireError(String message, String imageUrl) {
        if (hasListeners(TiC.EVENT_ERROR)) {
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

    private class Animator extends TimerTask {
        private Loader loader;

        public Animator(Loader loader) {
            this.loader = loader;
        }

        public void run() {
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

                ArrayBlockingQueue<BitmapWithIndex> bitmapQueue = loader.getBitmapQueue();
                
                //Fire stop event when animation finishes
                if (!isLoading.get() && bitmapQueue.isEmpty()) {
                    fireStop();
                }

                BitmapWithIndex b = bitmapQueue.take();
                Log.d(TAG, "set image: " + b.index, Log.DEBUG_MODE);
                setImage(b.bitmap, false);
                fireChange(b.index);

                // When the animation is paused, the timer will pause in the
                // middle of a period.
                // When the animation resumes, the timer resumes from where it
                // left off. As a result, it will look like
                // one frame is left out when resumed (TIMOB-10207).
                // To avoid this, we force the thread to wait for one period on
                // resume.
                if (waitOnResume) {
                    Thread.sleep(duration);
                    waitOnResume = false;
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Loader interrupted");
            }
        }
    }
    
    public int getCurrentIndex() {
        int total = (animDrawable != null) ? animDrawable.getNumberOfFrames() : 1;
        int result = (animDrawable != null) ? animDrawable.getCurrentFrame() : currentIndex;
        
        return reverse ? (total - result) : result;
    }
    
    public float getProgress() {
        int total = (animDrawable != null) ? animDrawable.getNumberOfFrames() : 1;
        float result = ((animDrawable != null) ? animDrawable.getCurrentFrame() : currentIndex) / (float)total;
         
        return reverse ? (1 - result) : result;
    }
    
    public void handleSetCurrentIndex(int index) {
        if (animDrawable != null) {
            if (getDrawable() != animDrawable) {
                setDrawable(animDrawable, true);
            }
            animDrawable.setFrame(index);
        }
    }
    
    public void setCurrentIndex(int index) {
        if (!TiApplication.isUIThread()) {
            TiMessenger.sendBlockingMainMessage(
                    mainHandler.obtainMessage(SET_INDEX), Integer.valueOf(index));
        } else {
            handleSetCurrentIndex(index);
        }
    }
    
    public void handleSetProgress(float progress) {
        if (animDrawable != null) {
            if (getDrawable() != animDrawable) {
                setDrawable(animDrawable);
            }
            animDrawable.setProgress(progress);
        }
    }
    
    public void setProgress(float progress) {
        if (!TiApplication.isUIThread()) {
            TiMessenger.sendBlockingMainMessage(
                    mainHandler.obtainMessage(SET_PROGRESS), Float.valueOf(progress));
        } else {
            handleSetProgress(progress);
        }
    }

    public void start() {
        currentIndex = (animDrawable != null) ? (reverse ? animDrawable
                .getNumberOfFrames() - 1 : 0) : 0;
        if (!TiApplication.isUIThread()) {
            Message message = mainHandler.obtainMessage(START);
            message.sendToTarget();
        } else {
            handleStart();
        }
    }

    public void handleStart() {
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
                Log.d(TAG, "STARTING LOADER THREAD " + loaderThread + " for "
                        + this, Log.DEBUG_MODE);
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

    public void pause() {
        paused = true;
        if (animDrawable != null) {
            animDrawable.pause();
            return;
        }
    }

    public void handleResume() {
        if (animDrawable != null) {
            if (getDrawable() != animDrawable) {
                setDrawable(animDrawable);
            }
            animDrawable.resume();
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

    public void resume() {
        paused = false;
        if (!TiApplication.isUIThread()) {
            Message message = mainHandler.obtainMessage(RESUME);
            message.sendToTarget();
        } else {
            handleResume();
        }
    }

    public void stop() {
        if (!TiApplication.isUIThread()) {
            Message message = mainHandler.obtainMessage(STOP);
            message.sendToTarget();
        } else {
            handleStop();
        }
    }

    public void handleStop() {
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

    private void setImageSource(Object object) {
        imageSources = new ArrayList<TiDrawableReference>();
        if (object instanceof Object[]) {
            for (Object o : (Object[]) object) {
                imageSources.add(makeImageSource(o));
            }
        } else {
            imageSources.add(makeImageSource(object));
        }
    }

    private void setAnimatedImageSource(Object object) {
        animatedImageSources = new ArrayList<TiDrawableReference>();
        if (object instanceof Object[]) {
            for (Object o : (Object[]) object) {
                animatedImageSources.add(makeImageSource(o));
            }
        } else {
            animatedImageSources.add(makeImageSource(object));
        }
    }

    private void setImageSource(TiDrawableReference source) {
        imageSources = new ArrayList<TiDrawableReference>();
        imageSources.add(source);
    }

    private TiDrawableReference makeImageSource(Object object) {
        TiDrawableReference source = TiDrawableReference.fromObject(proxy, object);
        // Check for orientation and decodeRetries only if an image is
        // specified
        boolean autoRotate = TiConvert.toBoolean(proxy.getProperty(TiC.PROPERTY_AUTOROTATE), false);
        if (autoRotate) {
            getView().setOrientation(source.getOrientation());
        }
        int decodeRetries = TiConvert.toInt(proxy.getProperty(TiC.PROPERTY_DECODE_RETRIES), TiDrawableReference.DEFAULT_DECODE_RETRIES);
        source.setDecodeRetries(decodeRetries);
        return source;
    }


    private void setImageInternal() {

        if (imageSources == null || imageSources.size() == 0
                || imageSources.get(0) == null
                || imageSources.get(0).isTypeNull()) {
            // here we can transition to the default image
            setDefaultImage(proxy.viewInitialised());
            currentRef = null;
            loadingRef = null;
            return;
        }
        
        if (reusing) {
            setDefaultImage(false);
        }

        if (imageSources.size() == 1) {
            TiDrawableReference imageref = imageSources.get(0);
            currentRef = loadingRef = imageref;
            TiImageHelper.downloadDrawable(imageViewProxy, imageref, localLoadSync, this);
        } else {
            setImages();
        }
    }

    private void setDefaultImage(final boolean withTransition) {
        TiImageView view = getView();

        if (view == null 
//                || (!withTransition && proxy.viewInitialised() && transitionDict != null)
                ) {
            return;
        }
        
        if (defaultImageSource == null) {
            setDrawable(null, withTransition);
        }
        else {
            setDrawable(defaultImageSource.getDrawable(), withTransition);
        }
    }
    
    
    private boolean isArrayNullOrEmpty(ArrayList<TiDrawableReference> array) {
        return (array == null || array.size() == 0
                || array.get(0) == null
                || array.get(0).isTypeNull());
    }
    
    @Override
    protected void didProcessProperties() {
        if ((mProcessUpdateFlags & TIFLAG_NEEDS_LAYOUT) != 0) {
            TiImageView view = getView();
            if (view != null) {
                view.setWidthDefined(!(layoutParams.autoSizeWidth() && (layoutParams.optionLeft == null || layoutParams.optionRight == null)));
                view.setHeightDefined(!(layoutParams.autoSizeHeight() && (layoutParams.optionTop == null || layoutParams.optionBottom == null)));
    
                // If height and width is not defined, disable scaling for scrollview
                // since an image
                // can extend beyond the screensize in scrollview.
                if (proxy.getParent() instanceof ScrollViewProxy && !view.getHeightDefined()
                        && !view.getWidthDefined()) {
                    view.setEnableScale(false);
                }
            }
        }
        if (isArrayNullOrEmpty(imageSources) && 
                isArrayNullOrEmpty(animatedImageSources)) {
            setDefaultImage(false);
        }
        super.didProcessProperties();
    }
    
    @Override
    public void didRealize() {
        super.didRealize();
        boolean animating = TiConvert.toBoolean(proxy.getProperty("animating"), false);
        if (animating) {
            start();
        }
    }
    

    @Override
    public void processProperties(KrollDict d) {
        
        super.processProperties(d);
        TiImageView view = getView();
        if (view != null) {
            view.setConfigured(true);
        }
    }

    @Override
    public void propertySet(String key, Object newValue, Object oldValue,
            boolean changedProperty) {
        TiImageView view = getView();
        if (view == null) {
            return;
        }
        switch (key) {
        case TiC.PROPERTY_ENABLE_ZOOM_CONTROLS:
            view.setEnableZoomControls(TiConvert.toBoolean(newValue));
            break;
        case TiC.PROPERTY_ANIMATION_DURATION:
            // view.setAnimationDuration( TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_LOCAL_LOAD_SYNC:
            localLoadSync = TiConvert.toBoolean(newValue);
            break;
        case TiC.PROPERTY_SCALE_TYPE:
            setWantedScaleType(TiConvert.toInt(newValue));
            break;
        case TiC.PROPERTY_IMAGE_MASK:
            setImageMask(newValue);
            break;
        case TiC.PROPERTY_ONLY_TRANSITION_IF_REMOTE:
            onlyTransitionIfRemote = TiConvert.toBoolean(newValue);
            break;
        case TiC.PROPERTY_FILTER_OPTIONS:
            filterOptions = (HashMap) newValue;
            if (currentImage != null && currentImage instanceof BitmapDrawable) {
                if (filterOptions != null) {
                    (new FilterAndSetTask(currentRef, !onlyTransitionIfRemote))
                            .execute(((BitmapDrawable) currentImage)
                                    .getBitmap());
                } else {
                    setDrawable(currentImage, !onlyTransitionIfRemote);
                }
            }
            break;
        case TiC.PROPERTY_DEFAULT_IMAGE:
            defaultImageSource = makeImageSource(newValue);
            break;
        case TiC.PROPERTY_IMAGE:
            boolean changeImage = true;
            TiDrawableReference source = makeImageSource(newValue);
            if (firedLoad && imageSources != null && imageSources.size() == 1) {
                if (imageSources.get(0).equals(source)) {
                    changeImage = false;
                }
            }
            if (changeImage) {
                if (animator != null) {
                    stop();
                }
                setImageSource(source);
                firedLoad = false;
                setImageInternal();
            }
            break;
        case TiC.PROPERTY_IMAGES:
            if (newValue instanceof Object[]) {
                setImageSource(newValue);
                setImages();
            }
            break;
        case TiC.PROPERTY_ANIMATED_IMAGES:
            setAnimatedImageSource(newValue);
            setAnimatedImages();
            break;
        case TiC.PROPERTY_TRANSITION:
            if (newValue instanceof HashMap) {
                transitionDict = (HashMap) newValue;
            } else {
                transitionDict = null;
            }
            break;
        case TiC.PROPERTY_DURATION:
            duration = TiConvert.toInt(newValue, DEFAULT_DURATION);
            if (duration < MIN_DURATION) {
                duration = MIN_DURATION;
            }
            if (animDrawable != null) {
                animDrawable.setDuration(duration);
            }
            break;
        case TiC.PROPERTY_REVERSE:
            reverse = TiConvert.toBoolean(newValue, false);
            if (animDrawable != null) {
                animDrawable.setReverse(reverse);
            }
            break;
        case TiC.PROPERTY_AUTOREVERSE:
            autoreverse = TiConvert.toBoolean(newValue, false);
            if (animDrawable != null) {
                animDrawable.setAutoreverse(autoreverse);
            }
            break;
        case TiC.PROPERTY_REPEAT_COUNT:
            repeatCount = TiConvert.toInt(newValue, INFINITE);
            break;
        case TiC.PROPERTY_INDEX:
            setCurrentIndex(TiConvert.toInt(newValue, 0));
            break;
        case TiC.PROPERTY_PROGRESS:
            setProgress(TiConvert.toFloat(newValue, 0));
            break;
        default:
            super.propertySet(key, newValue, oldValue, changedProperty);
            break;
        }
    }

    private void setImageMask(Object mask) {
        TiImageView view = getView();
        if (view == null)
            return;

        boolean tileImage = proxy.getProperties().optBoolean(
                TiC.PROPERTY_BACKGROUND_REPEAT, false);
        view.setMask(TiUIHelper.buildImageDrawable(nativeView.getContext(), mask, tileImage, proxy));
    }

    public void onDestroy(Activity activity) {
    }

    public void onPause(Activity activity) {
        pause();
    }

    public void onResume(Activity activity) {
        resume();
    }

    public void onStart(Activity activity) {
    }

    public void onStop(Activity activity) {
        stop();
    }

    public boolean isAnimating() {
        return animating.get() && !paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isReverse() {
        return reverse;
    }

    public TiBlob toBlob() {
        TiImageView view = getView();
        if (view != null) {
            Drawable drawable = view.getImageDrawable();
            if (drawable == null && imageSources != null
                    && imageSources.size() == 1) {
                drawable = imageSources.get(0).getDrawable();
            }
            if (drawable != null) {
                Bitmap bitmap = null;
                if (drawable instanceof BitmapDrawable) {
                    bitmap = ((BitmapDrawable) drawable).getBitmap();

                } else if (drawable instanceof SVGDrawable) {
                    bitmap = ((SVGDrawable) drawable).getBitmap();
                }
                return bitmap == null ? null : TiBlob.blobFromObject(bitmap);
            }
        }

        return null;
    }

    @Override
    public void release() {
        super.release();
        if (loader != null) {
            synchronized (loader) {
                loader.notify();
            }
            loader = null;
        }
        animating.set(false);
        isStopping.set(true);
        TiApplication.getPicassoInstance().cancelRequest(this);
        synchronized (releasedLock) {
            if (imageSources != null) {
                imageSources.clear();
                imageSources = null;
            }
            if (animatedImageSources != null) {
                animatedImageSources.clear();
                animatedImageSources = null;
            }
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        defaultImageSource = null;
    }

    private void setWantedScaleType(int type) {
        TiImageView view = getView();
        if (view == null)
            return;
        ScaleType result = ScaleType.FIT_XY;
        switch (type) {
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

    @Override
    public void onBitmapLoaded(Bitmap bitmap, LoadedFrom from) {
        Log.d(TAG, "loadedFrom "+ from + ", " + bitmap.getWidth(), Log.DEBUG_MODE);
        if (loadingRef != currentRef) {
            return;
        }
        boolean transition = !onlyTransitionIfRemote || from == LoadedFrom.NETWORK;
        if (filterOptions != null) {
            (new FilterAndSetTask(loadingRef, transition)).execute(bitmap);
        }
        else {
            handleSetDrawable(new BitmapDrawable(getContext().getResources(),bitmap), transition);
            fireLoad(TiC.PROPERTY_IMAGE, bitmap);
        }
        loadingRef = null;
    }
    
    @Override
    public void onDrawableLoaded(Drawable drawable, LoadedFrom from) {
        Log.d(TAG, "loadedFrom "+ from, Log.DEBUG_MODE);
        if (loadingRef != currentRef) {
            return;
        }
        boolean transition = !onlyTransitionIfRemote || from == LoadedFrom.NETWORK;
        Bitmap bitmap = null; 
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        if (filterOptions != null && bitmap != null) {
            (new FilterAndSetTask(loadingRef, transition)).execute(bitmap);
        }
        else {
            handleSetDrawable(drawable, transition);
            fireLoad(TiC.PROPERTY_IMAGE, bitmap);
        }
        loadingRef = null;
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        if (loadingRef != null) {
            fireError("Download Failed", loadingRef.getUrl());
            loadingRef = null;
        }        
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {
    }

    @Override
    public void onCreate(Activity activity, Bundle savedInstanceState) {
    }
}
