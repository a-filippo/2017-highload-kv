package ru.mail.polis.dao;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public DAOValue(@NotNull InputStream inputStream, int size){
        this.inputStream = inputStream;
        this.size = size;
    }

    public int size(){
        return this.size;
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
}
