package ru.mail.polis.dao.daomodel;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Vector;

import javax.sql.DataSource;

import org.jetbrains.annotations.Nullable;

public class DBConnectionPool {
    private DataSource dataSource;

    private Vector<Connection> availableConns;

    DBConnectionPool(DataSource dataSource) throws SQLException {
        this.dataSource = dataSource;
        this.availableConns = new Vector<>();
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public synchronized Connection retrieve() throws SQLException {
        Connection newConn;
        if (availableConns.size() == 0) {
            newConn = getConnection();
        } else {
            newConn = availableConns.lastElement();
            availableConns.removeElement(newConn);
        }
        return newConn;
    }

    public synchronized void putback(@Nullable Connection c) throws NullPointerException {
        if (c != null) {
            availableConns.addElement(c);
        }
    }
}
