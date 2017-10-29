package ru.mail.polis;

public class ReplicaParameters {
    private int ack;
    private int from;
    ReplicaParameters(String parameters){
        String[] parameter = parameters.split("/");
        ack = Integer.parseInt(parameter[0]);
        from = Integer.parseInt(parameter[1]);
    }

    ReplicaParameters(int ack, int from){
        this.ack = ack;
        this.from = from;
    }

    public int ack() {
        return ack;
    }

    public int from() {
        return from;
    }
}
