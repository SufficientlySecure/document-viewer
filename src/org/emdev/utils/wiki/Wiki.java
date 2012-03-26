package org.emdev.utils.wiki;

import java.util.StringTokenizer;

import org.emdev.utils.LengthUtils;

public class Wiki {

    public static CharSequence fromWiki(String text) {
        StringBuilder buf = new StringBuilder("<html><body>");

        StringTokenizer st = new StringTokenizer(text, "\n\r");
        int listLevel = 0;
        while (st.hasMoreElements()) {
            String s = (String) st.nextElement();
            String trimmed = s.trim();
            if (LengthUtils.isEmpty(trimmed)) {
                if (listLevel > 0) {
                    do {
                        buf.append("</ul>");
                    } while (--listLevel > 0);
                    buf.append("\n");
                }
                buf.append("<br>");
                buf.append("\n");
                continue;
            } else if (trimmed.startsWith("#")) {
                continue;
            } else if (trimmed.startsWith("=")) {
                if (listLevel > 0) {
                    do {
                        buf.append("</ul>");
                    } while (--listLevel > 0);
                    buf.append("\n");
                }
                int count = 1;
                for (int i = 1; i < s.length() && s.charAt(i) == '='; i++, count++) {
                }
                int end = s.indexOf("=", count);
                if (end == -1) {
                    end = s.length();
                }
                buf.append("<h").append(count).append(">");
                buf.append(s.substring(count, end).trim());
                buf.append("</h").append(count).append("/>");
                buf.append("\n");
            } else if (trimmed.startsWith("*")) {
                int count = s.indexOf("*");
                if (count > listLevel) {
                    do {
                        buf.append("<ul>");
                    } while (count > ++listLevel);
                    buf.append("\n");
                } else if (count < listLevel) {
                    do {
                        buf.append("</ul>");
                    } while (count < --listLevel);
                    buf.append("\n");
                }
                buf.append("<li>");
                buf.append(trimmed.substring(1).trim());
                buf.append("</li>");
                buf.append("\n");
            } else {
                while (listLevel > 0) {
                    buf.append("</ul>");
                    listLevel--;
                }
                buf.append("<br>");
                buf.append(trimmed);
            }
        }

        String content = buf.append("</body></html>").toString();
        // System.out.println(content);
        return content;
    }
}
