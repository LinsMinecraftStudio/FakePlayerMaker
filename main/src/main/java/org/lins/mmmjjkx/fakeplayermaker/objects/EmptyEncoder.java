package org.lins.mmmjjkx.fakeplayermaker.objects;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class EmptyEncoder extends MessageToByteEncoder<String> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, String string, ByteBuf byteBuf) {
        byteBuf.writeBytes(string.getBytes());
    }
}
