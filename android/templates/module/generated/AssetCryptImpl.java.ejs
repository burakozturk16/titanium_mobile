package <%- appid %>;

import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.lang.reflect.Method;
import java.lang.System;
import org.appcelerator.kroll.util.KrollAssetHelper;
import org.appcelerator.kroll.common.Log;
import android.os.Debug;

public class AssetCryptImpl implements KrollAssetHelper.AssetCrypt
{
	private boolean isProduction = false;
	
	public void setIsProduction(boolean production) 
	{
		isProduction = production;
	}

	private static class Range {
		int offset;
		int length;
		public Range(int offset, int length) {
			this.offset = offset;
			this.length = length;
		}
	}

	<%- encryptedAssets %>

	public String readAsset(String path)
	{
		if (isProduction && Debug.isDebuggerConnected()) {
			Log.e("AssetCryptImpl", "Illegal State. Exit.");
			System.exit(1);
		}

		Range range = assets.get(path);
		if (range == null) {
			return null;
		}
		return new String(filterDataInRange(assetsBytes, range.offset, range.length));
	}

	public boolean assetExists(String path)
	{
		return assets.containsKey(path);
	}

	public List<String> list(String path) {
		List<String> result = new ArrayList<String>();
		String  realPath = path;
		if (!realPath.endsWith("/")) {
			realPath = realPath + "/";
		}

		Set<String> keys = assets.keySet();
		Iterator<String> ite = keys.iterator();

		while (ite.hasNext()) {
			String candidate = ite.next();
			if (candidate.startsWith(realPath)) {
				result.add(candidate.replace(realPath, "").split("/")[0]);
			}
		}
		return result;
	}

	private static byte[] filterDataInRange(byte[] data, int offset, int length)
	{
		try {
			Class clazz = Class.forName("org.appcelerator.titanium.TiVerify");
			Method method = clazz.getMethod("filterDataInRange", new Class[] {data.getClass(), int.class, int.class});
			return (byte[])method.invoke(clazz, new Object[] { data, offset, length });
		} catch (Exception e) {
			Log.e("AssetCryptImpl", "Unable to load asset data.", e);
		}
		return new byte[0];
	}
}
