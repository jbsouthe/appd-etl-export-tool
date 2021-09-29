package com.cisco.josouthe.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database {
    private static final Logger logger = LogManager.getFormatterLogger();

    String connectionString, user, password, defaultTable;

    public Database( String connectionString, String user, String password, String defaultTable) {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
        this.defaultTable = defaultTable;
        logger.info("Testing Database connection returned: "+ isDatabaseAvailable());
    }

    public boolean isDatabaseAvailable() {
        Connection conn = null;
        try{
            conn = DriverManager.getConnection( this.connectionString, this.user, this.password);
            if( conn != null ) return true;
        } catch (Exception exception) {
            logger.error("Error testing database connection settings, Exception: %s", exception.toString());
        } finally {
            if( conn != null ) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
        return false;
    }
}
