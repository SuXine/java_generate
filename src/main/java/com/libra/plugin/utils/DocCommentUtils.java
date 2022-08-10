package com.libra.plugin.utils;

public class DocCommentUtils {
    public static String splitDocCommentText(String str) {
        if (str == null)
            return "";
        int strStartIndex = str.indexOf("/**\n");
        int strEndIndex = str.indexOf("\n", str.indexOf("\n") + 1);
        if (strStartIndex < 0)
            return "";
        if (strEndIndex < 0)
            return "";
        String substring = str.substring(strStartIndex, strEndIndex).substring("/**\n".length());
        substring = substring.replace("*", "");
        substring = substring.replace("/", "");
        substring = substring.replace("\n", " ");
        return substring.trim();
    }
}