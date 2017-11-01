package ru.mail.polis;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;

public class MyServiceEntityDelete extends MyServiceEntityAction {
    public MyServiceEntityDelete(@NotNull MyServiceParameters myServiceParameters) throws NoSuchReplicasException, IllegalIdException {
        super(myServiceParameters);
    }

    @Override
    public void execute() throws IOException {
        try {
            dao.delete(id);
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_DELETE, 0);
            httpExchange.getResponseBody().close();
        } catch (IllegalArgumentException e) {
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
            httpExchange.getResponseBody().close();
        }
    }
}
