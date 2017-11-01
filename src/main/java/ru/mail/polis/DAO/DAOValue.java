package ru.mail.polis.dao;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.input.TeeInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ru.mail.polis.IOHelpers;

public class DAOValue implements Closeable {
    public static final int MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE = 65536;

    @NotNull
    private InputStream inputStream;

    @Nullable
    private InputStream proxedInputStream;

    @Nullable
    private OutputStream outputStream;

    private int size;

    private long timestamp;

//    @Nullable
//    private HashCalculating hashCalculating;

    public DAOValue(@NotNull InputStream inputStream, int size, long timestamp){
        this.inputStream = inputStream;
        this.size = size;
        this.timestamp = timestamp;
    }

    public int size(){
        return this.size;
    }

    public long timestamp() {
        return timestamp;
    }

    public @NotNull InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        if (outputStream != null){
            outputStream.close();
        }
    }

//    public String getHash() throws IOException {
//        try {
//            return hashCalculating.calculate();
//        } catch (NoSuchAlgorithmException e){
//            e.printStackTrace();
//            throw new IOException();
//        }
//    }
//
//    void addHashCalculating(HashCalculating hashCalculating){
//        this.hashCalculating = hashCalculating;
//    }

    void setOutputStream(@NotNull OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void writeValue() throws IOException {
        IOHelpers.copy(inputStream, outputStream);
    }

    /**
     * @return - InputStream, при чтении которого автоматически записывается значение из inputStream в outputStream
     */
    @Nullable
    public InputStream getProxedInputStream(){
//       return new TeeInputStream(inputStream, outputStream);
        return proxedInputStream;
    }

    void setProxedInputStream(@NotNull InputStream proxedInputStream){
        this.proxedInputStream = proxedInputStream;
    }

//    interface HashCalculating{
//        String calculate() throws NoSuchAlgorithmException;
//    }
}
