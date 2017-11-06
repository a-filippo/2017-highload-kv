package ru.mail.polis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import ru.mail.polis.dao.DAO;

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
    private final HttpExchange httpExchange;

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
        this.httpExchange = myServiceParameters.getHttpExchange();
        this.replicasHosts = myServiceParameters.getReplicasHosts();
        this.threadPool = myServiceParameters.getThreadPool();

        ServiceQueryParameters serviceQueryParameters = new ServiceQueryParameters(httpExchange.getRequestURI().getQuery());
        this.replicaParameters = serviceQueryParameters.getReplicaParameters(replicasHosts.size() + 1);
        this.id = serviceQueryParameters.getId();

        this.fromReplicas = getFromReplicas(httpExchange);
    }

    final public void execute() throws IOException {
        if (fromReplicas.empty()) {
            processQueryFromClient();
        } else {
            processQueryFromReplica();
        }
    }

    public abstract void processQueryFromReplica() throws IOException;

    public abstract void processQueryFromClient() throws IOException;

    @NotNull
    private ListOfReplicas getFromReplicas(@NotNull HttpExchange httpExchange){
        String fromStorage = httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_FROM_REPLICAS);
        return new ListOfReplicas(fromStorage);
    }

    protected long getTimestamp(){
        String timestampString = httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_TIMESTAMP);
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
        return Integer.parseInt(httpExchange.getRequestHeaders().getFirst(HTTP.CONTENT_LEN));
    }

    protected void sendEmptyResponse(int status) throws IOException {
        httpExchange.sendResponseHeaders(status, 0);
        httpExchange.getResponseBody().close();
        httpExchange.close();
    }

    protected void sendResponse(int status, int size, InputStream inputStream) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();
        httpExchange.sendResponseHeaders(status, size);
        IOHelpers.copy(inputStream, outputStream);
        inputStream.close();
        outputStream.close();
        httpExchange.close();
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
        return httpExchange.getRequestBody();
    }

    protected Headers getResponseHeaders(){
        return httpExchange.getResponseHeaders();
    }

    protected Headers getRequestHeaders(){
        return httpExchange.getRequestHeaders();
    }

    protected URI sameQueryOnReplica(String replicaHost) throws URISyntaxException {
        return new URI(replicaHost + MyService.CONTEXT_ENTITY + "?" + httpExchange.getRequestURI().getQuery());
    }

    interface ForEachReplicaInQueryFromClient{
        Future<ResultOfReplicaAnswer> execute(String replica) throws IOException;
    }
}
