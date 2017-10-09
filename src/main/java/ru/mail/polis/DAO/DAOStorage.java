package ru.mail.polis.DAO;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import ru.mail.polis.DAO.DAOModel.DAOModel;
import ru.mail.polis.DAO.DAOModel.DAOModelValue;
import ru.mail.polis.DAO.DAOModel.MySQLDAOModel;

public class DAOStorage implements DAO {
    private static final int MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE = 65536;
    private String HARD_STORAGE_PATH;

    private DAOModel modelValues;

    public DAOStorage(File data) throws IOException {
        HARD_STORAGE_PATH = data.getAbsolutePath();
        try {
            modelValues = new MySQLDAOModel();
        } catch (SQLException e){
            throw new IOException();
        }
    }

    @NotNull
    @Override
    public DAOValue get(@NotNull String key) throws NoSuchElementException, IOException, IllegalArgumentException {
        throwArgumentException(key);

        DAOModelValue value = modelValues.getValue(key);

        if (value != null){
            int size = value.getSize();
            String path = value.getPath();
            InputStream inputStream;

            if (path.equals("")){
                inputStream = new ByteArrayInputStream(value.getValue());
            } else {
                Path p = Paths.get(HARD_STORAGE_PATH + path);
                inputStream = Files.newInputStream(p);
            }

            return new DAOValue(inputStream, size);
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

        if (size < MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE){
            DBpath = "";
            byteValue = new byte[size];
            inputStream.read(byteValue);
        } else {
            DBpath = key;
            byteValue = new byte[0];
            Files.copy(inputStream, Paths.get(HARD_STORAGE_PATH + key), StandardCopyOption.REPLACE_EXISTING);
        }

        DAOModelValue modelValue = new DAOModelValue(key);
        modelValue.setSize(size);
        modelValue.setPath(DBpath);
        modelValue.setValue(byteValue);

        modelValues.putValue(modelValue, issetKey);
    }

    @Override
    public void delete(@NotNull String key) throws IOException, IllegalArgumentException {
        throwArgumentException(key);

        String path = modelValues.getPath(key);

        if (path != null) {

            if (!path.equals("")) {
                Files.delete(Paths.get(HARD_STORAGE_PATH + path));
            }

            modelValues.deleteValue(key);
        }
    }

    private void throwArgumentException(@NotNull String key) throws IllegalArgumentException{
        boolean correct = (key.length() > 0);
        if (!correct){
            throw new IllegalArgumentException();
        }
    }
}
