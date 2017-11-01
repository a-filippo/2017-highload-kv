package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.xml.ws.spi.http.HttpExchange;

import org.apache.http.HttpEntity;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.InputStreamEntity;

public class PutHttpQuery extends HttpQuery {
    public PutHttpQuery(URI uri) {
        super(Request.Put(uri));
    }



//    @Override
//    public PutHttpQueryResult execute() {
//        return null;
//    }
}
