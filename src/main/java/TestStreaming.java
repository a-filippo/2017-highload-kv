import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.input.TeeInputStream;

public class TestStreaming {
    public static void main(String[] args) {
        try {
            InputStream inputStream = new FileInputStream("/Users/afilippo/polis/big_file");

            OutputStream outputStream1 = new FileOutputStream("/Users/afilippo/polis/big_file_1");
            OutputStream outputStream2 = new FileOutputStream("/Users/afilippo/polis/big_file_2");

            InputStream inputStream1 = new TeeInputStream(inputStream, outputStream1);

            final byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = inputStream1.read(buffer)) >= 0) {
                outputStream2.write(buffer, 0, count);
            }


        } catch (Exception exception){
            exception.printStackTrace();
        }
    }
}
