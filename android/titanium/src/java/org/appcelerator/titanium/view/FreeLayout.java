package org.appcelerator.titanium.view;

import org.appcelerator.kroll.common.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Transformation;
import android.widget.FrameLayout;

@SuppressLint("NewApi")
public class FreeLayout extends FrameLayout {
	public static final int FLAG_TRANSFORMED = 16;
	public FreeLayout(Context context) {
        super(context);
        setStaticTransformationsEnabled(true);
    }
    public Matrix transformedMatrix;
    
    public Matrix getMyViewMatrix() {
    	if (transformedMatrix != null) return transformedMatrix;
    	ViewGroup.LayoutParams layoutParams=getLayoutParams();
    	if (layoutParams instanceof LayoutParams) {
    		LayoutParams params = (LayoutParams)layoutParams;
    		if (params.matrix != null) {
    			transformedMatrix =params.matrix.getMatrix(this, params.anchorX, params.anchorY);
    			float dx = params.anchorX * getWidth();
    			float dy = params.anchorY * getHeight();
    			transformedMatrix.preTranslate(-dx, -dy);
    			transformedMatrix.postTranslate(dx, dy);
    		}
    		return transformedMatrix;
        }
    	return null;
    }
    
    public static Matrix getViewMatrix(View view) {
    	if (view instanceof FreeLayout) return ((FreeLayout)view).getMyViewMatrix();
        ViewGroup.LayoutParams layoutParams=view.getLayoutParams();
        if (layoutParams instanceof LayoutParams) {
    		LayoutParams params = (LayoutParams)layoutParams;
    		if (params.matrix != null) {
    			Matrix m = params.matrix.getMatrix(view, params.anchorX, params.anchorY);
    			float dx = params.anchorX * view.getWidth();
    			float dy = params.anchorY * view.getHeight();
    			m.preTranslate(-dx, -dy);
    			m.postTranslate(dx, dy);
    			return m;
    		}
        }
		return null;
    }
    
    public static void transformFrame(Rect rect,Matrix m) {
        m_tempRectF.set(rect);
        m_tempRectF.sort();
        m.mapRect(m_tempRectF);
        m_tempRectF.roundOut(rect);
    }

    public static Rect preInvalidate(View view,Rect dirty) {
    	if (dirty.isEmpty()) return dirty;
        Matrix m=getViewMatrix(view);
        if (m!=null) {
            if (dirty!=m_tempRect) {
                m_tempRect.set(dirty);
                dirty=m_tempRect;
            }
            transformFrame(m_tempRect,m);
        }
        return dirty;
    }
    public static void invalidate(View view) {
        m_tempRect.set(
            view.getScrollX(),
            view.getScrollY(),
            view.getScrollX()+view.getWidth(),
            view.getScrollY()+view.getHeight());
        view.invalidate(m_tempRect);
    }
    public static void invalidate(View view,int left,int top,int right,int bottom) {
        m_tempRect.set(left,top,right,bottom);
        view.invalidate(m_tempRect);
    }
    
//    public static boolean preDispatchTouchEvent(View view,MotionEvent event) {
//        Matrix m=getViewMatrix(view);
//        if (m!=null) {
//            Matrix mi=new Matrix();
//            if (m.invert(mi)) {
//                float[] points=new float[]{event.getX(),event.getY()};
//                Rect rect = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
//                mi.mapPoints(points);
//                if (event.getAction()==MotionEvent.ACTION_DOWN && !rect.contains((int)points[0],(int)points[1])) {
//                    return false;
//                }
//                event.setLocation(points[0],points[1]);
//            }
//        }
//        return true;
//    }
    
    public static void postGetHitRect(View view,Rect hitRect) {
        Matrix m=getViewMatrix(view);
        if (m!=null) {
        	Matrix realM = new Matrix(m);
        	realM.preTranslate(-hitRect.left, -hitRect.top);
        	realM.postTranslate(hitRect.left, hitRect.top);
            transformFrame(hitRect,realM);
        }
    }

    ///////////////////////////////////////////// LayoutParams

    public static class LayoutParams extends FrameLayout.LayoutParams {
        public LayoutParams(int width,int height,Ti2DMatrix matrix) {
            super(width,height);
            this.matrix=matrix;
        }
        public LayoutParams(int width,int height) {
            this(width,height,null);
        }
        public LayoutParams(FreeLayout.LayoutParams source) {
            super(source);
            this.matrix = source.matrix;
        }
        public LayoutParams(FrameLayout.LayoutParams source) {
            super(source);
        }
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public float anchorX = 0.5f;
        public float anchorY = 0.5f;
        public Ti2DMatrix matrix;
    }
    
