package org.appcelerator.titanium.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiBlob;
import org.appcelerator.titanium.TiFileProxy;
import org.appcelerator.titanium.io.TiBaseFile;
import org.appcelerator.titanium.io.TiResourceFile;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import android.util.Base64OutputStream;

public class TiHTTPHelper {
    private static final String TAG = "TiRequestBuilder";

    public static Object prepareBuilder(
            com.squareup.okhttp.Request.Builder builder, final String method,
            final Object userData) {
        boolean needMultipart = false;
        final String mLower = method.toLowerCase();
        boolean isPost = mLower.equals("post");
        boolean isPut = mLower.equals("put");
        boolean isGet = !isPost && !isPut;
        RequestBody requestBody = null;
        if (userData != null) {
            if (userData instanceof HashMap) {
                
                builder.addHeader("Content-Type", "application/json; charset=utf-8");
                HashMap<String, Object> data = (HashMap) userData;

                if (!isGet) {
                    for (String key : data.keySet()) {
                        Object value = data.get(key);

                        if (value != null) {
                            // if the value is a proxy, we need to get the
                            // actual file object
                            if (value instanceof TiFileProxy) {
                                value = ((TiFileProxy) value).getBaseFile();
                            }

                            if (value instanceof TiBaseFile
                                    || value instanceof TiBlob) {
                                needMultipart = true;
                                break;
                            }
                        }
                    }
                }

                if (needMultipart) {
                    MultipartBuilder multipart = new MultipartBuilder()
                            .type(MultipartBuilder.FORM);
                    for (String key : data.keySet()) {
                        Object value = data.get(key);
                        if (value instanceof String) {
                            multipart.addFormDataPart(key, (String) value);
                        } else {
                            RequestBody body = requestBodyFromData(value);
                            if (body != null) {
                                multipart.addFormDataPart(key, null, body);
                            }
                        }
                    }
                    requestBody = multipart.build();
                } else {
                    FormEncodingBuilder formBody = new FormEncodingBuilder();
                    for (String key : data.keySet()) {
                        Object value = data.get(key);
                        formBody.add(key, TiConvert.toString(value));
                    }
                    requestBody = formBody.build();
                }
            } else {
                if (userData instanceof String) {
                    builder.addHeader("Content-Type", "charset=utf-8");
                }
                requestBody = requestBodyFromData(userData);
            }
        }

        if (isPost) {
            builder.post(requestBody);
        } else if (isPut) {
            builder.put(requestBody);
        }
        return builder;
    }

    private static RequestBody requestBodyFromData(Object value) {
        if (value instanceof TiBaseFile && !(value instanceof TiResourceFile)) {
            TiBaseFile baseFile = (TiBaseFile) value;
            File file = baseFile.getNativeFile();
            return RequestBody.create(
                    MediaType.parse(TiMimeTypeHelper.getMimeType(file)), file);
        } else if (value instanceof TiBlob || value instanceof TiResourceFile) {
            try {
                TiBlob blob;
                if (value instanceof TiBlob) {
                    blob = (TiBlob) value;
                } else {
                    blob = ((TiResourceFile) value).read();
                }
                String mimeType = blob.getMimeType();
                File tmpFile = File.createTempFile(
                        "tixhr",
                        "."
                                + TiMimeTypeHelper
                                        .getFileExtensionFromMimeType(mimeType,
                                                "txt"));
                FileOutputStream fos = new FileOutputStream(tmpFile);
                if (blob.getType() == TiBlob.TYPE_STREAM_BASE64) {
                    TiBaseFile.copyStream(blob.getInputStream(),
                            new Base64OutputStream(fos,
                                    android.util.Base64.DEFAULT));
                } else {
                    fos.write(blob.getBytes());
                }
                fos.close();

                return RequestBody.create(MediaType.parse(mimeType), tmpFile);

            } catch (IOException e) {
                Log.e(TAG, "Error adding put data: " + e.getMessage());
            }
        } else {
            return RequestBody.create(MediaType.parse("text/plain"),
                    TiConvert.toString(value));
        }
        return null;
    }
}
