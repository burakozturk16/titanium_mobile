package org.appcelerator.titanium.view;

import java.util.WeakHashMap;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.util.TiUIHelper;
import org.appcelerator.titanium.util.TiUIHelper.Shadow;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
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
	ColorDrawable colorDrawable = null;
	ColorDrawable defaultColorDrawable = null;
	boolean needsDrawing = false;
	
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
	
	private void updateNeedsDrawing() {
		needsDrawing = (color != Color.TRANSPARENT || defaultColor != Color.TRANSPARENT ||
				gradientDrawable != null || imageDrawable != null || innerShadows != null);
	}
	
	@Override
	public void draw(Canvas canvas) {
		if (needsDrawing && !bounds.isEmpty()) {
			Path path = parent.getPath();
			if (path != null){
				try {
					canvas.clipPath(path);
				} catch (Exception e) {
					Log.w(TAG, "clipPath failed on canvas: " + e.getMessage(), Log.DEBUG_MODE);
				}
				if (color != Color.TRANSPARENT) {
					paint.setColor(color);
					canvas.drawPath(path, paint);		
				}
				else if(defaultColor != Color.TRANSPARENT) { 
					paint.setColor(defaultColor);
					canvas.drawPath(path, paint);		
				}
	            paint.setColor(Color.WHITE);
				if (gradientDrawable != null) 
				{
					paint.setShader(((TiGradientDrawable)gradientDrawable).getShaderFactory().resize((int)bounds.width(), (int)bounds.height()));
					canvas.drawPath(path, paint);		
				}
				if (imageDrawable != null) {
					if (imageDrawable instanceof BitmapDrawable){
						paint.setShader(new BitmapShader(((BitmapDrawable)imageDrawable).getBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
						canvas.drawPath(path, paint);
					}
					imageDrawable.draw(canvas);
				}
	            paint.setColor(0);
	            paint.setShader(null);
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
		                
		                paint.setXfermode(null);
		                paint.setMaskFilter(null);
		                paint.setShader(new BitmapShader(tempBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
						canvas.drawPath(path, paint);
		                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
					}
				}
	            paint.setShader(null);
			}
			else  {
				canvas.clipRect(bounds);
				if (colorDrawable != null)
				{
					colorDrawable.draw(canvas);
				}
				else if (defaultColorDrawable != null)
				{
					defaultColorDrawable.draw(canvas);
				}
				if (gradientDrawable != null) 
				{
					gradientDrawable.draw(canvas);
				}
				if (imageDrawable != null) {
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
			}
		}
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
		applyAlphaToDrawable(defaultColorDrawable);
		applyAlphaToDrawable(colorDrawable);
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
		if (colorDrawable != null) 
			colorDrawable.setBounds(bounds);
		if (defaultColorDrawable != null) 
			defaultColorDrawable.setBounds(bounds);
	}
	
	public void releaseDelegate() {
		clearBitmap();
		imageDrawable = null;
		gradientDrawable = null;
		colorDrawable = null;
		defaultColorDrawable = null;
	}
	
	public void setColor(int color)
	{
		clearBitmap();
		if (colorDrawable == null)
		{
			colorDrawable = new ColorDrawable();
			applyAlphaToDrawable(colorDrawable);
		}
		this.color = color;
		colorDrawable.setColor(color);
		updateNeedsDrawing();
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
		updateNeedsDrawing();
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
		updateNeedsDrawing();
	}
	
	public void setDefaultColor(int color) {
		clearBitmap();
		if (defaultColorDrawable == null)
		{
			defaultColorDrawable = new ColorDrawable();
			applyAlphaToDrawable(defaultColorDrawable);
		}
		defaultColor = color;
		defaultColorDrawable.setColor(color);
		updateNeedsDrawing();
	}
	
	public void setInnerShadows(Shadow[] shadows) {
		clearBitmap();
		innerShadows = shadows;
		updateNeedsDrawing();
	}
}