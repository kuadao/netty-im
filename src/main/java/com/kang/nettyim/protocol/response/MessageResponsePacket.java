package com.kang.nettyim.protocol.response;

import com.kang.nettyim.protocol.Command;
import com.kang.nettyim.protocol.Packet;
import lombok.Data;

@Data
public class MessageResponsePacket extends Packet {

    private String message;

    @Override
    public Byte getCommand() {
        return Command.MESSAGE_RESPONSE;
    }
}
