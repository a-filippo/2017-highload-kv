package ru.mail.polis;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Starts storage and waits for shutdown
 *
 * @author Vadim Tsesko <mail@incubos.org>
 */
public final class Server {
    private static final int[] PORTS = {8080, 8081, 8082, 8083};

    private Server() {
        // Not instantiable
    }

    public static void main(String[] args) throws IOException {
        Set<String> replicas = new HashSet<>(2 * PORTS.length);

        for (final int port : PORTS){
            replicas.add("http://localhost:" + port);
        }

        for (final int port : PORTS){
            startOneNode(port, replicas);
        }
    }

    private static void startOneNode(int port, Set<String> replicas) throws IOException{
        // Temporary storage in the file system
        final File data = Files.createTempDirectory();

        // Start the storage
        final KVService storage =
                KVServiceFactory.create(
                        port,
                        data,
                        replicas);
        storage.start();
        Runtime.getRuntime().addShutdownHook(new Thread(storage::stop));
    }
}
