package ru.mail.polis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOHelpers {
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException{
        final byte[] buffer = new byte[64 * 1024];
        int count;
        while ((count = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, count);
        }
    }
}
