package ru.mail.polis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IOHelpers {
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException{
        final byte[] buffer = new byte[64 * 1024];
        int count;
        while ((count = inputStream.read(buffer)) >= 0) {
            outputStream.write(buffer, 0, count);
        }
    }

    public static void createDirIfNoExists(String stringPath) throws IOException{
        Path path = Paths.get(stringPath);
        if (!java.nio.file.Files.exists(path)) {
            try {
                Files.createDirectory(path);
            } catch (FileAlreadyExistsException e){
                // nothing
            }
        }
    }
}
