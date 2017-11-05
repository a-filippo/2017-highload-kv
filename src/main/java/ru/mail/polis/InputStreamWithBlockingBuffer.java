package ru.mail.polis;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.NotNull;

public class InputStreamWithBlockingBuffer extends InputStream{
    @Override
    public int read() throws IOException {
        return 0;
    }

    @Override
    public int read(@NotNull byte[] b) throws IOException {
        return super.read(b);
    }

    @Override
    public int read(@NotNull byte[] b, int off, int len) throws IOException {
        return super.read(b, off, len);
    }
}
