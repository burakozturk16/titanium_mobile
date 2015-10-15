package org.appcelerator.titanium.view;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

//public class MaskableView extends FreeLayout {
//	
//	private static final boolean HONEYCOMB_OR_GREATER = (Build.VERSION.SDK_INT >= 11);
//	
//	private Paint maskPaint;
//	Bitmap originalMask;
//	Bitmap usedMask;
//	Rect lastBounds = new Rect();
//	
//	public MaskableView(Context context) {
//		super(context);
//		maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//		if (HONEYCOMB_OR_GREATER)
//		{
//			maskPaint.setColor(0xFFFFFFFF);
//			maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
//		}
//	}
//	
//	private void updateMask(Rect bounds){
//		if (!bounds.isEmpty() && !lastBounds.equals(bounds)) {
//			usedMask = convertToAlphaMask(originalMask, bounds);
//			lastBounds.set(bounds);
//		}
//	}
//	
//	@Override
//	public void draw(Canvas canvas)
//	{
//		if (originalMask != null) {
//			Rect bounds = new Rect();
//			getDrawingRect(bounds);
//			if (HONEYCOMB_OR_GREATER)
//			{
//				super.draw(canvas);
//				canvas.drawBitmap(originalMask, lastBounds, bounds, maskPaint);
//			}
//			else {
//				updateMask(bounds);
//
//				Bitmap b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
//				Canvas c = new Canvas(b);
//				super.draw(c);
//				maskPaint.setShader(createShader(b));
//				canvas.drawBitmap(usedMask, 0, 0, maskPaint);
//			}
//		}
//		else {
//			super.draw(canvas);
//		}
//	}
//	@Override
//	protected void dispatchDraw(Canvas canvas)
//	{
//		if (originalMask != null && willNotDraw()) {
//			Rect bounds = new Rect();
//			getDrawingRect(bounds);
//			if (HONEYCOMB_OR_GREATER)
//			{
//				super.dispatchDraw(canvas);
//				canvas.drawBitmap(originalMask, lastBounds, bounds, maskPaint);
//			}
//			else {
//				updateMask(bounds);
//
//				Bitmap b = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
//				Canvas c = new Canvas(b);
//				super.dispatchDraw(c);
//				maskPaint.setShader(createShader(b));
//				canvas.drawBitmap(usedMask, 0, 0, maskPaint);
//			}
//			
//		}
//		else {
//			super.dispatchDraw(canvas);
//		}
//	}
//	
//	private static Bitmap convertToAlphaMask(Bitmap b, Rect bounds) {
//		Bitmap a = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ALPHA_8);
//		Canvas c = new Canvas(a);
//		c.drawBitmap(b, new Rect(0,0, b.getWidth(), b.getHeight()), bounds, null);
//		return a;
//	}
//	
//	private static Shader createShader(Bitmap b) {
//		return new BitmapShader(b, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
//	}
//
//	public void setMask(Bitmap bitmap)
//	{
//	
//		if (bitmap != null) {
//			this.originalMask = bitmap;
//			if (HONEYCOMB_OR_GREATER){
//				lastBounds.set(0,0, bitmap.getWidth(), bitmap.getHeight());
//				maskPaint.setShader(new BitmapShader(convertToAlphaMask(originalMask, new Rect(0,0, originalMask.getWidth(), originalMask.getHeight())), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
//			}
//		}
//		else {
//			this.originalMask = null;
//		}
//		invalidate();
//	}
//}

public class MaskableView extends TiCompositeLayout implements OnGlobalLayoutListener {

    // Constants
    private static final String TAG = "MaskableView";

    private static final int MODE_ADD = 0;
    private static final int MODE_CLEAR = 1;
    private static final int MODE_DARKEN = 2;
    private static final int MODE_DST = 3;
    private static final int MODE_DST_ATOP = 4;
    private static final int MODE_DST_IN = 5;
    private static final int MODE_DST_OUT = 6;
    private static final int MODE_DST_OVER = 7;
    private static final int MODE_LIGHTEN = 8;
    private static final int MODE_MULTIPLY = 9;
    private static final int MODE_OVERLAY = 10;
    private static final int MODE_SCREEN = 11;
    private static final int MODE_SRC = 12;
    private static final int MODE_SRC_ATOP = 13;
    private static final int MODE_SRC_IN = 14;
    private static final int MODE_SRC_OUT = 15;
    private static final int MODE_SRC_OVER = 16;
    private static final int MODE_XOR = 17;

    private Handler mHandler;
    
    private final Rect viewRect = new Rect();
    private final Rect maskRect = new Rect();

    // Mask props

    private Drawable mDrawableMask = null;

    private Bitmap mFinalMask = null;
    private boolean enabled = false;

    // Drawing props
    private Paint mPaint = null;
    private static PorterDuffXfermode DEFAULT_MODE = new PorterDuffXfermode(
            PorterDuff.Mode.DST_IN);
    private PorterDuffXfermode mPorterDuffXferMode = DEFAULT_MODE;
    
