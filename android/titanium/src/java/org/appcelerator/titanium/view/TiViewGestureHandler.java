package org.appcelerator.titanium.view;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiC;
import org.appcelerator.titanium.TiDimension;
import org.appcelerator.titanium.util.TiViewHelper;

import android.content.Context;
import android.graphics.PointF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

import com.almeros.android.gestures.MoveGestureDetector;
import com.almeros.android.gestures.RotateGestureDetector;
import com.almeros.android.gestures.ShoveGestureDetector;

public class TiViewGestureHandler {
    private static final String TAG = "TiViewGestureHandler";

    private GestureDetector mGestureDetector = null;
    protected ScaleGestureDetector mScaleGestureDetector = null;
    protected RotateGestureDetector mRotateGestureDetector = null;
    protected ShoveGestureDetector mShoveGestureDetector = null;
    protected MoveGestureDetector mPanGestureDetector = null;
    protected VelocityTracker velocityTracker;
    private final TiUIView mView;
    private final KrollProxy mProxy;
    private final Context mContext;

    private boolean mEnabled = false;
    private boolean mRotationEnabled = false;
    private boolean mScaleEnabled = false;
    private boolean mShoveEnabled = false;
    private boolean mPanEnabled = false;
    private boolean m2FingersEnabled = false;

    protected boolean mIsFlinging = false;
    private boolean mIsScaling = false;
    private boolean mIsRotating = false;
    private boolean mIsShoving = false;
    private boolean mIsPaning = false;

    private boolean mSimultaneousRotationScale = true;
    private boolean mVelocityComputed = false;
    private MotionEvent mCurrEvent = null;

    private final int ACTION_DOWN = 0;
    private final int ACTION_FLING = 1;
    private final int ACTION_LONG_PRESS = 2;
    private final int ACTION_SCROLL = 3;
    private final int ACTION_TAP = 4;
    private final int ACTION_DOUBLE_TAP = 5;
    private final int ACTION_SHOW_PRESS = 6;
    private final int ACTION_SCALE = 7;
    private final int ACTION_ROTATE = 8;
    private final int ACTION_SHOVE = 9;
    private final int ACTION_MOVE = 10;

    private boolean shouldIgnoreAction(final int action) {
        return shouldIgnoreAction(action, true);
    }

    private boolean shouldIgnoreAction(final int action,
            final boolean checkAnimated) {
        if (checkAnimated && isAnimating())
            return true;
        switch (action) {
        case ACTION_DOWN: {
            return false;
        }
        case ACTION_FLING:
        case ACTION_LONG_PRESS:
        case ACTION_SCROLL:
        case ACTION_TAP:
        case ACTION_DOUBLE_TAP:
        case ACTION_SHOW_PRESS: {
            return mIsScaling || mIsRotating || mIsShoving || mIsPaning;
        }
        case ACTION_SCALE: {
            return mIsShoving || mIsPaning || (!mSimultaneousRotationScale && mIsRotating);
        }
        case ACTION_ROTATE: {
            return mIsShoving || mIsPaning || (!mSimultaneousRotationScale && mIsScaling);
        }
        case ACTION_SHOVE: {
            return mIsScaling || mIsRotating || mIsPaning;
        }
        case ACTION_MOVE: {
            return mIsScaling || mIsRotating || mIsShoving;
        }
        default:
            return false;
        }
    }

    private boolean isAnimating() {
        // TODO Auto-generated method stub
        return false;
    }

