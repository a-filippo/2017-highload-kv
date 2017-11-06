package ru.mail.polis.myserviceentity;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.jetbrains.annotations.NotNull;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.IllegalIdException;
import ru.mail.polis.replicahelpers.ListOfReplicas;
import ru.mail.polis.replicahelpers.ReplicaParametersException;
import ru.mail.polis.replicahelpers.ResultOfReplicaAnswer;
import ru.mail.polis.replicahelpers.ResultsOfReplicasAnswer;
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
            sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_DELETE);
        } catch (IllegalArgumentException e) {
            sendEmptyResponse(HttpHelpers.STATUS_BAD_ARGUMENT);
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
                        HttpQuery deleteHttpQuery = HttpQuery.Delete(sameQueryOnReplica(replicaHost));

                        deleteHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        deleteHttpQuery.addTimestamp(timestamp);
                        HttpQueryResult httpQueryResult = deleteHttpQuery.execute();

                        result.workingReplica();

                        if (httpQueryResult.getStatusCode() == HttpHelpers.STATUS_SUCCESS_DELETE) {
                            result.successOperation();
                        }

                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (HttpHostConnectException | ConnectTimeoutException e){
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
