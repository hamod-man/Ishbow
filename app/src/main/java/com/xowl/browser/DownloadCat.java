package com.xowl.browser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** تصنيف الملفات المنزّلة حسب الصيغة، وكل تصنيف له مجلد خاص داخل Download/Ishbow */
public enum DownloadCat {
    PDF("PDF", "ملفات PDF", "📕"),
    IMAGES("Images", "الصور", "🖼"),
    VIDEOS("Videos", "الفيديوهات", "🎬"),
    DOCS("Documents", "المستندات والملفات الأخرى", "📁");

    public final String folder, label, icon;

    DownloadCat(String folder, String label, String icon) {
        this.folder = folder;
        this.label = label;
        this.icon = icon;
    }

    private static final Set<String> IMG = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic", "heif", "avif", "ico", "tiff"));
    private static final Set<String> VID = new HashSet<>(Arrays.asList(
            "mp4", "mkv", "webm", "avi", "mov", "3gp", "flv", "m4v", "ts", "wmv", "mpeg", "mpg"));

    public static DownloadCat of(String name, String mime) {
        String ext = "";
        if (name != null) {
            int i = name.lastIndexOf('.');
            if (i >= 0) ext = name.substring(i + 1).toLowerCase(Locale.ROOT);
        }
        if (ext.equals("pdf")) return PDF;
        if (IMG.contains(ext)) return IMAGES;
        if (VID.contains(ext)) return VIDEOS;
        if (mime != null) {
            if (mime.equals("application/pdf")) return PDF;
            if (mime.startsWith("image/")) return IMAGES;
            if (mime.startsWith("video/")) return VIDEOS;
        }
        return DOCS;
    }

    public static DownloadCat fromFolder(String f) {
        for (DownloadCat c : values()) if (c.folder.equalsIgnoreCase(f)) return c;
        return null;
    }
}
