package ru.mail.polis;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import com.sun.net.httpserver.HttpServer;

import ru.mail.polis.dao.DAO;

public class MyService implements KVService {
    public final static String CONTEXT_ENTITY = "/v0/entity";

    @NotNull
    private final HttpServer httpServer;

    @NotNull
    private final ListOfReplicas replicasHosts;

    @NotNull
    private final ThreadPoolReplicasQuerys threadPool;

    @NotNull
    private final String myReplicaHost;

    @NotNull
    private final DAO dao;

    public MyService(int port, @NotNull DAO dao, Set<String> replicas) throws IOException {
//        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer = null;

        StorageHttpServer storageHttpServer = new StorageHttpServer(port);


        this.dao = dao;

        this.myReplicaHost = "http://localhost:" + port;

        this.replicasHosts = new ListOfReplicas(replicas);
        this.replicasHosts.remove(this.myReplicaHost);

        this.threadPool = new ThreadPoolReplicasQuerys();
        storageHttpServer.setDao(dao);
        storageHttpServer.setMyReplicaHost(myReplicaHost);
        storageHttpServer.setReplicasHosts(replicasHosts);
        storageHttpServer.start();

//        this.createRouters();
    }

    private void createRouters(){
        try {

            this.httpServer.createContext("/v0/status", httpExchange -> {
                final String response = "ONLINE";
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS, response.length());
                httpExchange.getResponseBody().write(response.getBytes());
                httpExchange.close();
            });

            this.httpServer.createContext(CONTEXT_ENTITY, httpExchange -> {
                try {
                    System.out.println(httpExchange.getRequestHeaders().values().toString());

                    MyServiceParameters myServiceParameters = new MyServiceParameters()
                            .setHttpExchange(httpExchange)
                            .setDao(dao)
                            .setThreadPool(threadPool)
                            .setMyReplicaHost(myReplicaHost)
                            .setReplicasHosts(replicasHosts);

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
                            MyServiceEntityHead myServiceEntityHead = new MyServiceEntityHead(myServiceParameters);
                            myServiceEntityHead.execute();
                            break;

                        default:
                            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_NOT_FOUND, 0);
                            httpExchange.getResponseBody().close();
                            httpExchange.close();
                    }
                } catch (IllegalIdException | ReplicaParametersException e){
                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                    httpExchange.getResponseBody().close();
                    httpExchange.close();
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
        this.threadPool.start();
//        this.httpServer.start();
    }

    @Override
    public void stop() {
//        this.httpServer.stop(0);
        this.threadPool.stop();
    }
}