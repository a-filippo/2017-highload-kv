package ru.mail.polis.DAO.DAOModel;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DAOModel {
    @Nullable
    DAOModelValue getValue(@NotNull String key) throws IOException;

    @Nullable
    String getPath(@NotNull String key) throws IOException;

    void putValue(@NotNull DAOModelValue value, boolean issetInStore) throws IOException;

    void deleteValue(@NotNull String key) throws IOException;
}
