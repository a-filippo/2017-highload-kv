package ru.mail.polis.dao.daomodel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.h2.jdbcx.JdbcDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class H2DAOModel implements DAOModel {
    private DBConnectionPool connectionPool;

    private static final String TABLE_STORAGE = "STORAGE";
    private static final String COL_KEY = "storage_key";
    private static final String COL_VALUE = "storage_value";
    private static final String COL_PATH = "storage_path";
    private static final String COL_TIMESTAMP = "storage_timestamp";
    private static final String COL_SIZE = "storage_value_size";

    private PreparedStatementStore getRowPreparedStatementStore;
    private PreparedStatementStore updateRowPreparedStatementStore;
    private PreparedStatementStore insertRowPreparedStatementStore;
    private PreparedStatementStore getPathRowPreparedStatementStore;

    public H2DAOModel(String dbPath) throws SQLException {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e){
            throw new SQLException();
        }

        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:" + dbPath + "");

        connectionPool = new DBConnectionPool(ds);

        createTable();
        prepareStatements();
    }

    @Override
    public void stop() throws IOException {
        try {
            connectionPool.retrieve().createStatement().execute("SHUTDOWN");
        } catch (SQLException e){
            String state = e.getSQLState();
            // 08006 connection with db closed
            // XJ004 db not found
            if (!state.equals("08006") &&
                !state.equals("XJ004")){
                e.printStackTrace();
                throw new IOException();
            }
        }
    }

    private void createTable() throws SQLException {
        Connection connection = connectionPool.retrieve();

        DatabaseMetaData databaseMetaData = connection.getMetaData();

        ResultSet resultSet = databaseMetaData.getTables(null, null, TABLE_STORAGE, null);
        if (!resultSet.next()){
            Statement statement = connection.createStatement();
            statement.execute(
            "CREATE TABLE "+TABLE_STORAGE+"(" +
                    COL_KEY+" VARCHAR (256) NOT NULL, " +
                    COL_VALUE+" blob (65536), " +
                    COL_PATH+" VARCHAR (256) NOT NULL, " +
                    COL_TIMESTAMP+" BIGINT NOT NULL, " +
                    COL_SIZE+" int NOT NULL" +
                    ")"
            );

            statement.execute("ALTER TABLE " + TABLE_STORAGE + " ADD PRIMARY KEY (" + COL_KEY + ")");
            statement.close();
        }

        connectionPool.putback(connection);
    }

    @Nullable
    @Override
    public DAOModelValue getValue(@NotNull String key) throws IOException{
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getRowPreparedStatementStore.getStatement();
            preparedStatement.setString(1, key);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()){
                DAOModelValue value = new DAOModelValue(key);
                value.setSize(rs.getInt(COL_SIZE));
                value.setPath(rs.getString(COL_PATH));
                value.setValue(rs.getBytes(COL_VALUE));
                value.setTimestamp(rs.getLong(COL_TIMESTAMP));

                rs.close();
                return value;
            } else {
                return null;
            }
        } catch (SQLException e){
            throw new IOException();
        } finally {
            getRowPreparedStatementStore.putback(preparedStatement);
        }
    }

    @Nullable
    @Override
    public String getPath(@NotNull String key) throws IOException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = getPathRowPreparedStatementStore.getStatement();
            preparedStatement.setString(1, key);
            ResultSet rs = preparedStatement.executeQuery();
            String path = rs.next() ? rs.getString(COL_PATH) : null;

            rs.close();
            return path;
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        } finally {
            getPathRowPreparedStatementStore.putback(preparedStatement);
        }
    }

    @Override
    public void putValue(@NotNull DAOModelValue value, boolean issetInStore) throws IOException {
        PreparedStatementStore preparedStatementStore = issetInStore ? updateRowPreparedStatementStore : insertRowPreparedStatementStore;
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = preparedStatementStore.getStatement();
            preparedStatement.setBytes(1, value.getValue());
            preparedStatement.setInt(2, value.getSize());
            preparedStatement.setLong(3, value.getTimestamp());
            preparedStatement.setString(4, value.getPath());
            preparedStatement.setString(5, value.getKey());
            preparedStatement.executeUpdate();
        } catch (SQLException e){
            if (e.getSQLState().equals("23505")){
                putValue(value, true);
            } else {
                e.printStackTrace();
                throw new IOException();
            }
        } finally {
            preparedStatementStore.putback(preparedStatement);
        }
    }

    @Override
    public void deleteValue(@NotNull String key, long deleteTimestamp) throws IOException{
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = updateRowPreparedStatementStore.getStatement();
            preparedStatement.setBytes(1, new byte[0]);
            preparedStatement.setInt(2, -1);
            preparedStatement.setLong(3, deleteTimestamp);
            preparedStatement.setString(4, "");
            preparedStatement.setString(5, key);
            preparedStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        } finally {
            updateRowPreparedStatementStore.putback(preparedStatement);
        }
    }

    private void prepareStatements() throws SQLException{
        getRowPreparedStatementStore = new PreparedStatementStore(
            "SELECT " +
            COL_VALUE + ", " +
            COL_TIMESTAMP + ", " +
            COL_SIZE + ", " +
            COL_PATH + " from " +
            TABLE_STORAGE + " where " +
            COL_KEY + " = ?"
        );

        updateRowPreparedStatementStore = new PreparedStatementStore(
            "UPDATE " +
            TABLE_STORAGE + " SET " +
            COL_VALUE + " = ?," +
            COL_SIZE + " = ?," +
            COL_TIMESTAMP + " = ?," +
            COL_PATH + " = ? WHERE " +
            COL_KEY + " = ?"
        );

        getPathRowPreparedStatementStore = new PreparedStatementStore(
            "select " +
            COL_PATH + " from " +
            TABLE_STORAGE + " where " +
            COL_KEY + " = ?"
        );

        insertRowPreparedStatementStore = new PreparedStatementStore(
            "INSERT INTO " +
            TABLE_STORAGE + " (" +
            COL_VALUE + ", " +
            COL_SIZE + ", " +
            COL_TIMESTAMP + ", " +
            COL_PATH + ", " +
            COL_KEY + ") values (?, ?, ?, ?, ?)"
        );
    }

    private class PreparedStatementStore{
        private Queue<PreparedStatement> availableStatements;
        private String query;

        PreparedStatementStore(String query) throws SQLException {
            this.query = query;
            this.availableStatements = new ConcurrentLinkedQueue<>();
        }

        private PreparedStatement getPreparedStatement() throws SQLException {
            return connectionPool.retrieve().prepareStatement(this.query);
        }

        PreparedStatement getStatement() throws SQLException {
            PreparedStatement preparedStatement = null;
            while (preparedStatement == null || preparedStatement.isClosed()) {
                if ((preparedStatement = availableStatements.poll()) == null){
                    preparedStatement = getPreparedStatement();
                }
            }
            return preparedStatement;
        }

        void putback(PreparedStatement preparedStatement) {
            try {
                if (preparedStatement != null && !preparedStatement.isClosed()) {
                    availableStatements.add(preparedStatement);
                }
            } catch (SQLException e){
                //
            }
        }
    }
}