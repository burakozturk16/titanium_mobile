package org.appcelerator.titanium.transition;

import android.graphics.Camera;
import android.graphics.Matrix;
import android.view.View;

import com.nineoldandroids.animation.TypeEvaluator;


public class Rotate3dEvaluator implements TypeEvaluator<Matrix> {
//	static private FloatEvaluator floatEvaluator = new FloatEvaluator();
	
	private final float mFromDegrees;
    private final float mToDegrees;
    private final float mCenterX;
    private final float mCenterY;
    private final float mDepthZ;
    private final float mFromTranslateX;
    private final float mFromTranslateY;
    private final float mToTranslateX;
    private final float mToTranslateY;
    private final View mView;
    private int  mWidth;
    private int  mHeight;
    private Camera mCamera;

	public Rotate3dEvaluator(View view, float fromDegrees, float toDegrees,
            float centerX, float centerY, float depthZ, float fromTranslateX, float toTranslateX, float fromTranslateY, float toTranslateY) {
		mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mCenterX = centerX;
        mCenterY = centerY;
        mDepthZ = depthZ;
        mView = view;
        mFromTranslateX = fromTranslateX;
        mToTranslateX = toTranslateX;
        mFromTranslateY = fromTranslateY;
        mToTranslateY = toTranslateY;
		 mCamera = new Camera();
	}
	
	public Matrix evaluate(float fraction, Matrix startValue,
			Matrix endValue) {
		if (fraction == 0) {
			mWidth = mView.getWidth();
		    mHeight = mView.getHeight();
		}
		final float fromDegrees = mFromDegrees;
        float degrees = fromDegrees + ((mToDegrees - fromDegrees) * fraction);
        float translateX = mFromTranslateX + ((mToTranslateX - mFromTranslateX) * fraction);
        float translateY = mFromTranslateY + ((mToTranslateY - mFromTranslateY) * fraction);
        translateX*=mWidth;
        translateY*=mHeight;
        final float centerX = mCenterX*mWidth;
        final float centerY = mCenterY*mHeight;
        final Camera camera = mCamera;
        
        final Matrix matrix = startValue;

        camera.save();
        camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - fraction));
        camera.translate(translateX, translateY, translateX);
        camera.rotateY(degrees);
        camera.getMatrix(matrix);
        camera.restore();

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
        return matrix;

	}

}
