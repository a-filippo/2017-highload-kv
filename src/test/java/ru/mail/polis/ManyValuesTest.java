package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.assertEquals;

@Ignore
public class ManyValuesTest extends TestBase{
    private static int port;
    private static File data;
    private static KVService storage;
    @Rule
    public final Timeout globalTimeout = Timeout.seconds(3600);

    @BeforeClass
    public static void beforeAll() throws IOException, InterruptedException {
        port = randomPort();
        data = Files.createTempDirectory();
//        storage = KVServiceFactory.create(port, data);
        storage.start();
    }

    @AfterClass
    public static void afterAll() throws IOException {
        storage.stop();
        Files.recursiveDelete(data);
    }

    @Test
    public void manyValues() throws Exception {
        byte[] value = new byte[1024];
        ThreadLocalRandom.current().nextBytes(value);
        for (int i = 0; i < 10000000; i++){
            assertEquals(201, upsert("key-" + i, value).getStatusLine().getStatusCode());
        }
    }

    private HttpResponse upsert(
            @NotNull final String key,
            @NotNull final byte[] data) throws IOException {
        return Request.Put(url(key)).bodyByteArray(data).execute().returnResponse();
    }

    @NotNull
    private String url(@NotNull final String id) {
        return "http://localhost:" + port + "/v0/entity?id=" + id;
    }
}
