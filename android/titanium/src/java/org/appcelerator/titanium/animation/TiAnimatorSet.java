package org.appcelerator.titanium.animation;

import com.nineoldandroids.animation.AnimatorSet;

public class TiAnimatorSet extends TiAnimator {
	private AnimatorSet set;
	private AnimatorSet clonableSet;
	private AnimatorSet clonableReverseSet;
	private AnimatorSet reverseSet;
	public int repeatCount;
	public boolean needsToRestartFromBeginning = false;
	public int currentRepeatCount;
	TiAnimatorListener listener;

	public TiAnimatorSet() {
		set = new AnimatorSet();
	}

	public AnimatorSet set() {
		return set;
	}
	
	public AnimatorSet getOrCreateReverseSet() {
		if (reverseSet == null) {
			reverseSet = new AnimatorSet();
		}
		return reverseSet;
	}
	
	public AnimatorSet reverseSet() {
		return reverseSet;
	}
	
	public void resetSet() {
		set = clonableSet.clone();
	}
	
	public void resetReverseSet() {
		if (clonableReverseSet != null) {
			reverseSet = clonableReverseSet.clone();
		}
	}
	
	@Override
	protected void handleCancel() {
		super.handleCancel();
		set.cancel();
		if (reverseSet != null) {
			reverseSet.cancel();
		}
	}
	
	@Override
	public void cancelWithoutResetting(){
		super.cancelWithoutResetting();
		set.cancel();
		if (reverseSet != null) {
			reverseSet.cancel();
		}
	}
	
	public void setRepeatCount (int count) {
		repeatCount = currentRepeatCount = count;
	}
	
	public void setAnimating (boolean value) {
		animating = value;
	}
	
	public boolean getAnimating () {
		return animating;
	}
	
	public void setListener (TiAnimatorListener listener) {
		this.listener = listener;
		set.removeAllListeners();
		set.addListener(listener);
		if (reverseSet != null) {
			reverseSet.removeAllListeners();
			reverseSet.addListener(listener);
		}
	}
	
	public void createClonableSets () {
		this.clonableSet = set.clone();
		if (reverseSet != null) {
			this.clonableReverseSet = reverseSet.clone();
		}
	}

	public void aboutToBePrepared() {
		if (needsToRestartFromBeginning && restartFromBeginning) {
			restartFromBeginning();
		}
	}
}