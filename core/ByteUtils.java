package core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ByteUtils {
    public static final byte PAYLOAD_LENGTH = 0b00000101; //5

    public static short combine(byte b1, byte b2) {
        return (short) ((b2 << 8) + (b1 & 0xFF));
    }
    public static byte[] readFileToBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }
}

