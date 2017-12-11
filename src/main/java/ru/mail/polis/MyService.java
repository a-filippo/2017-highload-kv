package ru.mail.polis;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.Executors;

import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.HttpServer;

import ru.mail.polis.dao.DAO;
import ru.mail.polis.httpclient.HttpQueryCreator;
import ru.mail.polis.myserviceentity.MyServiceEntityDelete;
import ru.mail.polis.myserviceentity.MyServiceEntityGet;
import ru.mail.polis.myserviceentity.MyServiceEntityPut;
import ru.mail.polis.myserviceentity.MyServiceParameters;
import ru.mail.polis.myserviceentity.ThreadPoolReplicasQuerys;
import ru.mail.polis.replicahelpers.ListOfReplicas;
import ru.mail.polis.replicahelpers.ReplicaParametersException;

public class MyService implements KVService {
    public final static String CONTEXT_ENTITY = "/v0/entity";

    @NotNull
    private final HttpServer httpServer;

    @NotNull
    private final ListOfReplicas replicasHosts;

    @NotNull
    private final ThreadPoolReplicasQuerys threadPool;

    @NotNull
    private final HttpQueryCreator httpQueryCreator;

    @NotNull
    private final String myReplicaHost;

    @NotNull
    private final DAO dao;

    public MyService(int port, @NotNull DAO dao, Set<String> replicas) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.httpServer.setExecutor(Executors.newFixedThreadPool(8));

        this.dao = dao;

        this.myReplicaHost = "http://localhost:" + port;

        this.replicasHosts = new ListOfReplicas(replicas);
        this.replicasHosts.remove(this.myReplicaHost);

        this.httpQueryCreator = new HttpQueryCreator();

        this.threadPool = new ThreadPoolReplicasQuerys();

        this.createRouters();
    }

    private void createRouters(){
        try {

            this.httpServer.createContext("/v0/status", httpExchange -> {
                final byte[] response = "ONLINE".getBytes();
                httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS, response.length);
                httpExchange.getResponseBody().write(response);
                httpExchange.close();
            });

            this.httpServer.createContext(CONTEXT_ENTITY, httpExchange -> {
                try {
                    MyServiceParameters myServiceParameters = new MyServiceParameters()
                            .setHttpExchange(httpExchange)
                            .setDao(dao)
                            .setHttpQueryCreator(httpQueryCreator)
                            .setThreadPool(threadPool)
                            .setMyReplicaHost(myReplicaHost)
                            .setReplicasHosts(replicasHosts);

                    switch (httpExchange.getRequestMethod()) {
                        case "GET":
                            MyServiceEntityGet myServiceEntityGet = new MyServiceEntityGet(myServiceParameters);
                            myServiceEntityGet.execute();
                            if (httpExchange.getResponseCode() >= 400){
                                System.out.println(httpExchange.getResponseCode());
                                System.out.println(httpExchange.getResponseHeaders());
                            }
                            break;

                        case "PUT":
                            MyServiceEntityPut myServiceEntityPut = new MyServiceEntityPut(myServiceParameters);
                            myServiceEntityPut.execute();
                            break;

                        case "DELETE":
                            MyServiceEntityDelete myServiceEntityDelete = new MyServiceEntityDelete(myServiceParameters);
                            myServiceEntityDelete.execute();
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
                    e.printStackTrace();
                } catch (IOException e) {
                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_INTERNAL_ERROR, 0);
                    httpExchange.getResponseBody().close();
                    httpExchange.close();
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
        this.httpServer.start();
        this.httpQueryCreator.start();
    }

    @Override
    public void stop() {
        try {
            dao.stop();
        } catch (IOException e){
            e.printStackTrace();
        }
        this.httpServer.stop(0);
        this.httpQueryCreator.stop();
        this.threadPool.stop();
    }
}