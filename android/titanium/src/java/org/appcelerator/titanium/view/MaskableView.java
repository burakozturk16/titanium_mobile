package org.appcelerator.titanium.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;


public class MaskableView extends FreeLayout {
	
	
	private Paint maskPaint;
	Bitmap bitmap;
	
	public MaskableView(Context context) {
		super(context);
		maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    maskPaint.setColor(0xFFFFFFFF);
		maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		if (bitmap != null) {
			Rect bounds = new Rect();
			getDrawingRect(bounds);
			
			Bitmap mBitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
			Canvas mCanvas = new Canvas(mBitmap);
			super.dispatchDraw(mCanvas);
			mCanvas.drawBitmap(bitmap, new Rect(0,0, bitmap.getWidth(), bitmap.getHeight()), bounds, maskPaint);
			canvas.drawBitmap(mBitmap, bounds, bounds, new Paint());
			mBitmap.recycle();
		}
		else {
			super.dispatchDraw(canvas);
		}
	}

	public void setMask(Bitmap bitmap)
	{
		this.bitmap = bitmap;
	}
}
