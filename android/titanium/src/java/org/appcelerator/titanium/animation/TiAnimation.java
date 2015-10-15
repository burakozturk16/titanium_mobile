package org.appcelerator.titanium.animation;

import org.appcelerator.kroll.KrollProxy;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.TiMessenger.CommandNoReturn;

@Kroll.proxy
public class TiAnimation extends KrollProxy {
	private TiAnimator animator;
	
	public TiAnimation(){
		animator = null;
	}

	public void setAnimator(TiAnimator animator) {
		this.animator = animator;
	}

	
	@Kroll.getProperty
	public boolean animating() {
		if (animator != null)
			return animator.animating();
		return false;
	}
	
	@Kroll.method
	public void cancel() {
	    runInUiThread(new CommandNoReturn() {
            @Override
            public void execute() {
                if (animator != null) {
                    animator.cancel();
                }
            }
        }, true);
		
	}
	
	@Kroll.method
	public void cancelWithoutResetting() {
	    runInUiThread(new CommandNoReturn() {
            @Override
            public void execute() {
                if (animator != null) {
                    animator.cancelWithoutResetting();
                }
            }
        }, true);
	}

}
