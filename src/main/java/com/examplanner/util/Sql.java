package com.examplanner.util;
import com.examplanner.config.DbConnection;
import java.sql.*;
import java.util.*;

public class Sql {
    public static List<Map<String,Object>> rows(Connection c,String sql,Object... values) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) {
            bind(s,values);
            try(ResultSet rs=s.executeQuery()) {
                List<Map<String,Object>> result=new ArrayList<>();
                ResultSetMetaData m=rs.getMetaData();
                while(rs.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=m.getColumnCount();i++) row.put(m.getColumnLabel(i),rs.getObject(i));
                    result.add(row);
                }
                return result;
            }
        }
    }
    public static Map<String,Object> one(Connection c,String sql,Object... values) throws SQLException {
        List<Map<String,Object>> list=rows(c,sql,values);
        if(list.isEmpty()) throw new IllegalArgumentException("Record not found");
        return list.get(0);
    }
    public static int update(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)) { bind(s,args); return s.executeUpdate(); }
    }
    public static int insert(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(s,args); s.executeUpdate();
            try(ResultSet rs=s.getGeneratedKeys()) { if(!rs.next()) throw new SQLException("Missing generated id"); return rs.getInt(1); }
        }
    }
    private static void bind(PreparedStatement s,Object[] values) throws SQLException {
        for(int i=0;i<values.length;i++) s.setObject(i+1,values[i]);
    }
    @FunctionalInterface public interface Work<T> { T run(Connection c) throws SQLException; }
    public static <T> T write(Work<T> work) throws SQLException {
        try(Connection c=DbConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                // Validation and mutation share one lock and transaction in this small application.
                one(c,"SELECT id FROM planner_lock WHERE id=1 FOR UPDATE");
                T result=work.run(c); c.commit(); return result;
            } catch(SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }
    public static int number(Map<String,Object> row,String key) { return ((Number)row.get(key)).intValue(); }
    public static boolean flag(Map<String,Object> row,String key) {
        Object v=row.get(key); return Boolean.TRUE.equals(v) || (v instanceof Number && ((Number)v).intValue()!=0);
    }
}
