package io.ristretto.decaptcha.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class IOHelper {

    private static final int EOF = -1;

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static long copy(InputStream input, OutputStream output) throws IOException {
        return copy(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    public static long copy(InputStream input, OutputStream output, byte[] buffer) throws IOException {
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != EOF) {
            output.write(buffer, 0, read);
            total += read;
        }
        return total;
    }

}
