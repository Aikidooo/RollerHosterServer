package core;

import core.fields.States;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

//THIS IS JUST TEMPORARY TEST CLIENT FOR TESTING THE SERVER!!!

public class Client {
    private Socket clientSocket;
    private OutputStream out;
    private BufferedInputStream in;

    public void startConnection(String ip, int port) throws IOException{
        clientSocket = new Socket(ip, port);
        in = new BufferedInputStream(clientSocket.getInputStream());
        out = clientSocket.getOutputStream();
    }

    public void sendMessage(String content, byte state) throws IOException{
        byte[] bContent = content.getBytes(StandardCharsets.UTF_8);

        byte[] payload = new byte[ByteUtils.PAYLOAD_LENGTH];

        short packetLength = (short)(ByteUtils.PAYLOAD_LENGTH + bContent.length);
        short cookie = 0b0000000000000000; //not necessary in send

        String binaryPacketLength = Integer.toBinaryString(0xFFFF & packetLength);
        String binaryCookie = "0000000000000000";

        payload[0] = Byte.parseByte(binaryPacketLength.substring(0, 8));
        payload[1] = Byte.parseByte(binaryPacketLength.substring(8, 16));
        payload[2] = state;
        payload[3] = Byte.parseByte(binaryCookie.substring(0, 8));
        payload[4] = Byte.parseByte(binaryCookie.substring(8, 16));

        //Concatenating payload and contents together
        byte[] both = Arrays.copyOf(payload, payload.length + bContent.length);
        System.arraycopy(bContent, 0, both, payload.length, bContent.length);

        out.write(both);

    }

    public String receiveMessage() throws IOException{
        byte[] rawData = new byte[10000];
        int readNum = in.read(rawData);
        byte[] data = Arrays.copyOfRange(rawData, 0, readNum);


        short packetLength = (short)(((short)data[0] << 8) | (short) data[1]); //Probably no work
        byte state = data[2];
        short cookie = (short)(((short)data[3] << 8) | (short) data[4]); //Probably also no work
        byte[] content = Arrays.copyOfRange(data, 5, data.length);

        return new String(content, StandardCharsets.UTF_8);
    }

    public void stopConnection() throws IOException{
        in.close();
        out.close();
        clientSocket.close();
    }

    public static void main(String[] args){
        Client client = new Client();
        try {
            client.startConnection("localhost", 10000);
            System.out.println("Sending authentication token");
            client.sendMessage("NiceToEatYou", States.Authentication);

        } catch(IOException e){
            e.printStackTrace();
        }
    }
}
