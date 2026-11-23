package com.termux.zerocore.utils;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH 连接资料与私钥的独立存储。
 *
 * 位置：应用私有目录下的 ssh-connect/（不在 home 里，也不和用户文件混在一起）：
 *   <filesDir>/ssh-connect/*.conf    连接资料
 *   <filesDir>/ssh-connect/keys/*.key 私钥
 *
 * 文件格式（简单 key=value，便于排查与备份）：
 *   host=1.2.3.4
 *   port=22
 *   user=root
 *   key=<filesDir>/ssh-connect/keys/xxx.key
 *
 * 首次使用会把旧版 ~/ssh-connect 里的资料和 ~/.ssh/zt_server_*.key 迁移过来。
 */
public final class SshStore {

    private static final String DIR_NAME = "ssh-connect";
    private static final String KEY_DIR_NAME = "keys";

    private static Context appContext;

    private SshStore() {
    }

    /** 由 UI 在打开弹窗时调用一次。 */
    public static void attach(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    public static File getDir() {
        if (appContext != null) {
            return new File(appContext.getFilesDir(), DIR_NAME);
        }
        // 兜底：没有上下文时退回旧位置，保证不崩
        return new File(TermuxConstants.TERMUX_HOME_DIR, DIR_NAME);
    }

    public static File getKeyDir() {
        File dir = new File(getDir(), KEY_DIR_NAME);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    public static class Entry {
        public String host = "";
        public String user = "";
        public int port = 22;
        public String key = "";
        public File file;
    }

    /** 读取全部连接资料（坏文件跳过）。 */
    public static List<Entry> list() {
        List<Entry> result = new ArrayList<>();
        File dir = getDir();
        if (!dir.exists() && !dir.mkdirs()) {
            return result;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return result;
        }
        for (File f : files) {
            if (f == null || !f.isFile() || !f.getName().endsWith(".conf")) {
                continue;
            }
            Entry e = parse(f);
            if (e != null) {
                result.add(e);
            }
        }
        return result;
    }

    public static void save(String host, String user, int port, String keyPath) {
        try {
            File dir = getDir();
            if (!dir.exists() && !dir.mkdirs()) {
                return;
            }
            String name = (user + "@" + host + "_" + port)
                .replaceAll("[^a-zA-Z0-9@_.-]", "_") + ".conf";
            File file = new File(dir, name);
            StringBuilder sb = new StringBuilder();
            sb.append("host=").append(host).append('\n');
            sb.append("port=").append(port).append('\n');
            sb.append("user=").append(user).append('\n');
            sb.append("key=").append(keyPath == null ? "" : keyPath).append('\n');
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(sb.toString().getBytes("UTF-8"));
            }
        } catch (Exception ignored) {
        }
    }

    /** 删除某条连接资料。 */
    public static boolean delete(Entry entry) {
        try {
            return entry != null && entry.file != null && entry.file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 旧数据迁移：~/ssh-connect 的 conf、~/.ssh/zt_server_*.key 搬到新目录。
     * 幂等，只在目标目录为空时执行。
     */
    public static void migrateIfNeeded() {
        try {
            File newDir = getDir();
            File oldDir = new File(TermuxConstants.TERMUX_HOME_DIR, DIR_NAME);
            if (oldDir.equals(newDir) || !oldDir.isDirectory()) {
                return;
            }
            if (!newDir.exists() && !newDir.mkdirs()) {
                return;
            }
            File[] existing = newDir.listFiles();
            if (existing != null && existing.length > 0) {
                return;
            }
            File[] oldFiles = oldDir.listFiles();
            if (oldFiles == null) {
                return;
            }
            for (File src : oldFiles) {
                if (src == null || !src.isFile()) {
                    continue;
                }
                File dst = new File(newDir, src.getName());
                copy(src, dst);
                //noinspection ResultOfMethodCallIgnored
                src.delete();
            }
            //noinspection ResultOfMethodCallIgnored
            oldDir.delete();
            migrateKeys();
        } catch (Exception ignored) {
        }
    }

    private static void migrateKeys() {
        try {
            File sshDir = new File(TermuxConstants.TERMUX_HOME_DIR, ".ssh");
            File[] keys = sshDir.listFiles();
            if (keys == null) {
                return;
            }
            File keyDir = getKeyDir();
            for (File src : keys) {
                if (src == null || !src.isFile() || !src.getName().startsWith("zt_server_")) {
                    continue;
                }
                File dst = new File(keyDir, src.getName());
                copy(src, dst);
                //noinspection ResultOfMethodCallIgnored
                src.delete();
            }
        } catch (Exception ignored) {
        }
    }

    private static void copy(File src, File dst) {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            //noinspection ResultOfMethodCallIgnored
            dst.setReadable(true, true);
            //noinspection ResultOfMethodCallIgnored
            dst.setWritable(true, true);
        } catch (Exception ignored) {
        }
    }

    private static Entry parse(File file) {
        try {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(new java.io.FileInputStream(file), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            Entry e = new Entry();
            e.file = file;
            for (String line : sb.toString().split("\n")) {
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String k = line.substring(0, idx).trim();
                String v = line.substring(idx + 1).trim();
                switch (k) {
                    case "host": e.host = v; break;
                    case "user": e.user = v; break;
                    case "port":
                        try {
                            e.port = Integer.parseInt(v);
                        } catch (Exception ignored) {
                        }
                        break;
                    case "key": e.key = v; break;
                    default:
                        break;
                }
            }
            if (e.host.isEmpty() || e.user.isEmpty()) {
                return null;
            }
            return e;
        } catch (Exception e) {
            return null;
        }
    }
}
