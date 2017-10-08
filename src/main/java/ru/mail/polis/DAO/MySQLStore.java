package ru.mail.polis.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Vector;

import org.jetbrains.annotations.Nullable;

public class MySQLStore {
    private static final String DB_URL = "jdbc:mysql://localhost:8889/kv_storage?useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASS = "root";
    private final int INITIAL_CONN_COUNT = 10;

    private Vector<Connection> availableConns;
    private Vector<Connection> usedConns;

    public MySQLStore(){
        availableConns = new Vector<>();
        usedConns = new Vector<>();

        for (int i = 0; i < INITIAL_CONN_COUNT; i++) {
            availableConns.addElement(getConnection());
        }
    }

    private Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return conn;
    }

    public synchronized Connection retrieve() throws SQLException {
        Connection newConn;
        if (availableConns.size() == 0) {
            newConn = getConnection();
        } else {
            newConn = availableConns.lastElement();
            availableConns.removeElement(newConn);
        }
        usedConns.addElement(newConn);
        return newConn;
    }

    public synchronized void putback(@Nullable Connection c) throws NullPointerException {
        if (c != null) {
            if (usedConns.removeElement(c)) {
                availableConns.addElement(c);
            } else {
                throw new NullPointerException("Connection not in the usedConns array");
            }
        }
    }
}
