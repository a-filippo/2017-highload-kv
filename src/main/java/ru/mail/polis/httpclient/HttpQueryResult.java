package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Response;
import org.apache.http.protocol.HTTP;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.ListOfReplicas;

public class HttpQueryResult {
    private Response response;
    private HttpResponse httpResponse;

    HttpQueryResult(Response response) throws IOException {
        this.response = response;

        this.httpResponse = response.returnResponse();
    }

    public int getStatusCode(){
        return httpResponse.getStatusLine().getStatusCode();
    }

    public ListOfReplicas getListOfSuccessReplicas(){
        String fromStorageString = httpResponse.getFirstHeader(HttpHelpers.HEADER_FROM_REPLICAS).getValue();
        return new ListOfReplicas(fromStorageString);
    }

    public InputStream getInputStream() throws IOException {
        return httpResponse.getEntity().getContent();
    }

//    public String getHash(){
//        return httpResponse.getFirstHeader(HttpHelpers.HEADER_HASH_OF_VALUE).getValue();
//    }

    public long getTimestamp(){
        Header timestampHeader = httpResponse.getFirstHeader(HttpHelpers.HEADER_TIMESTAMP);
        return timestampHeader == null ? -1 : Long.valueOf(timestampHeader.getValue());
    }

    public int getSizeFromHeader(){
        Header contentLengthHeader = httpResponse.getFirstHeader(HTTP.CONTENT_LEN);
        return contentLengthHeader == null ? -1 : Integer.valueOf(contentLengthHeader.getValue());
    }

    public int getValueSize(){
        Header valueSizeHeader = httpResponse.getFirstHeader(HttpHelpers.HEADER_SIZE);
        return valueSizeHeader == null ? -1 : Integer.valueOf(valueSizeHeader.getValue());
    }
}
