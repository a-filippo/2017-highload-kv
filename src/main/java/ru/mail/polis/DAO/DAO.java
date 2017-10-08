package ru.mail.polis.DAO;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

import org.jetbrains.annotations.NotNull;

public interface DAO {
    @NotNull
    DAOValue get (@NotNull String key) throws NoSuchElementException, IOException, IllegalArgumentException;

    void put (@NotNull String key, @NotNull DAOValue value) throws IOException, IllegalArgumentException;

    void delete (@NotNull String key) throws IOException, IllegalArgumentException;
}
