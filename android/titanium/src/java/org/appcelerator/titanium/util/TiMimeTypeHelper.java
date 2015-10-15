/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2010 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.io.File;
import java.util.HashMap;

import android.webkit.MimeTypeMap;

public class TiMimeTypeHelper
{

	public static final HashMap<String, String> EXTRA_TEXT_MIMETYPES = new HashMap<String, String>();
	static {
		EXTRA_TEXT_MIMETYPES.put("js", "javascript");
		EXTRA_TEXT_MIMETYPES.put("html", "html");
		EXTRA_TEXT_MIMETYPES.put("htm", "html");
	}
    public static final HashMap<String, String> EXTRA_IMAGE_MIMETYPES = new HashMap<String, String>();
    static {
        EXTRA_IMAGE_MIMETYPES.put("png", "png");
        EXTRA_IMAGE_MIMETYPES.put("jpg", "jpg");
        EXTRA_IMAGE_MIMETYPES.put("gif", "gif");
        EXTRA_IMAGE_MIMETYPES.put("bmp", "bmp");
        EXTRA_IMAGE_MIMETYPES.put("pjpeg", "pjpeg");
        EXTRA_IMAGE_MIMETYPES.put("tiff", "tiff");
    } 
	
	public static String getMimeType(String url) {
		return getMimeType(url, "application/octet-stream");
	}
	
	public static String getMimeType(File file) {
        return getMimeType(file.getAbsolutePath());
    }
    
	public static String getMimeTypeFromFileExtension(String extension, String defaultType) {
		MimeTypeMap mtm = MimeTypeMap.getSingleton();
		String mimetype = defaultType;

		if (extension != null) {
			String type = mtm.getMimeTypeFromExtension(extension);
			if (type != null) {
				mimetype = type;
			} else {
				String lowerExtension = extension.toLowerCase();
		            if (EXTRA_TEXT_MIMETYPES.containsKey(lowerExtension)) {
		                return "text/" +  EXTRA_TEXT_MIMETYPES.get(lowerExtension);
		            } else if (EXTRA_IMAGE_MIMETYPES.containsKey(lowerExtension)) {
		                return "image/" +  EXTRA_IMAGE_MIMETYPES.get(lowerExtension);
		            }
			}
		}

		return mimetype;
	}
	
	public static String getFileExtensionFromUrl(String url) {
		return MimeTypeMap.getFileExtensionFromUrl(url);
	}

	public static String getMimeType(String url, String defaultType)
	{
		String extension = "";
		int pos = url.lastIndexOf('.');
		if (pos > 0) {
			extension = url.substring(pos + 1);
		}
		return getMimeTypeFromFileExtension(extension, defaultType);
	}
	
	public static String getFileExtensionFromMimeType(String mimeType, String defaultExtension)
	{
		String result = defaultExtension;
		String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
		if (extension != null) {
			result = extension;
		} else {
//			return mimeType;
		}
		
		return result;
	}
	
	public static boolean isBinaryMimeType(String mimeType) {
		if (mimeType != null) {
			String parts[] = mimeType.split(";");
			mimeType = parts[0];
			
			if (mimeType.startsWith("application/") && !mimeType.endsWith("xml"))
			{
				return true;
			}
			else if (mimeType.startsWith("image/") && !mimeType.endsWith("xml"))
			{
				return true;
			}
			else if (mimeType.startsWith("audio/") || mimeType.startsWith("video/")) 
			{
				return true;
			}
			else return false;
		}
		return false;
	}
}
