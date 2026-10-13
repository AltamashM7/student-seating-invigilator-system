package com.examplanner.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class JsonUtil {
    public static String json(Object value) {
        if(value==null) return "null";
        if(value instanceof Number || value instanceof Boolean) return value.toString();
        if(value instanceof Map<?,?> map) {
            StringJoiner j=new StringJoiner(",","{","}");
            map.forEach((k,v)->j.add(quote(k.toString())+":"+json(v))); return j.toString();
        }
        if(value instanceof Iterable<?> list) { StringJoiner j=new StringJoiner(",","[","]"); for(Object v:list) j.add(json(v)); return j.toString(); }
        return quote(value.toString());
    }
    public static String quote(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + escape(value) + "\"";
    }

    public static String message(String key, String value) {
        return "{\"" + escape(key) + "\":" + quote(value) + "}";
    }

    public static void send(HttpServletResponse response, int status, String json) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        StringBuilder b=new StringBuilder();
        for(char c:value.toCharArray()) {
            switch(c) {
                case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r"); case '\t' -> b.append("\\t");
                default -> { if(c<32) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
            }
        }
        return b.toString();
    }
}
