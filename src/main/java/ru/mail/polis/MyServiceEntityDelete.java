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
    public void execute() throws IOException {
        List<String> listOfReplicasForRequest = findReplicas(id);

        int from = replicaParameters.from();

        int countOfWorkingReplicas = 0;
        int countOfSuccess = 0;

        if (fromReplicas.empty()) {

            for (int i = 0; i < from; i++) {
                String replicaHost = listOfReplicasForRequest.get(i);

                if (replicaHost.equals(myReplicaHost)) {
                    dao.delete(id);
                    countOfSuccess++;
                    countOfWorkingReplicas++;
                } else {

                    try {
                        HttpQuery deleteHttpQuery = HttpQuery.Delete(new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));

                        deleteHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        HttpQueryResult httpQueryResult = deleteHttpQuery.execute();

                        countOfWorkingReplicas++;

                        if (httpQueryResult.getStatusCode() == HttpHelpers.STATUS_SUCCESS_DELETE) {
                            countOfSuccess++;
                        }

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (HttpHostConnectException e) {
                        // nothing
                    }
                }

            }

            if (countOfWorkingReplicas < replicaParameters.ack()){
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS, 0);
                httpExchange.getResponseBody().close();
                return;
            } else {
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS_DELETE, 0);
                httpExchange.getResponseBody().close();
            }


        } else {
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
}
