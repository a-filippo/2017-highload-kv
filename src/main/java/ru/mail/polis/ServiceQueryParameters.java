package ru.mail.polis;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ServiceQueryParameters {
    @Nullable
    private String id = null;
    @Nullable
    private ReplicaParameters replicaParameters = null;

    public ServiceQueryParameters(Map<String, List<String>> parameters) throws IllegalIdException {
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            switch (entry.getKey()){
                case "id":
                    if (entry.getValue() == null || entry.getValue().isEmpty()){
                        throw new IllegalIdException("id key is null");
                    }
                    id = entry.getValue().get(0);
                    break;
                case "replicas":
                    if (entry.getValue() == null || entry.getValue().isEmpty()){
                        break;
                    }
                    replicaParameters = new ReplicaParameters(entry.getValue().get(0));
                    break;
            }

        }
    }

    @NotNull
    String getId() {
        if (id == null){
            throw new IllegalArgumentException();
        }
        return id;
    }

    @NotNull
    ReplicaParameters getReplicaParameters(int replicasCount) throws ReplicaParametersException {
        if (replicaParameters == null){
            return new ReplicaParameters(replicasCount / 2 + 1, replicasCount);
        } else if (
                replicaParameters.from() > replicasCount ||
                replicaParameters.ack() > replicaParameters.from() ||
                replicaParameters.ack() < 1){
            throw new ReplicaParametersException("Ack: " + replicaParameters.ack() + ", from: " + replicaParameters.from());
        } else {
            return replicaParameters;
        }
    }

    public String getQuery(){
        if (replicaParameters == null){
            return "id=" + id;
        } else {
            return "id=" + id + "&replicas=" + replicaParameters.ack() + "/" + replicaParameters.from();
        }
    }

    ServiceQueryParameters(@NotNull String query) throws IllegalIdException {
        String[] parameters = query.split("&");
        for (final String parameter : parameters){
            String[] parameterSplit = parameter.split("=");
            switch (parameterSplit[0]){
                case "id":
                    if (parameterSplit.length == 1){
                        throw new IllegalIdException("id key is null");
                    }
                    id = parameterSplit[1];
                    break;
                case "replicas":
                    replicaParameters = new ReplicaParameters(parameterSplit[1]);
                    break;
            }
        }
    }
}
