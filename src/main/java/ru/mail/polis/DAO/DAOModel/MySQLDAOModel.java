package ru.mail.polis.DAO.DAOModel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.mail.polis.DAO.MySQLStore;

public class MySQLDAOModel implements DAOModel {
    private MySQLStore mySQLStore;

    private static final String TABLE_STORAGE = "storage";
    private static final String COL_KEY = "storage_key";
    private static final String COL_VALUE = "storage_value";
    private static final String COL_PATH = "storage_path";
    private static final String COL_SIZE = "storage_value_size";

    private PreparedStatement getRowPreparedStatement;
    private PreparedStatement updateRowPreparedStatement;
    private PreparedStatement insertRowPreparedStatement;
    private PreparedStatement getPathRowPreparedStatement;
    private PreparedStatement deleteRowPreparedStatement;

    public MySQLDAOModel() throws SQLException {
        this.mySQLStore = new MySQLStore();
        prepareStatements();
    }

    @Nullable
    @Override
    public DAOModelValue getValue(@NotNull String key) throws IOException{
        try {
            getRowPreparedStatement.setString(1, key);
            ResultSet rs = getRowPreparedStatement.executeQuery();

            if (rs.next()){
                DAOModelValue value = new DAOModelValue(key);
                value.setSize(rs.getInt(COL_SIZE));
                value.setPath(rs.getString(COL_PATH));
                value.setValue(rs.getBytes(COL_VALUE));

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
            getPathRowPreparedStatement.setString(1, key);
            ResultSet rs = getPathRowPreparedStatement.executeQuery();

            return rs.next() ? rs.getString(COL_PATH) : null;
        } catch (SQLException e){
            throw new IOException();
        }
    }

    @Override
    public void putValue(@NotNull DAOModelValue value, boolean issetInStore) throws IOException {
        try {
            PreparedStatement statement = issetInStore ? updateRowPreparedStatement : insertRowPreparedStatement;
            statement.setBytes(1, value.getValue());
            statement.setInt(2, value.getSize());
            statement.setString(3, value.getPath());
            statement.setString(4, value.getKey());
            statement.executeUpdate();
        } catch (SQLException e){
            throw new IOException();
        }
    }

    @Override
    public void deleteValue(@NotNull String key) throws IOException{
        try {
            deleteRowPreparedStatement.setString(1, key);
            deleteRowPreparedStatement.executeUpdate();
        } catch (SQLException e){
            throw new IOException();
        }
    }

    private void prepareStatements() throws SQLException{
        getRowPreparedStatement = prepareStatement(
            "SELECT " +
            COL_VALUE + ", " +
            COL_SIZE + ", " +
            COL_PATH + " from " +
            TABLE_STORAGE + " where " +
            COL_KEY + " = ?"
        );

        updateRowPreparedStatement = prepareStatement(
            "UPDATE " +
            TABLE_STORAGE + " set " +
            COL_VALUE + " = ?," +
            COL_SIZE + " = ?," +
            COL_PATH + " = ? where " +
            COL_KEY + " = ?"
        );

        getPathRowPreparedStatement = prepareStatement(
            "select " +
            COL_PATH + " from " +
            TABLE_STORAGE + " where " +
            COL_KEY + " = ?"
        );

        insertRowPreparedStatement = prepareStatement(
            "INSERT INTO " +
            TABLE_STORAGE + " set " +
            COL_VALUE + " = ?, " +
            COL_SIZE + " = ?, " +
            COL_PATH + " = ?, " +
            COL_KEY + " = ?"
        );

        deleteRowPreparedStatement = prepareStatement(
            "DELETE FROM " +
            TABLE_STORAGE + " WHERE " +
            COL_KEY + " = ?"
        );
    }

    private PreparedStatement prepareStatement(String query) throws SQLException {
        Connection connection = mySQLStore.retrieve();
        return connection.prepareStatement(query);
    }
}