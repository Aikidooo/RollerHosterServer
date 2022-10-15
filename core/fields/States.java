package core.fields;

public class States {
    public static final byte Authentication = 0b00000000; //0
    public static final byte SyncCheck = 0b00000001; //1
    public static final byte FileTransfer = 0b00000010; //2
    public static final byte Disconnect = 0b00000011; //3
}
