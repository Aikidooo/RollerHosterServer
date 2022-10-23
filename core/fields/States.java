package core.fields;

public class States {
    public static final byte None = -1;
    public static final byte Authentication = 0b00000000; //0
    public static final byte ExchangeFiletree = 0b00000001; //1
    public static final byte FileDownload = 0b00000010; //2
    public static final byte FileUpload = 0b00000011; //3
    public static final byte Debug = 0b00000101; //4

    public static String get(byte state){
        if(state == Authentication) return "Authentication";
        else if(state == ExchangeFiletree) return "FiletreeExchange";
        else if(state == FileDownload) return "FileDownload";
        else if(state == FileUpload) return "FileUpload";
        else if(state == Debug) return "Debug";
        else return "None";
    }
}
