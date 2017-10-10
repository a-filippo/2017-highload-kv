package ru.mail.polis;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

import com.sun.net.httpserver.HttpServer;

import ru.mail.polis.DAO.DAO;
import ru.mail.polis.DAO.DAOValue;

public class MyService implements KVService {
    @NotNull
    private final HttpServer httpServer;

    @NotNull
    private final DAO dao;

    public MyService(int port, @NotNull DAO dao) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        this.dao = dao;

        try {
            this.httpServer.createContext("/v0/status", httpExchange -> {
                final String response = "ONLINE";
                httpExchange.sendResponseHeaders(200, response.length());
                httpExchange.getResponseBody().write(response.getBytes());
                httpExchange.close();
            });

            this.httpServer.createContext("/v0/entity", httpExchange -> {
                try {
                    final String id = extractId(httpExchange.getRequestURI().getQuery());

                    switch (httpExchange.getRequestMethod()) {
                        case "GET":
                            try (DAOValue value = dao.get(id);
                                 OutputStream os = httpExchange.getResponseBody()) {

                                httpExchange.sendResponseHeaders(200, value.size());
                                final byte[] buffer = new byte[64 * 1024];
                                int count;
                                InputStream is = value.getInputStream();
                                while ((count = is.read(buffer)) >= 0) {
                                    os.write(buffer, 0, count);
                                }
                            } catch (NoSuchElementException e) {
                                httpExchange.sendResponseHeaders(404, 0);
                                httpExchange.getResponseBody().close();
                            } catch (IllegalArgumentException e) {
                                httpExchange.sendResponseHeaders(400, 0);
                                httpExchange.getResponseBody().close();
                            }
                            break;

                        case "PUT":
                            int size = Integer.parseInt(httpExchange.getRequestHeaders().getFirst("Content-Length"));

                            try (DAOValue value = new DAOValue(httpExchange.getRequestBody(), size)) {
                                dao.put(id, value);
                                httpExchange.sendResponseHeaders(201, 0);
                                httpExchange.getResponseBody().close();
                            } catch (IllegalArgumentException e) {
                                httpExchange.sendResponseHeaders(400, 0);
                                httpExchange.getResponseBody().close();
                            }
                            break;

                        case "DELETE":
                            try {
                                dao.delete(id);
                                httpExchange.sendResponseHeaders(202, 0);
                                httpExchange.getResponseBody().close();
                            } catch (IllegalArgumentException e) {
                                httpExchange.sendResponseHeaders(400, 0);
                                httpExchange.getResponseBody().close();
                            }
                            break;

                        default:
                            httpExchange.sendResponseHeaders(404, 0);
                            httpExchange.getResponseBody().close();
                    }

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
    private static String extractId(@NotNull String query){
        if (!query.startsWith("id=")){
            throw new IllegalArgumentException();
        }

        return query.substring(3);
    }
}