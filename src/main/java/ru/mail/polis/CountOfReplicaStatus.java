package ru.mail.polis;

public class CountOfReplicaStatus {
    final public OneReplicaStatus working;
    final public OneReplicaStatus put;
    final public OneReplicaStatus notFound;
    final public OneReplicaStatus deleted;
//    final public OneReplicaStatus working;
//    final public OneReplicaStatus working;

    CountOfReplicaStatus(){
        working = new OneReplicaStatus();
        put = new OneReplicaStatus();
        notFound = new OneReplicaStatus();
        deleted = new OneReplicaStatus();
    }

    public class OneReplicaStatus{
        private int count;

        OneReplicaStatus(){
            count = 0;
        }

        public int get(){
            return count;
        }

        public void plus(){
            count++;
        }
    }
}
