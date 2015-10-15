package ti.modules.titanium.ui.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.appcelerator.kroll.common.Log;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.LevelListDrawable;
import android.os.SystemClock;

public class TiAnimationDrawable extends DrawableContainer implements Runnable, Animatable {
    private final DrawableContainerState mAnimationState;
    private int mCurFrame = -1;
    private boolean mMutated;
    private boolean autoreverse = false;
    private boolean reverse = false;
    private boolean paused = false;
	private boolean actualReverse = false;
//	private int duration = 50;
	List<Integer> durations;
	private final String TAG = "TiAnimationDrawable";

    public TiAnimationDrawable() {
        super();
        durations = new ArrayList<Integer>();
        mAnimationState = (DrawableContainerState) new LevelListDrawable().getConstantState();
       setConstantState(mAnimationState);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if (changed || restart) {
                setFrame(0, true, false);
            }
        } else {
            unscheduleSelf(this);
        }
        return changed;
    }

    /**
     * <p>Starts the animation, looping if necessary. This method has no effect
     * if the animation is running. Do not call this in the {@link android.app.Activity#onCreate}
     * method of your activity, because the {@link ti.modules.titanium.ui.widget.TiAnimationDrawable} is
     * not yet fully attached to the window. If you want to play
     * the animation immediately, without requiring interaction, then you might want to call it
     * from the {@link android.app.Activity#onWindowFocusChanged} method in your activity,
     * which will get called when Android brings your window into focus.</p>
     *
     * @see #isRunning()
     * @see #stop()
     */
    public void start() {
        if (!isRunning()) {
        	resetCurrFrame();
        	actualReverse = reverse;
            paused = false;
            run();
        }
        else {
        	resetCurrFrame();
        	actualReverse = reverse;
       }
    }

    /**
     * <p>Stops the animation. This method has no effect if the animation is
     * not running.</p>
     *
     * @see #isRunning()
     * @see #start()
     */
    public void stop() {
        if (isRunning()) {
            setFrame(reverse?getNumberOfFrames()-1:0, true, false);
        }
    }

    public void pause() {
        if (isRunning() && !paused) {
            paused = true;
            super.unscheduleSelf(this);
        }
    }

    public void resume() {
    	if (!isRunning()) {
    		start();
    	}
    	else if (paused) {
            paused = false;
            run();
        }
    }

    public void pauseOrResume() {
        if (isRunning()) {
            Log.d(TAG, "pauseOrResume isRunning");
        	if (paused) {
        		resume();
        	} else {
        		pause();
        	}
        }
        else{
            Log.d(TAG, "pauseOrResume not running");
            start();
        }
    }
    
    public void setFrame(int frame) {
        pause();
        setFrame(reverse?getNumberOfFrames()-frame:frame, false, false);
    }
    
    public void setProgress(float progress) {
        setFrame(Math.round((reverse?1-progress:progress) * getNumberOfFrames()));
    }
    
    private void resetCurrFrame(){
    	mCurFrame = reverse?getNumberOfFrames():-1;
    }

    
    /**
     * <p>Indicates whether the animation is currently running or not.</p>
     *
     * @return true if the animation is running, false otherwise
     */
    public boolean isRunning() {
        return mCurFrame > -1 && mCurFrame < getNumberOfFrames();
    }

    /**
     * <p>This method exists for implementation purpose only and should not be
     * called directly. Invoke {@link #start()} instead.</p>
     *
     * @see #start()
     */
    public void run() {
        nextFrame(false);
    }

    @Override
    public void unscheduleSelf(Runnable what) {
    	resetCurrFrame();
        super.unscheduleSelf(what);
    }

    /**
     * @return The number of frames in the animation
     */
    public int getNumberOfFrames() {
        return mAnimationState.getChildCount();
    }
    
    /**
     * @return The Drawable at the specified frame index
     */
    public Drawable getFrame(int index) {
        return mAnimationState.getChildren()[index];
    }
    
    /**
     * Add a frame to the animation
     * 
     * @param frame The frame to add
     * @param duration How long in milliseconds the frame should appear
     */
    public void addFrame(Drawable frame, int duration) {
    	durations.add((Integer)duration);

        mAnimationState.addChild(frame);
        if (mCurFrame >= 0) {
            setFrame(0, true, false);
        }
    }
    
    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
    	if (reverse != this.reverse) {
    		this.reverse = reverse;
			actualReverse = !actualReverse;
    	}
    }

    public boolean isAutoreverse() {
        return autoreverse;
    }

    public void setAutoreverse(boolean autoreverse) {
    	this.autoreverse = autoreverse;
    }  
    
    private void nextFrame(boolean unschedule) {
        int next = mCurFrame;
        if (actualReverse ) {
			if (next == 0) {
				if (autoreverse) {
					actualReverse = !actualReverse;
					next ++;
				}
				else {
					next = getNumberOfFrames() -1;
				}
			}
			else {
				next --;
			}
		} else {
			if (next == getNumberOfFrames() -1) {
				if (autoreverse) {
					actualReverse = !actualReverse;
					next --;
				}
				else {
					next = 0;
				}
			}
			else {
				next ++;
			}
		}
        
        setFrame(next, unschedule, true);
    }

    private void setFrame(int frame, boolean unschedule, boolean animate) {
        if (frame >= mAnimationState.getChildCount()) {
            return;
        }
        mCurFrame = frame;
        selectDrawable(frame);
        if (unschedule) {
            unscheduleSelf(this);
        }
        if (animate) {
            // Unscheduling may have clobbered this value; restore it to record that we're animating
            mCurFrame = frame;
            scheduleSelf(this, SystemClock.uptimeMillis() + durations.get(frame));
        }
    }
    
    public int getCurrentFrame() {
        return mCurFrame;
    }

    public float getProgress() {
        return (float)mCurFrame / mAnimationState.getChildCount();
    }
    
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mMutated = true;
        }
        return this;
    }

	public void setDuration(int duration) {
		durations.clear();
		for (int i = 0; i < getNumberOfFrames(); i++) {
			durations.add(duration);
		}
		
	}
}

