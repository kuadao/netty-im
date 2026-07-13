package com.kang.nettyim.protocol;

import com.kang.nettyim.protocol.request.LoginRequestPacket;
import com.kang.nettyim.protocol.request.MessageRequestPacket;
import com.kang.nettyim.protocol.response.LoginResponsePacket;
import com.kang.nettyim.protocol.response.MessageResponsePacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.HashMap;
import java.util.Map;

public class PacketCodeC {

    public static final PacketCodeC INSTANCE = new PacketCodeC();

    private static final int MAGIC_NUMBER = 0x12345678;

    private static final Map<Byte, Class<? extends Packet>> COMMAND_TO_PACK_TYPE;
    private static final Map<Byte, Serializer> ALGORITHM_TO_SERIALIZER;

    static {
        COMMAND_TO_PACK_TYPE = new HashMap<>();
        COMMAND_TO_PACK_TYPE.put(Command.LOGIN_REQUEST, LoginRequestPacket.class);
        COMMAND_TO_PACK_TYPE.put(Command.LOGIN_RESPONSE, LoginResponsePacket.class);
        COMMAND_TO_PACK_TYPE.put(Command.MESSAGE_REQUEST, MessageRequestPacket.class);
        COMMAND_TO_PACK_TYPE.put(Command.MESSAGE_RESPONSE, MessageResponsePacket.class);

        ALGORITHM_TO_SERIALIZER = new HashMap<>();
        ALGORITHM_TO_SERIALIZER.put(SerializerAlgorithm.JSON, new JSONSerializer());
    }

    public ByteBuf encode(Packet packet, ByteBuf buffer) {
        byte[] bytes = Serializer.DEFAULT.serialize(packet);
        buffer.writeInt(MAGIC_NUMBER);
        buffer.writeByte(packet.getVersion());
        buffer.writeByte(Serializer.DEFAULT.getSerializerAlgorithm());
        buffer.writeByte(packet.getCommand());
        buffer.writeInt(bytes.length);
        buffer.writeBytes(bytes);
        return buffer;
    }

    public ByteBuf encode(Packet packet, ByteBufAllocator allocator) {
        return encode(packet, allocator.ioBuffer());
    }

    public Packet decode(ByteBuf buffer) {
        buffer.skipBytes(4);
        buffer.skipBytes(1);
        byte serializerAlgorithm = buffer.readByte();
        byte command = buffer.readByte();
        int length = buffer.readInt();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        Class<? extends Packet> packetType = COMMAND_TO_PACK_TYPE.get(command);
        Serializer serializer = ALGORITHM_TO_SERIALIZER.get(serializerAlgorithm);
        if (packetType != null && serializer != null) {
            return serializer.deserialize(bytes, packetType);
        }
        return null;
    }

    public static void main(String[] args) {
        LoginRequestPacket loginRequestPacket = new LoginRequestPacket();
        loginRequestPacket.setUsername("张三");
        loginRequestPacket.setUserId(1);
        loginRequestPacket.setPassword("10086");
        PacketCodeC packetCodeC = new PacketCodeC();
        ByteBuf encode = packetCodeC.encode(loginRequestPacket, ByteBufAllocator.DEFAULT);
        Packet decode = packetCodeC.decode(encode);
        System.out.println(decode);
    }
}
