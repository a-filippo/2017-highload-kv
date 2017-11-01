package ru.mail.polis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ListOfReplicas implements Iterable<String>{
    @NotNull
    private Set<String> replicas;

    public ListOfReplicas(ListOfReplicas listOfReplicas){
        replicas = new HashSet<>(listOfReplicas.replicas);
    }

    public ListOfReplicas(@NotNull Set<String> replicas) {
        this.replicas = new HashSet<>(replicas);
    }

    public ListOfReplicas(@Nullable String line) {
        if (line == null){
            this.replicas = new HashSet<>();
        } else {
            this.replicas = new HashSet<>(Arrays.asList(line.split(",")));
        }
    }

    public ListOfReplicas() {
        replicas = new HashSet<>();
    }

    @NotNull
    public String toLine(){
        return StringUtils.join(replicas, ",");
    }

    public void add(String value){
        replicas.add(value);
    }

    public void remove(String value){
        replicas.remove(value);
    }

    public int size(){
        return replicas.size();
    }

    public void exclude(ListOfReplicas otherList){
        replicas.removeAll(otherList.replicas);
    }

    public boolean empty(){
        return replicas.isEmpty();
    }

    public String[] toArray(){
        return replicas.toArray(new String[replicas.size()]);
    }

    public boolean contains(String value){
        return replicas.contains(value);
    }

    @NotNull
    @Override
    public Iterator<String> iterator() {
        return replicas.iterator();
    }

    @Override
    public Spliterator<String> spliterator() {
        return replicas.spliterator();
    }
}
