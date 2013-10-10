package ti.modules.titanium.image;

import java.io.IOException;
import java.util.HashMap;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.ContextSpecific;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiImageHelper;
import org.appcelerator.titanium.util.TiImageHelper.FilterType;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.TiMessenger;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;
import android.os.AsyncTask;
import android.os.Message;

@SuppressWarnings("rawtypes")
@Kroll.module @ContextSpecific
public class ImageModule extends KrollModule
{
	private static final String TAG = "ImageModule";

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_GETVIEWIMAGE = MSG_FIRST_ID + 100;

	@Kroll.constant public static final int FILTER_GAUSSIAN_BLUR = FilterType.kFilterGaussianBlur.ordinal();
	@Kroll.constant public static final int FILTER_BOX_BLUR = FilterType.kFilterBoxBlur.ordinal();

	//This handler callback is tied to the UI thread.
	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
			case MSG_GETVIEWIMAGE : {
				AsyncResult result = (AsyncResult) msg.obj;
				result.setResult(TiUIHelper.viewToBitmap(null, (View)result.getArg()));
				return true;
			}
		}
		return super.handleMessage(msg);
	}
	
	public ImageModule()
	{
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app)
	{
		Log.d(TAG, "inside onAppCreate");
//		mGPUImage = new GPUImage(app.getBaseContext());
		// put module init code that needs to run when the application is created
	}
	
	private String getPathToApplicationAsset(String assetName)
	{
		// The url for an application asset can be created by resolving the specified
		// path with the proxy context. This locates a resource relative to the 
		// application resources folder

		String result = resolveUrl(null, assetName);

		return result;
	}
	
	
	private class FilterBitmapTask extends AsyncTask< Object, Void, Bitmap >
	{
		KrollFunction callback;
		KrollProxy proxy;

		@Override
		protected Bitmap doInBackground(Object... params)
		{
			proxy = (KrollProxy)params[0];
			Bitmap bitmap = (Bitmap)params[1];
			FilterType filterType = FilterType.values()[((Integer)params[2]).intValue()];
			HashMap options = (HashMap)params[3];
			callback = (KrollFunction)params[4];
			return TiImageHelper.imageFiltered(bitmap, filterType, options);
		}
		/**
		 * Always invoked on UI thread.
		 */
		@Override
		protected void onPostExecute(Bitmap image)
		{
			KrollDict result = new KrollDict();
			if (image != null) {
				result.put("image", TiBlob.blobFromImage(image));
			}
			this.callback.callAsync(this.proxy.getKrollObject(), new Object[] { result });
		}
	}
	
	@Kroll.method
	public TiBlob getFilteredImage(Object image, int filterType, @Kroll.argument(optional=true) HashMap options) {
		Bitmap bitmap = null;
		if (image instanceof Bitmap) {
			bitmap = (Bitmap) image;
		}
		else if (image instanceof TiBlob) {
			bitmap = ((TiBlob)image).getImage();
		}
		else if (image instanceof String) {
			try {
				// Load the image from the application assets
				String url = getPathToApplicationAsset((String)image);
				TiBaseFile file = TiFileFactory.createTitaniumFile(new String[] { url }, false);
				bitmap = TiUIHelper.createBitmap(file.getInputStream());
			} catch (IOException e) {
				Log.e(TAG,"Could not load image");
				return null;
			}
		}
		if (bitmap == null) {
			return null;
		}
		
		if (options != null) {
			if (options.containsKey("callback")) {
				KrollFunction callback = (KrollFunction) options.get("callback");
				if (callback != null) {
					(new FilterBitmapTask()).execute(this, bitmap, Integer.valueOf(filterType), options, callback);
					return null;
				}
			}
		}
		Bitmap result = TiImageHelper.imageFiltered(bitmap, FilterType.values()[filterType], options);
		
		if (result != null) {
			return TiBlob.blobFromImage(result);
		}
		return null;
	}
	
	private class FilterViewTask extends AsyncTask< Object, Void, Bitmap >
	{
		KrollFunction callback;
		KrollProxy proxy;

		@Override
		protected Bitmap doInBackground(Object... params)
		{
			proxy = (KrollProxy)params[0];
			TiViewProxy viewProxy = (TiViewProxy)params[1];
			int filterType = ((Integer)params[2]).intValue();
			HashMap options = (HashMap)params[3];
			TiUIView view = viewProxy.getOrCreateView();
			Bitmap bitmap = TiUIHelper.viewToBitmap(viewProxy.getProperties(), view.getOuterView());
			callback = (KrollFunction)params[4];
			return TiImageHelper.imageFiltered(bitmap, FilterType.values()[filterType], options);
		}
		/**
		 * Always invoked on UI thread.
		 */
		@Override
		protected void onPostExecute(Bitmap image)
		{
			KrollDict result = new KrollDict();
			if (image != null) {
				result.put("image", TiBlob.blobFromImage(image));
			}
			this.callback.callAsync(this.proxy.getKrollObject(), new Object[] { result });
		}
	}
	
	@Kroll.method
	public TiBlob getFilteredViewToImage(TiViewProxy viewProxy, int filterType, @Kroll.argument(optional=true) HashMap options) {
		
		if (options != null) {
			if (options.containsKey("callback")) {
				KrollFunction callback = (KrollFunction) options.get("callback");
				if (callback != null) {
					(new FilterViewTask()).execute(this, viewProxy, Integer.valueOf(filterType), options, callback);
					return null;
				}
			}
		}
		
		TiUIView view = viewProxy.getOrCreateView();
		if (view == null) {
			return null;
		}
		
		Bitmap bitmap = null;
		if (TiApplication.isUIThread()) {
			bitmap = TiUIHelper.viewToBitmap(viewProxy.getProperties(), view.getOuterView());
		} else {
			bitmap = (Bitmap) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETVIEWIMAGE), new Object[]{view.getOuterView()});
		}
		return getFilteredImage(bitmap, filterType, options);
	}
	
	@Kroll.method
	public TiBlob getFilteredScreenshot(int filterType, @Kroll.argument(optional=true) HashMap options) {
		Activity a = TiApplication.getAppCurrentActivity();

		while (a.getParent() != null) {
			a = a.getParent();
		}

		Window w = a.getWindow();

		while (w.getContainer() != null) {
			w = w.getContainer();
		}
		
		Bitmap bitmap = null;
		if (TiApplication.isUIThread()) {
			bitmap = TiUIHelper.viewToBitmap(null, w.getDecorView());
		} else {
			bitmap = (Bitmap) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETVIEWIMAGE), w.getDecorView());
		}
		return getFilteredImage(bitmap, filterType, options);
	}
}

