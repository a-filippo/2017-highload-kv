package ru.mail.polis.httpclient;

import java.io.IOException;

import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.ListOfReplicas;

abstract public class HttpQuery {

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

    public void addHeader(String key, String value){
        request.addHeader(key, value);
    }

    public HttpQueryResult execute() throws IOException {
        return new HttpQueryResult(request.execute());
    }

    public void addReplicasToRequest(ListOfReplicas replicas){
        addHeader(HttpHelpers.HEADER_FROM_REPLICAS, replicas.toLine());
    }

    interface CompleteResponse{
        void action(Content content);
    }
}
