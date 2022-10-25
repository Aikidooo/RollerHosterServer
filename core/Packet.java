package core;

import core.fields.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Packet {
    public final byte packetLengthFirst;
    public final byte packetLengthSecond;
    public final byte state;
    public byte cookieFirst;
    public byte cookieSecond;

    public final byte[] payload;
    public final byte[] data;

    private byte[] packet;

    Logger logger = new Logger("packet");

    Packet(short packetLength, byte state, short cookie, byte[] data){
        if(cookie == 0 && state != States.Authentication){
            logger.log("ERROR", "Invalid cookie.");
            throw new IllegalArgumentException("A packets cookie can't be empty if the state is not authentication");
        }

        this.packetLengthFirst = (byte) packetLength;
        this.packetLengthSecond = (byte) ((packetLength >> 8) & 0xFF);
        this.state = state;
        this.cookieFirst = (byte) cookie;
        this.cookieSecond = (byte) ((cookie >> 8) & 0xFF);
        this.data = data;
        this.payload = combinePayload();

        logger.log("INFO",  "Packet built.");
    }
    //Overload constructor with received packet processing
    Packet(byte[] packet){
        this.packet = packet;

        this.payload = Arrays.copyOfRange(packet, 0, ByteUtils.PAYLOAD_LENGTH);
        this.data = Arrays.copyOfRange(packet, ByteUtils.PAYLOAD_LENGTH, packet.length);

        this.packetLengthFirst = payload[0];
        this.packetLengthSecond = payload[1];
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
        System.arraycopy(data, 0, packet, payload.length, data.length);
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
