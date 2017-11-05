package ru.mail.polis;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.conn.HttpHostConnectException;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.httpclient.HttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;

public class MyServiceEntityDelete extends MyServiceEntityAction {
    public MyServiceEntityDelete(@NotNull MyServiceParameters myServiceParameters) throws ReplicaParametersException, IllegalIdException {
        super(myServiceParameters);
    }

    @Override
    public void processQueryFromReplica() throws IOException {
        try {
            dao.delete(id, getTimestamp());
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_DELETE, 0);
            httpExchange.getResponseBody().close();
        } catch (IllegalArgumentException e) {
            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
            httpExchange.getResponseBody().close();
        }
    }

    @Override
    public void processQueryFromClient() throws IOException {
        final long timestamp = getCurrentTimestamp();

        ResultsOfReplicasAnswer results = forEachNeedingReplica(replicaHost -> {
            if (replicaHost.equals(myReplicaHost)) {
                return threadPool.addWork(() -> {
                    ResultOfReplicaAnswer result = new ResultOfReplicaAnswer(myReplicaHost);
                    dao.delete(id, timestamp);
                    result.successOperation();
                    result.workingReplica();
                    return result;
                });
            } else {

                return threadPool.addWork(() -> {
                    ResultOfReplicaAnswer result = new ResultOfReplicaAnswer(myReplicaHost);
                    try {
                        HttpQuery deleteHttpQuery = HttpQuery.Delete(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));

                        deleteHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        deleteHttpQuery.addTimestamp(timestamp);
                        HttpQueryResult httpQueryResult = deleteHttpQuery.execute();

                        result.workingReplica();

                        if (httpQueryResult.getStatusCode() == HttpHelpers.STATUS_SUCCESS_DELETE) {
                            result.successOperation();
                        }

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (HttpHostConnectException e) {
                        // nothing
                    }
                    return result;
                });
            }
        });

        if (results.getWorkingReplicas() < replicaParameters.ack()){
            sendEmptyResponse(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS);
        } else {
            sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_DELETE);
        }
    }
}
