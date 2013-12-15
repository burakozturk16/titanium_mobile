package org.appcelerator.titanium.view;

import org.appcelerator.titanium.util.TiUIHelper;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

public class OneStateDrawable extends Drawable {
	private static final String TAG = "OneStateDrawable";
			
	Drawable colorDrawable;
	Drawable imageDrawable; //BitmapDrawable or NinePatchDrawable
	Drawable gradientDrawable;
	private Drawable defaultColorDrawable;
	
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

	@Override
	public void draw(Canvas canvas) {
		if (colorDrawable != null) {
			drawColorDrawable((ColorDrawable) colorDrawable, canvas);
		}
		else if(defaultColorDrawable != null) {
			drawColorDrawable((ColorDrawable) defaultColorDrawable, canvas);
		}
		if (gradientDrawable != null)
			gradientDrawable.draw(canvas);
		if (imageDrawable != null) {
			imageDrawable.draw(canvas);
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
//		if (alpha == 255 || alpha == -1) {
//			drawable.setColorFilter(null);
//		}
//		else {
//			drawable.setColorFilter(TiUIHelper.createColorFilterForOpacity((float)alpha / 255));
//		}
	}

	@Override
	public void setAlpha(int alpha) {
		this.alpha = alpha;
		//dont set it for the color or we break the actual color alpha
//		applyAlphaToDrawable(defaultColorDrawable);
//		applyAlphaToDrawable(colorDrawable);
		applyAlphaToDrawable(gradientDrawable);
		applyAlphaToDrawable(gradientDrawable);
	}

	@Override
	public void setColorFilter(ColorFilter cf) {			
	}
	
	@Override
	public void setBounds (Rect bounds) {
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
		if (colorDrawable != null)
			colorDrawable.invalidateSelf();
		if (gradientDrawable != null)
			gradientDrawable.invalidateSelf();
		if (imageDrawable != null)
			imageDrawable.invalidateSelf();
	}
	
	public void releaseDelegate() {
		imageDrawable = null;
		colorDrawable = null;
		gradientDrawable = null;
	}
	
	public void invalidateDrawable(Drawable who) {
		if (colorDrawable == who)
			colorDrawable.invalidateSelf();
		else if (gradientDrawable  == who)
			gradientDrawable.invalidateSelf();
		else if (imageDrawable  == who)
			imageDrawable.invalidateSelf();
	}
	
	public void setColorDrawable(Drawable drawable)
	{
		applyAlphaToDrawable(drawable);
		colorDrawable = drawable;
	}
	
	public void setColor(int color)
	{
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
		applyAlphaToDrawable(drawable);
		imageDrawable = drawable;
	}
	
	public void setImageRepeat(boolean repeat)
	{
		if (imageDrawable != null && imageDrawable instanceof BitmapDrawable) {
			BitmapDrawable drawable  = (BitmapDrawable)imageDrawable;
			drawable.setTileModeX(Shader.TileMode.REPEAT);
			drawable.setTileModeY(Shader.TileMode.REPEAT);
		}
	}
	
	public void setGradientDrawable(Drawable drawable)
	{
		applyAlphaToDrawable(drawable);
		gradientDrawable = drawable;
	}
	
	protected void setNativeView(View view)
	{
		if (gradientDrawable != null && imageDrawable instanceof TiGradientDrawable) {
			TiGradientDrawable drawable  = (TiGradientDrawable)gradientDrawable;
			drawable.invalidateSelf();
		}
	}
	public void setDefaultColorDrawable(ColorDrawable drawable) {
		applyAlphaToDrawable(drawable);
		defaultColorDrawable = drawable;
	}
	
}