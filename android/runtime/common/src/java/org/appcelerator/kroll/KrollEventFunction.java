package org.appcelerator.kroll;

import java.util.HashMap;

public class KrollEventFunction implements KrollEventCallback {
	public KrollObject krollObject;
	public KrollFunction krollFunction;
	
	public KrollEventFunction(KrollObject krollObject, KrollFunction krollFunction)
	{
		this.krollObject = krollObject;
		this.krollFunction = krollFunction;
	}

	@Override
	public void call(Object data) {
		krollFunction.call(krollObject, (HashMap) data);
	}
}
