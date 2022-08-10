package com.libra.plugin.utils;

import java.text.SimpleDateFormat;
import java.util.Date;


public class DateUtils {
    public static String getNowDateString() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        return format.format(new Date());
    }
}


