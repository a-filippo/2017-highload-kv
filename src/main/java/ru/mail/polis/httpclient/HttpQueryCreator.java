package ru.mail.polis.httpclient;

import java.net.URI;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.jetbrains.annotations.NotNull;

public class HttpQueryCreator {
    private MyHttpClientPool myHttpClientPool;

    public HttpQueryCreator() {
        myHttpClientPool = new MyHttpClientPool();
    }

    @NotNull
    public HttpQuery put(URI uri){
        return new HttpQuery(new HttpPut(uri), myHttpClientPool);
    }

    @NotNull
    public HttpQuery get(URI uri){
        return new HttpQuery(new HttpGet(uri), myHttpClientPool);
    }

    @NotNull
    public HttpQuery delete(URI uri){
        return new HttpQuery(new HttpDelete(uri), myHttpClientPool);
    }

    public void stop() {
        myHttpClientPool.stop();
    }

    public void start() {
        myHttpClientPool.start();
    }
}