    private boolean hwDisabled = false;

    public MaskableView(Context context) {
        super(context);
    }

    public MaskableView(Context context, AttributeSet attrs) {
        this(context);
    }

    public MaskableView(Context context, AttributeSet attrs, int defStyle) {
        this(context);
    }

    private void construct(Context context) {
        mHandler = new Handler();
        mPaint = createPaint();
    }
    
    private void disableHWAcceleration() {
        if (hwDisabled) return;
        hwDisabled = true;
        setDrawingCacheEnabled(true);
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null); // Only works for software
                                                     // layers
        }
    }
    
    private void enableHWAcceleration() {
        if (!hwDisabled) return;
        hwDisabled = false;
        setDrawingCacheEnabled(false);
        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_HARDWARE, null); // Only works for software
                                                     // layers
        }
    }

    private Paint createPaint() {
        Paint output = new Paint(Paint.ANTI_ALIAS_FLAG);
        output.setXfermode(mPorterDuffXferMode);
        return output;
    }

    private void initMask(Drawable input) {
        mDrawableMask = input;
        if (mDrawableMask instanceof AnimationDrawable) {
            mDrawableMask.setCallback(this);
        }
        updateEnabledState();
    }

    public Drawable getDrawableMask() {
        return mDrawableMask;
    }
    private Bitmap makeBitmapMask(final Drawable drawable) {
        return makeBitmapMask(drawable, getMeasuredWidth(), getMeasuredHeight());
    }
        
    private Bitmap makeBitmapMask(final Drawable drawable, final int width, final int height) {
        if (drawable != null) {
            if (width > 0 && height > 0) {
                Bitmap mask = Bitmap.createBitmap(width,
                        height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mask);
                drawable.setBounds(0, 0, width, height);
                drawable.draw(canvas);
                return mask;
            } else {
                log("Can't create a mask with height 0 or width 0. Or the layout has no children and is wrap content");
                return null;
            }
        } else {
            log("No bitmap mask loaded, view will NOT be masked !");
        }
        return null;
    }
    
    private Bitmap makeBitmapMask(final View view) {
        if (view != null) {
            Drawable d =  view.getBackground();
            float scaleFactor = 0.5f;
            int width = (int) (getMeasuredWidth() * scaleFactor);
            int height = (int) (getMeasuredHeight() * scaleFactor);
            if (d!= null && width > 0 && height > 0) {
                d.setBounds((int) (view.getLeft() * scaleFactor), 
                        (int) (view.getTop() * scaleFactor), 
                        (int) (view.getRight() * scaleFactor), 
                        (int) (view.getBottom() * scaleFactor));
                Bitmap mask = Bitmap.createBitmap(width,
                        height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(mask);
                d.draw(canvas);
                return mask;
            } else {
                log("Can't create a mask with height 0 or width 0. Or the layout has no children and is wrap content");
                return null;
            }
        } else {
            log("No bitmap mask loaded, view will NOT be masked !");
        }
        return null;
    }

    public void setMask(int drawableRes) {
        clearMaskView();
        Resources res = getResources();
        if (res != null) {
            setMask(res.getDrawable(drawableRes));
        } else {
            log("Unable to load resources, mask will not be loaded as drawable");
        }
    }

    public void setMask(Drawable input) {
        clearMaskView();
        initMask(input);
        swapBitmapMask(makeBitmapMask(mDrawableMask));
        invalidate();
    }
    
    private void updateEnabledState() {
        boolean newEnabled = (mDrawableMask != null || mMaskView != null);
        if (newEnabled != enabled) {
            enabled = newEnabled;
            if (enabled) {
                if (mPaint == null) {
                    construct(getContext());
                }
                disableHWAcceleration();
            }
            else {
                enableHWAcceleration();
            }
        }
    }

//     Once the size has changed we need to remake the mask.
     @Override
     protected void onSizeChanged(int w, int h, int oldw, int oldh) {
         super.onSizeChanged(w, h, oldw, oldh);
         if (enabled) {
             viewRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
             setSize(w, h);
         }
     }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (enabled && changed) {
            viewRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
            setSize(getMeasuredWidth(), getMeasuredHeight());
        }
    }

    private void setSize(int width, int height) {
        if (width > 0 && height > 0) {
            if (mDrawableMask != null) {
                // Remake the 9patch
                swapBitmapMask(makeBitmapMask(mDrawableMask, width, height));
            }
        } else {
            log("Width and height must be higher than 0");
        }
    }
    
    private void drawMask(Canvas canvas) {
        if (maskindispatch && mFinalMask != null && mPaint != null) {
            canvas.drawBitmap(mFinalMask, maskRect, viewRect, mPaint);
        }
    }
    
    private boolean maskindispatch = true;
    // Drawing
    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (enabled) {
            drawMask(canvas);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (enabled) {
            maskindispatch = false;
            super.draw(canvas);
            maskindispatch = true;
            drawMask(canvas);
        } else {
            super.draw(canvas);
        }
    }
    
    private View mMaskView = null;
    
    private void addObserverForView(final View view) {
        final ViewTreeObserver treeObserver = view.getViewTreeObserver();
        if (treeObserver != null && treeObserver.isAlive()) {
            treeObserver.addOnGlobalLayoutListener(this);
        }
    }
    private void removeObserverForView(final View view) {
        final ViewTreeObserver treeObserver = view.getViewTreeObserver();
        if (treeObserver != null && treeObserver.isAlive()) {
            if (TiC.JELLY_BEAN_OR_GREATER) {
                treeObserver.removeOnGlobalLayoutListener(this);
            } else {
                treeObserver.removeGlobalOnLayoutListener(this);
            }
        }
    }
    
    public void setMaskView(View maskView) {
        if (maskView == null) {
            clearMaskView();
        }
        else {
            mMaskView = maskView;
            addObserverForView(mMaskView);
        }
        updateEnabledState();
        invalidate();
    }
    
    private void clearMaskView() {
        if (mMaskView != null) {
            removeObserverForView(mMaskView);
            mMaskView = null;
        }
    }

    // Logging
    private void log(String message) {
        Log.d(TAG, message, Log.DEBUG_MODE);
    }

    // Animation
    @Override
    public void invalidateDrawable(Drawable dr) {
        if (dr != getBackground() && dr != null) {
            initMask(dr);
            swapBitmapMask(makeBitmapMask(dr));
            invalidate();
        }
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        if (who != null && what != null) {
            mHandler.postAtTime(what, when);
        }
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        if (who != null && what != null) {
            mHandler.removeCallbacks(what);
        }
    }

    private void swapBitmapMask(Bitmap newMask) {
        if (mFinalMask != null && !mFinalMask.isRecycled()) {
            mFinalMask.recycle();
        }
        mFinalMask = newMask;
        if (mFinalMask != null) {
            maskRect.set(0, 0, mFinalMask.getWidth(), mFinalMask.getHeight());
        }
    }

    // Utils
    private PorterDuffXfermode getModeFromInteger(int index) {
        PorterDuff.Mode mode = null;
        switch (index) {
        case MODE_ADD:
            if (Build.VERSION.SDK_INT >= 11) {
                mode = PorterDuff.Mode.ADD;
            } else {
                log("MODE_ADD is not supported on api lvl "
                        + Build.VERSION.SDK_INT);
            }
        case MODE_CLEAR:
            mode = PorterDuff.Mode.CLEAR;
            break;
        case MODE_DARKEN:
            mode = PorterDuff.Mode.DARKEN;
            break;
        case MODE_DST:
            mode = PorterDuff.Mode.DST;
            break;
        case MODE_DST_ATOP:
            mode = PorterDuff.Mode.DST_ATOP;
            break;
        case MODE_DST_IN:
            mode = PorterDuff.Mode.DST_IN;
            break;
        case MODE_DST_OUT:
            mode = PorterDuff.Mode.DST_OUT;
            break;
        case MODE_DST_OVER:
            mode = PorterDuff.Mode.DST_OVER;
            break;
        case MODE_LIGHTEN:
            mode = PorterDuff.Mode.LIGHTEN;
            break;
        case MODE_MULTIPLY:
            mode = PorterDuff.Mode.MULTIPLY;
            break;
        case MODE_OVERLAY:
            if (Build.VERSION.SDK_INT >= 11) {
                mode = PorterDuff.Mode.OVERLAY;
            } else {
                log("MODE_OVERLAY is not supported on api lvl "
                        + Build.VERSION.SDK_INT);
            }
        case MODE_SCREEN:
            mode = PorterDuff.Mode.SCREEN;
            break;
        case MODE_SRC:
            mode = PorterDuff.Mode.SRC;
            break;
        case MODE_SRC_ATOP:
            mode = PorterDuff.Mode.SRC_ATOP;
            break;
        case MODE_SRC_IN:
            mode = PorterDuff.Mode.SRC_IN;
            break;
        case MODE_SRC_OUT:
            mode = PorterDuff.Mode.SRC_OUT;
            break;
        case MODE_SRC_OVER:
            mode = PorterDuff.Mode.SRC_OVER;
            break;
        case MODE_XOR:
            mode = PorterDuff.Mode.XOR;
            break;
        default:
            mode = PorterDuff.Mode.DST_IN;
        }
        return new PorterDuffXfermode(mode);
    }

    public void setBlending(int blend) {
        mPorterDuffXferMode = getModeFromInteger(blend);
        mPaint.setXfermode(mPorterDuffXferMode);
    }

    @Override
    public void onGlobalLayout() {
        swapBitmapMask(makeBitmapMask(mMaskView));
        invalidate();
    }
}
