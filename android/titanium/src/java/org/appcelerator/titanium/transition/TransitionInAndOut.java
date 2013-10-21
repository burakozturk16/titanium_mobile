package org.appcelerator.titanium.transition;

public class TransitionInAndOut {
	public Transition inTranstion;
	public Transition outTranstion;
	public int type;
	public int subType;

	public TransitionInAndOut(int type, int subtype, int duration) {
		this.type = type;
		subType = subtype;
		this.inTranstion = TransitionHelper.transitionForType(type, subtype, false, duration);
		this.outTranstion = TransitionHelper.transitionForType(type, subtype, true, duration);
	}
}