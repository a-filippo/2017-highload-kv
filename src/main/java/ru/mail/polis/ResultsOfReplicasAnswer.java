package ru.mail.polis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResultsOfReplicasAnswer {
    Map<Long, ListOfReplicas> infoOfReplicasByTimestamp;
    Set<Long> deletedTimestamps;

    private boolean badArgument = false;
    private int notFound = 0;
    private int workingReplicas = 0;
    private int successOperations = 0;

    public ResultsOfReplicasAnswer(List<ResultOfReplicaAnswer> results){
        infoOfReplicasByTimestamp = new HashMap<>();
        deletedTimestamps = new HashSet<>();

        for (ResultOfReplicaAnswer result : results){
            badArgument = badArgument || result.isBadArgument();

            if (result.isNotFound()){
                notFound++;
            }

            if (result.isWorkingReplica()){
                workingReplicas++;
            }

            if (result.isSuccessOperation()){
                successOperations++;
            }

            if (result.getValueTimestamp() > 0){
                addToListOfReplicasInMap(infoOfReplicasByTimestamp, result.getValueTimestamp(), result.getReplicaHost());
            }

            if (result.getDeletedTimestamp() > 0){
                deletedTimestamps.add(result.getDeletedTimestamp());
            }
        }
    }

    public boolean isBadArgument() {
        return badArgument;
    }

    public int getNotFound() {
        return notFound;
    }

    public int getWorkingReplicas() {
        return workingReplicas;
    }

    public int getSuccessOperations() {
        return successOperations;
    }

    public Map<Long, ListOfReplicas> getInfoOfReplicasByTimestamp() {
        return infoOfReplicasByTimestamp;
    }

    public Set<Long> getDeletedTimestamps() {
        return deletedTimestamps;
    }

    private void addToListOfReplicasInMap(Map<Long, ListOfReplicas> infoOfReplicasByTimestamp, Long timestamp, String replicaHost) {
        ListOfReplicas listOfReplicas = infoOfReplicasByTimestamp.get(timestamp);
        if (listOfReplicas == null) {
            listOfReplicas = new ListOfReplicas();
            infoOfReplicasByTimestamp.put(timestamp, listOfReplicas);
        }
        listOfReplicas.add(replicaHost);
    }
}
