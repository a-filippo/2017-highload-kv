package ru.mail.polis;

import java.io.IOException;

import org.apache.http.protocol.HTTP;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    protected final String myReplicaHost;

    @NotNull
    protected final HttpExchange httpExchange;

    @NotNull
    protected final ListOfReplicas replicasHosts;

    @NotNull
    protected final ReplicaParameters replicaParameters;

    @NotNull
    protected final ListOfReplicas fromReplicas;

    @Nullable
    protected final String nextReplica;

    public MyServiceEntityAction(@NotNull MyServiceParameters myServiceParameters) {
        this.myServiceParameters = myServiceParameters;

        this.dao = myServiceParameters.getDao();
        this.myReplicaHost = myServiceParameters.getMyReplicaHost();
        this.httpExchange = myServiceParameters.getHttpExchange();
        this.replicasHosts = myServiceParameters.getReplicasHosts();

        ServiceQueryParameters serviceQueryParameters = new ServiceQueryParameters(httpExchange.getRequestURI().getQuery());
        this.replicaParameters = serviceQueryParameters.getReplicaParameters(replicasHosts.size());
        this.id = serviceQueryParameters.getId();

        this.fromReplicas = getFromReplicas(httpExchange);

        this.nextReplica = findNextReplica();
    }

    public abstract void execute() throws IOException;

    @NotNull
    private ListOfReplicas getFromReplicas(@NotNull HttpExchange httpExchange){
        String fromStorage = httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_FROM_REPLICAS);
        return new ListOfReplicas(fromStorage);
    }

    @Nullable
    private String findNextReplica(){
        if (fromReplicas.size() < replicaParameters.from() - 1){
            for (String replicaHost : replicasHosts){
                if (!fromReplicas.contains(replicaHost)){
                    return replicaHost;
                }
            }
        }
        return null;
    }

    protected int getSize(){
        return Integer.parseInt(httpExchange.getRequestHeaders().getFirst(HTTP.CONTENT_LEN));
    }
}
