package ru.mail.polis.httpclient;

import java.io.IOException;

import org.apache.http.client.fluent.Response;

public class PutHttpQueryResult extends HttpQueryResult{
    public PutHttpQueryResult(Response response) throws IOException {
        super(response);
    }
}
