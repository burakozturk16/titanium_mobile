package org.appcelerator.titanium.transition;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.annotation.SuppressLint;
import android.graphics.Camera;
import android.graphics.Matrix;

/**
 * An animation that rotates the view on the Y axis between two specified angles.
 * This animation also adds a translation on the Z axis (depth) to improve the effect.
 */
public class Rotate3dAnimation extends Animation {
    private final float mFromDegrees;
    private final float mToDegrees;
    private final float mCenterX;
    private final float mCenterY;
    private final float mDepthZ;
    private final float mFromTranslateX;
    private final float mFromTranslateY;
    private final float mToTranslateX;
    private final float mToTranslateY;
    private final boolean mReverse;
    private final View mView;
    private int  mWidth;
    private int  mHeight;
    private Camera mCamera;

    /**
     * Creates a new 3D rotation on the Y axis. The rotation is defined by its
     * start angle and its end angle. Both angles are in degrees. The rotation
     * is performed around a center point on the 2D space, definied by a pair
     * of X and Y coordinates, called centerX and centerY. When the animation
     * starts, a translation on the Z axis (depth) is performed. The length
     * of the translation can be specified, as well as whether the translation
     * should be reversed in time.
     *
     * @param fromDegrees the start angle of the 3D rotation
     * @param toDegrees the end angle of the 3D rotation
     * @param centerX the X center of the 3D rotation
     * @param centerY the Y center of the 3D rotation
     * @param reverse true if the translation should be reversed, false otherwise
     */
    public Rotate3dAnimation(View view, float fromDegrees, float toDegrees,
            float centerX, float centerY, float depthZ, float fromTranslateX, float toTranslateX, float fromTranslateY, float toTranslateY, boolean reverse) {
        mFromDegrees = fromDegrees;
        mToDegrees = toDegrees;
        mCenterX = centerX;
        mCenterY = centerY;
        mDepthZ = depthZ;
        mReverse = reverse;
        mView = view;
        mFromTranslateX = fromTranslateX;
        mToTranslateX = toTranslateX;
        mFromTranslateY = fromTranslateY;
        mToTranslateY = toTranslateY;
    }

    @Override
    public void initialize(int width, int height, int parentWidth, int parentHeight) {
        super.initialize(width, height, parentWidth, parentHeight);
        mCamera = new Camera();
        mWidth = mView.getWidth();
        mHeight = mView.getHeight();
    }

    @SuppressLint("NewApi")
	@Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        final float fromDegrees = mFromDegrees;
        float degrees = fromDegrees + ((mToDegrees - fromDegrees) * interpolatedTime);
        float translateX = mFromTranslateX + ((mToTranslateX - mFromTranslateX) * interpolatedTime);
        float translateY = mFromTranslateY + ((mToTranslateY - mFromTranslateY) * interpolatedTime);
        translateX*=mWidth;
        translateY*=mHeight;
        final float centerX = mCenterX*mWidth;
        final float centerY = mCenterY*mHeight;
        final Camera camera = mCamera;
        
        
        mView.setVisibility((degrees>-90 && degrees<90)?View.VISIBLE:View.GONE);

        final Matrix matrix = t.getMatrix();

        camera.save();
        if (mReverse) {
            camera.translate(0.0f, 0.0f, mDepthZ * interpolatedTime);
        } else {
            camera.translate(0.0f, 0.0f, mDepthZ * (1.0f - interpolatedTime));
        }
        camera.translate(translateX, translateY, translateX);
        camera.rotateY(degrees);
        camera.getMatrix(matrix);
        camera.restore();

        matrix.preTranslate(-centerX, -centerY);
        matrix.postTranslate(centerX, centerY);
//        matrix.postTranslate(translateX, translateY);
    }
}
