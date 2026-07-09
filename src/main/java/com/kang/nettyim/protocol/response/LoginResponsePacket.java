package com.kang.nettyim.protocol.response;

import com.kang.nettyim.protocol.Command;
import com.kang.nettyim.protocol.Packet;
import lombok.Data;

@Data
public class LoginResponsePacket extends Packet {

    private boolean success;

    private String reason;

    @Override
    public Byte getCommand() {
        return Command.LOGIN_RESPONSE;
    }
}
