package ru.mail.polis.httpclient;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.protocol.HTTP;

import ru.mail.polis.HttpHelpers;

public class HttpQueryResult {
    private CloseableHttpResponse response;

    HttpQueryResult(CloseableHttpResponse response) throws IOException {
        this.response = response;
    }

    public int getStatusCode(){
        return response.getStatusLine().getStatusCode();
    }

    public InputStream getInputStream() throws IOException {
        return response.getEntity().getContent();
    }

    public long getTimestamp(){
        Header timestampHeader = response.getFirstHeader(HttpHelpers.HEADER_TIMESTAMP);
        return timestampHeader == null ? -1 : Long.valueOf(timestampHeader.getValue());
    }

    public int getSizeFromHeader(){
        Header contentLengthHeader = response.getFirstHeader(HTTP.CONTENT_LEN);
        return contentLengthHeader == null ? -1 : Integer.valueOf(contentLengthHeader.getValue());
    }

    public int getValueSize(){
        Header valueSizeHeader = response.getFirstHeader(HttpHelpers.HEADER_SIZE);
        return valueSizeHeader == null ? -1 : Integer.valueOf(valueSizeHeader.getValue());
    }
}
