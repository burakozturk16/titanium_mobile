package org.appcelerator.titanium.view;



import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;


public class MaskableView extends FreeLayout {
	
	private static final boolean HONEYCOMB_OR_GREATER = (Build.VERSION.SDK_INT >= 11);
	
	private Paint maskPaint;
	Bitmap originalMask;
	Bitmap usedMask;
	Rect lastBounds = new Rect();
	
	public MaskableView(Context context) {
		super(context);
		maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		if (HONEYCOMB_OR_GREATER)
		{
			maskPaint.setColor(0xFFFFFFFF);
			maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
		}
	}
	
	private void updateMask(Rect bounds){
		if (!bounds.isEmpty() && !lastBounds.equals(bounds)) {
			usedMask = convertToAlphaMask(originalMask, bounds);
			lastBounds.set(bounds);
		}
	}
	
	@Override
	public void draw(Canvas canvas)
	{
		if (originalMask != null) {
			Rect bounds = new Rect();
			getDrawingRect(bounds);
			if (HONEYCOMB_OR_GREATER)
			{
				super.draw(canvas);
				canvas.drawBitmap(originalMask, lastBounds, bounds, maskPaint);
			}
			else {
				updateMask(bounds);

				Bitmap b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(b);
				super.draw(c);
				maskPaint.setShader(createShader(b));
				canvas.drawBitmap(usedMask, 0, 0, maskPaint);
			}
		}
		else {
			super.draw(canvas);
		}
	}
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		if (originalMask != null && willNotDraw()) {
			Rect bounds = new Rect();
			getDrawingRect(bounds);
			if (HONEYCOMB_OR_GREATER)
			{
				super.dispatchDraw(canvas);
				canvas.drawBitmap(originalMask, lastBounds, bounds, maskPaint);
			}
			else {
				updateMask(bounds);

				Bitmap b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(b);
				super.dispatchDraw(c);
				maskPaint.setShader(createShader(b));
				canvas.drawBitmap(usedMask, 0, 0, maskPaint);
			}
			
		}
		else {
			super.dispatchDraw(canvas);
		}
	}
	
	private static Bitmap convertToAlphaMask(Bitmap b, Rect bounds) {
		Bitmap a = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ALPHA_8);
		Canvas c = new Canvas(a);
		c.drawBitmap(b, new Rect(0,0, b.getWidth(), b.getHeight()), bounds, null);
		return a;
	}
	
	private static Shader createShader(Bitmap b) {
		return new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
	}

	public void setMask(Bitmap bitmap)
	{
	
		if (bitmap != null) {
			this.originalMask = bitmap;
			if (HONEYCOMB_OR_GREATER){
				lastBounds.set(0,0, bitmap.getWidth(), bitmap.getHeight());
				maskPaint.setShader(new BitmapShader(convertToAlphaMask(originalMask, new Rect(0,0, originalMask.getWidth(), originalMask.getHeight())), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
			}
		}
		else {
			this.originalMask = null;
		}
		invalidate();
	}
}
