package ru.mail.polis.replicahelpers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResultOfReplicaAnswer {
    @Nullable
    private Exception exception;

    @NotNull
    private final String replicaHost;

    private boolean workingReplica = false;
    private boolean notFound = false;
    private boolean successOperation = false;
    private boolean badArgument = false;
    private long deletedTimestamp = -1;
    private long valueTimestamp = -1;

    public ResultOfReplicaAnswer(@NotNull String replicaHost) {
        this.replicaHost = replicaHost;
    }

    public void notFound(){
        notFound = true;
    }

    public void setDeleted(long timestamp){
        deletedTimestamp = timestamp;
    }

    public void workingReplica(){
        workingReplica = true;
    }

    public void successOperation(){
        successOperation = true;
    }

    public void setValueTimestamp(long timestamp){
        valueTimestamp = timestamp;
    }

    public void badArgument(){
        badArgument = true;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(@NotNull Exception exception) {
        this.exception = exception;
    }

    @NotNull
    public String getReplicaHost() {
        return replicaHost;
    }

    public boolean isWorkingReplica() {
        return workingReplica;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public boolean isSuccessOperation() {
        return successOperation;
    }

    public boolean isBadArgument() {
        return badArgument;
    }

    public long getDeletedTimestamp() {
        return deletedTimestamp;
    }

    public long getValueTimestamp() {
        return valueTimestamp;
    }
}
