package com.termux.zerocore.http_service;

import static android.os.Build.VERSION_CODES.R;

import android.os.Environment;
import android.util.Log;

import com.example.xh_lib.utils.UUtils;
import com.termux.R;
import com.termux.shared.termux.TermuxConstants;
import com.termux.zerocore.url.FileUrl;
import com.termux.zerocore.utils.FileIOUtils;
import com.termux.zerocore.control.ControlBootstrap;
import com.termux.zerocore.control.ControlEventStream;
import com.termux.zerocore.control.ControlGateway;
import com.termux.zerocore.control.ControlRequest;
import com.termux.zerocore.control.ControlResponse;
import com.termux.zerocore.control.ControlRouter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.util.Streams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * Created by wangmingxing on 2017/6/20.
 */

public class HttpServer extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private static final String MIME_JSON = "application/json; charset=utf-8";
    private NanoFileUpload mFileUpload;
    private OnStatusUpdateListener mStatusUpdateListener;

    interface OnStatusUpdateListener {
        void onUploadingProgressUpdate(int progress);
        void onUploadingFile(File file, boolean done);
        void onDownloadingFile(File file, boolean done);
    }

    class DownloadResponse extends Response {
        private File downloadFile;

        DownloadResponse(File downloadFile, InputStream stream) {
            super(Response.Status.OK, "application/octet-stream", stream, downloadFile.length());
            this.downloadFile = downloadFile;
        }

        @Override
        protected void send(OutputStream outputStream) {
            super.send(outputStream);
            if (mStatusUpdateListener != null) {
                mStatusUpdateListener.onDownloadingFile(downloadFile, true);
            }
        }
    }

    public HttpServer(int port) {
        super(port);
        try {
            ControlBootstrap.ensureRegistered(UUtils.getContext());
        } catch (Throwable ignored) {
            // context may not be ready yet; handlers resolve it lazily instead
        }
        mFileUpload = new NanoFileUpload(new DiskFileItemFactory());
        mFileUpload.setProgressListener(new ProgressListener() {
            int progress = 0;
            @Override
            public void update(long pBytesRead, long pContentLength, int pItems) {
                //Log.d(TAG, pBytesRead + " bytes has been read, totol " + pContentLength + " bytes");
                if (mStatusUpdateListener != null) {
                    int p = (int) (pBytesRead * 100 / pContentLength);
                    if (p != progress) {
                        progress = p;
                        mStatusUpdateListener.onUploadingProgressUpdate(progress);
                    }
                }
            }
        });
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        Map<String, String> header = session.getHeaders();
        Map<String, String> parms = session.getParms();
        String answer = "Success!";
        Log.d(TAG, "uri=" + uri);
        Log.d(TAG, "method=" + method);
        Log.d(TAG, "header=" + header);
        Log.d(TAG, "params=" + parms);

        // Unified control plane endpoints (strictly additive). If a real file or
        // directory already lives at the same path we fall through to the legacy
        // file browser, so existing behaviour is preserved byte for byte.
        if (uri.startsWith("/control/") || uri.equals("/openapi.json") || uri.equals("/mcp")) {
            boolean shadowsExistingPath = false;
            try {
                shadowsExistingPath = new File(FileUrl.INSTANCE.getMainFilesUrl() + uri).exists();
            } catch (Throwable ignored) {
            }
            if (!shadowsExistingPath) {
                return serveControl(session);
            }
        }

        // for file upload
        if (NanoFileUpload.isMultipartContent(session)) {
            try {
                FileItemIterator iterator = mFileUpload.getItemIterator(session);
                while (iterator.hasNext()) {
                    FileItemStream item = iterator.next();
                    String name = item.getFieldName();
                    InputStream inputStream = item.openStream();
                    if (item.isFormField()) {
                        Log.d(TAG, "Item is form filed, name=" +
                            name + ",value=" + Streams.asString(inputStream));
                    } else {
                        String fileName = item.getName();
                        Log.d(TAG, "Item is file field, name=" + name + ",fileName=" + fileName);

                        File file = new File(Environment.getExternalStoragePublicDirectory(
                            Environment.DIRECTORY_DOWNLOADS), fileName);
                        String path = file.getAbsolutePath();
                        Log.d(TAG, "Save file to " + path);
                        if (mStatusUpdateListener != null) {
                            mStatusUpdateListener.onUploadingFile(file, false);
                        }

                        FileOutputStream fos = new FileOutputStream(file);
                        Streams.copy(inputStream, fos, true);
                        if (mStatusUpdateListener != null) {
                            mStatusUpdateListener.onUploadingFile(file, true);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (method.equals(Method.GET)) {
            // for file browse and download
            File rootFile = new File(FileUrl.INSTANCE.getMainFilesUrl());
            uri = uri.replace(rootFile.getAbsolutePath(), "");
            rootFile = new File(rootFile + uri);
            if (!rootFile.exists()) {
                return newFixedLengthResponse("Error! No such file or dirctory");
            }

            if (rootFile.isDirectory()) {
                // list directory files
                Log.d(TAG, "list " + rootFile.getPath());
                File[] files = rootFile.listFiles();
                File file1 = new File(FileUrl.INSTANCE.getMainFilesUrl(),  "css1.css");
                answer = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; " +
                    "charset=utf-8\"><title> ZeroTermux File Browser</title><link rel=\"stylesheet\" href=\"" + file1.getAbsolutePath() + "\"  type=\"text/css\"/>" +
                    "<div id=\"title111\">ZeroTermux File Browser</div>";

                if (files != null && files.length > 0) {
                    Arrays.sort(
                        files,
                        new FileComparator()
                    );
                }
              /*  answer += "<a id=\"btn2\" href=\"" + rootFile.getParentFile().getAbsolutePath()
                    + "\" alt = \"\">" + UUtils.getString(com.termux.R.string.open_ttyd_http)
                    + "</a>";
                answer += "<a id=\"btn2\" href=\"" + rootFile.getParentFile().getAbsolutePath()
                    + "\" alt = \"\">" + UUtils.getString(com.termux.R.string.open_file_http)
                    + "</a>";*/
                answer += "<a id=\"btn2\" href=\"" + rootFile.getParentFile().getAbsolutePath()
                    + "\" alt = \"\">..."
                    + "</a>";
                for (File file : files) {

                    String isFile = UUtils.getString(com.termux.R.string.http_folder);
                    String isFileLink = "";
                    if (file.isDirectory()) {
                        isFile = UUtils.getString(com.termux.R.string.http_folder);
                        isFileLink = file.getName() + "/";
                    } else {
                        isFile = UUtils.getString(com.termux.R.string.http_file);
                        isFileLink = file.getName();
                    }
                    answer += "<a id=\"btn2\" href=\"" + file.getAbsolutePath()
                        + "\" alt = \"\">" + isFileLink
                        + "</a>";
                    /**
                     *
                     * <br>[time: "
                     *                         +  UUtils.getFtpDate(file.lastModified())
                     *                         + "] [size:" + FileIOUtils.INSTANCE.formatFileSize(file.length())
                     *                         + "] [type:" + isFile
                     *                         + "]<br>
                     *
                     */
                }

                answer += "</head></html>";
            } else {
                // serve file download
                InputStream inputStream;
                Response response = null;
                Log.d(TAG, "downloading file " + rootFile.getAbsolutePath());
                if (mStatusUpdateListener != null) {
                    mStatusUpdateListener.onDownloadingFile(rootFile, false);
                }

                try {
                    inputStream = new FileInputStream(rootFile);
                    response = new DownloadResponse(rootFile, inputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response != null) {
                    response.addHeader(
                        "Content-Disposition", "attachment; filename=" + rootFile.getName());
                    return response;
                } else {
                    return newFixedLengthResponse("Error downloading file!");
                }
            }
        }

        return newFixedLengthResponse(answer);
    }


    /**
     * Minimal control-plane surface on the file HTTP line. The JSON model is the
     * same as the AI debug server, so any line can drive the shared registry.
     */
    private Response serveControl(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        try {
            if (uri.equals("/openapi.json") && method.equals(Method.GET)) {
                return jsonResponse(ControlGateway.openApiString(UUtils.getContext()));
            }
            if (uri.equals("/control/capabilities") && method.equals(Method.GET)) {
                return jsonResponse(ControlGateway.capabilitiesString(UUtils.getContext()));
            }
            if (uri.equals("/control/audit") && method.equals(Method.GET)) {
                int limit = 50;
                String limitParam = session.getParms().get("limit");
                if (limitParam != null) {
                    try {
                        limit = Integer.parseInt(limitParam);
                    } catch (NumberFormatException ignored) {
                    }
                }
                return jsonResponse(ControlGateway.auditString(limit));
            }
            if (uri.equals("/control/invoke") && (method.equals(Method.POST) || method.equals(Method.GET))) {
                JsonObject body = new JsonObject();
                if (method.equals(Method.POST)) {
                    Map<String, String> files = new HashMap<>();
                    session.parseBody(files);
                    String postData = files.get("postData");
                    if (postData != null && !postData.trim().isEmpty()) {
                        try {
                            body = JsonParser.parseString(postData).getAsJsonObject();
                        } catch (Exception ignored) {
                        }
                    }
                } else {
                    Map<String, String> query = session.getParms();
                    body.addProperty("id", query.get("id"));
                    body.addProperty("capability", query.get("capability"));
                    body.addProperty("action", query.get("action"));
                }
                ControlResponse response = ControlRouter.get().route(
                    ControlRequest.fromJson(body), "file-http");
                return jsonResponse(response.toJsonString());
            }
            if (uri.equals("/mcp") && method.equals(Method.POST)) {
                Map<String, String> files = new HashMap<>();
                session.parseBody(files);
                String postData = files.get("postData");
                JsonObject request = JsonParser.parseString(
                    postData == null || postData.trim().isEmpty() ? "{}" : postData).getAsJsonObject();
                JsonObject response = ControlGateway.handleMcp(UUtils.getContext(), request);
                if (response == null) {
                    return newFixedLengthResponse(Response.Status.ACCEPTED, MIME_JSON, "");
                }
                return jsonResponse(response.toString());
            }
            if (uri.equals("/control/events") && method.equals(Method.GET)) {
                Response response = newChunkedResponse(
                    Response.Status.OK, "text/event-stream; charset=utf-8", new ControlEventStream());
                response.addHeader("Cache-Control", "no-cache");
                response.addHeader("X-Accel-Buffering", "no");
                return response;
            }
        } catch (Exception e) {
            return jsonResponse("{\"ok\":false,\"error\":\"" + e.getMessage() + "\"}");
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_JSON,
            "{\"ok\":false,\"error\":\"not found\"}");
    }

    private Response jsonResponse(String body) {
        Response response = newFixedLengthResponse(Response.Status.OK, MIME_JSON, body);
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");
        return response;
    }

    public void setOnStatusUpdateListener(OnStatusUpdateListener listener) {
        mStatusUpdateListener = listener;
    }
}
