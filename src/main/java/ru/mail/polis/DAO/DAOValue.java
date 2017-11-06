package ru.mail.polis.dao;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import org.jetbrains.annotations.NotNull;

public class DAOValue implements Closeable {
    public static final int MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE = 65536;

    @NotNull
    private InputStream inputStream;

    private int size;

    private long timestamp;

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
    }
}
