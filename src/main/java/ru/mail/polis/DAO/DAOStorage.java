package ru.mail.polis.dao;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.io.input.TeeInputStream;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.SHA1;
import ru.mail.polis.dao.daomodel.DAOModel;
import ru.mail.polis.dao.daomodel.DAOModelValue;
import ru.mail.polis.dao.daomodel.DerbyDAOModel;

public class DAOStorage implements DAO {
    private String HARD_STORAGE_FOLDER = "storage";
    private String DB_PATH = "db";
    private String HARD_STORAGE_FULL_PATH;
    private String DB_FULL_PATH;

    private DAOModel modelValues;

    public DAOStorage(File data) throws IOException {
        String path = data.getAbsolutePath();
        HARD_STORAGE_FULL_PATH = path + File.separator + HARD_STORAGE_FOLDER + File.separator;
        Files.createDirectory(Paths.get(HARD_STORAGE_FULL_PATH));
        DB_FULL_PATH = path + File.separator + DB_PATH + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

        try {
            modelValues = new DerbyDAOModel(DB_FULL_PATH);
        } catch (SQLException e){
            e.printStackTrace();
            throw new IOException();
        }
    }

    @NotNull
    @Override
    public DAOValue get(@NotNull String key) throws NoSuchElementException, IOException, IllegalArgumentException {
        throwArgumentException(key);

        DAOModelValue value = modelValues.getValue(key);

        if (value != null){
//            int size = value.getSize();
            String path = value.getPath();
            InputStream inputStream;

//            DAOValue.HashCalculating hashCalculating;

            if (path.equals("")){
                inputStream = new ByteArrayInputStream(value.getValue());
//                hashCalculating = () -> SHA1.calculateHash(value.getValue());
            } else {
                File file = new File(HARD_STORAGE_FULL_PATH + path);
                inputStream = new FileInputStream(file);
//                hashCalculating = () -> SHA1.calculateHash(file);
            }

//            DAOValue daoValue = new DAOValue(inputStream, size);
//            daoValue.addHashCalculating(hashCalculating);

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
            value.setProxedInputStream(new ByteArrayInputStream(byteValue));

        } else {
            DBpath = key;
            byteValue = new byte[0];
//            Path filePath = Paths.get(HARD_STORAGE_FULL_PATH + key);
            value.setProxedInputStream(
                new TeeInputStream(inputStream, new FileOutputStream(HARD_STORAGE_FULL_PATH + key))
            );
//            value.setOutputStream(new FileOutputStream(HARD_STORAGE_FULL_PATH + key));
//            InputStream proxedInputStream = new TeeInputStream(inputStream, fileOutputStream);
//            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        DAOModelValue modelValue = new DAOModelValue(key);
        modelValue.setSize(size);
        modelValue.setPath(DBpath);
        modelValue.setTimestamp(value.timestamp());
        modelValue.setValue(byteValue);

        modelValues.putValue(modelValue, issetKey);
    }

    @Override
    public void delete(@NotNull String key) throws IOException, IllegalArgumentException {
        throwArgumentException(key);

        String path = modelValues.getPath(key);

        if (path != null) {

            if (!path.equals("")) {
                Files.delete(Paths.get(HARD_STORAGE_FULL_PATH + path));
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
