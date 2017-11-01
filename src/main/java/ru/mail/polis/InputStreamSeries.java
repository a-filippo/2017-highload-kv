package ru.mail.polis;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.NotNull;

public class InputStreamSeries extends InputStream{
    private int[] inputStreamSizes;
    private int byteInBlock;
    private int inputStreamsCount;
    private boolean[] needReeding;

    private InputStream[] inputStreams;

    public InputStreamSeries(int byteInBlock, int[] inputStreamSizes){
        this.inputStreamSizes = inputStreamSizes;
        this.byteInBlock = byteInBlock;
        inputStreamsCount = inputStreamSizes.length;

        inputStreams = new InputStream[inputStreamsCount];
        needReeding = new boolean[inputStreamsCount];
        for (int i = 0; i < inputStreamsCount; i++){
            needReeding[i] = true;
        }
    }

    public void read(InputStream inputStream) throws IOException {
        final byte[] defaultBuffer = new byte[byteInBlock];
        byte[] buffer;

        int[] inputStreamsReaded = new int[inputStreamsCount];

        int count;

        boolean read = true;
        int index = 0;

        while (read){
            buffer = getBufferForReading(defaultBuffer, inputStreamsReaded[index], index);

            while ((count = inputStream.read(buffer)) >= 0) {
//                outputStream.write(buffer, 0, count);
            }
        }


    }

    private byte[] getBufferForReading(byte[] defaultBuffer, int inputStreamReaded, int index){
        if (inputStreamReaded + byteInBlock <= inputStreamSizes[index]){
            return defaultBuffer;
        } else {
            return new byte[inputStreamSizes[index] - inputStreamReaded];
        }
    }


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
