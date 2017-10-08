package ru.mail.polis.DAO;

import java.io.InputStream;

public class DAOValue {
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

}
