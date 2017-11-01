package ru.mail.polis;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.NoSuchElementException;
import java.util.Set;


import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.dao.DAOValue;
import ru.mail.polis.httpclient.HttpQueryResult;
import ru.mail.polis.httpclient.PutHttpQuery;

public class MyService implements KVService {
    public final static String CONTEXT_ENTITY = "/v0/entity";

    @NotNull
    private final HttpServer httpServer;

//    private final int replicasCount;

//    @NotNull
//    private final ReplicaParameters defaultReplicaParameters;

    @NotNull
    private final ListOfReplicas replicasHosts;

    @NotNull
    private final String myReplicaHost;

    @NotNull
    private final DAO dao;

    public MyService(int port, @NotNull DAO dao, Set<String> replicas) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;

//        HttpQuery.setHttpQueryPool(new HttpQueryPool());

        this.myReplicaHost = "http://localhost:" + port;

//        this.replicasCount = replicas.size();
//        this.defaultReplicaParameters = getDefaultReplicas(this.replicasCount);
        this.replicasHosts = new ListOfReplicas(replicas);
        this.replicasHosts.remove(this.myReplicaHost);

        try {
            this.httpServer.createContext("/v0/status", httpExchange -> {
                final String response = "ONLINE";
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS, response.length());
                httpExchange.getResponseBody().write(response.getBytes());
                httpExchange.close();
            });

            this.httpServer.createContext(CONTEXT_ENTITY, httpExchange -> {
                try {
                    MyServiceParameters myServiceParameters = new MyServiceParameters()
                            .setHttpExchange(httpExchange)
                            .setDao(dao)
                            .setMyReplicaHost(myReplicaHost)
                            .setReplicasHosts(replicasHosts);


//                    ServiceQueryParameters parameters = new ServiceQueryParameters(httpExchange.getRequestURI().getQuery());
//                    final String id = parameters.getId();
//                    ReplicaParameters replicaParameters = parameters.getReplicaParameters();
//
//                    ListOfReplicas fromReplicas = getFromReplicas(httpExchange);
//                    String nextReplica = findNextReplica(fromReplicas, replicaParameters);
//                    System.out.println(httpExchange.getRequestHeaders().values().toString());
                    switch (httpExchange.getRequestMethod()) {
                        case "GET":
                            MyServiceEntityGet myServiceEntityGet = new MyServiceEntityGet(myServiceParameters);
                            myServiceEntityGet.execute();
                            break;

                        case "PUT":
                            MyServiceEntityPut myServiceEntityPut = new MyServiceEntityPut(myServiceParameters);
                            myServiceEntityPut.execute();
                            break;

                        case "DELETE":
                            MyServiceEntityDelete myServiceEntityDelete = new MyServiceEntityDelete(myServiceParameters);
                            myServiceEntityDelete.execute();
                            break;

                        case "HEAD":
                            System.out.println(httpExchange.getRequestHeaders().values().toString());
                            MyServiceEntityHead myServiceEntityHead = new MyServiceEntityHead(myServiceParameters);
                            myServiceEntityHead.execute();
                            break;

                        default:
                            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
                            httpExchange.getResponseBody().close();
                    }
                } catch (IllegalIdException e){
                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                    httpExchange.getResponseBody().close();
                } catch (NoSuchReplicasException e){
                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_ENOUGH_REPLICAS, 0);
                    httpExchange.getResponseBody().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        this.httpServer.start();
    }

    @Override
    public void stop() {
        this.httpServer.stop(0);
    }

    @NotNull
    private ListOfReplicas getFromReplicas(@NotNull HttpExchange httpExchange){
        String fromStorage = httpExchange.getRequestHeaders().getFirst(HttpHelpers.HEADER_FROM_REPLICAS);
        return new ListOfReplicas(fromStorage);
    }

    @Nullable
    private String findNextReplica(ListOfReplicas fromReplicas, ReplicaParameters parameters){
        if (fromReplicas.size() < parameters.from() - 1){
            for (String replicaHost : replicasHosts){
                if (!fromReplicas.contains(replicaHost)){
                    return replicaHost;
                }
            }
        }
        return null;
    }

    private ReplicaParameters getDefaultReplicas(int clusterSize){
        return new ReplicaParameters(clusterSize / 2 + 1, clusterSize);
    }
}