package ru.mail.polis.myserviceentity;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ru.mail.polis.replicahelpers.ResultOfReplicaAnswer;

public class ThreadPoolReplicasQuerys {
    private ExecutorService executor;

    public void start(){
        executor = Executors.newFixedThreadPool(1000);
    }

    public void stop(){
        executor.shutdown();
    }

    public Future<ResultOfReplicaAnswer> addWork(Callable<ResultOfReplicaAnswer> work){
        return executor.submit(work);
    }
}
