package ru.mail.polis.dao.daomodel;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import org.apache.derby.jdbc.EmbeddedDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.mail.polis.IOHelpers;

public class DerbyDAOModel implements DAOModel {
    private final String DB_URL;

    private MySQLPool mySQLPool;

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

    public DerbyDAOModel(String dbPath) throws SQLException {
        String folderOfDatabase;
        try {
            folderOfDatabase = createDatabaseFolderIfNeeding(dbPath);
        } catch (IOException e){
            e.printStackTrace();
            throw new SQLException("Error of creating tables");
        }

        DB_URL = dbPath + File.separator + folderOfDatabase;

        EmbeddedDataSource dataSource = new EmbeddedDataSource();
        dataSource.setDatabaseName(DB_URL);
        dataSource.setCreateDatabase("create");

        mySQLPool = new MySQLPool(dataSource);

        createTable();
        prepareStatements();
    }

    @Override
    public void stop() throws IOException {
        try {
            DriverManager.getConnection("jdbc:derby:" + DB_URL + ";shutdown=true");
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

    private String createDatabaseFolderIfNeeding(String dbPath) throws IOException{
        IOHelpers.createDirIfNoExists(dbPath);
        File dir = new File(dbPath);
        String[] files = dir.list();
        String folderOfDatabase;
        if (files.length == 0){
            folderOfDatabase = String.valueOf(System.currentTimeMillis());
        } else {
            folderOfDatabase = files[0];
        }
        return folderOfDatabase;
    }

    private void createTable() throws SQLException {
        Connection connection = mySQLPool.retrieve();


        DatabaseMetaData databaseMetaData = connection.getMetaData();

        ResultSet resultSet = databaseMetaData.getTables(null, "APP", TABLE_STORAGE, null);
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

        mySQLPool.putback(connection);
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
        private LinkedList<PreparedStatement> availableStatements;
        private String query;

        PreparedStatementStore(String query){
            this.query = query;
            this.availableStatements = new LinkedList<>();
        }

        synchronized PreparedStatement getStatement() throws SQLException {
            PreparedStatement preparedStatement = null;
            while (preparedStatement == null || preparedStatement.isClosed()) {
                if (availableStatements.isEmpty()){
                    preparedStatement = mySQLPool.retrieve().prepareStatement(this.query);
                } else {
                    preparedStatement = availableStatements.removeFirst();
                }
            }
            return preparedStatement;
        }

        synchronized void putback(PreparedStatement preparedStatement) {
            try {
                if (preparedStatement != null && !preparedStatement.isClosed()) {
                    availableStatements.addLast(preparedStatement);
                }
            } catch (SQLException e){
                //
            }
        }
    }
}