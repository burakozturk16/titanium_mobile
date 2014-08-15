/**
 * Appcelerator Titanium Mobile
 * Copyright (c) 2009-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Apache Public License
 * Please see the LICENSE included with this distribution for details.
 */
package org.appcelerator.titanium.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.digest.DigestUtils;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.kroll.util.KrollStreamHelper;
import org.appcelerator.titanium.TiApplication;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Pair;

/**
 * Manages the asynchronous opening of InputStreams from URIs so that
 * the resources get put into our TiResponseCache.
 */
public class TiDownloadManager implements Handler.Callback
{
	private static final String TAG = "TiDownloadManager";
	private static final int MSG_FIRE_DOWNLOAD_FINISHED = 1000;
	private static final int MSG_FIRE_DOWNLOAD_FAILED = 1001;
	protected static TiDownloadManager _instance;
	public static final int THREAD_POOL_SIZE = 2;

	protected HashMap<String, ArrayList<SoftReference<TiDownloadListener>>> listeners = new HashMap<String, ArrayList<SoftReference<TiDownloadListener>>>();
	protected ArrayList<String> downloadingURIs = new ArrayList<String>();
	protected ExecutorService threadPool;
	protected Handler handler;

	public static TiDownloadManager getInstance()
	{
		if (_instance == null) {
			_instance = new TiDownloadManager();
		}
		return _instance;
	}

	protected TiDownloadManager()
	{
		handler = new Handler(this);
		threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
	}

	public void download(URI uri, HashMap options, TiDownloadListener listener)
	{
		if (TiResponseCache.peek(uri)) {
			sendMessage(uri, null, MSG_FIRE_DOWNLOAD_FINISHED);
		} else {
			startDownload(uri, options, listener);
		}
	}
	
	public void download(URI uri, TiDownloadListener listener)
    {
        download(uri, null, listener);
    }
	
	private void sendMessage(URI uri, HttpURLConnection connection, int what)
	{
		Message msg = handler.obtainMessage(what);
		if (connection == null) {
	        msg.obj = uri;
		}
		else {
	        msg.obj = Pair.create(uri,connection);
		}
		msg.sendToTarget();
	}

	protected void startDownload(URI uri, HashMap options, TiDownloadListener listener)
	{
		String hash = DigestUtils.shaHex(uri.toString());
		ArrayList<SoftReference<TiDownloadListener>> listenerList = null;
		synchronized (listeners) {
			if (!listeners.containsKey(hash)) {
				listenerList = new ArrayList<SoftReference<TiDownloadListener>>();
				listeners.put(hash, listenerList);
			} else {
				listenerList = listeners.get(hash);
			}
			// We only allow a listener once per URI
			for (SoftReference<TiDownloadListener> l : listenerList) {
				if (l.get() == listener) {
					return;
				}
			}
			listenerList.add(new SoftReference<TiDownloadListener>(listener));
		}
		synchronized (downloadingURIs) {
			if (!downloadingURIs.contains(hash)) {
				downloadingURIs.add(hash);
				threadPool.execute(new DownloadJob(uri, options));
			}
		}
	}

	protected void handleFireDownloadMessage(URI uri, HttpURLConnection connection, int what)
	{
		ArrayList<SoftReference<TiDownloadListener>> toRemove = new ArrayList<SoftReference<TiDownloadListener>>();
		synchronized (listeners) {
			String hash = DigestUtils.shaHex(uri.toString());
			ArrayList<SoftReference<TiDownloadListener>> listenerList = listeners.get(hash);
			for (SoftReference<TiDownloadListener> listener : listenerList) {
				TiDownloadListener downloadListener = listener.get();
				if (downloadListener != null) {
					if (what == MSG_FIRE_DOWNLOAD_FINISHED) {
						downloadListener.downloadTaskFinished(uri, connection);
					} else {
						downloadListener.downloadTaskFailed(uri, connection);
					}
					toRemove.add(listener);
				}
			}
			for (SoftReference<TiDownloadListener> listener : toRemove) {
				listenerList.remove(listener);
			}
		}
	}

	protected class DownloadJob implements Runnable
	{
		protected URI uri;
		protected HashMap<String, Object> options;

		public DownloadJob(URI uri)
		{
			this.uri = uri;
		}
		
		public DownloadJob(URI uri, HashMap<String, Object> options)
        {
            this.uri = uri;
            this.options = options;
        }

        protected String getHeader(Map<String, List<String>> headers,
                String header) {
            List<String> values = headers.get(header);
            if (values == null || values.size() == 0) {
                return null;
            }
            return values.get(values.size() - 1);
        }
        
