package org.appcelerator.titanium.view;

import java.util.WeakHashMap;

import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.Shadow;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Pair;

public class OneStateDrawable extends Drawable {
	
	
//	private static final boolean ICE_CREAM_OR_GREATER = (Build.VERSION.SDK_INT >= 14);
	private static final String TAG = "OneStateDrawable";
    public static final int DENSITY_NONE = 0;
	private RectF bounds = new RectF();
	
	private static WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;
    
    private Canvas tempCanvas;
    private Bitmap tempBitmap;
    
			
	int color = Color.TRANSPARENT;
	Drawable imageDrawable; //BitmapDrawable or NinePatchDrawable
	Drawable gradientDrawable;
	private int defaultColor = Color.TRANSPARENT;
	private Shadow[] innerShadows = null;
	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	TiBackgroundDrawable parent;
	
//	Bitmap cachedBitmap = null;
//	Canvas cacheCanvas = null;
//	
	private int alpha = 255;
	
	public OneStateDrawable(TiBackgroundDrawable parent) 
	{
		this.parent = parent;
	}


	
	private void generateTempCanvas(){
		if(canvasStore == null){
            canvasStore = new WeakHashMap<String, Pair<Canvas, Bitmap>>();
        }
        String key = String.format("%fx%f", bounds.width(), bounds.height());
        Pair<Canvas, Bitmap> stored = canvasStore.get(key);
        if(stored != null){
            tempCanvas = stored.first;
            tempBitmap = stored.second;
        }else{
	        tempCanvas = new Canvas();
	        tempBitmap = Bitmap.createBitmap((int)bounds.width(), (int)bounds.height(), Bitmap.Config.ARGB_8888);
	        tempCanvas.setBitmap(tempBitmap);
	        canvasStore.put(key, new Pair<Canvas, Bitmap>(tempCanvas, tempBitmap));
        }
    }
	@Override
	public void draw(Canvas canvas) {

		boolean needsDrawing = (color != Color.TRANSPARENT || defaultColor != Color.TRANSPARENT ||
				gradientDrawable != null || imageDrawable != null);
		if (needsDrawing && !bounds.isEmpty()) {
//			if (cacheCanvas == null)
//			{
//				cacheCanvas = new Canvas();
//			}
//			Log.d(TAG, "Bitmap.createBitmap " + (int)bounds.width() + ", " + (int)bounds.height());
//			cachedBitmap = Bitmap.createBitmap((int)bounds.width(), (int)bounds.height(), Bitmap.Config.ARGB_8888);
//			cacheCanvas.setBitmap(cachedBitmap);
			Path path = parent.getPath();
			if (path != null){
				canvas.clipPath(path);
			}
			if (color != Color.TRANSPARENT) {
//				paint.setColor(color);
//				canvas.drawPath(path, paint);
				canvas.drawColor(color);
			}
			else if(defaultColor != Color.TRANSPARENT) { 
				paint.setColor(defaultColor);
//				canvas.drawPath(path, paint);
				canvas.drawColor(defaultColor);
			}
			if (gradientDrawable != null) 
			{
				gradientDrawable.draw(canvas);
//				paint.setColor(Color.WHITE);
//				paint.setShader(((TiGradientDrawable)gradientDrawable).getShaderFactory().resize((int)bounds.width(), (int)bounds.height()));
//				canvas.drawPath(path, paint);
			}
			if (imageDrawable != null) {
//				canvas.clipPath(path);
				imageDrawable.draw(canvas);
			}
            paint.setShader(null);
            paint.setColor(0);
			if (innerShadows != null && innerShadows.length > 0) {
				generateTempCanvas();
				for(Shadow shadow : innerShadows){
					paint.setColor(TiUIHelper.adjustColorAlpha(shadow.color, 0.7f));
					tempCanvas.drawPath(path, paint);
	                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
	                paint.setMaskFilter(new BlurMaskFilter(shadow.radius, BlurMaskFilter.Blur.NORMAL));
	                tempCanvas.save();
	                tempCanvas.translate(shadow.dx, shadow.dy);
					paint.setColor(Color.WHITE);
					tempCanvas.drawPath(path, paint);
	                tempCanvas.restore();
	                canvas.drawBitmap(tempBitmap, 0, 0, null);
	                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
	                
	                paint.setXfermode(null);
	                paint.setMaskFilter(null);
				}
			}
//			if (ICE_CREAM_OR_GREATER && cacheCanvas != null)
//				cacheCanvas.setBitmap(null);
		}
//		if (cachedBitmap != null) {
//			if (alpha != -1) paint.setAlpha(alpha);
//			canvas.drawBitmap(cachedBitmap, 0, 0, null);
//		}
	}

