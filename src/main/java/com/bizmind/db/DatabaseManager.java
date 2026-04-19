package com.bizmind.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseManager {

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() throws Exception {
        Properties props = new Properties();
        InputStream in = getClass().getResourceAsStream("/db.properties");
        if (in == null) throw new RuntimeException("db.properties not found in classpath");
        props.load(in);
        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        connection = DriverManager.getConnection(url, user, password);
    }

    public static DatabaseManager getInstance() throws Exception {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
