package ru.mail.polis.myserviceentity;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.Headers;

import ru.mail.polis.HttpHelpers;
import ru.mail.polis.IllegalIdException;
import ru.mail.polis.replicahelpers.ListOfReplicas;
import ru.mail.polis.replicahelpers.ReplicaParametersException;
import ru.mail.polis.replicahelpers.ResultOfReplicaAnswer;
import ru.mail.polis.replicahelpers.ResultsOfReplicasAnswer;
import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.HttpQuery;
import ru.mail.polis.httpclient.HttpQueryResult;

public class MyServiceEntityGet extends MyServiceEntityAction{

    public MyServiceEntityGet(@NotNull MyServiceParameters myServiceParameters) throws ReplicaParametersException, IllegalIdException {
        super(myServiceParameters);
    }

    @Override
    public void processQueryFromReplica() throws IOException {
        try (DAOValue value = dao.get(id)){

            if (HttpHelpers.HEADER_GET_INFO_VALUE.equals(getRequestHeaders().getFirst(HttpHelpers.HEADER_GET_INFO))){

                Headers headers = getResponseHeaders();
                headers.add(HttpHelpers.HEADER_TIMESTAMP, String.valueOf(value.timestamp()));
                headers.add(HttpHelpers.HEADER_SIZE, String.valueOf(value.size()));

                sendEmptyResponse(HttpHelpers.STATUS_SUCCESS_GET);
            } else {
                if (value.size() < 0){
                    sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
                } else {
                    sendResponse(HttpHelpers.STATUS_SUCCESS_GET, value.size(), value.getInputStream());
                }
            }
        } catch (NoSuchElementException e) {
            sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
        } catch (IllegalArgumentException e) {
            sendEmptyResponse(HttpHelpers.STATUS_BAD_ARGUMENT);
        }
    }

    @Override
    public void processQueryFromClient() throws IOException {
        ResultsOfReplicasAnswer results = forEachNeedingReplica(replicaHost -> {

            if (replicaHost.equals(myReplicaHost)) {

                return threadPool.addWork(() -> {
                    ResultOfReplicaAnswer result = new ResultOfReplicaAnswer(myReplicaHost);

                    try (DAOValue daoValue = dao.get(id)) {
                        Long timestamp = daoValue.timestamp();

                        if (daoValue.size() < 0){
                            result.setDeleted(timestamp);
                        }

                        result.setValueTimestamp(timestamp);
                        result.workingReplica();

                    } catch (IllegalArgumentException e) {
                        result.badArgument();
                    } catch (NoSuchElementException e){
                        result.notFound();
                        result.workingReplica();
                    }

                    return result;
                });

            } else {

                return threadPool.addWork(() -> {

                    ResultOfReplicaAnswer result = new ResultOfReplicaAnswer(replicaHost);

                    try {
                        HttpQuery httpQuery = httpQueryCreator.get(sameQueryOnReplica(replicaHost));

                        httpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));
                        httpQuery.addHeader(HttpHelpers.HEADER_GET_INFO, HttpHelpers.HEADER_GET_INFO_VALUE);

                        HttpQueryResult headQueryResult = httpQuery.execute();
                        Long timestamp = headQueryResult.getTimestamp();

                        int valueSize = headQueryResult.getValueSize();

                        result.workingReplica();

                        switch (headQueryResult.getStatusCode()) {
                            case HttpHelpers.STATUS_BAD_ARGUMENT:
                                result.badArgument();
                                break;
                            case HttpHelpers.STATUS_NOT_FOUND:
                                result.notFound();
                                break;
                            case HttpHelpers.STATUS_SUCCESS_GET:
                                if (valueSize < 0){
                                    result.setDeleted(timestamp);
                                }
                                result.setValueTimestamp(timestamp);
                                break;
                        }
                        headQueryResult.close();
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    } catch (HttpHostConnectException | ConnectTimeoutException e){
                        // nothing
                    } catch (IOException e){
                        e.printStackTrace();
                    }

                    return result;
                });
            }
        });

        if (results.getWorkingReplicas() < replicaParameters.ack()){
            sendEmptyResponse(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS);
            return;
        }

        Map<Long, ListOfReplicas> infoOfReplicasByTimestamp = results.getInfoOfReplicasByTimestamp();

        int maxTimestampCount = -1;
        long timestampOfMaxCount = -1;
        long maxTimestamp = -1;
        for (Map.Entry<Long, ListOfReplicas> entry : infoOfReplicasByTimestamp.entrySet()) {
            int entryReplicasSize = entry.getValue().size();
            long entryTimestamp = entry.getKey();

            if (entryTimestamp > maxTimestamp){
                maxTimestamp = entryTimestamp;
            }

            if (entryReplicasSize > maxTimestampCount ||
                    entryReplicasSize == maxTimestampCount && entryTimestamp > timestampOfMaxCount) {
                maxTimestampCount = entryReplicasSize;
                timestampOfMaxCount = entryTimestamp;
            }
        }

        Set<Long> deletedTimestamps = results.getDeletedTimestamps();
        if (deletedTimestamps != null && deletedTimestamps.contains(maxTimestamp)){
            sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
            return;
        }

        ListOfReplicas replicasWithNeedingValue = infoOfReplicasByTimestamp.get(timestampOfMaxCount);

        if (replicasWithNeedingValue == null){
            sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
        } else if (results.getWorkingReplicas() >= replicaParameters.ack()) {
            if (replicasWithNeedingValue.contains(myReplicaHost)) {
                try (DAOValue daoValue = dao.get(id)) {
                    if (daoValue.size() < 0) {
                        sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
                    } else {
                        sendResponse(HttpHelpers.STATUS_SUCCESS_GET, daoValue.size(), daoValue.getInputStream());
                    }
                } catch (IllegalArgumentException e) {
                    sendEmptyResponse(HttpHelpers.STATUS_BAD_ARGUMENT);
                } catch (NoSuchElementException e){
                    sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
                }
            } else {
                sendValueFromReplica(replicasWithNeedingValue.toArray(), 0);
            }
        } else if (results.getNotFound() >= replicaParameters.ack()) {
            sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
        } else {
            sendEmptyResponse(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS);
        }
    }

    private void sendValueFromReplica(String[] replicas, int numberOfReplica) throws IOException {
        try {
            HttpQuery getHttpQuery = httpQueryCreator.get(sameQueryOnReplica(replicas[numberOfReplica]));
            getHttpQuery.addReplicasToRequest(new ListOfReplicas(myReplicaHost));

            HttpQueryResult getValueResult = getHttpQuery.execute();
            switch (getValueResult.getStatusCode()){
                case HttpHelpers.STATUS_NOT_FOUND:
                    sendEmptyResponse(HttpHelpers.STATUS_NOT_FOUND);
                    break;
                case HttpHelpers.STATUS_SUCCESS_GET:
                    sendResponse(HttpHelpers.STATUS_SUCCESS_GET, getValueResult.getSizeFromHeader(), getValueResult.getInputStream());
                    break;
            }
            getValueResult.close();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            throw new IOException();
        } catch (HttpHostConnectException | ConnectTimeoutException e){
            sendValueFromReplica(replicas, numberOfReplica + 1);
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
