package ru.mail.polis;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class ServiceQueryParameters {
    @Nullable
    private String id = null;
    @Nullable
    private ReplicaParameters replicaParameters = null;

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
