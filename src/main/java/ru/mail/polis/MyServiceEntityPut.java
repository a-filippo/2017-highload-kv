package ru.mail.polis;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.HttpExchange;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.HttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;
import ru.mail.polis.httpclient.PutHttpQuery;

public class MyServiceEntityPut extends MyServiceEntityAction {

    public MyServiceEntityPut(@NotNull MyServiceParameters myServiceParameters) throws NoSuchReplicasException {
        super(myServiceParameters);
    }

    @Override
    public void execute() throws IOException{

        int size = getSize();

        long timestamp = getTimestamp();
        if (timestamp < 0){
            timestamp = System.currentTimeMillis();
        }

        try (DAOValue value = new DAOValue(httpExchange.getRequestBody(), size, timestamp)) {

            dao.put(id, value);
            ListOfReplicas listOfSuccessReplicasPut = new ListOfReplicas();;

            if (nextReplica != null){
                HttpQuery putHttpQuery;
                try {
                    putHttpQuery = HttpQuery.Put(new URI(nextReplica + MyService.CONTEXT_ENTITY + "?" +httpExchange.getRequestURI().getQuery()));
                    ListOfReplicas replicasToRequest = new ListOfReplicas(fromReplicas);
                    replicasToRequest.add(myReplicaHost);
                    putHttpQuery.addReplicasToRequest(replicasToRequest);
                    putHttpQuery.addTimestamp(timestamp);

                    putHttpQuery.setBody(value.getProxedInputStream(), size);

                    HttpQueryResult httpQueryResult = putHttpQuery.execute();

                    if (httpQueryResult.getStatusCode() == HttpHelpers.STATUS_SUCCESS_PUT){
                        listOfSuccessReplicasPut = httpQueryResult.getListOfSuccessReplicas();
                    } else {
                        // TODO
                    }

                } catch (Exception e){
                    e.printStackTrace();
                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_INTERNAL_ERROR, 0);
                }
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
}