	@Override
	public int getOpacity() {
		return 0;
	}
	
	private void applyAlphaToDrawable (Drawable drawable)
	{
		if (drawable == null) return;
		if (drawable instanceof ColorDrawable) return;
		drawable.setAlpha(alpha);
//		if (alpha == 255 || alpha == -1) {
//			drawable.setColorFilter(null);
//		}
//		else {
//			drawable.setColorFilter(TiUIHelper.createColorFilterForOpacity((float)alpha / 255));
//		}
	}
	
	private void clearBitmap()
	{
//		if (cachedBitmap != null) {
//			cachedBitmap.recycle();
//			cachedBitmap = null;
//		}
	}

	@Override
	public void setAlpha(int alpha) {
		this.alpha = alpha;
		 clearBitmap();
		//dont set it for the color or we break the actual color alpha
//		applyAlphaToDrawable(defaultColorDrawable);
//		applyAlphaToDrawable(colorDrawable);
		applyAlphaToDrawable(imageDrawable);
		applyAlphaToDrawable(gradientDrawable);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {			
	}
	
	
	
	@Override
	public void setBounds (Rect bounds) {
		this.bounds = new RectF(bounds);
		clearBitmap();
		if (gradientDrawable != null)
			gradientDrawable.setBounds(bounds);
		if (imageDrawable != null) 
			imageDrawable.setBounds(bounds);
	}
	
//	@Override
//	public boolean setState (int[] stateSet) {
//		boolean result = false;
//		if (colorDrawable != null)
//			result |= colorDrawable.setState(stateSet);
//		if (gradientDrawable != null)
//			result |= gradientDrawable.setState(stateSet);
//		if (imageDrawable != null)
//			result |= imageDrawable.setState(stateSet);
//		return result;
//	}
	
//	@Override
//	public void invalidateSelf() {
//		clearBitmap();
//		if (gradientDrawable != null)
//			gradientDrawable.invalidateSelf();
//		if (imageDrawable != null)
//			imageDrawable.invalidateSelf();
//	}
	
	public void releaseDelegate() {
		clearBitmap();
		imageDrawable = null;
		gradientDrawable = null;
	}
	
//	public void invalidateDrawable(Drawable who) {
//		clearBitmap();
//		if (gradientDrawable  == who)
//			gradientDrawable.invalidateSelf();
//		else if (imageDrawable  == who)
//			imageDrawable.invalidateSelf();
//	}
	
	public void setColor(int color)
	{
		clearBitmap();
		this.color = color;
	}
	
	public int getColor()
	{
		return color;
	}
	
	public void setBitmapDrawable(Drawable drawable)
	{
		clearBitmap();
		applyAlphaToDrawable(drawable);
		imageDrawable = drawable;
	}
	
	public void setImageRepeat(boolean repeat)
	{
		if (imageDrawable != null && imageDrawable instanceof BitmapDrawable) {
			clearBitmap();
			BitmapDrawable drawable  = (BitmapDrawable)imageDrawable;
			drawable.setTileModeX(Shader.TileMode.REPEAT);
			drawable.setTileModeY(Shader.TileMode.REPEAT);
		}
	}
	
	public void setGradientDrawable(Drawable drawable)
	{
		clearBitmap();
		applyAlphaToDrawable(drawable);
		gradientDrawable = drawable;
	}
	
	public void setDefaultColor(int color) {
		clearBitmap();
		defaultColor = color;
	}
	
	public void setInnerShadows(Shadow[] shadows) {
		clearBitmap();
		innerShadows = shadows;
	}
}