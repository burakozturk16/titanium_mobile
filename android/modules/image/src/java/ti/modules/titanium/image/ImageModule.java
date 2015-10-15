package ti.modules.titanium.image;

import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.ContextSpecific;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiImageHelper.FilterType;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.TiMessenger;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;
import android.os.AsyncTask;
import android.os.Message;

@SuppressWarnings("rawtypes")
@Kroll.module
@ContextSpecific
public class ImageModule extends KrollModule {
    private static final String TAG = "ImageModule";

    private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

    private static final int MSG_GETVIEWIMAGE = MSG_FIRST_ID + 100;

    @Kroll.constant
    public static final int FILTER_GAUSSIAN_BLUR = FilterType.kFilterGaussianBlur
            .ordinal();
    @Kroll.constant
    public static final int FILTER_BOX_BLUR = FilterType.kFilterBoxBlur
            .ordinal();
    @Kroll.constant
    public static final int FILTER_IOS_BLUR = FilterType.kFilteriOSBlur
            .ordinal();

    // This handler callback is tied to the UI thread.
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
        case MSG_GETVIEWIMAGE: {
            AsyncResult result = (AsyncResult) msg.obj;
            result.setResult(TiUIHelper.viewToBitmap(null,
                    (View) result.getArg()));
            return true;
        }
        }
        return super.handleMessage(msg);
    }

    public ImageModule() {
        super();
    }

    @Kroll.onAppCreate
    public static void onAppCreate(TiApplication app) {
        Log.d(TAG, "inside onAppCreate");
        // mGPUImage = new GPUImage(app.getBaseContext());
        // put module init code that needs to run when the application is
        // created
    }

    private String getPathToApplicationAsset(String assetName) {
        // The url for an application asset can be created by resolving the
        // specified
        // path with the proxy context. This locates a resource relative to the
        // application resources folder

        String result = resolveUrl(null, assetName);

        return result;
    }

    private class FilterBitmapTask extends AsyncTask<Object, Void, TiBlob> {
        KrollFunction callback;
        KrollProxy proxy;

        @Override
        protected TiBlob doInBackground(Object... params) {
            proxy = (KrollProxy) params[0];
            Bitmap bitmap = (Bitmap) params[1];
            HashMap options = (HashMap) params[2];
            callback = (KrollFunction) params[3];
            return getFilteredImage(bitmap, options);
        }

        /**
         * Always invoked on UI thread.
         */
        @Override
        protected void onPostExecute(TiBlob blobImage) {
            KrollDict result = new KrollDict();
            if (blobImage != null) {
                result.put("image", blobImage);
            }
            this.callback.callAsync(this.proxy.getKrollObject(),
                    new Object[] { result });
        }
    }
    
    private class FilterDrawableTask extends AsyncTask<Object, Void, TiBlob> {
        KrollFunction callback;
        KrollProxy proxy;

        @Override
        protected TiBlob doInBackground(Object... params) {
            proxy = (KrollProxy) params[0];
            Drawable drawable = (Drawable) params[1];
            HashMap options = (HashMap) params[2];
            callback = (KrollFunction) params[3];
            return getFilteredImage(drawable, options);
        }

        /**
         * Always invoked on UI thread.
         */
        @Override
        protected void onPostExecute(TiBlob blobImage) {
            KrollDict result = new KrollDict();
            if (blobImage != null) {
                result.put("image", blobImage);
            }
            this.callback.callAsync(this.proxy.getKrollObject(),
                    new Object[] { result });
        }
    }

    @Kroll.method
    public TiBlob getFilteredImage(Object image,
            @Kroll.argument(optional = true) HashMap options) {
        Drawable drawable = TiUIHelper.buildImageDrawable(getActivity(), image, false, this);
        if (drawable == null) {
            return null;
        }
//        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        
        Pair<Drawable, KrollDict> result = null;
        if (options != null) {
            if (options.containsKey("callback")) {
                KrollFunction callback = (KrollFunction) options
                        .get("callback");
                options.remove("callback");
                if (callback != null) {
                    (new FilterDrawableTask()).execute(this, drawable, options,
                            callback);
                    return null;
                }
            }
            result = TiImageHelper.drawableFiltered(drawable, options, true);
        }

        if (result != null) {
            TiBlob blob = TiBlob.blobFromObject(result.first);
            blob.addInfo(result.second);
            return blob;
        }
        return null;
    }

    private class FilterViewTask extends AsyncTask<Object, Void, TiBlob> {
        KrollFunction callback;
        KrollProxy proxy;

        @Override
        protected TiBlob doInBackground(Object... params) {
            proxy = (KrollProxy) params[0];
            TiViewProxy viewProxy = (TiViewProxy) params[1];
            HashMap options = (HashMap) params[2];
            TiUIView view = viewProxy.getOrCreateView();
            Bitmap bitmap = null;
            try {
                bitmap = TiUIHelper.viewToBitmap(viewProxy.getProperties(),
                    view.getOuterView());
                callback = (KrollFunction) params[3];
            } catch (Exception e) {
                bitmap = null;
            }
            return getFilteredImage(bitmap, options);
        }

        /**
         * Always invoked on UI thread.
         */
        @Override
        protected void onPostExecute(TiBlob blob) {
            // KrollDict result = new KrollDict();
            // if (blob != null) {
            // result.put("image", blob);
            // }
            // this.callback.callAsync(this.proxy.getKrollObject(), new Object[]
            // { result });
        }
    }

    private class FilterScreenShotTask extends AsyncTask<Object, Void, TiBlob> {
        @Override
        protected TiBlob doInBackground(Object... params) {
            HashMap options = (HashMap) params[0];
            View view = TiApplication.getAppCurrentActivity().getWindow()
                    .getDecorView();
            Bitmap bitmap = null;
            try {
                bitmap = TiUIHelper.viewToBitmap(null, view);
                Rect statusBar = new Rect();
                view.getWindowVisibleDisplayFrame(statusBar);
                bitmap = Bitmap.createBitmap(bitmap, 0, statusBar.top,
                        bitmap.getWidth(), bitmap.getHeight() - statusBar.top,
                        null, true);
            } catch (Exception e) {
                bitmap = null;
            }
            return getFilteredImage(bitmap, options);
        }
    }

    @Kroll.method
    public TiBlob getFilteredViewToImage(TiViewProxy viewProxy,
            @Kroll.argument(optional = true) HashMap options) {

        if (options != null) {
            if (options.containsKey("callback")) {
                KrollFunction callback = (KrollFunction) options
                        .get("callback");
                if (callback != null) {
                    (new FilterViewTask()).execute(this, viewProxy, options,
                            callback);
                    return null;
                }
            }
        }

        TiUIView view = viewProxy.getOrCreateView();
        if (view == null) {
            return null;
        }

        Bitmap bitmap = null;

        try {
            if (TiApplication.isUIThread()) {
                bitmap = TiUIHelper.viewToBitmap(viewProxy.getProperties(),
                        view.getOuterView());
            } else {
                bitmap = (Bitmap) TiMessenger.sendBlockingMainMessage(
                        getMainHandler().obtainMessage(MSG_GETVIEWIMAGE),
                        view.getOuterView());
            }
        } catch (Exception e) {
            bitmap = null;
        }
        return getFilteredImage(bitmap, options);
    }

    @Kroll.method
    public TiBlob getFilteredScreenshot(HashMap options) {
        if (options != null) {
            if (options.containsKey("callback")) {
                KrollFunction callback = (KrollFunction) options
                        .get("callback");
                if (callback != null) {
                    (new FilterScreenShotTask()).execute(options);
                    return null;
                }
            }
        }

        View view = TiApplication.getAppCurrentActivity().getWindow()
                .getDecorView();
        if (view == null) {
            return null;
        }

        Bitmap bitmap = null;
        try {
            if (TiApplication.isUIThread()) {
                bitmap = TiUIHelper.viewToBitmap(null, view);
            } else {
                bitmap = (Bitmap) TiMessenger.sendBlockingMainMessage(
                        getMainHandler().obtainMessage(MSG_GETVIEWIMAGE),
                        view);
            }
            Rect statusBar = new Rect();
            view.getWindowVisibleDisplayFrame(statusBar);
            bitmap = Bitmap.createBitmap(bitmap, 0, statusBar.top,
                    bitmap.getWidth(), bitmap.getHeight() - statusBar.top,
                    null, true);
        } catch (Exception e) {
            bitmap = null;
        }
        return getFilteredImage(bitmap, options);
    }
}
