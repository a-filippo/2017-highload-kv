package ru.mail.polis.httpclient;

import java.net.URI;

import org.apache.http.client.fluent.Request;

public class GetHttpQuery extends HttpQuery {
    public GetHttpQuery(URI uri) {
        super(Request.Get(uri));
    }




}
