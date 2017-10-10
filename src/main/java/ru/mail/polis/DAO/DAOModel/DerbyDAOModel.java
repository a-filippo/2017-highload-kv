package ru.mail.polis.DAO.DAOModel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DerbyDAOModel implements DAOModel {
    private final String DB_URL;
    private static final String TABLE_STORAGE = "STORAGE";
    private static final String COL_KEY = "storage_key";
    private static final String COL_VALUE = "storage_value";
    private static final String COL_PATH = "storage_path";
    private static final String COL_SIZE = "storage_value_size";

    private PreparedStatementStore GetRowPreparedStatementStore;
    private PreparedStatementStore UpdateRowPreparedStatementStore;
    private PreparedStatementStore InsertRowPreparedStatementStore;
    private PreparedStatementStore GetPathRowPreparedStatementStore;
    private PreparedStatementStore DeleteRowPreparedStatementStore;

    public DerbyDAOModel(String dbPath) throws SQLException {
        DB_URL = "jdbc:derby:" + dbPath + ";create=true";
        createTable();
        prepareStatements();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private void createTable() throws SQLException {
        Connection connection = getConnection();

        DatabaseMetaData databaseMetaData = connection.getMetaData();
        ResultSet resultSet = databaseMetaData.getTables(null, "APP", TABLE_STORAGE, null);
        if (!resultSet.next()){
            Statement statement = connection.createStatement();
            statement.execute(
                "CREATE TABLE "+TABLE_STORAGE+"(" +
                COL_KEY+" VARCHAR (256) NOT NULL, " +
                COL_VALUE+" blob (65536), " +
                COL_PATH+" VARCHAR (256) NOT NULL, " +
                COL_SIZE+" int NOT NULL" +
                ")"
            );

            statement.execute("ALTER TABLE " + TABLE_STORAGE + " ADD PRIMARY KEY (" + COL_KEY + ")");
            statement.close();
        }

        connection.close();
    }

    @Nullable
    @Override
    public DAOModelValue getValue(@NotNull String key) throws IOException{
        try {
            PreparedStatement preparedStatement = GetRowPreparedStatementStore.getStatement();
            preparedStatement.setString(1, key);
            ResultSet rs = preparedStatement.executeQuery();

            if (rs.next()){
                DAOModelValue value = new DAOModelValue(key);
                value.setSize(rs.getInt(COL_SIZE));
                value.setPath(rs.getString(COL_PATH));
                value.setValue(rs.getBytes(COL_VALUE));

                rs.close();
                return value;
            } else {
                return null;
            }
        } catch (SQLException e){
            throw new IOException();
        }
    }

    @Nullable
    @Override
    public String getPath(@NotNull String key) throws IOException {
        try {
            PreparedStatement preparedStatement = GetPathRowPreparedStatementStore.getStatement();
            preparedStatement.setString(1, key);
            ResultSet rs = preparedStatement.executeQuery();
            String path = rs.next() ? rs.getString(COL_PATH) : null;

            rs.close();
            return path;
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        }
    }

    @Override
    public void putValue(@NotNull DAOModelValue value, boolean issetInStore) throws IOException {
        try {
            PreparedStatementStore preparedStatementStore = issetInStore ? UpdateRowPreparedStatementStore : InsertRowPreparedStatementStore;
            PreparedStatement preparedStatement = preparedStatementStore.getStatement();
            preparedStatement.setBytes(1, value.getValue());
            preparedStatement.setInt(2, value.getSize());
            preparedStatement.setString(3, value.getPath());
            preparedStatement.setString(4, value.getKey());
            preparedStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        }
    }

    @Override
    public void deleteValue(@NotNull String key) throws IOException{
        try {
            PreparedStatement preparedStatement = DeleteRowPreparedStatementStore.getStatement();
            preparedStatement.setString(1, key);
            preparedStatement.executeUpdate();
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        }
    }

    private void prepareStatements() throws SQLException{
        GetRowPreparedStatementStore = new PreparedStatementStore(
            "SELECT " +
            COL_VALUE + ", " +
            COL_SIZE + ", " +
            COL_PATH + " from " +
            TABLE_STORAGE + " where " +
            COL_KEY + " = ?"
        );

        UpdateRowPreparedStatementStore = new PreparedStatementStore(
            "UPDATE " +
            TABLE_STORAGE + " SET " +
            COL_VALUE + " = ?," +
            COL_SIZE + " = ?," +
            COL_PATH + " = ? WHERE " +
            COL_KEY + " = ?"
        );

        GetPathRowPreparedStatementStore = new PreparedStatementStore(
            "select " +
            COL_PATH + " from " +
            TABLE_STORAGE + " where " +
            COL_KEY + " = ?"
        );

        InsertRowPreparedStatementStore = new PreparedStatementStore(
            "INSERT INTO " +
            TABLE_STORAGE + " (" +
            COL_VALUE + ", " +
            COL_SIZE + ", " +
            COL_PATH + ", " +
            COL_KEY + ") values (?, ?, ?, ?)"
        );

        DeleteRowPreparedStatementStore = new PreparedStatementStore(
            "DELETE FROM " +
            TABLE_STORAGE + " WHERE " +
            COL_KEY + " = ?"
        );
    }

    private class PreparedStatementStore{
        private PreparedStatement preparedStatement;
        private String query;

        PreparedStatementStore(String query){
            this.query = query;
        }

        PreparedStatement getStatement() throws SQLException {
            if (preparedStatement == null || preparedStatement.isClosed()){
                preparedStatement = generatePreparedStatement();
            }
            return preparedStatement;
        }

        private PreparedStatement generatePreparedStatement() throws SQLException {
            return getConnection().prepareStatement(this.query);
        }
    }
}