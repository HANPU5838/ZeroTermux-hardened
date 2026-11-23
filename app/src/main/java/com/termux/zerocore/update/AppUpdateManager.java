package com.termux.zerocore.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.example.xh_lib.utils.LogUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.termux.BuildConfig;
import com.termux.zerocore.http.HTTPIP;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 正式版更新：三个仓库各自 /releases/latest 的版本检测、APK 下载。
 * 不改动原有的下载站逻辑，只负责 GitHub 正式版渠道。
 */
public class AppUpdateManager {

    private static final String TAG = "AppUpdateManager";
    private static final String KEY_MAIN = "main";
    private static final String KEY_API = "api";
    private static final String KEY_X11 = "x11";

    public static class Release {
        public String key;
        public String name;
        public String packageName;
        public String version;       // 远端最新版本号（tag 去掉 v）
        public String installed;     // 本机已安装版本，未安装为 null
        public String publishedAt;
        public String notes;
        public String apkName;
        public String apkUrl;
        public long apkSize;
        public boolean available;    // 远端信息解析成功
        public boolean hasUpdate;
        public String repoUrl;
    }

    public interface FetchCallback {
        void onResult(List<Release> releases);
        void onError(String message);
    }

    public interface DownloadCallback {
        void onProgress(int percent, long downloaded, long total);
        void onSuccess(File file);
        void onError(String message);
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);