    private GestureDetector getOrCreateGestureDetector() {
        if (this.mGestureDetector == null) {
            this.mGestureDetector = new GestureDetector(mContext,
                    new GestureDetector.SimpleOnGestureListener() {
                        @Override
                        public boolean onDown(final MotionEvent e) {
//                            if (shouldIgnoreAction(ACTION_DOWN)) {
//                                return true;
//                            }
                            // Stop scrolling if we are in the middle of a
                            // fling!
                            if (mIsFlinging) {
                                mIsFlinging = false;
                            }
                            return true;
                        }

                        @Override
                        public boolean onFling(final MotionEvent e1,
                                final MotionEvent e2, final float velocityX,
                                final float velocityY) {
                            if (shouldIgnoreAction(ACTION_FLING)) {
                                return true;
                            }

                            if (mProxy.hasListeners(TiC.EVENT_SWIPE, false)) {
                                Log.d(TAG, "SWIPE on " + mProxy, Log.DEBUG_MODE);
                                KrollDict data = mView.dictFromEvent(e2);
                                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                                    data.put(TiC.EVENT_PROPERTY_DIRECTION,
                                            velocityX > 0 ? "right" : "left");
                                } else {
                                    data.put(TiC.EVENT_PROPERTY_DIRECTION,
                                            velocityY > 0 ? "down" : "up");
                                }

                                mProxy.fireEvent(TiC.EVENT_SWIPE, data,
                                        false, false);
                            }
                            return true;
                        }

                        @Override
                        public void onLongPress(final MotionEvent e) {
                            if (shouldIgnoreAction(ACTION_LONG_PRESS)) {
                                return;
                            }
                            if (mProxy.hasListeners(TiC.EVENT_LONGPRESS, false)) {
                                mProxy.fireEvent(TiC.EVENT_LONGPRESS, mView.dictFromEvent(e), false, false);
                            }
                        }

                        @Override
                        public boolean onScroll(final MotionEvent e1,
                                final MotionEvent e2, final float distanceX,
                                final float distanceY) {
                            if (shouldIgnoreAction(ACTION_SCROLL)) {
                                return true;
                            }
                            return true;
                        }

                        @Override
                        public void onShowPress(final MotionEvent e) {
                            if (shouldIgnoreAction(ACTION_SHOW_PRESS)) {
                                return;
                            }
                        }

                        @Override
                        public boolean onSingleTapUp(final MotionEvent e) {
                            return false;
                        }

                        @Override
                        public boolean onSingleTapConfirmed(final MotionEvent e) {
                            if (shouldIgnoreAction(ACTION_TAP)) {
                                return true;
                            }
                            if (mProxy
                                    .hasListeners(TiC.EVENT_SINGLE_TAP, false)) {
                                Log.d(TAG, "TAP, TAP, TAP on " + mView,
                                        Log.DEBUG_MODE);
                                return mProxy.fireEvent(TiC.EVENT_SINGLE_TAP, mView.dictFromEvent(e), false, false);
                            }
                            return false;
                        }

                        @Override
                        public boolean onDoubleTap(final MotionEvent e) {
                            if (shouldIgnoreAction(ACTION_DOUBLE_TAP)) {
                                return true;
                            }
                            boolean hasDoubleTap = mProxy.hasListeners(
                                    TiC.EVENT_DOUBLE_TAP, false);
//                            boolean hasDoubleClick = mProxy
//                                    .hierarchyHasListener(TiC.EVENT_DOUBLE_CLICK);

//                            if (hasDoubleTap || hasDoubleClick) {
                              if (hasDoubleTap) {
                                KrollDict event = mView.dictFromEvent(e);
//                                if (hasDoubleTap)
                                    mProxy.fireEvent(TiC.EVENT_DOUBLE_TAP,
                                            event, false, false);
//                                if (hasDoubleClick)
//                                    mProxy.fireEvent(TiC.EVENT_DOUBLE_CLICK,
//                                            event, true, false);
                                return true;
                            }
                            return false;
                        }
                    });
        }
        return this.mGestureDetector;
    }
    
    private final class OnScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {
//        private float lastFocusX;
//      private float lastFocusY;

//      private float firstFocusX;
//      private float firstFocusY;
      private float firstSpan;
      private float currentScale;

      // When distinguishing twofingertap and pinch events,
      // minimum motion (in pixels)
      // to qualify as a scale event.
      private static final float SCALE_THRESHOLD = 6.0f;
      
      public float getCurrentScale() {
          return currentScale;
      }

      @Override
      public boolean onScaleBegin(
              ScaleGestureDetector detector) {
          firstSpan = detector.getCurrentSpan() == 0 ? 1 : detector.getCurrentSpan();
          currentScale = 1.0f;

          if (shouldIgnoreAction(ACTION_SCALE)) {
              return true;
          }

          return true;
      }

      @Override
      public boolean onScale(ScaleGestureDetector detector) {
          if (shouldIgnoreAction(ACTION_SCALE, false)) {
              return mIsScaling;
          }
          float delta = detector.getCurrentSpan() - firstSpan;

          if (Math.abs(delta) > SCALE_THRESHOLD) {
              mIsScaling = true;
          }

          // Suppress scale events (and allow for possible
          // two-finger tap events)
          // until we've moved at least a few pixels. Without
          // this check, two-finger
          // taps are very hard to register on some older
          // devices.
          if (!mIsScaling && Math.abs(delta) > SCALE_THRESHOLD) {
              mIsScaling = true;
              fireEventForAction(ACTION_SCALE, detector, this, 0);
          } else if (mIsScaling) {
              fireEventForAction(ACTION_SCALE, detector, this, 1);
          }
          return true;
      }

      @Override
      public void onScaleEnd(ScaleGestureDetector detector) {
          if (mIsScaling) {
              mIsScaling = false;
              fireEventForAction(ACTION_SCALE, detector, this, 2);
          }
      }
  }


    private ScaleGestureDetector getOrCreateScaleGestureDetector() {
        if (this.mScaleGestureDetector == null) {
            this.mScaleGestureDetector = new ScaleGestureDetector(mContext,
                    new OnScaleGestureListener());
        }
        return this.mScaleGestureDetector;
    }

    public void setQuickScaleEnabled(final boolean value) {
        if (TiC.KIT_KAT_OR_GREATER) {
            this.mScaleGestureDetector.setQuickScaleEnabled(value);
        }
    }
    
    private boolean fireEventForAction(final int action,
            final Object theDetector,final Object theListener, final int state) {
        String key = null;
        KrollDict event = TiViewHelper.dictFromMotionEvent(mView.getTouchView(), mCurrEvent);
       if (!mVelocityComputed) {
            mVelocityComputed = true;
            this.velocityTracker.computeCurrentVelocity(1000);
        }
        switch (action) {
        case ACTION_ROTATE: {
            RotateGestureDetector detector = (RotateGestureDetector) theDetector;
            final float currentDelta = ((OnRotateGestureListener)theListener).getCurrentDelta();
            final float timeDelta = detector.getTimeDelta() == 0 ? 1 : detector
                    .getTimeDelta();
            key = TiC.EVENT_ROTATE;
            event.put(TiC.EVENT_PROPERTY_ANGLE, currentDelta);
            event.put(TiC.EVENT_PROPERTY_VELOCITY, (currentDelta - 1.0f)
                    / timeDelta * 1000);
            break;
        }
        case ACTION_MOVE: {
            MoveGestureDetector detector = (MoveGestureDetector) theDetector;
            PointF translation = detector.getTranslationDelta();
            final float timeDelta = detector.getTimeDelta() == 0 ? 1 : detector
                    .getTimeDelta();
            TiDimension nativeValue = new TiDimension(0, TiDimension.TYPE_WIDTH);

            KrollDict point = new KrollDict();
            point.put(TiC.EVENT_PROPERTY_X, (translation.x - 1.0f) / timeDelta
                    * 1000);
            point.put(TiC.EVENT_PROPERTY_Y, (translation.y - 1.0f) / timeDelta
                    * 1000);
            event.put(TiC.EVENT_PROPERTY_VELOCITY, point);
            
            point = new KrollDict();
            nativeValue.setValue(translation.x);
            point.put(TiC.EVENT_PROPERTY_X, nativeValue.getAsDefault());
            nativeValue.setValue(translation.y);
            Log.d(TAG, "test " + translation.y + " , " + point.get(TiC.EVENT_PROPERTY_X));
            point.put(TiC.EVENT_PROPERTY_Y, nativeValue.getAsDefault());
            event.put(TiC.EVENT_PROPERTY_TRANSLATION, point);
            key = TiC.EVENT_PAN;
            break;
        }
        case ACTION_SCALE: {
            ScaleGestureDetector detector = (ScaleGestureDetector) theDetector;
            final float timeDelta = detector.getTimeDelta() == 0 ? 1 : detector.getTimeDelta();
            final float currentScale = ((OnScaleGestureListener)theListener).getCurrentScale();
            key = TiC.EVENT_PINCH;
            event.put(TiC.EVENT_PROPERTY_SCALE,
                    currentScale);
            event.put(TiC.EVENT_PROPERTY_VELOCITY,
                    (detector.getScaleFactor() - 1.0f)
                            / timeDelta * 1000);
            break;
        }
        default:
            return false;
        }
        if (event != null) {
            event.put(TiC.EVENT_PROPERTY_STATE, (state == 0)?"start":((state == 2)?"end":"move"));
            if (state == 0) {
                return mProxy.fireEvent(key + "start", event, false, false);
            } else if (state == 2) {
                return mProxy.fireEvent(key + "end", event, false, false);
            }
            return mProxy.fireEvent(key, event, false, false);
        }
        return false;
    }
    
    private final class OnRotateGestureListener implements RotateGestureDetector.OnRotateGestureListener {
//        private float firstAngle; // starting angle
        private float currentDelta;
        
        public float getCurrentDelta() {
            return currentDelta;
        }

        @Override
        public boolean onRotate(RotateGestureDetector detector) {
            float delta = detector.getRotationDegreesDelta();
            currentDelta += delta;
            if (shouldIgnoreAction(ACTION_ROTATE, false)) {
                return true;
            }

            if (!mIsRotating && Math.abs(currentDelta) > 5) {
                mIsRotating = true;
                fireEventForAction(ACTION_ROTATE, detector, this, 0);
            } else if (mIsRotating) {
                fireEventForAction(ACTION_ROTATE, detector, this, 1);
            }
            return true;
        }

        @Override
        public boolean onRotateBegin(
                RotateGestureDetector detector) {
            if (shouldIgnoreAction(ACTION_ROTATE)) {
                return true;
            }
//            firstAngle = 0;
            currentDelta = 0;
           return true;
        }

        @Override
        public void onRotateEnd(RotateGestureDetector detector) {
            if (mIsRotating) {
                mIsRotating = false;
                fireEventForAction(ACTION_ROTATE, detector, this, 2);
            }
        }
    }
    
    

    private RotateGestureDetector getOrCreateRotateGestureDetector() {
        if (this.mRotateGestureDetector == null) {
            this.mRotateGestureDetector = new RotateGestureDetector(mContext,
                    new OnRotateGestureListener());
        }
        return this.mRotateGestureDetector;
    }

    private ShoveGestureDetector getOrCreateShoveGestureDetector() {
        if (this.mShoveGestureDetector == null) {
            this.mShoveGestureDetector = new ShoveGestureDetector(mContext,
                    new ShoveGestureDetector.OnShoveGestureListener() {
                        private float currentDelta; // starting delta

                        @Override
                        public boolean onShove(ShoveGestureDetector detector) {

                            float delta = detector.getShovePixelsDelta();
                            if (shouldIgnoreAction(ACTION_SHOVE, false)) {
                                return true;
                            }

                            currentDelta += delta;
                            if (!mIsShoving && Math.abs(currentDelta) > 3) {
                                mIsShoving = true;
                            }
                            if (mIsShoving) {
                            }

                            return true;
                        }

                        @Override
                        public boolean onShoveBegin(
                                ShoveGestureDetector detector) {
                            if (shouldIgnoreAction(ACTION_SHOVE)) {
                                return mIsShoving;
                            }
                            currentDelta = 0;
                            return true;
                        }

                        @Override
                        public void onShoveEnd(ShoveGestureDetector detector) {
                            mIsShoving = false;
                        }
                    });
        }
        return this.mShoveGestureDetector;
    }
    
    private MoveGestureDetector getOrCreateMoveGestureDetector() {
        if (this.mPanGestureDetector == null) {
            ViewConfiguration vc = ViewConfiguration.get(this.mView.getOuterView().getContext());
            final int threshold = vc.getScaledPagingTouchSlop();
            this.mPanGestureDetector = new MoveGestureDetector(mContext,
                    new MoveGestureDetector.OnMoveGestureListener() {
                       
                        @Override
                        public void onMoveEnd(MoveGestureDetector detector) {
                            if (mIsPaning) {
                                mIsPaning = false;
                                fireEventForAction(ACTION_MOVE, detector, this, 2);
                            }
                        }
                        
                        @Override
                        public boolean onMoveBegin(MoveGestureDetector detector) {
                            if (shouldIgnoreAction(ACTION_MOVE)) {
                                return mIsPaning;
                            }
                            return true;
                        }
                        
                        @Override
                        public boolean onMove(MoveGestureDetector detector) {
                            if (shouldIgnoreAction(ACTION_MOVE, false)) {
                                return true;
                            }
                            PointF delta = detector.getTranslationDelta();
                            if (!mIsPaning && (Math.abs(delta.x) > threshold || Math.abs(delta.y) > threshold)) {
                                mIsPaning = true;
                                fireEventForAction(ACTION_MOVE, detector, this, 0);
                            } else if (mIsPaning) {
                                fireEventForAction(ACTION_MOVE, detector, this, 1);
                            }
                            return false;
                        }
                    });
        }
        return this.mPanGestureDetector;
    }

    public TiViewGestureHandler(final TiUIView view) {
        this.mView = view;
        this.mContext = view.getContext();
        this.mProxy = this.mView.proxy;

    }
    
    private void updateEnabled() {
        mEnabled = (mGestureDetector != null) || mRotationEnabled || mScaleEnabled || 
                mShoveEnabled || mPanEnabled || m2FingersEnabled;
        if (!mEnabled && this.velocityTracker != null) {
            this.velocityTracker.clear();
            this.velocityTracker = null;
        }
    }
    
    private VelocityTracker getVelocityTracker() {
        if (this.velocityTracker == null) {
            // Retrieve a new VelocityTracker object to watch
            // the
            // velocity of a motion.
            this.velocityTracker = VelocityTracker.obtain();
        }
        return this.velocityTracker;
    }

    public boolean onTouch(final View view, final MotionEvent event) {
        if (!mEnabled) {
            return false;
        }
        boolean handled = false;
        mCurrEvent = event;
        
        int action = event.getActionMasked();
        switch (action) {
        case MotionEvent.ACTION_DOWN:
            if (this.velocityTracker != null) {
                // Reset the velocity tracker back to its initial
                // state.
                this.velocityTracker.clear();
            }
            getVelocityTracker().addMovement(event);
            break;
        case MotionEvent.ACTION_MOVE:
            getVelocityTracker().addMovement(event);
        default:
            break;
        }
        mVelocityComputed = false;

        if (mGestureDetector != null) {
            handled |= mGestureDetector.onTouchEvent(event);
        }
        if (mRotationEnabled) {
            // can't use the scale detector's onTouchEvent() result as it always
            // returns true (Android issue #42591
//            if (rotatedEvent.getPointerCount() > 1) {
            handled |= mRotateGestureDetector.onTouchEvent(event);
//            }
        }
        if (mScaleEnabled) {
            handled |= mScaleGestureDetector.onTouchEvent(event);
        }
        if (mShoveEnabled) {
            handled |= mShoveGestureDetector.onTouchEvent(event);
        }
        
        if (mPanEnabled) {
            handled |= mPanGestureDetector.onTouchEvent(event);
        }
        
        if (m2FingersEnabled) {
            canTapTwoFingers = canTapTwoFingers & !isInteracting();
            handled |= handleTwoFingersTap(event);
        }
        mCurrEvent = null;
        return handled;
    }

    public boolean isScaling() {
        return mIsScaling;
    }

    public boolean isRotating() {
        return mIsRotating;
    }

    public boolean isFlinging() {
        return mIsFlinging;
    }

    public boolean isShoving() {
        return mIsShoving;
    }
    public boolean isPaning() {
        return mIsPaning;
    }

    public boolean isInteracting() {
        return mIsScaling || mIsRotating || mIsFlinging || mIsShoving || mIsPaning;
    }

    public boolean isScaleEnabled() {
        return mScaleEnabled;
    }

    public boolean isRotationEnabled() {
        return mRotationEnabled;
    }

    public boolean isShoveEnabled() {
        return mShoveEnabled;
    }
    
    public boolean isPanEnabled() {
        return mPanEnabled;
    }

    public void setRotationEnabled(final boolean enabled) {
        mRotationEnabled = enabled;
        if (enabled) {
            getOrCreateRotateGestureDetector();
        } else {
            mRotateGestureDetector = null;
        }
        updateEnabled();
   }

    public void setScaleEnabled(final boolean enabled) {
        mScaleEnabled = enabled;
        if (enabled) {
            getOrCreateScaleGestureDetector();
        } else {
            mScaleGestureDetector = null;
        }
        updateEnabled();
    }

    public void setShoveEnabled(final boolean enabled) {
        mShoveEnabled = enabled;
        if (enabled) {
            getOrCreateShoveGestureDetector();
        } else {
            mShoveGestureDetector = null;
        }
        updateEnabled();
    }
    
    public void setPanEnabled(final boolean enabled) {
        mPanEnabled = enabled;
        if (enabled) {
            getOrCreateMoveGestureDetector();
        } else {
            mPanGestureDetector = null;
        }
        updateEnabled();
    }
    
    public void setGlobalEnabled(final boolean enabled) {
        if (enabled) {
            getOrCreateGestureDetector();
        } else {
            mGestureDetector = null;
        }
        updateEnabled();
    }
    
    public void setTwoFingersTapEnabled(final boolean enabled) {
        m2FingersEnabled = enabled;
        updateEnabled();
    }

    public void flingeHasStopped() {
        mIsFlinging = false;
    }

    public void flingeHasStarted() {
        mIsFlinging = true;
    }

    private boolean canTapTwoFingers = false;
    private int multiTouchDownCount = 0;

    private boolean handleTwoFingersTap(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        for (int i = 0; i < pointerCount; i++) {
            int action = event.getActionMasked();
            switch (action) {
            case MotionEvent.ACTION_DOWN:
                multiTouchDownCount = 0;
                break;
            case MotionEvent.ACTION_UP:
                if (canTapTwoFingers) {
                    // handle two fingers event
//                    if (mProxy.hasListeners(TiC.EVENT_TWOFINGERTAP, false)) {
                        mProxy.fireEvent(TiC.EVENT_TWOFINGERTAP, mView.dictFromEvent(event), false, false);
//                    }
                    canTapTwoFingers = false;
                    multiTouchDownCount = 0;
                    return true;
                }
                canTapTwoFingers = false;
                multiTouchDownCount = 0;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiTouchDownCount++;
                canTapTwoFingers = multiTouchDownCount > 1;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                multiTouchDownCount--;
                break;
            default:
            }
        }
        return false;
    }

}
