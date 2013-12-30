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


public class MaskableView extends FreeLayout {
	
	
	private Paint maskPaint;
	Bitmap bitmap;
	Rect bitmapRect = new Rect();
	
	public MaskableView(Context context) {
		super(context);
		maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    maskPaint.setColor(0xFFFFFFFF);
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
	}
	
	@Override
	public void draw(Canvas canvas)
	{
		if (bitmap != null) {
			Rect bounds = new Rect();
			getDrawingRect(bounds);
			super.draw(canvas);
			canvas.drawBitmap(bitmap, bitmapRect, bounds, maskPaint);
		}
		else {
			super.draw(canvas);
		}
	}
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		if (bitmap != null && willNotDraw()) {
			Rect bounds = new Rect();
			getDrawingRect(bounds);
			super.dispatchDraw(canvas);
			canvas.drawBitmap(bitmap, bitmapRect, bounds, maskPaint);
		}
		else {
			super.dispatchDraw(canvas);
		}
	}
	
	private static Bitmap convertToAlphaMask(Bitmap b) {
		Bitmap a = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ALPHA_8);
		Canvas c = new Canvas(a);
		c.drawBitmap(b, 0.0f, 0.0f, null);
		return a;
	}

	public void setMask(Bitmap bitmap)
	{
		this.bitmap = bitmap;
	
		if (bitmap != null) {
			bitmapRect.set(0,0, bitmap.getWidth(), bitmap.getHeight());
			maskPaint.setShader(new BitmapShader(convertToAlphaMask(bitmap), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
		}
		else {
		}
		invalidate();
	}
}
