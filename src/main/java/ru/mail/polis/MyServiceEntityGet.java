package ru.mail.polis;

import java.io.IOException;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import ru.mail.polis.dao.DAOValue;

public class MyServiceEntityGet extends MyServiceEntityAction{
    public MyServiceEntityGet(@NotNull MyServiceParameters myServiceParameters) {
        super(myServiceParameters);
    }

    @Override
    public void execute() throws IOException {
        try (DAOValue value = dao.get(id);
             OutputStream outputStream = httpExchange.getResponseBody()) {

            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, value.size());
            IOHelpers.copy(value.getInputStream(), outputStream);
        } catch (NoSuchElementException e) {
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
            httpExchange.getResponseBody().close();
        } catch (IllegalArgumentException e) {
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
            httpExchange.getResponseBody().close();
        }
    }
}
