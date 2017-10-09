package ru.mail.polis.DAO;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class DAOValue implements Closeable {
    private InputStream inputStream;
    private int size;

    public DAOValue(InputStream inputStream, int size){
        this.inputStream = inputStream;
        this.size = size;
    }

    public int size(){
        return this.size;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
