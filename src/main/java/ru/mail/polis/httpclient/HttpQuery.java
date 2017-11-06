package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.replicahelpers.ListOfReplicas;

public class HttpQuery {
    private static final int TIMEOUT = 700;
    protected HttpRequestBase request;
    protected CloseableHttpClient client;

    public HttpQuery(HttpRequestBase request) {

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
            .setConnectTimeout(TIMEOUT);

        this.client = httpClientBuilder
            .disableAutomaticRetries()
            .setDefaultRequestConfig(requestBuilder.build())
            .build();
        this.request = request;
    }

    @NotNull
    public static HttpQuery Put(URI uri){
        return new HttpQuery(new HttpPut(uri));
    }

    @NotNull
    public static HttpQuery Get(URI uri){
        return new HttpQuery(new HttpGet(uri));
    }

    @NotNull
    public static HttpQuery Delete(URI uri){
        return new HttpQuery(new HttpDelete(uri));
    }

    public void addHeader(String key, String value){
        request.addHeader(key, value);
    }

    public HttpQueryResult execute() throws IOException {
        return new HttpQueryResult(client.execute(request));
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
