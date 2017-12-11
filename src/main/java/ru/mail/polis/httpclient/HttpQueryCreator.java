package ru.mail.polis.httpclient;

import java.net.URI;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.jetbrains.annotations.NotNull;

public class HttpQueryCreator {
    private MyHttpClientCreator myHttpClientCreator;

    public HttpQueryCreator() {
        myHttpClientCreator = new MyHttpClientCreator();
    }

    @NotNull
    public HttpQuery put(URI uri){
        return new HttpQuery(new HttpPut(uri), myHttpClientCreator);
    }

    @NotNull
    public HttpQuery get(URI uri){
        return new HttpQuery(new HttpGet(uri), myHttpClientCreator);
    }

    @NotNull
    public HttpQuery delete(URI uri){
        return new HttpQuery(new HttpDelete(uri), myHttpClientCreator);
    }

    public void stop() {
        myHttpClientCreator.stop();
    }

    public void start() {
        myHttpClientCreator.start();
    }
}
