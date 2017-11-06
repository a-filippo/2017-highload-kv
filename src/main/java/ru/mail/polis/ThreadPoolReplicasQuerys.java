package ru.mail.polis;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadPoolReplicasQuerys {
    private ExecutorService executor;

    public void start(){
        executor = Executors.newCachedThreadPool();
    }

    public void stop(){
        executor.shutdown();
    }

    public Future<ResultOfReplicaAnswer> addWork(Callable<ResultOfReplicaAnswer> work){
        return executor.submit(work);
    }
}
