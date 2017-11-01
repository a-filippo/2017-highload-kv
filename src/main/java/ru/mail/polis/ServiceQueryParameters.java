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
    ReplicaParameters getReplicaParameters(int replicasCount) throws NoSuchReplicasException{
        if (replicaParameters == null){
            return new ReplicaParameters(replicasCount / 2 + 1, replicasCount);
        } else if (
                replicaParameters.from() > replicasCount ||
                replicaParameters.ack() > replicaParameters.from() ||
                replicaParameters.ack() > replicasCount){
            throw new NoSuchReplicasException("Ack: " + replicaParameters.ack() + ", from: " + replicaParameters.from());
        } else {
            return replicaParameters;
        }
    }

    ServiceQueryParameters(@NotNull String query){
        String[] parameters = query.split("&");
        for (final String parameter : parameters){
            String[] parameterSplit = parameter.split("=");
            switch (parameterSplit[0]){
                case "id":
                    id = parameterSplit[1];
                    break;
                case "replicas":
                    replicaParameters = new ReplicaParameters(parameterSplit[1]);
                    break;
            }
        }
    }
}
