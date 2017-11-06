package ru.mail.polis;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import fi.iki.elonen.NanoHTTPD;
import ru.mail.polis.dao.DAO;

public class StorageHttpServer extends NanoHTTPD{

    @NotNull
    private ListOfReplicas replicasHosts;

    @NotNull
    private final ThreadPoolReplicasQuerys threadPool;

    @NotNull
    private String myReplicaHost;

    @NotNull
    private DAO dao;

    public StorageHttpServer(int port) {
        super(port);

        threadPool = new ThreadPoolReplicasQuerys();
        threadPool.start();
    }

    public void setReplicasHosts(@NotNull ListOfReplicas replicasHosts) {
        this.replicasHosts = replicasHosts;
    }

    public void setMyReplicaHost(@NotNull String myReplicaHost) {
        this.myReplicaHost = myReplicaHost;
    }

    public void setDao(@NotNull DAO dao) {
        this.dao = dao;
    }

    @Override
    public Response serve(IHTTPSession session) {
        switch (session.getUri()){
            case "/v0/status":
                return newFixedLengthResponse(Response.Status.OK, null, "ONLINE");
            case MyService.CONTEXT_ENTITY:
                try {
                    System.out.println(session.getParameters().toString());

                    MyServiceParameters myServiceParameters = new MyServiceParameters()
//                            .setHttpExchange(httpExchange)
                            .setSession(session)
                            .setDao(dao)
                            .setThreadPool(threadPool)
                            .setMyReplicaHost(myReplicaHost)
                            .setReplicasHosts(replicasHosts);

                    switch (session.getMethod()) {
                        case GET:
                            MyServiceEntityGet myServiceEntityGet = new MyServiceEntityGet(myServiceParameters);
                            return myServiceEntityGet.execute();
//                            break;

                        case PUT:
                            MyServiceEntityPut myServiceEntityPut = new MyServiceEntityPut(myServiceParameters);
                            return myServiceEntityPut.execute();
//                            break;

                        case DELETE:
                            MyServiceEntityDelete myServiceEntityDelete = new MyServiceEntityDelete(myServiceParameters);
                            return myServiceEntityDelete.execute();
//                            break;

                        case HEAD:
                            MyServiceEntityHead myServiceEntityHead = new MyServiceEntityHead(myServiceParameters);
                            return myServiceEntityHead.execute();
//                            break;

                        default:
                            return newFixedLengthResponse(Response.Status.NOT_FOUND, null, null);
                    }
                } catch (IllegalIdException | ReplicaParametersException e){
//                    httpExchange.sendResponseHeaders(HttpHelpers.STATUS_BAD_ARGUMENT, 0);
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, null, null);
                } catch (IOException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.BAD_REQUEST, null, null);
                }
            default:
                return newFixedLengthResponse(Response.Status.NOT_FOUND, null, null);
        }
//        this.httpServer.createContext(, httpExchange -> {
//            final String response = "ONLINE";
//            httpExchange.sendResponseHeaders(HttpHelpers.STATUS_SUCCESS, response.length());
//            httpExchange.getResponseBody().write(response.getBytes());
//            httpExchange.close();
//        });


//        this.ClientHandler
//        Async
//        session.
//        session.
//        Map<String, List<String>> parameters = session.getParameters();
//
//        session.getInputStream();
//
//        session.getUri();
//
//        newFixedLengthResponse()
//
//        return newFixedLengthResponse("sfsadf");



        //        return super.serve(session);
    }

    public void startServer() throws IOException {
        this.start();
    }

    public void stopServer(){
        this.stop();
    }
}
