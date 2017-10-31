package ru.mail.polis;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.GetHttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;
import ru.mail.polis.httpclient.PutHttpQuery;

public class MyServiceEntityGet extends MyServiceEntityAction{

    public MyServiceEntityGet(@NotNull MyServiceParameters myServiceParameters) {
        super(myServiceParameters);
    }

    @Override
    public void execute() throws IOException {
        try (DAOValue value = dao.get(id);
             OutputStream outputStream = httpExchange.getResponseBody()) {

            ListOfReplicas listOfSuccessReplicasGet = new ListOfReplicas();

            List<String> listOfReplicas = findReplicas(id);




            if (nextReplica != null){
                GetHttpQuery getHttpQuery;
                try {
                    getHttpQuery = new GetHttpQuery(new URI(nextReplica + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery()));
                    ListOfReplicas replicasToRequest = new ListOfReplicas(fromReplicas);
                    replicasToRequest.add(myReplicaHost);
                    getHttpQuery.addReplicasToRequest(replicasToRequest);

//                    getHttpQuery.setBody(value.getProxedInputStream(), size);

                    HttpQueryResult httpQueryResult = getHttpQuery.execute();

                    if (httpQueryResult.getStatusCode() == HttpHelpers.STATUS_SUCCESS_GET){
                        listOfSuccessReplicasGet = httpQueryResult.getListOfSuccessReplicas();
                        if (httpQueryResult.getHash().equals(value.getHash())) {
                            listOfSuccessReplicasGet.add(myReplicaHost);
                            // what next?
                        } else {

                        }
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

            listOfSuccessReplicasGet.add(myReplicaHost);

            if (!fromReplicas.empty()) {
                httpExchange.getResponseHeaders().add(HttpHelpers.HEADER_FROM_REPLICAS, listOfSuccessReplicasGet.toLine());
            }











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
