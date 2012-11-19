/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2010-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package ti.modules.titanium.network;

import ti.modules.titanium.network.TiHTTPClient;

public class TiPersistentHTTPClient extends TiHTTPClient
{
	public TiPersistentHTTPClient(KrollProxy proxy)
	{
		super(proxy);

		//to make sure if someone used System.setProperty("http.keepAlive", "false");
		this.setRequestHeader("Connection","Keep-Alive");
	}
}