        private Map<String, List<String>> makeLowerCaseHeaders(Map<String, List<String>> origHeaders)
        {
            Map<String, List<String>> headers = new HashMap<String, List<String>>(origHeaders.size());
            for (String key : origHeaders.keySet()) {
                if (key != null) {
                    headers.put(key.toLowerCase(), origHeaders.get(key));
                }
            }
            return headers;
        }
        
		public void run()
		{
			try {
                OkHttpClient client = new OkHttpClient();
                URL url = uri.toURL();
                HttpURLConnection http = client.open(url);
                http.setUseCaches(true);
                http.addRequestProperty("TiCache", "true");
                http.addRequestProperty("Cache-Control", "no-cached");
                http.addRequestProperty("User-Agent", TiApplication.getInstance().getUserAgent());
                http.addRequestProperty("X-Requested-With", "XMLHttpRequest");

                if (options != null) {
                    Object value = options.get("headers");
                    if (value != null && value instanceof HashMap) {
                        HashMap<String, Object> headers = (HashMap<String, Object>)value;
                        for (Map.Entry<String, Object> entry : headers.entrySet()) {
                            http.addRequestProperty(entry.getKey(), TiConvert.toString(entry.getValue()));
                        }
                    }
                    if (options.containsKey("timeout")) {
                        int timeout = TiConvert.toInt(options, "timeout");
                        client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
                    }
                    if (options.containsKey("autoRedirect")) {
                        http.setInstanceFollowRedirects(TiConvert.toBoolean(options, "autoRedirect"));
                    }
                    if (options.containsKey("method"))
                    {
                        Object data = options.get("data");
                        if (data instanceof String) {
                            http.setRequestProperty("Content-Type", "charset=utf-8");
                        }
                        else if (data instanceof HashMap) {
                            http.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                        }
                        String dataToSend =  TiConvert.toString(data);
                        if (dataToSend != null) {
                            byte[] outputInBytes = dataToSend.getBytes("UTF-8");
                            OutputStream os = http.getOutputStream();
                            os.write( outputInBytes );
                            os.close();
                        }
                        
                        http.setRequestMethod(TiConvert.toString(options, "method"));
                    }
                }
                else {
                    int timeout = 20000;
                    client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
                    
                }
                
                for (Map.Entry<String, List<String>> k : http.getHeaderFields().entrySet()) {
                    for (String v : k.getValue()){
                         Log.d(TAG, k.getKey() + ":" + v);
                    }
                }
                
                int code = http.getResponseCode();
                boolean success = code >= 200 && code < 300;
                boolean nocache = false;
                if (success) {
                    Map<String, List<String>> headers = makeLowerCaseHeaders(http
                            .getHeaderFields());
                    String cacheControl = getHeader(headers, "cache-control");
                    if (cacheControl != null
                            && cacheControl
                                    .matches("^.*(no-cache|no-store|must-revalidate).*")) {
                        nocache = true;
//                        TiResponseCache.getDefault().put(uri, http);
                    }
                }
                if (nocache == false) {
                    InputStream stream = http.getInputStream();
                    KrollStreamHelper.pump(stream, null);
                    stream.close();
                }

				synchronized (downloadingURIs) {
					downloadingURIs.remove(DigestUtils.shaHex(uri.toString()));
				}

				// If there is additional background task, run it here.
				String hash = DigestUtils.shaHex(uri.toString());
				ArrayList<SoftReference<TiDownloadListener>> listenerList;
				synchronized (listeners) {
					listenerList = listeners.get(hash);
				}
				for (SoftReference<TiDownloadListener> listener : listenerList) {
					if (listener.get() != null) {
//					    if (success) {
		                       listener.get().postDownload(uri, http);
//					    }
//					    else {
//					        listener.get().downloadTaskFailed(uri, http);
//					    }
					}
				}

				sendMessage(uri, http, success?MSG_FIRE_DOWNLOAD_FINISHED:MSG_FIRE_DOWNLOAD_FAILED);
			} catch (Exception e) {
				
				synchronized (downloadingURIs) {
					downloadingURIs.remove(DigestUtils.shaHex(uri.toString()));
				}				
				
				// fire a download fail event if we are unable to download
				sendMessage(uri, null, MSG_FIRE_DOWNLOAD_FAILED);
				Log.e(TAG, "Exception downloading " + uri, e);
			}
		}
	}

	public boolean handleMessage(Message msg)
	{
		switch (msg.what) {
			case MSG_FIRE_DOWNLOAD_FINISHED:
			case MSG_FIRE_DOWNLOAD_FAILED:
			    if (msg.obj instanceof Pair) {
			        Pair<Object, Object> pair= (Pair<Object, Object>) msg.obj; 
	                handleFireDownloadMessage((URI) pair.first, (HttpURLConnection) pair.second, msg.what);
			    }
			    else {
                    handleFireDownloadMessage((URI) msg.obj, null, msg.what);
			    }
			    return true;
		}
		return false;
	}
}
