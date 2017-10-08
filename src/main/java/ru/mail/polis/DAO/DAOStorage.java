package ru.mail.polis.DAO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

public class DAOStorage implements DAO {
    private static final int MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE = 65536;
    private static final String HARD_STORAGE_PATH = "/Users/afilippo/polis/storage/";
    private static final String DB_URL = "jdbc:mysql://localhost:8889/kv_storage?useLegacyDatetimeCode=false&serverTimezone=UTC";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASS = "root";
    private static final String TABLE_STORAGE = "storage";
    private static final String COL_KEY = "storage_key";
    private static final String COL_VALUE = "storage_value";
    private static final String COL_PATH = "storage_path";
    private static final String COL_SIZE = "storage_value_size";

    @NotNull
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASS);
    }

    @NotNull
    @Override
    public DAOValue get(@NotNull String key) throws NoSuchElementException, IOException, IllegalArgumentException {
        try {
            Connection connection = this.getConnection();

            String queryString = "select " +
                    COL_VALUE + ", " +
                    COL_SIZE + ", " +
                    COL_PATH + " from " +
                    TABLE_STORAGE + " where " +
                    COL_KEY + " = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, key);
            ResultSet rs = query.executeQuery();

            if (rs.next()){

                int size = rs.getInt(COL_SIZE);
                String path = rs.getString(COL_PATH);
                InputStream inputStream;

                if (path.equals("")){
                    inputStream = new ByteArrayInputStream(rs.getBytes(COL_VALUE));
                } else {
                    Path p = Paths.get(HARD_STORAGE_PATH + path);
                    inputStream = Files.newInputStream(p);
                }

                return new DAOValue(inputStream, size);
            } else {
                throw new NoSuchElementException();
            }
        } catch (SQLException e){
            throw new IOException();
        }
    }

    @Override
    public void put(@NotNull String key, @NotNull DAOValue value) throws IOException, IllegalArgumentException {
        try {
            int size = value.size();
            InputStream inputStream = value.getInputStream();

            boolean issetKey = this.issetKey(key);
            boolean insert = !issetKey;

            String DBpath;
            byte[] byteValue;

            if (size < MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE){
                DBpath = "";
                byteValue = new byte[size];
                if (inputStream.read(byteValue) < 0){
                    throw new IOException();
                }
            } else {
                DBpath = key;
                byteValue = new byte[0];
                Files.copy(inputStream, Paths.get(HARD_STORAGE_PATH + key), StandardCopyOption.REPLACE_EXISTING);
            }

            this.addValueToDB(key, byteValue, size, DBpath, insert);

        } catch (SQLException e){
            throw new IOException();
        }
    }

    /**
     * Проверяем существование ключа по запросу в БД
     *
     * @param key
     * @return - существует?
     * @throws SQLException
     */
    private boolean issetKey(@NotNull String key) throws SQLException {
        Connection connection = this.getConnection();
        String queryString = "select " +
                COL_KEY + " from " +
                TABLE_STORAGE + " where " +
                COL_KEY + " = ?";
        PreparedStatement query = connection.prepareStatement(queryString);
        query.setString(1, key);
        ResultSet rs = query.executeQuery();
        return rs.next();
    }

    @Override
    public void delete(@NotNull String key) throws IOException, IllegalArgumentException {
        try {
            Connection connection = this.getConnection();

            String queryString = "select " +
                    COL_PATH + " from " +
                    TABLE_STORAGE + " where " +
                    COL_KEY + " = ?";

            PreparedStatement query = connection.prepareStatement(queryString);
            query.setString(1, key);
            ResultSet rs = query.executeQuery();
            if (rs.next()) {
                String path = rs.getString(COL_PATH);

                if (!path.equals("")) {
                    Files.delete(Paths.get(HARD_STORAGE_PATH + path));
                }

                connection = this.getConnection();
                queryString = "DELETE FROM " +
                        TABLE_STORAGE + " WHERE " +
                        COL_KEY + " = ?";

                query = connection.prepareStatement(queryString);
                query.setString(1, key);
                query.executeUpdate();
            }

        } catch (SQLException e){
            throw new IOException();
        }
    }

    /**
     * Используем флаг insert и отправляем два разных запроса вместо конструкции "ON DUPLICATE KEY UPDATE".
     * В конструкции "ON DUPLICATE KEY UPDATE" нужно вставлять в запрос значение "value" 2 раза.
     *
     * @param key
     * @param value
     * @param size
     * @param path - путь к файлу относительно рута сторейджа
     * @param insert - вставляем строку? (или обновляем?)
     * @throws SQLException
     */
    private void addValueToDB(@NotNull String key, @NotNull byte[] value, int size, @NotNull String path, boolean insert) throws SQLException {
        Connection connection = this.getConnection();
        String queryString;
        if (insert){
            queryString = "INSERT INTO " +
                    TABLE_STORAGE + " set " +
                    COL_VALUE + " = ?, " +
                    COL_SIZE + " = ?, " +
                    COL_PATH + " = ?, " +
                    COL_KEY + " = ?";
        } else {
            queryString = "UPDATE " +
                    TABLE_STORAGE + " set " +
                    COL_VALUE + " = ?," +
                    COL_SIZE + " = ?," +
                    COL_PATH + " = ? where " +
                    COL_KEY + " = ?";
        }

        PreparedStatement query = connection.prepareStatement(queryString);
        query.setBytes(1, value);
        query.setInt(2, size);
        query.setString(3, path);
        query.setString(4, key);
        query.executeUpdate();
    }
}
