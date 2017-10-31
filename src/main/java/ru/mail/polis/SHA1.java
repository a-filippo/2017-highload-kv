package ru.mail.polis;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class SHA1 {
    public static String calculateHash(File file) throws NoSuchAlgorithmException {
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            InputStream fis = new FileInputStream(file);
            int n = 0;
            byte[] buffer = new byte[8192];
            while (n != -1) {
                n = fis.read(buffer);
                if (n > 0) {
                    crypt.update(buffer, 0, n);
                }
            }
            return byteToHex(crypt.digest());
        } catch (Exception e){
            e.printStackTrace();
            throw new NoSuchAlgorithmException();
        }
    }

    public static String calculateHash(byte[] bytes) throws NoSuchAlgorithmException{
        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
        crypt.reset();
        crypt.update(bytes);
        return byteToHex(crypt.digest());
    }

    private static String byteToHex(final byte[] hash){
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
