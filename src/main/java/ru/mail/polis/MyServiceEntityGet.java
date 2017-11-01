package ru.mail.polis;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.GetHttpQuery;
import ru.mail.polis.httpclient.HttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;
import ru.mail.polis.httpclient.PutHttpQuery;

public class MyServiceEntityGet extends MyServiceEntityAction{

    public MyServiceEntityGet(@NotNull MyServiceParameters myServiceParameters) throws NoSuchReplicasException {
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

                    } catch (IllegalArgumentException e) {
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                        httpExchange.getResponseBody().close();
                    } catch (NoSuchElementException e){

                    }

                } else {

                    try {
                        HttpQuery httpQuery = HttpQuery.Head(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));

                        HttpQueryResult headQueryResult = httpQuery.execute();
                        Long timestamp = headQueryResult.getTimestamp();

                        if (timestamp < 0) {
                            continue;
                        }

                        if (headQueryResult.getStatusCode() == HttpHelpers.STATUS_BAD_ARGUMENT) {
                            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                            httpExchange.getResponseBody().close();
                            return;
                        }

                        ListOfReplicas listOfReplicas = infoOfReplicasByTimestamp.get(timestamp);
                        if (listOfReplicas == null) {
                            listOfReplicas = new ListOfReplicas();
                            infoOfReplicasByTimestamp.put(timestamp, listOfReplicas);
                        }
                        listOfReplicas.add(replicaHost);

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        throw new IOException();
                    }
                }
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


            if (replicasWithNeedingValue.size() >= replicaParameters.ack()) {
                if (replicasWithNeedingValue.contains(myReplicaHost)) {
                    try (DAOValue daoValue = dao.get(id)) {
                        httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, daoValue.size());
                        IOHelpers.copy(daoValue.getInputStream(), httpExchange.getResponseBody());
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

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        throw new IOException();
                    }
                }
            } else {
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS, 0);
                httpExchange.getResponseBody().close();
            }

        // запрос от реплики
        } else {
            try (DAOValue value = dao.get(id)){

                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_GET, 0);
                IOHelpers.copy(value.getInputStream(), httpExchange.getResponseBody());
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
