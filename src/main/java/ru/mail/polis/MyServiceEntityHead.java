package ru.mail.polis;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.Headers;

import ru.mail.polis.dao.DAOValue;

public class MyServiceEntityHead extends MyServiceEntityAction {

    public MyServiceEntityHead(@NotNull MyServiceParameters myServiceParameters) throws ReplicaParametersException, IllegalIdException {
        super(myServiceParameters);
    }

//    @Override
//    public void execute() throws IOException {
//        try (DAOValue value = dao.get(id)) {
//
//            Headers headers = httpExchange.getResponseHeaders();
//            headers.add(HttpHelpers.HEADER_TIMESTAMP, String.valueOf(value.timestamp()));
//            headers.add(HttpHelpers.HEADER_SIZE, String.valueOf(value.size()));
//
//            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_HEAD, -1);
////            httpExchange.getResponseBody().
////            httpExchange.getResponseBody().close();
//
//        } catch (NoSuchElementException e) {
//            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, -1);
////            httpExchange.getResponseBody().close();
//        } catch (IllegalArgumentException e) {
//            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, -1);
////            httpExchange.getResponseBody().close();
//        }
//    }

    @Override
    public void processQueryFromReplica() throws IOException {

    }

    @Override
    public void processQueryFromClient() throws IOException {

    }
}
