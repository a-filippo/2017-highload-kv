package ru.mail.polis.httpclient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.client.fluent.Async;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.concurrent.FutureCallback;
import org.jetbrains.annotations.NotNull;

public class HttpQueryPool {
    @NotNull
    private Async async;

    public HttpQueryPool() {
        ExecutorService threadpool = Executors.newCachedThreadPool();
        async = Async.newInstance().use(threadpool);
    }

    Future<Content> startRequest(Request request, FutureCallback<Content> futureCallback) {
        return async.execute(request, futureCallback);
    }
}
