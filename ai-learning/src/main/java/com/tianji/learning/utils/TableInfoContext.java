package com.tianji.learning.utils;

import org.springframework.stereotype.Component;

@Component
public class TableInfoContext {
    private static final ThreadLocal<String> TL = new ThreadLocal<>();
    public static void setInfo(String tableName){
        TL.set(tableName);
    }
    public static String getInfo(){
        return TL.get();
    }
    public static void remove(){
        TL.remove();
    }
}
