package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.ListOfReplicas;

public class HttpQueryResult {
    private Response response;
    private HttpResponse httpResponse;
    private InputStream inputStreamResponse;

    HttpQueryResult(Response response) throws IOException {
        this.response = response;

        this.httpResponse = response.returnResponse();
//        this.inputStreamResponse = response.returnContent().asStream();
    }

    public int getStatusCode(){
        return httpResponse.getStatusLine().getStatusCode();
    }

    public ListOfReplicas getListOfSuccessReplicas(){
        String fromStorageString = httpResponse.getFirstHeader(HttpHelpers.HEADER_FROM_REPLICAS).getValue();
        return new ListOfReplicas(fromStorageString);
    }
}
