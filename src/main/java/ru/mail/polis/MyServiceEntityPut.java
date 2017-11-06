package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.conn.ConnectTimeoutException;
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

    @Override
    public void processQueryFromReplica() throws IOException {
        try (DAOValue value = new DAOValue(getRequestInputStream(), getSize(), getTimestamp())) {
            dao.put(id, value);
            sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_PUT);
        }
    }

    @Override
    public void processQueryFromClient() throws IOException {
        int size = getSize();
        final long timestamp = getCurrentTimestamp();

        TemporaryValueStorage temporaryValueStorage = new TemporaryValueStorage(
                dao.getStoragePath() + File.separator + DAOStorage.TEMP_PATH,
                getRequestInputStream(),
                size
            );

        ResultsOfReplicasAnswer results = forEachNeedingReplica(replicaHost -> {

            if (replicaHost.equals(myReplicaHost)) {
                return threadPool.addWork(() -> {
                    ResultOfReplicaAnswer result = new ResultOfReplicaAnswer(myReplicaHost);

                    DAOValue value = new DAOValue(temporaryValueStorage.getInputStream(), size, timestamp);
                    dao.put(id, value);
                    value.close();
                    result.workingReplica();
                    result.successOperation();
                    return result;
                });

            } else {

                return threadPool.addWork(() -> {
                    ResultOfReplicaAnswer result = new ResultOfReplicaAnswer(myReplicaHost);

                    try {
                        HttpQuery putHttpQuery = HttpQuery.Put(sameQueryOnReplica(replicaHost));
                        putHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        putHttpQuery.addTimestamp(timestamp);

                        InputStream inputStream = temporaryValueStorage.getInputStream();
                        putHttpQuery.setBody(inputStream, size);

                        HttpQueryResult httpQueryResult = putHttpQuery.execute();
                        result.workingReplica();
                        inputStream.close();

                        switch (httpQueryResult.getStatusCode()) {
                            case HttpHelpers.STATUS_SUCCESS_PUT:
                                result.successOperation();
                                break;
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

        temporaryValueStorage.close();

        if (results.getSuccessOperations() < replicaParameters.ack()){
            sendEmptyResponse(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS);
        } else {
            sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_PUT);
        }
    }
}
