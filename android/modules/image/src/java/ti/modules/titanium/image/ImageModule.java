package ti.modules.titanium.image;

import java.io.IOException;
import java.util.HashMap;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import jp.co.cyberagent.android.gpuimage.GPUImageGaussianBlurFilter;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.annotations.Kroll;

import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiFileFactory;
import org.appcelerator.titanium.proxy.TiViewProxy;
import org.appcelerator.titanium.util.TiConvert;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.view.TiUIView;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.common.AsyncResult;
import org.appcelerator.kroll.common.TiMessenger;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;
import android.os.Message;

@Kroll.module
public class ImageModule extends KrollModule
{
	private static final String TAG = "ImageModule";
	private static GPUImage mGPUImage;

	private static final int MSG_FIRST_ID = KrollProxy.MSG_LAST_ID + 1;

	private static final int MSG_GETVIEWIMAGE = MSG_FIRST_ID + 100;

	@Kroll.constant public static final int FILTER_GAUSSIAN_BLUR = FilterType.GAUSSIAN_BLUR;

	//This handler callback is tied to the UI thread.
	public boolean handleMessage(Message msg)
	{
		switch(msg.what) {
			case MSG_GETVIEWIMAGE : {
				AsyncResult result = (AsyncResult) msg.obj;
				Object[] array = (Object[]) result.getArg();
				result.setResult(TiUIHelper.viewToBitmap(null, (View)array[0], ((Number)array[1]).floatValue()));
				return true;
			}
		}
		return super.handleMessage(msg);
	}
	
	public ImageModule()
	{
		super();
		mGPUImage = new GPUImage(TiApplication.getInstance().getBaseContext());
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
	
	private Bitmap getFilteredBitmap(Bitmap bitmap, int filterType, HashMap options) {
		switch (filterType) {
		case FilterType.GAUSSIAN_BLUR:
		{
			float blurSize = 1.0f;
			if (options != null) {
				blurSize = TiConvert.toFloat(options.get("blurSize"), 1.0f);
			}
			mGPUImage.setFilter(new GPUImageGaussianBlurFilter(blurSize));
			return mGPUImage.getBitmapWithFilterApplied(bitmap);
		}

		default:
			break;
		}
		return null;
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
		
		Bitmap result = getFilteredBitmap(bitmap, filterType, options);
		if (result != null) {
			return TiBlob.blobFromImage(result);
		}
		return null;
	}
	
	@Kroll.method
	public TiBlob getFilteredViewToImage(TiViewProxy viewProxy, Number scale, int filterType, @Kroll.argument(optional=true) HashMap options) {
		TiUIView view = viewProxy.getOrCreateView();
		if (view == null) {
			return null;
		}
		Bitmap bitmap = null;
		if (TiApplication.isUIThread()) {
			bitmap = TiUIHelper.viewToBitmap(viewProxy.getProperties(), view.getOuterView(), scale.floatValue());
		} else {
			bitmap = (Bitmap) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETVIEWIMAGE), new Object[]{view.getOuterView(), scale});
		}
		return getFilteredImage(bitmap, filterType, options);
	}
	
	@Kroll.method
	public TiBlob getFilteredScreenshot(Number scale, int filterType, @Kroll.argument(optional=true) HashMap options) {
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
			bitmap = TiUIHelper.viewToBitmap(null, w.getDecorView(), scale.floatValue());
		} else {
			bitmap = (Bitmap) TiMessenger.sendBlockingMainMessage(getMainHandler().obtainMessage(MSG_GETVIEWIMAGE), new Object[]{w.getDecorView(), scale});
		}
		return getFilteredImage(bitmap, filterType, options);
	}
}