    public static void fetchAll(final Context context, final FetchCallback callback) {
        POOL.execute(() -> {
            final List<Release> result = new ArrayList<>();
            final String[] keys = {KEY_MAIN, KEY_API, KEY_X11};
            final String[] urls = {HTTPIP.GITHUB_VERSION, HTTPIP.GITHUB_VERSION_API, HTTPIP.GITHUB_VERSION_X11};
            final CountDownLatch latch = new CountDownLatch(keys.length);
            final List<String> errors = new ArrayList<>();

            for (int i = 0; i < keys.length; i++) {
                final String key = keys[i];
                final String url = urls[i];
                POOL.execute(() -> {
                    try {
                        Release r = parseRelease(key, fetchJson(url), context);
                        synchronized (result) { result.add(r); }
                    } catch (Exception e) {
                        LogUtils.e(TAG, "fetch " + key + " failed: " + e);
                        synchronized (errors) { errors.add(key); }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sortByKey(result, keys);
            post(() -> {
                if (result.isEmpty()) {
                    callback.onError("all");
                } else {
                    callback.onResult(result);
                }
            });
        });
    }

    private static void sortByKey(List<Release> list, String[] keys) {
        List<Release> sorted = new ArrayList<>();
        for (String k : keys) {
            for (Release r : list) {
                if (k.equals(r.key)) { sorted.add(r); break; }
            }
        }
        list.clear();
        list.addAll(sorted);
    }

    private static Release parseRelease(String key, String json, Context context) {
        Release r = new Release();
        r.key = key;
        switch (key) {
            case KEY_MAIN:
                r.name = "ZeroTermux 主程序";
                r.packageName = context.getPackageName();
                r.repoUrl = "https://github.com/" + HTTPIP.REPO_MAIN;
                break;
            case KEY_API:
                r.name = "Termux:API";
                r.packageName = "com.termux.api";
                r.repoUrl = "https://github.com/" + HTTPIP.REPO_API;
                break;
            default:
                r.name = "Termux:X11";
                r.packageName = "com.termux.x11";
                r.repoUrl = "https://github.com/" + HTTPIP.REPO_X11;
                break;
        }

        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        String tag = obj.has("tag_name") ? obj.get("tag_name").getAsString() : "";
        r.version = normalize(tag);
        r.publishedAt = obj.has("published_at") ? obj.get("published_at").getAsString() : "";
        if (r.publishedAt.length() >= 10) r.publishedAt = r.publishedAt.substring(0, 10);
        r.notes = obj.has("body") && !obj.get("body").isJsonNull()
            ? obj.get("body").getAsString() : "";

        // 按文件名前缀精确匹配本仓库对应的 APK，避免 Release 里混入其它组件时下错包
        final String apkPrefix;
        switch (key) {
            case KEY_MAIN: apkPrefix = "ZeroTermux-"; break;
            case KEY_API:  apkPrefix = "Termux-API"; break;
            default:       apkPrefix = "Termux-X11"; break;
        }
        if (obj.has("assets")) {
            JsonArray assets = obj.getAsJsonArray("assets");
            for (JsonElement e : assets) {
                JsonObject a = e.getAsJsonObject();
                String n = a.has("name") ? a.get("name").getAsString() : "";
                if (n.endsWith(".apk") && n.startsWith(apkPrefix)) {
                    r.apkName = n;
                    r.apkUrl = a.has("browser_download_url")
                        ? a.get("browser_download_url").getAsString() : "";
                    r.apkSize = a.has("size") ? a.get("size").getAsLong() : 0;
                    break;
                }
            }
        }

        r.installed = installedVersion(context, r.packageName);
        if (KEY_MAIN.equals(key) && r.installed == null) {
            r.installed = BuildConfig.VERSION_NAME;
        }
        r.available = !r.version.isEmpty();
        // 只有主程序能可靠地按版本号比较；配套组件和主程序版本号体系不同，不做比较
        r.hasUpdate = KEY_MAIN.equals(key) && r.available
            && compareVersion(r.version, r.installed) > 0;
        return r;
    }

    private static String installedVersion(Context context, String packageName) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(packageName, 0);
            return pi.versionName == null ? "" : pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private static String normalize(String tag) {
        if (tag == null) return "";
        String v = tag.trim();
        if (v.startsWith("v") || v.startsWith("V")) v = v.substring(1);
        return v;
    }

    /** 返回 1 表示 a 更新，-1 表示 b 更新，0 相等。按点分段，数字段按数值比。 */
    public static int compareVersion(String a, String b) {
        if (a == null || a.isEmpty()) return -1;
        if (b == null || b.isEmpty()) return 1;
        String[] pa = a.split("[.\\-]");
        String[] pb = b.split("[.\\-]");
        int n = Math.max(pa.length, pb.length);
        for (int i = 0; i < n; i++) {
            String sa = i < pa.length ? pa[i] : "0";
            String sb = i < pb.length ? pb[i] : "0";
            try {
                long ia = Long.parseLong(sa);
                long ib = Long.parseLong(sb);
                if (ia != ib) return ia > ib ? 1 : -1;
            } catch (NumberFormatException e) {
                int c = sa.compareToIgnoreCase(sb);
                if (c != 0) return c > 0 ? 1 : -1;
            }
        }
        return 0;
    }

    public static String formatSize(long bytes) {
        if (bytes <= 0) return "";
        double mb = bytes / 1048576.0;
        return String.format("%.1f MB", mb);
    }

    private static long parseContentLength(String value) {
        if (value == null) return -1;
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException e) { return -1; }
    }

    public static void download(final Context context, final Release release,
                                final DownloadCallback callback) {
        POOL.execute(() -> {
            HttpURLConnection conn = null;
            InputStream in = null;
            OutputStream out = null;
            File target = null;
            try {
                File base = context.getExternalFilesDir(null);
                if (base == null) base = context.getCacheDir();
                File dir = new File(base, "update");
                if (!dir.exists() && !dir.mkdirs()) {
                    throw new java.io.IOException("cannot create download dir");
                }
                target = new File(dir, release.apkName);
                File tmp = new File(dir, release.apkName + ".part");
                if (tmp.exists()) tmp.delete();

                URL url = new URL(release.apkUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(60000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "ZeroTermux-Updater");
                conn.connect();
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) {
                    throw new java.io.IOException("HTTP " + code);
                }
                long total = parseContentLength(conn.getHeaderField("Content-Length"));
                in = conn.getInputStream();
                out = new FileOutputStream(tmp);
                byte[] buf = new byte[16384];
                long done = 0;
                int lastPercent = -1;
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                    done += len;
                    if (total > 0) {
                        int percent = (int) (done * 100 / total);
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            final int p = percent;
                            final long d = done;
                            post(() -> callback.onProgress(p, d, total));
                        }
                    }
                }
                out.flush();
                out.close();
                out = null;
                in.close();
                in = null;
                if (target.exists()) target.delete();
                if (!tmp.renameTo(target)) {
                    throw new java.io.IOException("rename failed");
                }
                final File ok = target;
                post(() -> callback.onSuccess(ok));
            } catch (Exception e) {
                LogUtils.e(TAG, "download failed: " + e);
                final String msg = e.getMessage() == null ? "error" : e.getMessage();
                post(() -> callback.onError(msg));
            } finally {
                if (in != null) try { in.close(); } catch (Exception ignored) {}
                if (out != null) try { out.close(); } catch (Exception ignored) {}
                if (conn != null) conn.disconnect();
            }
        });
    }

    private static String fetchJson(String urlString) throws Exception {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "ZeroTermux-Updater");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.connect();
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new java.io.IOException("HTTP " + code);
            }
            in = conn.getInputStream();
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) bos.write(buf, 0, len);
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            if (in != null) try { in.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }

    private static void post(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }
}
