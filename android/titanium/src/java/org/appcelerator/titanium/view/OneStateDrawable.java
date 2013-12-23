package org.appcelerator.titanium.view;

import java.util.WeakHashMap;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
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
	
	public static class Shadow {
		public float radius = 12;
		public float dx = 0;
		public float dy = 20;
		public int color = Color.BLUE;
		
		public Shadow() 
		{
		}
		public Shadow(int color) 
		{
			this.color = color;
		}
	}
	private static final String TAG = "OneStateDrawable";
	private RectF bounds = new RectF();
	
	private WeakHashMap<String, Pair<Canvas, Bitmap>> canvasStore;
    
    private Canvas tempCanvas;
    private Bitmap tempBitmap;
			
	Drawable colorDrawable;
	Drawable imageDrawable; //BitmapDrawable or NinePatchDrawable
	Drawable gradientDrawable;
	private Drawable defaultColorDrawable;
//	private Shadow[] outerShadows = {new Shadow(Color.RED)};
	private Shadow[] innerShadows = null;
	private float[] radius = null;
	Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	Path path;
	
	Bitmap cachedBitmap;
	Canvas cacheCanvas = new Canvas();
	
	private int alpha = 255;
	
	private void drawColorDrawable(ColorDrawable drawable, Canvas canvas)
	{
		int oldAlpha = -1;
		if (alpha < 255 && alpha != -1) {
			oldAlpha = drawable.getAlpha();
			drawable.setAlpha((alpha*oldAlpha)/255);
		}
		drawable.draw(canvas);
		if(oldAlpha != -1) {
			colorDrawable.setAlpha(oldAlpha);
		}
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
//		if (outerShadows != null && outerShadows.length > 0) {
//			for(Shadow shadow : outerShadows){
//				canvas.save();
//				canvas.translate(50, 20);
//				paint.setColor(shadow.color); 
//                paint.setShadowLayer(shadow.radius, shadow.dx, shadow.dy, shadow.color);
//                canvas.drawPath(path, paint);
//                paint.clearShadowLayer();
//                canvas.restore();
////	            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
////                canvas.drawPath(path, paint);
////                paint.setXfermode(null);
//			}
//		}
		if (cachedBitmap == null && !bounds.isEmpty()) {
			cachedBitmap = Bitmap.createBitmap((int)bounds.width(), (int)bounds.height(), Bitmap.Config.ARGB_8888);
			cacheCanvas.setBitmap(cachedBitmap);
			cacheCanvas.clipPath(path);
			if (colorDrawable != null) {
				drawColorDrawable((ColorDrawable) colorDrawable, cacheCanvas);
			}
			else if(defaultColorDrawable != null) {
				drawColorDrawable((ColorDrawable) defaultColorDrawable, cacheCanvas);
			}
			if (gradientDrawable != null)
				gradientDrawable.draw(cacheCanvas);
			if (imageDrawable != null) {
				imageDrawable.draw(cacheCanvas);
			}
			if (innerShadows != null && innerShadows.length > 0) {
				generateTempCanvas();
				for(Shadow shadow : innerShadows){
					paint.setColor(shadow.color);
					tempCanvas.drawPath(path, paint);
	                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
	                paint.setMaskFilter(new BlurMaskFilter(shadow.radius, BlurMaskFilter.Blur.NORMAL));
	                tempCanvas.save();
	                tempCanvas.translate(shadow.dx, shadow.dy);
					paint.setColor(Color.WHITE);
					tempCanvas.drawPath(path, paint);
	                tempCanvas.restore();
	                cacheCanvas.drawBitmap(tempBitmap, 0, 0, null);
	                tempCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
	                
	                paint.setXfermode(null);
	                paint.setMaskFilter(null);
				}
			}
			cacheCanvas.setBitmap(null);
		}
		if (cachedBitmap != null) canvas.drawBitmap(cachedBitmap, 0, 0, null);
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
		if (cachedBitmap != null) {
			cachedBitmap.recycle();
			cachedBitmap = null;
		}
	}

	@Override
	public void setAlpha(int alpha) {
		this.alpha = alpha;
		 clearBitmap();
		//dont set it for the color or we break the actual color alpha
//		applyAlphaToDrawable(defaultColorDrawable);
//		applyAlphaToDrawable(colorDrawable);
		applyAlphaToDrawable(gradientDrawable);
		applyAlphaToDrawable(gradientDrawable);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {			
	}
	
	
	private void updatePath(){
		path = new Path();
		if (radius != null) {
			path.addRoundRect(bounds, radius, Direction.CW);
		}
		else {
			path.addRect(bounds, Direction.CW);
		}
	}
	
	@Override
	public void setBounds (Rect bounds) {
		this.bounds = new RectF(bounds);
		clearBitmap();
		updatePath();
		if (colorDrawable != null)
			colorDrawable.setBounds(bounds);
		if (gradientDrawable != null)
			gradientDrawable.setBounds(bounds);
		if (imageDrawable != null) 
			imageDrawable.setBounds(bounds);
	}
	
	@Override
	public boolean setState (int[] stateSet) {
		boolean result = false;
		if (colorDrawable != null)
			result |= colorDrawable.setState(stateSet);
		if (gradientDrawable != null)
			result |= gradientDrawable.setState(stateSet);
		if (imageDrawable != null)
			result |= imageDrawable.setState(stateSet);
		return result;
	}
	
	@Override
	public void invalidateSelf() {
		clearBitmap();
		if (colorDrawable != null)
			colorDrawable.invalidateSelf();
		if (gradientDrawable != null)
			gradientDrawable.invalidateSelf();
		if (imageDrawable != null)
			imageDrawable.invalidateSelf();
	}
	
	public void releaseDelegate() {
		clearBitmap();
		imageDrawable = null;
		colorDrawable = null;
		gradientDrawable = null;
	}
	
	public void invalidateDrawable(Drawable who) {
		clearBitmap();
		if (colorDrawable == who)
			colorDrawable.invalidateSelf();
		else if (gradientDrawable  == who)
			gradientDrawable.invalidateSelf();
		else if (imageDrawable  == who)
			imageDrawable.invalidateSelf();
	}
	
	public void setColorDrawable(Drawable drawable)
	{
		clearBitmap();
		applyAlphaToDrawable(drawable);
		colorDrawable = drawable;
	}
	
	public void setColor(int color)
	{
		clearBitmap();
		if (colorDrawable  == null) {
			colorDrawable = new ColorDrawable(color);
//			applyAlphaToDrawable(colorDrawable);
		}
		else {
			((ColorDrawable)colorDrawable).setColor(color);
		}
	}
	
	public int getColor()
	{
		if (colorDrawable != null)
			return ((ColorDrawable)colorDrawable).getColor();
		return Color.TRANSPARENT;
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
	
//	protected void setNativeView(View view)
//	{
//		if (gradientDrawable != null && imageDrawable instanceof TiGradientDrawable) {
//			TiGradientDrawable drawable  = (TiGradientDrawable)gradientDrawable;
//			drawable.invalidateSelf();
//		}
//	}
	public void setDefaultColorDrawable(ColorDrawable drawable) {
		clearBitmap();
		applyAlphaToDrawable(drawable);
		defaultColorDrawable = drawable;
	}
	
	public void setRadius(float[] radius)
	{
		clearBitmap();
		this.radius = radius;
		if (!bounds.isEmpty()) {
			updatePath();
		}
	}
	
}