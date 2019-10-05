package com.data.browser.db;

import com.dbutils.common.DBConnections;

import java.sql.Connection;
import java.util.concurrent.Callable;

import static com.data.browser.Utils.logStackTrace;

public class DBConnectionTask implements Callable<Connection> {
    String user;
    String password;
    String host;
    String service;
    String port;
    String dbType;

    public DBConnectionTask(String user, String host, String port, String dbType) {
        this.user = user;
        this.host = host;
        this.port = port;
        this.dbType = dbType;
    }

    public DBConnectionTask(String user, String password, String host, String service, String port, String dbType) {
        this.user = user;
        this.password = password;
        this.host = host;
        this.service = service;
        this.port = port;
        this.dbType = dbType;
    }

    @Override
    public Connection call() throws Exception {
        Connection connection = null;

        if (dbType.equals("Oracle")) {
            try {
                connection = DBConnections.getOracleConnection(user, password, host, service, port);
                return connection;
            } catch (Exception ex) {
                logStackTrace(ex);
            }
        }

        if (dbType.equals("SQL Server")) {
            try {
                connection = DBConnections.getSqlServerConnection(user, host, port);
                return connection;
            } catch (Exception ex) {
                logStackTrace(ex);
            }
        }

        return connection;
    }
}
