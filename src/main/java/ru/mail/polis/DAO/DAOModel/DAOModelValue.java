package ru.mail.polis.DAO.DAOModel;

public class DAOModelValue {
    private byte[] value;
    private int size;
    private String path;
    private String key;

    public DAOModelValue(String key){
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

    public String getKey() {
        return key;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
