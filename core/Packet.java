package core;

import core.fields.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Packet {
    public final byte packetLengthFirst;
    public final byte packetLengthSecond;
    public final byte state;
    public final byte cookieFirst;
    public final byte cookieSecond;

    private final byte[] payload;
    private final byte[] data;

    private byte[] packet;

    public Packet(short packetLength, byte state, short cookie, byte[] data){
        String binaryPacketLength = Integer.toBinaryString(0xFFFF & packetLength);
        String binaryCookie = Integer.toBinaryString(0xFFFF & cookie);

        if(cookie == 0 && state != States.Authentication){
            throw new IllegalArgumentException("A packets cookie can't be empty if the state is not authentication");
        }

        this.packetLengthFirst = Byte.parseByte(binaryPacketLength.substring(0, 8));
        this.packetLengthSecond = Byte.parseByte(binaryPacketLength.substring(8, 16));
        this.state = state;
        this.cookieFirst = Byte.parseByte(binaryCookie.substring(0, 8));
        this.cookieSecond = Byte.parseByte(binaryCookie.substring(8, 16));

        this.data = data;
        this.payload = combinePayload();
    }
    //Overload constructor with received packet processing
    public Packet(byte[] packet){
        this.packet = packet;

        this.payload = Arrays.copyOfRange(packet, 0, ByteUtils.PAYLOAD_LENGTH);
        this.data = Arrays.copyOfRange(packet, ByteUtils.PAYLOAD_LENGTH, packet.length);

        this.packetLengthFirst = payload[0];;
        this.packetLengthSecond = payload[1];;
        this.state = payload[2];
        this.cookieFirst = payload[3];
        this.cookieSecond = payload[4];

    }

    private byte[] combinePayload(){
        return new byte[] {packetLengthFirst, packetLengthSecond, state, cookieFirst, cookieSecond};
    }

    public byte[] get(){
        //Concatenating payload and data together
        this.packet = Arrays.copyOf(payload, payload.length + data.length);
        System.arraycopy(data, 0, data, payload.length, data.length);
        return this.packet;
    }

    public short getShortPacketLength(){
        return ByteUtils.combine(packetLengthFirst, packetLengthSecond);
    }

    public short getShortCookie(){
        return ByteUtils.combine(cookieFirst, cookieSecond);
    }

    public String getStringData(){
        return new String(data, StandardCharsets.UTF_8);
    }
}
