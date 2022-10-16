package core;

public class ByteUtils {
    public static final byte PAYLOAD_LENGTH = 0b00000101; //5

    public static short combine(byte b1, byte b2) {
        short sh = (short) b1; //e.g. 11111111 -> 00000000 11111111
        sh <<= 8;  //e.g. 00000000 11111111 -> 11111111 00000000

        return (short) (sh | b2);   //e.g.  11111111 00000000
    }                               //               10101010
                                    //      ----------------- (bitwise OR comparison)
                                    //      11111111 10101010

}

