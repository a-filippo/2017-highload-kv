package ru.mail.polis;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.http.conn.HttpHostConnectException;
import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.Headers;

import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.HttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;

public class MyServiceEntityGet extends MyServiceEntityAction{

    public MyServiceEntityGet(@NotNull MyServiceParameters myServiceParameters) throws ReplicaParametersException, IllegalIdException {
        super(myServiceParameters);
    }

    @Override
    public void execute() throws IOException {
//        try (DAOValue value = dao.get(id);
//             OutputStream outputStream = httpExchange.getResponseBody()) {

        // запрос от клиента
        if (fromReplicas.empty()) {

            DAOValue value = null;

            ListOfReplicas listOfSuccessReplicasGet = new ListOfReplicas();

            List<String> listOfReplicasForRequest = findReplicas(id);

            Map<Long, ListOfReplicas> infoOfReplicasByTimestamp = new HashMap<>();
            int countOfWorkingReplicas = 0;
            int notFoundCount = 0;

            int from = replicaParameters.from();

            for (int i = 0; i < from; i++) {
                String replicaHost = listOfReplicasForRequest.get(i);

                if (replicaHost.equals(myReplicaHost)) {

                    try (DAOValue daoValue = dao.get(id)) {
                        value = daoValue;
                        Long timestamp = daoValue.timestamp();

                        ListOfReplicas listOfReplicas = infoOfReplicasByTimestamp.get(timestamp);
                        if (listOfReplicas == null) {
                            listOfReplicas = new ListOfReplicas();
                            infoOfReplicasByTimestamp.put(timestamp, listOfReplicas);
                        }
                        listOfReplicas.add(replicaHost);
                        countOfWorkingReplicas++;

                    } catch (IllegalArgumentException e) {
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                        httpExchange.getResponseBody().close();
                        return;
                    } catch (NoSuchElementException e){
                        countOfWorkingReplicas++;
                        notFoundCount++;
                    }

                } else {

                    try {
                        HttpQuery httpQuery = HttpQuery.Get(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));

                        httpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        httpQuery.addHeader(HttpHelpers.HEADER_GET_INFO, HttpHelpers.HEADER_GET_INFO_VALUE);

                        HttpQueryResult headQueryResult = httpQuery.execute();
                        Long timestamp = headQueryResult.getTimestamp();
                        countOfWorkingReplicas++;

//                        if (timestamp < 0) {
//                            continue;
//                        }

                        switch (headQueryResult.getStatusCode()) {
                            case HttpHelpers.STATUS_BAD_ARGUMENT:
                                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                                httpExchange.getResponseBody().close();
                                return;
                            case HttpHelpers.STATUS_NOT_FOUND:
                                notFoundCount++;
                                break;
                            case HttpHelpers.STATUS_SUCCESS_GET:
                                ListOfReplicas listOfReplicas = infoOfReplicasByTimestamp.get(timestamp);
                                if (listOfReplicas == null) {
                                    listOfReplicas = new ListOfReplicas();
                                    infoOfReplicasByTimestamp.put(timestamp, listOfReplicas);
                                }
                                listOfReplicas.add(replicaHost);
                                break;
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        throw new IOException();
                    } catch (HttpHostConnectException e){
                        // nothing
                    }
                }
            }

            if (countOfWorkingReplicas < replicaParameters.ack()){
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS, 0);
                httpExchange.getResponseBody().close();
                return;
            }

            int maxTimestampCount = -1;
            long timestampOfMaxCount = -1;
            for (Map.Entry<Long, ListOfReplicas> entry : infoOfReplicasByTimestamp.entrySet()) {
                int entryReplicasSize = entry.getValue().size();
                if (entryReplicasSize > maxTimestampCount) {
                    maxTimestampCount = entryReplicasSize;
                    timestampOfMaxCount = entry.getKey();
                }
            }

            ListOfReplicas replicasWithNeedingValue = infoOfReplicasByTimestamp.get(timestampOfMaxCount);


            if (replicasWithNeedingValue == null){
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
                httpExchange.getResponseBody().close();
            } else if (replicasWithNeedingValue.size() >= replicaParameters.ack()) {
                if (replicasWithNeedingValue.contains(myReplicaHost)) {
                    try (DAOValue daoValue = dao.get(id)) {
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, daoValue.size());
                        IOHelpers.copy(daoValue.getInputStream(), httpExchange.getResponseBody());
                        httpExchange.getResponseBody().close();
                    } catch (IllegalArgumentException e) {
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                        httpExchange.getResponseBody().close();
                    } catch (NoSuchElementException e){
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
                        httpExchange.getResponseBody().close();
                    }
                } else {
                    String getFromReplica = replicasWithNeedingValue.toArray()[0];

                    try {
                        HttpQuery getHttpQuery = HttpQuery.Get(new URI(getFromReplica + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));
                        getHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));

                        HttpQueryResult getValueResult = getHttpQuery.execute();
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, 0);
                        IOHelpers.copy(getValueResult.getInputStream(), httpExchange.getResponseBody());
                        httpExchange.getResponseBody().close();

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        throw new IOException();
                    } catch (HttpHostConnectException e){
                        System.out.println("eeeeeee"); // TODO
                        e.printStackTrace();
                    }
                }
            } else if (notFoundCount >= replicaParameters.ack()) {
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
                httpExchange.getResponseBody().close();
            } else {
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS, 0);
                httpExchange.getResponseBody().close();
            }

        // запрос от реплики
        } else {

            try (DAOValue value = dao.get(id)){

                if (HttpHelpers.HEADER_GET_INFO_VALUE.equals(httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_GET_INFO))){

                    Headers headers = httpExchange.getResponseHeaders();
                    headers.add(HttpHelpers.HEADER_TIMESTAMP, String.valueOf(value.timestamp()));
                    headers.add(HttpHelpers.HEADER_SIZE, String.valueOf(value.size()));

                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, 0);
                } else {
                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, value.size());
                    IOHelpers.copy(value.getInputStream(), httpExchange.getResponseBody());
                }
                httpExchange.getResponseBody().close();
            } catch (NoSuchElementException e) {
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
                httpExchange.getResponseBody().close();
            } catch (IllegalArgumentException e) {
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                httpExchange.getResponseBody().close();
            }
        }
    }
}
