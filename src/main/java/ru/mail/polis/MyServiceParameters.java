package ru.mail.polis;


import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.HttpExchange;

import ru.mail.polis.dao.DAO;

public class MyServiceParameters {
    @NotNull
    private DAO dao;

    @NotNull
    private String myReplicaHost;

    @NotNull
    private HttpExchange httpExchange;

    @NotNull
    private ListOfReplicas replicasHosts;

    @NotNull
    public ListOfReplicas getReplicasHosts() {
        return replicasHosts;
    }

    @NotNull
    public MyServiceParameters setReplicasHosts(@NotNull ListOfReplicas replicasHosts) {
        this.replicasHosts = replicasHosts;
        return this;
    }

    @NotNull
    public DAO getDao() {
        return dao;
    }

    @NotNull
    public MyServiceParameters setDao(@NotNull DAO dao) {
        this.dao = dao;
        return this;
    }

    @NotNull
    public String getMyReplicaHost() {
        return myReplicaHost;
    }

    public MyServiceParameters setMyReplicaHost(@NotNull String myReplicaHost) {
        this.myReplicaHost = myReplicaHost;
        return this;
    }

    @NotNull
    public HttpExchange getHttpExchange() {
        return httpExchange;
    }

    @NotNull
    public MyServiceParameters setHttpExchange(@NotNull HttpExchange httpExchange) {
        this.httpExchange = httpExchange;
        return this;
    }
}
