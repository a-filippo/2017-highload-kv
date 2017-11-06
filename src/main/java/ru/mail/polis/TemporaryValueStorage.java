package ru.mail.polis;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.Nullable;

import ru.mail.polis.dao.DAOValue;

public class TemporaryValueStorage implements Closeable {
    private InputStream inputStream;

    @Nullable
    private String pathToFile;
    private byte[] byteValue;

    private ReturnInputStream returnInputStream;
    private List<InputStream> inputStreams;


    public TemporaryValueStorage(String pathToDir, InputStream inputStream, int size) throws IOException{
        this.inputStream = inputStream;
        String filename = System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
        inputStreams = Collections.synchronizedList(new ArrayList<>());

        if (size < DAOValue.MIN_VALUE_SIZE_FOR_STORAGE_IN_FILE){
            byteValue = new byte[size];
            inputStream.read(byteValue);
            returnInputStream = () -> new ByteArrayInputStream(byteValue);
        } else {
            pathToFile = pathToDir + File.separator + filename;
            IOHelpers.copy(inputStream, new FileOutputStream(pathToFile));
            returnInputStream = () -> new FileInputStream(pathToFile);
        }
        inputStream.close();
    }

    public InputStream getInputStream() throws IOException {
        InputStream inputStream = returnInputStream.inputStream();
        inputStreams.add(inputStream);
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
        for (InputStream inputStream : inputStreams){
            inputStream.close();
        }
        if (pathToFile != null){
            Files.delete(Paths.get(pathToFile));
        }
    }

    private interface ReturnInputStream{
        InputStream inputStream() throws IOException;
    }
}
