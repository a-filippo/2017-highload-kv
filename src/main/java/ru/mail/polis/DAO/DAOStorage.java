package ru.mail.polis.dao;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import ru.mail.polis.IOHelpers;
import ru.mail.polis.dao.daomodel.DAOModel;
import ru.mail.polis.dao.daomodel.DAOModelValue;
import ru.mail.polis.dao.daomodel.H2DAOModel;

public class DAOStorage implements DAO {
    public static final String HARD_STORAGE_FOLDER = "storage";
    public static final String DB_PATH = "db";
    public static final String TEMP_PATH = "temp";
    private String HARD_STORAGE_FULL_PATH;
    private String DB_FULL_PATH;

    private String rootPath;

    private DAOModel modelValues;

    public DAOStorage(File data) throws IOException {
        rootPath = data.getAbsolutePath();
        HARD_STORAGE_FULL_PATH = rootPath + File.separator + HARD_STORAGE_FOLDER + File.separator;
        IOHelpers.createDirIfNoExists(HARD_STORAGE_FULL_PATH);
        IOHelpers.createDirIfNoExists(rootPath + File.separator + TEMP_PATH);
        DB_FULL_PATH = rootPath + File.separator + DB_PATH;

        try {
            modelValues = new H2DAOModel(DB_FULL_PATH);
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        }
    }

    @Override
    public void stop() throws IOException{
        modelValues.stop();
    }

    @NotNull
    @Override
    public String getStoragePath() {
        return rootPath;
    }

    @NotNull
    @Override
    public DAOValue get(@NotNull String key) throws NoSuchElementException, IOException, IllegalArgumentException {
        throwArgumentException(key);

        DAOModelValue value = modelValues.getValue(key);

        if (value != null){
            String path = value.getPath();
            InputStream inputStream;

            if (path.equals("")){
                inputStream = new ByteArrayInputStream(value.getValue());
            } else {
                File file = new File(HARD_STORAGE_FULL_PATH + path);
                inputStream = new FileInputStream(file);
            }

            return new DAOValue(inputStream, value.getSize(), value.getTimestamp());
        } else {
            throw new NoSuchElementException();
        }
    }

    @Override
    public void put(@NotNull String key, @NotNull DAOValue value) throws IOException, IllegalArgumentException {
        throwArgumentException(key);

        int size = value.size();
        InputStream inputStream = value.getInputStream();

        boolean issetKey = modelValues.getPath(key) != null;

        String DBpath;
        byte[] byteValue;

        if (size < DAOValue.MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE){
            DBpath = "";
            byteValue = new byte[size];
            inputStream.read(byteValue);
        } else {
            DBpath = key;
            byteValue = new byte[0];
            IOHelpers.copy(inputStream, new FileOutputStream(HARD_STORAGE_FULL_PATH + key));
        }

        DAOModelValue modelValue = new DAOModelValue(key);
        modelValue.setSize(size);
        modelValue.setPath(DBpath);
        modelValue.setTimestamp(value.timestamp());
        modelValue.setValue(byteValue);

        modelValues.putValue(modelValue, issetKey);
    }

    @Override
    public void delete(@NotNull String key, long deleteTimestamp) throws IOException, IllegalArgumentException {
        throwArgumentException(key);

        String path = modelValues.getPath(key);

        if (path != null) {

            if (!path.equals("")) {
                Files.delete(Paths.get(HARD_STORAGE_FULL_PATH + path));
            }

            modelValues.deleteValue(key, deleteTimestamp);
        }
    }

    private void throwArgumentException(@NotNull String key) throws IllegalArgumentException{
        boolean correct = (key.length() > 0);
        if (!correct){
            throw new IllegalArgumentException();
        }
    }
}
