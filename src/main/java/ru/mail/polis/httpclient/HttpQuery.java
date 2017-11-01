package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.InputStreamEntity;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.ListOfReplicas;

public class HttpQuery {

//    protected static HttpQueryPool httpQueryPool;
    protected Request request;
//    private CompleteResponse completeResponse = null;
//
//    public static void setHttpQueryPool(HttpQueryPool httpQueryPool){
//        HttpQuery.httpQueryPool = httpQueryPool;
//    }

    public HttpQuery(Request request) {
        this.request = request;
    }

    public static HttpQuery Put(URI uri){
        return new HttpQuery(Request.Put(uri));
    }

    public static HttpQuery Head(URI uri){
        return new HttpQuery(Request.Head(uri));
    }

    public static HttpQuery Get(URI uri){
        return new HttpQuery(Request.Get(uri));
    }

    public void addHeader(String key, String value){
        request.addHeader(key, value);
    }

    public HttpQueryResult execute() throws IOException {
        return new HttpQueryResult(request.execute());
    }

    public void addReplicasToRequest(ListOfReplicas replicas){
        addHeader(HttpHelpers.HEADER_FROM_REPLICAS, replicas.toLine());
    }

    public void addTimestamp(long timestamp){
        addHeader(HttpHelpers.HEADER_TIMESTAMP, String.valueOf(timestamp));
    }

    public void setBody(InputStream inputStream, int size) throws IOException {
        request.body(new InputStreamEntity(inputStream, (long)size));
    }

//    public void addSize(int size){
//        addHeader(HttpHelpers.HEADER_SIZE, String.valueOf(size));
//    }

    interface CompleteResponse{
        void action(Content content);
    }
}
