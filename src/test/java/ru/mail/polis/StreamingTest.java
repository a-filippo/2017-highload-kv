package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.assertEquals;

public class StreamingTest extends ClusterTestBase {
    @Rule
    public final Timeout globalTimeout = Timeout.seconds(120);
    private int port0;
    private int port1;
    private File data0;
    private File data1;
    private KVService storage0;
    private KVService storage1;

    @Before
    public void beforeEach() throws IOException, InterruptedException {
        port0 = randomPort();
        port1 = randomPort();
        endpoints = new LinkedHashSet<>(Arrays.asList(endpoint(port0), endpoint(port1)));
        data0 = Files.createTempDirectory();
        data1 = Files.createTempDirectory();
        storage0 = KVServiceFactory.create(port0, data0, endpoints);
        storage0.start();
        storage1 = KVServiceFactory.create(port1, data1, endpoints);
        storage1.start();
    }

    @After
    public void afterEach() throws IOException {
        storage0.stop();
        Files.recursiveDelete(data0);
        storage1.stop();
        Files.recursiveDelete(data1);
        endpoints = Collections.emptySet();
    }

    @Test
    public void streaming1500MB() throws Exception {
        String key = randomKey();
        byte[] byteArrayValue = randomValue();
        int valueSize = 1024 * 1024 * 1536;

        InputStream value = new RepeatValuesInputStream(byteArrayValue, valueSize);

        int statusCode = Request.Put(url(0, key, 2, 2))
            .body(new InputStreamEntity(value, valueSize))
            .execute()
            .returnResponse()
            .getStatusLine()
            .getStatusCode();
        value.close();

        assertEquals(201, statusCode);

        storage0.stop();

        CloseableHttpResponse getValueResponse = HttpClientBuilder
                .create()
                .build()
                .execute(new HttpGet(url(1, key, 1, 2)));


        assertEquals(200, getValueResponse.getStatusLine().getStatusCode());


        InputStream valueFromStorage = getValueResponse.getEntity().getContent();
        value = new RepeatValuesInputStream(byteArrayValue, valueSize);

        compareTwoInputStream(value, valueFromStorage);

        value.close();
        valueFromStorage.close();
    }

    private void compareTwoInputStream(InputStream inputStream1, InputStream inputStream2) throws Exception {
        int byte1;
        int byte2;
        while (true){
            byte1 = inputStream1.read();
            byte2 = inputStream2.read();
            if (byte1 < 0 && byte2 < 0){
                break;
            } else {
                assertEquals(byte1, byte2);
            }
        }
    }

    class RepeatValuesInputStream extends InputStream {
        private byte[] values;
        private int valuesSize;
        private int number;
        private int length;

        RepeatValuesInputStream(byte[] values, int length){
            this.number = 0;
            this.values = values;
            this.length = length;
            this.valuesSize = values.length;
        }

        @Override
        public int read() throws IOException {
            return (number < length) ? values[number++ % valuesSize] & 0xff : -1;
        }
    }
}