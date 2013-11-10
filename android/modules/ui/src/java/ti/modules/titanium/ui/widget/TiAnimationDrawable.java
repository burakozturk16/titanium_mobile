package ti.modules.titanium.ui.widget;

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
	private int duration = 50;

    public TiAnimationDrawable() {
        super();
        mAnimationState = (DrawableContainerState) new LevelListDrawable().getConstantState();
       setConstantState(mAnimationState);
    }

    @Override
    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        if (visible) {
            if (changed || restart) {
                setFrame(0, true, true);
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
        if (isRunning() && paused) {
            paused = false;
            run();
        }
    }

    public void pauseOrResume() {
        if (isRunning()) {
        	if (paused) {
        		resume();
        	} else {
        		pause();
        	}
        }
        else{
        	start();
        }
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

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    /**
     * Add a frame to the animation
     * 
     * @param frame The frame to add
     * @param duration How long in milliseconds the frame should appear
     */
    public void addFrame(Drawable frame) {
        mAnimationState.addChild(frame);
        if (mCurFrame < 0) {
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
            scheduleSelf(this, SystemClock.uptimeMillis() + duration);
        }
    }


    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mMutated = true;
        }
        return this;
    }
}

