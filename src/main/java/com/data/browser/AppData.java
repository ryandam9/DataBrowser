package com.data.browser;

import com.dbutils.common.ColumnDetail;
import com.dbutils.common.TableDetail;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppData {
    public static String dbSelection;
    public static String user;
    public static String password;
    public static String host;
    public static String service;
    public static String port;
    public static Map<String, Map<String, Map<TableDetail, List<ColumnDetail>>>> tables = new HashMap<>();
    public static Map<String, List<String>> schemas;
    public static PreparedStatement sqlStatement;

    // Constants
    public static final String ORACLE = "Oracle";
    public static final String SQL_SERVER = "SQL Server";
}
