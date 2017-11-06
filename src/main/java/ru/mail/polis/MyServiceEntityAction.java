package ru.mail.polis;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//import com.sun.net.httpserver.Headers;
//import com.sun.net.httpserver.HttpExchange;

import fi.iki.elonen.NanoHTTPD;
import ru.mail.polis.dao.DAO;

//import static ru.mail.polis.HttpHelpers;

abstract public class MyServiceEntityAction {
    @NotNull
    protected MyServiceParameters myServiceParameters;

    @NotNull
    protected final DAO dao;

    @NotNull
    protected final String id;

    @NotNull
    protected final ThreadPoolReplicasQuerys threadPool;

    @NotNull
    protected final String myReplicaHost;

    @NotNull
    protected final ServiceQueryParameters serviceQueryParameters;

    @NotNull
    private NanoHTTPD.IHTTPSession session;

    @Nullable
    private NanoHTTPD.Response response;

//    @NotNull
//    private final HttpExchange httpExchange;

    private Map<String, String> responseHeaders;

    @NotNull
    protected final ListOfReplicas replicasHosts;

    @NotNull
    protected final ReplicaParameters replicaParameters;

    @NotNull
    protected final ListOfReplicas fromReplicas;

    public MyServiceEntityAction(@NotNull MyServiceParameters myServiceParameters) throws ReplicaParametersException, IllegalIdException {
        this.myServiceParameters = myServiceParameters;

        this.dao = myServiceParameters.getDao();
        this.myReplicaHost = myServiceParameters.getMyReplicaHost();
        this.session = myServiceParameters.getSession();
        //        this.httpExchange = myServiceParameters.getHttpExchange();
        this.replicasHosts = myServiceParameters.getReplicasHosts();
        this.threadPool = myServiceParameters.getThreadPool();

        this.serviceQueryParameters = new ServiceQueryParameters(session.getParameters());
        this.replicaParameters = serviceQueryParameters.getReplicaParameters(replicasHosts.size() + 1);
        this.id = serviceQueryParameters.getId();

//        this.fromReplicas = getFromReplicas(httpExchange);
        this.fromReplicas = getFromReplicas(session);

        responseHeaders = new HashMap<>();
    }

    final public NanoHTTPD.Response execute() throws IOException {
        if (fromReplicas.empty()) {
            processQueryFromClient();
        } else {
            processQueryFromReplica();
        }

        if (this.response == null){
            sendEmptyResponse(HttpHelpers.STATUS_INTERNAL_ERROR);
        }

        for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            response.addHeader(entry.getKey(), entry.getValue());
        }
        return response;
    }

    public abstract void processQueryFromReplica() throws IOException;

    public abstract void processQueryFromClient() throws IOException;

//    @NotNull
//    private ListOfReplicas getFromReplicas(@NotNull HttpExchange httpExchange){
//        String fromStorage = httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_FROM_REPLICAS);
//        return new ListOfReplicas(fromStorage);
//    }

    @NotNull
    private ListOfReplicas getFromReplicas(@NotNull NanoHTTPD.IHTTPSession session) {
        String fromStorage = session.getHeaders().get(HttpHelpers.HEADER_FROM_REPLICAS);
        return new ListOfReplicas(fromStorage);
    }

//    protected long getTimestamp(){
//        String timestampString = httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_TIMESTAMP);
//        return timestampString == null ? -1 : Long.valueOf(timestampString);
//    }

    protected long getTimestamp(){
        String timestampString = session.getHeaders().get(HttpHelpers.HEADER_TIMESTAMP);
        return timestampString == null ? -1 : Long.valueOf(timestampString);
    }

    protected List<String> findReplicas(String key){
        ListOfReplicas allReplicas = new ListOfReplicas(replicasHosts);
        allReplicas.add(myReplicaHost);
        List<String> listOfAllReplicas = Arrays.asList(allReplicas.toArray());

        int hash = key.hashCode();
        listOfAllReplicas.sort(Comparator.comparingInt(string -> string.hashCode() ^ hash));

        return listOfAllReplicas;
    }

    protected long getCurrentTimestamp(){
        return System.currentTimeMillis();
    }

    protected int getSize(){
        String length = session.getHeaders().get(HTTP.CONTENT_LEN.toLowerCase());
        if (length == null){
            return 0;
        } else {
            return Integer.parseInt(length);
        }
//        return Integer.parseInt(httpExchange.getRequestHeaders().getFirst(HTTP.CONTENT_LEN));
    }

    protected void sendEmptyResponse(int status) throws IOException {
//        httpExchange.sendResponseHeaders(status, 0);
//        httpExchange.getResponseBody().close();
//        httpExchange.close();
        response = NanoHTTPD.newFixedLengthResponse(getStatus(status), null, null);
    }

    private NanoHTTPD.Response.IStatus getStatus(int status){
        switch (status){
            case 200:
                return NanoHTTPD.Response.Status.OK;
            case 201:
                return NanoHTTPD.Response.Status.CREATED;
            case 202:
                return NanoHTTPD.Response.Status.ACCEPTED;
            case 400:
                return NanoHTTPD.Response.Status.BAD_REQUEST;
            case 404:
                return NanoHTTPD.Response.Status.NOT_FOUND;
            case 500:
                return NanoHTTPD.Response.Status.INTERNAL_ERROR;
            case 504:
                return new NanoHTTPD.Response.IStatus() {
                    @Override
                    public String getDescription() {
                        return "504 Gateway Timeout";
                    }

                    @Override
                    public int getRequestStatus() {
                        return 504;
                    }
                };
            default:
                return null;
        }
    }

    protected void sendResponse(int status, int size, InputStream inputStream) throws IOException {
//        OutputStream outputStream = httpExchange.getResponseBody();
//        httpExchange.sendResponseHeaders(status, size);
//        IOHelpers.copy(inputStream, outputStream);
//        inputStream.close();
//        outputStream.close();
//        httpExchange.close();
        response = NanoHTTPD.newFixedLengthResponse(getStatus(status), null, inputStream, size);
    }

    protected ResultsOfReplicasAnswer forEachNeedingReplica(ForEachReplicaInQueryFromClient forEachReplica) throws IOException {
        List<Future<ResultOfReplicaAnswer>> futures = new ArrayList<>();
        List<String> listOfReplicasForRequest = findReplicas(id);

        int from = replicaParameters.from();

        for (int i = 0; i < from; i++) {
            futures.add(forEachReplica.execute(listOfReplicasForRequest.get(i)));
        }

        List<ResultOfReplicaAnswer> listOfResults = new ArrayList<>();

        for (Future<ResultOfReplicaAnswer> future : futures){
            try {
                listOfResults.add(future.get());
            } catch (ExecutionException | InterruptedException e){
                e.printStackTrace();
                throw new IOException("Error with multithreading");
            }
        }

        return new ResultsOfReplicasAnswer(listOfResults);
    }

    protected InputStream getRequestInputStream(){
//        return httpExchange.getRequestBody();
        return session.getInputStream();
    }

    protected void setResponseHeader(String key, String value){
//        return httpExchange.getResponseHeaders();
        responseHeaders.put(key, value);
    }

//    protected Headers getRequestHeaders(){
//        return httpExchange.getRequestHeaders();
//    }

    protected String getRequestHeader(String value){
        return session.getHeaders().get(value.toLowerCase());
    }

    protected URI sameQueryOnReplica(String replicaHost) throws URISyntaxException {
        return new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + serviceQueryParameters.getQuery());
    }

    interface ForEachReplicaInQueryFromClient{
        Future<ResultOfReplicaAnswer> execute(String replica) throws IOException;
    }
}
