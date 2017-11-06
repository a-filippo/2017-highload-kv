package ru.mail.polis.dao.daomodel;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DAOModel {
    void stop() throws IOException;

    @Nullable
    DAOModelValue getValue(@NotNull String key) throws IOException;

    @Nullable
    String getPath(@NotNull String key) throws IOException;

    void putValue(@NotNull DAOModelValue value, boolean issetInStore) throws IOException;

    void deleteValue(@NotNull String key, long deleteTimestamp) throws IOException;
}
