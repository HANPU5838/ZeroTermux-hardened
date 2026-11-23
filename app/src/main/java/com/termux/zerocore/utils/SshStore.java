package com.termux.zerocore.utils;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SSH 连接资料的落盘存储：每条连接一个 .conf 文件，放在 ~/ssh-connect/ 下，
 * 用户可以直接用编辑器改，也可以整个删掉。
 *
 * 文件格式（简单 key=value，便于手改）：
 *   host=1.2.3.4
 *   port=22
 *   user=root
 *   key=/data/data/com.termux/files/home/.ssh/xxx.key
 */
public final class SshStore {

    private SshStore() {
    }

    public static File getDir() {
        return new File(TermuxConstants.TERMUX_HOME_DIR, "ssh-connect");
    }

    public static class Entry {
        public String host = "";
        public String user = "";
        public int port = 22;
        public String key = "";
        public File file;
    }

    /** 读取目录里的全部连接资料（坏文件跳过）。 */
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

    /** 清空全部连接资料，返回是否成功。 */
    public static boolean clearAll() {
        try {
            File dir = getDir();
            File[] files = dir.listFiles();
            if (files == null) {
                return true;
            }
            for (File f : files) {
                if (f != null && f.isFile()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            }
            return true;
        } catch (Exception e) {
            return false;
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