    ///////////////////////////////////////////// implementation

//	@Override
//	protected void onSizeChanged (int w, int h, int oldw, int oldh) {
////		transformedMatrix = null;
//		super.onSizeChanged(w, h, oldw, oldh);
//	}
	
    @Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		 transformedMatrix = null;
		super.setLayoutParams(params);
	}
	
    @Override
    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
		 transformedMatrix = null;
		 super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        measureChildren(widthMeasureSpec,heightMeasureSpec);
//        setMeasuredDimension(
//            resolveSize(getSuggestedMinimumWidth(),widthMeasureSpec),
//            resolveSize(getSuggestedMinimumWidth(),heightMeasureSpec));
    }

    @Override
    protected FrameLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(super.generateDefaultLayoutParams());
    }

    @Override
    protected void onLayout(boolean changed,int l,int t,int r,int b) {
		 transformedMatrix = null;
		 super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }
    
    @Override
    protected boolean getChildStaticTransformation(View child,Transformation t) {
        Matrix m=getViewMatrix(child);
        if (m!=null) {
            t.getMatrix().set(m);
            t.setTransformationType(Transformation.TYPE_MATRIX);
            return true;
        } else {
            return false;
        }
    }

	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {

		for (int i = getChildCount() - 1; i >= 0; i--) {
			View v = getChildAt(i);
			if (v.getVisibility() != View.VISIBLE)
				continue;
			Matrix m = getViewMatrix(v);
			if (m != null) {
				float[] points = new float[] { ev.getX(), ev.getY() };
				RectF rect = new RectF(v.getLeft(), v.getTop(), v.getRight(),
						v.getBottom());
				
				Matrix realM = new Matrix(m);
				realM.preTranslate(-rect.left, -rect.top);
				realM.postTranslate(rect.left, rect.top);
				realM.mapRect(rect);
				if (rect.contains((int) points[0], (int) points[1])) {
					Matrix mi = new Matrix();
					realM.invert(mi);
					if (mi != null) {
						mi.mapPoints(points);
						ev.setEdgeFlags(ev.getEdgeFlags() | FLAG_TRANSFORMED); //a trick to mark the event as transformed
						ev.setLocation(points[0], points[1]); //without the transform point the view wont receive it
						return super.dispatchTouchEvent(ev);
					}
				}
			}
		}
		Matrix m = getMyViewMatrix();
		int flag = ev.getEdgeFlags();
		if (m != null && ((flag & FLAG_TRANSFORMED) != FLAG_TRANSFORMED) ) {
			float[] points = new float[] { ev.getX(), ev.getY() }; //untransformed point
			RectF rect = new RectF(getLeft(), getTop(), getRight(), getBottom()); //untransformed rect
			Matrix realM = new Matrix(m);
			realM.preTranslate(-rect.left, -rect.top);
			realM.postTranslate(rect.left, rect.top);
			realM.mapRect(rect);
			points[0] += getLeft();
			points[1] += getTop();
			if (!rect.contains(points[0], points[1]) && ev.getAction() == MotionEvent.ACTION_DOWN) {
				Log.d("FreeLayout", this + "not dispatching");
				return false;
			}
		}
		return super.dispatchTouchEvent(ev);
	}
    
    @Override
    public ViewParent invalidateChildInParent(final int[] location,final Rect dirty) {
    	Matrix m=getMyViewMatrix();
        if (m==null) {
            return super.invalidateChildInParent(location,dirty);
        }
        m_tempICPRect.set(dirty);
        ViewParent result=super.invalidateChildInParent(location,m_tempICPRect);
        dirty.union(m_tempICPRect);
        transformFrame(dirty,m);
        return result;
    }
    
    @Override
    public void getHitRect(Rect hitRect) {
        super.getHitRect(hitRect);
        postGetHitRect(this,hitRect);
    }    
    @Override
    public void invalidate(Rect dirty) {
        dirty=preInvalidate(this,dirty);
        super.invalidate(dirty);
    }
    
    @Override
    public void invalidate() {
        invalidate(this);
    }
    
    @Override
    public void invalidate(int left,int top,int right,int bottom) {
        invalidate(this,left,top,right,bottom);
    }

    ///////////////////////////////////////////// data

    private static Rect m_tempRect=new Rect();
    private static Rect m_tempICPRect=new Rect();
    private static RectF m_tempRectF=new RectF();
}