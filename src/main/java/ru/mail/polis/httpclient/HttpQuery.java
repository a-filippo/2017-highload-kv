package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.replicahelpers.ListOfReplicas;

public class HttpQuery {
    private MyHttpClientPool clientPool;
    private HttpRequestBase request;

    HttpQuery(HttpRequestBase request, MyHttpClientPool clientPool) {
        this.request = request;
        this.clientPool = clientPool;
    }

    public void addHeader(String key, String value){
        request.addHeader(key, value);
    }

    public HttpQueryResult execute() throws IOException {
        return new HttpQueryResult(request, clientPool.execute(request));
    }

    public void addReplicasToRequest(ListOfReplicas replicas){
        addHeader(HttpHelpers.HEADER_FROM_REPLICAS, replicas.toLine());
    }

    public void addTimestamp(long timestamp){
        addHeader(HttpHelpers.HEADER_TIMESTAMP, String.valueOf(timestamp));
    }

    public void setBody(InputStream inputStream, int size) throws IOException {
        if (this.request instanceof HttpEntityEnclosingRequest) {
            ((HttpEntityEnclosingRequest) this.request).setEntity(new InputStreamEntity(inputStream, (long)size));
        } else {
            throw new IllegalStateException(this.request.getMethod()
                    + " request cannot enclose an entity");
        }
    }
}
