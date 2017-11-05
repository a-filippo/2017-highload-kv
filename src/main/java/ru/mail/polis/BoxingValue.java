package ru.mail.polis;

public class BoxingValue<T> {
    private T value;

    public BoxingValue(){
        this.value = null;
    }

    public BoxingValue(T value) {
        this.value = value;
    }

    public synchronized void set(T value){
        this.value = value;
    }

    public T get(){
        return value;
    }
}
