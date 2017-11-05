package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.conn.HttpHostConnectException;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.dao.DAOStorage;
import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.HttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;

public class MyServiceEntityPut extends MyServiceEntityAction {

    public MyServiceEntityPut(@NotNull MyServiceParameters myServiceParameters) throws ReplicaParametersException, IllegalIdException {
        super(myServiceParameters);
    }

//    @Override
//    public void execute() throws IOException {
//        int size = getSizeFromHeader();
//
//        long timestamp = getTimestamp();
//        if (timestamp < 0){
//            timestamp = getCurrentTimestamp();
//        }
//
//        if (fromReplicas.empty()) {
//
//            String tempPath = dao.getStoragePath() + File.separator + DAOStorage.TEMP_PATH;
//
//            try (TemporaryValueStorage temporaryValueStorage = new TemporaryValueStorage(
//                    tempPath,
//                    httpExchange.getRequestBody(),
//                    size
//            )) {
//
//                int countOfWorkingReplicas = 0;
//                int countOfValueCopy = 0;
//
//                int from = replicaParameters.from();
//
//                List<String> listOfReplicasForRequest = findReplicas(id);
//
//                for (int i = 0; i < from; i++) {
//                    String replicaHost = listOfReplicasForRequest.get(i);
//
//                    if (replicaHost.equals(myReplicaHost)) {
//
//                        DAOValue value = new DAOValue(temporaryValueStorage.getInputStream(), size, timestamp);
//                        dao.put(id, value);
//                        value.close();
//                        countOfWorkingReplicas++;
//                        countOfValueCopy++;
//
//                    } else {
//
//                        try {
//                            HttpQuery putHttpQuery = HttpQuery.Put(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));
//                            putHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
//                            putHttpQuery.addTimestamp(timestamp);
//
//                            InputStream inputStream = temporaryValueStorage.getInputStream();
//                            putHttpQuery.setBody(inputStream, size);
//
//                            HttpQueryResult httpQueryResult = putHttpQuery.execute();
//                            countOfWorkingReplicas++;
//                            inputStream.close();
//
//                            switch (httpQueryResult.getStatusCode()) {
//                                case HttpHelpers.STATUS_SUCCESS_PUT:
//                                    countOfValueCopy++;
//                                    break;
//                            }
//
//                        } catch (URISyntaxException e) {
//                            e.printStackTrace();
//                        } catch (HttpHostConnectException e){
//                            // nothing
//                        }
//                    }
//                }
//
//                if (countOfValueCopy < replicaParameters.ack()){
//                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS, 0);
//                    httpExchange.getResponseBody().close();
//                } else {
//                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_PUT, 0);
//                    httpExchange.getResponseBody().close();
//                }
//            }
//        } else {
//            try (DAOValue value = new DAOValue(httpExchange.getRequestBody(), size, timestamp)) {
//
//                dao.put(id, value);
//
//                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_PUT, 0);
//                httpExchange.getResponseBody().close();
//            }
//        }
//    }

    @Override
    public void processQueryFromReplica() throws IOException {
        try (DAOValue value = new DAOValue(httpExchange.getRequestBody(), getSize(), getTimestamp())) {
            dao.put(id, value);
            sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_PUT);
        }
    }

    @Override
    public void processQueryFromClient() throws IOException {
        int size = getSize();
        final long timestamp = getCurrentTimestamp();

        CountOfReplicaStatus counts = new CountOfReplicaStatus();

        try (TemporaryValueStorage temporaryValueStorage = new TemporaryValueStorage(
                dao.getStoragePath() + File.separator + DAOStorage.TEMP_PATH,
                httpExchange.getRequestBody(),
                size
            )) {

            forEachNeedingReplica(replicaHost -> {

                if (replicaHost.equals(myReplicaHost)) {

                    DAOValue value = new DAOValue(temporaryValueStorage.getInputStream(), size, timestamp);
                    dao.put(id, value);
                    value.close();
                    counts.working.plus();
                    counts.put.plus();

                } else {

                    try {
                        HttpQuery putHttpQuery = HttpQuery.Put(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));
                        putHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        putHttpQuery.addTimestamp(timestamp);

                        InputStream inputStream = temporaryValueStorage.getInputStream();
                        putHttpQuery.setBody(inputStream, size);

                        HttpQueryResult httpQueryResult = putHttpQuery.execute();
                        counts.working.plus();
                        inputStream.close();

                        switch (httpQueryResult.getStatusCode()) {
                            case HttpHelpers.STATUS_SUCCESS_PUT:
                                counts.put.plus();
                                break;
                        }

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (HttpHostConnectException e){
                        // nothing
                    }
                }

                return true;
            });
        }

        if (counts.put.get() < replicaParameters.ack()){
            sendEmptyResponse(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS);
        } else {
            sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_PUT);
        }
    }
/*
    @Override
    public void execute() throws IOException{

        int size = getSizeFromHeader();

        long timestamp = getTimestamp();
        if (timestamp < 0){
            timestamp = System.currentTimeMillis();
        }

        try (DAOValue value = new DAOValue(httpExchange.getRequestBody(), size, timestamp)) {

            dao.put(id, value);
            ListOfReplicas listOfSuccessReplicasPut = new ListOfReplicas();

            List<String> replicas = findReplicas(id);
            int numOfReplicaForQuery = 0;



            boolean successNextQuery = false;

            if (!replicas.isEmpty()) {
                while (numOfReplicaForQuery < replicas.size() && !successNextQuery) {
                    HttpQuery putHttpQuery;
                    try {
                        String replicaHost = replicas.get(numOfReplicaForQuery);

                        putHttpQuery = HttpQuery.Put(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));
                        ListOfReplicas replicasToRequest = new ListOfReplicas(fromReplicas);
                        replicasToRequest.add(myReplicaHost);
                        putHttpQuery.addReplicasToRequest(replicasToRequest);
                        putHttpQuery.addTimestamp(timestamp);

                        putHttpQuery.setBody(value.getProxedInputStream(), size);

                        HttpQueryResult httpQueryResult = putHttpQuery.execute();


                        if (httpQueryResult.getStatusCode() == HttpHelpers.STATUS_SUCCESS_PUT) {
                            listOfSuccessReplicasPut = httpQueryResult.getListOfSuccessReplicas();
                            successNextQuery = true;
                        } else {
                            // TODO
                        }

                    } catch (HttpHostConnectException e) {
                        httpExchange.getResponseHeaders().add(HttpHelpers.HEADER_FROM_REPLICAS, listOfSuccessReplicasPut.toLine());
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_INTERNAL_ERROR, 0);
                        httpExchange.getResponseBody().close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_INTERNAL_ERROR, 0);
                        httpExchange.getResponseBody().close();
                        return;
                    }
                }

//                if (!successNextQuery)

            } else {
                IOHelpers.copy(value.getProxedInputStream(), new NullOutputStream());
            }

            listOfSuccessReplicasPut.add(myReplicaHost);

            if (!fromReplicas.empty()) {
                httpExchange.getResponseHeaders().add(HttpHelpers.HEADER_FROM_REPLICAS, listOfSuccessReplicasPut.toLine());
            }
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_PUT, 0);
            httpExchange.getResponseBody().close();
        } catch (IllegalArgumentException e) {
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
            httpExchange.getResponseBody().close();
        } catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }
    */


}
