package ru.mail.polis.dao;

import java.io.IOException;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

public interface DAO {
    @NotNull
    String getStoragePath();

    @NotNull
    DAOValue get (@NotNull String key) throws NoSuchElementException, IOException, IllegalArgumentException;

    void put (@NotNull String key, @NotNull DAOValue value) throws IOException, IllegalArgumentException;

    void delete (@NotNull String key, long deleteTimestamp) throws IOException, IllegalArgumentException;
}
