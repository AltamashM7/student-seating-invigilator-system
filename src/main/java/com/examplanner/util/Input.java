package com.examplanner.util;
import jakarta.servlet.http.HttpServletRequest;
public class Input {
    public static String text(HttpServletRequest r,String key,int max) {
        String v=r.getParameter(key);
        if(v==null && "resource".equals(key) && r.getAttribute(key)!=null) v=r.getAttribute(key).toString();
        if(v==null || v.trim().isEmpty() || v.trim().length()>max) throw new IllegalArgumentException("Valid "+key+" is required (maximum "+max+" characters)");
        return v.trim();
    }
    public static int integer(HttpServletRequest r,String key,int min,int max) {
        try { int v=Integer.parseInt(r.getParameter(key)); if(v<min || v>max) throw new NumberFormatException(); return v; }
        catch(NumberFormatException e) { throw new IllegalArgumentException("Invalid "+key); }
    }
    public static int id(HttpServletRequest r,String key) { return integer(r,key,1,Integer.MAX_VALUE); }
    public static boolean flag(HttpServletRequest r,String key) { return "true".equals(r.getParameter(key)) || "1".equals(r.getParameter(key)); }
}
