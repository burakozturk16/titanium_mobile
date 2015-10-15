/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.kroll.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.List;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

public class KrollAssetHelper
{
	private static final String TAG = "TiAssetHelper";
	private static WeakReference<AssetManager> manager;
	private static String packageName, cacheDir;
	private static AssetCrypt assetCrypt;

	public interface AssetCrypt
	{
		String readAsset(String path);
		boolean assetExists(String path);
		List<String> list(String path);
	}

	public static void setAssetCrypt(AssetCrypt assetCrypt)
	{
		KrollAssetHelper.assetCrypt = assetCrypt;
	}

	public static void init(Context context)
	{
	    if (KrollAssetHelper.manager != null) {
	        return;
	    }
		KrollAssetHelper.manager = new WeakReference<AssetManager>(context.getAssets());
		KrollAssetHelper.packageName = context.getPackageName();
		KrollAssetHelper.cacheDir = context.getCacheDir().getAbsolutePath();
	}

	public static String readAsset(String path)
	{

		String resourcePath = path.replace("Resources/", "");

		if (assetCrypt != null) {
			String asset = assetCrypt.readAsset(resourcePath);
			if (asset != null) {
				Log.d(TAG, "Fetching \"" + resourcePath + "\" with assetCrypt...");
				return asset;
			}
		}

		try {
			AssetManager assetManager = manager.get();
			if (assetManager == null) {
				Log.e(TAG, "AssetManager is null, can't read asset: " + path);
				return null;
			}

			InputStream in = assetManager.open(path);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte buffer[] = new byte[1024];
			int count = 0;

			while ((count = in.read(buffer)) != -1) {
				if (out != null) {
					out.write(buffer, 0, count);
				}
			}

			Log.d(TAG, "Fetching \"" + resourcePath + "\" with assetManager...");
			return out.toString();

		} catch (IOException e) {
			Log.e(TAG, "Error while reading asset \"" + path + "\":", e);
		}

		return null;
	}

	public static String readFile(String path)
	{
		try {
			FileInputStream in = new FileInputStream(path);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte buffer[] = new byte[1024];
			int count = 0;

			while ((count = in.read(buffer)) != -1) {
				if (out != null) {
					out.write(buffer, 0, count);
				}
			}

			return out.toString();

		} catch (FileNotFoundException e) {
			Log.e(TAG, "File not found: " + path, e);

		} catch (IOException e) {
			Log.e(TAG, "Error while reading file: " + path, e);
		}

		return null;
	}

	public static boolean fileExists(String path)
	{
		String resourcePath = path;
		if (resourcePath != null && resourcePath.startsWith("Resources/")) {
			resourcePath = resourcePath.replace("Resources/", "");
		}

		return (assetCrypt != null && assetCrypt.assetExists(resourcePath));
	}

	public static void getDirectoryListing(String path, List<String> listing)
	{
		if (assetCrypt == null || path == null) return;
		String resourcePath = path;
		if (resourcePath.startsWith("Resources/")) {
			resourcePath = resourcePath.replace("Resources/", "");
		}
		if (resourcePath.endsWith("/")) { 
			resourcePath = resourcePath.substring(0, resourcePath.lastIndexOf("/"));
		}
		listing.addAll(assetCrypt.list(resourcePath));
	}

	public static String getPackageName()
	{
		return packageName;
	}

	public static String getCacheDir()
	{
		return cacheDir;
	}
}
